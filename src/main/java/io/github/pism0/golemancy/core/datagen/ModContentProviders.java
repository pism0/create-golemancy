package io.github.pism0.golemancy.core.datagen;

import net.minecraft.core.HolderLookup;
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
    }

    static void addClient(GatherDataEvent event, DataGenerator generator, PackOutput output, ExistingFileHelper files) {
    }
}
