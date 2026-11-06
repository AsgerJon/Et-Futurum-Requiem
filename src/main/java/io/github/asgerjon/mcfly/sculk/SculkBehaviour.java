package io.github.asgerjon.mcfly.sculk;

import io.github.asgerjon.mcfly.base.McFlyMultiface;
import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 26.3's `SculkBehaviour` (SPEC-modern-movement.md §90.9, step H3): what a block does with a charge cursor on it. Sculk
 * and the vein have their own; any other block the default (`DEFAULT`): veins spread into its space, and its charge
 * lasts while the cursor's decay delay does. Faces are a bit set in `ForgeDirection` order, -1 for 26.3's null.
 */
public interface SculkBehaviour {

	SculkBehaviour DEFAULT = new SculkBehaviour() {
		@Override
		public boolean attemptSpreadVein(World world, int x, int y, int z, Block block, int facings, boolean postProcess) {
			SculkVeinBlock vein = SculkVeinBlock.vein();
			if (vein == null) {
				return false;
			}
			if (facings < 0) {
				return vein.sameSpaceSpreader().spreadAll(world, x, y, z, block, McFlyMultiface.faces(world, x, y, z), postProcess) > 0;
			}
			if (facings != 0) {
				return block.isAir(world, x, y, z) && SculkVeinBlock.regrow(world, x, y, z, facings);
			}
			return SculkBehaviour.super.attemptSpreadVein(world, x, y, z, block, facings, postProcess);
		}

		@Override
		public int attemptUseCharge(SculkSpreader.ChargeCursor cursor, World world, int ox, int oy, int oz, Random random, SculkSpreader spreader, boolean spreadVeins) {
			return cursor.decayDelay() > 0 ? cursor.charge() : 0;
		}

		@Override
		public int updateDecayDelay(int age) {
			return Math.max(age - 1, 0);
		}
	};

	/** The block's own, or the default. */
	static SculkBehaviour of(Block block) {
		return block instanceof SculkBehaviour ? (SculkBehaviour) block : DEFAULT;
	}

	/** 26.3's `getSculkSpreadDelay`. */
	default int spreadDelay() {
		return 1;
	}

	/** 26.3's `onDischarged`. */
	default void onDischarged(World world, int x, int y, int z, Random random) {
	}

	/** 26.3's `attemptSpreadVein`: the vein's spreader from this block. */
	default boolean attemptSpreadVein(World world, int x, int y, int z, Block block, int facings, boolean postProcess) {
		SculkVeinBlock vein = SculkVeinBlock.vein();
		return vein != null && vein.spreader().spreadAll(world, x, y, z, block, McFlyMultiface.faces(world, x, y, z), postProcess) > 0;
	}

	/** 26.3's `canChangeBlockStateOnSpread`. */
	default boolean canChangeBlockStateOnSpread() {
		return true;
	}

	/** 26.3's `updateDecayDelay`. */
	default int updateDecayDelay(int age) {
		return 1;
	}

	/** 26.3's `attemptUseCharge`: the cursor's charge after this block's use of it. */
	int attemptUseCharge(SculkSpreader.ChargeCursor cursor, World world, int ox, int oy, int oz, Random random, SculkSpreader spreader, boolean spreadVeins);

}
