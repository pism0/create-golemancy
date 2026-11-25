package io.github.pism0.golemancy.gametest;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.core.ModDataComponents;
import io.github.pism0.golemancy.core.ModEntities;
import io.github.pism0.golemancy.core.ModItems;
import io.github.pism0.golemancy.data.BehaviorConfig;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.entity.ConstructEntity;
import io.github.pism0.golemancy.lifecycle.Ownership;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

import static io.github.pism0.golemancy.gametest.GolemTestUtil.ARENA;
import static io.github.pism0.golemancy.gametest.GolemTestUtil.PLATFORM;

/** Milestone 2 gate: a golem exists as an entity, persists its spec and fights. */
@GameTestHolder(Golemancy.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EntityTests {
    private EntityTests() {
    }

    private static void assertClose(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 0.01, what + ": expected " + expected + ", got " + actual);
    }

    /** The spawn item places the right entity type with the spec it carried. */
    @GameTest(template = PLATFORM)
    public static void dormantItemDeploysGolemWithSpec(GameTestHelper helper) {
        GolemSpec spec = GolemTestUtil.spec("brute", "clay", "fire").withOwner(GolemTestUtil.someOwner());
        ConstructEntity golem = GolemTestUtil.deploy(helper, spec, 4, 4);

        helper.assertTrue(golem.getType() == ModEntities.BRUTE.get(), "wrong entity type: " + golem.getType());
        helper.assertTrue(golem.getSpec().frame().equals(spec.frame()) && golem.getSpec().material().equals(spec.material())
                && golem.getSpec().core().equals(spec.core()), "spec was not copied: " + golem.getSpec());
        helper.assertTrue(GolemTestUtil.someOwner().equals(golem.getOwnerUUID()), "owner was not copied");
        helper.assertTrue(golem.getSpec().behavior().home().isPresent(), "deploy position should become home");
        // Brute 90 health x clay 1.0; armour 4 x 0.6; speed 0.22 x 0.85.
        assertClose(helper, golem.getMaxHealth(), 90.0, "max health");
        assertClose(helper, golem.getAttributeValue(Attributes.ARMOR), 2.4, "armour");
        assertClose(helper, golem.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.187, "movement speed");
        assertClose(helper, golem.getAttributeValue(Attributes.ATTACK_DAMAGE), 12.0, "attack damage");
        assertClose(helper, golem.getHealth(), 90.0, "health");
        helper.assertTrue(golem.getEnergy().isFull() && golem.getEnergy().getMaxEnergy() > 0, "a fresh golem starts with full energy");
        helper.succeed();
    }

    /** Attributes come from the spec and recalculating never stacks them (SDD 10.2). */
    @GameTest(template = PLATFORM)
    public static void attributesFollowSpecWithoutStacking(GameTestHelper helper) {
        ConstructEntity golem = GolemTestUtil.deploy(helper, GolemTestUtil.spec("guardian", "stone", "arcane"), 4, 4);
        // Guardian 60 x stone 1.2 = 72; armour 8 x 1.5 = 12.
        assertClose(helper, golem.getMaxHealth(), 72.0, "stone guardian health");
        assertClose(helper, golem.getAttributeValue(Attributes.ARMOR), 12.0, "stone guardian armour");
        for (int i = 0; i < 5; i++) golem.recalculate();
        assertClose(helper, golem.getMaxHealth(), 72.0, "health after repeated recalculation");
        assertClose(helper, golem.getAttributeValue(Attributes.ARMOR), 12.0, "armour after repeated recalculation");

        // Changing the spec changes the attributes; health keeps its fraction.
        golem.setHealth(36f);
        golem.applySpec(golem.getSpec().withIds(Golemancy.id("guardian"), Golemancy.id("clay"), Golemancy.id("arcane")));
        assertClose(helper, golem.getMaxHealth(), 60.0, "clay guardian health");
        assertClose(helper, golem.getHealth(), 30.0, "health fraction preserved");
        helper.succeed();
    }

    /** The spec, energy and health survive an entity save and load (chunk unload / restart). */
    @GameTest(template = PLATFORM)
    public static void golemPersistsThroughSaveAndLoad(GameTestHelper helper) {
        GolemSpec spec = GolemTestUtil.spec("guardian", "stone", "frost").withOwner(GolemTestUtil.someOwner())
                .withCoreIntegrity(0.5f)
                .withBehavior(new BehaviorConfig(BehaviorConfig.Profile.SOLDIER, Optional.of(new BlockPos(1, 2, 3)), 7, false,
                        BehaviorConfig.MoveMode.FOLLOW, false));
        ConstructEntity golem = GolemTestUtil.deploy(helper, spec, 4, 4);
        // Deploying resets home to the deploy position; put the test value back.
        golem.applySpec(golem.getSpec().withBehavior(spec.behavior()));
        golem.setEnergy(321f);
        golem.setHealth(55f);
        GolemSpec before = golem.getSpec();

        CompoundTag saved = new CompoundTag();
        helper.assertTrue(golem.saveAsPassenger(saved), "entity refused to save");
        golem.discard();

        Entity loaded = EntityType.loadEntityRecursive(saved, helper.getLevel(), e -> e);
        helper.assertTrue(loaded instanceof ConstructEntity, "loaded entity is not a construct: " + loaded);
        ConstructEntity reloaded = (ConstructEntity) loaded;
        helper.getLevel().addFreshEntity(reloaded);
        reloaded.tick();

        helper.assertTrue(before.equals(reloaded.getSpec()), "spec changed across save/load:\n" + before + "\n" + reloaded.getSpec());
        assertClose(helper, reloaded.getEnergy().getEnergy(), 321.0, "energy");
        assertClose(helper, reloaded.getHealth(), 55.0, "health");
        // Stone guardian: 72 health. Derived values are recalculated, not loaded.
        assertClose(helper, reloaded.getMaxHealth(), 72.0, "max health after load");
        helper.succeed();
    }

    /** SDD 15.3: data that cannot be decoded turns the golem into Remains instead of deleting it. */
    @GameTest(template = PLATFORM)
    public static void unreadableSpecBecomesRemains(GameTestHelper helper) {
        ConstructEntity golem = GolemTestUtil.deploy(helper,
                GolemTestUtil.spec("brute", "clay", "fire").withOwner(GolemTestUtil.someOwner()), 4, 4);
        CompoundTag saved = new CompoundTag();
        golem.saveAsPassenger(saved);
        golem.discard();

        // Corrupt the spec: the core field becomes a list, which no schema accepts.
        CompoundTag specTag = saved.getCompound("GolemSpec");
        specTag.put("core", new net.minecraft.nbt.ListTag());
        saved.put("GolemSpec", specTag);

        Entity loaded = EntityType.loadEntityRecursive(saved, helper.getLevel(), e -> e);
        helper.assertTrue(loaded instanceof ConstructEntity, "corrupt data must still produce an entity to convert");
        helper.getLevel().addFreshEntity(loaded);
        loaded.tick();

        helper.assertTrue(loaded.isRemoved(), "the unreadable golem should have been replaced");
        ItemEntity remains = GolemTestUtil.itemsInStructure(helper).stream()
                .filter(e -> e.getItem().is(ModItems.CONSTRUCT_REMAINS.get())).findFirst().orElse(null);
        helper.assertTrue(remains != null, "no Construct Remains were dropped");
        GolemSpec salvaged = remains.getItem().get(ModDataComponents.GOLEM_SPEC);
        helper.assertTrue(salvaged != null && salvaged.frame().equals(Golemancy.id("brute"))
                && salvaged.material().equals(Golemancy.id("clay")), "readable ids were not salvaged: " + salvaged);
        helper.assertTrue(GolemTestUtil.someOwner().equals(salvaged.owner()), "owner was not salvaged");
        helper.assertTrue(!salvaged.hasCore(), "the unreadable core must be treated as shattered");
        helper.succeed();
    }

    /** Combat AI: a Guardian notices a hostile inside its radius and attacks it. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void guardianAttacksHostile(GameTestHelper helper) {
        ConstructEntity golem = GolemTestUtil.deploy(helper, GolemTestUtil.spec("guardian", "stone", "arcane"), 4, 7);
        Husk husk = helper.spawn(EntityType.HUSK, 10, 2, 7);
        husk.setPersistenceRequired();
        helper.succeedWhen(() -> {
            helper.assertTrue(husk.getLastHurtByMob() == golem || !husk.isAlive(), "the husk has not been hit by the golem yet");
            // Stone strikes slow (material strike_slow_ticks).
            helper.assertTrue(!husk.isAlive() || husk.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),
                    "stone strikes should apply Slowness");
        });
    }

    /**
     * The Brute is wider (1.9) than vanilla melee reach is tuned for. Against a target that stays
     * put it must still close in and land hits, not stop just out of reach.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void bruteReachesAndHitsStationaryTarget(GameTestHelper helper) {
        ConstructEntity golem = GolemTestUtil.deploy(helper, GolemTestUtil.spec("brute", "clay", "arcane"), 3, 7);
        Husk husk = helper.spawn(EntityType.HUSK, 11, 2, 7);
        husk.setNoAi(true);
        husk.setPersistenceRequired();
        golem.setTarget(husk);
        helper.succeedWhen(() -> helper.assertTrue(husk.getLastHurtByMob() == golem || !husk.isAlive(),
                "the brute has not hit the stationary husk (distance " + String.format("%.2f", golem.distanceTo(husk)) + ")"));
    }

    /**
     * A wide golem that walks up to a target ends its path a little outside melee reach (the
     * navigation counts the last node as reached). It must close the remaining gap and hit. The
     * positions are the ones a Brute stalled at in game, 2.3 blocks from a pig.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void bruteClosesLastGapToTarget(GameTestHelper helper) {
        ConstructEntity golem = GolemTestUtil.deploy(helper, GolemTestUtil.spec("brute", "clay", "arcane"), 9, 9);
        net.minecraft.world.entity.animal.Pig pig = helper.spawn(EntityType.PIG, 9, 2, 6);
        pig.setNoAi(true);
        net.minecraft.world.phys.Vec3 stall = helper.absoluteVec(new net.minecraft.world.phys.Vec3(9.89, 2, 8.77));
        golem.setPos(stall.x, stall.y, stall.z);
        golem.setTarget(pig);
        helper.succeedWhen(() -> helper.assertTrue(pig.getLastHurtByMob() == golem || !pig.isAlive(),
                "the brute stalled short of the pig (distance " + String.format("%.2f", golem.distanceTo(pig)) + ")"));
    }

    /** A golem with combat switched off ignores hostiles. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void combatToggleIsRespected(GameTestHelper helper) {
        GolemSpec spec = GolemTestUtil.spec("guardian", "stone", "arcane");
        ConstructEntity golem = GolemTestUtil.deploy(helper, spec, 4, 7);
        golem.applySpec(golem.getSpec().withBehavior(golem.getSpec().behavior().withCombat(false)));
        Husk husk = helper.spawn(EntityType.HUSK, 10, 2, 7);
        husk.setNoAi(true);
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(golem.getTarget() == null, "a golem with combat off acquired a target");
            helper.assertTrue(husk.getHealth() == husk.getMaxHealth(), "a golem with combat off attacked");
            helper.succeed();
        });
    }

    /** Ownership: a golem never targets its owner and treats the owner as an ally. */
    @GameTest(template = PLATFORM)
    public static void golemNeverTargetsOwner(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        ConstructEntity golem = GolemTestUtil.deploy(helper,
                GolemTestUtil.spec("guardian", "stone", "arcane").withOwner(owner.getUUID()), 4, 4);

        helper.assertTrue(Ownership.isAlly(golem, owner), "owner is not an ally");
        helper.assertFalse(Ownership.mayAttack(golem, owner), "golem may attack its owner");
        golem.setTarget(owner);
        helper.assertTrue(golem.getTarget() == null, "golem accepted its owner as a target");

        // Golem PvP is off by default: other players are not valid targets either.
        helper.assertFalse(Ownership.mayAttack(golem, stranger), "golem may attack a player while golem PvP is off");
        // A second golem of the same owner is an ally; one with another owner is not.
        ConstructEntity sibling = GolemTestUtil.deploy(helper,
                GolemTestUtil.spec("brute", "clay", "arcane").withOwner(owner.getUUID()), 6, 6);
        ConstructEntity foreign = GolemTestUtil.deploy(helper,
                GolemTestUtil.spec("brute", "clay", "arcane").withOwner(stranger.getUUID()), 2, 6);
        helper.assertTrue(Ownership.isAlly(golem, sibling), "same-owner golems must be allies");
        helper.assertFalse(Ownership.isAlly(golem, foreign), "golems of different owners are not allies");
        helper.succeed();
    }
}
