package io.github.pism0.golemancy.lifecycle;

import io.github.pism0.golemancy.core.ModDataComponents;
import io.github.pism0.golemancy.entity.ConstructEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Control Wand (SDD 12.2). Used on a golem it opens the configuration screen and links the
 * wand to that golem; sneak-used on a golem it packs the golem up; used on a block while linked it
 * sets the golem's home there. The golem-side cases are handled in
 * {@link GolemLifecycle#interact}; all checks run on the server.
 */
public class ControlWandItem extends Item {
    /** How far from the golem a new home may be set. */
    public static final int MAX_HOME_DISTANCE = 64;

    public ControlWandItem(Properties properties) {
        super(properties);
    }

    /** Remembers which golem this wand last handled, so "use on block" knows whose home to set. */
    public static void link(ItemStack wand, ConstructEntity golem) {
        wand.set(ModDataComponents.LINKED_GOLEM, golem.getUUID());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack wand = context.getItemInHand();
        UUID linked = wand.get(ModDataComponents.LINKED_GOLEM);
        if (linked == null) {
            player.displayClientMessage(Component.translatable("golemancy.message.wand_unlinked"), true);
            return InteractionResult.FAIL;
        }
        Entity entity = level.getEntity(linked);
        if (!(entity instanceof ConstructEntity golem) || !golem.isAlive()) {
            player.displayClientMessage(Component.translatable("golemancy.message.wand_lost"), true);
            return InteractionResult.FAIL;
        }
        if (!Ownership.canControl(player, golem)) {
            player.displayClientMessage(Component.translatable("golemancy.message.not_owner").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        BlockPos home = context.getClickedPos().relative(context.getClickedFace());
        if (!home.closerThan(golem.blockPosition(), MAX_HOME_DISTANCE)) {
            player.displayClientMessage(Component.translatable("golemancy.message.home_too_far", MAX_HOME_DISTANCE), true);
            return InteractionResult.FAIL;
        }
        golem.applySpec(golem.getSpec().withBehavior(golem.getSpec().behavior().withHome(Optional.of(home))));
        player.displayClientMessage(Component.translatable("golemancy.message.home_set",
                home.getX(), home.getY(), home.getZ()), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("golemancy.tooltip.wand.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("golemancy.tooltip.wand.sneak").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("golemancy.tooltip.wand.block").withStyle(ChatFormatting.GRAY));
        if (stack.has(ModDataComponents.LINKED_GOLEM)) {
            tooltip.add(Component.translatable("golemancy.tooltip.wand.linked").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
