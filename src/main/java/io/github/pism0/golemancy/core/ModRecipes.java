package io.github.pism0.golemancy.core;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.block.BindingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Golemancy recipe types. Everything except {@code golemancy:binding} uses stock Create types. */
public final class ModRecipes {
    private ModRecipes() {
    }

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Golemancy.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Golemancy.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BindingRecipe>> BINDING =
            TYPES.register("binding", () -> RecipeType.simple(Golemancy.id("binding")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BindingRecipe>> BINDING_SERIALIZER =
            SERIALIZERS.register("binding", BindingRecipe.Serializer::new);
}
