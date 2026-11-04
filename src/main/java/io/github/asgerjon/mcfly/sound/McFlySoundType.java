package io.github.asgerjon.mcfly.sound;

import io.github.asgerjon.mcfly.McFly;
import net.minecraft.block.Block;

/**
 * 26.3's sound types for our blocks (SPEC-modern-movement.md §79.4, B9; 26.3's `SoundType`): `block.<name>.break`,
 * `.step` and `.place` from MCLib's assets, volume and pitch 1. Their events are in {@link McFlySounds}.
 */
public class McFlySoundType extends Block.SoundType {

	/** The lightning rod's (26.3's `COPPER`). */
	public static final McFlySoundType COPPER = new McFlySoundType("copper");
	public static final McFlySoundType LODESTONE = new McFlySoundType("lodestone");
	public static final McFlySoundType POWDER_SNOW = new McFlySoundType("powder_snow");
	/** Kelp's (26.3's `WET_GRASS`). */
	public static final McFlySoundType WET_GRASS = new McFlySoundType("wet_grass");

	public static final McFlySoundType[] VALUES = {COPPER, LODESTONE, POWDER_SNOW, WET_GRASS};

	public McFlySoundType(String name) {
		super(name, 1.0F, 1.0F);
	}

	@Override
	public String getBreakSound() {
		return McFly.MC_ASSETS + ":block." + soundName + ".break";
	}

	@Override
	public String getStepResourcePath() {
		return McFly.MC_ASSETS + ":block." + soundName + ".step";
	}

	/** The place sound (MCP's `getPlaceSound`). */
	@Override
	public String func_150496_b() {
		return McFly.MC_ASSETS + ":block." + soundName + ".place";
	}
}
