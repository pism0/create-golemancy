package io.github.pism0.golemancy.core.network;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.entity.ConstructEntity;
import io.github.pism0.golemancy.lifecycle.GolemLifecycle;
import io.github.pism0.golemancy.lifecycle.GolemRequests;
import io.github.pism0.golemancy.magic.CoreDef;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Optional;
import java.util.UUID;

/**
 * Golemancy's two packets (SDD 16.1, 16.2):
 * <ul>
 *   <li>server → client {@link OpenScreen}: the full spec and live numbers, sent only when a
 *       player opens (or changes something in) the configuration screen;</li>
 *   <li>client → server {@link Request}: "please do X to golem N". Never trusted: the server
 *       re-checks ownership, distance and the value in {@link GolemRequests}.</li>
 * </ul>
 */
public final class GolemNetwork {
    private GolemNetwork() {
    }

    private static final String PROTOCOL = "1";

    /** Everything the three tabs show. */
    public record OpenScreen(int entityId, GolemSpec spec, float energy, float maxEnergy, float passiveDrain,
                             String ownerName, boolean refresh) implements CustomPacketPayload {
        public static final Type<OpenScreen> TYPE = new Type<>(Golemancy.id("open_golem_screen"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreen> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entityId);
                    GolemSpec.STREAM_CODEC.encode(buf, p.spec);
                    buf.writeFloat(p.energy);
                    buf.writeFloat(p.maxEnergy);
                    buf.writeFloat(p.passiveDrain);
                    buf.writeUtf(p.ownerName, 64);
                    buf.writeBoolean(p.refresh);
                },
                buf -> new OpenScreen(buf.readVarInt(), GolemSpec.STREAM_CODEC.decode(buf), buf.readFloat(), buf.readFloat(),
                        buf.readFloat(), buf.readUtf(64), buf.readBoolean()));

        @Override
        public Type<OpenScreen> type() {
            return TYPE;
        }
    }

    /** A request from the screen. {@code action} is a {@link GolemRequests.Action} ordinal. */
    public record Request(int entityId, int action, int value) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Golemancy.id("golem_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Request::entityId,
                ByteBufCodecs.VAR_INT, Request::action,
                ByteBufCodecs.VAR_INT, Request::value,
                Request::new);

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(GolemNetwork::onRegisterPayloads);
        GolemLifecycle.screenOpener = (player, golem) -> sendScreen(player, golem, false);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(OpenScreen.TYPE, OpenScreen.STREAM_CODEC, GolemNetwork::handleOpenScreen);
        registrar.playToServer(Request.TYPE, Request.STREAM_CODEC, GolemNetwork::handleRequest);
    }

    private static void handleOpenScreen(OpenScreen payload, IPayloadContext context) {
        // The lambda body is the only place that names a client class, so a dedicated server never loads it.
        context.enqueueWork(() -> io.github.pism0.golemancy.client.ClientPayloads.openGolemScreen(payload));
    }

    private static void handleRequest(Request payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof ConstructEntity golem)) return;
            Optional<GolemRequests.Action> action = GolemRequests.Action.byId(payload.action());
            if (action.isEmpty()) return;
            boolean applied = GolemRequests.apply(player, golem, action.get(), payload.value());
            if (applied && golem.isAlive()) {
                sendScreen(player, golem, true);
            } else if (!applied) {
                Golemancy.LOGGER.debug("Rejected golem request {} from {}", action.get(), player.getGameProfile().getName());
            }
        });
    }

    /** Sends the full spec and live numbers to one player (SDD 16.1: "full spec sent on open-screen"). */
    public static void sendScreen(ServerPlayer player, ConstructEntity golem, boolean refresh) {
        CoreDef core = golem.getCoreDef();
        PacketDistributor.sendToPlayer(player, new OpenScreen(golem.getId(), golem.getSpec(),
                golem.getEnergy().getEnergy(), golem.getEnergy().getMaxEnergy(),
                core == null ? 0f : core.passiveDrain(), ownerName(golem), refresh));
    }

    private static String ownerName(ConstructEntity golem) {
        UUID owner = golem.getOwnerUUID();
        if (owner == null || golem.level().getServer() == null) return "";
        ServerPlayer online = golem.level().getServer().getPlayerList().getPlayer(owner);
        if (online != null) return online.getGameProfile().getName();
        var cache = golem.level().getServer().getProfileCache();
        return cache == null ? "?" : cache.get(owner).map(profile -> profile.getName()).orElse("?");
    }
}
