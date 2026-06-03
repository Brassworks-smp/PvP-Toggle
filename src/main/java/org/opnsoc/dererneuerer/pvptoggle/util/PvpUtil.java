package org.opnsoc.dererneuerer.pvptoggle.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PvpUtil {
    public static Component msg(String text) {
        return Component.literal("§8[§cPvP§8] §7" + text);
    }

    public static Component success(String text) {
        return Component.literal("§8[§cPvP§8] §a✔ §7" + text);
    }

    public static Component error(String text) {
        return Component.literal("§8[§cPvP§8] §c✖ §7" + text);
    }

    public static Component warning(String text) {
        return Component.literal("§8[§cPvP§8] §e⚠ §7" + text);
    }

    public static Component info(String text) {
        return Component.literal("§8[§cPvP§8] §bℹ §7" + text);
    }

    public static Component admin(String text) {
        return Component.literal("§8[§cPvP§8] §d⚙ §7" + text);
    }

    public static void tell(ServerPlayer player, String text) {
        player.sendSystemMessage(msg(text));
    }

    public static void tellSuccess(ServerPlayer player, String text) {
        player.sendSystemMessage(success(text));
    }

    public static void tellError(ServerPlayer player, String text) {
        player.sendSystemMessage(error(text));
    }

    public static void tellWarning(ServerPlayer player, String text) {
        player.sendSystemMessage(warning(text));
    }

    public static void tellInfo(ServerPlayer player, String text) {
        player.sendSystemMessage(info(text));
    }

    public static void tellAdmin(ServerPlayer player, String text) {
        player.sendSystemMessage(admin(text));
    }

    public static String formatRemaining(long untilMillis) {
        long remaining = Math.max(0L, untilMillis - System.currentTimeMillis());
        long seconds = (remaining + 999L) / 1000L;
        long minutes = seconds / 60L;
        long restSeconds = seconds % 60L;

        if (minutes <= 0) return restSeconds + "s";
        return minutes + "m " + restSeconds + "s";
    }
}