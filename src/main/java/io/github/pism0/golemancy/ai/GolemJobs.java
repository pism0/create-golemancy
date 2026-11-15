package io.github.pism0.golemancy.ai;

import io.github.pism0.golemancy.core.GolemancyConfig;
import io.github.pism0.golemancy.data.GolemDefs;
import io.github.pism0.golemancy.data.InstalledSpell;
import io.github.pism0.golemancy.entity.ConstructEntity;
import io.github.pism0.golemancy.magic.CoreDef;
import io.github.pism0.golemancy.magic.MagicServices;
import io.github.pism0.golemancy.magic.SpellProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** The per-golem job instances a behaviour chooses between. One set per golem, created with its goal. */
public final class GolemJobs {
    private final ConstructEntity golem;
    public final IGolemJob idle;
    public final IGolemJob melee;
    public final IGolemJob returnHome;
    public final IGolemJob followOwner;
    public final CastSpellJob castSpell;

    public GolemJobs(ConstructEntity golem) {
        this.golem = golem;
        this.idle = new IdleJob();
        this.melee = new MeleeJob();
        this.returnHome = new MoveToJob("return_home") {
            @Override
            Vec3 destination() {
                return Targeting.centerOf(golem.getAnchor());
            }
        };
        this.followOwner = new MoveToJob("follow_owner") {
            @Override
            @Nullable
            Vec3 destination() {
                LivingEntity owner = golem.getOwner();
                return owner == null || owner.level() != golem.level() ? null : owner.position();
            }

            @Override
            double stopDistance() {
                return 3.0;
            }
        };
        this.castSpell = new CastSpellJob();
    }

    /** Stand still, look around now and then. */
    private final class IdleJob implements IGolemJob {
        private int lookTime;
        private double lookX;
        private double lookZ;

        @Override
        public void start() {
            golem.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (lookTime-- <= 0 && golem.getRandom().nextInt(120) == 0) {
                double angle = golem.getRandom().nextDouble() * Math.PI * 2;
                lookX = Math.cos(angle);
                lookZ = Math.sin(angle);
                lookTime = 30 + golem.getRandom().nextInt(30);
            }
            if (lookTime > 0) {
                golem.getLookControl().setLookAt(golem.getX() + lookX, golem.getEyeY(), golem.getZ() + lookZ);
            }
        }

        @Override
        public String name() {
            return "idle";
        }
    }

    /** Close with the target and strike on the frame's attack interval. */
    private final class MeleeJob implements IGolemJob {
        private int cooldown;
        private int repath;

        @Override
        public void tick() {
            LivingEntity target = golem.getTarget();
            if (cooldown > 0) cooldown--;
            if (target == null || !target.isAlive()) return;
            golem.getLookControl().setLookAt(target, 30f, 30f);
            if (golem.isWithinMeleeAttackRange(target)) {
                golem.getNavigation().stop();
                if (cooldown <= 0 && golem.getSensing().hasLineOfSight(target)) {
                    cooldown = golem.getResolved().attackInterval();
                    golem.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    golem.playAttackAnimation();
                    golem.doHurtTarget(target);
                }
            } else if (--repath <= 0) {
                // Path recomputation is throttled, with a longer back-off when no path is found (SDD 16.1).
                boolean found = golem.getNavigation().moveTo(target, 1.0);
                repath = found ? 10 : 40;
            }
        }

        @Override
        public void finish() {
            golem.getNavigation().stop();
        }

        @Override
        public String name() {
            return "melee";
        }
    }

    /** Walk to a destination and stop near it. Reuses the current path and backs off on failure. */
    private abstract class MoveToJob implements IGolemJob {
        private final String name;
        private int repath;
        private int failures;

        MoveToJob(String name) {
            this.name = name;
        }

        @Nullable
        abstract Vec3 destination();

        double stopDistance() {
            return 1.5;
        }

        @Override
        public void start() {
            repath = 0;
            failures = 0;
        }

        @Override
        public void tick() {
            Vec3 dest = destination();
            if (dest == null) return;
            double stop = stopDistance();
            if (golem.position().distanceToSqr(dest) <= stop * stop) {
                golem.getNavigation().stop();
                return;
            }
            if (--repath <= 0) {
                boolean found = golem.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0);
                failures = found ? 0 : failures + 1;
                repath = found ? 20 : Math.min(200, 40 * failures);
            }
        }

