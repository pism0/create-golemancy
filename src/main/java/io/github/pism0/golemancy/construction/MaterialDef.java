package io.github.pism0.golemancy.construction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Optional;

/**
 * A body material (SDD 5.2). Data-driven: {@code data/<namespace>/golemancy/material/<id>.json}.
 *
 * @param healthMultiplier    multiplies the frame's base health
 * @param armorMultiplier     multiplies the frame's base armour
 * @param speedMultiplier     multiplies the frame's base movement speed
 * @param knockbackResistance added to the frame's base knockback resistance
 * @param conductivity        scales spell power and energy capacity
 * @param affinity            optional Iron's school id this material favours
 * @param absorbs             damage types that heal instead of harm (SDD 10.3)
 * @param resists             damage types that are reduced
 * @param resistFactor        fraction of resisted damage that still gets through
 * @param repairItem          item used for field repair and for rebuilding Remains (SDD 11.3)
 * @param texture             texture name; the frame model uses {@code textures/entity/<frame>_<texture>.png}
 * @param color               RGB tint for the Dormant Golem statuette and the UI
 * @param waterRegen          health regenerated per second while in water or rain
 * @param strikeSlowTicks     Slowness duration applied by this material's melee hits (0 = none)
 */
public record MaterialDef(
        float healthMultiplier,
        float armorMultiplier,
        float speedMultiplier,
        float knockbackResistance,
        float conductivity,
        Optional<ResourceLocation> affinity,
        TagKey<DamageType> absorbs,
        TagKey<DamageType> resists,
        float resistFactor,
        Ingredient repairItem,
        String texture,
        int color,
        float waterRegen,
        int strikeSlowTicks
) {
    public static final Codec<MaterialDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("health_multiplier", 1f).forGetter(MaterialDef::healthMultiplier),
            Codec.FLOAT.optionalFieldOf("armor_multiplier", 1f).forGetter(MaterialDef::armorMultiplier),
            Codec.FLOAT.optionalFieldOf("speed_multiplier", 1f).forGetter(MaterialDef::speedMultiplier),
            Codec.FLOAT.optionalFieldOf("knockback_resistance", 0f).forGetter(MaterialDef::knockbackResistance),
            Codec.FLOAT.optionalFieldOf("conductivity", 1f).forGetter(MaterialDef::conductivity),
            ResourceLocation.CODEC.optionalFieldOf("affinity").forGetter(MaterialDef::affinity),
            TagKey.hashedCodec(Registries.DAMAGE_TYPE).fieldOf("absorbs").forGetter(MaterialDef::absorbs),
            TagKey.hashedCodec(Registries.DAMAGE_TYPE).fieldOf("resists").forGetter(MaterialDef::resists),
            Codec.floatRange(0f, 1f).optionalFieldOf("resist_factor", 0.5f).forGetter(MaterialDef::resistFactor),
            Ingredient.CODEC_NONEMPTY.fieldOf("repair_item").forGetter(MaterialDef::repairItem),
            Codec.STRING.fieldOf("texture").forGetter(MaterialDef::texture),
            Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(MaterialDef::color),
            Codec.FLOAT.optionalFieldOf("water_regen", 0f).forGetter(MaterialDef::waterRegen),
            Codec.INT.optionalFieldOf("strike_slow_ticks", 0).forGetter(MaterialDef::strikeSlowTicks)
    ).apply(i, MaterialDef::new));
}
