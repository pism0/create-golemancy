package io.github.pism0.golemancy.ai;

import io.github.pism0.golemancy.entity.ConstructEntity;
import io.github.pism0.golemancy.magic.IConstructEnergy;

/**
 * What jobs use to spend energy (SDD 12.5): wraps the golem's {@link IConstructEnergy} and applies
 * the server's drain multiplier, so no job talks to the energy store directly.
 */
public interface IGolemEnergyHandler {
    /** Whether {@code amount} (before the server multiplier) can be paid right now. */
    boolean canAfford(float amount);

    /** Pays {@code amount}; returns false and spends nothing if it cannot be afforded. */
    boolean spend(float amount);

    static IGolemEnergyHandler of(ConstructEntity golem) {
        return new IGolemEnergyHandler() {
            @Override
            public boolean canAfford(float amount) {
                float scaled = amount * io.github.pism0.golemancy.core.GolemancyConfig.ENERGY_DRAIN_MULTIPLIER.get().floatValue();
                return golem.getEnergy().canAfford(scaled);
            }

            @Override
            public boolean spend(float amount) {
                return golem.consumeEnergy(amount);
            }
        };
    }
}
