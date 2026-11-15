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

        // --- Binding Altar --------------------------------------------------------------------------
        lang.addBlock(io.github.pism0.golemancy.core.ModBlocks.BINDING_ALTAR, "Binding Altar");
        lang.add("golemancy.altar.status.empty", "Waiting for a Construct Frame or Remains");
        lang.add("golemancy.altar.status.invalid_body", "This item cannot be bound");
        lang.add("golemancy.altar.status.missing_core", "Needs a Core");
        lang.add("golemancy.altar.status.invalid_core", "That is not a Core");
        lang.add("golemancy.altar.status.invalid_inscription", "The inscription cannot be used");
        lang.add("golemancy.altar.status.missing_repair_material", "Needs repair material");
        lang.add("golemancy.altar.status.missing_fuel", "Needs a fuel item");
        lang.add("golemancy.altar.status.ready", "Ready to bind");
        lang.add("golemancy.altar.status.output_ready", "Dormant Golem ready to collect");
        lang.add("golemancy.altar.inscription.not_inscription", "Only spell scrolls can be inscribed");
        lang.add("golemancy.altar.inscription.blank", "The scroll holds no spell");
        lang.add("golemancy.altar.inscription.not_allowed", "Golems cannot cast this spell");
        lang.add("golemancy.altar.inscription.no_slot", "This frame and core have no free spell slot");
        lang.add("golemancy.altar.inscription.wrong_school", "The spell's school does not match the core");
        lang.add("golemancy.altar.inscription.level_too_high", "The spell's level is too high for this core");
        lang.add("golemancy.altar.too_slow", "Needs at least %s RPM");
        lang.add("golemancy.altar.progress", "Binding: %s%%");
        lang.add("golemancy.altar.catalyst", "Catalyst: %1$s (%2$s mB)");

        // --- JEI ----------------------------------------------------------------------------------
        lang.add("golemancy.jei.binding", "Binding Altar");
        lang.add("golemancy.jei.bind", "Bind");
        lang.add("golemancy.jei.rebuild", "Rebuild");
        lang.add("golemancy.jei.scroll_optional", "Scroll is optional");
        lang.add("golemancy.jei.no_spell_slot", "This core cannot hold a spell");
        lang.add("golemancy.jei.rebuild.hint", "A new core restores integrity");
        lang.add("golemancy.jei.min_speed", "Min. %s RPM, shaft from below");
        lang.add("golemancy.jei.info.control_wand", "Use on a golem you own to configure it and link the wand. Sneak-use to pack the golem up into an item. Use a linked wand on a block to set the golem's home.");
        lang.add("golemancy.jei.info.binding_altar", "Powered by a shaft from below. Place a Construct Frame and a Core on it (plus an optional spell scroll) to bind a Dormant Golem, or Construct Remains with repair material and fuel to rebuild one. Accepts funnels, chutes, deployers and mechanical arms.");
    }
}
