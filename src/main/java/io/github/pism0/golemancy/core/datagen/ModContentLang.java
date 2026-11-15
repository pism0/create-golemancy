package io.github.pism0.golemancy.core.datagen;

import io.github.pism0.golemancy.core.ModEntities;
import io.github.pism0.golemancy.core.ModItems;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** Translations for content added after the foundation milestone (entities, altar, UI, JEI, Ponder). */
final class ModContentLang {
    private ModContentLang() {
    }

    static void add(LanguageProvider lang) {
        // --- Entities (SDD 10.1: "Material Frame", never a vanilla mob name) ---------------------
        lang.addEntityType(ModEntities.GUARDIAN, "Guardian");
        lang.addEntityType(ModEntities.BRUTE, "Brute");
        lang.add("entity.golemancy.construct.name", "%1$s %2$s");

        // --- Lifecycle items --------------------------------------------------------------------
        lang.addItem(ModItems.DORMANT_GOLEM, "Dormant Golem");
        lang.add("item.golemancy.dormant_golem.named", "Dormant %s");
        lang.addItem(ModItems.CONSTRUCT_REMAINS, "Construct Remains");
        lang.add("item.golemancy.construct_remains.named", "%s Remains");
        lang.addItem(ModItems.CONTROL_WAND, "Control Wand");

        lang.add("golemancy.tooltip.empty", "Empty");
        lang.add("golemancy.tooltip.core", "Core: %s");
        lang.add("golemancy.tooltip.integrity", "Core integrity: %s%%");
        lang.add("golemancy.tooltip.spell", "Spell: %s");
        lang.add("golemancy.tooltip.spell_level", "%1$s (level %2$s)");
        lang.add("golemancy.tooltip.energy", "Energy: %s");
        lang.add("golemancy.tooltip.energy_full", "Energy: full");
        lang.add("golemancy.tooltip.health", "Health: %s%%");
        lang.add("golemancy.tooltip.owned", "Bound to an owner");
        lang.add("golemancy.tooltip.unowned", "Unbound: the first player to deploy it becomes its owner");
        lang.add("golemancy.tooltip.dormant_hint", "Use on a block to deploy");
        lang.add("golemancy.tooltip.remains_hint", "Rebuild at a Binding Altar with repair material and fuel");
        lang.add("golemancy.tooltip.remains_shattered_hint", "Core shattered: rebuild at a Binding Altar with a new core");
        lang.add("golemancy.tooltip.wand.use", "Use on a golem: configure");
        lang.add("golemancy.tooltip.wand.sneak", "Sneak-use on a golem: pack up");
        lang.add("golemancy.tooltip.wand.block", "Use on a block: set the linked golem's home");
        lang.add("golemancy.tooltip.wand.linked", "Linked to a golem");

        lang.add("golemancy.message.no_spec", "This golem has no usable design");
        lang.add("golemancy.message.no_core", "This construct has no core. Rebuild it at a Binding Altar");
        lang.add("golemancy.message.not_owner", "This golem is not yours");
        lang.add("golemancy.message.too_many", "You already have %s active golems");
        lang.add("golemancy.message.no_room", "Not enough room to deploy here");
        lang.add("golemancy.message.energy_full", "Energy is already full");
        lang.add("golemancy.message.repair_not_ready", "Cannot repair right now");
        lang.add("golemancy.message.status", "%1$s: energy %2$s%%, health %3$s/%4$s");
        lang.add("golemancy.message.wand_unlinked", "Use the wand on a golem first");
        lang.add("golemancy.message.wand_lost", "The linked golem is not nearby");
        lang.add("golemancy.message.home_too_far", "Home must be within %s blocks of the golem");
        lang.add("golemancy.message.home_set", "Home set to %1$s, %2$s, %3$s");
    }
}
