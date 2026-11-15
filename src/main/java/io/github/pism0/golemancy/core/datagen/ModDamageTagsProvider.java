package io.github.pism0.golemancy.core.datagen;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.core.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * Damage interaction tags (SDD 10.3). Iron's damage types are added as optional entries so the
 * tags still load if a future Iron's version renames one.
 */
public final class ModDamageTagsProvider extends DamageTypeTagsProvider {
    public ModDamageTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, lookup, Golemancy.MOD_ID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Stone absorbs nothing; it shrugs off physical blows.
        tag(ModTags.absorbedBy("stone"));
        tag(ModTags.resistedBy("stone")).add(
                DamageTypes.MOB_ATTACK, DamageTypes.MOB_ATTACK_NO_AGGRO, DamageTypes.PLAYER_ATTACK,
                DamageTypes.ARROW, DamageTypes.TRIDENT, DamageTypes.MOB_PROJECTILE, DamageTypes.THROWN,
                DamageTypes.STING, DamageTypes.CACTUS, DamageTypes.SWEET_BERRY_BUSH);

        // Clay absorbs Nature and poison magic (the v1.0 "acid" idea mapped onto real damage types).
        // Vanilla poison is the generic `minecraft:magic` type, which is deliberately not included.
        tag(ModTags.absorbedBy("clay"))
                .addOptional(ExternalIds.DAMAGE_NATURE_MAGIC)
                .addOptional(ExternalIds.DAMAGE_POISON_CLOUD);
        // Soft clay deforms instead of cracking.
        tag(ModTags.resistedBy("clay")).add(DamageTypes.FALL, DamageTypes.FLY_INTO_WALL, DamageTypes.CRAMMING);

        tag(ModTags.resistedByCore("fire"))
                .add(DamageTypes.IN_FIRE, DamageTypes.ON_FIRE, DamageTypes.LAVA, DamageTypes.HOT_FLOOR,
                        DamageTypes.CAMPFIRE, DamageTypes.FIREBALL, DamageTypes.UNATTRIBUTED_FIREBALL)
                .addOptional(ExternalIds.DAMAGE_FIRE_MAGIC)
                .addOptional(ExternalIds.DAMAGE_FIRE_FIELD);
        tag(ModTags.resistedByCore("frost"))
                .add(DamageTypes.FREEZE)
                .addOptional(ExternalIds.DAMAGE_ICE_MAGIC);
    }
}
