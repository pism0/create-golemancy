package io.github.pism0.golemancy.integration.irons_spellbooks;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.data.InstalledSpell;
import io.github.pism0.golemancy.magic.ISpellCaster;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * {@link ISpellCaster} backed by Iron's Spells 'n Spellbooks. Uses classes from Iron's API package
 * only (see KNOWN_INTERNALS.md). A golem casts the way Iron's own mobs end up casting:
 * {@code AbstractSpell.onCast(level, spellLevel, entity, CastSource.MOB, mobMagicData)}.
 *
 * <p>Only {@link CastType#INSTANT} spells are supported: they need no per-tick cast state, and
 * they are aimed purely by where the caster is looking (docs/DECISIONS.md D-007, D-011).
 */
public final class IronsSpellCaster implements ISpellCaster {
    private static final ResourceLocation POWER_MODIFIER = Golemancy.id("spec_spell_power");

    @Nullable
    private static AbstractSpell find(ResourceLocation id) {
        // Registry lookup returns null for unknown ids (SpellRegistry.getSpell would return the "none" spell).
        return SpellRegistry.REGISTRY.get(id);
    }

    @Override
    public boolean canCast(ResourceLocation spellId) {
        AbstractSpell spell = find(spellId);
        return spell != null && spell.isEnabled() && spell.getCastType() == CastType.INSTANT;
    }

    @Override
    public boolean cast(Mob caster, InstalledSpell installed, @Nullable LivingEntity target, float powerMultiplier) {
        AbstractSpell spell = find(installed.spell());
        if (spell == null || !canCast(installed.spell()) || caster.level().isClientSide) return false;
        int level = Math.max(spell.getMinLevel(), Math.min(installed.level(), spell.getMaxLevel()));
        try {
            MagicData magicData = new MagicData(true);
            if (!spell.checkPreCastConditions(caster.level(), level, caster, magicData)) return false;
            applySpellPower(caster, powerMultiplier);
            spell.onCast(caster.level(), level, caster, CastSource.MOB, magicData);
            spell.onServerCastComplete(caster.level(), level, caster, magicData, false);
            return true;
        } catch (Throwable t) {
            // A misbehaving spell must never take the server down (SDD 19).
            Golemancy.LOGGER.error("Spell '{}' failed when cast by {}", installed.spell(), caster, t);
            return false;
        }
    }

    /** Material conductivity and core integrity scale Iron's spell power attribute on the golem. */
    private static void applySpellPower(Mob caster, float multiplier) {
        AttributeInstance power = caster.getAttribute(AttributeRegistry.SPELL_POWER);
        if (power == null) return;
        power.addOrUpdateTransientModifier(new AttributeModifier(POWER_MODIFIER, multiplier - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /**
     * The spell's school. Iron's builds its per-spell configuration lazily, the first time a player
     * joins (or on /reload); before that every spell reports the global default school. An
     * automated line can run before anyone has logged in (force-loaded chunks after a restart),
     * so when the configured school is just that global default, the spell's own built-in school
     * is used instead. Server-configured school overrides still win once the config is loaded.
     */
    @Override
    public Optional<ResourceLocation> schoolOf(ResourceLocation spellId) {
        AbstractSpell spell = find(spellId);
        if (spell == null) return Optional.empty();
        ResourceLocation configured = spell.getSchoolType().getId();
        ResourceLocation builtIn = spell.getDefaultConfig().schoolResource;
        ResourceLocation globalDefault = SpellConfigParameter.SCHOOL.defaultValue().get().getId();
        if (builtIn != null && configured.equals(globalDefault)) {
            return Optional.of(builtIn);
        }
        return Optional.of(configured);
    }

    @Override
    public float manaCost(ResourceLocation spellId, int level) {
        AbstractSpell spell = find(spellId);
        return spell == null ? 0f : spell.getManaCost(level);
    }

    @Override
    public Optional<InstalledSpell> readInscription(ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) return Optional.empty();
        ISpellContainer container = ISpellContainer.get(stack);
        if (container == null) return Optional.empty();
        List<SpellSlot> spells = container.getActiveSpells();
        if (spells.isEmpty()) return Optional.empty();
        SpellSlot slot = spells.getFirst();
        ResourceLocation id = ResourceLocation.tryParse(slot.getSpell().getSpellId());
        return id == null ? Optional.empty() : Optional.of(new InstalledSpell(id, Math.max(1, slot.getLevel())));
    }

    /**
     * Writes a spell onto an inscription item, the way Iron's makes scrolls. Used by tests and by
     * the creative tab; gameplay only ever reads inscriptions.
     */
    public static boolean inscribe(ItemStack stack, ResourceLocation spellId, int level) {
        AbstractSpell spell = find(spellId);
        if (spell == null) return false;
        ISpellContainer.createScrollContainer(spell, level, stack);
        return true;
    }
}
