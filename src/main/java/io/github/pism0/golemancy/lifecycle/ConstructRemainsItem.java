package io.github.pism0.golemancy.lifecycle;

import io.github.pism0.golemancy.core.ModDataComponents;
import io.github.pism0.golemancy.data.GolemSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * What a destroyed golem leaves behind (SDD 11.3): its spec with reduced core integrity. Rebuilt at
 * the Binding Altar with repair materials and fuel instead of a new frame and core.
 */
public class ConstructRemainsItem extends Item {
    public ConstructRemainsItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        GolemSpec spec = stack.get(ModDataComponents.GOLEM_SPEC);
        if (spec == null) return super.getName(stack);
        return Component.translatable("item.golemancy.construct_remains.named", SpecTooltips.constructName(spec));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GolemSpec spec = stack.get(ModDataComponents.GOLEM_SPEC);
        if (spec == null) {
            tooltip.add(Component.translatable("golemancy.tooltip.empty").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            return;
        }
        SpecTooltips.append(tooltip, spec, null, true);
    }
}
