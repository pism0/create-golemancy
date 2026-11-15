package io.github.pism0.golemancy.ai;

import io.github.pism0.golemancy.entity.ConstructEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * The single vanilla goal that drives a golem (SDD 12.5). On the staggered decision interval it
 * asks the golem's {@link IGolemBehavior} which {@link IGolemJob} to run; every tick it ticks the
 * current job so movement and animation stay smooth.
 */
public final class JobRunnerGoal extends Goal {
    private final ConstructEntity golem;
    private final GolemJobs jobs;
    private IGolemJob current;

    public JobRunnerGoal(ConstructEntity golem) {
        this.golem = golem;
        this.jobs = new GolemJobs(golem);
        this.current = jobs.idle;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return golem.getState() == ConstructEntity.State.ACTIVE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        current = jobs.idle;
        current.start();
    }

    @Override
    public void tick() {
        if (golem.isDecisionTick()) {
            IGolemJob next = IGolemBehavior.forProfile(golem.getSpec().behavior().profile()).chooseJob(golem, jobs);
            if (next != current) {
                current.finish();
                current = next;
                current.start();
            }
        }
        current.tick();
    }

    @Override
    public void stop() {
        current.finish();
        current = jobs.idle;
        golem.getNavigation().stop();
    }

    /** The job currently running (for tests and debugging). */
    public String currentJobName() {
        return current.name();
    }
}
