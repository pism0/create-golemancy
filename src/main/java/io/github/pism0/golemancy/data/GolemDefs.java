package io.github.pism0.golemancy.data;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.construction.FrameDef;
import io.github.pism0.golemancy.construction.MaterialDef;
import io.github.pism0.golemancy.core.ModRegistries;
import io.github.pism0.golemancy.magic.CoreDef;
import io.github.pism0.golemancy.magic.FuelDef;
import io.github.pism0.golemancy.magic.SpellProfile;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lookups into Golemancy's datapack registries, and the "unknown id falls back to a safe default"
 * rule from SDD 4.3 / 15.3.
 */
public final class GolemDefs {
    private GolemDefs() {
    }

    public static Optional<MaterialDef> material(RegistryAccess access, ResourceLocation id) {
        return get(access, ModRegistries.MATERIAL, id);
    }

    public static Optional<FrameDef> frame(RegistryAccess access, ResourceLocation id) {
        return get(access, ModRegistries.FRAME, id);
    }

    public static Optional<CoreDef> core(RegistryAccess access, ResourceLocation id) {
        return get(access, ModRegistries.CORE, id);
    }

    /** The profile that allows {@code spellId}, if the spell is on the allow-list. */
    public static Optional<SpellProfile> spellProfile(RegistryAccess access, ResourceLocation spellId) {
        return access.registry(ModRegistries.SPELL_PROFILE)
                .flatMap(reg -> reg.stream().filter(p -> p.spell().equals(spellId)).findFirst());
    }

    /** The id of the core definition whose item is {@code stack}'s item. */
    public static Optional<ResourceLocation> coreIdForItem(RegistryAccess access, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        return access.registry(ModRegistries.CORE).flatMap(reg -> reg.entrySet().stream()
                .filter(e -> stack.is(e.getValue().item()))
                .map(e -> e.getKey().location())
                .findFirst());
    }

    /** Energy one item of {@code stack} restores, or 0 if it is not a fuel. Highest matching entry wins. */
    public static int fuelEnergy(RegistryAccess access, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return access.registry(ModRegistries.FUEL)
                .map(reg -> reg.stream().filter(f -> f.ingredient().test(stack)).mapToInt(FuelDef::energy).max().orElse(0))
                .orElse(0);
    }

    public static List<Map.Entry<ResourceKey<SpellProfile>, SpellProfile>> spellProfiles(RegistryAccess access) {
        return access.registry(ModRegistries.SPELL_PROFILE)
                .map(reg -> List.copyOf(reg.entrySet()))
                .orElse(List.of());
    }

    /**
     * Replaces ids that no longer exist (for example because a datapack removed them) with safe
     * defaults and logs a warning. Never throws: a broken spec must not crash the server.
     */
    public static GolemSpec sanitize(GolemSpec spec, RegistryAccess access) {
        ResourceLocation frame = spec.frame();
        ResourceLocation material = spec.material();
        ResourceLocation core = spec.core();

        if (frame(access, frame).isEmpty()) {
            Golemancy.LOGGER.warn("Unknown golem frame '{}', falling back to '{}'", frame, GolemSpec.DEFAULT_FRAME);
            frame = GolemSpec.DEFAULT_FRAME;
        }
        if (material(access, material).isEmpty()) {
            Golemancy.LOGGER.warn("Unknown golem material '{}', falling back to '{}'", material, GolemSpec.DEFAULT_MATERIAL);
            material = GolemSpec.DEFAULT_MATERIAL;
        }
        if (spec.hasCore() && core(access, core).isEmpty()) {
            Golemancy.LOGGER.warn("Unknown golem core '{}', falling back to '{}'", core, GolemSpec.DEFAULT_CORE);
            core = GolemSpec.DEFAULT_CORE;
        }

        List<InstalledSpell> spells = new ArrayList<>();
        for (InstalledSpell installed : spec.spells()) {
            if (spellProfile(access, installed.spell()).isPresent()) {
                spells.add(installed);
            } else {
                Golemancy.LOGGER.warn("Golem spell '{}' is not on the allow-list any more, removing it", installed.spell());
            }
        }

        GolemSpec result = spec;
        if (!frame.equals(spec.frame()) || !material.equals(spec.material()) || !core.equals(spec.core())) {
            result = result.withIds(frame, material, core);
        }
        if (spells.size() != spec.spells().size()) {
            result = result.withSpells(spec.inscriptions(), spells);
        }
        return result;
    }

    private static <T> Optional<T> get(RegistryAccess access, ResourceKey<Registry<T>> key, ResourceLocation id) {
        return access.registry(key).map(reg -> reg.get(id));
    }
}
