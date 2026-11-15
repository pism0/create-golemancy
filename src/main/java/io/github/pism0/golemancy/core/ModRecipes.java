package io.github.pism0.golemancy.core;

import io.github.pism0.golemancy.Golemancy;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Golemancy recipe types. Everything except {@code golemancy:binding} uses stock Create types. */
public final class ModRecipes {
    private ModRecipes() {
    }

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Golemancy.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Golemancy.MOD_ID);
}
