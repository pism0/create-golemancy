package io.github.pism0.golemancy.core.datagen;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.core.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.Locale;

/** English names and tooltips for everything in the mod. */
public final class ModLangProvider extends LanguageProvider {
    public ModLangProvider(PackOutput output) {
        super(output, Golemancy.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.golemancy.main", "Create: Golemancy");

        // --- Parts ---------------------------------------------------------------------------
        for (String material : ModItems.MATERIALS) {
            String m = title(material);
            addItem(ModItems.part(material, "plate"), m + " Plate");
            addItem(ModItems.part(material, "limb"), m + " Limb");
            addItem(ModItems.part(material, "head"), m + " Head");
            addItem(ModItems.part(material, "torso_shell"), m + " Torso Shell");
            for (String frame : ModItems.FRAMES) {
                String name = m + " " + title(frame);
                addItem(ModItems.frame(material, frame), name + " Frame");
                addItem(ModItems.INCOMPLETE_FRAMES.get(material + "_" + frame), "Incomplete " + name + " Frame");
            }
            add("golemancy.material.golemancy." + material, m);
        }
        add("golemancy.tooltip.part", "Construct part");
        add("golemancy.tooltip.frame", "Bind with a core at a Binding Altar");

        // --- Cores ---------------------------------------------------------------------------
        addItem(ModItems.CORE_HOUSING, "Core Housing");
        addItem(ModItems.INCOMPLETE_CORE_HOUSING, "Incomplete Core Housing");
        addItem(ModItems.QUENCHED_CORE_HOUSING, "Quenched Core Housing");
        addItem(ModItems.MOLTEN_CORE_HOUSING, "Molten Core Housing");
        for (String core : ModItems.CORES) {
            addItem(ModItems.core(core), title(core) + " Core");
            add("golemancy.core.golemancy." + core, title(core) + " Core");
        }
        add("golemancy.core.golemancy.none", "Shattered");
        add("golemancy.tooltip.core.arcane", "Neutral core. Animates a construct; cannot cast spells.");
        add("golemancy.tooltip.core.fire", "Fire school. Burning strikes, fire resistance, one spell slot.");
        add("golemancy.tooltip.core.frost", "Ice school. Slowing strikes, frost resistance, one spell slot.");

        // --- Frames and profiles -------------------------------------------------------------
        for (String frame : ModItems.FRAMES) {
            add("golemancy.frame.golemancy." + frame, title(frame));
        }
        add("golemancy.profile.guardian", "Guardian");
        add("golemancy.profile.soldier", "Soldier");
        add("golemancy.profile.guardian.desc", "Holds its home position and engages hostiles inside the radius.");
        add("golemancy.profile.soldier.desc", "Follows its owner and attacks hostiles.");
        add("golemancy.move_mode.stay", "Stay");
        add("golemancy.move_mode.follow", "Follow");

        ModContentLang.add(this);
    }

    static String title(String id) {
        StringBuilder out = new StringBuilder();
        for (String word : id.split("_")) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }
}
