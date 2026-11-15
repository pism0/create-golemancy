package io.github.pism0.golemancy.core.datagen;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.core.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.Map;

/** Item models. Every texture referenced here is produced by tools/art (checked by ExistingFileHelper). */
public final class ModItemModelProvider extends ItemModelProvider {
    /** Items drawn from several layers; layers are tinted at runtime (see client.ItemTints). */
    private static final Map<String, List<String>> LAYERED = Map.of(
            "dormant_golem", List.of("dormant_golem_body", "dormant_golem_core", "dormant_golem_base"),
            "construct_remains", List.of("construct_remains_body", "construct_remains_core"));

    public ModItemModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, Golemancy.MOD_ID, files);
    }

    @Override
    protected void registerModels() {
        for (DeferredHolder<Item, ? extends Item> holder : ModItems.all()) {
            Item item = holder.get();
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            if (item instanceof BlockItem) {
                // Block items use the block model; generated with the block states.
                continue;
            }
            if (LAYERED.containsKey(path)) {
                var builder = withExistingParent("item/" + path, mcLoc("item/generated"));
                List<String> layers = LAYERED.get(path);
                for (int i = 0; i < layers.size(); i++) {
                    builder.texture("layer" + i, modLoc("item/" + layers.get(i)));
                }
            } else if (path.equals("control_wand")) {
                withExistingParent("item/" + path, mcLoc("item/handheld")).texture("layer0", modLoc("item/" + path));
            } else {
                withExistingParent("item/" + path, mcLoc("item/generated")).texture("layer0", modLoc("item/" + path));
            }
        }
    }
}
