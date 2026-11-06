package com.superloot.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

import com.superloot.SuperLoot;

/**
 * SuperLoot settings, stored in config/superloot.json.
 * Changes apply to land that hasn't been explored yet; chests already in the world stay as they are.
 */
public final class SuperLootConfig {
	/** How many chests appear in each 256x256 block area. */
	public enum ChestAmount {
		LOW(0, 1),
		MEDIUM(1, 2),
		HIGH(2, 3);

		public final int min;
		public final int max;

		ChestAmount(int min, int max) {
			this.min = min;
			this.max = max;
		}
	}

	/** What kind of loot the chests hold. */
	public enum LootQuality {
		/** Everyday supplies: food, torches, basic ores, simple tools. */
		COMMON,
		/** Mostly common chests, with about one in four being a rare chest. */
		MIXED,
		/** Treasure: diamonds, enchanted gear, golden apples and more. */
		RARE
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "superloot.json";

	private static volatile ChestAmount chestAmount = ChestAmount.MEDIUM;
	private static volatile LootQuality lootQuality = LootQuality.COMMON;

	private SuperLootConfig() {
	}

	public static ChestAmount chestAmount() {
		return chestAmount;
	}

	public static LootQuality lootQuality() {
		return lootQuality;
	}

	public static void setChestAmount(ChestAmount value) {
		chestAmount = value;
	}

	public static void setLootQuality(LootQuality value) {
		lootQuality = value;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	public static void load() {
		Path file = path();

		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				chestAmount = readEnum(json, "chest_amount", ChestAmount.class, ChestAmount.MEDIUM);
				lootQuality = readEnum(json, "loot_quality", LootQuality.class, LootQuality.COMMON);
			} catch (Exception e) {
				SuperLoot.LOGGER.warn("Could not read {}, using default settings", file, e);
			}
		}

		// Write the file back so it always exists and lists every setting.
		save();
	}

	public static void save() {
		JsonObject json = new JsonObject();
		json.addProperty("_help_chest_amount", "low = 0-1, medium = 1-2, high = 2-3 chests per 256x256 block area");
		json.addProperty("chest_amount", chestAmount.name().toLowerCase(Locale.ROOT));
		json.addProperty("_help_loot_quality", "common = everyday supplies, mixed = mostly common with some rare, rare = treasure");
		json.addProperty("loot_quality", lootQuality.name().toLowerCase(Locale.ROOT));

		Path file = path();

		try {
			Files.createDirectories(file.getParent());

			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(json, writer);
			}
		} catch (IOException e) {
			SuperLoot.LOGGER.warn("Could not save {}", file, e);
		}
	}

	private static <E extends Enum<E>> E readEnum(JsonObject json, String key, Class<E> type, E fallback) {
		if (!json.has(key)) {
			return fallback;
		}

		try {
			return Enum.valueOf(type, json.get(key).getAsString().trim().toUpperCase(Locale.ROOT));
		} catch (Exception e) {
			SuperLoot.LOGGER.warn("Unknown value for '{}' in {}, using {}", key, FILE_NAME, fallback.name().toLowerCase(Locale.ROOT));
			return fallback;
		}
	}
}
