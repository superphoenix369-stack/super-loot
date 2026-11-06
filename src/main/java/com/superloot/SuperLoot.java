package com.superloot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootTable;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import com.superloot.config.SuperLootConfig;
import com.superloot.world.LootChestFeature;

/**
 * SuperLoot: scatters 0-3 loot chests across every 256x256 block area of the Overworld surface
 * (on land, or on the sea floor when the spot is under water).
 */
public final class SuperLoot implements ModInitializer {
	public static final String MOD_ID = "superloot";
	public static final Logger LOGGER = LoggerFactory.getLogger("SuperLoot");

	public static final ResourceKey<LootTable> COMMON_LOOT = lootTable("chests/common");
	public static final ResourceKey<LootTable> RARE_LOOT = lootTable("chests/rare");

	public static final ResourceKey<PlacedFeature> LOOT_CHESTS_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, id("loot_chests"));

	@Override
	public void onInitialize() {
		SuperLootConfig.load();

		// The feature type that does the actual chest placement.
		Registry.register(BuiltInRegistries.FEATURE_TYPE, id("loot_chests"), LootChestFeature.CODEC);

		// Run it in every Overworld biome, in the last decoration step (after trees, plants and snow),
		// so chests sit on the finished surface.
		BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
				LOOT_CHESTS_PLACED
		);

		LOGGER.info("SuperLoot ready: chest amount = {}, loot quality = {}",
				SuperLootConfig.chestAmount(), SuperLootConfig.lootQuality());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static ResourceKey<LootTable> lootTable(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, id(path));
	}
}
