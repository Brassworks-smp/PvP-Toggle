package org.opnsoc.dererneuerer.pvptoggle.network;

import net.minecraft.server.level.ServerPlayer;
import org.opnsoc.dererneuerer.pvptoggle.combat.PvpRules;
import org.opnsoc.dererneuerer.pvptoggle.config.Config;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpData;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpStorage;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class PvpMenuService {
    public static boolean sendSnapshot(ServerPlayer player) {
        PvpData data = PvpStorage.get(player.server);
        UUID owner = player.getUUID();

        List<PvpMenuStatePayload.PlayerEntry> players = player.server.getPlayerList().getPlayers().stream()
                .filter(other -> !other.getUUID().equals(owner))
                .sorted(Comparator.comparing(other -> other.getName().getString(), String.CASE_INSENSITIVE_ORDER))
                .limit(512)
                .map(other -> entry(data, owner, other))
                .toList();

        PvpMenuStatePayload payload = new PvpMenuStatePayload(
                data.isPvpOff(owner),
                data.pendingUntil(owner),
                Config.takeEffectTimeMinutes,
                data.blockedCount(owner),
                data.pendingBlockedCount(owner),
                players
        );
        return PvptoggleNetwork.sendToClient(player, payload);
    }

    private static PvpMenuStatePayload.PlayerEntry entry(PvpData data, UUID owner, ServerPlayer other) {
        UUID target = other.getUUID();
        PvpMenuStatePayload.Relation relation;
        long pendingUntil = 0L;

        if (data.hasBlocked(owner, target)) {
            relation = PvpMenuStatePayload.Relation.BLOCKED;
        } else if (data.hasPendingBlock(owner, target)) {
            relation = PvpMenuStatePayload.Relation.PENDING;
            pendingUntil = data.pendingBlockUntil(owner, target);
        } else {
            relation = PvpMenuStatePayload.Relation.ALLOWED;
        }

        return new PvpMenuStatePayload.PlayerEntry(
                target,
                other.getName().getString(),
                relation,
                PvpRules.canAttack(data, owner, target),
                pendingUntil
        );
    }

    private PvpMenuService() {
    }
}
