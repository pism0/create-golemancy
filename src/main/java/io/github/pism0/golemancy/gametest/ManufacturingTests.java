package io.github.pism0.golemancy.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.block.BindingLogic;
import io.github.pism0.golemancy.block.BindingLogic.Plan;
import io.github.pism0.golemancy.block.BindingLogic.Status;
import io.github.pism0.golemancy.core.ModBlocks;
import io.github.pism0.golemancy.core.ModDataComponents;
import io.github.pism0.golemancy.core.ModItems;
import io.github.pism0.golemancy.core.ModTags;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.data.InstalledSpell;
import io.github.pism0.golemancy.integration.create.BindingAltarBlock;
import io.github.pism0.golemancy.integration.create.BindingAltarBlockEntity;
import io.github.pism0.golemancy.integration.create.CreateIntegration;
import io.github.pism0.golemancy.integration.irons_spellbooks.IronsSpellCaster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.List;
import java.util.Optional;

import static io.github.pism0.golemancy.gametest.GolemTestUtil.PLATFORM;

/** Milestone 3 gate: parts, frames, cores and the Binding Altar, all automatable. */
@GameTestHolder(Golemancy.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ManufacturingTests {
    private ManufacturingTests() {
    }

    private static final ResourceLocation FIREBOLT = ResourceLocation.parse("irons_spellbooks:firebolt");
    private static final ResourceLocation ICICLE = ResourceLocation.parse("irons_spellbooks:icicle");

    /** An inscription item (whatever the golemancy:inscriptions tag holds) carrying the given spell. */
    static ItemStack scroll(ResourceLocation spell, int level) {
        Item item = BuiltInRegistries.ITEM.getTag(ModTags.INSCRIPTIONS)
                .flatMap(tag -> tag.stream().findFirst())
                .map(holder -> holder.value())
                .orElseThrow(() -> new IllegalStateException("golemancy:inscriptions is empty"));
        ItemStack stack = new ItemStack(item);
        if (!IronsSpellCaster.inscribe(stack, spell, level)) {
            throw new IllegalStateException("unknown spell " + spell);
        }
        return stack;
    }

    static ItemStack frame(String material, String frame) {
        return new ItemStack(ModItems.frame(material, frame).get());
    }

    static ItemStack core(String core) {
        return new ItemStack(ModItems.core(core).get());
    }

    // ------------------------------------------------------------------------------------------
    // Stock Create recipes (SDD 20.1 criteria 1-3)
    // ------------------------------------------------------------------------------------------

    /** Every manufacturing recipe is loaded, under the golemancy namespace, with the expected type. */
    @GameTest(template = PLATFORM)
    public static void manufacturingRecipesLoad(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        List<String> expected = List.of(
                "cutting/stone_plate", "pressing/clay_plate",
                "compacting/stone_limb", "compacting/clay_limb",
                "mechanical_crafting/stone_head", "mechanical_crafting/clay_head",
                "mechanical_crafting/stone_torso_shell", "mechanical_crafting/clay_torso_shell",
                "sequenced_assembly/core_housing",
                "sequenced_assembly/frame/stone_guardian", "sequenced_assembly/frame/stone_brute",
                "sequenced_assembly/frame/clay_guardian", "sequenced_assembly/frame/clay_brute",
                "filling/quenched_core_housing", "filling/molten_core_housing",
                "mixing/arcane_core", "mixing/fire_core", "mixing/frost_core",
                "binding/stone_guardian", "binding/stone_brute", "binding/clay_guardian", "binding/clay_brute",
                "crafting/binding_altar", "crafting/control_wand");
        for (String path : expected) {
            helper.assertTrue(recipes.byKey(Golemancy.id(path)).isPresent(), "recipe golemancy:" + path + " did not load");
        }
        // SDD 9: the Create: Wizardry recipe exists exactly when that mod is installed.
        boolean wizardry = net.neoforged.fml.ModList.get().isLoaded("create_wizardry");
        boolean compat = recipes.byKey(Golemancy.id("filling/compat/create_wizardry/arcane_core")).isPresent();
        helper.assertTrue(wizardry == compat, "Create: Wizardry recipe loaded=" + compat + " but mod present=" + wizardry);
        helper.succeed();
    }

    /** Runs a stack through Create's own sequenced-assembly lookup, step by step, the way belts and depots do. */
    private static ItemStack assemble(GameTestHelper helper, ItemStack start, List<ItemStack> steps) {
        var level = helper.getLevel();
        ItemStack current = start.copy();
        for (ItemStack held : steps) {
            ItemStack before = current;
            if (held.isEmpty()) {
                Optional<RecipeHolder<PressingRecipe>> press = SequencedAssemblyRecipe.getRecipe(level, before,
                        AllRecipeTypes.PRESSING.getType(), PressingRecipe.class);
                helper.assertTrue(press.isPresent(), "no pressing step applies to " + before);
                current = press.get().value().rollResults(level.random).getFirst();
            } else {
                ItemStackHandler slots = new ItemStackHandler(2);
                slots.setStackInSlot(0, before);
                slots.setStackInSlot(1, held);
                RecipeWrapper input = new RecipeWrapper(slots);
                Optional<RecipeHolder<DeployerApplicationRecipe>> deploy = SequencedAssemblyRecipe.getRecipe(level, input,
                        AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class);
                helper.assertTrue(deploy.isPresent(), "no deploying step applies to " + before + " with " + held);
                current = deploy.get().value().rollResults(level.random).getFirst();
            }
        }
        return current;
    }

    /** Both frames are built from the same torso shell; the first deployed item selects the recipe. */
    @GameTest(template = PLATFORM)
    public static void sequencedAssemblyBuildsFrames(GameTestHelper helper) {
        for (String material : ModItems.MATERIALS) {
            ItemStack shell = new ItemStack(ModItems.part(material, "torso_shell").get());
            ItemStack limb = new ItemStack(ModItems.part(material, "limb").get());
            ItemStack head = new ItemStack(ModItems.part(material, "head").get());
            ItemStack plate = new ItemStack(ModItems.part(material, "plate").get());
            ItemStack housing = new ItemStack(ModItems.CORE_HOUSING.get());

            ItemStack guardian = assemble(helper, shell, List.of(limb, limb, head, ItemStack.EMPTY, housing));
            helper.assertTrue(guardian.is(ModItems.frame(material, "guardian").get()), material + " guardian line produced " + guardian);

            ItemStack brute = assemble(helper, shell, List.of(plate, limb, limb, head, ItemStack.EMPTY, housing));
            helper.assertTrue(brute.is(ModItems.frame(material, "brute").get()), material + " brute line produced " + brute);
        }
        // Core housing: copper sheet, amethyst, press, arcane essence. The inputs come from the recipe itself,
        // so this test names no third-party item ids.
        SequencedAssemblyRecipe housing = (SequencedAssemblyRecipe) helper.getLevel().getRecipeManager()
                .byKey(Golemancy.id("sequenced_assembly/core_housing")).orElseThrow().value();
        ItemStack sheet = housing.getIngredient().getItems()[0];
        ItemStack essence = housing.getSequence().get(2).getRecipe().getIngredients().get(1).getItems()[0];
        ItemStack built = assemble(helper, sheet, List.of(new ItemStack(Items.AMETHYST_SHARD), ItemStack.EMPTY, essence));
        helper.assertTrue(built.is(ModItems.CORE_HOUSING.get()), "core housing line produced " + built);
        helper.succeed();
    }

    /**
     * M0 spike 1, pinned as a regression test: sequenced assembly does not carry extra data
     * components from step to step. If this ever starts failing, modular parts through stock
     * sequenced assembly have become possible and SDD 4.4 should be revisited.
     */
    @GameTest(template = PLATFORM)
    public static void sequencedAssemblyDiscardsExtraComponents(GameTestHelper helper) {
        ItemStack shell = new ItemStack(ModItems.part("stone", "torso_shell").get());
        shell.set(DataComponents.CUSTOM_NAME, Component.literal("spike-marker"));
        ItemStack limb = new ItemStack(ModItems.part("stone", "limb").get());
        ItemStack advanced = assemble(helper, shell, List.of(limb));
        helper.assertTrue(advanced.is(ModItems.INCOMPLETE_FRAMES.get("stone_guardian").get()), "unexpected transitional item " + advanced);
        helper.assertFalse(advanced.has(DataComponents.CUSTOM_NAME), "a custom component survived a sequenced assembly step");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------
    // Binding logic
    // ------------------------------------------------------------------------------------------

    private static Plan plan(GameTestHelper helper, ItemStack body, ItemStack core, ItemStack inscription) {
        return BindingLogic.plan(helper.getLevel(), body, core, inscription, List.of());
    }

    /** Valid inputs give a Dormant Golem with exactly the spec the inputs describe. */
    @GameTest(template = PLATFORM)
    public static void bindingBuildsTheRightSpec(GameTestHelper helper) {
        Plan plan = plan(helper, frame("clay", "brute"), core("frost"), scroll(ICICLE, 2));
        helper.assertTrue(plan.ready(), "plan is not ready: " + plan.status() + " " + plan.detailKey());
        helper.assertTrue(plan.output().is(ModItems.DORMANT_GOLEM.get()), "output is not a Dormant Golem");
        GolemSpec spec = plan.output().get(ModDataComponents.GOLEM_SPEC);
        helper.assertTrue(spec != null, "output has no spec");
        helper.assertTrue(spec.schema() == GolemSpec.CURRENT_SCHEMA, "schema " + spec.schema());
        helper.assertTrue(spec.frame().equals(Golemancy.id("brute")), "frame " + spec.frame());
        helper.assertTrue(spec.material().equals(Golemancy.id("clay")), "material " + spec.material());
        helper.assertTrue(spec.core().equals(Golemancy.id("frost")), "core " + spec.core());
        helper.assertTrue(spec.spells().equals(List.of(new InstalledSpell(ICICLE, 2))), "spells " + spec.spells());
        helper.assertTrue(spec.inscriptions().size() == 1, "inscriptions " + spec.inscriptions());
        helper.assertTrue(spec.coreIntegrity() == 1f && !spec.hasOwner(), "a fresh golem has full integrity and no owner");
        helper.assertTrue(spec.behavior().profile() == io.github.pism0.golemancy.data.BehaviorConfig.Profile.SOLDIER,
                "a brute should default to the Soldier profile");
        helper.assertTrue(plan.consumeCore() && plan.consumeInscription(), "core and inscription must be consumed");

        // No inscription is fine too, and an Arcane Core binds without spells.
        Plan plain = plan(helper, frame("stone", "guardian"), core("arcane"), ItemStack.EMPTY);
        helper.assertTrue(plain.ready() && plain.output().get(ModDataComponents.GOLEM_SPEC).spells().isEmpty(),
                "frame + arcane core should bind without a scroll");
        helper.succeed();
    }

    /** Invalid inputs are rejected with a reason and never produce an output (SDD 7.4). */
    @GameTest(template = PLATFORM)
    public static void bindingRejectsInvalidInputs(GameTestHelper helper) {
        ItemStack guardian = frame("stone", "guardian");

        record Case(String name, Plan plan, Status status, String detail) {
        }
        List<Case> cases = List.of(
                new Case("nothing", plan(helper, ItemStack.EMPTY, core("fire"), ItemStack.EMPTY), Status.EMPTY, ""),
                new Case("not a frame", plan(helper, new ItemStack(Items.DIRT), core("fire"), ItemStack.EMPTY), Status.INVALID_BODY, ""),
                new Case("no core", plan(helper, guardian, ItemStack.EMPTY, ItemStack.EMPTY), Status.MISSING_CORE, ""),
                new Case("not a core", plan(helper, guardian, new ItemStack(Items.DIAMOND), ItemStack.EMPTY), Status.INVALID_CORE, ""),
                new Case("not a scroll", plan(helper, guardian, core("fire"), new ItemStack(Items.PAPER)),
                        Status.INVALID_INSCRIPTION, "golemancy.altar.inscription.not_inscription"),
                new Case("arcane core has no spell slot", plan(helper, guardian, core("arcane"), scroll(FIREBOLT, 1)),
                        Status.INVALID_INSCRIPTION, "golemancy.altar.inscription.no_slot"),
                new Case("wrong school", plan(helper, guardian, core("fire"), scroll(ICICLE, 1)),
                        Status.INVALID_INSCRIPTION, "golemancy.altar.inscription.wrong_school"),
                new Case("level above the core cap", plan(helper, guardian, core("fire"), scroll(FIREBOLT, 9)),
                        Status.INVALID_INSCRIPTION, "golemancy.altar.inscription.level_too_high"),
                new Case("spell not on the allow-list", plan(helper, guardian, core("fire"),
                        scroll(ResourceLocation.parse("irons_spellbooks:fireball"), 1)),
                        Status.INVALID_INSCRIPTION, "golemancy.altar.inscription.not_allowed"));
        for (Case c : cases) {
            helper.assertTrue(c.plan.status() == c.status, c.name + ": expected " + c.status + ", got " + c.plan.status());
            helper.assertTrue(c.plan.detailKey().equals(c.detail), c.name + ": detail '" + c.plan.detailKey() + "'");
            helper.assertTrue(c.plan.output().isEmpty(), c.name + ": a rejected plan must not have an output");
        }
        // A school-neutral spell is accepted by any core that has a slot.
        Plan shield = plan(helper, guardian, core("fire"), scroll(ResourceLocation.parse("irons_spellbooks:shield"), 1));
        helper.assertTrue(shield.ready(), "school-neutral shield should be accepted by a fire core: " + shield.detailKey());
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------
    // The altar as a kinetic block
    // ------------------------------------------------------------------------------------------

    /** Places a creative motor pointing up with the altar on top; returns the altar. */
    private static BindingAltarBlockEntity placePoweredAltar(GameTestHelper helper, BlockPos motorPos, int rpm) {
        helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.UP));
        helper.setBlock(motorPos.above(), ModBlocks.BINDING_ALTAR.get());
        CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) helper.getBlockEntity(motorPos);
        motor.generatedSpeed.setValue(rpm);
        return (BindingAltarBlockEntity) helper.getBlockEntity(motorPos.above());
    }

    /** End to end at the block: items in through the item capability, shaft power from below, golem out. */
    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void altarBindsUnderRotation(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(4, 2, 4);
        BindingAltarBlockEntity altar = placePoweredAltar(helper, motorPos, 64);
        IItemHandler io = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(motorPos.above()), Direction.UP);
        helper.assertTrue(io != null, "the altar exposes no item handler");

        // Unrelated items are refused; valid ones are routed to their slots whatever slot is addressed.
        helper.assertTrue(io.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "dirt was accepted");
        helper.assertTrue(io.insertItem(0, core("fire"), false).isEmpty(), "core was refused");
        helper.assertTrue(io.insertItem(0, frame("stone", "guardian"), false).isEmpty(), "frame was refused");
        helper.assertTrue(io.insertItem(0, scroll(FIREBOLT, 1), false).isEmpty(), "scroll was refused");
        helper.assertTrue(io.insertItem(0, frame("stone", "guardian"), false).getCount() == 1, "a second frame was accepted");
        // Inputs cannot be pulled back out by automation.
        for (int slot = 0; slot < BindingAltarBlockEntity.INPUT_SLOTS; slot++) {
            helper.assertTrue(io.extractItem(slot, 1, true).isEmpty(), "automation could extract an input");
        }

        helper.succeedWhen(() -> {
            ItemStack result = altar.getOutput();
            helper.assertTrue(result.is(ModItems.DORMANT_GOLEM.get()), "no Dormant Golem yet (speed " + altar.getSpeed()
                    + ", status " + altar.getPlan().status() + ")");
            GolemSpec spec = result.get(ModDataComponents.GOLEM_SPEC);
            helper.assertTrue(spec != null && spec.frame().equals(Golemancy.id("guardian")) && spec.material().equals(Golemancy.id("stone"))
                    && spec.core().equals(Golemancy.id("fire")) && spec.spells().equals(List.of(new InstalledSpell(FIREBOLT, 1))),
                    "wrong spec on the output: " + spec);
            for (int slot = 0; slot < BindingAltarBlockEntity.INPUT_SLOTS; slot++) {
                helper.assertTrue(altar.getInput(slot).isEmpty(), "input slot " + slot + " was not consumed");
            }
            helper.assertFalse(altar.getBlockState().getValue(BindingAltarBlock.WORKING), "altar still shows as working");
            // The finished golem can be extracted by automation.
            helper.assertTrue(io.extractItem(BindingAltarBlockEntity.INPUT_SLOTS, 1, true).is(ModItems.DORMANT_GOLEM.get()),
                    "automation cannot extract the finished golem");
        });
    }

    /** Below the minimum speed nothing happens; raising the speed starts the binding. */
    @GameTest(template = PLATFORM, timeoutTicks = 500)
    public static void altarNeedsMinimumSpeed(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(4, 2, 4);
        BindingAltarBlockEntity altar = placePoweredAltar(helper, motorPos, 16);
        altar.insert(frame("clay", "guardian"), false);
        altar.insert(core("arcane"), false);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(altar.getPlan().ready(), "inputs should be valid");
            helper.assertTrue(altar.getOutput().isEmpty(), "the altar worked at 16 RPM, below its 32 RPM minimum");
            helper.assertTrue(altar.getProgressFraction() == 0f, "progress advanced below the minimum speed");
            ((CreativeMotorBlockEntity) helper.getBlockEntity(motorPos)).generatedSpeed.setValue(128);
        });
        helper.runAfterDelay(121, () -> helper.succeedWhen(() ->
                helper.assertTrue(altar.getOutput().is(ModItems.DORMANT_GOLEM.get()), "no output after speeding up")));
    }

    /** Fully hands-off: a hopper feeds the altar and the output is available to extraction (SDD 20.1 #4, #8). */
    @GameTest(template = PLATFORM, timeoutTicks = 500)
    public static void altarIsFedByAutomation(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(4, 2, 4);
        BindingAltarBlockEntity altar = placePoweredAltar(helper, motorPos, 64);
        BlockPos hopperPos = motorPos.above(2);
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(BlockStateProperties.FACING_HOPPER, Direction.DOWN));
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getBlockEntity(hopperPos);
        hopper.setItem(0, frame("stone", "brute"));
        hopper.setItem(1, core("frost"));
        hopper.setItem(2, scroll(ICICLE, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(altar.getOutput().is(ModItems.DORMANT_GOLEM.get()), "hopper-fed altar produced nothing yet");
            GolemSpec spec = altar.getOutput().get(ModDataComponents.GOLEM_SPEC);
            helper.assertTrue(spec.frame().equals(Golemancy.id("brute")) && spec.core().equals(Golemancy.id("frost"))
                    && spec.spells().size() == 1, "wrong spec from the automated line: " + spec);
        });
    }

    /** Mechanical arms can target the altar directly. */
    @GameTest(template = PLATFORM)
    public static void mechanicalArmCanTargetAltar(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        helper.setBlock(pos, ModBlocks.BINDING_ALTAR.get());
        ArmInteractionPointType type = ArmInteractionPointType.getPrimaryType(helper.getLevel(), helper.absolutePos(pos),
                helper.getBlockState(pos));
        helper.assertTrue(type == CreateIntegration.ALTAR_ARM_POINT.get(), "arm interaction point type is " + type);
        helper.succeed();
    }
}
