package io.github.asgerjon.mcfly.copper;

import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

import java.util.Random;

/**
 * Copper ore and deepslate copper ore (SPEC-modern-movement.md §87), EFR's `copper_ore` and `deepslate_copper_ore`,
 * meta 0. 26.3's: iron ore's strength (3 and 3), the deepslate one's 4.5 and 3 and its sound; a stone pickaxe; 2 to 5
 * raw copper (Fortune's ore bonus on top: `ApplyBonusCount.oreDrops`), no experience; Silk Touch, the ore.
 */
public class CopperOreBlock extends McFlyBlock {

	public CopperOreBlock(boolean deepslate) {
		super(Material.rock);
		setNames(deepslate ? "deepslate_copper_ore" : "copper_ore");
		if (deepslate) {
			setHardness(4.5F);
			// deepslate's own sound type moves with the deepslate family (its item)
			setStepSound(io.github.asgerjon.mcfly.sound.McFlySoundType.DEEPSLATE);
		} else {
			setHardness(3.0F);
			setStepSound(soundTypeStone);
		}
		this.blockResistance = 3.0F * 5.0F;
		setHarvestLevel("pickaxe", 1);
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return McFlyItems.RAW_ORE.get();
	}

	@Override
	public int damageDropped(int meta) {
		return RawOreItem.COPPER;
	}

	@Override
	public int quantityDropped(Random random) {
		return 2 + random.nextInt(4);
	}

	/** 26.3's ore bonus: times (a random of 0 to fortune + 1, less 1, at least 0) + 1. */
	@Override
	public int quantityDroppedWithBonus(int fortune, Random random) {
		int count = quantityDropped(random);
		return count * oreBonus(fortune, random);
	}

	public static int oreBonus(int fortune, Random random) {
		if (fortune <= 0) {
			return 1;
		}
		return Math.max(0, random.nextInt(fortune + 2) - 1) + 1;
	}
}
