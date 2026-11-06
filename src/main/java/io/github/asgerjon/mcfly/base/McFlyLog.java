package io.github.asgerjon.mcfly.base;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;

/**
 * A wood's logs in one id (SPEC-modern-movement.md §90.6, §91.3), 26.3's log blocks (`RotatedPillarBlock` with
 * `logProperties`) in EFR's form, on {@link McFlyKindLog}: meta & 3 the kind (0 log, 1 wood, 2 stripped log, 3 stripped
 * wood), meta & 12 the axis. Textures `<wood>_log` and `_top`, `stripped_<wood>_log` and `_top`.
 */
public class McFlyLog extends McFlyKindLog {

	/** A log of one colour upright and another lying; the wood and the stripped kinds the upright one (mangrove's). */
	public McFlyLog(String wood, MapColor top, MapColor side) {
		this(wood, Block.soundTypeWood, top, side, top, top, top, top);
	}

	/** Each kind's colours: the log upright and lying, the wood, the stripped log upright and lying, the stripped wood. */
	public McFlyLog(String wood, Block.SoundType sound, MapColor logUpright, MapColor logLying, MapColor woodColour, MapColor strippedUpright,
					MapColor strippedLying, MapColor strippedWood) {
		super(wood + "_log", sound,
			new Kind(wood + "_log", logUpright, logLying, wood + "_log", wood + "_log_top"),
			Kind.wood(wood + "_wood", woodColour, wood + "_log"),
			new Kind("stripped_" + wood + "_log", strippedUpright, strippedLying, "stripped_" + wood + "_log", "stripped_" + wood + "_log_top"),
			Kind.wood("stripped_" + wood + "_wood", strippedWood, "stripped_" + wood + "_log"));
	}
}
