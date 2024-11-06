package me.glitch.aitecraft.shareenderchest.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("share-ender-chest.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config load() {
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            return GSON.fromJson(reader, Config.class);
        } catch (IOException e) {
            Config config = new Config();
            save(config);
            return config;
        }
    }

    public static void save(Config config) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.out.println("Error saving config: " + e.getMessage());
        }
    }
}
