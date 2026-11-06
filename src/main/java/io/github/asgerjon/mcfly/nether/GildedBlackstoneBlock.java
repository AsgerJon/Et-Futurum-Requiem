package io.github.asgerjon.mcfly.nether;

import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;

/**
 * Gilded blackstone (SPEC-modern-movement.md §88.3): blackstone's strength, its own sound, and 26.3's loot: 2 to 5 gold
 * nuggets at 0.1, 0.14285715, 0.25 and 1 by Fortune's level (`table_bonus`), else itself; Silk Touch itself.
 */
public class GildedBlackstoneBlock extends McFlyBlock {

	private static final float[] NUGGETS = {0.1F, 0.14285715F, 0.25F, 1.0F};

	public GildedBlackstoneBlock() {
		super(Material.rock);
		setNames("gilded_blackstone");
		setHardness(1.5F);
		this.blockResistance = 6.0F * 5.0F;
		setStepSound(McFlySoundType.GILDED_BLACKSTONE);
		setHarvestLevel("pickaxe", 0);
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
		ArrayList<ItemStack> drops = new ArrayList<>();
		if (world.rand.nextFloat() < NUGGETS[Math.max(0, Math.min(3, fortune))]) {
			drops.add(new ItemStack(Items.gold_nugget, 2 + world.rand.nextInt(4)));
		} else {
			drops.add(new ItemStack(this));
		}
		return drops;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.blackColor;
	}
}
