package io.github.pism0.golemancy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.pism0.golemancy.Golemancy;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

/**
 * The single source of truth for a golem (SDD 4.3). Immutable. Lives on Dormant Golem and Construct
 * Remains items as the {@code golemancy:golem_spec} data component and in the entity's saved data.
 * Attributes are always recalculated from it and never saved separately.
 *
 * <p>{@code frame}, {@code material} and {@code core} are ids in the {@code golemancy:frame},
 * {@code golemancy:material} and {@code golemancy:core} datapack registries. A shattered core is
 * {@link #NO_CORE}. "No owner" is {@link Util#NIL_UUID}.
 */
public record GolemSpec(
        int schema,
        ResourceLocation frame,
        ResourceLocation material,
        ResourceLocation core,
        List<ResourceLocation> inscriptions,
        List<InstalledSpell> spells,
        BehaviorConfig behavior,
        UUID owner,
        float coreIntegrity
) {
    /** Current data version. Bump it and add a step to {@link GolemSpecMigrator} on any format change. */
    public static final int CURRENT_SCHEMA = 1;

    public static final ResourceLocation DEFAULT_FRAME = Golemancy.id("guardian");
    public static final ResourceLocation DEFAULT_MATERIAL = Golemancy.id("stone");
    public static final ResourceLocation DEFAULT_CORE = Golemancy.id("arcane");
    /** Marker for a golem whose core shattered (integrity reached zero). */
    public static final ResourceLocation NO_CORE = Golemancy.id("none");

    /** Codec for the current schema only. Use {@link #CODEC} wherever the data may be old. */
    public static final Codec<GolemSpec> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("schema").forGetter(GolemSpec::schema),
            ResourceLocation.CODEC.fieldOf("frame").forGetter(GolemSpec::frame),
            ResourceLocation.CODEC.fieldOf("material").forGetter(GolemSpec::material),
            ResourceLocation.CODEC.fieldOf("core").forGetter(GolemSpec::core),
            ResourceLocation.CODEC.listOf().optionalFieldOf("inscriptions", List.of()).forGetter(GolemSpec::inscriptions),
            InstalledSpell.CODEC.listOf().optionalFieldOf("spells", List.of()).forGetter(GolemSpec::spells),
            BehaviorConfig.CODEC.optionalFieldOf("behavior", BehaviorConfig.DEFAULT).forGetter(GolemSpec::behavior),
            UUIDUtil.CODEC.optionalFieldOf("owner", Util.NIL_UUID).forGetter(GolemSpec::owner),
            Codec.FLOAT.optionalFieldOf("core_integrity", 1f).forGetter(GolemSpec::coreIntegrity)
    ).apply(i, GolemSpec::new));

    /**
     * The codec to use for saving and loading. Every decode runs {@link GolemSpecMigrator} first, so
     * data written by an older schema is upgraded step by step before it is parsed.
     */
    public static final Codec<GolemSpec> CODEC = Codec.PASSTHROUGH.flatXmap(
            dynamic -> GolemSpecMigrator.migrate(dynamic).flatMap(DIRECT_CODEC::parse),
            GolemSpec::encodeDynamic);

    public static final StreamCodec<RegistryFriendlyByteBuf, GolemSpec> STREAM_CODEC = StreamCodec.of(
            GolemSpec::write, GolemSpec::read);

    private static final StreamCodec<io.netty.buffer.ByteBuf, List<ResourceLocation>> ID_LIST =
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<io.netty.buffer.ByteBuf, List<InstalledSpell>> SPELL_LIST =
            InstalledSpell.STREAM_CODEC.apply(ByteBufCodecs.list());

    private static void write(RegistryFriendlyByteBuf buf, GolemSpec spec) {
        buf.writeVarInt(spec.schema);
        ResourceLocation.STREAM_CODEC.encode(buf, spec.frame);
        ResourceLocation.STREAM_CODEC.encode(buf, spec.material);
        ResourceLocation.STREAM_CODEC.encode(buf, spec.core);
        ID_LIST.encode(buf, spec.inscriptions);
        SPELL_LIST.encode(buf, spec.spells);
        BehaviorConfig.STREAM_CODEC.encode(buf, spec.behavior);
        UUIDUtil.STREAM_CODEC.encode(buf, spec.owner);
        buf.writeFloat(spec.coreIntegrity);
    }

    private static GolemSpec read(RegistryFriendlyByteBuf buf) {
        return new GolemSpec(
                buf.readVarInt(),
                ResourceLocation.STREAM_CODEC.decode(buf),
                ResourceLocation.STREAM_CODEC.decode(buf),
                ResourceLocation.STREAM_CODEC.decode(buf),
                ID_LIST.decode(buf),
                SPELL_LIST.decode(buf),
                BehaviorConfig.STREAM_CODEC.decode(buf),
                UUIDUtil.STREAM_CODEC.decode(buf),
                buf.readFloat());
    }

    public GolemSpec {
        inscriptions = List.copyOf(inscriptions);
        spells = List.copyOf(spells);
        coreIntegrity = Math.max(0f, Math.min(1f, coreIntegrity));
    }

    private static DataResult<Dynamic<?>> encodeDynamic(GolemSpec spec) {
        return DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, spec).map(json -> new Dynamic<>(JsonOps.INSTANCE, json));
    }

    /** A fresh spec at the current schema with default behaviour and a full-integrity core. */
    public static GolemSpec create(ResourceLocation frame, ResourceLocation material, ResourceLocation core) {
        return new GolemSpec(CURRENT_SCHEMA, frame, material, core, List.of(), List.of(),
                BehaviorConfig.DEFAULT, Util.NIL_UUID, 1f);
    }

    public boolean hasOwner() {
        return !Util.NIL_UUID.equals(owner);
    }

    public boolean hasCore() {
        return !NO_CORE.equals(core);
    }

    public GolemSpec withOwner(UUID newOwner) {
        return new GolemSpec(schema, frame, material, core, inscriptions, spells, behavior,
                newOwner == null ? Util.NIL_UUID : newOwner, coreIntegrity);
    }

    public GolemSpec withBehavior(BehaviorConfig b) {
        return new GolemSpec(schema, frame, material, core, inscriptions, spells, b, owner, coreIntegrity);
    }

    public GolemSpec withCoreIntegrity(float integrity) {
        return new GolemSpec(schema, frame, material, core, inscriptions, spells, behavior, owner, integrity);
    }

    public GolemSpec withCore(ResourceLocation newCore, float integrity) {
        return new GolemSpec(schema, frame, material, newCore, inscriptions, spells, behavior, owner, integrity);
    }

    public GolemSpec withSpells(List<ResourceLocation> newInscriptions, List<InstalledSpell> newSpells) {
        return new GolemSpec(schema, frame, material, core, newInscriptions, newSpells, behavior, owner, coreIntegrity);
    }

    public GolemSpec withIds(ResourceLocation newFrame, ResourceLocation newMaterial, ResourceLocation newCore) {
        return new GolemSpec(schema, newFrame, newMaterial, newCore, inscriptions, spells, behavior, owner, coreIntegrity);
    }

    /** The core shattered: keep the frame, lose the core and every installed spell (SDD 11.3). */
    public GolemSpec shattered() {
        return new GolemSpec(schema, frame, material, NO_CORE, List.of(), List.of(), behavior, owner, 0f);
    }
}
