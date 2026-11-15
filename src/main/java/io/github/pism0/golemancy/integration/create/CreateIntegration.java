package io.github.pism0.golemancy.integration.create;

import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.core.GolemancyConfig;
import io.github.pism0.golemancy.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Everything that touches Create's code outside the altar classes themselves (SDD 3.3). Create has
 * no stable addon API, so a Create update means a porting pass over this package.
 */
public final class CreateIntegration {
    private CreateIntegration() {
    }

    private static final DeferredRegister<ArmInteractionPointType> ARM_POINTS =
            DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, Golemancy.MOD_ID);

    /** Lets mechanical arms deposit into and take from the Binding Altar directly. */
    public static final DeferredHolder<ArmInteractionPointType, ArmInteractionPointType> ALTAR_ARM_POINT =
            ARM_POINTS.register("binding_altar", () -> new ArmInteractionPointType() {
                @Override
                public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
                    return state.is(ModBlocks.BINDING_ALTAR.get());
                }

                @Override
                public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
                    // The default point uses the block's item handler capability, which routes inputs
                    // and exposes only the finished golem for extraction.
                    return new ArmInteractionPoint(this, level, pos, state);
                }
            });

    public static void register(IEventBus modBus) {
        ARM_POINTS.register(modBus);
        modBus.addListener(CreateIntegration::onCommonSetup);
        modBus.addListener(CreateIntegration::onRegisterCapabilities);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        // Stress impact is read through the supplier, so the server config can change it.
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(ModBlocks.BINDING_ALTAR.get(),
                () -> GolemancyConfig.SPEC.isLoaded() ? GolemancyConfig.ALTAR_STRESS_IMPACT.get() : 4.0));
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        BindingAltarBlockEntity.registerCapabilities(event);
    }
}
