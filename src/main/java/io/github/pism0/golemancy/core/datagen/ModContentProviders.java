package io.github.pism0.golemancy.core.datagen;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.core.ModEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Providers for content added after the foundation milestone (recipes, block states, loot,
 * item tags). Kept separate from {@link DataGenerators} so the core wiring stays small.
 */
final class ModContentProviders {
    private ModContentProviders() {
    }

    static void addServer(GatherDataEvent event, DataGenerator generator, PackOutput output,
                          CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        generator.addProvider(event.includeServer(), new EntityTypeTagsProvider(output, lookup, Golemancy.MOD_ID, files) {
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                // Constructs do not breathe.
                tag(EntityTypeTags.CAN_BREATHE_UNDER_WATER).add(ModEntities.GUARDIAN.get(), ModEntities.BRUTE.get());
            }
        });
    }

    static void addClient(GatherDataEvent event, DataGenerator generator, PackOutput output, ExistingFileHelper files) {
    }
}
