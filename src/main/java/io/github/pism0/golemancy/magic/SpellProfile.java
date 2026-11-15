package io.github.pism0.golemancy.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * A spell compatibility profile (SDD 7.5). The set of profiles is the allow-list: a spell without
 * one cannot be installed. Data-driven: {@code data/<namespace>/golemancy/spell_profile/<id>.json}.
 *
 * @param spell         the Iron's spell id
 * @param targeting     who the spell is aimed at
 * @param range         maximum distance to the target, in blocks
 * @param minCooldown   minimum ticks between casts
 * @param energyCost    construct energy per cast, before the core's cost multiplier
 * @param requiresLos   whether line of sight to the target is required
 * @param schoolNeutral whether any core with a spell slot may drive it, regardless of school
 * @param maxLevel      highest level a golem may use
 */
public record SpellProfile(
        ResourceLocation spell,
        Targeting targeting,
        float range,
        int minCooldown,
        float energyCost,
        boolean requiresLos,
        boolean schoolNeutral,
        int maxLevel
) {
    public static final Codec<SpellProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("spell").forGetter(SpellProfile::spell),
            Targeting.CODEC.fieldOf("targeting").forGetter(SpellProfile::targeting),
            Codec.FLOAT.optionalFieldOf("range", 16f).forGetter(SpellProfile::range),
            Codec.intRange(1, 72000).optionalFieldOf("min_cooldown", 60).forGetter(SpellProfile::minCooldown),
            Codec.FLOAT.optionalFieldOf("energy_cost", 10f).forGetter(SpellProfile::energyCost),
            Codec.BOOL.optionalFieldOf("requires_los", true).forGetter(SpellProfile::requiresLos),
            Codec.BOOL.optionalFieldOf("school_neutral", false).forGetter(SpellProfile::schoolNeutral),
            Codec.intRange(1, 100).optionalFieldOf("max_level", 3).forGetter(SpellProfile::maxLevel)
    ).apply(i, SpellProfile::new));

    public enum Targeting implements StringRepresentable {
        SELF("self"),
        ALLY("ally"),
        ENEMY("enemy"),
        AREA("area");

        public static final Codec<Targeting> CODEC = StringRepresentable.fromEnum(Targeting::values);
        private final String name;

        Targeting(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
