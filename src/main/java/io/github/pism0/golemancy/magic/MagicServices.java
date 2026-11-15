package io.github.pism0.golemancy.magic;

/**
 * Where the adapters behind Golemancy's magic interfaces are registered (SDD 3.2). Golem logic
 * asks here and never touches Iron's Spells classes directly.
 */
public final class MagicServices {
    private MagicServices() {
    }

    private static volatile ISpellCaster spellCaster = ISpellCaster.NONE;
    private static volatile IAffinitySource affinitySource = IAffinitySource.NONE;

    public static ISpellCaster spellCaster() {
        return spellCaster;
    }

    public static IAffinitySource affinitySource() {
        return affinitySource;
    }

    public static void register(ISpellCaster caster, IAffinitySource affinities) {
        spellCaster = caster;
        affinitySource = affinities;
    }
}
