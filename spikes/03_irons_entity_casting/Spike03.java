// THROWAWAY SPIKE - not compiled into the mod. See spikes/README.md.
//
// SDD 7.3 questions, answered against Iron's Spells 1.21.1-3.16.3:
//
// Q1  Is the mob spellcasting path in the API jar or only in the full mod?
//     Only in the full mod. AbstractSpellCastingMob, WizardAttackGoal, SpellBarrageGoal, IMagicSummon
//     and SummonManager live outside io.redspace.ironsspellbooks.api. The API jar has
//     api.entity.IMagicEntity, but its signature mentions capabilities.magic.SyncedSpellData, which
//     is NOT in the API jar, so it cannot even be implemented when compiling against :api only.
//
// Q2  Can a non-Iron's entity cast without extending Iron's base mob class?
//     Yes. What AbstractSpellCastingMob does for INSTANT / LONG spells boils down to:
//         spell.checkPreCastConditions(level, lvl, mob, magicData)
//         magicData.initiateCast(spell, lvl, castTime, CastSource.MOB, slot)
//         spell.onServerPreCast(level, lvl, mob, magicData)
//         ...after castTime ticks...
//         spell.onCast(level, lvl, mob, CastSource.MOB, magicData)
//         spell.onServerCastComplete(level, lvl, mob, magicData, false)
//     Every one of those is on api.spells.AbstractSpell / api.magic.MagicData and takes a plain
//     LivingEntity. Projectile spells (firebolt, icicle) aim along entity.getLookAngle(); raycast
//     spells (ray_of_frost, shield) use RaycastBuilder.begin(level, entity). So the golem only has
//     to face its target before the cast.
//
// Q3  Does the summon manager give ownership and friendly fire for free?
//     No. DamageSources.isFriendlyFireBetween only unwraps IMagicSummon (full mod) and vanilla
//     teams. Projectiles do record the caster as owner, so Golemancy filters friendly fire itself
//     in LivingIncomingDamageEvent: damage whose attacker is a construct never reaches its owner,
//     the owner's team or the owner's other constructs.

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

final class Spike03 {
    /** Minimal instant cast from any Mob. This is what IronsSpellCaster does in the real mod. */
    static boolean castInstant(Mob caster, ResourceLocation spellId, int level, MagicData magicData) {
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        if (spell == SpellRegistry.none() || spell.getCastType() != CastType.INSTANT) return false;
        if (!spell.checkPreCastConditions(caster.level(), level, caster, magicData)) return false;
        spell.onCast(caster.level(), level, caster, CastSource.MOB, magicData);
        spell.onServerCastComplete(caster.level(), level, caster, magicData, false);
        return true;
    }
}

// Conclusion: ISpellCaster is implemented by Golemancy's own adapter (IronsSpellCaster) using API
// classes only, restricted to CastType.INSTANT spells on the allow-list. CONTINUOUS / LONG spells
// need per-tick cast state that mirrors AbstractSpellCastingMob and are deferred.
