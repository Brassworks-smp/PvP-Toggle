package org.opnsoc.dererneuerer.pvptoggle.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.opnsoc.dererneuerer.pvptoggle.Pvptoggle;
import org.opnsoc.dererneuerer.pvptoggle.client.ClientPvpIcons;

import java.lang.reflect.InvocationTargetException;

public final class PvptoggleNetwork {
    private static final String LEGACY_PROTOCOL_VERSION = "1";

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(LEGACY_PROTOCOL_VERSION).optional();

        registrar.playToClient(
                PvpIconStatePayload.TYPE,
                PvpIconStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientPvpIcons.set(payload.target(), payload.canAttack())
                )
        );

        registrar.playToClient(
                PvpMenuStatePayload.TYPE,
                PvpMenuStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> receiveMenuStateOnClient(payload))
        );

        registrar.playToServer(
                PvpMenuActionPayload.TYPE,
                PvpMenuActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        PvpMenuActionHandler.handle(player, payload);
                    }
                })
        );
    }

    public static boolean sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        if (!player.connection.hasChannel(payload)) {
            return false;
        }

        PacketDistributor.sendToPlayer(player, payload);
        return true;
    }

    private static void receiveMenuStateOnClient(PvpMenuStatePayload payload) {
        try {
            Class<?> bridge = Class.forName("org.opnsoc.dererneuerer.pvptoggle.client.PvpMenuClientBridge");
            bridge.getMethod("receive", PvpMenuStatePayload.class).invoke(null, payload);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            Pvptoggle.LOGGER.error("Could not resolve the client PvP menu bridge", exception);
        } catch (InvocationTargetException exception) {
            Pvptoggle.LOGGER.error("The client PvP menu bridge failed", exception.getCause());
        }
    }

    private PvptoggleNetwork() {
    }
}
