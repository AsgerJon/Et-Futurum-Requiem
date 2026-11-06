package io.github.asgerjon.mcfly.copper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.world.IBlockAccess;

/**
 * The copper grate (SPEC-modern-movement.md §87), EFR's `copper_grate`: meta stage, +4 waxed. 26.3's
 * `WaterloggedTransparentBlock`: light through it, not a full block (never suffocating, no redstone through it), its
 * faces against a grate of the same stage and wax hidden (26.3's: the same block); 1.7.10 has no waterlogging.
 */
public class CopperGrateBlock extends CopperMetaBlock {

	private static final int[] ITEMS = {0, 1, 2, 3, 4, 5, 6, 7};

	public CopperGrateBlock() {
		super("copper_grate", McFlySoundType.COPPER_GRATE);
		setLightOpacity(0);
	}

	@Override
	public int[] itemMetas() {
		return ITEMS;
	}

	@Override
	public int stageOf(int meta) {
		return meta % 4;
	}

	@Override
	public boolean waxedOf(int meta) {
		return meta >= 4;
	}

	@Override
	protected int metaFor(int stage, boolean waxed, int oldMeta) {
		return stage + (waxed ? 4 : 0);
	}

	@Override
	public String nameOf(int meta) {
		return Weathering.name("copper_grate", stageOf(meta), waxedOf(meta));
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	/** x, y, z the neighbour's: a face against the same grate isn't drawn. */
	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
		int[] from = {x, y, z};
		int[][] offsets = {{0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}};
		int ox = from[0] + offsets[side][0];
		int oy = from[1] + offsets[side][1];
		int oz = from[2] + offsets[side][2];
		if (world.getBlock(x, y, z) == this && world.getBlockMetadata(x, y, z) == world.getBlockMetadata(ox, oy, oz)) {
			return false;
		}
		return super.shouldSideBeRendered(world, x, y, z, side);
	}
}
