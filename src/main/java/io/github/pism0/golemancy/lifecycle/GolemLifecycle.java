package io.github.pism0.golemancy.lifecycle;

import com.mojang.serialization.Dynamic;
import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.construction.FrameDef;
import io.github.pism0.golemancy.construction.MaterialDef;
import io.github.pism0.golemancy.core.GolemancyConfig;
import io.github.pism0.golemancy.core.ModDataComponents;
import io.github.pism0.golemancy.core.ModItems;
import io.github.pism0.golemancy.data.ConstructState;
import io.github.pism0.golemancy.data.GolemDefs;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.entity.ConstructEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * The golem lifecycle (SDD 11): Dormant item ⇄ Active entity ⇄ Powered down, and Remains on death.
 * Everything here runs on the server and checks ownership before changing anything.
 */
public final class GolemLifecycle {
    private GolemLifecycle() {
    }

    /** Opens the configuration screen for a player. Replaced by the networking layer at startup. */
    public static BiConsumer<ServerPlayer, ConstructEntity> screenOpener = (player, golem) ->
            player.displayClientMessage(statusLine(golem), true);

    /** Wall-clock time each offline owner was last seen, for the abandoned-golem rule. */
    private static final Map<UUID, Long> LAST_SEEN = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------------------------------
    // Items
    // ------------------------------------------------------------------------------------------

    public static ItemStack dormantStack(GolemSpec spec, ConstructState state) {
        ItemStack stack = new ItemStack(ModItems.DORMANT_GOLEM.get());
        stack.set(ModDataComponents.GOLEM_SPEC, spec);
        stack.set(ModDataComponents.CONSTRUCT_STATE, state);
        return stack;
    }

    public static ItemStack remainsStack(GolemSpec spec) {
        ItemStack stack = new ItemStack(ModItems.CONSTRUCT_REMAINS.get());
        stack.set(ModDataComponents.GOLEM_SPEC, spec);
        return stack;
    }

    /** Remains for a golem that was just destroyed: integrity drops, and at zero the core shatters. */
    public static ItemStack remainsAfterDeath(ConstructEntity golem) {
        return remainsStack(afterDeath(golem.getSpec()));
    }

    public static GolemSpec afterDeath(GolemSpec spec) {
        float loss = GolemancyConfig.INTEGRITY_LOSS_ON_DEATH.get().floatValue();
        float integrity = spec.coreIntegrity() - loss;
        if (!spec.hasCore() || integrity <= 0.0001f) {
            return spec.shattered();
        }
        return spec.withCoreIntegrity(integrity);
    }

    /**
     * Best-effort recovery of a spec whose saved data would not decode (SDD 15.3): keep whatever
     * ids can be read, drop the rest. The result has a shattered core, so it must be rebuilt.
     */
    public static GolemSpec salvage(@Nullable Tag raw) {
        GolemSpec base = GolemSpec.create(GolemSpec.DEFAULT_FRAME, GolemSpec.DEFAULT_MATERIAL, GolemSpec.DEFAULT_CORE);
        if (raw == null) return base.shattered();
        Dynamic<Tag> data = new Dynamic<>(NbtOps.INSTANCE, raw);
        ResourceLocation frame = data.get("frame").asString().result().map(ResourceLocation::tryParse).orElse(null);
        ResourceLocation material = data.get("material").asString().result().map(ResourceLocation::tryParse).orElse(null);
        GolemSpec salvaged = base.withIds(frame == null ? GolemSpec.DEFAULT_FRAME : frame,
                material == null ? GolemSpec.DEFAULT_MATERIAL : material, GolemSpec.DEFAULT_CORE);
        Optional<UUID> owner = net.minecraft.core.UUIDUtil.CODEC.parse(data.get("owner").orElseEmptyMap()).result();
        if (owner.isPresent()) {
            salvaged = salvaged.withOwner(owner.get());
        }
        return salvaged.shattered();
    }

    // ------------------------------------------------------------------------------------------
    // Deploy and pack up
    // ------------------------------------------------------------------------------------------

    /**
     * Places the golem carried by a Dormant Golem item (SDD 4.2 step 5, 11.2).
     *
     * @param player the player using the item, or null / a fake player for machines
     * @return the new entity, or empty with a message sent to the player if it was refused
     */
    public static Optional<ConstructEntity> deploy(ItemStack stack, ServerLevel level, BlockPos clicked, Direction face,
                                                   @Nullable ServerPlayer player) {
        GolemSpec carried = stack.get(ModDataComponents.GOLEM_SPEC);
        if (carried == null) {
            return refuse(player, "golemancy.message.no_spec");
        }
        GolemSpec spec = GolemDefs.sanitize(carried, level.registryAccess());
        if (!spec.hasCore()) {
            return refuse(player, "golemancy.message.no_core");
        }
        boolean realPlayer = player != null && !(player instanceof FakePlayer);
        if (realPlayer) {
            if (spec.hasOwner() && !Ownership.canControl(player, spec.owner(), level.getServer())) {
                return refuse(player, "golemancy.message.not_owner");
            }
            if (!spec.hasOwner()) {
                spec = spec.withOwner(player.getUUID());
            }
        }
        if (spec.hasOwner()) {
            int max = GolemancyConfig.MAX_GOLEMS_PER_PLAYER.get();
            if (Ownership.countActive(level.getServer(), spec.owner()) >= max) {
                return refuse(player, "golemancy.message.too_many", max);
            }
        }
        Optional<FrameDef> frame = GolemDefs.frame(level.registryAccess(), spec.frame());
        if (frame.isEmpty()) {
            return refuse(player, "golemancy.message.no_spec");
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(frame.get().entityType());
        Entity created = type.create(level);
        if (!(created instanceof ConstructEntity golem)) {
            Golemancy.LOGGER.error("Frame '{}' names entity type '{}', which is not a construct", spec.frame(), frame.get().entityType());
            return refuse(player, "golemancy.message.no_spec");
        }
        BlockPos pos = clicked.relative(face);
        if (!level.noCollision(golem.getType().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(pos)))) {
            return refuse(player, "golemancy.message.no_room");
        }
        float yaw = player == null ? 0f : player.getYRot() + 180f;
        golem.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
        golem.setYHeadRot(yaw);
        golem.setYBodyRot(yaw);
        // The deploy position becomes home; the Control Wand or the screen can move it afterwards.
        spec = spec.withBehavior(spec.behavior().withHome(Optional.of(pos)));
        golem.loadFromItem(spec, stack.getOrDefault(ModDataComponents.CONSTRUCT_STATE, ConstructState.FRESH));
        level.addFreshEntity(golem);
        if (!golem.getEnergy().isEmpty()) {
            golem.beginActivation();
        }
        return Optional.of(golem);
    }

