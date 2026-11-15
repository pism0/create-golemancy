package io.github.pism0.golemancy.magic;

import io.github.pism0.golemancy.data.InstalledSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Casts spells for a golem (SDD 3.2). Golem AI only ever sees this interface; the default
 * implementation is the Iron's Spells adapter in {@code integration.irons_spellbooks}.
 */
public interface ISpellCaster {
    /** Whether {@code spellId} exists and is of a kind this caster can drive from an entity. */
    boolean canCast(ResourceLocation spellId);

    /**
     * Casts {@code spell} from {@code caster}, which must already be facing {@code target} (or
     * anywhere, for self-targeted spells). Server-side only.
     *
     * @param powerMultiplier scales spell power (material conductivity × core integrity)
     * @return true if the spell was cast
     */
    boolean cast(Mob caster, InstalledSpell spell, @Nullable LivingEntity target, float powerMultiplier);

    /** The school a spell belongs to, if the spell exists. */
    Optional<ResourceLocation> schoolOf(ResourceLocation spellId);

    /** The spell's own mana cost at a level (converted to construct energy by the core). */
    float manaCost(ResourceLocation spellId, int level);

    /** Reads the spell stored on an inscription item such as a scroll, if any. */
    Optional<InstalledSpell> readInscription(ItemStack stack);

    /** A caster that knows no spells. Used until an integration registers a real one. */
    ISpellCaster NONE = new ISpellCaster() {
        @Override
        public boolean canCast(ResourceLocation spellId) {
            return false;
        }

        @Override
        public boolean cast(Mob caster, InstalledSpell spell, @Nullable LivingEntity target, float powerMultiplier) {
            return false;
        }

        @Override
        public Optional<ResourceLocation> schoolOf(ResourceLocation spellId) {
            return Optional.empty();
        }

        @Override
        public float manaCost(ResourceLocation spellId, int level) {
            return 0f;
        }

        @Override
        public Optional<InstalledSpell> readInscription(ItemStack stack) {
            return Optional.empty();
        }
    };
}
