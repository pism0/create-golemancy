package io.github.pism0.golemancy.core.datagen;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.construction.FrameDef;
import io.github.pism0.golemancy.construction.MaterialDef;
import io.github.pism0.golemancy.core.ModItems;
import io.github.pism0.golemancy.core.ModRegistries;
import io.github.pism0.golemancy.core.ModTags;
import io.github.pism0.golemancy.data.BehaviorConfig;
import io.github.pism0.golemancy.magic.CoreDef;
import io.github.pism0.golemancy.magic.FuelDef;
import io.github.pism0.golemancy.magic.SpellProfile;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Generates the datapack registry content: materials, frames, cores, spell profiles and fuels
 * (SDD 15.1). These are the balance numbers; datapacks and modpacks can override any file.
 */
public final class ModDefinitionsProvider extends DatapackBuiltinEntriesProvider {

    // Tints must match tools/art/palettes.py (MATERIAL_TINT / CORE_TINT).
    private static final int STONE_TINT = 0x8D939C;
    private static final int CLAY_TINT = 0xB96D49;
    private static final int ARCANE_TINT = 0x9B72EA;
    private static final int FIRE_TINT = 0xF98A1E;
    private static final int FROST_TINT = 0x78D2E8;

    private static final ResourceKey<FuelDef> FUEL_MANA_BUCKET = fuelKey("create_wizardry/mana_bucket");

