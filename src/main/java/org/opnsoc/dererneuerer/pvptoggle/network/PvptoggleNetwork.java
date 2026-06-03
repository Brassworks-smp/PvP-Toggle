package org.opnsoc.dererneuerer.pvptoggle.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.opnsoc.dererneuerer.pvptoggle.client.ClientPvpIcons;

public class PvptoggleNetwork {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();

        registrar.playToClient(
                PvpIconStatePayload.TYPE,
                PvpIconStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientPvpIcons.set(payload.target(), payload.canAttack())
                )
        );
    }

    public static void sendToClient(ServerPlayer player, PvpIconStatePayload payload) {
        if (!player.connection.hasChannel(payload)) {
            return;
        }

        PacketDistributor.sendToPlayer(player, payload);
    }
}