package io.github.pism0.golemancy.integration.create.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.core.ModBlockEntities;
import io.github.pism0.golemancy.integration.create.BindingAltarBlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client side of the Create integration: the altar's renderer, its Flywheel visual and Ponder. */
@EventBusSubscriber(modid = Golemancy.MOD_ID, value = Dist.CLIENT)
public final class CreateClientIntegration {
    private CreateClientIntegration() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BINDING_ALTAR.get(), BindingAltarRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // With Flywheel active the rotating half shaft is an instanced visual (as for Create's own
            // machines); the block entity renderer still runs to draw the items on the altar.
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.BINDING_ALTAR.get())
                    .factory((context, altar, partialTick) -> new OrientedRotatingVisual<BindingAltarBlockEntity>(
                            context, altar, partialTick, Direction.SOUTH, Direction.DOWN,
                            Models.partial(AllPartialModels.SHAFT_HALF)))
                    .skipVanillaRender(altar -> false)
                    .apply();
            GolemancyPonder.register();
        });
    }
}
