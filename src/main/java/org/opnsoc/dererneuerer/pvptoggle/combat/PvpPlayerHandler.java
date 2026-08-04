package org.opnsoc.dererneuerer.pvptoggle.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.opnsoc.dererneuerer.pvptoggle.config.Config;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpData;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpStorage;

public final class PvpPlayerHandler {
    private static final String PVP_OFF_TEAM = "pvp_off_collision";

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        updateCollision(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        updateCollision(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        updateCollision(event.getEntity());
    }

    public static void updateCollision(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        Scoreboard scoreboard = player.serverLevel().getScoreboard();
        PlayerTeam pvpTeam = scoreboard.getPlayerTeam(PVP_OFF_TEAM);
        if (pvpTeam == null) {
            pvpTeam = scoreboard.addPlayerTeam(PVP_OFF_TEAM);
            pvpTeam.setCollisionRule(Team.CollisionRule.NEVER);
            pvpTeam.setSeeFriendlyInvisibles(false);
            pvpTeam.setAllowFriendlyFire(false);
        }

        PvpData data = PvpStorage.get(player.server);
        String playerName = player.getScoreboardName();
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(playerName);
        boolean shouldBlockPushing = Config.blockPlayerPushing && data.isPvpOff(player.getUUID());

        if (shouldBlockPushing) {
            if (currentTeam != null && currentTeam != pvpTeam) {
                data.rememberPreviousTeam(player.getUUID(), currentTeam.getName());
            }
            if (currentTeam != pvpTeam) {
                scoreboard.addPlayerToTeam(playerName, pvpTeam);
                PvpStorage.save(player.server);
            }
            return;
        }

        if (currentTeam == pvpTeam) {
            scoreboard.removePlayerFromTeam(playerName, pvpTeam);
        }

        String previousTeamName = data.takePreviousTeam(player.getUUID());
        if (previousTeamName != null) {
            PlayerTeam previousTeam = scoreboard.getPlayerTeam(previousTeamName);
            if (previousTeam != null) {
                scoreboard.addPlayerToTeam(playerName, previousTeam);
            }
            PvpStorage.save(player.server);
        }
    }
}
