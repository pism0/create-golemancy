package io.github.pism0.golemancy.ai;

/**
 * One unit of work for a golem (SDD 12.5): attack a target, return home, follow the owner, cast a
 * spell. New jobs are added without touching the entity class.
 */
public interface IGolemJob {
    /** Called once when the behaviour switches to this job. */
    default void start() {
    }

    /** Called every tick while this job is current (movement and animation need per-tick updates). */
    void tick();

    /** Called once when the behaviour switches away from this job. */
    default void finish() {
    }

    /** Short name for debugging and tests. */
    String name();
}
