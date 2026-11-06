package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collections;
import java.util.List;

/**
 * The lantern and the soul lantern (SPEC-modern-movement.md §88.5), 26.3's `LanternBlock` in EFR's stored form: meta 0
 * standing, 1 hanging. Placed by where the player looks (26.3's `getStateForPlacement`: the first vertical of its look
 * directions, hanging if up, else the other, whichever it survives), held by a block that supports the middle of the face
 * it touches (26.3's `canSupportCenter`), gone when that goes; 3.5 and 3.5, by hand (no tool rule), broken by pistons,
 * light 15 or 10, its sound; 26.3's shapes and models; its item a flat sprite.
 */
public class LanternBlock extends McFlyBlock implements ModelledBlock, NetherLightItem.Flat {

	private final String name;

	public LanternBlock(String name, int light) {
		super(Material.iron);
		this.name = name;
		setNames(name);
		strength(3.5F, 3.5F).tool("pickaxe", 0).sound(McFlySoundType.LANTERN).colour(MapColor.ironColor);
		setLightLevel(light / 15.0F);
	}

	public static boolean hanging(int meta) {
		return (meta & 1) == 1;
	}

	/**
	 * 26.3's `Block.canSupportCenter` in 1.7.10's terms: a solid face, or a fence's, wall's or pane's post, an upright
	 * chain, end rod or lightning rod (each covers the middle 2 by 2 of the face).
	 */
	public static boolean supportsCenter(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		Block block = world.getBlock(x, y, z);
		if (block.isSideSolid(world, x, y, z, face)) {
			return true;
		}
		if (face != ForgeDirection.UP && face != ForgeDirection.DOWN) {
			return false;
		}
		int meta = world.getBlockMetadata(x, y, z);
		return block instanceof BlockFence || block instanceof BlockWall || block instanceof BlockPane
			|| block instanceof ChainBlock && ChainBlock.axis(meta) == 0
			|| block instanceof io.github.asgerjon.mcfly.end.EndRodBlock && (meta & 7) < 2
			|| block instanceof io.github.asgerjon.mcfly.copper.LightningRodBlock && (meta & 7) < 2;
	}

	private static boolean survives(IBlockAccess world, int x, int y, int z, boolean hanging) {
		return hanging ? supportsCenter(world, x, y + 1, z, ForgeDirection.DOWN) : supportsCenter(world, x, y - 1, z, ForgeDirection.UP);
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return super.canPlaceBlockAt(world, x, y, z) && (survives(world, x, y, z, false) || survives(world, x, y, z, true));
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		boolean up = placer != null && placer.rotationPitch < 0.0F;
		boolean hanging = survives(world, x, y, z, up) ? up : !up;
		world.setBlockMetadataWithNotify(x, y, z, hanging ? 1 : 0, 2);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		if (!survives(world, x, y, z, hanging(world.getBlockMetadata(x, y, z)))) {
			dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
			world.setBlockToAir(x, y, z);
		}
	}

	@Override
	public boolean canHarvestBlock(EntityPlayer player, int meta) {
		return true;
	}

	@Override
	public int damageDropped(int meta) {
		return 0;
	}

	@Override
	public int getMobilityFlag() {
		return 1;
	}

	// ---------------------------------------------------------------- shape: (5, 0, 5) to (11, 7, 11) and (6, 7, 6) to (10, 9, 10), a pixel up hanging

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		float up = hanging(world.getBlockMetadata(x, y, z)) ? 1.0F : 0.0F;
		setBlockBounds(5.0F / 16, up / 16, 5.0F / 16, 11.0F / 16, (9.0F + up) / 16, 11.0F / 16);
	}

	@Override
	public void setBlockBoundsForItemRender() {
		setBlockBounds(5.0F / 16, 0.0F, 5.0F / 16, 11.0F / 16, 9.0F / 16, 11.0F / 16);
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
		double up = hanging(world.getBlockMetadata(x, y, z)) ? 1.0D / 16 : 0.0D;
		AxisAlignedBB body = AxisAlignedBB.getBoundingBox(x + 5.0D / 16, y + up, z + 5.0D / 16, x + 11.0D / 16, y + up + 7.0D / 16, z + 11.0D / 16);
		AxisAlignedBB top = AxisAlignedBB.getBoundingBox(x + 6.0D / 16, y + up + 7.0D / 16, z + 6.0D / 16, x + 10.0D / 16, y + up + 9.0D / 16, z + 10.0D / 16);
		for (AxisAlignedBB box : new AxisAlignedBB[]{body, top}) {
			if (mask.intersectsWith(box)) {
				list.add(box);
			}
		}
		setBlockBoundsBasedOnState(world, x, y, z);
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public int getRenderType() {
		return McFlyRenderIds.MODEL;
	}

	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		return Collections.singletonList(new ModelPart(name + (hanging(world.getBlockMetadata(x, y, z)) ? "_hanging" : "")));
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(new ModelPart(name));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		return blockIcon;
	}

	@Override
	public String itemTexture() {
		return name;
	}
}
