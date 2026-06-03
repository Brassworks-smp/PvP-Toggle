package org.opnsoc.dererneuerer.pvptoggle.command;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.opnsoc.dererneuerer.pvptoggle.combat.PvpPlayerHandler;
import org.opnsoc.dererneuerer.pvptoggle.config.Config;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpData;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpStorage;
import org.opnsoc.dererneuerer.pvptoggle.network.PvpIconSync;
import org.opnsoc.dererneuerer.pvptoggle.util.PvpUtil;

import java.util.UUID;

public class PvpCommandHandler {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("pvp")

                        .then(Commands.literal("off").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PvpData data = PvpStorage.get(player.server);

                            int minutes = Config.takeEffectTimeMinutes;
                            if (minutes <= 0) {
                                data.setPvpOffNow(player.getUUID());
                                PvpStorage.save(player.server);
                                PvpIconSync.syncAll(player);

                                PvpUtil.tellError(player, "PvP is now §cdisabled§7.");
                            } else {
                                long until = System.currentTimeMillis() + minutes * 60_000L;

                                data.setPvpOffDelayed(player.getUUID(), until);
                                PvpStorage.save(player.server);
                                PvpIconSync.syncAll(player);

                                PvpUtil.tellWarning(player,
                                        "PvP will be disabled in §f" + minutes + " minute" + (minutes == 1 ? "" : "s") + "§7.");
                            }

                            return 1;
                        }))

                        .then(Commands.literal("on").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PvpData data = PvpStorage.get(player.server);

                            data.setPvpOn(player.getUUID());
                            PvpStorage.save(player.server);
                            PvpIconSync.syncAll(player);

                            PvpUtil.tellSuccess(player, "PvP is now §aenabled§7.");
                            return 1;
                        }))

                        .then(Commands.literal("block")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer self = ctx.getSource().getPlayerOrException();
                                            ServerPlayer other = EntityArgument.getPlayer(ctx, "player");

                                            if (self.getUUID().equals(other.getUUID())) {
                                                PvpUtil.tellError(self, "You cannot block yourself.");
                                                return 0;
                                            }

                                            PvpData data = PvpStorage.get(self.server);

                                            int minutes = Config.takeEffectTimeMinutes;

                                            if (minutes <= 0) {
                                                data.block(self.getUUID(), other.getUUID());
                                                PvpStorage.save(self.server);
                                                PvpIconSync.syncAll(self);

                                                PvpUtil.tellError(self,
                                                        "PvP between you and §f"
                                                                + other.getName().getString()
                                                                + " §7has been §cblocked§7.");
                                            } else {
                                                long until = System.currentTimeMillis() + minutes * 60_000L;

                                                data.blockDelayed(self.getUUID(), other.getUUID(), until);
                                                PvpStorage.save(self.server);
                                                PvpIconSync.syncAll(self);

                                                PvpUtil.tellWarning(self,
                                                        "PvP between you and §f"
                                                                + other.getName().getString()
                                                                + " §7will be blocked in §f"
                                                                + minutes
                                                                + " minute"
                                                                + (minutes == 1 ? "" : "s")
                                                                + "§7.");
                                            }

                                            return 1;
                                        })))

                        .then(Commands.literal("unblock")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer self = ctx.getSource().getPlayerOrException();
                                            ServerPlayer other = EntityArgument.getPlayer(ctx, "player");

                                            PvpData data = PvpStorage.get(self.server);

                                            data.unblock(self.getUUID(), other.getUUID());
                                            PvpStorage.save(self.server);
                                            PvpIconSync.syncAll(self);

                                            PvpUtil.tellSuccess(self,
                                                    "PvP between you and §f"
                                                            + other.getName().getString()
                                                            + " §7is now §aallowed§7 again.");

                                            return 1;
                                        })))

                        .then(Commands.literal("status").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PvpData data = PvpStorage.get(player.server);

                            UUID id = player.getUUID();
                            String blockInfo = "§8 | §7Blocked players: §f"
                                    + data.blockedCount(id)
                                    + " §8| §7Pending blocks: §f"
                                    + data.pendingBlockedCount(id);

                            if (data.isPvpOff(id)) {
                                PvpUtil.tellInfo(player,
                                        "Status: §cPvP OFF" + blockInfo);
                            } else if (data.isPending(id)) {
                                PvpUtil.tellWarning(player,
                                        "Status: §ePvP OFF pending §8| §7Active in: §f"
                                                + PvpUtil.formatRemaining(data.pendingUntil(id))
                                                + blockInfo);
                            } else {
                                PvpUtil.tellSuccess(player,
                                        "Status: §aPvP ON" + blockInfo);
                            }

                            return 1;
                        }))

                        .then(Commands.literal("help").executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("""
                                            §8§m                                                  §r
                                            §8[§cPvP§8] §fCommands
                                            §8§m                                                  §r
                                            §c/pvp off §8- §7Disable PvP after the configured delay
                                            §a/pvp on §8- §7Enable PvP
                                            §c/pvp block <player> §8- §7Block PvP with a player after the configured delay
                                            §a/pvp unblock <player> §8- §7Allow PvP with a player again
                                            §b/pvp status §8- §7Show your current PvP status
                                            §8§m                                                  §r
                                            """), false);

                            return 1;
                        }))
        );

        event.getDispatcher().register(
                Commands.literal("pvpadmin")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("forceoff")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

                                            PvpData data = PvpStorage.get(target.server);

                                            data.setPvpOffNow(target.getUUID());
                                            PvpStorage.save(target.server);

                                            PvpPlayerHandler.updateCollision(target);
                                            PvpIconSync.syncAll(target);

                                            PvpUtil.tellAdmin(target,
                                                    "An admin has §cdisabled §7your PvP.");

                                            ctx.getSource().sendSuccess(
                                                    () -> PvpUtil.admin(
                                                            "PvP disabled for §f"
                                                                    + target.getName().getString()
                                                                    + "§7."),
                                                    true
                                            );

                                            return 1;
                                        })))

                        .then(Commands.literal("forceon")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

                                            PvpData data = PvpStorage.get(target.server);

                                            data.setPvpOn(target.getUUID());
                                            PvpStorage.save(target.server);

                                            PvpPlayerHandler.updateCollision(target);
                                            PvpIconSync.syncAll(target);

                                            PvpUtil.tellAdmin(target,
                                                    "An admin has §aenabled §7your PvP.");

                                            ctx.getSource().sendSuccess(
                                                    () -> PvpUtil.admin(
                                                            "PvP enabled for §f"
                                                                    + target.getName().getString()
                                                                    + "§7."),
                                                    true
                                            );

                                            return 1;
                                        })))

                        .then(Commands.literal("reload").executes(ctx -> {
                            PvpStorage.load(ctx.getSource().getServer());

                            for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                                PvpIconSync.syncForViewer(player);
                            }

                            ctx.getSource().sendSuccess(
                                    () -> PvpUtil.admin("PvP data reloaded."),
                                    true
                            );

                            return 1;
                        }))
        );
    }
}