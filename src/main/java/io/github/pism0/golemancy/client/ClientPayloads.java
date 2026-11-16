package io.github.pism0.golemancy.client;

import io.github.pism0.golemancy.core.network.GolemNetwork;
import io.github.pism0.golemancy.entity.ConstructEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Client-side handling of server packets. Only ever loaded on the client. */
public final class ClientPayloads {
    private ClientPayloads() {
    }

    public static void openGolemScreen(GolemNetwork.OpenScreen payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(payload.entityId());
        if (!(entity instanceof ConstructEntity golem)) return;
        if (payload.refresh()) {
            // An answer to one of our own requests: update the open screen in place, keep the tab.
            if (mc.screen instanceof GolemScreen open && open.entityId() == payload.entityId()) {
                open.update(payload);
            }
            return;
        }
        mc.setScreen(new GolemScreen(golem, payload));
    }
}
