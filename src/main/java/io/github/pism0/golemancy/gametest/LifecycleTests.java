package io.github.pism0.golemancy.gametest;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.block.BindingLogic;
import io.github.pism0.golemancy.block.BindingLogic.Plan;
import io.github.pism0.golemancy.block.BindingLogic.Status;
import io.github.pism0.golemancy.core.GolemancyConfig;
import io.github.pism0.golemancy.core.ModDataComponents;
import io.github.pism0.golemancy.core.ModItems;
import io.github.pism0.golemancy.core.ModRegistries;
import io.github.pism0.golemancy.core.ModTags;
import io.github.pism0.golemancy.data.BehaviorConfig;
import io.github.pism0.golemancy.data.ConstructState;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.data.InstalledSpell;
import io.github.pism0.golemancy.entity.ConstructEntity;
import io.github.pism0.golemancy.lifecycle.GolemLifecycle;
import io.github.pism0.golemancy.lifecycle.GolemRequests;
import io.github.pism0.golemancy.lifecycle.Ownership;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static io.github.pism0.golemancy.gametest.GolemTestUtil.ARENA;
import static io.github.pism0.golemancy.gametest.GolemTestUtil.PLATFORM;

/** Milestone 4 gate: magic, energy, the golem lifecycle and ownership (SDD 6.4, 7, 10.3, 11). */
@GameTestHolder(Golemancy.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LifecycleTests {
    private LifecycleTests() {
    }

    private static final ResourceLocation FIREBOLT = ResourceLocation.parse("irons_spellbooks:firebolt");

    private static void assertClose(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 0.01, what + ": expected " + expected + ", got " + actual);
    }

    private static GolemSpec withSpell(GolemSpec spec, ResourceLocation spell, int level) {
        return spec.withSpells(List.of(), List.of(new InstalledSpell(spell, level)));
    }

    private static int playerCounter;

    private static ServerPlayer player(GameTestHelper helper, double x, double z) {
        ServerPlayer player = TestPlayer.create(helper.getLevel(), "tester" + (playerCounter++));
        BlockPos pos = helper.absolutePos(new BlockPos((int) x, 2, (int) z));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return player;
    }

    // ------------------------------------------------------------------------------------------
    // Magic
    // ------------------------------------------------------------------------------------------

    /**
     * A golem casts its installed spell at a hostile through the Iron's adapter (M0 spike 3 pinned):
     * the cast costs energy, starts a cooldown, and the target is hit by Iron's spell damage.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void golemCastsInstalledSpell(GameTestHelper helper) {
        GolemSpec spec = withSpell(GolemTestUtil.spec("guardian", "stone", "fire"), FIREBOLT, 1);
        ConstructEntity golem = GolemTestUtil.deploy(helper, spec, 3, 7);
        float startEnergy = golem.getEnergy().getEnergy();
        Husk husk = helper.spawn(EntityType.HUSK, 11, 2, 7);
        husk.setNoAi(true);
        husk.setPersistenceRequired();
        boolean[] hitBySpell = {false};
        java.util.Set<String> seen = new java.util.TreeSet<>();
        helper.succeedWhen(() -> {
            DamageSource last = husk.getLastDamageSource();
            if (last != null) {
                seen.add(last.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("?"));
            }
            // Any Iron's "<school>_magic" type counts: with no player online Iron's spell config is not
            // loaded yet and the school (hence the damage type) falls back to its default (D-016).
            if (last != null && last.typeHolder().unwrapKey().map(k -> k.location().getNamespace().equals("irons_spellbooks")
                    && k.location().getPath().endsWith("_magic")).orElse(false)) {
                hitBySpell[0] = true;
            }
            helper.assertTrue(golem.getNextSpellTick() > 0, "the golem has not cast yet");
            helper.assertTrue(golem.getEnergy().getEnergy() <= startEnergy - 19f,
                    "casting did not cost energy: " + golem.getEnergy().getEnergy() + " of " + startEnergy);
            helper.assertTrue(hitBySpell[0], "the husk was not hit by spell damage (damage seen: " + seen + ", golem at " + golem.position() + ", husk at " + husk.position() + ")");
        });
    }

    /** Spellcasting can be switched off per golem, and an Arcane Core never casts. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void castingRespectsToggleAndCore(GameTestHelper helper) {
        GolemSpec fire = withSpell(GolemTestUtil.spec("guardian", "stone", "fire"), FIREBOLT, 1);
        ConstructEntity silenced = GolemTestUtil.deploy(helper, fire, 3, 4);
        silenced.applySpec(silenced.getSpec().withBehavior(silenced.getSpec().behavior().withCastSpells(false).withRadius(4)));
        // An Arcane Core has no spell slot: even a spec that somehow lists a spell must not cast it.
        ConstructEntity arcane = GolemTestUtil.deploy(helper, withSpell(GolemTestUtil.spec("guardian", "stone", "arcane"), FIREBOLT, 1), 3, 10);
        arcane.applySpec(arcane.getSpec().withBehavior(arcane.getSpec().behavior().withRadius(4)));
        Husk husk = helper.spawn(EntityType.HUSK, 12, 2, 7);
        husk.setNoAi(true);
        silenced.setTarget(husk);
        arcane.setTarget(husk);
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(silenced.getNextSpellTick() == 0, "a golem with casting switched off cast a spell");
            helper.assertTrue(arcane.getNextSpellTick() == 0, "a golem with an Arcane Core cast a spell");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------
    // Damage rules
    // ------------------------------------------------------------------------------------------

    private static DamageSource taggedDamage(GameTestHelper helper, net.minecraft.tags.TagKey<DamageType> tag) {
        Holder<DamageType> type = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getTag(tag).flatMap(named -> named.stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("tag " + tag.location() + " is empty"));
        return new DamageSource(type);
    }

    /** Absorbed damage heals a fraction, up to a cap per window; past the cap it hurts normally. */
    @GameTest(template = PLATFORM)
    public static void absorptionIsCapped(GameTestHelper helper) {
        ConstructEntity golem = GolemTestUtil.deploy(helper, GolemTestUtil.spec("brute", "clay", "arcane"), 4, 4);
        DamageSource nature = taggedDamage(helper, ModTags.absorbedBy("clay"));
        float fraction = GolemancyConfig.ABSORPTION_FRACTION.get().floatValue();
        float cap = GolemancyConfig.ABSORPTION_CAP.get().floatValue();
        helper.assertTrue(fraction == 0.5f && cap == 20f, "test assumes the default absorption config");

        golem.setHealth(30f);
        // 16 damage absorbed: heals 8 and does no harm.
        helper.assertFalse(golem.hurt(nature, 16f), "absorbed damage should not count as a hit");
        assertClose(helper, golem.getHealth(), 38.0, "health after first absorbed hit");
        // 16 more: heals another 8 (16 of the 20 cap used).
        golem.hurt(nature, 16f);
        assertClose(helper, golem.getHealth(), 46.0, "health after second absorbed hit");
        // 16 more: only 4 of healing is left under the cap. That covers 8 damage; the other 8 hurts.
        golem.hurt(nature, 16f);
        assertClose(helper, golem.getAbsorbedInWindow(), 20.0, "absorbed total must stop at the cap");
        helper.assertTrue(golem.getHealth() < 50f, "damage beyond the cap must be taken normally, health " + golem.getHealth());
        // Cap exhausted: the next hit is plain damage, no healing loop is possible.
        golem.invulnerableTime = 0;
        float before = golem.getHealth();
        helper.assertTrue(golem.hurt(nature, 10f), "damage past the cap should hurt");
        helper.assertTrue(golem.getHealth() < before, "damage past the cap did not reduce health");
        assertClose(helper, golem.getAbsorbedInWindow(), 20.0, "no absorption past the cap");

        // A material that does not absorb this type is simply hurt by it.
        ConstructEntity stone = GolemTestUtil.deploy(helper, GolemTestUtil.spec("guardian", "stone", "arcane"), 6, 6);
        float stoneBefore = stone.getHealth();
        stone.hurt(nature, 10f);
        helper.assertTrue(stone.getHealth() < stoneBefore, "stone should not absorb nature damage");
        helper.succeed();
    }

    /** Friendly fire: a golem's attacks never land on allies (SDD 11.4). */
    @GameTest(template = PLATFORM)
    public static void friendlyFireIsCancelled(GameTestHelper helper) {
        ServerPlayer owner = player(helper, 1, 1);
        ConstructEntity golem = GolemTestUtil.deploy(helper, GolemTestUtil.spec("guardian", "stone", "fire").withOwner(owner.getUUID()), 3, 4);
        ConstructEntity sibling = GolemTestUtil.deploy(helper, GolemTestUtil.spec("brute", "clay", "arcane").withOwner(owner.getUUID()), 6, 4);
        float before = sibling.getHealth();
        boolean hurt = sibling.hurt(helper.getLevel().damageSources().mobAttack(golem), 10f);
        helper.assertFalse(hurt, "a golem damaged an allied golem");
        assertClose(helper, sibling.getHealth(), before, "allied golem health");
        // The same blow from something else does land.
        Husk husk = helper.spawn(EntityType.HUSK, 2, 2, 7);
        helper.assertTrue(sibling.hurt(helper.getLevel().damageSources().mobAttack(husk), 10f), "a hostile's attack was blocked");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------
    // Energy
    // ------------------------------------------------------------------------------------------

    /** Energy reaching zero powers the golem down (it does not die); recharging wakes it (SDD 6.4, 11.1). */
    @GameTest(template = PLATFORM, timeoutTicks = 300)
    public static void energyZeroPowersDownAndRechargeWakes(GameTestHelper helper) {
        ConstructEntity golem = GolemTestUtil.deploy(helper, GolemTestUtil.spec("guardian", "stone", "arcane"), 4, 4);
        ServerPlayer feeder = player(helper, 4, 2);
        golem.setEnergy(0.2f); // less than one decision cycle of passive drain (0.25)
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(golem.isAlive(), "running out of energy must not kill the golem");
            helper.assertTrue(golem.getEnergy().isEmpty(), "energy should be zero, is " + golem.getEnergy().getEnergy());
            helper.assertTrue(golem.getState() == ConstructEntity.State.POWERED_DOWN, "state is " + golem.getState());
            helper.assertTrue(golem.getNavigation().isDone(), "a powered-down golem must not be pathfinding");
            GolemSpec before = golem.getSpec();

            // Hand-feed a fuel (redstone = 60 in the default data). Anyone may do this.
            ItemStack fuel = new ItemStack(Items.REDSTONE, 3);
            feeder.setItemInHand(InteractionHand.MAIN_HAND, fuel);
            InteractionResult result = GolemLifecycle.interact(golem, feeder, fuel);
            helper.assertTrue(result.consumesAction(), "feeding fuel did nothing");
            helper.assertTrue(fuel.getCount() == 2, "one fuel item should be consumed");
            assertClose(helper, golem.getEnergy().getEnergy(), 60.0, "energy after one redstone");
            helper.assertTrue(golem.getState() == ConstructEntity.State.ACTIVATING, "a recharged golem should be waking up, is " + golem.getState());
            helper.assertTrue(before.equals(golem.getSpec()), "powering down changed the spec");
        });
        helper.runAfterDelay(41 + ConstructEntity.ACTIVATION_TICKS + 5, () -> {
            helper.assertTrue(golem.getState() == ConstructEntity.State.ACTIVE, "golem did not finish waking, is " + golem.getState());
            helper.succeed();
        });
    }

    /** The conditional Create: Wizardry fuel exists exactly when that mod is installed (SDD 9.2). */
    @GameTest(template = PLATFORM)
    public static void wizardryFuelIsConditional(GameTestHelper helper) {
        var fuels = helper.getLevel().registryAccess().registryOrThrow(ModRegistries.FUEL);
        boolean present = fuels.containsKey(Golemancy.id("create_wizardry/mana_bucket"));
        boolean loaded = ModList.get().isLoaded("create_wizardry");
        helper.assertTrue(present == loaded, "mana bucket fuel present=" + present + " but Create: Wizardry loaded=" + loaded);
        // The base path never depends on it.
        helper.assertTrue(fuels.containsKey(Golemancy.id("arcane_essence")) && fuels.containsKey(Golemancy.id("redstone")),
                "base fuels are missing");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------
    // Death, Remains, repair
    // ------------------------------------------------------------------------------------------

    private static GolemSpec remainsSpec(GameTestHelper helper) {
        ItemEntity dropped = GolemTestUtil.itemsInStructure(helper).stream()
                .filter(e -> e.getItem().is(ModItems.CONSTRUCT_REMAINS.get())).findFirst()
                .orElseThrow(() -> new IllegalStateException("no Construct Remains were dropped"));
        GolemSpec spec = dropped.getItem().get(ModDataComponents.GOLEM_SPEC);
        dropped.discard();
        return spec;
    }

    /** Death drops Remains with reduced integrity; at zero the core shatters (SDD 11.3). */
    @GameTest(template = PLATFORM)
    public static void deathDropsRemainsWithReducedIntegrity(GameTestHelper helper) {
        GolemSpec spec = withSpell(GolemTestUtil.spec("brute", "clay", "fire"), FIREBOLT, 2).withOwner(GolemTestUtil.someOwner());
        ConstructEntity golem = GolemTestUtil.deploy(helper, spec, 4, 4);
        golem.kill();
        helper.assertTrue(golem.isDeadOrDying(), "the golem did not die");
        GolemSpec remains = remainsSpec(helper);
        assertClose(helper, remains.coreIntegrity(), 0.75, "integrity after one death");
        helper.assertTrue(remains.frame().equals(spec.frame()) && remains.material().equals(spec.material())
                && remains.core().equals(spec.core()), "remains lost their design: " + remains);
        helper.assertTrue(remains.spells().equals(spec.spells()), "remains lost their spells");
        helper.assertTrue(GolemTestUtil.someOwner().equals(remains.owner()), "remains lost their owner");

        // A core at its last 25% shatters on the next death: frame kept, core and spells gone.
        ConstructEntity fragile = GolemTestUtil.deploy(helper, spec.withCoreIntegrity(0.25f), 4, 4);
        fragile.kill();
        GolemSpec shattered = remainsSpec(helper);
        helper.assertFalse(shattered.hasCore(), "the core should have shattered: " + shattered);
        helper.assertTrue(shattered.spells().isEmpty() && shattered.coreIntegrity() == 0f, "a shattered core keeps nothing");
        helper.assertTrue(shattered.frame().equals(spec.frame()) && shattered.material().equals(spec.material()),
                "the frame must survive a shattered core");
        // Remains cannot be deployed.
        helper.assertTrue(GolemLifecycle.deploy(GolemLifecycle.remainsStack(shattered), helper.getLevel(),
                helper.absolutePos(new BlockPos(4, 1, 4)), Direction.UP, null).isEmpty(), "shattered remains were deployable");
        helper.succeed();
    }

    /** Remains are rebuilt at the altar with repair material and fuel, not a new frame and core (SDD 11.3). */
    @GameTest(template = PLATFORM)
    public static void remainsAreRebuiltAtAltar(GameTestHelper helper) {
        GolemSpec damaged = withSpell(GolemTestUtil.spec("brute", "clay", "fire"), FIREBOLT, 1)
                .withOwner(GolemTestUtil.someOwner()).withCoreIntegrity(0.5f);
        ItemStack remains = GolemLifecycle.remainsStack(damaged);
        ItemStack clay = new ItemStack(Items.CLAY_BALL, 4);
        ItemStack fuel = new ItemStack(Items.REDSTONE);
        var level = helper.getLevel();

        Plan ok = BindingLogic.plan(level, remains, ItemStack.EMPTY, ItemStack.EMPTY, List.of(clay, fuel));
        helper.assertTrue(ok.ready() && ok.repair(), "rebuild should be ready: " + ok.status());
        GolemSpec rebuilt = ok.output().get(ModDataComponents.GOLEM_SPEC);
        helper.assertTrue(ok.output().is(ModItems.DORMANT_GOLEM.get()) && damaged.equals(rebuilt),
                "a plain rebuild returns the same spec, integrity unchanged: " + rebuilt);
        helper.assertTrue(ok.repairItems() == 4 && ok.consumeFuel() && !ok.consumeCore(), "wrong rebuild cost");

        helper.assertTrue(BindingLogic.plan(level, remains, ItemStack.EMPTY, ItemStack.EMPTY,
                List.of(new ItemStack(Items.CLAY_BALL, 3), fuel)).status() == Status.MISSING_REPAIR_MATERIAL, "3 of 4 repair items accepted");
        helper.assertTrue(BindingLogic.plan(level, remains, ItemStack.EMPTY, ItemStack.EMPTY,
                List.of(new ItemStack(Items.COBBLESTONE, 8), fuel)).status() == Status.MISSING_REPAIR_MATERIAL, "the wrong material's repair item was accepted");
        helper.assertTrue(BindingLogic.plan(level, remains, ItemStack.EMPTY, ItemStack.EMPTY,
                List.of(clay)).status() == Status.MISSING_FUEL, "rebuild without fuel accepted");

        // A new core of the same type is the dedicated repair that restores integrity; spells stay.
        Plan recored = BindingLogic.plan(level, remains, ManufacturingTests.core("fire"), ItemStack.EMPTY, List.of(clay, fuel));
        GolemSpec recoredSpec = recored.output().get(ModDataComponents.GOLEM_SPEC);
        helper.assertTrue(recored.ready() && recored.consumeCore() && recoredSpec.coreIntegrity() == 1f
                && recoredSpec.spells().equals(damaged.spells()), "a same-type core should restore integrity and keep spells: " + recoredSpec);

        // Shattered remains need a core; a different core type starts without spells.
        ItemStack shattered = GolemLifecycle.remainsStack(damaged.shattered());
        helper.assertTrue(BindingLogic.plan(level, shattered, ItemStack.EMPTY, ItemStack.EMPTY, List.of(clay, fuel)).status()
                == Status.MISSING_CORE, "shattered remains rebuilt without a core");
        Plan refit = BindingLogic.plan(level, shattered, ManufacturingTests.core("frost"), ItemStack.EMPTY, List.of(clay, fuel));
        GolemSpec refitSpec = refit.output().get(ModDataComponents.GOLEM_SPEC);
        helper.assertTrue(refit.ready() && refitSpec.core().equals(Golemancy.id("frost")) && refitSpec.coreIntegrity() == 1f
                && refitSpec.spells().isEmpty() && GolemTestUtil.someOwner().equals(refitSpec.owner()), "refit spec is wrong: " + refitSpec);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------
    // Pack up, redeploy, ownership
    // ------------------------------------------------------------------------------------------

    /** Packing up and redeploying keeps the spec, energy, health and name (SDD 11.2). */
    @GameTest(template = PLATFORM)
    public static void packUpAndRedeployKeepsState(GameTestHelper helper) {
        ServerPlayer owner = player(helper, 4, 2);
        GolemSpec spec = withSpell(GolemTestUtil.spec("guardian", "stone", "frost"), ResourceLocation.parse("irons_spellbooks:icicle"), 1)
                .withOwner(owner.getUUID());
        ConstructEntity golem = GolemTestUtil.deploy(helper, spec, 4, 4);
        golem.applySpec(golem.getSpec().withBehavior(golem.getSpec().behavior().withProfile(BehaviorConfig.Profile.SOLDIER).withRadius(9)));
        golem.setEnergy(777f);
        golem.setHealth(36f); // half of 72
        golem.setCustomName(Component.literal("Rocky"));
        GolemSpec before = golem.getSpec();

        // Sneak-use with the Control Wand packs it up into the owner's inventory.
        ItemStack wand = new ItemStack(ModItems.CONTROL_WAND.get());
        owner.setItemInHand(InteractionHand.MAIN_HAND, wand);
        owner.setShiftKeyDown(true);
        helper.assertTrue(GolemLifecycle.interact(golem, owner, wand).consumesAction(), "pack-up was refused");
        helper.assertTrue(golem.isRemoved(), "the golem is still in the world");
        ItemStack packed = ItemStack.EMPTY;
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            if (owner.getInventory().getItem(i).is(ModItems.DORMANT_GOLEM.get())) packed = owner.getInventory().getItem(i);
        }
        helper.assertTrue(!packed.isEmpty(), "no Dormant Golem in the owner's inventory");
        helper.assertTrue(before.equals(packed.get(ModDataComponents.GOLEM_SPEC)), "the packed spec differs");
        ConstructState state = packed.get(ModDataComponents.CONSTRUCT_STATE);
        helper.assertTrue(state != null && state.energy() == 777f && Math.abs(state.healthFraction() - 0.5f) < 0.01f
                && state.name().equals(Optional.of("Rocky")), "packed state is wrong: " + state);

        ConstructEntity redeployed = GolemTestUtil.deploy(helper, packed, 6, 6);
        assertClose(helper, redeployed.getEnergy().getEnergy(), 777.0, "energy after redeploy");
        assertClose(helper, redeployed.getHealth(), 36.0, "health after redeploy");
        helper.assertTrue(redeployed.getCustomName() != null && redeployed.getCustomName().getString().equals("Rocky"), "name lost");
        GolemSpec after = redeployed.getSpec();
        helper.assertTrue(after.spells().equals(before.spells()) && after.behavior().profile() == BehaviorConfig.Profile.SOLDIER
                && after.behavior().radius() == 9 && after.owner().equals(owner.getUUID()) && after.core().equals(before.core()),
                "spec changed across pack-up: " + after);
        helper.succeed();
    }

    /** Only the owner (or team, or an operator) can configure, pack up or deploy a golem (SDD 11.4, 16.2). */
    @GameTest(template = PLATFORM)
    public static void ownershipIsEnforced(GameTestHelper helper) {
        ServerPlayer owner = player(helper, 4, 2);
        ServerPlayer stranger = player(helper, 4, 6);
        ConstructEntity golem = GolemTestUtil.deploy(helper, GolemTestUtil.spec("guardian", "stone", "arcane").withOwner(owner.getUUID()), 4, 4);
        GolemSpec before = golem.getSpec();

        helper.assertTrue(Ownership.canControl(owner, golem), "the owner cannot control their golem");
        helper.assertFalse(Ownership.canControl(stranger, golem), "a stranger can control the golem");

        // Stranger: wand use, sneak pack-up and every screen request are refused, and nothing changes.
        ItemStack wand = new ItemStack(ModItems.CONTROL_WAND.get());
        stranger.setItemInHand(InteractionHand.MAIN_HAND, wand);
        helper.assertTrue(GolemLifecycle.interact(golem, stranger, wand) == InteractionResult.FAIL, "stranger could use the wand");
        stranger.setShiftKeyDown(true);
        helper.assertTrue(GolemLifecycle.interact(golem, stranger, wand) == InteractionResult.FAIL, "stranger could pack up the golem");
        helper.assertFalse(golem.isRemoved(), "the golem was packed up by a stranger");
        for (GolemRequests.Action action : GolemRequests.Action.values()) {
            helper.assertFalse(GolemRequests.apply(stranger, golem, action, 1), "stranger's request " + action + " was applied");
        }
        helper.assertTrue(before.equals(golem.getSpec()) && !golem.isRemoved(), "a stranger changed the golem");

        // Owner: requests work, but out-of-range values are still rejected.
        helper.assertTrue(GolemRequests.apply(owner, golem, GolemRequests.Action.SET_PROFILE, BehaviorConfig.Profile.SOLDIER.ordinal()), "owner request refused");
        helper.assertTrue(golem.getSpec().behavior().profile() == BehaviorConfig.Profile.SOLDIER, "profile was not changed");
        helper.assertTrue(GolemRequests.apply(owner, golem, GolemRequests.Action.SET_RADIUS, 20), "radius request refused");
        helper.assertTrue(golem.getSpec().behavior().radius() == 20, "radius was not changed");
        helper.assertFalse(GolemRequests.apply(owner, golem, GolemRequests.Action.SET_RADIUS, 4096), "an absurd radius was accepted");
        helper.assertFalse(GolemRequests.apply(owner, golem, GolemRequests.Action.SET_PROFILE, 99), "an invalid profile was accepted");
        helper.assertTrue(GolemRequests.apply(owner, golem, GolemRequests.Action.SET_COMBAT, 0)
                && !golem.getSpec().behavior().combat(), "combat toggle failed");
        // Too far away: even the owner's request is refused.
        owner.setPos(owner.getX() + 40, owner.getY(), owner.getZ());
        helper.assertFalse(GolemRequests.apply(owner, golem, GolemRequests.Action.SET_RADIUS, 10), "a request from 40 blocks away was applied");

        // A Dormant Golem bound to someone else cannot be deployed by a stranger.
        ItemStack owned = GolemLifecycle.dormantStack(before, ConstructState.FRESH);
        helper.assertTrue(GolemLifecycle.deploy(owned, helper.getLevel(), helper.absolutePos(new BlockPos(6, 1, 6)), Direction.UP, stranger).isEmpty(),
                "a stranger deployed someone else's golem");
        // An unbound one is claimed by whoever deploys it.
        ItemStack unbound = GolemLifecycle.dormantStack(GolemTestUtil.spec("brute", "clay", "arcane"), ConstructState.FRESH);
        ConstructEntity claimed = GolemLifecycle.deploy(unbound, helper.getLevel(), helper.absolutePos(new BlockPos(6, 1, 6)), Direction.UP, stranger).orElseThrow();
        helper.assertTrue(stranger.getUUID().equals(claimed.getOwnerUUID()), "the deployer did not become the owner");
        helper.succeed();
    }

    /** The per-player cap on active golems is enforced at deploy time (SDD 15.2). */
    @GameTest(template = ARENA)
    public static void activeGolemCapIsEnforced(GameTestHelper helper) {
        int max = GolemancyConfig.MAX_GOLEMS_PER_PLAYER.get();
        java.util.UUID owner = java.util.UUID.randomUUID();
        GolemSpec spec = GolemTestUtil.spec("guardian", "stone", "arcane").withOwner(owner);
        for (int i = 0; i < max; i++) {
            GolemTestUtil.deploy(helper, spec, 1 + (i % 4) * 3, 1 + (i / 4) * 3);
        }
        helper.assertTrue(Ownership.countActive(helper.getLevel().getServer(), owner) == max, "count is off");
        helper.assertTrue(GolemLifecycle.deploy(GolemLifecycle.dormantStack(spec, ConstructState.FRESH), helper.getLevel(),
                helper.absolutePos(new BlockPos(13, 1, 13)), Direction.UP, null).isEmpty(), "golem number " + (max + 1) + " was deployed");
        helper.succeed();
    }

    /** Field repair heals a fraction, costs energy and has a cooldown (SDD 11.3). */
    @GameTest(template = PLATFORM)
    public static void fieldRepairHasCostAndCooldown(GameTestHelper helper) {
        ServerPlayer anyone = player(helper, 4, 2);
        ConstructEntity golem = GolemTestUtil.deploy(helper, GolemTestUtil.spec("guardian", "stone", "arcane"), 4, 4);
        golem.setHealth(20f);
        float energy = golem.getEnergy().getEnergy();
        ItemStack stone = new ItemStack(Items.COBBLESTONE, 4);
        anyone.setItemInHand(InteractionHand.MAIN_HAND, stone);
        helper.assertTrue(GolemLifecycle.interact(golem, anyone, stone) == InteractionResult.SUCCESS, "field repair refused");
        assertClose(helper, golem.getHealth(), 20.0 + 72.0 * 0.25, "health after field repair");
        helper.assertTrue(stone.getCount() == 3, "repair item not consumed");
        helper.assertTrue(golem.getEnergy().getEnergy() < energy, "field repair cost no energy");
        // Immediately again: on cooldown, nothing consumed.
        helper.assertTrue(GolemLifecycle.interact(golem, anyone, stone) == InteractionResult.CONSUME, "repair during cooldown should do nothing");
        helper.assertTrue(stone.getCount() == 3, "an item was consumed during the cooldown");
        // The wrong material's item is not a repair item at all.
        ItemStack clay = new ItemStack(Items.CLAY_BALL);
        helper.assertTrue(GolemLifecycle.interact(golem, anyone, clay) == InteractionResult.PASS, "clay repaired a stone golem");
        helper.succeed();
    }
}
