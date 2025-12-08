package win.demistorm.stormiespiders.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import win.demistorm.stormiespiders.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ModConfig {

    private static final Path CONFIG_FILE = Paths.get("config", "stormiespiders.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean preventClimbingInRain = false;

    // Data structure matching the JSON format
    public static final class ConfigData {
        public GeneralSection general = new GeneralSection();

        public static final class GeneralSection {
            public boolean prevent_climbing_in_rain = false;
        }
    }

    public static final class Data {
        public static boolean preventClimbingInRain() {
            return preventClimbingInRain;
        }

        public static void setPreventClimbingInRain(boolean value) {
            preventClimbingInRain = value;
            save();
        }
    }

    public static void load() {
        try {
            // Create config directory if it doesn't exist
            if (Files.notExists(CONFIG_FILE.getParent())) {
                Files.createDirectories(CONFIG_FILE.getParent());
            }

            // If config file doesn't exist, create it with defaults
            if (Files.notExists(CONFIG_FILE)) {
                save();
                return;
            }

            // Read and parse JSON config using Gson
            String content = Files.readString(CONFIG_FILE).trim();

            if (content.isEmpty()) {
                save();
                return;
            }

            // Parse with Gson instead of manually
            ConfigData data = GSON.fromJson(content, ConfigData.class);
            if (data != null && data.general != null) {
                preventClimbingInRain = data.general.prevent_climbing_in_rain;
            }

        } catch (IOException e) {
            Constants.LOG.error("Failed to load config, using defaults", e);
            // Use default values
            preventClimbingInRain = false;
        }
    }

    public static void save() {
        try {
            // Create config directory if it doesn't exist
            if (Files.notExists(CONFIG_FILE.getParent())) {
                Files.createDirectories(CONFIG_FILE.getParent());
            }

            // Create JSON content
            ConfigData data = new ConfigData();
            data.general.prevent_climbing_in_rain = preventClimbingInRain;

            Files.writeString(CONFIG_FILE, GSON.toJson(data));

        } catch (IOException e) {
            Constants.LOG.error("Failed to save config", e);
        }
    }
}