        @Override
        public void finish() {
            golem.getNavigation().stop();
        }

        @Override
        public String name() {
            return name;
        }
    }

    /**
     * Cast the installed spell (SDD 7). Handles target choice, range, line of sight, cooldown and
     * energy cost here; the actual cast goes through {@code ISpellCaster}.
     */
    public final class CastSpellJob implements IGolemJob {
        @Nullable
        private InstalledSpell spell;
        @Nullable
        private SpellProfile profile;
        private int windup;

        /** Whether a cast is possible right now; remembers what to cast if so. */
        public boolean prepare() {
            if (windup > 0) return true; // already winding up: keep the chosen spell
            spell = null;
            profile = null;
            if (!GolemancyConfig.SPELLCASTING.get() || !golem.getSpec().behavior().castSpells()) return false;
            if (golem.level().getGameTime() < golem.getNextSpellTick()) return false;
            CoreDef core = golem.getCoreDef();
            if (core == null || core.spellSlots() <= 0) return false;
            for (InstalledSpell installed : golem.getSpec().spells()) {
                Optional<SpellProfile> candidate = GolemDefs.spellProfile(golem.level().registryAccess(), installed.spell());
                if (candidate.isEmpty() || !MagicServices.spellCaster().canCast(installed.spell())) continue;
                SpellProfile p = candidate.get();
                if (!golem.getEnergy().canAfford(energyCost(p, core))) continue;
                LivingEntity target = golem.getTarget();
                boolean usable = switch (p.targeting()) {
                    case ENEMY, AREA -> target != null && target.isAlive()
                            && golem.distanceToSqr(target) <= p.range() * p.range()
                            && golem.distanceToSqr(target) >= 9.0 // too close: melee instead
                            && (!p.requiresLos() || golem.getSensing().hasLineOfSight(target));
                    // Defensive self-casts only make sense in a fight.
                    case SELF -> target != null && target.isAlive() && golem.getHealth() < golem.getMaxHealth() * 0.75f;
                    case ALLY -> false; // no ally-targeted spells in v0.1
                };
                if (usable) {
                    spell = installed;
                    profile = p;
                    return true;
                }
            }
            return false;
        }

        private float energyCost(SpellProfile p, CoreDef core) {
            return p.energyCost() * core.spellCostMultiplier();
        }

        @Override
        public void start() {
            golem.getNavigation().stop();
            windup = 10; // face the target and play the cast animation before releasing the spell
            golem.playCastAnimation();
        }

        @Override
        public void tick() {
            LivingEntity target = golem.getTarget();
            if (target != null) {
                golem.getLookControl().setLookAt(target, 60f, 60f);
                faceTarget(target);
            }
            if (windup > 0 && --windup == 0) {
                release(target);
            }
        }

        private void faceTarget(LivingEntity target) {
            // Projectile spells fly along the caster's look vector, so the body must really point at the target.
            Vec3 delta = target.getEyePosition().subtract(golem.getEyePosition());
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
            golem.setYRot(yaw);
            golem.setYHeadRot(yaw);
            golem.setYBodyRot(yaw);
            golem.setXRot(pitch);
        }

        private void release(@Nullable LivingEntity target) {
            CoreDef core = golem.getCoreDef();
            if (spell == null || profile == null || core == null) return;
            if (profile.targeting() != SpellProfile.Targeting.SELF && (target == null || !target.isAlive())) return;
            float cost = energyCost(profile, core);
            if (!golem.consumeEnergy(cost)) return;
            int level = Math.min(spell.level(), Math.min(profile.maxLevel(), core.maxSpellLevel()));
            boolean cast = MagicServices.spellCaster().cast(golem, new InstalledSpell(spell.spell(), level), target,
                    golem.getResolved().spellPower());
            if (cast) {
                golem.setNextSpellTick(golem.level().getGameTime() + profile.minCooldown());
            } else {
                // Refund: nothing happened. Back off briefly so a broken spell is not retried every decision.
                golem.rechargeEnergy(cost * GolemancyConfig.ENERGY_DRAIN_MULTIPLIER.get().floatValue());
                golem.setNextSpellTick(golem.level().getGameTime() + 100);
            }
        }

        public boolean isBusy() {
            return windup > 0;
        }

        @Override
        public void finish() {
            windup = 0;
        }

        @Override
        public String name() {
            return "cast_spell";
        }
    }

    public BlockPos anchor() {
        return golem.getAnchor();
    }
}
