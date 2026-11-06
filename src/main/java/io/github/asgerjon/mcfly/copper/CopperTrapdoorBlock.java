package io.github.asgerjon.mcfly.copper;

import io.github.asgerjon.mcfly.base.RandomTicking;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Copper trapdoors (SPEC-modern-movement.md §87), EFR's 8 blocks (`copper_trapdoor` ... `waxed_oxidized_copper_trapdoor`),
 * their metas 1.7.10's trapdoors'. On McFly's trapdoor (§91.6: 26.3's powered state, placing, climbing, its model), with
 * 26.3's `BlockSetType.COPPER`: opened by hand as by redstone, with `block.copper_trapdoor.open` and `close`; 3 and 6, a
 * pickaxe for drops; 26.3's ageing.
 */
public class CopperTrapdoorBlock extends io.github.asgerjon.mcfly.base.McFlyTrapdoor implements WeatheringCopper, RandomTicking {

	/** Not 1.7.10's iron (which no hand opens), but needing a tool as it. */
	static final Material COPPER = new Material(MapColor.adobeColor) {
		{
			setRequiresTool();
		}
	};

	private final int stage;
	private final boolean waxed;

	public CopperTrapdoorBlock(int stage, boolean waxed) {
		super(Weathering.name("copper_trapdoor", stage, waxed), COPPER, io.github.asgerjon.mcfly.base.McFlySetType.COPPER, Weathering.colour(stage),
			Weathering.name("copper_trapdoor", stage, false), false);
		this.stage = stage;
		this.waxed = waxed;
		strength(3.0F, 6.0F);
		setHarvestLevel("pickaxe", 1);
		setTickRandomly(!waxed);
	}

	public static Block of(int stage, boolean waxed) {
		return McFlyBlocks.valueOf(Weathering.name("copper_trapdoor", stage, waxed).toUpperCase()).get();
	}

	@Override
	public int stage(IBlockAccess world, int x, int y, int z) {
		return stage;
	}

	@Override
	public boolean waxed(IBlockAccess world, int x, int y, int z) {
		return waxed;
	}

	@Override
	public void become(World world, int x, int y, int z, int stage, boolean waxed) {
		world.setBlock(x, y, z, of(stage, waxed), world.getBlockMetadata(x, y, z), 3);
	}

	@Override
	public void randomTick(World world, int x, int y, int z, Random random) {
		Weathering.randomTick(this, world, x, y, z, random);
	}

}
