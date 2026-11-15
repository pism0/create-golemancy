package io.github.pism0.golemancy.ai;

import io.github.pism0.golemancy.data.BehaviorConfig;
import io.github.pism0.golemancy.entity.ConstructEntity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Soldier (SDD 12.1): follows its owner (or holds position when told to stay) and attacks hostiles
 * it notices, plus whatever its owner is fighting.
 */
public final class SoldierBehavior implements IGolemBehavior {
    public static final SoldierBehavior INSTANCE = new SoldierBehavior();
    /** How far a Soldier looks for trouble on its own. */
    private static final double NOTICE_RANGE = 12.0;

    private SoldierBehavior() {
    }

    @Override
    public IGolemJob chooseJob(ConstructEntity golem, GolemJobs jobs) {
        BehaviorConfig config = golem.getSpec().behavior();
        double leash = golem.getResolved().followRange();

        if (config.combat()) {
            LivingEntity target = golem.getTarget();
            if (!Targeting.stillValid(golem, target, golem.position(), leash)) {
                target = Targeting.aggressor(golem);
                if (!Targeting.stillValid(golem, target, golem.position(), leash)) {
                    target = Targeting.nearestHostile(golem, golem.position(), NOTICE_RANGE);
                }
                golem.setTarget(target);
            }
            if (golem.getTarget() != null) {
                return jobs.castSpell.isBusy() || jobs.castSpell.prepare() ? jobs.castSpell : jobs.melee;
            }
        } else if (golem.getTarget() != null) {
            golem.setTarget(null);
        }

        if (config.moveMode() == BehaviorConfig.MoveMode.FOLLOW) {
            LivingEntity owner = golem.getOwner();
            if (owner != null && owner.level() == golem.level() && golem.distanceToSqr(owner) > 25) {
                return jobs.followOwner;
            }
            return jobs.idle;
        }
        return golem.position().distanceToSqr(Targeting.centerOf(golem.getAnchor())) > 2.25 ? jobs.returnHome : jobs.idle;
    }
}
