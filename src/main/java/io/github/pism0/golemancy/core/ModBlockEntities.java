package io.github.pism0.golemancy.core;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.integration.create.BindingAltarBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private ModBlockEntities() {
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Golemancy.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BindingAltarBlockEntity>> BINDING_ALTAR =
            BLOCK_ENTITIES.register("binding_altar", () -> {
                // The type must be handed to the block entity it creates, hence the holder indirection.
                BlockEntityType<?>[] self = new BlockEntityType<?>[1];
                BlockEntityType<BindingAltarBlockEntity> type = BlockEntityType.Builder.<BindingAltarBlockEntity>of(
                        (pos, state) -> new BindingAltarBlockEntity(self[0], pos, state), ModBlocks.BINDING_ALTAR.get()).build(null);
                self[0] = type;
                return type;
            });
}
