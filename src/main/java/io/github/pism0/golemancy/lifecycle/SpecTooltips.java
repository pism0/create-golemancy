package io.github.pism0.golemancy.lifecycle;

import io.github.pism0.golemancy.data.ConstructState;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.data.InstalledSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Shared text for anything that describes a {@link GolemSpec}: item tooltips, JEI, the screen. */
public final class SpecTooltips {
    private SpecTooltips() {
    }

    public static MutableComponent material(ResourceLocation id) {
        return Component.translatable("golemancy.material." + id.toLanguageKey());
    }

    public static MutableComponent frame(ResourceLocation id) {
        return Component.translatable("golemancy.frame." + id.toLanguageKey());
    }

    public static MutableComponent core(ResourceLocation id) {
        return Component.translatable("golemancy.core." + id.toLanguageKey());
    }

    /** Spell names come from the spell's own namespace: {@code spell.<namespace>.<path>}. */
    public static MutableComponent spell(InstalledSpell spell) {
        return Component.translatable("golemancy.tooltip.spell_level",
                Component.translatable("spell." + spell.spell().getNamespace() + "." + spell.spell().getPath()),
                spell.level());
    }

    /** "Stone Guardian": material + frame (SDD 10.1 naming rule). */
    public static MutableComponent constructName(GolemSpec spec) {
        return Component.translatable("entity.golemancy.construct.name", material(spec.material()), frame(spec.frame()));
    }

    public static void append(List<Component> tooltip, GolemSpec spec, @Nullable ConstructState state, boolean remains) {
        tooltip.add(Component.translatable("golemancy.tooltip.core", core(spec.core()))
                .withStyle(spec.hasCore() ? ChatFormatting.GRAY : ChatFormatting.RED));
        if (spec.hasCore()) {
            tooltip.add(Component.translatable("golemancy.tooltip.integrity", Math.round(spec.coreIntegrity() * 100f))
                    .withStyle(spec.coreIntegrity() < 0.999f ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        }
        for (InstalledSpell spell : spec.spells()) {
            tooltip.add(Component.translatable("golemancy.tooltip.spell", spell(spell)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (state != null) {
            if (state.energy() < 0f) {
                tooltip.add(Component.translatable("golemancy.tooltip.energy_full").withStyle(ChatFormatting.AQUA));
            } else {
                tooltip.add(Component.translatable("golemancy.tooltip.energy", Math.round(state.energy())).withStyle(ChatFormatting.AQUA));
            }
            if (state.healthFraction() < 0.999f) {
                tooltip.add(Component.translatable("golemancy.tooltip.health", Math.round(state.healthFraction() * 100f))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        tooltip.add(Component.translatable(spec.hasOwner() ? "golemancy.tooltip.owned" : "golemancy.tooltip.unowned")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(remains
                ? (spec.hasCore() ? "golemancy.tooltip.remains_hint" : "golemancy.tooltip.remains_shattered_hint")
                : "golemancy.tooltip.dormant_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
