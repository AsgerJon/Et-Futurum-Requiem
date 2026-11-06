package io.github.asgerjon.mcfly.base;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 26.3's `MultifaceSpreader` (SPEC-modern-movement.md §90.9, step H3): spreading a multiface block's faces, from a
 * face toward a direction, by its spread types in order, into what its config allows. Faces and directions are ints in
 * `ForgeDirection` (26.3's `Direction`) order: down, up, north, south, west, east. Glow lichen's spreading (26.3's
 * default config) and the sculk vein's (its own) both go through it.
 */
public class MultifaceSpreader {

	private static final int[][] OFFSET = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

	/** 26.3's `SpreadType`: where a spread from a face toward a direction lands. */
	public enum SpreadType {
		/** The same space, the face toward the direction. */
		SAME_POSITION {
			@Override
			public SpreadPos spreadPos(int x, int y, int z, int toward, int from) {
				return new SpreadPos(x, y, z, toward);
			}
		},
		/** The next space that way, the same face. */
		SAME_PLANE {
			@Override
			public SpreadPos spreadPos(int x, int y, int z, int toward, int from) {
				return new SpreadPos(x + OFFSET[toward][0], y + OFFSET[toward][1], z + OFFSET[toward][2], from);
			}
		},
		/** Round the edge: the space that way and past the face, the face turned back. */
		WRAP_AROUND {
			@Override
			public SpreadPos spreadPos(int x, int y, int z, int toward, int from) {
				return new SpreadPos(x + OFFSET[toward][0] + OFFSET[from][0], y + OFFSET[toward][1] + OFFSET[from][1], z + OFFSET[toward][2] + OFFSET[from][2],
					toward ^ 1);
			}
		};

		public abstract SpreadPos spreadPos(int x, int y, int z, int toward, int from);
	}

	/** 26.3's `DEFAULT_SPREAD_ORDER`. */
	public static final SpreadType[] DEFAULT_SPREAD_ORDER = {SpreadType.SAME_POSITION, SpreadType.SAME_PLANE, SpreadType.WRAP_AROUND};

	/** 26.3's `SpreadPos`: a place and a face there. */
	public static final class SpreadPos {
		public final int x, y, z, face;

		public SpreadPos(int x, int y, int z, int face) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.face = face;
		}
	}

	/** 26.3's `SpreadConfig`: what may be spread into and how it's placed. A source is a block and its faces (0 for a block of none). */
	public interface Config {
		boolean canSpreadInto(IBlockAccess world, int sx, int sy, int sz, SpreadPos spread);

		/** 26.3's `placeBlock`: the face put there. */
		boolean place(World world, SpreadPos spread, boolean postProcess);

		default SpreadType[] spreadTypes() {
			return DEFAULT_SPREAD_ORDER;
		}

		/** Whether the source has the face (26.3's `hasFace`). */
		default boolean hasFace(Block block, int faces, int face) {
			return (faces & 1 << face) != 0;
		}

		/** 26.3's `isOtherBlockValidAsSource`: a block not of faces that spreads from all its sides. */
		default boolean isOtherBlockValidAsSource(Block block) {
			return false;
		}

		default boolean canSpreadFrom(Block block, int faces, int face) {
			return isOtherBlockValidAsSource(block) || hasFace(block, faces, face);
		}
	}

	/** 26.3's `DefaultSpreaderConfig`: into air or the block itself, where the face can be placed. */
	public static class DefaultConfig implements Config {
		protected final McFlyMultiface block;

		public DefaultConfig(McFlyMultiface block) {
			this.block = block;
		}

		/** 26.3's `stateCanBeReplaced`: air or the block itself (1.7.10 has no waterlogged faces). */
		protected boolean stateCanBeReplaced(IBlockAccess world, int sx, int sy, int sz, SpreadPos spread, Block existing) {
			return existing.isAir(world, spread.x, spread.y, spread.z) || existing == block;
		}

		@Override
		public boolean canSpreadInto(IBlockAccess world, int sx, int sy, int sz, SpreadPos spread) {
			if (spread.y < 0 || spread.y >= 256) {
				return false;
			}
			Block existing = world.getBlock(spread.x, spread.y, spread.z);
			return stateCanBeReplaced(world, sx, sy, sz, spread, existing) && block.canPlaceFace(world, spread.x, spread.y, spread.z, spread.face);
		}

		@Override
		public boolean place(World world, SpreadPos spread, boolean postProcess) {
			return block.addFace(world, spread.x, spread.y, spread.z, spread.face, 2);
		}
	}

	private final Config config;

	public MultifaceSpreader(Config config) {
		this.config = config;
	}

	public MultifaceSpreader(McFlyMultiface block) {
		this(new DefaultConfig(block));
	}

	public Config config() {
		return config;
	}

	/** 26.3's `canSpreadInAnyDirection`: from that face, toward some direction. */
	public boolean canSpreadInAnyDirection(IBlockAccess world, int x, int y, int z, Block block, int faces, int startingFace) {
		for (int toward = 0; toward < 6; toward++) {
			if (spreadFrom(world, x, y, z, block, faces, startingFace, toward) != null) {
				return true;
			}
		}
		return false;
	}

	/** 26.3's `spreadFromRandomFaceTowardRandomDirection`: the faces shuffled, each toward the directions shuffled; the first spread made. */
	public SpreadPos spreadFromRandomFaceTowardRandomDirection(World world, int x, int y, int z, Block block, int faces, Random random) {
		for (int from : shuffled(random)) {
			if (!config.canSpreadFrom(block, faces, from)) {
				continue;
			}
			SpreadPos spread = spreadFromFaceTowardRandomDirection(world, x, y, z, block, faces, from, random, false);
			if (spread != null) {
				return spread;
			}
		}
		return null;
	}

	/** 26.3's `spreadAll`: from every face it may spread from, toward every direction; how many faces were placed. */
	public long spreadAll(World world, int x, int y, int z, Block block, int faces, boolean postProcess) {
		long placed = 0;
		for (int from = 0; from < 6; from++) {
			if (!config.canSpreadFrom(block, faces, from)) {
				continue;
			}
			for (int toward = 0; toward < 6; toward++) {
				if (spreadFromFaceTowardDirection(world, x, y, z, block, faces, from, toward, postProcess) != null) {
					placed++;
				}
			}
		}
		return placed;
	}

	public SpreadPos spreadFromFaceTowardRandomDirection(World world, int x, int y, int z, Block block, int faces, int from, Random random, boolean postProcess) {
		for (int toward : shuffled(random)) {
			SpreadPos spread = spreadFromFaceTowardDirection(world, x, y, z, block, faces, from, toward, postProcess);
			if (spread != null) {
				return spread;
			}
		}
		return null;
	}

	/** 26.3's `spreadFromFaceTowardDirection`: where it would go, then placed there. */
	public SpreadPos spreadFromFaceTowardDirection(World world, int x, int y, int z, Block block, int faces, int from, int toward, boolean postProcess) {
		SpreadPos spread = spreadFrom(world, x, y, z, block, faces, from, toward);
		return spread != null && config.place(world, spread, postProcess) ? spread : null;
	}

	/** 26.3's `getSpreadFromFaceTowardDirection`: not along the face's own axis; from a face it has toward one it hasn't (or any, from another block); the first spread type that may. */
	public SpreadPos spreadFrom(IBlockAccess world, int x, int y, int z, Block block, int faces, int from, int toward) {
		if (toward >> 1 == from >> 1) {
			return null;
		}
		if (!config.isOtherBlockValidAsSource(block) && !(config.hasFace(block, faces, from) && !config.hasFace(block, faces, toward))) {
			return null;
		}
		for (SpreadType type : config.spreadTypes()) {
			SpreadPos spread = type.spreadPos(x, y, z, toward, from);
			if (config.canSpreadInto(world, x, y, z, spread)) {
				return spread;
			}
		}
		return null;
	}

	/** 26.3's `Direction.allShuffled`: the six in their order, shuffled as 26.3's `Util.shuffle` (Java's shuffle, the same steps). */
	private static List<Integer> shuffled(Random random) {
		List<Integer> order = new ArrayList<>(6);
		for (int face = 0; face < 6; face++) {
			order.add(face);
		}
		Collections.shuffle(order, random);
		return order;
	}
}
