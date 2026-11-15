package io.github.pism0.golemancy;

import io.github.pism0.golemancy.core.GolemancyConfig;
import io.github.pism0.golemancy.core.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Create: Golemancy. Golems manufactured on Create lines and animated with Iron's Spells magic. */
@Mod(Golemancy.MOD_ID)
public final class Golemancy {
    public static final String MOD_ID = "golemancy";
    public static final Logger LOGGER = LoggerFactory.getLogger("Golemancy");

    public Golemancy(IEventBus modBus, ModContainer container) {
        ModRegistries.register(modBus);
        io.github.pism0.golemancy.integration.create.CreateIntegration.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, GolemancyConfig.SPEC);
        // Iron's Spells is a required dependency, so its adapter is always registered.
        io.github.pism0.golemancy.integration.irons_spellbooks.IronsIntegration.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
