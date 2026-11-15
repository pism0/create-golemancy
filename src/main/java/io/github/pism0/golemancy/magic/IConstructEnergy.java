package io.github.pism0.golemancy.magic;

/**
 * Construct energy: the single internal resource a golem runs on (SDD 6.4). It is not Iron's
 * player mana and not a Create: Wizardry fluid. All energy logic is server-side.
 */
public interface IConstructEnergy {
    float getEnergy();

    float getMaxEnergy();

    /** Removes up to {@code amount}; returns what was actually drained. */
    float drain(float amount);

    /** Adds up to {@code amount}; returns what was actually stored. */
    float recharge(float amount);

    default boolean isEmpty() {
        return getEnergy() <= 0f;
    }

    default boolean isFull() {
        return getEnergy() >= getMaxEnergy();
    }

    default float getFraction() {
        return getMaxEnergy() <= 0f ? 0f : Math.max(0f, Math.min(1f, getEnergy() / getMaxEnergy()));
    }

    default boolean canAfford(float amount) {
        return getEnergy() >= amount;
    }
}
