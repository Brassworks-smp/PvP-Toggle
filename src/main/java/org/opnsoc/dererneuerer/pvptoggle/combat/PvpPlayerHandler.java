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

public class PvpPlayerHandler {

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
        if (!(entity instanceof ServerPlayer player)) return;

        Scoreboard scoreboard = player.serverLevel().getScoreboard();

        PlayerTeam team = scoreboard.getPlayerTeam(PVP_OFF_TEAM);

        if (team == null) {
            team = scoreboard.addPlayerTeam(PVP_OFF_TEAM);
            team.setCollisionRule(Team.CollisionRule.NEVER);
            team.setSeeFriendlyInvisibles(false);
            team.setAllowFriendlyFire(false);
        }

        PvpData data = PvpStorage.get(player.getServer());

        boolean shouldBlockPushing =
                Config.blockPlayerPushing &&
                        data.isPvpOff(player.getUUID());

        String playerName = player.getScoreboardName();

        if (shouldBlockPushing) {
            scoreboard.addPlayerToTeam(playerName, team);
        } else {
            PlayerTeam currentTeam = scoreboard.getPlayersTeam(playerName);

            if (currentTeam == team) {
                scoreboard.removePlayerFromTeam(playerName, team);
            }
        }
    }
}