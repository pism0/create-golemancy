package io.github.pism0.golemancy.block;

import io.github.pism0.golemancy.construction.FrameDef;
import io.github.pism0.golemancy.construction.MaterialDef;
import io.github.pism0.golemancy.core.GolemancyConfig;
import io.github.pism0.golemancy.core.ModDataComponents;
import io.github.pism0.golemancy.core.ModItems;
import io.github.pism0.golemancy.core.ModRecipes;
import io.github.pism0.golemancy.core.ModRegistries;
import io.github.pism0.golemancy.core.ModTags;
import io.github.pism0.golemancy.data.BehaviorConfig;
import io.github.pism0.golemancy.data.ConstructState;
import io.github.pism0.golemancy.data.GolemDefs;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.data.InstalledSpell;
import io.github.pism0.golemancy.lifecycle.GolemLifecycle;
import io.github.pism0.golemancy.magic.CoreDef;
import io.github.pism0.golemancy.magic.MagicServices;
import io.github.pism0.golemancy.magic.SpellProfile;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * What the Binding Altar does, with no Create or block code in it (SDD 4.2 step 4, 7.4, 8.2,
 * 11.3). Given the items on the altar it works out a {@link Plan}: what is missing, or what will be
 * produced and consumed. The same logic drives the block entity, the goggle tooltip, JEI and the
 * GameTests. Server-side.
 *
 * <p>Two modes, chosen by what sits in the body slot:
 * <ul>
 *   <li><b>Binding</b>: frame + core (+ one inscription) → Dormant Golem with a fresh spec.</li>
 *   <li><b>Rebuilding</b>: Construct Remains + repair material + fuel (+ a new core, required if
 *       the old one shattered) → Dormant Golem with the old spec.</li>
 * </ul>
 */
public final class BindingLogic {
    private BindingLogic() {
    }

    /** Repair items a rebuild costs before the server multiplier. */
    public static final int BASE_REPAIR_ITEMS = 4;

    /** Which altar slot an item belongs in. */
    public enum Slot {
        BODY, CORE, INSCRIPTION, SUPPLY
    }

    public enum Status {
        /** Nothing in the body slot. */
        EMPTY("empty"),
        /** The body item is neither a known frame nor Construct Remains. */
        INVALID_BODY("invalid_body"),
        MISSING_CORE("missing_core"),
        /** The item in the core slot is not a known core. */
        INVALID_CORE("invalid_core"),
        /** The inscription cannot be installed; {@link Plan#detailKey()} says why. */
        INVALID_INSCRIPTION("invalid_inscription"),
        MISSING_REPAIR_MATERIAL("missing_repair_material"),
        MISSING_FUEL("missing_fuel"),
        READY("ready");

        private final String key;

        Status(String key) {
            this.key = key;
        }

        public String translationKey() {
            return "golemancy.altar.status." + key;
        }
    }

