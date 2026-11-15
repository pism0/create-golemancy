package io.github.pism0.golemancy.core.datagen;

import com.simibubi.create.api.data.recipe.CompactingRecipeGen;
import com.simibubi.create.api.data.recipe.CuttingRecipeGen;
import com.simibubi.create.api.data.recipe.FillingRecipeGen;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import com.simibubi.create.api.data.recipe.PressingRecipeGen;
import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.block.BindingRecipe;
import io.github.pism0.golemancy.core.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.concurrent.CompletableFuture;

/**
 * All recipes. Everything except {@code golemancy:binding} is a stock Create recipe type (SDD 8.1),
 * generated with Create's own datagen helpers and kept under the {@code golemancy} namespace so
 * nothing can overwrite or be overwritten by Create or another addon (SDD 19).
 *
 * <p>The manufacturing path:
 * <pre>
 *   smooth stone --saw--> plates --basin+press--> limbs
 *   clay block --press--> plates                  plates --mechanical crafting--> head, torso shell
 *   torso shell --sequenced assembly (deploy limb x2, deploy head, press, deploy housing)--> frame
 *   copper sheet --sequenced assembly (deploy amethyst, press, deploy arcane essence)--> core housing
 *   core housing --spout (water | lava)--> quenched | molten housing --basin mix--> core
 *   frame + core (+ scroll) --Binding Altar--> Dormant Golem
 * </pre>
 */
final class ModRecipeProviders {
    private ModRecipeProviders() {
    }

    static void add(boolean run, DataGenerator generator, PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        generator.addProvider(run, new Parts(output, lookup));
        generator.addProvider(run, new Pressing(output, lookup));
        generator.addProvider(run, new Limbs(output, lookup));
        generator.addProvider(run, new Crafted(output, lookup));
        generator.addProvider(run, new Assembly(output, lookup));
        generator.addProvider(run, new Filling(output, lookup));
        generator.addProvider(run, new Mixing(output, lookup));
    }

    private static Item part(String material, String kind) {
        return ModItems.part(material, kind).get();
    }

