package org.opnsoc.dererneuerer.pvptoggle.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientPvpIcons {
    private static final Map<UUID, Boolean> STATES = new ConcurrentHashMap<>();

    public static void set(UUID player, boolean canAttack) {
        STATES.put(player, canAttack);
    }

    public static boolean canAttack(UUID player) {
        return STATES.getOrDefault(player, true);
    }
}