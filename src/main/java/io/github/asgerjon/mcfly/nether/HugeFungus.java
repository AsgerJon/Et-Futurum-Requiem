package io.github.asgerjon.mcfly.nether;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 26.3's huge fungus (SPEC-modern-movement.md §88.7), its `HugeFungusFeature` in 1.7.10's terms: on its nylium, a stem
 * 4 to 13 tall (doubled 1 time in 12), a hat of wart blocks with shroomlights (and, for the crimson one, weeping vines
 * hanging from it), 26.3's numbers throughout; planted (from a fungus's bonemeal) it breaks what's in the way and is never
 * the 3 by 3 huge kind; not planted (world generation), 1 in 17 (0.06) of them are.
 */
public final class HugeFungus {

	private final Block base;
	private final int baseMeta;
	private final Block stem;
	private final int stemMeta;
	private final Block hat;
	private final int hatMeta;
	private final Block decor;
	private final boolean vines;
	private final boolean planted;

	public HugeFungus(Block base, int baseMeta, Block stem, int stemMeta, Block hat, int hatMeta, Block decor, boolean vines, boolean planted) {
		this.base = base;
		this.baseMeta = baseMeta;
		this.stem = stem;
		this.stemMeta = stemMeta;
		this.hat = hat;
		this.hatMeta = hatMeta;
		this.decor = decor;
		this.vines = vines;
		this.planted = planted;
	}

	private static int between(Random random, int min, int max) {
		return min + random.nextInt(max - min + 1);
	}

	/** 26.3's `place`: false when it can't grow here (not on its nylium, or too tall for the world). */
	public boolean place(World world, Random random, int x, int y, int z) {
		if (world.getBlock(x, y - 1, z) != base || world.getBlockMetadata(x, y - 1, z) != baseMeta) {
			return false;
		}
		int height = between(random, 4, 13);
		if (random.nextInt(12) == 0) {
			height *= 2;
		}
		if (!planted && y + height + 1 >= world.getHeight()) {
			return false;
		}
		boolean huge = !planted && random.nextFloat() < 0.06F;
		world.setBlock(x, y, z, Blocks.air, 0, 4);
		placeStem(world, random, x, y, z, height, huge);
		placeHat(world, random, x, y, z, height, huge);
		return true;
	}

	/** 26.3's: what's replaceable, or (for the stem) a plant in the way. */
	private boolean replaceable(World world, int x, int y, int z, boolean plants) {
		Block block = world.getBlock(x, y, z);
		return block.isReplaceable(world, x, y, z) || plants && (block instanceof BlockBush || block instanceof NetherVinesBlock);
	}

	private void set(World world, int x, int y, int z, Block block, int meta) {
		world.setBlock(x, y, z, block, meta, planted ? 3 : 2);
	}

	private void placeStem(World world, Random random, int x, int y, int z, int height, boolean huge) {
		int radius = huge ? 1 : 0;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				boolean corner = huge && Math.abs(dx) == radius && Math.abs(dz) == radius;
				for (int dy = 0; dy < height; dy++) {
					int px = x + dx, py = y + dy, pz = z + dz;
					if (!replaceable(world, px, py, pz, true)) {
						continue;
					}
					if (planted) {
						if (!world.isAirBlock(px, py - 1, pz)) {
							world.func_147480_a(px, py, pz, true);
						}
						set(world, px, py, pz, stem, stemMeta);
					} else if (!corner || random.nextFloat() < 0.1F) {
						set(world, px, py, pz, stem, stemMeta);
					}
				}
			}
		}
	}

	private void placeHat(World world, Random random, int x, int y, int z, int height, boolean huge) {
		int hatHeight = Math.min(random.nextInt(1 + height / 3) + 5, height);
		int start = height - hatHeight;
		for (int dy = start; dy <= height; dy++) {
			int radius = dy < height - random.nextInt(3) ? 2 : 1;
			if (hatHeight > 8 && dy < start + 4) {
				radius = 3;
			}
			if (huge) {
				radius++;
			}
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					boolean edgeX = dx == -radius || dx == radius;
					boolean edgeZ = dz == -radius || dz == radius;
					boolean inside = !edgeX && !edgeZ && dy != height;
					boolean corner = edgeX && edgeZ;
					boolean bottom = dy < start + 3;
					int px = x + dx, py = y + dy, pz = z + dz;
					if (!replaceable(world, px, py, pz, false)) {
						continue;
					}
					if (planted && !world.isAirBlock(px, py - 1, pz)) {
						world.func_147480_a(px, py, pz, true);
					}
					if (bottom) {
						if (!inside) {
							hatDrop(world, random, px, py, pz);
						}
					} else if (inside) {
						hatBlock(world, random, px, py, pz, 0.1F, 0.2F, vines ? 0.1F : 0.0F);
					} else if (corner) {
						hatBlock(world, random, px, py, pz, 0.01F, 0.7F, vines ? 0.083F : 0.0F);
					} else {
						hatBlock(world, random, px, py, pz, 5.0E-4F, 0.98F, vines ? 0.07F : 0.0F);
					}
				}
			}
		}
	}

	private void hatBlock(World world, Random random, int x, int y, int z, float decorChance, float hatChance, float vineChance) {
		if (random.nextFloat() < decorChance) {
			set(world, x, y, z, decor, 0);
		} else if (random.nextFloat() < hatChance) {
			set(world, x, y, z, hat, hatMeta);
			if (random.nextFloat() < vineChance) {
				weepingVines(world, random, x, y, z);
			}
		}
	}

	private void hatDrop(World world, Random random, int x, int y, int z) {
		if (world.getBlock(x, y - 1, z) == hat && world.getBlockMetadata(x, y - 1, z) == hatMeta) {
			set(world, x, y, z, hat, hatMeta);
		} else if (random.nextFloat() < 0.15F) {
			set(world, x, y, z, hat, hatMeta);
			if (vines && random.nextInt(11) == 0) {
				weepingVines(world, random, x, y, z);
			}
		}
	}

	/** 26.3's: 1 to 5 (doubled 1 time in 7) weeping vines below a hat block, the head of age 23 to 25 (ours: stopped). */
	private void weepingVines(World world, Random random, int x, int y, int z) {
		if (!world.isAirBlock(x, y - 1, z) || !io.github.asgerjon.mcfly.registry.McFlyBlocks.WEEPING_VINES.isEnabled()) {
			return;
		}
		int length = between(random, 1, 5);
		if (random.nextInt(7) == 0) {
			length *= 2;
		}
		Block vine = io.github.asgerjon.mcfly.registry.McFlyBlocks.WEEPING_VINES.get();
		for (int i = 0, py = y - 1; i <= length; i++, py--) {
			if (!world.isAirBlock(x, py, z)) {
				continue;
			}
			boolean head = i == length || !world.isAirBlock(x, py - 1, z);
			world.setBlock(x, py, z, vine, head ? NetherVinesBlock.MAX_AGE : 0, 2);
			if (head) {
				break;
			}
		}
	}
}
