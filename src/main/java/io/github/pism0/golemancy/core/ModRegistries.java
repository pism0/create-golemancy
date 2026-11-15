package io.github.pism0.golemancy.core;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.construction.FrameDef;
import io.github.pism0.golemancy.construction.MaterialDef;
import io.github.pism0.golemancy.magic.CoreDef;
import io.github.pism0.golemancy.magic.FuelDef;
import io.github.pism0.golemancy.magic.SpellProfile;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Entry point for everything Golemancy registers, plus the keys of its datapack registries
 * (SDD 15.1). The datapack registries are synced to clients for tooltips, the UI and JEI.
 */
public final class ModRegistries {
    private ModRegistries() {
    }

    /** {@code data/<namespace>/golemancy/material/} */
    public static final ResourceKey<Registry<MaterialDef>> MATERIAL = ResourceKey.createRegistryKey(Golemancy.id("material"));
    /** {@code data/<namespace>/golemancy/frame/} */
    public static final ResourceKey<Registry<FrameDef>> FRAME = ResourceKey.createRegistryKey(Golemancy.id("frame"));
    /** {@code data/<namespace>/golemancy/core/} */
    public static final ResourceKey<Registry<CoreDef>> CORE = ResourceKey.createRegistryKey(Golemancy.id("core"));
    /** {@code data/<namespace>/golemancy/spell_profile/} */
    public static final ResourceKey<Registry<SpellProfile>> SPELL_PROFILE = ResourceKey.createRegistryKey(Golemancy.id("spell_profile"));
    /** {@code data/<namespace>/golemancy/fuel/} */
    public static final ResourceKey<Registry<FuelDef>> FUEL = ResourceKey.createRegistryKey(Golemancy.id("fuel"));

    public static void register(IEventBus modBus) {
        ModDataComponents.COMPONENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        modBus.addListener(ModRegistries::onNewDatapackRegistries);
    }

    private static void onNewDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(MATERIAL, MaterialDef.CODEC, MaterialDef.CODEC);
        event.dataPackRegistry(FRAME, FrameDef.CODEC, FrameDef.CODEC);
        event.dataPackRegistry(CORE, CoreDef.CODEC, CoreDef.CODEC);
        event.dataPackRegistry(SPELL_PROFILE, SpellProfile.CODEC, SpellProfile.CODEC);
        event.dataPackRegistry(FUEL, FuelDef.CODEC, FuelDef.CODEC);
    }
}
