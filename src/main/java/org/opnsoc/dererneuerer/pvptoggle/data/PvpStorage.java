package org.opnsoc.dererneuerer.pvptoggle.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.opnsoc.dererneuerer.pvptoggle.Pvptoggle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class PvpStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PvpData data = new PvpData();
    private static MinecraftServer loadedServer;

    public static synchronized PvpData get(MinecraftServer server) {
        ensureLoaded(server);
        if (data.activateExpiredPending()) {
            saveLoaded();
        }
        return data;
    }

    public static synchronized boolean activateExpiredPending(MinecraftServer server) {
        ensureLoaded(server);
        if (!data.activateExpiredPending()) {
            return false;
        }
        saveLoaded();
        return true;
    }

    public static synchronized void load(MinecraftServer server) {
        if (loadedServer != null && loadedServer != server) {
            saveLoaded();
        }

        loadedServer = server;
        Path path = file(server);
        if (!Files.exists(path)) {
            data = new PvpData();
            return;
        }

        try {
            PvpData loaded = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), PvpData.class);
            data = loaded == null ? new PvpData() : loaded;
            data.fixNulls();
        } catch (Exception exception) {
            Pvptoggle.LOGGER.error("Could not load PvP toggle data from {}", path, exception);
            data = new PvpData();
        }
    }

    public static synchronized void save(MinecraftServer server) {
        ensureLoaded(server);
        saveLoaded();
    }

    public static synchronized void unload(MinecraftServer server) {
        if (loadedServer != server) {
            return;
        }
        saveLoaded();
        loadedServer = null;
        data = new PvpData();
    }

    private static void ensureLoaded(MinecraftServer server) {
        if (loadedServer != server) {
            load(server);
        }
    }

    private static void saveLoaded() {
        if (loadedServer == null) {
            return;
        }

        Path target = file(loadedServer);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");

        try {
            Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            Pvptoggle.LOGGER.error("Could not save PvP toggle data to {}", target, exception);
        }
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("pvptoggle-data.json");
    }

    private PvpStorage() {
    }
}
