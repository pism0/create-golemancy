package io.github.pism0.golemancy.client;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.core.ModDataComponents;
import io.github.pism0.golemancy.core.ModEntities;
import io.github.pism0.golemancy.core.ModItems;
import io.github.pism0.golemancy.data.GolemDefs;
import io.github.pism0.golemancy.data.GolemSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client-only registration: entity renderers and item tints. */
@EventBusSubscriber(modid = Golemancy.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    private static final int WHITE = 0xFFFFFFFF;
    private static final int NO_CORE = 0xFF3A3540;

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GUARDIAN.get(), context -> new ConstructRenderer(context, 0.7f));
        event.registerEntityRenderer(ModEntities.BRUTE.get(), context -> new ConstructRenderer(context, 1.0f));
    }

    /**
     * Dormant Golem and Construct Remains are layered sprites: layer 0 takes the material colour,
     * layer 1 the core colour, further layers are drawn as they are.
     */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(ClientSetup::specTint, ModItems.DORMANT_GOLEM.get(), ModItems.CONSTRUCT_REMAINS.get());
    }

    private static int specTint(ItemStack stack, int layer) {
        GolemSpec spec = stack.get(ModDataComponents.GOLEM_SPEC);
        RegistryAccess access = registryAccess();
        if (spec == null || access == null || layer > 1) return WHITE;
        if (layer == 0) {
            return GolemDefs.material(access, spec.material()).map(m -> 0xFF000000 | m.color()).orElse(WHITE);
        }
        if (!spec.hasCore()) return NO_CORE;
        return GolemDefs.core(access, spec.core()).map(c -> 0xFF000000 | c.color()).orElse(NO_CORE);
    }

    private static RegistryAccess registryAccess() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) return mc.level.registryAccess();
        return mc.getConnection() != null ? mc.getConnection().registryAccess() : null;
    }
}
