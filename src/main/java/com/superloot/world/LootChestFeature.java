package com.superloot.world;

import java.util.SplittableRandom;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootTable;

import com.superloot.SuperLoot;
import com.superloot.config.SuperLootConfig;

/**
 * Places SuperLoot chests on the Overworld surface.
 *
 * <p>The world is divided into a grid of 256x256 block areas (16x16 chunks each). For every area,
 * a plan is worked out from the world seed: how many chests it gets and which chunk each chest is in.
 * Because the plan depends only on the seed and the area's position, every chunk of an area agrees
 * on the same plan, no matter what order chunks are generated in.
 *
 * <p>This runs once per chunk, in the last decoration step, so trees, plants and snow are already there.
 */
public record LootChestFeature() implements Feature {
	public static final LootChestFeature INSTANCE = new LootChestFeature();
	public static final MapCodec<LootChestFeature> CODEC = MapCodec.unit(INSTANCE);

	/** 16 chunks x 16 blocks = 256 blocks per side. */
	private static final int AREA_SIZE_IN_CHUNKS = 16;
	private static final int CHUNKS_PER_AREA = AREA_SIZE_IN_CHUNKS * AREA_SIZE_IN_CHUNKS;

	/** How many spots to try inside the chosen chunk before giving up on a chest. */
	private static final int PLACEMENT_ATTEMPTS = 48;

	/** How far down to look from the highest block (covers deep oceans and tall trees). */
	private static final int MAX_SCAN_DEPTH = 128;

	/** In "Mixed" loot mode, the chance a chest is a rare one. */
	private static final double MIXED_RARE_CHANCE = 0.25;

	@Override
	public MapCodec<LootChestFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
		int chunkX = origin.getX() >> 4;
		int chunkZ = origin.getZ() >> 4;
		int areaX = Math.floorDiv(chunkX, AREA_SIZE_IN_CHUNKS);
		int areaZ = Math.floorDiv(chunkZ, AREA_SIZE_IN_CHUNKS);
		int chunkIndexInArea = Math.floorMod(chunkX, AREA_SIZE_IN_CHUNKS) + Math.floorMod(chunkZ, AREA_SIZE_IN_CHUNKS) * AREA_SIZE_IN_CHUNKS;

		long areaSeed = areaSeed(level.getSeed(), areaX, areaZ);
		SplittableRandom areaRandom = new SplittableRandom(areaSeed);

		SuperLootConfig.ChestAmount amount = SuperLootConfig.chestAmount();
		int chestCount = amount.min + areaRandom.nextInt(amount.max - amount.min + 1);
		int[] chestChunks = pickDistinctChunks(areaRandom, chestCount);

		boolean placedAny = false;

		for (int i = 0; i < chestCount; i++) {
			if (chestChunks[i] != chunkIndexInArea) {
				continue;
			}

			SplittableRandom chestRandom = new SplittableRandom(areaSeed ^ ((i + 1) * 0x9E3779B97F4A7C15L));
			placedAny |= tryPlaceChest(level, random, chestRandom, chunkX << 4, chunkZ << 4);
		}

