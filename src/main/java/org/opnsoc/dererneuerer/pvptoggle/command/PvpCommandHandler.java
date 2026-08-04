package org.opnsoc.dererneuerer.pvptoggle.command;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.opnsoc.dererneuerer.pvptoggle.combat.PvpActions;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpData;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpStorage;
import org.opnsoc.dererneuerer.pvptoggle.network.PvpIconSync;
import org.opnsoc.dererneuerer.pvptoggle.network.PvpMenuService;
import org.opnsoc.dererneuerer.pvptoggle.util.PvpUtil;

import java.util.UUID;

public final class PvpCommandHandler {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("pvp")
                        .then(Commands.literal("off").executes(context -> {
                            PvpActions.disable(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("on").executes(context -> {
                            PvpActions.enable(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("block")
                                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                                    ServerPlayer self = context.getSource().getPlayerOrException();
                                    return PvpActions.block(self, EntityArgument.getPlayer(context, "player")) ? 1 : 0;
                                })))
                        .then(Commands.literal("unblock")
                                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                                    ServerPlayer self = context.getSource().getPlayerOrException();
                                    return PvpActions.unblock(self, EntityArgument.getPlayer(context, "player")) ? 1 : 0;
                                })))
                        .then(Commands.literal("status").executes(context -> showStatus(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("menu").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            if (!PvpMenuService.sendSnapshot(player)) {
                                PvpUtil.tellInfo(player, "The optional PvP menu is unavailable. All /pvp commands still work.");
                                return 0;
                            }
                            return 1;
                        }))
                        .then(Commands.literal("help").executes(context -> {
                            context.getSource().sendSuccess(() -> Component.literal("""
                                    §8§m                                                  §r
                                    §8[§cPvP§8] §fCommands
                                    §8§m                                                  §r
                                    §c/pvp off §8- §7Disable PvP after the configured delay
                                    §a/pvp on §8- §7Enable PvP
                                    §c/pvp block <player> §8- §7Block PvP with a player
                                    §a/pvp unblock <player> §8- §7Allow PvP with a player again
                                    §b/pvp status §8- §7Show your current PvP status
                                    §6/pvp menu §8- §7Open the optional BrassUI menu
                                    §8§m                                                  §r
                                    """), false);
                            return 1;
                        }))
        );

        event.getDispatcher().register(
                Commands.literal("pvpadmin")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("forceoff")
                                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    PvpActions.forceDisable(target);
                                    PvpUtil.tellAdmin(target, "An admin has §cdisabled §7your PvP.");
                                    context.getSource().sendSuccess(
                                            () -> PvpUtil.admin("PvP disabled for §f" + target.getName().getString() + "§7."), true);
                                    return 1;
                                })))
                        .then(Commands.literal("forceon")
                                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    PvpActions.forceEnable(target);
                                    PvpUtil.tellAdmin(target, "An admin has §aenabled §7your PvP.");
                                    context.getSource().sendSuccess(
                                            () -> PvpUtil.admin("PvP enabled for §f" + target.getName().getString() + "§7."), true);
                                    return 1;
                                })))
                        .then(Commands.literal("reload").executes(context -> {
                            PvpStorage.load(context.getSource().getServer());
                            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                PvpIconSync.syncForViewer(player);
                            }
                            context.getSource().sendSuccess(() -> PvpUtil.admin("PvP data reloaded."), true);
                            return 1;
                        }))
        );
    }

    private static int showStatus(ServerPlayer player) {
        PvpData data = PvpStorage.get(player.server);
        UUID playerId = player.getUUID();
        String blockInfo = "§8 | §7Blocked players: §f" + data.blockedCount(playerId)
                + " §8| §7Pending blocks: §f" + data.pendingBlockedCount(playerId);

        if (data.isPvpOff(playerId)) {
            PvpUtil.tellInfo(player, "Status: §cPvP OFF" + blockInfo);
        } else if (data.isPending(playerId)) {
            PvpUtil.tellWarning(player, "Status: §ePvP OFF pending §8| §7Active in: §f"
                    + PvpUtil.formatRemaining(data.pendingUntil(playerId)) + blockInfo);
        } else {
            PvpUtil.tellSuccess(player, "Status: §aPvP ON" + blockInfo);
        }
        return 1;
    }
}
