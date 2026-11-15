package io.github.pism0.golemancy.core;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.data.ConstructState;
import io.github.pism0.golemancy.data.GolemSpec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components (1.21 replacement for item NBT). Each has a Codec and a StreamCodec. */
public final class ModDataComponents {
    private ModDataComponents() {
    }

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Golemancy.MOD_ID);

    /** {@code golemancy:golem_spec}: the golem design carried by Dormant Golem and Construct Remains items. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GolemSpec>> GOLEM_SPEC =
            COMPONENTS.registerComponentType("golem_spec", builder -> builder
                    .persistent(GolemSpec.CODEC)
                    .networkSynchronized(GolemSpec.STREAM_CODEC)
                    .cacheEncoding());

    /** {@code golemancy:construct_state}: energy, health and name of a packed-up golem. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ConstructState>> CONSTRUCT_STATE =
            COMPONENTS.registerComponentType("construct_state", builder -> builder
                    .persistent(ConstructState.CODEC)
                    .networkSynchronized(ConstructState.STREAM_CODEC));

    /** {@code golemancy:linked_golem}: the golem a Control Wand last handled. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.UUID>> LINKED_GOLEM =
            COMPONENTS.registerComponentType("linked_golem", builder -> builder
                    .persistent(net.minecraft.core.UUIDUtil.CODEC)
                    .networkSynchronized(net.minecraft.core.UUIDUtil.STREAM_CODEC));
}
