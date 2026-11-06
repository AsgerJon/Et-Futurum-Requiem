package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.FireResistant;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.McFlyStairs;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * Netherite's blocks (SPEC-modern-movement.md §88.4), 26.3's: the netherite block (50 and 1200, a diamond pickaxe, a
 * beacon base) and ancient debris (30 and 1200, a diamond pickaxe, a column of its side and top), both with their sounds
 * and fire resistant; EFR's own netherite stairs (26.3 has none), kept for old saves, in no tab.
 */
public final class NetheriteBlocks {

	private NetheriteBlocks() {
	}

	public static class NetheriteBlock extends McFlyBlock implements FireResistant {
		public NetheriteBlock() {
			super(Material.iron);
			setNames("netherite_block");
			strength(50.0F, 1200.0F).tool("pickaxe", 3).sound(McFlySoundType.NETHERITE_BLOCK).colour(MapColor.blackColor);
		}

		@Override
		public boolean isBeaconBase(IBlockAccess world, int x, int y, int z, int beaconX, int beaconY, int beaconZ) {
			return true;
		}
	}

	public static class AncientDebris extends McFlyBlock implements FireResistant {
		@SideOnly(Side.CLIENT)
		private IIcon top;

		public AncientDebris() {
			super(Material.rock);
			setNames("ancient_debris");
			strength(30.0F, 1200.0F).tool("pickaxe", 3).sound(McFlySoundType.ANCIENT_DEBRIS).colour(MapColor.blackColor);
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			blockIcon = register.registerIcon("ancient_debris_side");
			top = register.registerIcon("ancient_debris_top");
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIcon(int side, int meta) {
			return side < 2 ? top : blockIcon;
		}
	}

	public static class Stairs extends McFlyStairs implements FireResistant {
		public Stairs(Block block) {
			super(block, 0, "netherite_stairs");
			strength(50.0F, 1200.0F);
			// kept for old saves only (26.3 has none): in no tab
			setCreativeTab(null);
		}
	}
}
