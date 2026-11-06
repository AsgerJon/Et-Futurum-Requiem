package ganymedes01.etfuturum.command.modern.arguments;

import com.github.bsideup.jabel.Desugar;
import ganymedes01.etfuturum.blocks.BlockBarrel;
import ganymedes01.etfuturum.blocks.BlockBeeHive;
import ganymedes01.etfuturum.blocks.BlockGlazedTerracotta;
import ganymedes01.etfuturum.blocks.BlockLoom;
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
	record Meta(int offset, Map<String, String> properties, Block block) {

		/** A state of the item's own block. */
		Meta(int offset, Map<String, String> properties) {
			this(offset, properties, null);
		}
	}

	@Desugar
	record Spec(List<Meta> metas, Map<String, String> defaults) {
	}

	private EfrBlockStates() {
	}

	/** Properties from name, value pairs, in order. */
	private static Map<String, String> props(String... pairs) {
		Map<String, String> map = new java.util.LinkedHashMap<>();
		for (int i = 0; i + 1 < pairs.length; i += 2) {
			map.put(pairs[i], pairs[i + 1]);
		}
		return map;
	}

	/** The block's spec for the item of this meta: the sapling id's two kinds differ (spec §90.6). */
	static Spec of(Block block, int meta) {
		if (block instanceof io.github.asgerjon.mcfly.caves.MangroveBlocks.Sapling) {
			List<Meta> metas = new ArrayList<>();
			if ((meta & 1) == 1) {
				// the cherry sapling: 26.3's stage
				metas.add(new Meta(0, map("stage", "0")));
				metas.add(new Meta(8, map("stage", "1")));
				return new Spec(metas, map("stage", "0"));
			}
			// the propagule: standing, stage 0 or 1, always age 4 (as placed); hanging, its age on metas 2, 4, 6, 10, 12
			metas.add(new Meta(0, map("stage", "0", "age", "4", "hanging", "false")));
			metas.add(new Meta(8, map("stage", "1", "age", "4", "hanging", "false")));
			for (int age = 0; age <= 4; age++) {
				metas.add(new Meta(io.github.asgerjon.mcfly.caves.MangroveBlocks.hangingMeta(age), map("stage", "0", "age", String.valueOf(age), "hanging", "true")));
			}
			return new Spec(metas, map("stage", "0", "age", "0", "hanging", "false"));
		}
		return of(block);
	}

	static Spec of(Block block) {
		if (block instanceof io.github.asgerjon.mcfly.wood.Bamboo.Stalk) {
			// spec 91.11: bit 0 age, bits 1 and 2 leaves (none, small, large), bit 3 stage
			String[] leaves = {"none", "small", "large", "none"};
			List<Meta> metas = new ArrayList<>();
			for (int meta = 0; meta < 16; meta++) {
				metas.add(new Meta(meta, map("age", String.valueOf(meta & 1), "leaves", leaves[meta >> 1 & 3], "stage", String.valueOf(meta >> 3 & 1))));
			}
			return new Spec(metas, map("age", "0", "leaves", "none", "stage", "0"));
		}
		if (block instanceof io.github.asgerjon.mcfly.base.McFlyShelf) {
			// spec 91.10: facing & 3 (north, south, west, east), +4 powered; side_chain in the tile entity (read unconnected)
			String[] facings = {"north", "south", "west", "east"};
			List<Meta> metas = new ArrayList<>();
			for (int meta = 0; meta < 8; meta++) {
				metas.add(new Meta(meta, map("facing", facings[meta & 3], "powered", meta >= 4 ? "true" : "false", "side_chain", "unconnected")));
			}
			return new Spec(metas, map("facing", "north", "powered", "false", "side_chain", "unconnected"));
		}
		if (block instanceof io.github.asgerjon.mcfly.base.McFlyHangingSign hanging) {
			// spec 91.9: the ceiling sign's meta its rotation (`attached` in its tile entity, so read as false here);
			// the wall sign's 2 north, 3 south, 4 west, 5 east
			List<Meta> metas = new ArrayList<>();
			if (hanging.ceiling) {
				for (int meta = 0; meta < 16; meta++) {
					metas.add(new Meta(meta, map("rotation", String.valueOf(meta), "attached", "false")));
				}
				return new Spec(metas, map("rotation", "8", "attached", "false"));
			}
			String[] facings = {"north", "south", "west", "east"};
			for (int i = 0; i < 4; i++) {
				metas.add(new Meta(i + 2, map("facing", facings[i])));
			}
			return new Spec(metas, map("facing", "north"));
		}
		if (block instanceof io.github.asgerjon.mcfly.copper.LightningRodBlock) {
			return sixWays("powered", "up");
		}
		if (block instanceof io.github.asgerjon.mcfly.copper.CopperBulbBlock) {
			// 26.3's lit and powered: lit is +4, powered is the other block (spec §87)
			Block poweredBulb = io.github.asgerjon.mcfly.registry.McFlyBlocks.POWERED_COPPER_BULB.get();
			List<Meta> metas = new java.util.ArrayList<>();
			metas.add(new Meta(0, props("lit", "false", "powered", "false")));
			metas.add(new Meta(4, props("lit", "true", "powered", "false")));
			metas.add(new Meta(0, props("lit", "false", "powered", "true"), poweredBulb));
			metas.add(new Meta(4, props("lit", "true", "powered", "true"), poweredBulb));
			return new Spec(metas, props("lit", "false", "powered", "false"));
		}
		if (block instanceof io.github.asgerjon.mcfly.redstone.ObserverBlock) {
			return sixWays("powered", "south");
		}
		if (block instanceof BlockBarrel) {
			return sixWays("open", "north");
		}
		if (block instanceof io.github.asgerjon.mcfly.end.EndRodBlock || block instanceof io.github.asgerjon.mcfly.caves.AmethystBlocks.Cluster) {
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
		if (block instanceof io.github.asgerjon.mcfly.nether.ChainBlock) {
			return byMeta("axis", new String[]{"y", "x", "z"}, "y");
		}
		if (block instanceof io.github.asgerjon.mcfly.caves.LushPlants.BigDripleaf) {
			// McFly's metas (spec §90.8): meta & 3 the facing (1.7.10's south, west, north, east), meta >> 2 the tilt
			List<Meta> metas = new ArrayList<>();
			for (int meta = 0; meta < 16; meta++) {
				metas.add(new Meta(meta, map("facing", io.github.asgerjon.mcfly.caves.LushPlants.FACINGS[meta & 3], "tilt",
					io.github.asgerjon.mcfly.caves.LushPlants.TILTS[meta >> 2])));
			}
			return new Spec(metas, map("facing", "north", "tilt", "none"));
		}
		if (block instanceof io.github.asgerjon.mcfly.sculk.SculkShriekerBlock) {
			// McFly's metas (spec §90.9, step H4): 1 shrieking, 2 can_summon
			List<Meta> metas = new ArrayList<>();
			for (int meta = 0; meta < 4; meta++) {
				metas.add(new Meta(meta, map("shrieking", (meta & 1) != 0 ? "true" : "false", "can_summon", (meta & 2) != 0 ? "true" : "false")));
			}
			return new Spec(metas, map("shrieking", "false", "can_summon", "false"));
		}
		if (block instanceof io.github.asgerjon.mcfly.sculk.SculkSensorBlock) {
			// McFly's metas (spec §90.9, step H2): the plain one's meta its phase; the calibrated one's meta & 3 its facing
			// (1.7.10's south, west, north, east), meta >> 2 its phase
			String[] phases = {"inactive", "active", "cooldown"};
			if (!((io.github.asgerjon.mcfly.sculk.SculkSensorBlock) block).calibrated()) {
				return byMeta("sculk_sensor_phase", phases, "inactive");
			}
			List<Meta> metas = new ArrayList<>();
			for (int meta = 0; meta < 12; meta++) {
				metas.add(new Meta(meta, map("facing", io.github.asgerjon.mcfly.caves.LushPlants.FACINGS[meta & 3], "sculk_sensor_phase", phases[meta >> 2])));
			}
			return new Spec(metas, map("facing", "north", "sculk_sensor_phase", "inactive"));
		}
		if (block instanceof io.github.asgerjon.mcfly.caves.LushPlants.Stem) {
			return byMeta("facing", io.github.asgerjon.mcfly.caves.LushPlants.FACINGS, "north");
		}
		if (block instanceof io.github.asgerjon.mcfly.caves.LushPlants.SmallDripleaf) {
			List<Meta> metas = new ArrayList<>();
			for (int meta = 0; meta < 8; meta++) {
				metas.add(new Meta(meta, map("facing", io.github.asgerjon.mcfly.caves.LushPlants.FACINGS[meta & 3], "half", meta >= 4 ? "upper" : "lower")));
			}
			return new Spec(metas, map("facing", "north", "half", "lower"));
		}
		if (block instanceof io.github.asgerjon.mcfly.caves.DripstoneBlocks.Pointed) {
			// EFR's metas (spec §90.7): meta % 5 the thickness (tip, frustum, middle, base, tip_merge), 5 and up pointing up
			List<Meta> metas = new ArrayList<>();
			for (int meta = 0; meta < 10; meta++) {
				metas.add(new Meta(meta, map("vertical_direction", meta >= 5 ? "up" : "down", "thickness",
					io.github.asgerjon.mcfly.caves.DripstoneBlocks.thicknessName(meta))));
			}
			return new Spec(metas, map("vertical_direction", "up", "thickness", "tip"));
		}
		if (block instanceof io.github.asgerjon.mcfly.nether.LanternBlock) {
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
