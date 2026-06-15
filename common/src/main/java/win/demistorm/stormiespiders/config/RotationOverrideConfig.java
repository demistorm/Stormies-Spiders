package win.demistorm.stormiespiders.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import win.demistorm.stormiespiders.Constants;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RotationOverrideConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_FILE = Path.of("config/stormiespiders/rotation-overrides.json");

	public enum OverrideStatus {
		NOT_LISTED,
		ROTATIONS_ONLY,
		DISABLED
	}

	private static volatile Map<String, Boolean> entityOverrides = new LinkedHashMap<>();
	private static volatile Set<String> allPatterns = ConcurrentHashMap.newKeySet();
	private static volatile Set<ResourceLocation> exactMatchCache = ConcurrentHashMap.newKeySet();
	private static volatile Map<String, Boolean> enabledPatterns = new ConcurrentHashMap<>();
	private static volatile Map<String, Boolean> disabledPatterns = new ConcurrentHashMap<>();
	private static volatile Set<ResourceLocation> enabledExactCache = ConcurrentHashMap.newKeySet();
	private static volatile Set<ResourceLocation> disabledExactCache = ConcurrentHashMap.newKeySet();

	public static void init() {
		loadRotationOverrides();
	}

	public static OverrideStatus getOverrideStatus(EntityType<?> entityType) {
		if (allPatterns.isEmpty()) {
			return OverrideStatus.NOT_LISTED;
		}

		ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
		String entityId = entityKey.toString();

		Boolean enabled = findMatch(entityKey, entityId);
		if (enabled == null) {
			return OverrideStatus.NOT_LISTED;
		}
		return enabled ? OverrideStatus.ROTATIONS_ONLY : OverrideStatus.DISABLED;
	}

	public static boolean isRotationOverrideEnabled(EntityType<?> entityType) {
		return getOverrideStatus(entityType) == OverrideStatus.ROTATIONS_ONLY;
	}

	public static boolean isClimberDisabled(EntityType<?> entityType) {
		return getOverrideStatus(entityType) != OverrideStatus.NOT_LISTED;
	}

	private static Boolean findMatch(ResourceLocation entityKey, String entityId) {
		if (enabledExactCache.contains(entityKey)) {
			return true;
		}
		if (disabledExactCache.contains(entityKey)) {
			return false;
		}

		for (Map.Entry<String, Boolean> entry : entityOverrides.entrySet()) {
			if (matchesPattern(entityId, entry.getKey())) {
				return entry.getValue();
			}
		}

		return null;
	}

	private static boolean matchesPattern(String entityId, String pattern) {
		if (!pattern.contains("*")) {
			return entityId.equals(pattern);
		}

		if (pattern.startsWith("*") && pattern.endsWith("*")) {
			String search = pattern.substring(1, pattern.length() - 1);
			return entityId.contains(search);
		} else if (pattern.startsWith("*")) {
			String suffix = pattern.substring(1);
			return entityId.endsWith(suffix);
		} else if (pattern.endsWith("*")) {
			String prefix = pattern.substring(0, pattern.length() - 1);
			return entityId.startsWith(prefix);
		}

		return entityId.equals(pattern);
	}

	public static Map<String, Boolean> getRotationOverrideMap() {
		return new LinkedHashMap<>(entityOverrides);
	}

	public static List<String> getRotationOverrideList() {
		return new ArrayList<>(entityOverrides.keySet());
	}

	public static boolean isSmartDefaultEnabled(String entityId) {
		String lower = entityId.toLowerCase();
		return !lower.contains("spider") && !lower.contains("cavespider");
	}

	public static void setRotationOverrideMap(Map<String, Boolean> overrides) {
		Map<String, Boolean> newOverrides = new LinkedHashMap<>();
		Set<String> newAllPatterns = ConcurrentHashMap.newKeySet();
		Set<ResourceLocation> newExactCache = ConcurrentHashMap.newKeySet();
		Map<String, Boolean> newEnabled = new ConcurrentHashMap<>();
		Map<String, Boolean> newDisabled = new ConcurrentHashMap<>();
		Set<ResourceLocation> newEnabledExact = ConcurrentHashMap.newKeySet();
		Set<ResourceLocation> newDisabledExact = ConcurrentHashMap.newKeySet();

		for (Map.Entry<String, Boolean> entry : overrides.entrySet()) {
			String pattern = entry.getKey().trim();
			Boolean enabled = entry.getValue();
			if (pattern.isEmpty()) {
				continue;
			}

			if (!pattern.contains("*")) {
				try {
					ResourceLocation key = ResourceLocation.parse(pattern);
					if (BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
						newOverrides.put(pattern, enabled);
						newAllPatterns.add(pattern);
						newExactCache.add(key);
						if (enabled) {
							newEnabled.put(pattern, true);
							newEnabledExact.add(key);
						} else {
							newDisabled.put(pattern, false);
							newDisabledExact.add(key);
						}
					} else {
						Constants.LOG.warn("[RotationOverrideConfig] Unknown entity type in config: {}", pattern);
					}
				} catch (Exception e) {
					Constants.LOG.warn("[RotationOverrideConfig] Invalid entity ID in config: {}", pattern, e);
				}
			} else {
				newOverrides.put(pattern, enabled);
				newAllPatterns.add(pattern);
				if (enabled) {
					newEnabled.put(pattern, true);
				} else {
					newDisabled.put(pattern, false);
				}
			}
		}

		entityOverrides = newOverrides;
		allPatterns = newAllPatterns;
		exactMatchCache = newExactCache;
		enabledPatterns = newEnabled;
		disabledPatterns = newDisabled;
		enabledExactCache = newEnabledExact;
		disabledExactCache = newDisabledExact;

		writeConfig(newOverrides);

		Constants.LOG.info("[RotationOverrideConfig] Updated {} entity override entries in config", newOverrides.size());
	}

	private static void loadRotationOverrides() {
		Map<String, Boolean> overrides = readConfig();

		Set<String> allPat = ConcurrentHashMap.newKeySet();
		Set<ResourceLocation> exactCache = ConcurrentHashMap.newKeySet();
		Map<String, Boolean> enabled = new ConcurrentHashMap<>();
		Map<String, Boolean> disabled = new ConcurrentHashMap<>();
		Set<ResourceLocation> enabledExact = ConcurrentHashMap.newKeySet();
		Set<ResourceLocation> disabledExact = ConcurrentHashMap.newKeySet();

		for (Map.Entry<String, Boolean> entry : overrides.entrySet()) {
			String pattern = entry.getKey().trim();
			Boolean enabledVal = entry.getValue();
			if (pattern.isEmpty()) {
				continue;
			}

			if (!pattern.contains("*")) {
				try {
					ResourceLocation key = ResourceLocation.parse(pattern);
					if (BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
						allPat.add(pattern);
						exactCache.add(key);
						if (enabledVal) {
							enabled.put(pattern, true);
							enabledExact.add(key);
						} else {
							disabled.put(pattern, false);
							disabledExact.add(key);
						}
					} else {
						Constants.LOG.warn("[RotationOverrideConfig] Unknown entity type in config: {}", pattern);
					}
				} catch (Exception e) {
					Constants.LOG.warn("[RotationOverrideConfig] Invalid entity ID in config: {}", pattern, e);
				}
			} else {
				allPat.add(pattern);
				if (enabledVal) {
					enabled.put(pattern, true);
				} else {
					disabled.put(pattern, false);
				}
			}
		}

		entityOverrides = overrides;
		allPatterns = allPat;
		exactMatchCache = exactCache;
		enabledPatterns = enabled;
		disabledPatterns = disabled;
		enabledExactCache = enabledExact;
		disabledExactCache = disabledExact;

		Constants.LOG.info("[RotationOverrideConfig] Loaded {} entity override entries from config", overrides.size());
	}

	private static Map<String, Boolean> readConfig() {
		try {
			if (Files.exists(CONFIG_FILE)) {
				String json = Files.readString(CONFIG_FILE);
				JsonObject root = GSON.fromJson(json, JsonObject.class);
				if (root == null) {
					return new LinkedHashMap<>();
				}

				if (root.has("rotation_override_entities")) {
					JsonElement elem = root.get("rotation_override_entities");

					if (elem.isJsonArray()) {
						return migrateOldFormat(elem.getAsJsonArray());
					}

					if (elem.isJsonObject()) {
						Type mapType = new TypeToken<LinkedHashMap<String, Boolean>>() {}.getType();
						return GSON.fromJson(elem, mapType);
					}
				}
			}
		} catch (IOException | JsonParseException e) {
			Constants.LOG.error("[RotationOverrideConfig] Failed to read config file", e);
		}

		Map<String, Boolean> empty = new LinkedHashMap<>();
		writeConfig(empty);
		return empty;
	}

	private static LinkedHashMap<String, Boolean> migrateOldFormat(JsonArray array) {
		LinkedHashMap<String, Boolean> migrated = new LinkedHashMap<>();
		for (JsonElement elem : array) {
			String entry = elem.getAsString().trim();
			if (!entry.isEmpty()) {
				migrated.put(entry, true);
			}
		}
		Constants.LOG.info("[RotationOverrideConfig] Migrated {} entries from old config format", migrated.size());
		writeConfig(migrated);
		return migrated;
	}

	private static void writeConfig(Map<String, Boolean> overrides) {
		try {
			Files.createDirectories(CONFIG_FILE.getParent());
			JsonObject root = new JsonObject();
			JsonObject map = new JsonObject();
			for (Map.Entry<String, Boolean> entry : overrides.entrySet()) {
				map.addProperty(entry.getKey(), entry.getValue());
			}
			root.add("rotation_override_entities", map);
			Files.writeString(CONFIG_FILE, GSON.toJson(root));
		} catch (IOException e) {
			Constants.LOG.error("[RotationOverrideConfig] Failed to write config file", e);
		}
	}

	private RotationOverrideConfig() {}
}
