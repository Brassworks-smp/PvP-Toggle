package org.opnsoc.dererneuerer.pvptoggle.combat;

import org.opnsoc.dererneuerer.pvptoggle.data.PvpData;

import java.util.UUID;

public class PvpRules {

    public static boolean canAttack(PvpData data, UUID attacker, UUID target) {
        if (attacker.equals(target)) return false;

        if (data.isPvpOff(attacker)) return false;
        if (data.isPvpOff(target)) return false;

        if (data.hasBlocked(attacker, target)) return false;
        if (data.hasBlocked(target, attacker)) return false;

        return true;
    }
}