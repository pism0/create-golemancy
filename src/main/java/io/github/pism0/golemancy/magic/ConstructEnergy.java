package io.github.pism0.golemancy.magic;

/** Default {@link IConstructEnergy}: a clamped float store. */
public final class ConstructEnergy implements IConstructEnergy {
    private float energy;
    private float max = 1f;

    @Override
    public float getEnergy() {
        return energy;
    }

    @Override
    public float getMaxEnergy() {
        return max;
    }

    @Override
    public float drain(float amount) {
        if (amount <= 0f) return 0f;
        float drained = Math.min(energy, amount);
        energy -= drained;
        if (energy < 0.0001f) energy = 0f;
        return drained;
    }

    @Override
    public float recharge(float amount) {
        if (amount <= 0f) return 0f;
        float stored = Math.min(max - energy, amount);
        energy += Math.max(0f, stored);
        return Math.max(0f, stored);
    }

    public void set(float value) {
        energy = Math.max(0f, Math.min(max, value));
    }

    /** Changes the capacity; stored energy is clamped to the new capacity. */
    public void setMax(float newMax) {
        max = Math.max(0f, newMax);
        energy = Math.min(energy, max);
    }
}
