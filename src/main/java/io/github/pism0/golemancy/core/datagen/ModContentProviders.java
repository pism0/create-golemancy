package io.github.pism0.golemancy.core.datagen;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.core.ModBlocks;
import io.github.pism0.golemancy.core.ModEntities;
import io.github.pism0.golemancy.core.ModItems;
import io.github.pism0.golemancy.core.ModTags;
import io.github.pism0.golemancy.integration.create.BindingAltarBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Providers for content added after the foundation milestone (recipes, block states, loot,
 * tags). Kept separate from {@link DataGenerators} so the core wiring stays small.
 */
final class ModContentProviders {
    private ModContentProviders() {
    }

    static void addServer(GatherDataEvent event, DataGenerator generator, PackOutput output,
                          CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        boolean run = event.includeServer();

        generator.addProvider(run, new EntityTypeTagsProvider(output, lookup, Golemancy.MOD_ID, files) {
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                // Constructs do not breathe.
                tag(EntityTypeTags.CAN_BREATHE_UNDER_WATER).add(ModEntities.GUARDIAN.get(), ModEntities.BRUTE.get());
            }
        });

        BlockTagsProvider blockTags = new BlockTagsProvider(output, lookup, Golemancy.MOD_ID, files) {
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.BINDING_ALTAR.get());
            }
        };
        generator.addProvider(run, blockTags);

        generator.addProvider(run, new ItemTagsProvider(output, lookup, blockTags.contentsGetter(), Golemancy.MOD_ID, files) {
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                ModItems.CORE_ITEMS.values().forEach(core -> tag(ModTags.CORES).add(core.get()));
                ModItems.FRAME_ITEMS.values().forEach(frame -> tag(ModTags.FRAMES).add(frame.get()));
                // What the altar accepts as an inscription: Iron's spell scrolls. Optional so the tag
                // still loads if a future Iron's version renames the item.
                tag(ModTags.INSCRIPTIONS).addOptional(ExternalIds.SCROLL);
            }
        });

        generator.addProvider(run, new FluidTagsProvider(output, lookup, Golemancy.MOD_ID, files) {
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                // SDD 9.3: Create: Wizardry's mana as an altar catalyst. Data only; skipped if absent.
                ExternalIds.fluid(ExternalIds.WIZARDRY_MANA_FLUID); // validates the id during datagen
                tag(ModTags.BINDING_CATALYSTS).addOptional(ExternalIds.WIZARDRY_MANA_FLUID);
            }
        });

        generator.addProvider(run, new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)), lookup));

        ModRecipeProviders.add(run, generator, output, lookup);
    }

    static void addClient(GatherDataEvent event, DataGenerator generator, PackOutput output, ExistingFileHelper files) {
        generator.addProvider(event.includeClient(), new BlockStates(output, files));
    }

    private static final class BlockLoot extends BlockLootSubProvider {
        BlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            dropSelf(ModBlocks.BINDING_ALTAR.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(ModBlocks.BINDING_ALTAR.get());
        }
    }

    /**
     * The Binding Altar model: an andesite casing (y 0-10), a stone plinth inset by one pixel
     * (y 10-13) and a rune dish (y 13-16). The top texture switches while working.
     */
    private static final class BlockStates extends BlockStateProvider {
        BlockStates(PackOutput output, ExistingFileHelper files) {
            super(output, Golemancy.MOD_ID, files);
        }

        @Override
        protected void registerStatesAndModels() {
            ModelFile idle = altar("binding_altar", "block/binding_altar_top");
            ModelFile working = altar("binding_altar_working", "block/binding_altar_top_active");
            getVariantBuilder(ModBlocks.BINDING_ALTAR.get()).forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(state.getValue(BindingAltarBlock.WORKING) ? working : idle)
                    .build());
            simpleBlockItem(ModBlocks.BINDING_ALTAR.get(), idle);
        }

        private ModelFile altar(String name, String topTexture) {
            var model = models().withExistingParent(name, mcLoc("block/block"))
                    .texture("side", modLoc("block/binding_altar_side"))
                    .texture("top", modLoc(topTexture))
                    .texture("casing_top", modLoc("block/binding_altar_casing_top"))
                    .texture("bottom", modLoc("block/binding_altar_bottom"))
                    .texture("particle", modLoc("block/binding_altar_side"))
                    .renderType("minecraft:cutout");

            // Casing
            model.element().from(0, 0, 0).to(16, 10, 16).allFaces((dir, face) -> {
                switch (dir) {
                    case UP -> face.uvs(0, 0, 16, 16).texture("#casing_top");
                    case DOWN -> face.uvs(0, 0, 16, 16).texture("#bottom").cullface(Direction.DOWN);
                    default -> face.uvs(0, 6, 16, 16).texture("#side").cullface(dir);
                }
            }).end();
            // Plinth
            model.element().from(1, 10, 1).to(15, 13, 15).allFaces((dir, face) -> {
                switch (dir) {
                    case UP, DOWN -> face.uvs(1, 1, 15, 15).texture("#casing_top");
                    default -> face.uvs(1, 3, 15, 6).texture("#side");
                }
            }).end();
            // Rune dish
            model.element().from(0, 13, 0).to(16, 16, 16).allFaces((dir, face) -> {
                switch (dir) {
                    case UP -> face.uvs(0, 0, 16, 16).texture("#top").cullface(Direction.UP);
                    case DOWN -> face.uvs(0, 0, 16, 16).texture("#casing_top");
                    default -> face.uvs(0, 0, 16, 3).texture("#side").cullface(dir);
                }
            }).end();
            return model;
        }
    }
}
