package io.github.pism0.golemancy.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.pism0.golemancy.core.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * {@code golemancy:binding} (SDD 8.2): says which frame item becomes which frame and material, and
 * how long and how fast the Binding Altar must turn. One recipe per frame item keeps the recipe
 * count linear; the core and the inscription are combined in code by {@link BindingLogic}.
 *
 * <pre>{@code
 * {
 *   "type": "golemancy:binding",
 *   "frame": { "item": "golemancy:frame/stone_guardian" },
 *   "golem_frame": "golemancy:guardian",
 *   "golem_material": "golemancy:stone",
 *   "processing_time": 200,
 *   "min_speed": 32
 * }
 * }</pre>
 *
 * @param frame          the frame item (or tag) placed on the altar
 * @param golemFrame     id in the {@code golemancy:frame} registry
 * @param golemMaterial  id in the {@code golemancy:material} registry
 * @param processingTime ticks at 32 RPM; faster rotation shortens it proportionally
 * @param minSpeed       minimum rotation speed in RPM (the server config sets a floor)
 */
public record BindingRecipe(Ingredient frame, ResourceLocation golemFrame, ResourceLocation golemMaterial,
                            int processingTime, int minSpeed) implements Recipe<BindingRecipe.Input> {

    public static final int DEFAULT_TIME = 200;
    public static final int DEFAULT_SPEED = 32;

    public static final MapCodec<BindingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("frame").forGetter(BindingRecipe::frame),
            ResourceLocation.CODEC.fieldOf("golem_frame").forGetter(BindingRecipe::golemFrame),
            ResourceLocation.CODEC.fieldOf("golem_material").forGetter(BindingRecipe::golemMaterial),
            Codec.intRange(1, 72000).optionalFieldOf("processing_time", DEFAULT_TIME).forGetter(BindingRecipe::processingTime),
            Codec.intRange(1, 256).optionalFieldOf("min_speed", DEFAULT_SPEED).forGetter(BindingRecipe::minSpeed)
    ).apply(i, BindingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BindingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, BindingRecipe::frame,
            ResourceLocation.STREAM_CODEC, BindingRecipe::golemFrame,
            ResourceLocation.STREAM_CODEC, BindingRecipe::golemMaterial,
            ByteBufCodecs.VAR_INT, BindingRecipe::processingTime,
            ByteBufCodecs.VAR_INT, BindingRecipe::minSpeed,
            BindingRecipe::new);

    /** The frame item on the altar. */
    public record Input(ItemStack frame) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return frame;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        return frame.test(input.frame());
    }

    /** The real output depends on the core and inscription; see {@link BindingLogic}. */
    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, frame);
    }

    @Override
    public boolean isSpecial() {
        return true; // keep it out of the vanilla recipe book
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BINDING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.BINDING.get();
    }

    public static final class Serializer implements RecipeSerializer<BindingRecipe> {
        @Override
        public MapCodec<BindingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BindingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
