package io.github.pism0.golemancy.core;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.entity.ConstructEntity;
import io.github.pism0.golemancy.lifecycle.GolemLifecycle;
import io.github.pism0.golemancy.lifecycle.Ownership;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Server-side event wiring: attributes, friendly fire and the abandoned-golem clock. */
@EventBusSubscriber(modid = Golemancy.MOD_ID)
public final class CommonEvents {
    private CommonEvents() {
    }

    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GUARDIAN.get(), ConstructEntity.createAttributes().build());
        event.put(ModEntities.BRUTE.get(), ConstructEntity.createAttributes().build());
    }

    /**
     * Friendly fire (SDD 11.4): nothing a golem does, melee or spell, ever damages its owner, the
     * owner's team or allied golems, and it only damages other players when golem PvP is allowed.
     * Spell projectiles record the caster as their owner, so this covers Iron's spells too.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ConstructEntity golem)) return;
        LivingEntity victim = event.getEntity();
        if (victim == golem) return;
        if (Ownership.isAlly(golem, victim) || (victim instanceof Player && !Ownership.mayAttack(golem, victim))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        GolemLifecycle.onPlayerLoggedOut(event.getEntity().getUUID());
    }
}
