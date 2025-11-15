package win.demistorm.stormiespiders.config;

import win.demistorm.stormiespiders.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ModConfig {

    private static final Path CONFIG_FILE = Paths.get("config", "stormiespiders.json");
    private static boolean preventClimbingInRain = false;

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

            // Read and parse JSON config
            String content = Files.readString(CONFIG_FILE);
            content = content.trim();

            if (content.isEmpty()) {
                save();
                return;
            }

            // JSON parsing
            content = content.substring(1, content.length() - 1); // Remove outer braces

            if (content.contains("\"general\":")) {
                String generalSection = extractSection(content, "general");
                if (generalSection != null) {
                    preventClimbingInRain = parseBoolean(generalSection, "prevent_climbing_in_rain", false);
                }
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
            String json = String.format(
                "{\n" +
                "  \"general\": {\n" +
                "    \"prevent_climbing_in_rain\": %b\n" +
                "  }\n" +
                "}",
                preventClimbingInRain
            );

            Files.writeString(CONFIG_FILE, json);

        } catch (IOException e) {
            Constants.LOG.error("Failed to save config", e);
        }
    }

    private static String extractSection(String content, String sectionName) {
        String searchPattern = "\"" + sectionName + "\":";
        int startIndex = content.indexOf(searchPattern);
        if (startIndex == -1) return null;

        startIndex = content.indexOf('{', startIndex);
        if (startIndex == -1) return null;

        int braceCount = 1;
        int endIndex = startIndex + 1;

        while (endIndex < content.length() && braceCount > 0) {
            char c = content.charAt(endIndex);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            endIndex++;
        }

        if (braceCount == 0) {
            return content.substring(startIndex + 1, endIndex - 1);
        }

        return null;
    }

    private static boolean parseBoolean(String section, String key, boolean defaultValue) {
        String searchPattern = "\"" + key + "\":";
        int index = section.indexOf(searchPattern);
        if (index == -1) return defaultValue;

        index += searchPattern.length();

        // Skip whitespace
        while (index < section.length() && Character.isWhitespace(section.charAt(index))) {
            index++;
        }

        if (index >= section.length()) return defaultValue;

        // Parse boolean value
        if (section.substring(index).startsWith("true")) {
            return true;
        } else if (section.substring(index).startsWith("false")) {
            return false;
        }

        return defaultValue;
    }
}