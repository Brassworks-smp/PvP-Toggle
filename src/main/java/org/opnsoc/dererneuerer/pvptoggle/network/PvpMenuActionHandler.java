package org.opnsoc.dererneuerer.pvptoggle.network;

import net.minecraft.server.level.ServerPlayer;
import org.opnsoc.dererneuerer.pvptoggle.combat.PvpActions;
import org.opnsoc.dererneuerer.pvptoggle.util.PvpUtil;

public final class PvpMenuActionHandler {
    public static void handle(ServerPlayer player, PvpMenuActionPayload payload) {
        switch (payload.action()) {
            case ENABLE -> PvpActions.enable(player);
            case DISABLE -> PvpActions.disable(player);
            case BLOCK -> withTarget(player, payload, true);
            case UNBLOCK -> withTarget(player, payload, false);
            case REFRESH -> {
            }
        }
        PvpMenuService.sendSnapshot(player);
    }

    private static void withTarget(ServerPlayer player, PvpMenuActionPayload payload, boolean block) {
        if (payload.target() == null) {
            PvpUtil.tellError(player, "No player was selected.");
            return;
        }

        ServerPlayer target = player.server.getPlayerList().getPlayer(payload.target());
        if (target == null) {
            PvpUtil.tellError(player, "That player is no longer online.");
            return;
        }

        if (block) {
            PvpActions.block(player, target);
        } else {
            PvpActions.unblock(player, target);
        }
    }

    private PvpMenuActionHandler() {
    }
}
