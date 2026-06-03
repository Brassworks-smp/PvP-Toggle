package org.opnsoc.dererneuerer.pvptoggle.data;

import java.util.*;

public class PvpData {
    public Set<UUID> pvpOff = new HashSet<>();
    public Map<UUID, Long> pendingOffUntil = new HashMap<>();
    public Map<UUID, Set<UUID>> blockedPlayers = new HashMap<>();

    public void fixNulls() {
        if (pvpOff == null) pvpOff = new HashSet<>();
        if (pendingOffUntil == null) pendingOffUntil = new HashMap<>();
        if (blockedPlayers == null) blockedPlayers = new HashMap<>();
    }

    public boolean isPvpOff(UUID playerId) {
        activateExpiredPending();
        return pvpOff.contains(playerId);
    }

    public boolean isPending(UUID playerId) {
        activateExpiredPending();
        return pendingOffUntil.containsKey(playerId);
    }

    public long pendingUntil(UUID playerId) {
        activateExpiredPending();
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
        blockedPlayers.computeIfAbsent(owner, id -> new HashSet<>()).add(target);
    }

    public void unblock(UUID owner, UUID target) {
        Set<UUID> set = blockedPlayers.get(owner);
        if (set != null) {
            set.remove(target);
            if (set.isEmpty()) blockedPlayers.remove(owner);
        }
    }

    public boolean hasBlocked(UUID owner, UUID target) {
        return blockedPlayers.getOrDefault(owner, Set.of()).contains(target);
    }

    public int blockedCount(UUID owner) {
        return blockedPlayers.getOrDefault(owner, Set.of()).size();
    }

    public boolean activateExpiredPending() {
        boolean changed = false;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = pendingOffUntil.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (entry.getValue() <= now) {
                pvpOff.add(entry.getKey());
                iterator.remove();
                changed = true;
            }
        }

        return changed;
    }
}