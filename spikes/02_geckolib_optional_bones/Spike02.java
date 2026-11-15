// THROWAWAY SPIKE - not compiled into the mod. See spikes/README.md.
//
// Question: can every optional component be a bone in the frame model that is shown or hidden
// per entity from the GolemSpec, and what does it cost?
//
// GeckoLib 4.9.3 facts (software.bernie.geckolib):
//  - A BakedGeoModel is baked once per model file and SHARED by every entity using it.
//  - cache.object.GeoBone has setHidden(boolean) (also hides children) and setChildrenHidden(boolean).
//    Hidden bones are skipped in GeoRenderer#renderRecursively, so a hidden bone costs one branch.
//  - Because the baked model is shared, visibility is NOT per-entity state: it must be (re)applied
//    for the entity being drawn, in GeoEntityRenderer#preRender, on every render pass.
//
// So the pattern is: the renderer walks a fixed list of optional bone names and sets
// hidden = !spec.hasComponent(name) before each entity is drawn. Cost is O(optional bones) map
// lookups per rendered golem per frame; with <= 16 optional bones that is negligible next to
// the animation processor.

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.Set;

abstract class Spike02Renderer /* extends GeoEntityRenderer<ConstructEntity> */ {
    static final String[] OPTIONAL_BONES = {"opt_shoulder_plates", "opt_back_banner", "opt_head_crest", "opt_weapon"};

    abstract Set<String> visibleOptionalBones(/* ConstructEntity entity */);

    public void preRender(PoseStack pose, Object animatable, BakedGeoModel model, MultiBufferSource buffers,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int light, int overlay, int colour) {
        Set<String> visible = visibleOptionalBones();
        for (String name : OPTIONAL_BONES) {
            model.getBone(name).ifPresent(bone -> bone.setHidden(!visible.contains(name)));
        }
    }
}

// Conclusion: correct and cheap. v0.1 ships the optional bones in both models, always hidden
// (no components exist yet); Milestone 5 only needs to feed the set from GolemSpec.
// Gotcha recorded for M5: glow layers re-render the same baked model, so visibility set in
// preRender carries over to the glowmask pass automatically (isReRender = true skips nothing).
