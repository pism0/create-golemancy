package io.github.pism0.golemancy.core;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import io.github.pism0.golemancy.Golemancy;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every Golemancy item. Parts, housings and frames are plain items; behaviour lives in the lifecycle items. */
public final class ModItems {
    private ModItems() {
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Golemancy.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Golemancy.MOD_ID);

    /** Body materials shipped in v0.1. The material definitions themselves are datapack data. */
    public static final List<String> MATERIALS = List.of("stone", "clay");
    /** Frame types shipped in v0.1. */
    public static final List<String> FRAMES = List.of("guardian", "brute");
    /** Part kinds made for every material. */
    public static final List<String> PART_KINDS = List.of("plate", "limb", "head", "torso_shell");
    /** Cores shipped in v0.1. */
    public static final List<String> CORES = List.of("arcane", "fire", "frost");

    /** Items in creative-tab order. */
    private static final List<DeferredItem<? extends Item>> TAB_ORDER = new ArrayList<>();

    // --- Parts: <material>_<kind> ------------------------------------------------------------
    public static final Map<String, DeferredItem<Item>> PARTS = new LinkedHashMap<>();

    static {
        for (String material : MATERIALS) {
            for (String kind : PART_KINDS) {
                String name = material + "_" + kind;
                PARTS.put(name, simple(name));
            }
        }
    }

    // --- Core housing and its intermediates ----------------------------------------------------
    public static final DeferredItem<Item> CORE_HOUSING = simple("core_housing");
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_CORE_HOUSING = transitional("incomplete_core_housing");
    /** Spout-filled with water: the base for Arcane and Frost cores. */
    public static final DeferredItem<Item> QUENCHED_CORE_HOUSING = simple("quenched_core_housing");
    /** Spout-filled with lava: the base for Fire cores. */
    public static final DeferredItem<Item> MOLTEN_CORE_HOUSING = simple("molten_core_housing");

    // --- Cores: core/<type> ----------------------------------------------------------------------
    public static final Map<String, DeferredItem<Item>> CORE_ITEMS = new LinkedHashMap<>();

    static {
        for (String core : CORES) {
            CORE_ITEMS.put(core, track(ITEMS.register("core/" + core,
                    () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)))));
        }
    }

    // --- Frames: frame/<material>_<frame>, incomplete_frame/<material>_<frame> -------------------
    public static final Map<String, DeferredItem<Item>> FRAME_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<SequencedAssemblyItem>> INCOMPLETE_FRAMES = new LinkedHashMap<>();

    static {
        for (String material : MATERIALS) {
            for (String frame : FRAMES) {
                String name = material + "_" + frame;
                FRAME_ITEMS.put(name, track(ITEMS.register("frame/" + name,
                        () -> new Item(new Item.Properties().stacksTo(16)))));
            }
        }
        for (String material : MATERIALS) {
            for (String frame : FRAMES) {
                String name = material + "_" + frame;
                INCOMPLETE_FRAMES.put(name, transitional("incomplete_frame/" + name));
            }
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.golemancy.main"))
                    .icon(() -> new ItemStack(CORE_ITEMS.get("fire").get()))
                    .displayItems((params, output) -> TAB_ORDER.forEach(item -> output.accept(item.get())))
                    .build());

    public static DeferredItem<Item> part(String material, String kind) {
        return PARTS.get(material + "_" + kind);
    }

    public static DeferredItem<Item> frame(String material, String frame) {
        return FRAME_ITEMS.get(material + "_" + frame);
    }

    public static DeferredItem<Item> core(String core) {
        return CORE_ITEMS.get(core);
    }

    private static DeferredItem<Item> simple(String name) {
        return track(ITEMS.registerSimpleItem(name));
    }

    private static DeferredItem<SequencedAssemblyItem> transitional(String name) {
        // Not tracked: transitional items stay out of the creative tab, like Create's own.
        return ITEMS.register(name, () -> new SequencedAssemblyItem(new Item.Properties()));
    }

    static <T extends Item> DeferredItem<T> track(DeferredItem<T> item) {
        TAB_ORDER.add(item);
        return item;
    }

    /** Every registered item, for datagen. */
    public static List<DeferredHolder<Item, ? extends Item>> all() {
        return new ArrayList<>(ITEMS.getEntries());
    }
}
