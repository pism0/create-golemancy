package io.github.pism0.golemancy.integration.create.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import io.github.pism0.golemancy.integration.create.BindingAltarBlock;
import io.github.pism0.golemancy.integration.create.BindingAltarBlockEntity;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the half shaft under the altar (when Flywheel is not doing it) and the items resting on
 * top: the frame or remains in the middle, the core and inscription beside it, the finished golem
 * hovering.
 */
public class BindingAltarRenderer extends KineticBlockEntityRenderer<BindingAltarBlockEntity> {
    public BindingAltarRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(BindingAltarBlockEntity altar, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        // The items sit in the air above the altar; the light passed in is the altar's own (inside a
        // full block, so near zero), which drew everything almost black. Use the light above it.
        int itemLight = altar.getLevel() == null ? light
                : net.minecraft.client.renderer.LevelRenderer.getLightColor(altar.getLevel(), altar.getBlockPos().above());
        renderItems(altar, partialTicks, ms, buffer, itemLight, overlay);
        // Returns early when Flywheel visualisation is active; the visual draws the shaft then.
        super.renderSafe(altar, partialTicks, ms, buffer, light, overlay);
    }

    private void renderItems(BindingAltarBlockEntity altar, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                             int light, int overlay) {
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        float time = AnimationTickHolder.getRenderTime(altar.getLevel());
        boolean working = altar.getBlockState().getValue(BindingAltarBlock.WORKING);

        ItemStack output = altar.getOutput();
        if (!output.isEmpty()) {
            drawItem(items, altar, output, ms, buffer, light, overlay, 0f, 1.45f + (float) Math.sin(time * 0.08f) * 0.04f, 0f,
                    0.9f, time * 2f);
            return;
        }
        ItemStack body = altar.getInput(BindingAltarBlockEntity.SLOT_BODY);
        if (!body.isEmpty()) {
            float lift = working ? 0.12f * altar.getProgressFraction() : 0f;
            drawItem(items, altar, body, ms, buffer, light, overlay, 0f, 1.3f + lift, 0f, 0.8f, working ? time * 4f : 0f);
        }
        ItemStack core = altar.getInput(BindingAltarBlockEntity.SLOT_CORE);
        if (!core.isEmpty()) {
            drawItem(items, altar, core, ms, buffer, light, overlay, -0.3f, 1.12f, 0.3f, 0.4f, working ? -time * 6f : 45f);
        }
        ItemStack inscription = altar.getInput(BindingAltarBlockEntity.SLOT_INSCRIPTION);
        if (!inscription.isEmpty()) {
            drawItem(items, altar, inscription, ms, buffer, light, overlay, 0.3f, 1.12f, -0.3f, 0.4f, working ? time * 6f : -45f);
        }
    }

    private static void drawItem(ItemRenderer items, BindingAltarBlockEntity altar, ItemStack stack, PoseStack ms,
                                 MultiBufferSource buffer, int light, int overlay, float dx, float y, float dz,
                                 float scale, float yawDegrees) {
        ms.pushPose();
        ms.translate(0.5f + dx, y, 0.5f + dz);
        ms.mulPose(Axis.YP.rotationDegrees(yawDegrees));
        ms.scale(scale, scale, scale);
        items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, ms, buffer, altar.getLevel(), 0);
        ms.popPose();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(BindingAltarBlockEntity altar, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.DOWN);
    }
}
