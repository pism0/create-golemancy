package io.github.pism0.golemancy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.pism0.golemancy.entity.ConstructEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.List;

/** GeckoLib renderer for both frames. */
public final class ConstructRenderer extends GeoEntityRenderer<ConstructEntity> {
    /**
     * Optional component bones (SDD 10.4). They exist in both models and are hidden in v0.1; in
     * Milestone 5 this list is filtered by the golem's installed components instead.
     */
    public static final List<String> OPTIONAL_BONES =
            List.of("opt_shoulder_plates", "opt_back_banner", "opt_head_crest", "opt_weapon");

    public ConstructRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context, new ConstructModel());
        this.shadowRadius = shadowRadius;
        addRenderLayer(new CoreGlowLayer(this));
    }

    @Override
    public void preRender(PoseStack poseStack, ConstructEntity entity, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        // The baked model is shared by every construct of this frame, so visibility is applied per draw.
        for (String name : OPTIONAL_BONES) {
            model.getBone(name).ifPresent(bone -> bone.setHidden(true));
        }
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    protected float getDeathMaxRotation(ConstructEntity entity) {
        return 0f; // the death animation collapses the construct; no vanilla tip-over
    }
}
