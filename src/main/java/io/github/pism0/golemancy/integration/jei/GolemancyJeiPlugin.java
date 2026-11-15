package io.github.pism0.golemancy.integration.jei;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.block.BindingLogic;
import io.github.pism0.golemancy.block.BindingRecipe;
import io.github.pism0.golemancy.construction.MaterialDef;
import io.github.pism0.golemancy.core.GolemancyConfig;
import io.github.pism0.golemancy.core.ModDataComponents;
import io.github.pism0.golemancy.core.ModItems;
import io.github.pism0.golemancy.core.ModRecipes;
import io.github.pism0.golemancy.core.ModRegistries;
import io.github.pism0.golemancy.core.ModTags;
import io.github.pism0.golemancy.data.ConstructState;
import io.github.pism0.golemancy.data.GolemDefs;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.data.InstalledSpell;
import io.github.pism0.golemancy.integration.irons_spellbooks.IronsSpellCaster;
import io.github.pism0.golemancy.lifecycle.GolemLifecycle;
import io.github.pism0.golemancy.magic.CoreDef;
import io.github.pism0.golemancy.magic.SpellProfile;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * JEI integration (SDD 14.3): a Binding Altar category showing every frame × core combination and
 * how Construct Remains are rebuilt. Frame and core production lines need nothing here: they are
 * stock Create recipes, so Create's own JEI categories (sequenced assembly, filling, mixing, ...)
 * already list them, and looking up a frame or core in JEI leads straight to its full sequence.
 */
@JeiPlugin
public final class GolemancyJeiPlugin implements IModPlugin {
    public static final RecipeType<BindingDisplay> BINDING =
            RecipeType.create(Golemancy.MOD_ID, "binding", BindingDisplay.class);

    /**
     * One page in the Binding Altar category.
     *
     * @param body     frame, or Construct Remains
     * @param core     the core (may be empty for a rebuild that keeps its core)
     * @param extras   scrolls that may be inscribed (binding) or repair items (rebuilding)
     * @param fuel     fuel items (rebuilding only)
     * @param output   the resulting Dormant Golem
     * @param repair   true for a rebuild page
     * @param minSpeed minimum RPM
     */
    public record BindingDisplay(List<ItemStack> body, List<ItemStack> core, List<ItemStack> extras, List<ItemStack> fuel,
                                 ItemStack output, boolean repair, int minSpeed) {
    }

