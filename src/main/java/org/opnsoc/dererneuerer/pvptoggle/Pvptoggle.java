package org.opnsoc.dererneuerer.pvptoggle;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.opnsoc.dererneuerer.pvptoggle.client.PvpIconRenderer;
import org.opnsoc.dererneuerer.pvptoggle.client.PvpMenuClientBridge;
import org.opnsoc.dererneuerer.pvptoggle.combat.PvpDamageHandler;
import org.opnsoc.dererneuerer.pvptoggle.combat.PvpPlayerHandler;
import org.opnsoc.dererneuerer.pvptoggle.command.PvpCommandHandler;
import org.opnsoc.dererneuerer.pvptoggle.config.Config;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpPendingTickHandler;
import org.opnsoc.dererneuerer.pvptoggle.data.PvpStorageLifecycle;
import org.opnsoc.dererneuerer.pvptoggle.network.PvpIconSync;
import org.opnsoc.dererneuerer.pvptoggle.network.PvptoggleNetwork;
import org.slf4j.Logger;

@Mod(Pvptoggle.MODID)
public class Pvptoggle {
    public static final String MODID = "pvptoggle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Pvptoggle(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        modEventBus.addListener(Config::onLoad);
        modEventBus.addListener(Config::onReload);
        modEventBus.addListener(PvptoggleNetwork::registerPayloads);

        NeoForge.EVENT_BUS.register(new PvpCommandHandler());
        NeoForge.EVENT_BUS.register(new PvpDamageHandler());
        NeoForge.EVENT_BUS.register(new PvpPlayerHandler());
        NeoForge.EVENT_BUS.register(new PvpIconSync());
        NeoForge.EVENT_BUS.register(new PvpPendingTickHandler());
        NeoForge.EVENT_BUS.register(new PvpStorageLifecycle());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(PvpIconRenderer.class);
            NeoForge.EVENT_BUS.register(PvpMenuClientBridge.class);
        }
    }
}
