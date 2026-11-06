package io.github.asgerjon.mcfly.nether;

import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.copper.CopperOreBlock;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.world.IBlockAccess;

import java.util.Random;

/**
 * Nether gold ore (SPEC-modern-movement.md §88.3), 26.3's `DropExperienceBlock(UniformInt.of(0, 1))`: 3 and 3, its
 * sound, 2 to 6 gold nuggets (Fortune's ore bonus on top), 0 or 1 experience (EFR's 2 to 5); Silk Touch itself.
 */
public class NetherGoldOreBlock extends McFlyBlock {

	private final Random random = new Random();

	public NetherGoldOreBlock() {
		super(Material.rock);
		setNames("nether_gold_ore");
		setHardness(3.0F);
		this.blockResistance = 3.0F * 5.0F;
		setStepSound(McFlySoundType.NETHER_GOLD_ORE);
		setHarvestLevel("pickaxe", 0);
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return Items.gold_nugget;
	}

	@Override
	public int quantityDropped(int meta, int fortune, Random random) {
		return (2 + random.nextInt(5)) * CopperOreBlock.oreBonus(fortune, random);
	}

	@Override
	public int getExpDrop(IBlockAccess world, int meta, int fortune) {
		return random.nextInt(2);
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.netherrackColor;
	}
}