    /**
     * The outcome of looking at the altar's contents.
     *
     * @param status          what is missing, or READY
     * @param detailKey       translation key with more detail (inscription rejection reason), or ""
     * @param output          the Dormant Golem to produce (empty unless READY)
     * @param processingTime  ticks at 32 RPM
     * @param minSpeed        minimum RPM
     * @param repairItems     repair items to consume from the supply slots
     * @param consumeCore     whether the core item is used up
     * @param consumeInscription whether the inscription item is used up
     * @param consumeFuel     whether one fuel item is used up
     * @param repair          true in rebuilding mode
     */
    public record Plan(Status status, String detailKey, ItemStack output, int processingTime, int minSpeed,
                       int repairItems, boolean consumeCore, boolean consumeInscription, boolean consumeFuel,
                       boolean repair) {
        public boolean ready() {
            return status == Status.READY;
        }

        /** The plan of an altar with nothing on it. */
        public static Plan empty() {
            return blocked(Status.EMPTY, false);
        }

        static Plan blocked(Status status, boolean repair) {
            return blocked(status, "", repair);
        }

        static Plan blocked(Status status, String detail, boolean repair) {
            return new Plan(status, detail, ItemStack.EMPTY, 0, 0, 0, false, false, false, repair);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Slot routing
    // ------------------------------------------------------------------------------------------

    /** Where an item inserted by a funnel, chute, deployer or arm goes; empty if the altar has no use for it. */
    public static Optional<Slot> slotFor(Level level, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        RegistryAccess access = level.registryAccess();
        if (stack.is(ModItems.CONSTRUCT_REMAINS.get()) || findRecipe(level, stack).isPresent()) {
            return Optional.of(Slot.BODY);
        }
        if (GolemDefs.coreIdForItem(access, stack).isPresent()) {
            return Optional.of(Slot.CORE);
        }
        if (stack.is(ModTags.INSCRIPTIONS)) {
            return Optional.of(Slot.INSCRIPTION);
        }
        if (GolemDefs.fuelEnergy(access, stack) > 0 || isRepairMaterial(access, stack)) {
            return Optional.of(Slot.SUPPLY);
        }
        return Optional.empty();
    }

    private static boolean isRepairMaterial(RegistryAccess access, ItemStack stack) {
        return access.registry(ModRegistries.MATERIAL)
                .map(reg -> reg.stream().anyMatch(m -> m.repairItem().test(stack)))
                .orElse(false);
    }

    public static Optional<RecipeHolder<BindingRecipe>> findRecipe(Level level, ItemStack frame) {
        if (frame.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(ModRecipes.BINDING.get(), new BindingRecipe.Input(frame), level);
    }

    // ------------------------------------------------------------------------------------------
    // Planning
    // ------------------------------------------------------------------------------------------

    public static Plan plan(Level level, ItemStack body, ItemStack core, ItemStack inscription, List<ItemStack> supplies) {
        if (body.isEmpty()) return Plan.blocked(Status.EMPTY, false);
        if (body.is(ModItems.CONSTRUCT_REMAINS.get())) {
            return planRepair(level, body, core, supplies);
        }
        Optional<RecipeHolder<BindingRecipe>> recipe = findRecipe(level, body);
        if (recipe.isEmpty()) return Plan.blocked(Status.INVALID_BODY, false);
        return planBinding(level, recipe.get().value(), core, inscription);
    }

    private static Plan planBinding(Level level, BindingRecipe recipe, ItemStack core, ItemStack inscription) {
        RegistryAccess access = level.registryAccess();
        if (core.isEmpty()) return Plan.blocked(Status.MISSING_CORE, false);
        Optional<ResourceLocation> coreId = GolemDefs.coreIdForItem(access, core);
        if (coreId.isEmpty()) return Plan.blocked(Status.INVALID_CORE, false);

        GolemSpec spec = GolemSpec.create(recipe.golemFrame(), recipe.golemMaterial(), coreId.get());
        BehaviorConfig.Profile profile = GolemDefs.frame(access, recipe.golemFrame())
                .map(FrameDef::defaultProfile).orElse(BehaviorConfig.Profile.GUARDIAN);
        spec = spec.withBehavior(spec.behavior().withProfile(profile));

        boolean hasInscription = !inscription.isEmpty();
        if (hasInscription) {
            if (!inscription.is(ModTags.INSCRIPTIONS)) {
                return Plan.blocked(Status.INVALID_INSCRIPTION, "golemancy.altar.inscription.not_inscription", false);
            }
            Optional<InstalledSpell> spell = MagicServices.spellCaster().readInscription(inscription);
            if (spell.isEmpty()) {
                return Plan.blocked(Status.INVALID_INSCRIPTION, "golemancy.altar.inscription.blank", false);
            }
            Optional<String> problem = validateSpell(access, spec, spell.get());
            if (problem.isPresent()) {
                return Plan.blocked(Status.INVALID_INSCRIPTION, problem.get(), false);
            }
            spec = spec.withSpells(List.of(BuiltInRegistries.ITEM.getKey(inscription.getItem())), List.of(spell.get()));
        }
        ItemStack output = GolemLifecycle.dormantStack(spec, ConstructState.FRESH);
        return new Plan(Status.READY, "", output, recipe.processingTime(), recipe.minSpeed(), 0, true, hasInscription, false, false);
    }

    private static Plan planRepair(Level level, ItemStack remains, ItemStack core, List<ItemStack> supplies) {
        RegistryAccess access = level.registryAccess();
        GolemSpec carried = remains.get(ModDataComponents.GOLEM_SPEC);
        if (carried == null) return Plan.blocked(Status.INVALID_BODY, true);
        GolemSpec spec = GolemDefs.sanitize(carried, access);

        boolean consumeCore = false;
        if (!core.isEmpty()) {
            Optional<ResourceLocation> coreId = GolemDefs.coreIdForItem(access, core);
            if (coreId.isEmpty()) return Plan.blocked(Status.INVALID_CORE, true);
            // A new core restores integrity (SDD 11.3: the only way it recovers). A different core
            // type cannot keep spells of the old school.
            boolean sameType = coreId.get().equals(spec.core());
            spec = spec.withCore(coreId.get(), 1f);
            if (!sameType) {
                spec = spec.withSpells(List.of(), List.of());
            }
            consumeCore = true;
        } else if (!spec.hasCore()) {
            return Plan.blocked(Status.MISSING_CORE, true);
        }

        MaterialDef material = GolemDefs.material(access, spec.material()).orElse(null);
        int needed = (int) Math.ceil(BASE_REPAIR_ITEMS * GolemancyConfig.REPAIR_COST_MULTIPLIER.get());
        int repairAvailable = 0;
        boolean fuel = false;
        for (ItemStack supply : supplies) {
            if (supply.isEmpty()) continue;
            if (material != null && material.repairItem().test(supply)) {
                repairAvailable += supply.getCount();
            } else if (GolemDefs.fuelEnergy(access, supply) > 0) {
                fuel = true;
            }
        }
        if (repairAvailable < needed) return Plan.blocked(Status.MISSING_REPAIR_MATERIAL, true);
        if (!fuel) return Plan.blocked(Status.MISSING_FUEL, true);

        ItemStack output = GolemLifecycle.dormantStack(spec, ConstructState.FRESH);
        return new Plan(Status.READY, "", output, BindingRecipe.DEFAULT_TIME, BindingRecipe.DEFAULT_SPEED, needed,
                consumeCore, false, true, true);
    }

    /**
     * Spell installation rules (SDD 7.4). Returns a translation key describing the problem, or
     * empty if the spell may be installed in a golem with this frame and core.
     */
    public static Optional<String> validateSpell(RegistryAccess access, GolemSpec spec, InstalledSpell spell) {
        // 1. on the allow-list
        Optional<SpellProfile> profile = GolemDefs.spellProfile(access, spell.spell());
        if (profile.isEmpty() || !MagicServices.spellCaster().canCast(spell.spell())) {
            return Optional.of("golemancy.altar.inscription.not_allowed");
        }
        CoreDef core = GolemDefs.core(access, spec.core()).orElse(null);
        FrameDef frame = GolemDefs.frame(access, spec.frame()).orElse(null);
        if (core == null || frame == null) {
            return Optional.of("golemancy.altar.inscription.no_slot");
        }
        // 3. a free spell slot (frame and core must both provide one)
        int slots = Math.min(frame.spellSlots(), core.spellSlots());
        if (spec.spells().size() >= slots) {
            return Optional.of("golemancy.altar.inscription.no_slot");
        }
        // 2. school matches the core, or the spell is school-neutral
        if (!profile.get().schoolNeutral()) {
            Optional<ResourceLocation> school = MagicServices.spellCaster().schoolOf(spell.spell());
            if (core.school().isEmpty() || school.isEmpty() || !core.school().get().equals(school.get())) {
                io.github.pism0.golemancy.Golemancy.LOGGER.debug("Spell {} (school {}) does not match core {} (school {})",
                        spell.spell(), school, spec.core(), core.school());
                return Optional.of("golemancy.altar.inscription.wrong_school");
            }
        }
        // 4. level within the core's cap
        if (spell.level() > Math.min(core.maxSpellLevel(), profile.get().maxLevel())) {
            return Optional.of("golemancy.altar.inscription.level_too_high");
        }
        return Optional.empty();
    }
}
