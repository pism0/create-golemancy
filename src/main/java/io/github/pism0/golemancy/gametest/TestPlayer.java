package io.github.pism0.golemancy.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * A real {@link ServerPlayer} for tests that is never added to the player list. The vanilla mock
 * server player "joins" the server, which makes other mods (Iron's Spells) try to sync data over a
 * connection that does not exist. This one has no connection at all, so messages are dropped.
 * It is deliberately not a NeoForge FakePlayer: Golemancy treats those as machines.
 */
final class TestPlayer extends ServerPlayer {
    private TestPlayer(ServerLevel level, GameProfile profile) {
        super(level.getServer(), level, profile, ClientInformation.createDefault());
    }

    static TestPlayer create(ServerLevel level, String name) {
        return new TestPlayer(level, new GameProfile(UUID.randomUUID(), name));
    }

    @Override
    public void displayClientMessage(Component message, boolean actionBar) {
    }

    @Override
    public void sendSystemMessage(Component message) {
    }

    @Override
    public void sendSystemMessage(Component message, boolean overlay) {
    }
}
