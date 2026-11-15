package io.github.pism0.golemancy.client;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.construction.MaterialDef;
import io.github.pism0.golemancy.entity.ConstructEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Model lookup for constructs. The entity type picks the model and animations
 * ({@code geo/<type>.geo.json}, {@code animations/<type>.animation.json}); the material picks
 * the texture ({@code textures/entity/<type>_<material texture>.png}).
 */
public final class ConstructModel extends GeoModel<ConstructEntity> {

    static String modelName(ConstructEntity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
    }

    @Override
    public ResourceLocation getModelResource(ConstructEntity entity) {
        return Golemancy.id("geo/" + modelName(entity) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ConstructEntity entity) {
        MaterialDef material = entity.getMaterialDef();
        String texture = material == null ? "stone" : material.texture();
        return Golemancy.id("textures/entity/" + modelName(entity) + "_" + texture + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ConstructEntity entity) {
        return Golemancy.id("animations/" + modelName(entity) + ".animation.json");
    }

    /** The emissive mask shared by every material of a model. */
    public static ResourceLocation glowmask(ConstructEntity entity) {
        return Golemancy.id("textures/entity/" + modelName(entity) + "_core_glowmask.png");
    }

    @Override
    public void setCustomAnimations(ConstructEntity entity, long instanceId, AnimationState<ConstructEntity> state) {
        // Head tracking, layered on top of the keyframed animation. Not while slumped or dead.
        if (entity.getState() != ConstructEntity.State.ACTIVE || entity.isDeadOrDying()) return;
        GeoBone head = getAnimationProcessor().getBone("head");
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head == null || data == null) return;
        head.setRotX(head.getRotX() + data.headPitch() * Mth.DEG_TO_RAD);
        head.setRotY(head.getRotY() + data.netHeadYaw() * Mth.DEG_TO_RAD);
    }
}