    @Override
    public ResourceLocation getPluginUid() {
        return Golemancy.id("jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Dormant Golems and Remains differ by their spec, so JEI must not merge them into one entry.
        for (Item item : List.of(ModItems.DORMANT_GOLEM.get(), ModItems.CONSTRUCT_REMAINS.get())) {
            registration.registerSubtypeInterpreter(item, new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<>() {
                @Override
                public Object getSubtypeData(ItemStack stack, UidContext context) {
                    GolemSpec spec = stack.get(ModDataComponents.GOLEM_SPEC);
                    return spec == null ? null : spec.frame() + "/" + spec.material() + "/" + spec.core();
                }

                @Override
                public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                    Object data = getSubtypeData(stack, context);
                    return data == null ? "" : data.toString();
                }
            });
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new BindingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.BINDING_ALTAR.get()), BINDING);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        try {
            registration.addRecipes(BINDING, buildDisplays(level));
        } catch (RuntimeException e) {
            // A recipe viewer must never be the reason the game fails to load.
            Golemancy.LOGGER.error("Could not build the Binding Altar JEI pages", e);
        }
        registration.addItemStackInfo(new ItemStack(ModItems.CONTROL_WAND.get()),
                Component.translatable("golemancy.jei.info.control_wand"));
        registration.addItemStackInfo(new ItemStack(ModItems.BINDING_ALTAR.get()),
                Component.translatable("golemancy.jei.info.binding_altar"));
    }

    private static List<BindingDisplay> buildDisplays(Level level) {
        RegistryAccess access = level.registryAccess();
        List<BindingDisplay> displays = new ArrayList<>();
        var cores = access.registry(ModRegistries.CORE).map(reg -> List.copyOf(reg.entrySet())).orElse(List.of());
        int minSpeedFloor = GolemancyConfig.SPEC.isLoaded() ? GolemancyConfig.ALTAR_MIN_SPEED.get() : BindingRecipe.DEFAULT_SPEED;

        for (RecipeHolder<BindingRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.BINDING.get())) {
            BindingRecipe recipe = holder.value();
            List<ItemStack> frames = Arrays.asList(recipe.frame().getItems());
            int minSpeed = Math.max(recipe.minSpeed(), minSpeedFloor);
            for (var coreEntry : cores) {
                CoreDef core = coreEntry.getValue();
                ResourceLocation coreId = coreEntry.getKey().location();
                GolemSpec spec = GolemSpec.create(recipe.golemFrame(), recipe.golemMaterial(), coreId);
                ItemStack coreStack = new ItemStack(core.item());
                displays.add(new BindingDisplay(frames, List.of(coreStack), inscribableScrolls(access, spec), List.of(),
                        GolemLifecycle.dormantStack(spec, ConstructState.FRESH), false, minSpeed));
            }

            // Rebuilding: the same frame as Remains, with its material's repair item and any fuel.
            GolemSpec base = GolemSpec.create(recipe.golemFrame(), recipe.golemMaterial(), GolemSpec.DEFAULT_CORE);
            Optional<MaterialDef> material = GolemDefs.material(access, recipe.golemMaterial());
            if (material.isEmpty()) continue;
            int count = (int) Math.ceil(BindingLogic.BASE_REPAIR_ITEMS
                    * (GolemancyConfig.SPEC.isLoaded() ? GolemancyConfig.REPAIR_COST_MULTIPLIER.get() : 1.0));
            List<ItemStack> repairItems = Arrays.stream(material.get().repairItem().getItems())
                    .map(stack -> stack.copyWithCount(Math.max(1, count))).toList();
            List<ItemStack> fuels = access.registry(ModRegistries.FUEL)
                    .map(reg -> reg.stream().flatMap(f -> Arrays.stream(f.ingredient().getItems())).toList())
                    .orElse(List.of());
            GolemSpec damaged = base.withCoreIntegrity(0.75f);
            displays.add(new BindingDisplay(List.of(GolemLifecycle.remainsStack(damaged)), List.of(), repairItems, fuels,
                    GolemLifecycle.dormantStack(damaged, ConstructState.FRESH), true, minSpeedFloor));
        }
        return displays;
    }

    /** One scroll per allow-listed spell that this frame and core would accept, at level 1. */
    private static List<ItemStack> inscribableScrolls(RegistryAccess access, GolemSpec spec) {
        Optional<Item> scrollItem = BuiltInRegistries.ITEM.getTag(ModTags.INSCRIPTIONS)
                .flatMap(tag -> tag.stream().findFirst()).map(holder -> holder.value());
        if (scrollItem.isEmpty()) return List.of();
        List<ItemStack> scrolls = new ArrayList<>();
        for (var entry : GolemDefs.spellProfiles(access)) {
            SpellProfile profile = entry.getValue();
            InstalledSpell spell = new InstalledSpell(profile.spell(), 1);
            if (BindingLogic.validateSpell(access, spec, spell).isPresent()) continue;
            ItemStack scroll = new ItemStack(scrollItem.get());
            if (IronsSpellCaster.inscribe(scroll, profile.spell(), 1)) {
                scrolls.add(scroll);
            }
        }
        return scrolls;
    }

    static List<ItemStack> itemStacks(List<ItemStack> stacks) {
        return stacks;
    }

    static mezz.jei.api.ingredients.IIngredientType<ItemStack> itemType() {
        return VanillaTypes.ITEM_STACK;
    }
}
