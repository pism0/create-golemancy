package io.github.pism0.golemancy.core;

import io.github.pism0.golemancy.Golemancy;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity types: one per frame (SDD 10). The material only changes texture and stats. */
public final class ModEntities {
    private ModEntities() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Golemancy.MOD_ID);
}
