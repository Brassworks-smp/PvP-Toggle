package org.opnsoc.dererneuerer.pvptoggle.data;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public final class PvpStorageLifecycle {
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        PvpStorage.load(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        PvpStorage.unload(event.getServer());
    }
}