		return placedAny;
	}

	private static boolean tryPlaceChest(WorldGenLevel level, RandomSource random, SplittableRandom chestRandom, int chunkMinX, int chunkMinZ) {
		boolean rare = switch (SuperLootConfig.lootQuality()) {
			case COMMON -> false;
			case RARE -> true;
			case MIXED -> chestRandom.nextDouble() < MIXED_RARE_CHANCE;
		};
		ResourceKey<LootTable> lootTable = rare ? SuperLoot.RARE_LOOT : SuperLoot.COMMON_LOOT;

		for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
			int x = chunkMinX + chestRandom.nextInt(16);
			int z = chunkMinZ + chestRandom.nextInt(16);
			BlockPos spot = findSurfaceSpot(level, x, z);

			if (spot != null) {
				placeChest(level, random, spot, lootTable);
				return true;
			}
		}

		SuperLoot.LOGGER.debug("No room for a chest in chunk at {}, {}", chunkMinX, chunkMinZ);
		return false;
	}

	/**
	 * Finds the block just above the ground at this column: on land that is the grass/sand/stone surface
	 * (under any trees), and in water it is the sea, lake or river floor.
	 * Returns null if the spot is no good (for example a lava lake).
	 */
	private static BlockPos findSurfaceSpot(WorldGenLevel level, int x, int z) {
		int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
		int bottom = Math.max(level.getMinY(), top - MAX_SCAN_DEPTH);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int y = top - 1; y >= bottom; y--) {
			BlockState state = level.getBlockState(pos.set(x, y, z));

			if (state.getFluidState().is(FluidTags.LAVA)) {
				return null;
			}

			if (isGround(state)) {
				BlockPos target = new BlockPos(x, y + 1, z);

				if (target.getY() > level.getMaxY()) {
					return null;
				}

				return canPutChestIn(level.getBlockState(target)) ? target : null;
			}
		}

		return null;
	}

	/** Solid ground a chest can sit on. Tree leaves and logs don't count, so chests end up under trees. */
	private static boolean isGround(BlockState state) {
		return state.isSolid()
				&& !state.canBeReplaced()
				&& !state.is(BlockTags.LEAVES)
				&& !state.is(BlockTags.LOGS)
				&& state.getFluidState().isEmpty();
	}

	/** Air, water, or soft stuff like grass, flowers, seagrass or a snow layer. */
	private static boolean canPutChestIn(BlockState state) {
		if (state.getFluidState().is(FluidTags.LAVA)) {
			return false;
		}

		return state.isAir() || state.canBeReplaced();
	}

	private static void placeChest(WorldGenLevel level, RandomSource random, BlockPos pos, ResourceKey<LootTable> lootTable) {
		FluidState fluid = level.getFluidState(pos);
		boolean underWater = fluid.is(FluidTags.WATER);

		BlockState chest = Blocks.CHEST.defaultBlockState()
				.setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random))
				.setValue(ChestBlock.WATERLOGGED, underWater);

		level.setBlock(pos, chest, Block.UPDATE_CLIENTS);
		RandomizableContainer.setBlockEntityLootTable(level, random, pos, lootTable);

		removeUnsupportedBlocksAbove(level, pos);

		SuperLoot.LOGGER.debug("Placed {} chest at {}", lootTable == SuperLoot.RARE_LOOT ? "rare" : "common", pos);
	}

	/**
	 * If the chest replaced the bottom of a tall plant (tall grass, kelp, sugar cane...),
	 * remove the floating part above it, leaving water behind where there was water.
	 */
	private static void removeUnsupportedBlocksAbove(WorldGenLevel level, BlockPos chestPos) {
		BlockPos.MutableBlockPos pos = chestPos.mutable();

		for (int i = 0; i < 64; i++) {
			pos.move(Direction.UP);

			if (pos.getY() > level.getMaxY()) {
				return;
			}

			BlockState state = level.getBlockState(pos);

			if (state.isAir() || state.canSurvive(level, pos)) {
				return;
			}

			level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_CLIENTS);
		}
	}

	/** Picks a different chunk (0-255 within the area) for each chest, so chests are spread out. */
	private static int[] pickDistinctChunks(SplittableRandom random, int count) {
		int[] chunks = new int[count];

		for (int i = 0; i < count; i++) {
			int candidate;
			boolean taken;

			do {
				candidate = random.nextInt(CHUNKS_PER_AREA);
				taken = false;

				for (int j = 0; j < i; j++) {
					if (chunks[j] == candidate) {
						taken = true;
						break;
					}
				}
			} while (taken);

			chunks[i] = candidate;
		}

		return chunks;
	}

	private static long areaSeed(long worldSeed, int areaX, int areaZ) {
		long seed = worldSeed ^ 0x5355504552L; // "SUPER"
		seed ^= areaX * 0x632BE59BD9B4E019L;
		seed ^= areaZ * 0x85157AF5L;
		// Final mix (from SplitMix64) so neighbouring areas get unrelated plans.
		seed = (seed ^ (seed >>> 30)) * 0xBF58476D1CE4E5B9L;
		seed = (seed ^ (seed >>> 27)) * 0x94D049BB133111EBL;
		return seed ^ (seed >>> 31);
	}
}
