package org.opnsoc.dererneuerer.pvptoggle.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.opnsoc.dererneuerer.pvptoggle.combat.PvpRules;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpData;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpStorage;

public class PvpIconSync {

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAll(player);
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;

        syncSingle(viewer, target);
    }

    public static void syncForViewer(ServerPlayer viewer) {
        PvpData data = PvpStorage.get(viewer.server);
        for (ServerPlayer target : viewer.server.getPlayerList().getPlayers()) {
            if (viewer == target) continue;
            syncSingle(viewer, target, data);
        }
    }

    public static void syncSingle(ServerPlayer viewer, ServerPlayer target) {
        PvpData data = PvpStorage.get(viewer.server);
        syncSingle(viewer, target, data);
    }

    private static void syncSingle(ServerPlayer viewer, ServerPlayer target, PvpData data) {

        boolean canAttack = PvpRules.canAttack(
                data,
                viewer.getUUID(),
                target.getUUID()
        );

        PvptoggleNetwork.sendToClient(
                viewer,
                new PvpIconStatePayload(target.getUUID(), canAttack)
        );
    }

    public static void syncAll(ServerPlayer source) {
        PvpData data = PvpStorage.get(source.server);
        for (ServerPlayer viewer : source.server.getPlayerList().getPlayers()) {
            for (ServerPlayer target : source.server.getPlayerList().getPlayers()) {
                if (viewer != target) {
                    syncSingle(viewer, target, data);
                }
            }
        }
    }
}
