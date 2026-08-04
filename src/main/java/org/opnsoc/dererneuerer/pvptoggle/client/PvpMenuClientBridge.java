package org.opnsoc.dererneuerer.pvptoggle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.opnsoc.dererneuerer.pvptoggle.Pvptoggle;
import org.opnsoc.dererneuerer.pvptoggle.network.PvpMenuActionPayload;
import org.opnsoc.dererneuerer.pvptoggle.network.PvpMenuStatePayload;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public final class PvpMenuClientBridge {
    private static final String KOTLIN_FUNCTION = "kotlin.jvm.functions.Function0";
    private static final String SCREEN_CLASS = "org.opnsoc.dererneuerer.pvptoggle.client.brass.BrassPvpManagementScreen";
    private static Method openMethod;
    private static boolean resolutionFailed;

    public static void receive(PvpMenuStatePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isClassAvailable(KOTLIN_FUNCTION)) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("message.pvptoggle.ui_unavailable"), false);
            }
            return;
        }

        try {
            resolveOpenMethod().invoke(null, payload);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            reportFailure(minecraft, exception);
        } catch (InvocationTargetException exception) {
            reportFailure(minecraft, exception.getCause() == null ? exception : exception.getCause());
        } catch (LinkageError | RuntimeException exception) {
            reportFailure(minecraft, exception);
        }
    }

    public static void sendAction(PvpMenuActionPayload.Action action, UUID target) {
        PacketDistributor.sendToServer(new PvpMenuActionPayload(action, target));
    }

    private static Method resolveOpenMethod() throws ClassNotFoundException, NoSuchMethodException {
        if (openMethod == null) {
            Class<?> screen = Class.forName(SCREEN_CLASS, true, PvpMenuClientBridge.class.getClassLoader());
            openMethod = screen.getMethod("open", PvpMenuStatePayload.class);
        }
        return openMethod;
    }

    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className, false, PvpMenuClientBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }

    private static void reportFailure(Minecraft minecraft, Throwable throwable) {
        if (!resolutionFailed) {
            resolutionFailed = true;
            Pvptoggle.LOGGER.error("Could not open the optional BrassUI PvP menu", throwable);
        }
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("message.pvptoggle.ui_failed"), false);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPvpIcons.clear();
        openMethod = null;
        resolutionFailed = false;
    }

    private PvpMenuClientBridge() {
    }
}