    public ModDefinitionsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, builder(), conditions -> {
            // SDD 9.2: every Create: Wizardry entry carries a mod_loaded condition.
            conditions.accept(FUEL_MANA_BUCKET, new ModLoadedCondition(ExternalIds.WIZARDRY));
        }, Set.of(Golemancy.MOD_ID));
    }

    private static RegistrySetBuilder builder() {
        return new RegistrySetBuilder()
                .add(ModRegistries.MATERIAL, ModDefinitionsProvider::materials)
                .add(ModRegistries.FRAME, ModDefinitionsProvider::frames)
                .add(ModRegistries.CORE, ModDefinitionsProvider::cores)
                .add(ModRegistries.SPELL_PROFILE, ModDefinitionsProvider::spellProfiles)
                .add(ModRegistries.FUEL, ModDefinitionsProvider::fuels);
    }

    private static void materials(BootstrapContext<MaterialDef> ctx) {
        // Stone: durable and strong, a poor conductor. Resists physical blows; its strikes slow.
        ctx.register(key(ModRegistries.MATERIAL, "stone"), new MaterialDef(
                1.2f, 1.5f, 0.9f, 0.2f, 0.8f,
                Optional.empty(),
                ModTags.absorbedBy("stone"), ModTags.resistedBy("stone"), 0.6f,
                Ingredient.of(Items.STONE, Items.COBBLESTONE, Items.SMOOTH_STONE),
                "stone", STONE_TINT, 0f, 40));
        // Clay: cheap and heavy. Absorbs Nature and poison magic and regenerates in water.
        ctx.register(key(ModRegistries.MATERIAL, "clay"), new MaterialDef(
                1.0f, 0.6f, 0.85f, 0.1f, 1.0f,
                Optional.of(ExternalIds.SCHOOL_NATURE),
                ModTags.absorbedBy("clay"), ModTags.resistedBy("clay"), 0.5f,
                Ingredient.of(Items.CLAY_BALL),
                "clay", CLAY_TINT, 1.0f, 0));
    }

    private static void frames(BootstrapContext<FrameDef> ctx) {
        // Guardian: upright and defensive. Holds a position.
        ctx.register(key(ModRegistries.FRAME, "guardian"), new FrameDef(
                Golemancy.id("guardian"), "guardian",
                60f, 8f, 7f, 20, 0.25f, 0.6f, 24f, 1.0f, 1, 1, BehaviorConfig.Profile.GUARDIAN));
        // Brute: wide, slow, hits hard. Follows its owner.
        ctx.register(key(ModRegistries.FRAME, "brute"), new FrameDef(
                Golemancy.id("brute"), "brute",
                90f, 4f, 12f, 30, 0.22f, 0.8f, 24f, 0.8f, 1, 1, BehaviorConfig.Profile.SOLDIER));
    }

    private static void cores(BootstrapContext<CoreDef> ctx) {
        // Arcane: neutral, long-lived, cannot cast (SDD 6.2).
        ctx.register(key(ModRegistries.CORE, "arcane"), new CoreDef(
                ModItems.core("arcane").get().builtInRegistryHolder(), Optional.empty(),
                2400, 0.25f, 1.0f, 1, 1, 0, ARCANE_TINT, Optional.empty(), 0, 0));
        ctx.register(key(ModRegistries.CORE, "fire"), new CoreDef(
                ModItems.core("fire").get().builtInRegistryHolder(), Optional.of(ExternalIds.SCHOOL_FIRE),
                2000, 0.3f, 1.0f, 2, 3, 1, FIRE_TINT, Optional.of(ModTags.resistedByCore("fire")), 0, 60));
        ctx.register(key(ModRegistries.CORE, "frost"), new CoreDef(
                ModItems.core("frost").get().builtInRegistryHolder(), Optional.of(ExternalIds.SCHOOL_ICE),
                2000, 0.3f, 1.0f, 2, 3, 1, FROST_TINT, Optional.of(ModTags.resistedByCore("frost")), 60, 0));
    }

    /** The v0.1 allow-list. Why these four: docs/DECISIONS.md D-011. */
    private static void spellProfiles(BootstrapContext<SpellProfile> ctx) {
        ctx.register(key(ModRegistries.SPELL_PROFILE, "firebolt"), new SpellProfile(
                ExternalIds.SPELL_FIREBOLT, SpellProfile.Targeting.ENEMY, 16f, 50, 20f, true, false, 3));
        ctx.register(key(ModRegistries.SPELL_PROFILE, "icicle"), new SpellProfile(
                ExternalIds.SPELL_ICICLE, SpellProfile.Targeting.ENEMY, 16f, 50, 20f, true, false, 3));
        ctx.register(key(ModRegistries.SPELL_PROFILE, "ray_of_frost"), new SpellProfile(
                ExternalIds.SPELL_RAY_OF_FROST, SpellProfile.Targeting.ENEMY, 12f, 160, 45f, true, false, 3));
        ctx.register(key(ModRegistries.SPELL_PROFILE, "shield"), new SpellProfile(
                ExternalIds.SPELL_SHIELD, SpellProfile.Targeting.SELF, 0f, 300, 60f, false, true, 3));
    }

    private static void fuels(BootstrapContext<FuelDef> ctx) {
        ctx.register(fuelKey("arcane_essence"),
                new FuelDef(Ingredient.of(ExternalIds.item(ExternalIds.ARCANE_ESSENCE)), 600));
        ctx.register(fuelKey("blaze_powder"), new FuelDef(Ingredient.of(Items.BLAZE_POWDER), 200));
        ctx.register(fuelKey("glowstone_dust"), new FuelDef(Ingredient.of(Items.GLOWSTONE_DUST), 120));
        ctx.register(fuelKey("redstone"), new FuelDef(Ingredient.of(Items.REDSTONE), 60));

        // Create: Wizardry (optional). Written only when the mod is present during datagen, so the
        // ids are validated against the real registry; the files carry a mod_loaded condition.
        if (ModList.get().isLoaded(ExternalIds.WIZARDRY)) {
            ctx.register(FUEL_MANA_BUCKET,
                    new FuelDef(Ingredient.of(ExternalIds.item(ExternalIds.WIZARDRY_MANA_BUCKET)), 2400));
        } else {
            throw new IllegalStateException("Run datagen with Create: Wizardry present (the default) so the "
                    + "conditional integration data can be generated and validated.");
        }
    }

    private static <T> ResourceKey<T> key(ResourceKey<Registry<T>> registry, String path) {
        return ResourceKey.create(registry, Golemancy.id(path));
    }

    private static ResourceKey<FuelDef> fuelKey(String path) {
        return ResourceKey.create(ModRegistries.FUEL, ResourceLocation.fromNamespaceAndPath(Golemancy.MOD_ID, path));
    }
}
