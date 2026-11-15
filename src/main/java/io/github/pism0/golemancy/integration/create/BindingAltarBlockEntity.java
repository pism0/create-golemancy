package io.github.pism0.golemancy.integration.create;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.item.ItemHelper;
import io.github.pism0.golemancy.block.BindingLogic;
import io.github.pism0.golemancy.block.BindingLogic.Plan;
import io.github.pism0.golemancy.block.BindingLogic.Slot;
import io.github.pism0.golemancy.block.BindingLogic.Status;
import io.github.pism0.golemancy.core.GolemancyConfig;
import io.github.pism0.golemancy.core.ModBlockEntities;
import io.github.pism0.golemancy.core.ModTags;
import io.github.pism0.golemancy.data.GolemDefs;
import io.github.pism0.golemancy.construction.MaterialDef;
import io.github.pism0.golemancy.core.ModDataComponents;
import io.github.pism0.golemancy.data.GolemSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Block entity of the Binding Altar. Holds the inputs, runs {@link BindingLogic} whenever they
 * change, and advances a binding while it is turning fast enough. All logic is server-side; the
 * client receives the items, progress and status for rendering and tooltips.
 */
public class BindingAltarBlockEntity extends KineticBlockEntity implements Clearable {
    // Input slots. BODY, CORE and INSCRIPTION hold one item; the two SUPPLY slots hold stacks.
    public static final int SLOT_BODY = 0;
    public static final int SLOT_CORE = 1;
    public static final int SLOT_INSCRIPTION = 2;
    public static final int SLOT_SUPPLY_A = 3;
    public static final int SLOT_SUPPLY_B = 4;
    public static final int INPUT_SLOTS = 5;
    /** mB of catalyst fluid one binding uses, if any is in the tank. */
    public static final int CATALYST_PER_BINDING = 250;
    public static final int TANK_CAPACITY = 1000;

