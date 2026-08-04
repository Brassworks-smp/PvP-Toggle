package org.opnsoc.dererneuerer.pvptoggle.combat;

import net.minecraft.server.level.ServerPlayer;
import org.opnsoc.dererneuerer.pvptoggle.config.Config;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpData;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpStorage;
import org.opnsoc.dererneuerer.pvptoggle.network.PvpIconSync;
import org.opnsoc.dererneuerer.pvptoggle.util.PvpUtil;

public final class PvpActions {
    public static boolean disable(ServerPlayer player) {
        PvpData data = PvpStorage.get(player.server);
        if (data.isPvpOff(player.getUUID())) {
            PvpUtil.tellInfo(player, "PvP is already §cdisabled§7.");
            return false;
        }
        if (data.isPending(player.getUUID())) {
            PvpUtil.tellWarning(player, "PvP is already pending and will be disabled in §f"
                    + PvpUtil.formatRemaining(data.pendingUntil(player.getUUID())) + "§7.");
            return false;
        }

        int minutes = Config.takeEffectTimeMinutes;
        if (minutes <= 0) {
            data.setPvpOffNow(player.getUUID());
            PvpUtil.tellError(player, "PvP is now §cdisabled§7.");
            PvpPlayerHandler.updateCollision(player);
        } else {
            long until = System.currentTimeMillis() + minutes * 60_000L;
            data.setPvpOffDelayed(player.getUUID(), until);
            PvpUtil.tellWarning(player, "PvP will be disabled in §f" + minutes + " minute"
                    + (minutes == 1 ? "" : "s") + "§7.");
        }

        persistAndSync(player);
        return true;
    }

    public static boolean enable(ServerPlayer player) {
        PvpData data = PvpStorage.get(player.server);
        if (!data.isPvpOff(player.getUUID()) && !data.isPending(player.getUUID())) {
            PvpUtil.tellInfo(player, "PvP is already §aenabled§7.");
            return false;
        }

        data.setPvpOn(player.getUUID());
        PvpStorage.save(player.server);
        PvpPlayerHandler.updateCollision(player);
        PvpIconSync.syncAll(player);
        PvpUtil.tellSuccess(player, "PvP is now §aenabled§7.");
        return true;
    }

    public static boolean block(ServerPlayer owner, ServerPlayer target) {
        if (owner.getUUID().equals(target.getUUID())) {
            PvpUtil.tellError(owner, "You cannot block yourself.");
            return false;
        }

        PvpData data = PvpStorage.get(owner.server);
        if (data.hasBlocked(owner.getUUID(), target.getUUID())) {
            PvpUtil.tellInfo(owner, "PvP with §f" + target.getName().getString() + " §7is already blocked.");
            return false;
        }
        if (data.hasPendingBlock(owner.getUUID(), target.getUUID())) {
            PvpUtil.tellWarning(owner, "Blocking §f" + target.getName().getString() + " §7is already pending for §f"
                    + PvpUtil.formatRemaining(data.pendingBlockUntil(owner.getUUID(), target.getUUID())) + "§7.");
            return false;
        }

        int minutes = Config.takeEffectTimeMinutes;
        if (minutes <= 0) {
            data.block(owner.getUUID(), target.getUUID());
            PvpUtil.tellError(owner, "PvP between you and §f" + target.getName().getString() + " §7has been §cblocked§7.");
        } else {
            long until = System.currentTimeMillis() + minutes * 60_000L;
            data.blockDelayed(owner.getUUID(), target.getUUID(), until);
            PvpUtil.tellWarning(owner, "PvP between you and §f" + target.getName().getString()
                    + " §7will be blocked in §f" + minutes + " minute" + (minutes == 1 ? "" : "s") + "§7.");
        }

        persistAndSync(owner);
        return true;
    }

    public static boolean unblock(ServerPlayer owner, ServerPlayer target) {
        PvpData data = PvpStorage.get(owner.server);
        boolean wasBlocked = data.hasBlocked(owner.getUUID(), target.getUUID())
                || data.hasPendingBlock(owner.getUUID(), target.getUUID());
        if (!wasBlocked) {
            PvpUtil.tellInfo(owner, "PvP with §f" + target.getName().getString() + " §7is already allowed.");
            return false;
        }

        data.unblock(owner.getUUID(), target.getUUID());
        persistAndSync(owner);
        PvpUtil.tellSuccess(owner, "PvP between you and §f" + target.getName().getString() + " §7is now §aallowed§7 again.");
        return true;
    }

    public static void forceDisable(ServerPlayer target) {
        PvpData data = PvpStorage.get(target.server);
        data.setPvpOffNow(target.getUUID());
        PvpStorage.save(target.server);
        PvpPlayerHandler.updateCollision(target);
        PvpIconSync.syncAll(target);
    }

    public static void forceEnable(ServerPlayer target) {
        PvpData data = PvpStorage.get(target.server);
        data.setPvpOn(target.getUUID());
        PvpStorage.save(target.server);
        PvpPlayerHandler.updateCollision(target);
        PvpIconSync.syncAll(target);
    }

    private static void persistAndSync(ServerPlayer source) {
        PvpStorage.save(source.server);
        PvpIconSync.syncAll(source);
    }

    private PvpActions() {
    }
}
