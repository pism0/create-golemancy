package io.github.pism0.golemancy.construction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.pism0.golemancy.data.BehaviorConfig;
import net.minecraft.resources.ResourceLocation;

/**
 * A frame: body shape, entity type, base attributes and slots (SDD 5.3).
 * Data-driven: {@code data/<namespace>/golemancy/frame/<id>.json}.
 *
 * @param entityType        the entity type that embodies this frame (one entity type per frame, SDD 10)
 * @param model             model name: {@code geo/<model>.geo.json}, {@code animations/<model>.animation.json}
 * @param health            base maximum health
 * @param armor             base armour
 * @param attackDamage      base melee damage
 * @param attackInterval    ticks between melee swings
 * @param movementSpeed     base movement speed attribute
 * @param knockbackResistance base knockback resistance
 * @param followRange       how far it notices targets
 * @param energyMultiplier  scales the core's energy capacity
 * @param spellSlots        how many spells can be installed
 * @param inscriptionSlots  how many inscription items the Binding Altar accepts
 * @param defaultProfile    behaviour profile a freshly bound golem starts with
 */
public record FrameDef(
        ResourceLocation entityType,
        String model,
        float health,
        float armor,
        float attackDamage,
        int attackInterval,
        float movementSpeed,
        float knockbackResistance,
        float followRange,
        float energyMultiplier,
        int spellSlots,
        int inscriptionSlots,
        BehaviorConfig.Profile defaultProfile
) {
    public static final Codec<FrameDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("entity_type").forGetter(FrameDef::entityType),
            Codec.STRING.fieldOf("model").forGetter(FrameDef::model),
            Codec.FLOAT.fieldOf("health").forGetter(FrameDef::health),
            Codec.FLOAT.optionalFieldOf("armor", 0f).forGetter(FrameDef::armor),
            Codec.FLOAT.fieldOf("attack_damage").forGetter(FrameDef::attackDamage),
            Codec.intRange(5, 200).optionalFieldOf("attack_interval", 20).forGetter(FrameDef::attackInterval),
            Codec.FLOAT.fieldOf("movement_speed").forGetter(FrameDef::movementSpeed),
            Codec.FLOAT.optionalFieldOf("knockback_resistance", 0f).forGetter(FrameDef::knockbackResistance),
            Codec.FLOAT.optionalFieldOf("follow_range", 24f).forGetter(FrameDef::followRange),
            Codec.FLOAT.optionalFieldOf("energy_multiplier", 1f).forGetter(FrameDef::energyMultiplier),
            Codec.intRange(0, 8).optionalFieldOf("spell_slots", 1).forGetter(FrameDef::spellSlots),
            Codec.intRange(0, 8).optionalFieldOf("inscription_slots", 1).forGetter(FrameDef::inscriptionSlots),
            BehaviorConfig.Profile.CODEC.optionalFieldOf("default_profile", BehaviorConfig.Profile.GUARDIAN)
                    .forGetter(FrameDef::defaultProfile)
    ).apply(i, FrameDef::new));
}
