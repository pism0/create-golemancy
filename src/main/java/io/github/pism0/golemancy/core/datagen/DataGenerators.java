package io.github.pism0.golemancy.core.datagen;

import io.github.pism0.golemancy.Golemancy;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Datagen entry point: {@code ./gradlew runData}. All JSON content is generated here and the
 * output in {@code src/generated/resources} is committed.
 *
 * <p>Run it with the default dependency set (Create: Wizardry present) so the conditional
 * integration data can be validated against real ids.
 */
@EventBusSubscriber(modid = Golemancy.MOD_ID)
public final class DataGenerators {
    private DataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExistingFileHelper files = event.getExistingFileHelper();

        // Server data
        ModDefinitionsProvider definitions = new ModDefinitionsProvider(output, lookup);
        generator.addProvider(event.includeServer(), definitions);
        generator.addProvider(event.includeServer(), new ModDamageTagsProvider(output, lookup, files));
        ModContentProviders.addServer(event, generator, output, definitions.getRegistryProvider(), files);

        // Client assets
        generator.addProvider(event.includeClient(), new ModLangProvider(output));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, files));
        ModContentProviders.addClient(event, generator, output, files);
    }
}
