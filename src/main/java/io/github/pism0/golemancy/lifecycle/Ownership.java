package io.github.pism0.golemancy.lifecycle;

import io.github.pism0.golemancy.core.GolemancyConfig;
import io.github.pism0.golemancy.entity.ConstructEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Ownership and permission rules (SDD 11.4). Every check runs on the server; the client never
 * authorises anything (SDD 16.2).
 */
public final class Ownership {
    private Ownership() {
    }

    /** Operator permission level that may control any golem. */
    public static final int OPERATOR_LEVEL = 2;

    /**
     * Whether {@code player} may configure, pack up or otherwise change {@code golem}: the owner,
     * a member of the owner's team, or an operator. Unowned golems can be claimed by anyone.
     */
    public static boolean canControl(Player player, ConstructEntity golem) {
        return canControl(player, golem.getOwnerUUID(), golem.level().getServer());
    }

    public static boolean canControl(Player player, @Nullable UUID owner, @Nullable MinecraftServer server) {
        if (owner == null) return true;
        if (owner.equals(player.getUUID())) return true;
        if (player.hasPermissions(OPERATOR_LEVEL)) return true;
        return sameTeam(player, owner, server);
    }

    /** Whether {@code player} shares a scoreboard team with the player whose id is {@code owner}. */
    public static boolean sameTeam(Player player, UUID owner, @Nullable MinecraftServer server) {
        if (server == null) return false;
        PlayerTeam team = player.getScoreboard().getPlayersTeam(player.getScoreboardName());
        if (team == null) return false;
        ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(owner);
        if (ownerPlayer != null) {
            return team.getPlayers().contains(ownerPlayer.getScoreboardName());
        }
        // Owner offline: resolve the name from the profile cache so team membership still counts.
        return server.getProfileCache() != null && server.getProfileCache().get(owner)
                .map(profile -> team.getPlayers().contains(profile.getName())).orElse(false);
    }

    /**
     * Whether {@code other} is on {@code golem}'s side: its owner, the owner's team, or another
     * construct with the same or an allied owner. Golems never target or damage allies.
     */
    public static boolean isAlly(ConstructEntity golem, Entity other) {
        UUID owner = golem.getOwnerUUID();
        if (other == golem) return true;
        if (other instanceof ConstructEntity otherGolem) {
            UUID otherOwner = otherGolem.getOwnerUUID();
            if (owner == null || otherOwner == null) return owner == otherOwner;
            if (owner.equals(otherOwner)) return true;
            ServerPlayer otherPlayer = golem.level().getServer() == null ? null
                    : golem.level().getServer().getPlayerList().getPlayer(otherOwner);
            return otherPlayer != null && sameTeam(otherPlayer, owner, golem.level().getServer());
        }
        if (owner == null) return false;
        if (other instanceof Player player) {
            return owner.equals(player.getUUID()) || sameTeam(player, owner, golem.level().getServer());
        }
        // Pets and other owned creatures of the owner are allies too.
        if (other instanceof net.minecraft.world.entity.OwnableEntity ownable) {
            return owner.equals(ownable.getOwnerUUID());
        }
        return false;
    }

    /**
     * Whether {@code golem} may attack {@code target} at all. Hostile mobs always; anything that
     * attacked the golem or its owner; players only when golem PvP and server PvP are both on.
     */
    public static boolean mayAttack(ConstructEntity golem, LivingEntity target) {
        if (!target.isAlive() || target.isRemoved() || isAlly(golem, target)) return false;
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) return false;
            MinecraftServer server = golem.level().getServer();
            return GolemancyConfig.GOLEM_PVP.get() && server != null && server.isPvpAllowed();
        }
        return true;
    }

    /** Whether {@code target} is something a golem engages on its own initiative. */
    public static boolean isHostile(ConstructEntity golem, LivingEntity target) {
        return target instanceof Enemy && mayAttack(golem, target);
    }

    /** Active (deployed) golems owned by {@code owner} in loaded chunks of every dimension. */
    public static int countActive(MinecraftServer server, UUID owner) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ConstructEntity golem && owner.equals(golem.getOwnerUUID()) && golem.isAlive()) {
                    count++;
                }
            }
        }
        return count;
    }
}
