package io.github.asgerjon.mcfly.tree;

import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A tree of McFly's tree system (SPEC-modern-movement.md §90.5, §90.6), 26.3's `TreeFeature` in 1.7.10's blocks: its
 * trunk and foliage blocks, trunk and foliage placers, its root placer if any, minimum size, the block under its trunk
 * and its decorators. Placed only where it fits at its full height (or the size's clipped height): the roots, the trunk
 * (from above the roots), foliage at each of the trunk's attachments, then the decorators over what was placed.
 */
public final class Tree {

	final BlockProvider trunk;
	final BlockProvider foliage;
	final BlockProvider belowTrunk;
	private final TrunkPlacer trunkPlacer;
	private final FoliagePlacer foliagePlacer;
	private final RootPlacer rootPlacer;
	private final FeatureSize size;
	private final boolean ignoreVines;
	private final List<TreeDecorator> decorators;

	/** What this placing put down (26.3's trunk, foliage and root sets): on the server thread, one tree at a time. */
	private final List<int[]> logs = new ArrayList<>();
	private final List<int[]> leaves = new ArrayList<>();
	private final List<int[]> roots = new ArrayList<>();
	private final Set<Long> leafSet = new HashSet<>();

	public Tree(BlockProvider trunk, TrunkPlacer trunkPlacer, BlockProvider foliage, FoliagePlacer foliagePlacer, FeatureSize size, BlockProvider belowTrunk,
				boolean ignoreVines) {
		this(trunk, trunkPlacer, foliage, foliagePlacer, null, size, belowTrunk, ignoreVines, Collections.<TreeDecorator>emptyList());
	}

	public Tree(BlockProvider trunk, TrunkPlacer trunkPlacer, BlockProvider foliage, FoliagePlacer foliagePlacer, RootPlacer rootPlacer, FeatureSize size,
				BlockProvider belowTrunk, boolean ignoreVines, List<TreeDecorator> decorators) {
		this.trunk = trunk;
		this.trunkPlacer = trunkPlacer;
		this.foliage = foliage;
		this.foliagePlacer = foliagePlacer;
		this.rootPlacer = rootPlacer;
		this.size = size;
		this.belowTrunk = belowTrunk;
		this.ignoreVines = ignoreVines;
		this.decorators = decorators;
	}

	/** 26.3's `#replaceable_by_trees` (as far as 1.7.10 and McFly have them), or air: where a tree may put a block. */
	public static boolean validTreePos(World world, int x, int y, int z) {
		if (y < 0 || y >= world.getHeight()) {
			return false;
		}
		Block block = world.getBlock(x, y, z);
		return block.isAir(world, x, y, z) || block.isLeaves(world, x, y, z) || block == Blocks.tallgrass || block == Blocks.deadbush || block == Blocks.vine
			|| block == Blocks.double_plant || block == Blocks.yellow_flower || block == Blocks.red_flower || block.getMaterial() == Material.water
			|| block == McFlyBlocks.GLOW_LICHEN.get() || block == McFlyBlocks.HANGING_ROOTS.get() || block == McFlyBlocks.NETHER_ROOTS.get()
			|| block == McFlyBlocks.NETHER_SPROUTS.get();
	}

	static boolean isLog(World world, int x, int y, int z) {
		return world.getBlock(x, y, z).isWood(world, x, y, z);
	}

	static long key(int x, int y, int z) {
		return ((long) x & 0x3FFFFFF) << 38 | ((long) y & 0xFFF) << 26 | (long) z & 0x3FFFFFF;
	}

	/** A trunk block (26.3's trunk setter: the block below the trunk too). */
	void setLog(World world, int x, int y, int z, BlockProvider.State state) {
		logs.add(new int[]{x, y, z});
		world.setBlock(x, y, z, state.block, state.meta, 3);
	}

	/** A foliage block (26.3's foliage setter). */
	void setLeaf(World world, int x, int y, int z, BlockProvider.State state) {
		if (leafSet.add(key(x, y, z))) {
			leaves.add(new int[]{x, y, z});
		}
		world.setBlock(x, y, z, state.block, state.meta, 3);
	}

	/** A root, or what goes on one (26.3's root setter). */
	void setRoot(World world, int x, int y, int z, BlockProvider.State state) {
		roots.add(new int[]{x, y, z});
		world.setBlock(x, y, z, state.block, state.meta, 3);
	}

	/** Whether this placing has put foliage there (26.3's `FoliageSetter.isSet`). */
	boolean isLeafSet(int x, int y, int z) {
		return leafSet.contains(key(x, y, z));
	}

	/** 26.3's `TreeFeature.place` from (x, y, z), the sapling's place. */
	public boolean place(World world, Random random, int x, int y, int z) {
		logs.clear();
		leaves.clear();
		roots.clear();
		leafSet.clear();
		int treeHeight = trunkPlacer.treeHeight(random);
		int foliageHeight = foliagePlacer.foliageHeight(random);
		int radius = foliagePlacer.radius(random);
		int trunkY = rootPlacer == null ? y : rootPlacer.trunkOriginY(y, random);
		if (Math.min(y, trunkY) < 1 || Math.max(y, trunkY) + treeHeight + 1 > world.getHeight()) {
			return false;
		}
		int clipped = maxFreeHeight(world, treeHeight, x, trunkY, z);
		if (clipped < treeHeight && (size.minClippedHeight < 0 || clipped < size.minClippedHeight)) {
			return false;
		}
		if (rootPlacer != null && !rootPlacer.placeRoots(world, random, x, y, z, trunkY, this)) {
			return false;
		}
		List<FoliagePlacer.Attachment> attachments = trunkPlacer.placeTrunk(world, random, clipped, x, trunkY, z, this);
		for (FoliagePlacer.Attachment attachment : attachments) {
			foliagePlacer.createFoliage(world, random, this, clipped, attachment, foliageHeight, radius);
		}
		if (!decorators.isEmpty() && (!logs.isEmpty() || !leaves.isEmpty())) {
			TreeDecorator.Context context = new TreeDecorator.Context(world, random, logs, leaves, roots);
			for (TreeDecorator decorator : decorators) {
				decorator.place(context);
			}
		}
		return true;
	}

	/** 26.3's `getMaxFreeTreeHeight`: climbing from the trunk's base, each layer's square as wide as the size says. */
	private int maxFreeHeight(World world, int treeHeight, int x, int y, int z) {
		for (int dy = 0; dy <= treeHeight + 1; dy++) {
			int r = size.sizeAt(treeHeight, dy);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (!trunkPlacer.isFree(world, x + dx, y + dy, z + dz) || !ignoreVines && world.getBlock(x + dx, y + dy, z + dz) == Blocks.vine) {
						return dy - 2;
					}
				}
			}
		}
		return treeHeight;
	}

	/** 26.3's `TreeGrower.growTree` from a sapling: its place emptied first, the sapling put back if the tree doesn't fit. */
	public boolean grow(World world, Random random, int x, int y, int z) {
		Block sapling = world.getBlock(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);
		world.setBlock(x, y, z, Blocks.air, 0, 4);
		if (place(world, random, x, y, z)) {
			return true;
		}
		world.setBlock(x, y, z, sapling, meta, 4);
		return false;
	}
}
