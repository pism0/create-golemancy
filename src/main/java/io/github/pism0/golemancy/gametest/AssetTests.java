package io.github.pism0.golemancy.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.core.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Asset checks that can run without a display. The build machine has no OpenGL, so the client
 * cannot be started there; these tests load the shipped models and animations with GeckoLib's own
 * parser and check that every asset the code refers to by name really exists in the jar.
 */
@GameTestHolder(Golemancy.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AssetTests {
    private AssetTests() {
    }

    private static final List<String> MODELS = List.of("guardian", "brute");
    private static final List<String> ANIMATIONS = List.of("idle", "walk", "attack", "cast", "hurt", "activate", "dormant", "death");
    private static final List<String> BONES = List.of("root", "body", "core", "head", "arm_left", "arm_right", "leg_left", "leg_right",
            "opt_shoulder_plates", "opt_back_banner", "opt_head_crest", "opt_weapon");

    private static JsonObject json(String assetPath) throws Exception {
        try (InputStream in = Golemancy.class.getResourceAsStream("/assets/golemancy/" + assetPath)) {
            if (in == null) throw new IllegalStateException("missing asset: " + assetPath);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    static boolean exists(String assetPath) {
        return Golemancy.class.getResource("/assets/golemancy/" + assetPath) != null;
    }

    /** The .geo.json and .animation.json files parse with GeckoLib and contain every bone and animation the code uses. */
    @GameTest(template = GolemTestUtil.PLATFORM)
    public static void geckoLibAssetsParse(GameTestHelper helper) throws Exception {
        for (String name : MODELS) {
            Model raw = KeyFramesAdapter.GEO_GSON.fromJson(json("geo/" + name + ".geo.json"), Model.class);
            BakedGeoModel baked = BakedModelFactory.getForNamespace(Golemancy.MOD_ID).constructGeoModel(GeometryTree.fromModel(raw));
            for (String bone : BONES) {
                helper.assertTrue(baked.getBone(bone).isPresent(), name + " model is missing bone '" + bone + "'");
            }
            BakedAnimations animations = KeyFramesAdapter.GEO_GSON.fromJson(
                    GsonHelper.getAsJsonObject(json("animations/" + name + ".animation.json"), "animations"), BakedAnimations.class);
            for (String animation : ANIMATIONS) {
                var anim = animations.getAnimation(animation);
                helper.assertTrue(anim != null, name + " is missing animation '" + animation + "'");
                for (var boneAnimation : anim.boneAnimations()) {
                    helper.assertTrue(baked.getBone(boneAnimation.boneName()).isPresent(),
                            name + "." + animation + " animates unknown bone '" + boneAnimation.boneName() + "'");
                }
            }
            for (String material : ModItems.MATERIALS) {
                helper.assertTrue(exists("textures/entity/" + name + "_" + material + ".png"),
                        "missing entity texture " + name + "_" + material);
            }
            helper.assertTrue(exists("textures/entity/" + name + "_core_glowmask.png"), "missing glowmask for " + name);
        }
        helper.succeed();
    }

    /** Every registered item has a model, and every texture a model names exists. */
    @GameTest(template = GolemTestUtil.PLATFORM)
    public static void everyItemHasModelAndTextures(GameTestHelper helper) throws Exception {
        for (var holder : ModItems.all()) {
            String path = holder.getId().getPath();
            helper.assertTrue(exists("models/item/" + path + ".json"), "item '" + path + "' has no model");
            JsonObject model = json("models/item/" + path + ".json");
            if (model.has("textures")) {
                for (var entry : model.getAsJsonObject("textures").entrySet()) {
                    String texture = entry.getValue().getAsString();
                    if (!texture.startsWith(Golemancy.MOD_ID + ":")) continue;
                    String file = "textures/" + texture.substring(Golemancy.MOD_ID.length() + 1) + ".png";
                    helper.assertTrue(exists(file), "item '" + path + "' uses missing texture " + file);
                }
            }
            helper.assertTrue(langHas("item.golemancy." + path.replace('/', '.'))
                    || langHas("block.golemancy." + path.replace('/', '.')), "item '" + path + "' has no name in en_us.json");
        }
        helper.succeed();
    }

    private static JsonObject lang;

    static boolean langHas(String key) throws Exception {
        if (lang == null) lang = json("lang/en_us.json");
        return lang.has(key);
    }
}
