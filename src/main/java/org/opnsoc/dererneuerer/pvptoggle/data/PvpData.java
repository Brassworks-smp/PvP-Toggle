package org.opnsoc.dererneuerer.pvptoggle.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PvpData {
    public Set<UUID> pvpOff = new HashSet<>();
    public Map<UUID, Long> pendingOffUntil = new HashMap<>();
    public Map<UUID, Set<UUID>> blockedPlayers = new HashMap<>();
    public Map<UUID, Map<UUID, Long>> pendingBlockedPlayers = new HashMap<>();
    public Map<UUID, String> previousTeams = new HashMap<>();

    public void fixNulls() {
        if (pvpOff == null) pvpOff = new HashSet<>();
        if (pendingOffUntil == null) pendingOffUntil = new HashMap<>();
        if (blockedPlayers == null) blockedPlayers = new HashMap<>();
        if (pendingBlockedPlayers == null) pendingBlockedPlayers = new HashMap<>();
        if (previousTeams == null) previousTeams = new HashMap<>();
    }

    public boolean isPvpOff(UUID playerId) {
        return pvpOff.contains(playerId);
    }

    public boolean isPending(UUID playerId) {
        return pendingOffUntil.containsKey(playerId);
    }

    public long pendingUntil(UUID playerId) {
        return pendingOffUntil.getOrDefault(playerId, 0L);
    }

    public void setPvpOn(UUID playerId) {
        pvpOff.remove(playerId);
        pendingOffUntil.remove(playerId);
    }

    public void setPvpOffNow(UUID playerId) {
        pendingOffUntil.remove(playerId);
        pvpOff.add(playerId);
    }

    public void setPvpOffDelayed(UUID playerId, long activateAtMillis) {
        pvpOff.remove(playerId);
        pendingOffUntil.put(playerId, activateAtMillis);
    }

    public void cancelPending(UUID playerId) {
        pendingOffUntil.remove(playerId);
    }

    public void block(UUID owner, UUID target) {
        cancelPendingBlock(owner, target);
        blockedPlayers.computeIfAbsent(owner, id -> new HashSet<>()).add(target);
    }

    public void blockDelayed(UUID owner, UUID target, long activateAtMillis) {
        Set<UUID> blocked = blockedPlayers.get(owner);

        if (blocked != null) {
            blocked.remove(target);
            if (blocked.isEmpty()) {
                blockedPlayers.remove(owner);
            }
        }

        pendingBlockedPlayers
                .computeIfAbsent(owner, id -> new HashMap<>())
                .put(target, activateAtMillis);
    }

    public void cancelPendingBlock(UUID owner, UUID target) {
        Map<UUID, Long> pending = pendingBlockedPlayers.get(owner);

        if (pending != null) {
            pending.remove(target);

            if (pending.isEmpty()) {
                pendingBlockedPlayers.remove(owner);
            }
        }
    }

    public void unblock(UUID owner, UUID target) {
        Set<UUID> set = blockedPlayers.get(owner);
        if (set != null) {
            set.remove(target);
            if (set.isEmpty()) blockedPlayers.remove(owner);
        }

        cancelPendingBlock(owner, target);
    }

    public boolean hasBlocked(UUID owner, UUID target) {
        return blockedPlayers.getOrDefault(owner, Set.of()).contains(target);
    }

    public boolean hasPendingBlock(UUID owner, UUID target) {
        return pendingBlockedPlayers
                .getOrDefault(owner, Map.of())
                .containsKey(target);
    }

    public long pendingBlockUntil(UUID owner, UUID target) {
        return pendingBlockedPlayers
                .getOrDefault(owner, Map.of())
                .getOrDefault(target, 0L);
    }

    public int blockedCount(UUID owner) {
        return blockedPlayers.getOrDefault(owner, Set.of()).size();
    }

    public int pendingBlockedCount(UUID owner) {
        return pendingBlockedPlayers.getOrDefault(owner, Map.of()).size();
    }

    public void rememberPreviousTeam(UUID playerId, String teamName) {
        if (teamName != null && !teamName.isBlank()) {
            previousTeams.putIfAbsent(playerId, teamName);
        }
    }

    public String takePreviousTeam(UUID playerId) {
        return previousTeams.remove(playerId);
    }

    public boolean activateExpiredPending() {
        boolean changed = false;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> pvpOffIterator = pendingOffUntil.entrySet().iterator();

        while (pvpOffIterator.hasNext()) {
            Map.Entry<UUID, Long> entry = pvpOffIterator.next();
            if (entry.getValue() <= now) {
                pvpOff.add(entry.getKey());
                pvpOffIterator.remove();
                changed = true;
            }
        }

        Iterator<Map.Entry<UUID, Map<UUID, Long>>> ownerIterator = pendingBlockedPlayers.entrySet().iterator();

        while (ownerIterator.hasNext()) {
            Map.Entry<UUID, Map<UUID, Long>> ownerEntry = ownerIterator.next();
            UUID owner = ownerEntry.getKey();
            Map<UUID, Long> pendingTargets = ownerEntry.getValue();

            Iterator<Map.Entry<UUID, Long>> targetIterator = pendingTargets.entrySet().iterator();

            while (targetIterator.hasNext()) {
                Map.Entry<UUID, Long> targetEntry = targetIterator.next();

                if (targetEntry.getValue() <= now) {
                    blockedPlayers
                            .computeIfAbsent(owner, id -> new HashSet<>())
                            .add(targetEntry.getKey());

                    targetIterator.remove();
                    changed = true;
                }
            }

            if (pendingTargets.isEmpty()) {
                ownerIterator.remove();
            }
        }

        return changed;
    }
}
