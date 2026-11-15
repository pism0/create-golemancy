package io.github.pism0.golemancy.integration.jei;

import io.github.pism0.golemancy.core.ModItems;
import io.github.pism0.golemancy.integration.jei.GolemancyJeiPlugin.BindingDisplay;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The Binding Altar page: [frame | remains] + [core] + [scroll | repair items] (+ fuel) → Dormant Golem. */
public final class BindingCategory implements IRecipeCategory<BindingDisplay> {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 58;
    private static final int ROW_Y = 14;

    private final IDrawable icon;
    private final IDrawable arrow;

    public BindingCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(ModItems.BINDING_ALTAR.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<BindingDisplay> getRecipeType() {
        return GolemancyJeiPlugin.BINDING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("golemancy.jei.binding");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BindingDisplay display, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 4, ROW_Y).setStandardSlotBackground()
                .addItemStacks(display.body());
        var core = builder.addSlot(RecipeIngredientRole.INPUT, 26, ROW_Y).setStandardSlotBackground();
        if (!display.core().isEmpty()) {
            core.addItemStacks(display.core());
        }
        var extras = builder.addSlot(RecipeIngredientRole.INPUT, 48, ROW_Y).setStandardSlotBackground();
        if (!display.extras().isEmpty()) {
            extras.addItemStacks(display.extras());
        }
        if (display.repair()) {
            var fuel = builder.addSlot(RecipeIngredientRole.INPUT, 70, ROW_Y).setStandardSlotBackground();
            if (!display.fuel().isEmpty()) {
                fuel.addItemStacks(display.fuel());
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 126, ROW_Y).setOutputSlotBackground()
                .addItemStack(display.output());
    }

    @Override
    public void draw(BindingDisplay display, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 96, ROW_Y);
        Font font = Minecraft.getInstance().font;
        Component top = Component.translatable(display.repair() ? "golemancy.jei.rebuild" : "golemancy.jei.bind");
        graphics.drawString(font, top, 4, 2, 0xFF404040, false);
        Component hint;
        if (display.repair()) {
            hint = Component.translatable("golemancy.jei.rebuild.hint");
        } else if (display.extras().isEmpty()) {
            hint = Component.translatable("golemancy.jei.no_spell_slot");
        } else {
            hint = Component.translatable("golemancy.jei.scroll_optional");
        }
        graphics.drawString(font, hint, 4, 36, 0xFF606060, false);
        graphics.drawString(font, Component.translatable("golemancy.jei.min_speed", display.minSpeed()), 4, 47, 0xFF606060, false);
    }
}
