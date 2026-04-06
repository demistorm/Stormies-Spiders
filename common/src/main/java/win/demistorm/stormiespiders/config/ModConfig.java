package win.demistorm.stormiespiders.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import win.demistorm.stormiespiders.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ModConfig {

    private static final Path CONFIG_FILE = Paths.get("config", "stormiespiders", "stormiespiders.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean preventClimbingInRain = false;
    private static boolean disableDataSync = false;
    private static boolean canCrawlOnCeiling = true;
    private static boolean enableFallbackRotation = true;
    private static int fallbackUpdateInterval = 1;
    private static boolean canSwim = true;

    // Data structure matching the JSON format
    public static final class ConfigData {
        public GeneralSection general = new GeneralSection();

        public static final class GeneralSection {
            public boolean prevent_climbing_in_rain = false;
            public boolean disable_data_sync = false;
            public boolean can_crawl_on_ceiling = true;
            public boolean enable_fallback_rotation = true;
            public int fallback_update_interval = 1;
            public boolean can_swim = true;
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

        public static boolean disableDataSync() {
            return disableDataSync;
        }

        public static boolean canCrawlOnCeiling() {
            return canCrawlOnCeiling;
        }

        public static void setCanCrawlOnCeiling(boolean value) {
            canCrawlOnCeiling = value;
            save();
        }

        public static boolean enableFallbackRotation() {
            return enableFallbackRotation;
        }

        public static int fallbackUpdateInterval() {
            return fallbackUpdateInterval;
        }

        public static boolean canSwim() {
            return canSwim;
        }

        public static void setCanSwim(boolean value) {
            canSwim = value;
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
                disableDataSync = data.general.disable_data_sync;
                canCrawlOnCeiling = data.general.can_crawl_on_ceiling;
                enableFallbackRotation = data.general.enable_fallback_rotation;
                fallbackUpdateInterval = data.general.fallback_update_interval;
                canSwim = data.general.can_swim;
            }

            save();

        } catch (IOException e) {
            Constants.LOG.error("Failed to load config, using defaults", e);
            // Use default values
            preventClimbingInRain = false;
            disableDataSync = false;
            canCrawlOnCeiling = true;
            enableFallbackRotation = true;
            fallbackUpdateInterval = 1;
            canSwim = true;
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
            data.general.disable_data_sync = disableDataSync;
            data.general.can_crawl_on_ceiling = canCrawlOnCeiling;
            data.general.enable_fallback_rotation = enableFallbackRotation;
            data.general.fallback_update_interval = fallbackUpdateInterval;
            data.general.can_swim = canSwim;

            Files.writeString(CONFIG_FILE, GSON.toJson(data));

        } catch (IOException e) {
            Constants.LOG.error("Failed to save config", e);
        }
    }
}