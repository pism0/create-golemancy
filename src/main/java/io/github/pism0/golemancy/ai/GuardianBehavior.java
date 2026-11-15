package io.github.pism0.golemancy.ai;

import io.github.pism0.golemancy.data.BehaviorConfig;
import io.github.pism0.golemancy.entity.ConstructEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Guardian (SDD 12.1): holds a position, engages hostiles inside the radius, returns home. With
 * "follow" selected the position it holds is its owner instead of a fixed home.
 */
public final class GuardianBehavior implements IGolemBehavior {
    public static final GuardianBehavior INSTANCE = new GuardianBehavior();

    private GuardianBehavior() {
    }

    @Override
    public IGolemJob chooseJob(ConstructEntity golem, GolemJobs jobs) {
        BehaviorConfig config = golem.getSpec().behavior();
        boolean follow = config.moveMode() == BehaviorConfig.MoveMode.FOLLOW && golem.getOwner() != null
                && golem.getOwner().level() == golem.level();
        Vec3 center = follow ? golem.getOwner().position() : Targeting.centerOf(golem.getAnchor());
        double radius = config.radius();

        if (config.combat()) {
            LivingEntity target = golem.getTarget();
            // A Guardian does not chase beyond its post: drop targets that leave the radius (plus a margin).
            if (!Targeting.stillValid(golem, target, center, radius + 4)) {
                target = Targeting.nearestHostile(golem, center, radius);
                if (target == null) {
                    LivingEntity aggressor = Targeting.aggressor(golem);
                    if (Targeting.stillValid(golem, aggressor, center, radius + 4)) target = aggressor;
                }
                golem.setTarget(target);
            }
            if (golem.getTarget() != null) {
                return jobs.castSpell.isBusy() || jobs.castSpell.prepare() ? jobs.castSpell : jobs.melee;
            }
        } else if (golem.getTarget() != null) {
            golem.setTarget(null);
        }

        if (follow) {
            return golem.distanceToSqr(center) > 16 ? jobs.followOwner : jobs.idle;
        }
        return golem.position().distanceToSqr(center) > 2.25 ? jobs.returnHome : jobs.idle;
    }
}
