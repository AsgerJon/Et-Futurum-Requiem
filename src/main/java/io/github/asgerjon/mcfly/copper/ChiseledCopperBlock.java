package io.github.asgerjon.mcfly.copper;

import io.github.asgerjon.mcfly.sound.McFlySoundType;

/**
 * Chiseled copper (SPEC-modern-movement.md §87), EFR's `chiseled_copper`: meta stage, +4 waxed; the 12 to 15 EFR's
 * waxing in the world wrote (its `getFinalCopperMeta` was missing) read as waxed, written back as 4 to 7.
 */
public class ChiseledCopperBlock extends CopperMetaBlock {

	private static final int[] ITEMS = {0, 1, 2, 3, 4, 5, 6, 7};

	public ChiseledCopperBlock() {
		super("chiseled_copper", McFlySoundType.COPPER);
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
		return Weathering.name("chiseled_copper", stageOf(meta), waxedOf(meta));
	}
}
