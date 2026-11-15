package io.github.pism0.golemancy.core.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The one place where ids owned by other mods are written down. Datagen only: runtime code never
 * references these (SDD 6.3, 7.2). Each id was checked against the pinned jars and is listed in
 * docs/VERSIONS.md; {@link #item} and {@link #fluid} also fail datagen if an id is not registered,
 * so a dependency update that renames something is caught when data is regenerated.
 */
public final class ExternalIds {
    private ExternalIds() {
    }

    public static final String IRONS = "irons_spellbooks";
    public static final String CREATE = "create";
    public static final String WIZARDRY = "create_wizardry";

    // --- Iron's Spells 'n Spellbooks items ----------------------------------------------------
    public static final ResourceLocation ARCANE_ESSENCE = irons("arcane_essence");
    public static final ResourceLocation SCROLL = irons("scroll");

    // --- Iron's spells on the v0.1 allow-list -------------------------------------------------
    public static final ResourceLocation SPELL_FIREBOLT = irons("firebolt");
    public static final ResourceLocation SPELL_ICICLE = irons("icicle");
    public static final ResourceLocation SPELL_RAY_OF_FROST = irons("ray_of_frost");
    public static final ResourceLocation SPELL_SHIELD = irons("shield");

    // --- Iron's schools ---------------------------------------------------------------------------
    public static final ResourceLocation SCHOOL_FIRE = irons("fire");
    public static final ResourceLocation SCHOOL_ICE = irons("ice");
    public static final ResourceLocation SCHOOL_NATURE = irons("nature");

    // --- Iron's damage types ----------------------------------------------------------------------
    public static final ResourceLocation DAMAGE_FIRE_MAGIC = irons("fire_magic");
    public static final ResourceLocation DAMAGE_FIRE_FIELD = irons("fire_field");
    public static final ResourceLocation DAMAGE_ICE_MAGIC = irons("ice_magic");
    public static final ResourceLocation DAMAGE_NATURE_MAGIC = irons("nature_magic");
    public static final ResourceLocation DAMAGE_POISON_CLOUD = irons("poison_cloud");

    // --- Create items -----------------------------------------------------------------------------
    public static final ResourceLocation ANDESITE_ALLOY = create("andesite_alloy");
    public static final ResourceLocation ANDESITE_CASING = create("andesite_casing");
    public static final ResourceLocation SHAFT = create("shaft");
    public static final ResourceLocation COPPER_SHEET = create("copper_sheet");

    // --- Create: Wizardry (optional; only ever used behind a mod_loaded condition) --------------
    public static final ResourceLocation WIZARDRY_MANA_FLUID = wizardry("mana");
    public static final ResourceLocation WIZARDRY_MANA_BUCKET = wizardry("mana_bucket");

    public static ResourceLocation irons(String path) {
        return ResourceLocation.fromNamespaceAndPath(IRONS, path);
    }

    public static ResourceLocation create(String path) {
        return ResourceLocation.fromNamespaceAndPath(CREATE, path);
    }

    public static ResourceLocation wizardry(String path) {
        return ResourceLocation.fromNamespaceAndPath(WIZARDRY, path);
    }

    /** Looks an item up in the live registry and fails datagen if it does not exist. */
    public static Item item(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            throw new IllegalStateException("Datagen references item '" + id + "', which is not registered. "
                    + "Check the id against the pinned dependency version (docs/VERSIONS.md).");
        }
        return item;
    }

    /** Looks a fluid up in the live registry and fails datagen if it does not exist. */
    public static Fluid fluid(ResourceLocation id) {
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        if (fluid == Fluids.EMPTY) {
            throw new IllegalStateException("Datagen references fluid '" + id + "', which is not registered. "
                    + "Check the id against the pinned dependency version (docs/VERSIONS.md).");
        }
        return fluid;
    }
}
