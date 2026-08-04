package org.opnsoc.dererneuerer.pvptoggle.combat;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpData;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpStorage;
import org.opnsoc.dererneuerer.pvptoggle.util.PvpUtil;
import org.opnsoc.dererneuerer.pvptoggle.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PvpDamageHandler {

    private static final Map<UUID, RecentAttack> RECENT_ATTACKERS = new HashMap<>();
    private static final Map<UUID, Long> LAST_BLOCK_MESSAGE = new HashMap<>();
    private static final long RECENT_ATTACK_TICKS = 10;
    private static final long MESSAGE_COOLDOWN_MILLIS = 1000L;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker)) return;
        if (!(event.getTarget() instanceof ServerPlayer victim)) return;
        if (attacker.getUUID().equals(victim.getUUID())) return;

        if (isPvpBlocked(attacker, victim, true)) {
            event.setCanceled(true);
            clearHorizontalMotion(victim);
            notifyBlocked(attacker, "PvP against this player is §cdisabled§7.");
            return;
        }

        rememberAttack(attacker, victim);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (attacker.getUUID().equals(victim.getUUID())) return;

        if (isPvpBlocked(attacker, victim, true)) {
            event.setCanceled(true);
            clearHorizontalMotion(victim);
            notifyBlocked(attacker, "PvP against this player is §cdisabled§7.");
            return;
        }

        rememberAttack(attacker, victim);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKnockback(LivingKnockBackEvent event) {
        if (!Config.blockKnockback) return;
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        ServerPlayer attacker = getRecentAttacker(victim);
        if (attacker == null) return;
        if (attacker.getUUID().equals(victim.getUUID())) return;

        if (isPvpBlocked(attacker, victim, false)) {
            event.setCanceled(true);
            event.setStrength(0);
            clearHorizontalMotion(victim);
            notifyBlocked(attacker, "Knockback against this player is §cdisabled§7.");
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer actor)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;
        if (actor.getUUID().equals(target.getUUID())) return;

        if (isPvpBlocked(actor, target, true)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            clearHorizontalMotion(target);
            notifyBlocked(actor, "Player interaction is §cdisabled§7.");
            return;
        }

        rememberAttack(actor, target);
    }

    @SubscribeEvent
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer actor)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;
        if (actor.getUUID().equals(target.getUUID())) return;

        if (isPvpBlocked(actor, target, true)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            clearHorizontalMotion(target);
            notifyBlocked(actor, "Player interaction is §cdisabled§7.");
            return;
        }

        rememberAttack(actor, target);
    }

    private static boolean isPvpBlocked(ServerPlayer attacker, ServerPlayer victim, boolean cancelPendingOnAttack) {
        PvpData data = PvpStorage.get(victim.server);

        UUID attackerId = attacker.getUUID();
        UUID victimId = victim.getUUID();

        if (cancelPendingOnAttack && Config.cancelPendingOffOnAttack && data.isPending(attackerId)) {
            data.cancelPending(attackerId);
            PvpStorage.save(victim.server);
            PvpUtil.tellWarning(attacker, "Your pending §c/pvp off §7was cancelled because you attacked another player.");
        }

        return !PvpRules.canAttack(data, attackerId, victimId);
    }

    private static void rememberAttack(ServerPlayer attacker, ServerPlayer victim) {
        long tick = victim.serverLevel().getGameTime();
        RECENT_ATTACKERS.put(victim.getUUID(), new RecentAttack(attacker.getUUID(), tick));
    }

    private static ServerPlayer getRecentAttacker(ServerPlayer victim) {
        RecentAttack recent = RECENT_ATTACKERS.get(victim.getUUID());
        if (recent == null) return null;

        long now = victim.serverLevel().getGameTime();

        if (now - recent.tick > RECENT_ATTACK_TICKS) {
            RECENT_ATTACKERS.remove(victim.getUUID());
            return null;
        }

        return victim.server.getPlayerList().getPlayer(recent.attackerId);
    }

    private static void clearHorizontalMotion(ServerPlayer player) {
        player.setDeltaMovement(
                0,
                player.getDeltaMovement().y,
                0
        );

        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static void notifyBlocked(ServerPlayer player, String message) {
        if (!Config.sendActionMessages) return;

        long now = System.currentTimeMillis();
        long lastMessage = LAST_BLOCK_MESSAGE.getOrDefault(player.getUUID(), 0L);
        if (now - lastMessage < MESSAGE_COOLDOWN_MILLIS) return;

        LAST_BLOCK_MESSAGE.put(player.getUUID(), now);
        PvpUtil.tellError(player, message);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        RECENT_ATTACKERS.remove(playerId);
        RECENT_ATTACKERS.entrySet().removeIf(entry -> entry.getValue().attackerId().equals(playerId));
        LAST_BLOCK_MESSAGE.remove(playerId);
    }

    private record RecentAttack(UUID attackerId, long tick) {
    }
}
