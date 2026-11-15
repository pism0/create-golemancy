package io.github.pism0.golemancy.lifecycle;

import io.github.pism0.golemancy.core.ModDataComponents;
import io.github.pism0.golemancy.data.ConstructState;
import io.github.pism0.golemancy.data.GolemSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * A golem in item form (SDD 11.1). Carries its {@link GolemSpec} and {@link ConstructState} as data
 * components; using it on a block places the entity. Stacks to 1 so it can be stored and moved by
 * Create logistics.
 */
public class DormantGolemItem extends Item {
    public DormantGolemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        ServerPlayer player = context.getPlayer() instanceof ServerPlayer sp ? sp : null;
        var deployed = GolemLifecycle.deploy(stack, level, context.getClickedPos(), context.getClickedFace(), player);
        if (deployed.isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getName(ItemStack stack) {
        GolemSpec spec = stack.get(ModDataComponents.GOLEM_SPEC);
        if (spec == null) return super.getName(stack);
        return Component.translatable("item.golemancy.dormant_golem.named", SpecTooltips.constructName(spec));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GolemSpec spec = stack.get(ModDataComponents.GOLEM_SPEC);
        if (spec == null) {
            tooltip.add(Component.translatable("golemancy.tooltip.empty").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            return;
        }
        SpecTooltips.append(tooltip, spec, stack.getOrDefault(ModDataComponents.CONSTRUCT_STATE, ConstructState.FRESH), false);
    }
}
