package io.github.pism0.golemancy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.pism0.golemancy.entity.ConstructEntity;
import io.github.pism0.golemancy.magic.CoreDef;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Emissive pass for the core socket and eyes. Draws the model's {@code *_core_glowmask.png} at
 * full brightness, tinted with the core's colour and dimmed as energy runs low. A powered-down or
 * coreless construct does not glow.
 */
public final class CoreGlowLayer extends GeoRenderLayer<ConstructEntity> {
    public CoreGlowLayer(GeoRenderer<ConstructEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, ConstructEntity entity, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                       MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        CoreDef core = entity.getCoreDef();
        if (core == null || entity.isPoweredDown() || entity.isDeadOrDying()) return;

        float energy = entity.getEnergyFraction();
        // Low energy: a slow flicker (the "core instability" cue).
        float pulse = energy < 0.2f ? 0.55f + 0.45f * Mth.sin((entity.tickCount + partialTick) * 0.35f) : 1f;
        float brightness = Mth.clamp((0.45f + 0.55f * energy) * pulse, 0.15f, 1f);
        int rgb = core.color();
        int colour = FastColor.ARGB32.color(255,
                (int) (((rgb >> 16) & 0xFF) * brightness),
                (int) (((rgb >> 8) & 0xFF) * brightness),
                (int) ((rgb & 0xFF) * brightness));

        RenderType glow = RenderType.eyes(ConstructModel.glowmask(entity));
        getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, glow, bufferSource.getBuffer(glow),
                partialTick, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colour);
    }
}
