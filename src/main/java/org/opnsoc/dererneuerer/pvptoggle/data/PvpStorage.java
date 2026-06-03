package org.opnsoc.dererneuerer.pvptoggle.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.opnsoc.dererneuerer.pvptoggle.Pvptoggle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PvpStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PvpData data = new PvpData();

    public static PvpData get(MinecraftServer server) {
        load(server);

        if (data.activateExpiredPending()) {
            save(server);
        }

        return data;
    }

    public static void load(MinecraftServer server) {
        Path path = file(server);

        if (!Files.exists(path)) {
            data = new PvpData();
            return;
        }

        try {
            PvpData loaded = GSON.fromJson(Files.readString(path), PvpData.class);
            data = loaded == null ? new PvpData() : loaded;
            data.fixNulls();
        } catch (Exception exception) {
            Pvptoggle.LOGGER.error("Could not load PvP toggle data", exception);
            data = new PvpData();
        }
    }

    public static void save(MinecraftServer server) {
        try {
            Files.writeString(file(server), GSON.toJson(data));
        } catch (IOException exception) {
            Pvptoggle.LOGGER.error("Could not save PvP toggle data", exception);
        }
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("pvptoggle-data.json");
    }
}