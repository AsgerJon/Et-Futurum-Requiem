package io.github.asgerjon.mcfly.copper;

import io.github.asgerjon.mcfly.sound.McFlySoundType;

/**
 * The copper block and cut copper (SPEC-modern-movement.md §87), EFR's `copper_block`: meta stage (0 to 3), +4 cut,
 * +8 waxed; 26.3's `copper_block`, `exposed_copper`, ..., `waxed_oxidized_cut_copper`.
 */
public class CopperFullBlock extends CopperMetaBlock {

	private static final int[] ITEMS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

	public CopperFullBlock() {
		super("copper_block", McFlySoundType.COPPER);
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
		return meta >= 8;
	}

	public static boolean cut(int meta) {
		return meta % 8 >= 4;
	}

	@Override
	protected int metaFor(int stage, boolean waxed, int oldMeta) {
		return stage + (cut(oldMeta) ? 4 : 0) + (waxed ? 8 : 0);
	}

	@Override
	public String nameOf(int meta) {
		int stage = stageOf(meta);
		String kind = cut(meta) ? "cut_copper" : stage == 0 ? "copper_block" : "copper";
		return Weathering.name(kind, stage, waxedOf(meta));
	}

	/** A meta of this block: the stage, cut or not, waxed or not. */
	public static int meta(int stage, boolean cut, boolean waxed) {
		return stage + (cut ? 4 : 0) + (waxed ? 8 : 0);
	}
}