    private final ItemStackHandler inputs = new ItemStackHandler(INPUT_SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return slot <= SLOT_INSCRIPTION ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            inputsChanged();
        }
    };
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            inputsChanged();
        }
    };
    private final FluidTank tank = new FluidTank(TANK_CAPACITY, stack -> stack.is(ModTags.BINDING_CATALYSTS)) {
        @Override
        protected void onContentsChanged() {
            inputsChanged();
        }
    };
    private final IItemHandler automation = new AutomationHandler();

    private Plan plan = Plan.empty();
    private boolean planDirty = true;
    private float progress;
    // Client-side mirrors (also written to disk so the tooltip is right immediately after loading).
    private int syncedStatus = Status.EMPTY.ordinal();
    private String syncedDetail = "";
    private int syncedTime;
    private int syncedMinSpeed;

    public BindingAltarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BINDING_ALTAR.get(),
                (altar, side) -> altar.automation);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.BINDING_ALTAR.get(),
                (altar, side) -> altar.tank);
    }

    // ------------------------------------------------------------------------------------------
    // Inventory
    // ------------------------------------------------------------------------------------------

    private boolean displayOnly;

    /**
     * Sets the visible contents directly, with no recipe logic and no network sync. Only for
     * Ponder scenes, whose virtual world has no recipes or connections.
     */
    public void setForDisplay(ItemStack body, ItemStack core, ItemStack inscription, ItemStack result) {
        displayOnly = true;
        inputs.setStackInSlot(SLOT_BODY, body.copy());
        inputs.setStackInSlot(SLOT_CORE, core.copy());
        inputs.setStackInSlot(SLOT_INSCRIPTION, inscription.copy());
        output.setStackInSlot(0, result.copy());
        displayOnly = false;
    }

    private void inputsChanged() {
        if (displayOnly) return;
        planDirty = true;
        setChanged();
        if (level != null && !level.isClientSide) {
            sendData();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    /**
     * Puts an item where it belongs (frame/remains, core, inscription or supplies). Used by hand,
     * deployers, funnels, chutes and arms alike.
     *
     * @return what could not be accepted
     */
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (level == null || stack.isEmpty()) return stack;
        Optional<Slot> slot = BindingLogic.slotFor(level, stack);
        if (slot.isEmpty()) return stack;
        return switch (slot.get()) {
            case BODY -> inputs.insertItem(SLOT_BODY, stack, simulate);
            case CORE -> inputs.insertItem(SLOT_CORE, stack, simulate);
            case INSCRIPTION -> inputs.insertItem(SLOT_INSCRIPTION, stack, simulate);
            case SUPPLY -> {
                // Keep repair material and fuel in separate slots: fill a matching stack first.
                ItemStack rest = stack;
                for (int s : new int[]{SLOT_SUPPLY_A, SLOT_SUPPLY_B}) {
                    if (ItemStack.isSameItemSameComponents(inputs.getStackInSlot(s), rest)) {
                        rest = inputs.insertItem(s, rest, simulate);
                    }
                }
                for (int s : new int[]{SLOT_SUPPLY_A, SLOT_SUPPLY_B}) {
                    if (!rest.isEmpty() && inputs.getStackInSlot(s).isEmpty()) {
                        rest = inputs.insertItem(s, rest, simulate);
                    }
                }
                yield rest;
            }
        };
    }

    /** Empty-hand use: the finished golem if there is one, otherwise every input. */
    public List<ItemStack> takeForPlayer() {
        List<ItemStack> taken = new ArrayList<>();
        if (!output.getStackInSlot(0).isEmpty()) {
            taken.add(output.extractItem(0, 64, false));
            return taken;
        }
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = inputs.extractItem(slot, 64, false);
            if (!stack.isEmpty()) taken.add(stack);
        }
        return taken;
    }

    public ItemStack getInput(int slot) {
        return inputs.getStackInSlot(slot);
    }

    public ItemStack getOutput() {
        return output.getStackInSlot(0);
    }

    public FluidTank getTank() {
        return tank;
    }

    public IItemHandler getAutomationHandler() {
        return automation;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < INPUT_SLOTS; slot++) inputs.setStackInSlot(slot, ItemStack.EMPTY);
        output.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, inputs);
        ItemHelper.dropContents(level, worldPosition, output);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    // ------------------------------------------------------------------------------------------
    // Processing
    // ------------------------------------------------------------------------------------------

    public Plan getPlan() {
        if (planDirty && level != null && !level.isClientSide) {
            planDirty = false;
            plan = BindingLogic.plan(level, inputs.getStackInSlot(SLOT_BODY), inputs.getStackInSlot(SLOT_CORE),
                    inputs.getStackInSlot(SLOT_INSCRIPTION),
                    List.of(inputs.getStackInSlot(SLOT_SUPPLY_A), inputs.getStackInSlot(SLOT_SUPPLY_B)));
            syncedStatus = plan.status().ordinal();
            syncedDetail = plan.detailKey();
            syncedTime = plan.processingTime();
            syncedMinSpeed = requiredSpeed(plan);
            progress = 0f; // inputs changed: start over
            sendData();
        }
        return plan;
    }

    private static int requiredSpeed(Plan plan) {
        return Math.max(plan.minSpeed(), GolemancyConfig.ALTAR_MIN_SPEED.get());
    }

    public boolean hasCatalyst() {
        return tank.getFluidAmount() >= CATALYST_PER_BINDING;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;
        if (level.isClientSide) {
            if (getBlockState().getValue(BindingAltarBlock.WORKING)) spawnWorkingParticles();
            return;
        }
        Plan current = getPlan();
        boolean canWork = current.ready() && output.getStackInSlot(0).isEmpty()
                && Math.abs(getSpeed()) >= requiredSpeed(current) && !isOverStressed();
        if (canWork) {
            float perTick = Math.abs(getSpeed()) / 32f * GolemancyConfig.PROCESSING_SPEED_MULTIPLIER.get().floatValue();
            if (hasCatalyst()) perTick *= 2f;
            progress += perTick;
            if (progress >= current.processingTime()) {
                finish(current);
            } else if (level.getGameTime() % 10 == 0) {
                sendData();
            }
        } else if (progress > 0f && !current.ready()) {
            progress = 0f;
            sendData();
        }
        setWorking(canWork);
    }

    private void setWorking(boolean working) {
        BlockState state = getBlockState();
        if (state.getValue(BindingAltarBlock.WORKING) != working) {
            level.setBlock(worldPosition, state.setValue(BindingAltarBlock.WORKING, working), 3);
        }
    }

    private void finish(Plan done) {
        // Take the result first, then consume: changing the inventory recomputes the plan.
        ItemStack result = done.output().copy();
        boolean catalyst = hasCatalyst();

        if (done.repair()) {
            consumeRepairSupplies(done);
        }
        if (done.consumeCore()) inputs.extractItem(SLOT_CORE, 1, false);
        if (done.consumeInscription()) inputs.extractItem(SLOT_INSCRIPTION, 1, false);
        inputs.extractItem(SLOT_BODY, 1, false);
        if (catalyst) tank.drain(CATALYST_PER_BINDING, IFluidHandler.FluidAction.EXECUTE);

        output.setStackInSlot(0, result);
        progress = 0f;
        planDirty = true;
        level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.9f, 0.8f);
        notifyUpdate();
    }

    private void consumeRepairSupplies(Plan done) {
        GolemSpec spec = done.output().get(ModDataComponents.GOLEM_SPEC);
        MaterialDef material = spec == null ? null : GolemDefs.material(level.registryAccess(), spec.material()).orElse(null);
        int repairLeft = done.repairItems();
        boolean fuelLeft = done.consumeFuel();
        for (int slot : new int[]{SLOT_SUPPLY_A, SLOT_SUPPLY_B}) {
            ItemStack stack = inputs.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (material != null && material.repairItem().test(stack)) {
                if (repairLeft > 0) {
                    repairLeft -= inputs.extractItem(slot, repairLeft, false).getCount();
                }
            } else if (fuelLeft && GolemDefs.fuelEnergy(level.registryAccess(), stack) > 0) {
                inputs.extractItem(slot, 1, false);
                fuelLeft = false;
            }
        }
    }

    private void spawnWorkingParticles() {
        if (level.random.nextInt(3) != 0) return;
        double angle = level.random.nextDouble() * Math.PI * 2;
        double radius = 0.6 + level.random.nextDouble() * 0.4;
        double x = worldPosition.getX() + 0.5 + Math.cos(angle) * radius;
        double z = worldPosition.getZ() + 0.5 + Math.sin(angle) * radius;
        level.addParticle(ParticleTypes.ENCHANT, x, worldPosition.getY() + 1.9, z,
                -Math.cos(angle) * radius, -0.7, -Math.sin(angle) * radius);
    }

    /** 0..1 for rendering and the tooltip. */
    public float getProgressFraction() {
        return syncedTime <= 0 ? 0f : Mth.clamp(progress / syncedTime, 0f, 1f);
    }

    public Status getStatus() {
        Status[] values = Status.values();
        return values[Mth.clamp(syncedStatus, 0, values.length - 1)];
    }

    public int comparatorSignal() {
        if (!output.getStackInSlot(0).isEmpty()) return 15;
        return Mth.floor(getProgressFraction() * 14f);
    }

    // ------------------------------------------------------------------------------------------
    // Feedback (SDD 14.2): hover tooltip without goggles, kinetic stats with goggles
    // ------------------------------------------------------------------------------------------

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToTooltip(tooltip, isPlayerSneaking);
        addStatus(tooltip);
        return true;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (tooltip.isEmpty()) addStatus(tooltip);
        if (!tank.isEmpty()) {
            tooltip.add(line(Component.translatable("golemancy.altar.catalyst", tank.getFluid().getHoverName(),
                    tank.getFluidAmount()).withStyle(ChatFormatting.AQUA)));
        }
        return true;
    }

    private void addStatus(List<Component> tooltip) {
        Status status = getStatus();
        tooltip.add(line(Component.translatable("block.golemancy.binding_altar").withStyle(ChatFormatting.WHITE)));
        if (!output.getStackInSlot(0).isEmpty()) {
            tooltip.add(line(Component.translatable("golemancy.altar.status.output_ready").withStyle(ChatFormatting.GREEN)));
            return;
        }
        tooltip.add(line(Component.translatable(status.translationKey())
                .withStyle(status == Status.READY ? ChatFormatting.GREEN : ChatFormatting.GOLD)));
        if (!syncedDetail.isEmpty()) {
            tooltip.add(line(Component.translatable(syncedDetail).withStyle(ChatFormatting.RED)));
        }
        if (status == Status.READY) {
            if (Math.abs(getSpeed()) < syncedMinSpeed) {
                tooltip.add(line(Component.translatable("golemancy.altar.too_slow", syncedMinSpeed).withStyle(ChatFormatting.RED)));
            } else {
                tooltip.add(line(Component.translatable("golemancy.altar.progress", Math.round(getProgressFraction() * 100f))
                        .withStyle(ChatFormatting.GRAY)));
            }
        }
    }

    private static Component line(Component content) {
        // Create's overlay expects a four-space indent on every line.
        return Component.literal("    ").append(content);
    }

    // ------------------------------------------------------------------------------------------
    // Save and sync
    // ------------------------------------------------------------------------------------------

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Inputs", inputs.serializeNBT(registries));
        tag.put("Output", output.serializeNBT(registries));
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("Progress", progress);
        tag.putInt("Status", syncedStatus);
        tag.putString("Detail", syncedDetail);
        tag.putInt("Time", syncedTime);
        tag.putInt("MinSpeed", syncedMinSpeed);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inputs.deserializeNBT(registries, tag.getCompound("Inputs"));
        output.deserializeNBT(registries, tag.getCompound("Output"));
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        progress = tag.getFloat("Progress");
        syncedStatus = tag.getInt("Status");
        syncedDetail = tag.getString("Detail");
        syncedTime = tag.getInt("Time");
        syncedMinSpeed = tag.getInt("MinSpeed");
        if (!clientPacket) {
            // Recompute from the items on the next tick, but keep the loaded progress.
            float loaded = progress;
            planDirty = true;
            pendingProgress = loaded;
        }
    }

    /** Progress read from disk, restored after the first plan computation (which resets progress). */
    private float pendingProgress = -1f;

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level != null && !level.isClientSide && pendingProgress >= 0f) {
            getPlan();
            if (plan.ready()) progress = Math.min(pendingProgress, plan.processingTime());
            pendingProgress = -1f;
        }
    }

    /**
     * What funnels, chutes, hoppers and mechanical arms see: slot 0..4 accept items (routed to the
     * right place regardless of which slot was addressed), slot 5 is the finished golem.
     */
    private final class AutomationHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return INPUT_SLOTS + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < INPUT_SLOTS ? inputs.getStackInSlot(slot) : output.getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= INPUT_SLOTS) return stack;
            return insert(stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Only the output can be pulled out, so a line never steals its own inputs back.
            return slot < INPUT_SLOTS ? ItemStack.EMPTY : output.extractItem(0, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < INPUT_SLOTS ? inputs.getSlotLimit(slot) : 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS && level != null && BindingLogic.slotFor(level, stack).isPresent();
        }
    }
}
