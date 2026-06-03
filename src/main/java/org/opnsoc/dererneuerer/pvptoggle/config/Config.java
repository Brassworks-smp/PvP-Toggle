package org.opnsoc.dererneuerer.pvptoggle.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.opnsoc.dererneuerer.pvptoggle.Pvptoggle;

@EventBusSubscriber(modid = Pvptoggle.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ONE_SIDED_TOGGLE = BUILDER
            .comment("If true, /pvp off only protects the player from incoming PvP.")
            .define("one-sided-toggle", false);

    private static final ModConfigSpec.IntValue TAKE_EFFECT_TIME_MINUTES = BUILDER
            .comment("Delay in minutes until /pvp off becomes active.")
            .defineInRange("take-effect-time", 10, 0, 1440);

    private static final ModConfigSpec.BooleanValue CANCEL_PENDING_OFF_ON_ATTACK = BUILDER
            .comment("If true, attacking another player while /pvp off is pending cancels the pending toggle.")
            .define("cancel-pending-off-on-attack", true);

    private static final ModConfigSpec.BooleanValue SEND_ACTION_MESSAGES = BUILDER
            .comment("If true, players receive chat messages when PvP damage/knockback/pushing is blocked.")
            .define("send-action-messages", false);

    private static final ModConfigSpec.BooleanValue BLOCK_KNOCKBACK = BUILDER
            .comment("If true, PvP protection also blocks knockback-only weapons/effects.")
            .define("block-knockback", true);

    private static final ModConfigSpec.BooleanValue BLOCK_PLAYER_PUSHING = BUILDER
            .comment("If true, PvP protection also prevents protected players from being physically pushed by other players.")
            .define("block-player-pushing", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean oneSidedToggle;
    public static int takeEffectTimeMinutes;
    public static boolean cancelPendingOffOnAttack;
    public static boolean sendActionMessages;
    public static boolean blockKnockback;
    public static boolean blockPlayerPushing;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        oneSidedToggle = ONE_SIDED_TOGGLE.get();
        takeEffectTimeMinutes = TAKE_EFFECT_TIME_MINUTES.get();
        cancelPendingOffOnAttack = CANCEL_PENDING_OFF_ON_ATTACK.get();
        sendActionMessages = SEND_ACTION_MESSAGES.get();
        blockKnockback = BLOCK_KNOCKBACK.get();
        blockPlayerPushing = BLOCK_PLAYER_PUSHING.get();
    }
}