    /** Turns an active golem back into a Dormant Golem item, keeping spec, energy and health (SDD 11.2). */
    public static ItemStack packUp(ConstructEntity golem) {
        ItemStack stack = dormantStack(golem.getSpec(), golem.captureState());
        golem.discard();
        return stack;
    }

    // ------------------------------------------------------------------------------------------
    // Player interaction with a deployed golem
    // ------------------------------------------------------------------------------------------

    public static InteractionResult interact(ConstructEntity golem, ServerPlayer player, ItemStack held) {
        // Feeding fuel and patching up are harmless, so anyone (and any deployer) may do them.
        int fuel = GolemDefs.fuelEnergy(golem.level().registryAccess(), held);
        if (fuel > 0 && golem.getSpec().hasCore()) {
            if (golem.getEnergy().isFull()) {
                player.displayClientMessage(Component.translatable("golemancy.message.energy_full"), true);
                return InteractionResult.CONSUME;
            }
            golem.rechargeEnergy(fuel);
            if (!player.getAbilities().instabuild) held.shrink(1);
            golem.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
            player.displayClientMessage(statusLine(golem), true);
            return InteractionResult.SUCCESS;
        }
        MaterialDef material = golem.getMaterialDef();
        if (material != null && !held.isEmpty() && material.repairItem().test(held)) {
            if (golem.fieldRepair()) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                return InteractionResult.SUCCESS;
            }
            player.displayClientMessage(Component.translatable("golemancy.message.repair_not_ready"), true);
            return InteractionResult.CONSUME;
        }

        boolean wand = held.is(ModItems.CONTROL_WAND.get());
        boolean emptyHand = held.isEmpty();
        if (!wand && !emptyHand) return InteractionResult.PASS;

        // Everything below changes or reveals the golem's configuration: owner, team or operator only.
        if (!Ownership.canControl(player, golem)) {
            player.displayClientMessage(Component.translatable("golemancy.message.not_owner").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (golem.getOwnerUUID() == null && !(player instanceof FakePlayer)) {
            // An unowned golem (deployed by a machine) is claimed by the first player to handle it.
            golem.applySpec(golem.getSpec().withOwner(player.getUUID()));
        }
        if (player.isShiftKeyDown()) {
            if (wand || GolemancyConfig.EMPTY_HAND_PACK_UP.get()) {
                ItemStack packed = packUp(golem);
                if (!player.getInventory().add(packed)) {
                    player.drop(packed, false);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (wand) {
            ControlWandItem.link(held, golem);
        }
        screenOpener.accept(player, golem);
        return InteractionResult.SUCCESS;
    }

    public static Component statusLine(ConstructEntity golem) {
        int percent = Math.round(golem.getEnergy().getFraction() * 100f);
        return Component.translatable("golemancy.message.status", golem.getDisplayName(), percent,
                Math.round(golem.getHealth()), Math.round(golem.getMaxHealth()));
    }

    // ------------------------------------------------------------------------------------------
    // Abandoned golems
    // ------------------------------------------------------------------------------------------

    public static void onPlayerLoggedOut(UUID player) {
        LAST_SEEN.put(player, System.currentTimeMillis());
    }

    /** Whether {@code owner} is offline and has been for longer than {@code minutes} (SDD 11.4). */
    public static boolean ownerOfflineLongerThan(MinecraftServer server, UUID owner, int minutes) {
        if (server.getPlayerList().getPlayer(owner) != null) {
            LAST_SEEN.remove(owner);
            return false;
        }
        // First time we notice an absent owner (e.g. after a restart): start the clock now.
        long since = LAST_SEEN.computeIfAbsent(owner, id -> System.currentTimeMillis());
        return System.currentTimeMillis() - since > minutes * 60_000L;
    }

    private static Optional<ConstructEntity> refuse(@Nullable ServerPlayer player, String key, Object... args) {
        Golemancy.LOGGER.debug("Golem deploy refused: {}", key);
        if (player == null) {
            Golemancy.LOGGER.info("Golem deploy by a machine was refused: {}", key);
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), true);
        }
        return Optional.empty();
    }
}
