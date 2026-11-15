// THROWAWAY SPIKE - not compiled into the mod. See spikes/README.md.
//
// Question: can a data component that we attach to the input of a sequenced assembly recipe
// survive to the transitional item and to the final result?
//
// What Create 6.0.10-280 does (com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe):
//
//   private ItemStack advance(ResourceLocation id, ItemStack input, RandomSource random) {
//       int step = getStep(input);
//       if ((step + 1) / sequence.size() >= loops)
//           return rollResult(random);                         // (a) fixed result from the pool, .copy()
//       ItemStack advancedItem = getTransitionalItem().copyWithCount(1);   // (b) FRESH stack every step
//       advancedItem.set(AllDataComponents.SEQUENCED_ASSEMBLY, new SequencedAssembly(id, step + 1, ...));
//       return advancedItem;
//   }
//
// (b) builds a brand-new transitional stack on every step and copies nothing from `input`.
// (a) returns the recipe's fixed output. `advance` is private and is installed through
// ProcessingRecipe#enforceNextResult, so there is no supported hook to merge components.
//
// The experiment below is what the GameTest `sequencedAssemblyDiscardsExtraComponents` runs.

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class Spike01 {
    /** @return true when the marker component survived one assembly step (it never does). */
    static boolean markerSurvives(Level level, ItemStack firstInput) {
        ItemStack marked = firstInput.copy();
        marked.set(DataComponents.CUSTOM_NAME, Component.literal("spike-marker"));

        var next = SequencedAssemblyRecipe.getRecipe(level, marked,
                AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class);
        if (next.isEmpty()) throw new IllegalStateException("no sequenced recipe starts with " + firstInput);

        ItemStack advanced = next.get().value().rollResults(level.random).getFirst();
        return advanced.has(DataComponents.CUSTOM_NAME);
    }
}

// Conclusion: a per-slot modular chassis would need either a mixin into the private `advance`
// (fragile across Create updates) or a custom recipe type + custom deployer handling. Neither is
// worth it for v0.1. Modular parts go to a custom Assembly Platform block in Milestone 5 (SDD 4.4).
