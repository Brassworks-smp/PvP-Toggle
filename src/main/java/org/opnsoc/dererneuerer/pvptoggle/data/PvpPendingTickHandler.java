package org.opnsoc.dererneuerer.pvptoggle.data;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.opnsoc.dererneuerer.pvptoggle.combat.PvpPlayerHandler;
import org.opnsoc.dererneuerer.pvptoggle.network.PvpIconSync;

import java.util.List;

public class PvpPendingTickHandler {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private int ticks;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        ticks++;

        if (ticks < CHECK_INTERVAL_TICKS) {
            return;
        }

        ticks = 0;

        if (!PvpStorage.activateExpiredPending(event.getServer())) {
            return;
        }

        List<ServerPlayer> players = event.getServer().getPlayerList().getPlayers();

        if (players.isEmpty()) {
            return;
        }

        for (ServerPlayer player : players) {
            PvpPlayerHandler.updateCollision(player);
        }

        PvpIconSync.syncAll(players.getFirst());
    }
}