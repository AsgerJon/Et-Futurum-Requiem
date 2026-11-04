package ganymedes01.etfuturum.command.modern.arguments;

import com.github.bsideup.jabel.Desugar;
import ganymedes01.etfuturum.blocks.BlockAmethystCluster;
import ganymedes01.etfuturum.blocks.BlockBarrel;
import ganymedes01.etfuturum.blocks.BlockBeeHive;
import ganymedes01.etfuturum.blocks.BlockChain;
import ganymedes01.etfuturum.blocks.BlockEndRod;
import ganymedes01.etfuturum.blocks.BlockGlazedTerracotta;
import ganymedes01.etfuturum.blocks.BlockLantern;
import ganymedes01.etfuturum.blocks.BlockLightningRod;
import ganymedes01.etfuturum.blocks.BlockLoom;
import ganymedes01.etfuturum.blocks.BlockObserver;
import ganymedes01.etfuturum.blocks.BlockStonecutter;
import net.minecraft.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EFR's blocks of no vanilla shape whose meta holds modern properties (SPEC-modern-movement.md §64.3, handoff
 * upcoming 32): each meta (added to the block's own variant meta) with the properties it stands for, read from the
 * block's own placing code, and 26.3's defaults (its blocks' {@code registerDefaultState}). Blocks not here keep one
 * state, as before.
 */
final class EfrBlockStates {

	/** ForgeDirection's order. */
	private static final String[] SIX = {"down", "up", "north", "south", "west", "east"};

	@Desugar
	record Meta(int offset, Map<String, String> properties) {
	}

	@Desugar
	record Spec(List<Meta> metas, Map<String, String> defaults) {
	}

	private EfrBlockStates() {
	}

	static Spec of(Block block) {
		if (block instanceof BlockLightningRod) {
			return sixWays("powered", "up");
		}
		if (block instanceof BlockObserver) {
			return sixWays("powered", "south");
		}
		if (block instanceof BlockBarrel) {
			return sixWays("open", "north");
		}
		if (block instanceof BlockEndRod || block instanceof BlockAmethystCluster) {
			return sixWays(null, "up");
		}
		if (block instanceof BlockStonecutter || block instanceof BlockLoom) {
			// their front, towards the player
			return byMeta("facing", new String[]{"north", "south", "west", "east"}, "north");
		}
		if (block instanceof BlockGlazedTerracotta) {
			// 1.7.10's direction of the player's view, the pattern facing the player as 26.3's
			return byMeta("facing", new String[]{"south", "west", "north", "east"}, "north");
		}
		if (block instanceof BlockChain) {
			return byMeta("axis", new String[]{"y", "x", "z"}, "y");
		}
		if (block instanceof BlockLantern) {
			return byMeta("hanging", new String[]{"false", "true"}, "false");
		}
		if (block instanceof BlockBeeHive) {
			// 2..5 a ForgeDirection, 6 more when full; the levels between live in its block entity
			List<Meta> metas = new ArrayList<>();
			for (int full = 0; full <= 1; full++) {
				for (int side = 2; side < 6; side++) {
					metas.add(new Meta(side + 6 * full, map("facing", SIX[side], "honey_level", full == 1 ? "5" : "0")));
				}
			}
			return new Spec(metas, map("facing", "north", "honey_level", "0"));
		}
		return null;
	}

	/** A ForgeDirection in the low three bits, and a flag (if any) at 8. */
	private static Spec sixWays(String flag, String facing) {
		List<Meta> metas = new ArrayList<>();
		for (int on = 0; on <= (flag == null ? 0 : 1); on++) {
			for (int side = 0; side < 6; side++) {
				metas.add(new Meta(side + 8 * on, flag == null ? map("facing", SIX[side]) : map("facing", SIX[side], flag, on == 1 ? "true" : "false")));
			}
		}
		return new Spec(metas, flag == null ? map("facing", facing) : map("facing", facing, flag, "false"));
	}

	/** One property, its values at metas 0, 1, 2, ... */
	private static Spec byMeta(String property, String[] values, String defaultValue) {
		List<Meta> metas = new ArrayList<>();
		for (int meta = 0; meta < values.length; meta++) {
			metas.add(new Meta(meta, map(property, values[meta])));
		}
		return new Spec(metas, map(property, defaultValue));
	}

	private static Map<String, String> map(String... pairs) {
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			map.put(pairs[i], pairs[i + 1]);
		}
		return Collections.unmodifiableMap(map);
	}
}
