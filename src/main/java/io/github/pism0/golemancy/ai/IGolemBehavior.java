package io.github.pism0.golemancy.ai;

import io.github.pism0.golemancy.data.BehaviorConfig;
import io.github.pism0.golemancy.entity.ConstructEntity;

/**
 * A behaviour profile (SDD 12.1): decides, on the staggered decision interval, which job a golem
 * runs next. Profiles are stateless; per-golem state lives in the {@link GolemJobs} they are given.
 */
public interface IGolemBehavior {
    /** Picks the job to run until the next decision. May also update the golem's target. */
    IGolemJob chooseJob(ConstructEntity golem, GolemJobs jobs);

    static IGolemBehavior forProfile(BehaviorConfig.Profile profile) {
        return switch (profile) {
            case GUARDIAN -> GuardianBehavior.INSTANCE;
            case SOLDIER -> SoldierBehavior.INSTANCE;
        };
    }
}
