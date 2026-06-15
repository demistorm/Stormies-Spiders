package win.demistorm.stormiespiders.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import win.demistorm.stormiespiders.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NonClimbableBlocksConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_FILE = Path.of("config/stormiespiders/non-climbable-blocks.json");

	// In-memory cache of non-climbable block patterns
	private static volatile Set<String> nonClimbablePatterns = ConcurrentHashMap.newKeySet();
	private static volatile Set<ResourceLocation> exactMatchCache = ConcurrentHashMap.newKeySet();

	// Per-block result cache
	private static final Map<Block, Boolean> blockResultCache = new ConcurrentHashMap<>();

	// Initialize config
	public static void init() {
		loadNonClimbableBlocks();
	}

	// Configuration data class
	private static class NonClimbableBlocksConfigData {
		public List<String> non_climbable_blocks = List.of("*trapdoor");
	}

	// Check if a block state should be treated as non-climbable
	public static boolean isBlockNonClimbable(BlockState state) {
		Block block = state.getBlock();
		Boolean cached = blockResultCache.get(block);
		if (cached != null) {
			return cached;
		}

		boolean result = computeIsBlockNonClimbable(state);
		blockResultCache.put(block, result);
		return result;
	}

	// Uncached pattern matching
	private static boolean computeIsBlockNonClimbable(BlockState state) {
		if (nonClimbablePatterns.isEmpty()) {
			return false;
		}

		ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		String blockId = blockKey.toString();

		// Check exact match cache first for performance
		if (exactMatchCache.contains(blockKey)) {
			return true;
		}

		// Check all patterns
		for (String pattern : nonClimbablePatterns) {
			if (matchesPattern(blockId, pattern)) {
				return true;
			}
		}

		return false;
	}

	// Wildcard pattern matching
	private static boolean matchesPattern(String blockId, String pattern) {
		if (!pattern.contains("*")) {
			// Exact match
			return blockId.equals(pattern);
		}

		if (pattern.startsWith("*") && pattern.endsWith("*")) {
			// Contains match: "*door*" matches "oak_door", "trapdoor", etc.
			String search = pattern.substring(1, pattern.length() - 1);
			return blockId.contains(search);
		} else if (pattern.startsWith("*")) {
			// Suffix match: "*trapdoor" matches "oak_trapdoor", "iron_trapdoor"
			String suffix = pattern.substring(1);
			return blockId.endsWith(suffix);
		} else if (pattern.endsWith("*")) {
			// Prefix match: "minecraft:*" matches all minecraft blocks
			String prefix = pattern.substring(0, pattern.length() - 1);
			return blockId.startsWith(prefix);
		}

		// Fallback to exact match
		return blockId.equals(pattern);
	}

	// Get current non-climbable blocks as a list (for config screen)
	public static List<String> getNonClimbableBlocksList() {
		return new ArrayList<>(nonClimbablePatterns);
	}

	// Set non-climbable blocks from a list and save to config (for config screen)
	public static void setNonClimbableBlocksList(List<String> patterns) {
		Set<String> newPatterns = ConcurrentHashMap.newKeySet();
		Set<ResourceLocation> newCache = ConcurrentHashMap.newKeySet();

		// Validate and process patterns
		for (String pattern : patterns) {
			String trimmed = pattern.trim();
			if (trimmed.isEmpty()) {
				continue;
			}

			// Check if it's an exact match (no wildcard)
			if (!trimmed.contains("*")) {
				try {
					ResourceLocation key = new ResourceLocation(trimmed);
					if (BuiltInRegistries.BLOCK.containsKey(key)) {
						newPatterns.add(trimmed);
						newCache.add(key);
					} else {
						Constants.LOG.warn("[NonClimbableBlocksConfig] Unknown block in config: {}", trimmed);
					}
				} catch (Exception e) {
					Constants.LOG.warn("[NonClimbableBlocksConfig] Invalid block ID in config: {}", trimmed, e);
				}
			} else {
				// Wildcard pattern - just add it
				newPatterns.add(trimmed);
			}
		}

		nonClimbablePatterns = newPatterns;
		exactMatchCache = newCache;
		blockResultCache.clear();

		// Save to config file
		NonClimbableBlocksConfigData config = new NonClimbableBlocksConfigData();
		config.non_climbable_blocks = new ArrayList<>(patterns);
		writeConfig(config);

		Constants.LOG.info("[NonClimbableBlocksConfig] Updated {} non-climbable block patterns in config", newPatterns.size());
	}

	// Load non-climbable blocks from config file
	private static void loadNonClimbableBlocks() {
		NonClimbableBlocksConfigData config = readConfig();

		Set<String> patterns = ConcurrentHashMap.newKeySet();
		Set<ResourceLocation> cache = ConcurrentHashMap.newKeySet();

		for (String pattern : config.non_climbable_blocks) {
			String trimmed = pattern.trim();
			if (trimmed.isEmpty()) {
				continue;
			}

			// Check if it's an exact match (no wildcard)
			if (!trimmed.contains("*")) {
				try {
					ResourceLocation key = new ResourceLocation(trimmed);
					if (BuiltInRegistries.BLOCK.containsKey(key)) {
						patterns.add(trimmed);
						cache.add(key);
					} else {
						Constants.LOG.warn("[NonClimbableBlocksConfig] Unknown block in config: {}", trimmed);
					}
				} catch (Exception e) {
					Constants.LOG.warn("[NonClimbableBlocksConfig] Invalid block ID in config: {}", trimmed, e);
				}
			} else {
				// Wildcard pattern - just add it
				patterns.add(trimmed);
			}
		}

		nonClimbablePatterns = patterns;
		exactMatchCache = cache;
		blockResultCache.clear();

		Constants.LOG.info("[NonClimbableBlocksConfig] Loaded {} non-climbable block patterns from config", patterns.size());
	}

	private static NonClimbableBlocksConfigData readConfig() {
		try {
			if (Files.exists(CONFIG_FILE)) {
				String json = Files.readString(CONFIG_FILE);
				return GSON.fromJson(json, NonClimbableBlocksConfigData.class);
			}
		} catch (IOException e) {
			Constants.LOG.error("[NonClimbableBlocksConfig] Failed to read config file", e);
		}

		// Return default config if file doesn't exist or is invalid
		NonClimbableBlocksConfigData config = new NonClimbableBlocksConfigData();
		writeConfig(config);
		return config;
	}

	private static void writeConfig(NonClimbableBlocksConfigData config) {
		try {
			Files.createDirectories(CONFIG_FILE.getParent());
			String json = GSON.toJson(config);
			Files.writeString(CONFIG_FILE, json);
		} catch (IOException e) {
			Constants.LOG.error("[NonClimbableBlocksConfig] Failed to write config file", e);
		}
	}

	private NonClimbableBlocksConfig() {}
}
