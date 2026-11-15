package io.github.pism0.golemancy.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A construct energy fuel (SDD 6.4). Data-driven: {@code data/<namespace>/golemancy/fuel/<id>.json}.
 * Fuels are items in v0.1 (hand-fed or by deployer); fluid recharge points arrive in Milestone 6.
 *
 * @param ingredient the item or tag that counts as this fuel
 * @param energy     construct energy restored per item
 */
public record FuelDef(Ingredient ingredient, int energy) {
    public static final Codec<FuelDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(FuelDef::ingredient),
            Codec.intRange(1, 1_000_000).fieldOf("energy").forGetter(FuelDef::energy)
    ).apply(i, FuelDef::new));
}
