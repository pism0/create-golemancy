package io.github.pism0.golemancy.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server configuration (SDD 15.2). Server configs are synced to clients by NeoForge; the client
 * only ever displays these values and never decides them.
 */
public final class GolemancyConfig {
    private GolemancyConfig() {
    }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MAX_GOLEMS_PER_PLAYER;
    public static final ModConfigSpec.DoubleValue ENERGY_DRAIN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue DURABILITY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ABSORPTION_FRACTION;
    public static final ModConfigSpec.DoubleValue ABSORPTION_CAP;
    public static final ModConfigSpec.IntValue ABSORPTION_WINDOW_TICKS;
    public static final ModConfigSpec.BooleanValue SPELLCASTING;
    public static final ModConfigSpec.BooleanValue GOLEM_PVP;
    public static final ModConfigSpec.BooleanValue AUTONOMOUS_COMBAT;
    public static final ModConfigSpec.DoubleValue PROCESSING_SPEED_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue REPAIR_COST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue INTEGRITY_LOSS_ON_DEATH;
    public static final ModConfigSpec.IntValue ABANDONED_TIMEOUT_MINUTES;
    public static final ModConfigSpec.IntValue DECISION_INTERVAL;
    public static final ModConfigSpec.BooleanValue EMPTY_HAND_PACK_UP;
    public static final ModConfigSpec.DoubleValue FIELD_REPAIR_HEALTH;
    public static final ModConfigSpec.IntValue FIELD_REPAIR_COOLDOWN;
    public static final ModConfigSpec.DoubleValue FIELD_REPAIR_ENERGY_COST;
    public static final ModConfigSpec.IntValue ALTAR_MIN_SPEED;
    public static final ModConfigSpec.DoubleValue ALTAR_STRESS_IMPACT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("golems");
        MAX_GOLEMS_PER_PLAYER = b.comment("Maximum active (deployed) golems per player. Packed-up golems do not count.")
                .defineInRange("maxActiveGolemsPerPlayer", 8, 0, 1024);
        ENERGY_DRAIN_MULTIPLIER = b.comment("Multiplies passive energy drain and spell energy cost.")
                .defineInRange("energyDrainMultiplier", 1.0, 0.0, 100.0);
        DURABILITY_MULTIPLIER = b.comment("Multiplies golem maximum health.")
                .defineInRange("durabilityMultiplier", 1.0, 0.1, 100.0);
        DAMAGE_MULTIPLIER = b.comment("Multiplies golem melee damage.")
                .defineInRange("damageMultiplier", 1.0, 0.0, 100.0);
        DECISION_INTERVAL = b.comment("Ticks between AI decisions (target choice, spell choice, energy drain). Offset per golem.")
                .defineInRange("decisionIntervalTicks", 10, 1, 200);
        ABANDONED_TIMEOUT_MINUTES = b.comment("Golems whose owner has been offline longer than this power down. 0 disables.")
                .defineInRange("abandonedTimeoutMinutes", 0, 0, 525600);
        EMPTY_HAND_PACK_UP = b.comment("Allow the owner to pack up a golem by sneak-using it with an empty hand (the Control Wand always works).")
                .define("emptyHandPackUp", true);
        b.pop();

        b.push("combat");
        AUTONOMOUS_COMBAT = b.comment("Whether golems pick targets on their own.")
                .define("autonomousCombat", true);
        SPELLCASTING = b.comment("Whether golems may cast their installed spells.")
                .define("spellcasting", true);
        GOLEM_PVP = b.comment("Whether golems may damage players other than their owner (also requires server PvP).")
                .define("golemPvp", false);
        ABSORPTION_FRACTION = b.comment("Fraction of absorbed damage that is converted to healing.")
                .defineInRange("absorptionFraction", 0.5, 0.0, 1.0);
        ABSORPTION_CAP = b.comment("Maximum health restored by absorption per window. Damage beyond the cap is taken normally.")
                .defineInRange("absorptionCap", 20.0, 0.0, 10000.0);
        ABSORPTION_WINDOW_TICKS = b.comment("Length of the absorption window in ticks.")
                .defineInRange("absorptionWindowTicks", 200, 1, 72000);
        b.pop();

        b.push("lifecycle");
        INTEGRITY_LOSS_ON_DEATH = b.comment("Core integrity lost each time a golem is destroyed.")
                .defineInRange("integrityLossOnDeath", 0.25, 0.0, 1.0);
        REPAIR_COST_MULTIPLIER = b.comment("Multiplies the repair items needed to rebuild Construct Remains.")
                .defineInRange("repairCostMultiplier", 1.0, 0.0, 16.0);
        FIELD_REPAIR_HEALTH = b.comment("Fraction of maximum health restored by one field repair.")
                .defineInRange("fieldRepairHealthFraction", 0.25, 0.0, 1.0);
        FIELD_REPAIR_COOLDOWN = b.comment("Ticks before a golem can be field-repaired again.")
                .defineInRange("fieldRepairCooldownTicks", 100, 0, 72000);
        FIELD_REPAIR_ENERGY_COST = b.comment("Fraction of maximum energy one field repair costs.")
                .defineInRange("fieldRepairEnergyFraction", 0.05, 0.0, 1.0);
        b.pop();

        b.push("binding_altar");
        PROCESSING_SPEED_MULTIPLIER = b.comment("Multiplies Binding Altar processing speed.")
                .defineInRange("processingSpeedMultiplier", 1.0, 0.01, 100.0);
        ALTAR_MIN_SPEED = b.comment("Minimum rotation speed (RPM) the Binding Altar needs. A recipe may ask for more.")
                .defineInRange("minimumSpeed", 32, 1, 256);
        ALTAR_STRESS_IMPACT = b.comment("Stress impact of the Binding Altar (SU per RPM).")
                .defineInRange("stressImpact", 4.0, 0.0, 1024.0);
        b.pop();

        SPEC = b.build();
    }
}
