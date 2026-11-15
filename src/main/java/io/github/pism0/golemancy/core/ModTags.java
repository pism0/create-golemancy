package io.github.pism0.golemancy.core;

import io.github.pism0.golemancy.Golemancy;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;

/** Tag keys. Damage interactions are tag-driven, never hardcoded (SDD 10.3). */
public final class ModTags {
    private ModTags() {
    }

    /** {@code golemancy:absorbed_by/<material>}: damage types that heal instead of harm. */
    public static TagKey<DamageType> absorbedBy(String material) {
        return TagKey.create(Registries.DAMAGE_TYPE, Golemancy.id("absorbed_by/" + material));
    }

    /** {@code golemancy:resisted_by/<material>}: damage types that are reduced. */
    public static TagKey<DamageType> resistedBy(String material) {
        return TagKey.create(Registries.DAMAGE_TYPE, Golemancy.id("resisted_by/" + material));
    }

    /** {@code golemancy:resisted_by_core/<core>}: damage types a core lets its golem resist. */
    public static TagKey<DamageType> resistedByCore(String core) {
        return TagKey.create(Registries.DAMAGE_TYPE, Golemancy.id("resisted_by_core/" + core));
    }

    public static final TagKey<Item> CORES = item("cores");
    public static final TagKey<Item> FRAMES = item("frames");
    /** Items the Binding Altar accepts in its inscription slot (Iron's scrolls). */
    public static final TagKey<Item> INSCRIPTIONS = item("inscriptions");

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, Golemancy.id(path));
    }
}
