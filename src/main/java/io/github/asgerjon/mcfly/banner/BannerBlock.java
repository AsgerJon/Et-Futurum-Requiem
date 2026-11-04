package io.github.asgerjon.mcfly.banner;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayList;

/**
 * A banner, standing or on a wall (SPEC-modern-movement.md §80; 26.3's `BannerBlock`, `WallBannerBlock`): one block, its
 * meta a standing banner's rotation (16 steps) or the wall it hangs on (2 north to 5 east), its tile entity which it
 * is. No collision; its outline the pole (standing) or the flag against the wall; it breaks off when what holds it
 * goes; it drops itself with its layers.
 */
public class BannerBlock extends BlockContainer {

	public BannerBlock() {
		super(Material.wood);
		disableStats();
		setHardness(1.0F);
		setStepSound(soundTypeWood);
		setBlockName(McFly.name("banner"));
		setCreativeTab(McFlyTab.TAB);
		setBlockBounds(0.25F, 0.0F, 0.25F, 0.75F, 1.0F, 0.75F);
	}

	private static BannerBlockEntity banner(IBlockAccess world, int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile instanceof BannerBlockEntity ? (BannerBlockEntity) tile : null;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		BannerBlockEntity banner = banner(world, x, y, z);
		if (banner == null || banner.isStanding()) {
			setBlockBounds(0.25F, 0.0F, 0.25F, 0.75F, 1.0F, 0.75F);
			return;
		}
		// 26.3's wall banner outline: the flag's 2 pixels against the wall, 12.5 pixels high
		float top = 0.78125F;
		float depth = 0.125F;
		switch (world.getBlockMetadata(x, y, z)) {
			case 2 -> setBlockBounds(0.0F, 0.0F, 1.0F - depth, 1.0F, top, 1.0F);
			case 3 -> setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, top, depth);
			case 4 -> setBlockBounds(1.0F - depth, 0.0F, 0.0F, 1.0F, top, 1.0F);
			case 5 -> setBlockBounds(0.0F, 0.0F, 0.0F, depth, top, 1.0F);
			default -> setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	/** It breaks off when the block under a standing banner, or the wall behind a wall banner, isn't solid any more. */
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		BannerBlockEntity banner = banner(world, x, y, z);
		if (banner == null) {
			return;
		}
		boolean held;
		if (banner.isStanding()) {
			held = world.getBlock(x, y - 1, z).getMaterial().isSolid();
		} else {
			held = switch (world.getBlockMetadata(x, y, z)) {
				case 2 -> world.getBlock(x, y, z + 1).getMaterial().isSolid();
				case 3 -> world.getBlock(x, y, z - 1).getMaterial().isSolid();
				case 4 -> world.getBlock(x + 1, y, z).getMaterial().isSolid();
				case 5 -> world.getBlock(x - 1, y, z).getMaterial().isSolid();
				default -> true;
			};
		}
		if (!held) {
			dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
			world.setBlockToAir(x, y, z);
		}
		super.onNeighborBlockChange(world, x, y, z, neighbour);
	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
		BannerBlockEntity banner = banner(world, x, y, z);
		return banner != null ? banner.createStack() : super.getPickBlock(target, world, x, y, z, player);
	}

	/** Its drops while its tile entity is still there (1.7.10 removes it before `getDrops` otherwise). */
	@Override
	public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
		if (!player.capabilities.isCreativeMode) {
			int fortune = EnchantmentHelper.getFortuneModifier(player);
			ArrayList<ItemStack> drops = getDrops(world, x, y, z, meta, fortune);
			if (ForgeEventFactory.fireBlockHarvesting(drops, world, this, x, y, z, meta, fortune, 1.0F, EnchantmentHelper.getSilkTouchModifier(player), player) > 0.0F) {
				for (ItemStack stack : drops) {
					dropBlockAsItem(world, x, y, z, stack);
				}
			}
		}
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
		ArrayList<ItemStack> drops = new ArrayList<>();
		BannerBlockEntity banner = banner(world, x, y, z);
		if (banner != null) {
			drops.add(banner.createStack());
		}
		return drops;
	}

	/** Breaking particles: oak planks (26.3's banner's particle is oak planks). */
	@Override
	public IIcon getIcon(int side, int meta) {
		return Blocks.planks.getBlockTextureFromSide(side);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		// drawn by its tile entity's renderer
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return null;
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return super.getSelectedBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new BannerBlockEntity();
	}

	@Override
	public int getRenderType() {
		return -1;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
		return true;
	}
}
