package io.github.pism0.golemancy.lifecycle;

import io.github.pism0.golemancy.data.BehaviorConfig;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.entity.ConstructEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Server-side handling of everything the configuration screen can ask for (SDD 14.1, 16.2). The
 * client only sends a request; this class decides. Each request is checked for ownership,
 * distance and validity against the current spec before anything changes.
 */
public final class GolemRequests {
    private GolemRequests() {
    }

    /** How close a player must be to configure a golem. */
    public static final double MAX_DISTANCE = 8.0;

    /** What the screen can ask for. The ordinal is the wire format: only ever append. */
    public enum Action {
        /** value = {@link BehaviorConfig.Profile} ordinal */
        SET_PROFILE,
        /** value = radius in blocks */
        SET_RADIUS,
        /** value = 0 / 1 */
        SET_COMBAT,
        /** value = {@link BehaviorConfig.MoveMode} ordinal */
        SET_MOVE_MODE,
        /** value = 0 / 1 */
        SET_CAST_SPELLS,
        /** Home becomes where the golem is standing. */
        SET_HOME_HERE,
        /** Pack the golem up into the player's inventory. */
        PACK_UP;

        public static Optional<Action> byId(int id) {
            return id >= 0 && id < values().length ? Optional.of(values()[id]) : Optional.empty();
        }
    }

    /** Whether {@code player} is allowed to send requests for {@code golem} at all. */
    public static boolean mayRequest(ServerPlayer player, ConstructEntity golem) {
        return golem.isAlive()
                && golem.level() == player.level()
                && player.distanceToSqr(golem) <= MAX_DISTANCE * MAX_DISTANCE
                && Ownership.canControl(player, golem);
    }

    /**
     * Applies one request.
     *
     * @return true if the request was valid and applied
     */
    public static boolean apply(ServerPlayer player, ConstructEntity golem, Action action, int value) {
        if (!mayRequest(player, golem)) return false;
        GolemSpec spec = golem.getSpec();
        BehaviorConfig behavior = spec.behavior();
        switch (action) {
            case SET_PROFILE -> {
                BehaviorConfig.Profile[] profiles = BehaviorConfig.Profile.values();
                if (value < 0 || value >= profiles.length) return false;
                behavior = behavior.withProfile(profiles[value]);
            }
            case SET_RADIUS -> {
                if (value < BehaviorConfig.MIN_RADIUS || value > BehaviorConfig.MAX_RADIUS) return false;
                behavior = behavior.withRadius(value);
            }
            case SET_COMBAT -> {
                if (value != 0 && value != 1) return false;
                behavior = behavior.withCombat(value == 1);
            }
            case SET_MOVE_MODE -> {
                BehaviorConfig.MoveMode[] modes = BehaviorConfig.MoveMode.values();
                if (value < 0 || value >= modes.length) return false;
                behavior = behavior.withMoveMode(modes[value]);
            }
            case SET_CAST_SPELLS -> {
                if (value != 0 && value != 1) return false;
                behavior = behavior.withCastSpells(value == 1);
            }
            case SET_HOME_HERE -> behavior = behavior.withHome(Optional.of(golem.blockPosition()));
            case PACK_UP -> {
                ItemStack packed = GolemLifecycle.packUp(golem);
                if (!player.getInventory().add(packed)) {
                    player.drop(packed, false);
                }
                return true;
            }
        }
        golem.applySpec(spec.withBehavior(behavior));
        return true;
    }
}
