package io.github.asgerjon.mcfly.copper;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.RandomTicking;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.MapColor;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Cut copper stairs (SPEC-modern-movement.md §87), EFR's 8 blocks (`cut_copper_stairs` ... `waxed_oxidized_cut_copper_stairs`),
 * their metas 1.7.10's stairs'; drawn and as strong as the cut copper of their stage; 26.3's ageing.
 */
public class CopperStairsBlock extends BlockStairs implements WeatheringCopper, RandomTicking {

	private final int stage;
	private final boolean waxed;

	public CopperStairsBlock(Block copper, int stage, boolean waxed) {
		super(copper, CopperFullBlock.meta(stage, true, waxed));
		this.stage = stage;
		this.waxed = waxed;
		setBlockName(McFly.name(Weathering.name("cut_copper_stairs", stage, waxed)));
		setCreativeTab(McFlyTab.TAB);
		setHarvestLevel("pickaxe", 1);
		this.blockResistance = 6.0F * 5.0F;
		// lit by its neighbours, as 1.7.10's own stairs are (Block.registerBlocks sets it for vanilla's alone)
		this.useNeighborBrightness = true;
		setTickRandomly(!waxed);
	}

	/** The stairs of a stage and wax. */
	public static Block of(int stage, boolean waxed) {
		return McFlyBlocks.valueOf(Weathering.name("cut_copper_stairs", stage, waxed).toUpperCase()).get();
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

	@Override
	public MapColor getMapColor(int meta) {
		return Weathering.colour(stage);
	}
}
