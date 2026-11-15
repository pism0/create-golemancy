package io.github.pism0.golemancy.core;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.entity.ConstructEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity types: one per frame (SDD 10). The material only changes texture and stats. */
public final class ModEntities {
    private ModEntities() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Golemancy.MOD_ID);

    /** Guardian frame: upright, 34 px tall model. */
    public static final DeferredHolder<EntityType<?>, EntityType<ConstructEntity>> GUARDIAN = ENTITIES.register("guardian",
            () -> EntityType.Builder.<ConstructEntity>of(ConstructEntity::new, MobCategory.MISC)
                    .sized(1.3f, 2.2f).eyeHeight(1.9f).clientTrackingRange(10).build("guardian"));

    /** Brute frame: wide and low, 27 px tall model. */
    public static final DeferredHolder<EntityType<?>, EntityType<ConstructEntity>> BRUTE = ENTITIES.register("brute",
            () -> EntityType.Builder.<ConstructEntity>of(ConstructEntity::new, MobCategory.MISC)
                    .sized(1.9f, 1.8f).eyeHeight(1.45f).clientTrackingRange(10).build("brute"));
}