    /** Stone plates are sawn from smooth stone. */
    private static final class Parts extends CuttingRecipeGen {
        Parts(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, Golemancy.MOD_ID);
            create("stone_plate", b -> b.require(Items.SMOOTH_STONE).duration(100).output(part("stone", "plate"), 2));
        }
    }

    /** Clay plates are pressed from clay blocks. */
    private static final class Pressing extends PressingRecipeGen {
        Pressing(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, Golemancy.MOD_ID);
            create("clay_plate", b -> b.require(Items.CLAY).output(part("clay", "plate"), 2));
        }
    }

    /** Limbs: two plates compacted around an andesite alloy joint. */
    private static final class Limbs extends CompactingRecipeGen {
        Limbs(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, Golemancy.MOD_ID);
            for (String material : ModItems.MATERIALS) {
                create("" + material + "_limb", b -> b
                        .require(part(material, "plate"))
                        .require(part(material, "plate"))
                        .require(ExternalIds.item(ExternalIds.ANDESITE_ALLOY))
                        .output(part(material, "limb")));
            }
        }
    }

    /** Heads and torso shells: mechanical crafting from plates. */
    private static final class Crafted extends MechanicalCraftingRecipeGen {
        Crafted(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, Golemancy.MOD_ID);
            for (String material : ModItems.MATERIALS) {
                create(() -> part(material, "head")).recipe(b -> b
                        .key('P', part(material, "plate"))
                        .patternLine("PP")
                        .patternLine("PP"));
                create(() -> part(material, "torso_shell")).recipe(b -> b
                        .key('P', part(material, "plate"))
                        .key('C', ExternalIds.item(ExternalIds.ANDESITE_CASING))
                        .patternLine("PPP")
                        .patternLine("PCP")
                        .patternLine("PPP"));
            }
        }
    }

    /** Sequenced assembly: the core housing, and one frame recipe per material and frame type (SDD 4.2). */
    private static final class Assembly extends SequencedAssemblyRecipeGen {
        Assembly(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, Golemancy.MOD_ID);

            // SDD 6.3 step 1: deploy amethyst shard, press, deploy Arcane Essence.
            create("core_housing", b -> b
                    .require(ExternalIds.item(ExternalIds.COPPER_SHEET))
                    .transitionTo(ModItems.INCOMPLETE_CORE_HOUSING.get())
                    .addOutput(ModItems.CORE_HOUSING.get(), 1)
                    .loops(1)
                    .addStep(DeployerApplicationRecipe::new, rb -> rb.require(Items.AMETHYST_SHARD))
                    .addStep(PressingRecipe::new, rb -> rb)
                    .addStep(DeployerApplicationRecipe::new, rb -> rb.require(ExternalIds.item(ExternalIds.ARCANE_ESSENCE))));

            for (String material : ModItems.MATERIALS) {
                Item shell = part(material, "torso_shell");
                Item limb = part(material, "limb");
                Item head = part(material, "head");
                Item plate = part(material, "plate");

                // SDD 8.3. Guardian: limbs, head, press, core housing socket.
                create("frame/" + material + "_guardian", b -> b
                        .require(shell)
                        .transitionTo(ModItems.INCOMPLETE_FRAMES.get(material + "_guardian").get())
                        .addOutput(ModItems.frame(material, "guardian").get(), 1)
                        .loops(1)
                        .addStep(DeployerApplicationRecipe::new, rb -> rb.require(limb))
                        .addStep(DeployerApplicationRecipe::new, rb -> rb.require(limb))
                        .addStep(DeployerApplicationRecipe::new, rb -> rb.require(head))
                        .addStep(PressingRecipe::new, rb -> rb)
                        .addStep(DeployerApplicationRecipe::new, rb -> rb.require(ModItems.CORE_HOUSING.get())));

                // Brute: starts with an extra armour plate. The different first step is also what lets
                // both frames share the same torso shell (the deployer's held item selects the recipe).
                create("frame/" + material + "_brute", b -> b
                        .require(shell)
                        .transitionTo(ModItems.INCOMPLETE_FRAMES.get(material + "_brute").get())
                        .addOutput(ModItems.frame(material, "brute").get(), 1)
                        .loops(1)
                        .addStep(DeployerApplicationRecipe::new, rb -> rb.require(plate))
                        .addStep(DeployerApplicationRecipe::new, rb -> rb.require(limb))
                        .addStep(DeployerApplicationRecipe::new, rb -> rb.require(limb))
                        .addStep(DeployerApplicationRecipe::new, rb -> rb.require(head))
                        .addStep(PressingRecipe::new, rb -> rb)
                        .addStep(DeployerApplicationRecipe::new, rb -> rb.require(ModItems.CORE_HOUSING.get())));
            }

            // Binding recipes and the two hand-crafted items ride along in this generator: vanilla's
            // RecipeProvider#getName is final, so a second plain RecipeProvider could not be told apart.
            all.add(recipes -> {
                for (String material : ModItems.MATERIALS) {
                    for (String frame : ModItems.FRAMES) {
                        recipes.accept(Golemancy.id("binding/" + material + "_" + frame),
                                new BindingRecipe(Ingredient.of(ModItems.frame(material, frame).get()),
                                        Golemancy.id(frame), Golemancy.id(material),
                                        BindingRecipe.DEFAULT_TIME, BindingRecipe.DEFAULT_SPEED),
                                null);
                    }
                }

                ItemLike essence = ExternalIds.item(ExternalIds.ARCANE_ESSENCE);
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BINDING_ALTAR.get())
                        .pattern("AEA")
                        .pattern("PCP")
                        .pattern(" S ")
                        .define('A', Items.AMETHYST_SHARD)
                        .define('E', essence)
                        .define('P', part("stone", "plate"))
                        .define('C', ExternalIds.item(ExternalIds.ANDESITE_CASING))
                        .define('S', ExternalIds.item(ExternalIds.SHAFT))
                        .unlockedBy("has_arcane_essence", has(essence))
                        .save(recipes, Golemancy.id("crafting/binding_altar"));

                ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CONTROL_WAND.get())
                        .pattern("  E")
                        .pattern(" A ")
                        .pattern("S  ")
                        .define('E', essence)
                        .define('A', ExternalIds.item(ExternalIds.ANDESITE_ALLOY))
                        .define('S', Items.STICK)
                        .unlockedBy("has_arcane_essence", has(essence))
                        .save(recipes, Golemancy.id("crafting/control_wand"));
            });
        }
    }

    /** SDD 6.3 step 2: spout-fill the housing. */
    private static final class Filling extends FillingRecipeGen {
        Filling(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, Golemancy.MOD_ID);
            create("quenched_core_housing", b -> b
                    .require(ModItems.CORE_HOUSING.get()).require(Fluids.WATER, 250)
                    .output(ModItems.QUENCHED_CORE_HOUSING.get()));
            create("molten_core_housing", b -> b
                    .require(ModItems.CORE_HOUSING.get()).require(Fluids.LAVA, 250)
                    .output(ModItems.MOLTEN_CORE_HOUSING.get()));

            // Create: Wizardry (SDD 9.3): its mana fills a housing straight into an Arcane Core,
            // skipping the stabilising mix. Loads only when the mod is present.
            create("compat/create_wizardry/arcane_core", b -> b
                    .require(ModItems.CORE_HOUSING.get())
                    .require(SizedFluidIngredient.of(ExternalIds.fluid(ExternalIds.WIZARDRY_MANA_FLUID), 500))
                    .output(ModItems.core("arcane").get())
                    .whenModLoaded(ExternalIds.WIZARDRY));
        }
    }

    /** SDD 6.3 step 3: stabilise in a basin with Arcane Essence. */
    private static final class Mixing extends MixingRecipeGen {
        Mixing(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, Golemancy.MOD_ID);
            Item essence = ExternalIds.item(ExternalIds.ARCANE_ESSENCE);
            create("arcane_core", b -> b
                    .require(ModItems.QUENCHED_CORE_HOUSING.get())
                    .require(essence).require(essence)
                    .require(Items.AMETHYST_SHARD)
                    .requiresHeat(HeatCondition.HEATED)
                    .output(ModItems.core("arcane").get()));
            create("fire_core", b -> b
                    .require(ModItems.MOLTEN_CORE_HOUSING.get())
                    .require(essence)
                    .require(Items.BLAZE_POWDER).require(Items.BLAZE_POWDER)
                    .requiresHeat(HeatCondition.HEATED)
                    .output(ModItems.core("fire").get()));
            // Frost is mixed cold.
            create("frost_core", b -> b
                    .require(ModItems.QUENCHED_CORE_HOUSING.get())
                    .require(essence)
                    .require(Items.PACKED_ICE).require(Items.PACKED_ICE)
                    .output(ModItems.core("frost").get()));
        }
    }
}
