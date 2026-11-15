package io.github.pism0.golemancy.data;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import io.github.pism0.golemancy.Golemancy;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Upgrades saved {@link GolemSpec} data to {@link GolemSpec#CURRENT_SCHEMA} one step at a time
 * (SDD 4.3, 15.3). Runs on every decode, on untyped data, before the record codec sees it.
 *
 * <p>To change the format: bump {@code CURRENT_SCHEMA}, append a step to {@link #STEPS} that turns
 * schema N data into schema N+1 data, and add a GameTest with a sample of the old data.
 */
public final class GolemSpecMigrator {
    private GolemSpecMigrator() {
    }

    /** STEPS.get(n) upgrades schema n to schema n + 1. */
    private static final List<UnaryOperator<Dynamic<?>>> STEPS = List.of(
            GolemSpecMigrator::v0ToV1
    );

    public static DataResult<Dynamic<?>> migrate(Dynamic<?> data) {
        // Data written before the schema field existed is schema 0.
        int schema = data.get("schema").asInt(0);
        if (schema > GolemSpec.CURRENT_SCHEMA) {
            return DataResult.error(() -> "GolemSpec schema " + schema
                    + " is newer than this version of Golemancy (" + GolemSpec.CURRENT_SCHEMA + ")");
        }
        if (schema < 0) {
            return DataResult.error(() -> "Invalid GolemSpec schema " + schema);
        }
        Dynamic<?> current = data;
        for (int from = schema; from < GolemSpec.CURRENT_SCHEMA; from++) {
            current = STEPS.get(from).apply(current);
            current = current.set("schema", current.createInt(from + 1));
            Golemancy.LOGGER.info("Migrated a GolemSpec from schema {} to {}", from, from + 1);
        }
        return DataResult.success(current);
    }

    /**
     * Schema 0 was the pre-release prototype format:
     * <ul>
     *   <li>{@code integrity} instead of {@code core_integrity}</li>
     *   <li>{@code spells} as a plain list of spell ids, all implicitly level 1</li>
     *   <li>{@code behaviour} (British spelling) instead of {@code behavior}</li>
     * </ul>
     */
    private static Dynamic<?> v0ToV1(Dynamic<?> d) {
        Dynamic<?> out = d;
        if (out.get("integrity").result().isPresent()) {
            out = out.set("core_integrity", out.createFloat(out.get("integrity").asFloat(1f))).remove("integrity");
        }
        if (out.get("behaviour").result().isPresent()) {
            out = setRaw(out, "behavior", out.get("behaviour").result().get()).remove("behaviour");
        }
        if (out.get("spells").result().isPresent()) {
            out = upgradeSpells(out);
        }
        return out;
    }

    private static <T> Dynamic<T> upgradeSpells(Dynamic<T> holder) {
        // v0: "irons_spellbooks:firebolt"   v1: {spell: "irons_spellbooks:firebolt", level: 1}
        List<Dynamic<T>> upgraded = holder.get("spells").asList(spell -> spell.asString().result()
                .map(id -> holder.emptyMap()
                        .set("spell", holder.createString(id))
                        .set("level", holder.createInt(1)))
                .orElse(spell));
        return holder.set("spells", holder.createList(upgraded.stream()));
    }

    @SuppressWarnings("unchecked")
    private static <T> Dynamic<T> setRaw(Dynamic<T> target, String key, Dynamic<?> value) {
        // Both dynamics come from the same source data, so they share ops and value type.
        return target.set(key, (Dynamic<T>) value);
    }
}
