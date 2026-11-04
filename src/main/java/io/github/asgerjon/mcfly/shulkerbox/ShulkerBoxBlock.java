package io.github.asgerjon.mcfly.shulkerbox;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.lib.GUIIDs;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * A shulker box (SPEC-modern-movement.md §81; 26.3's `ShulkerBoxBlock`): one block for the plain box and the 16
 * colours (its tile entity holds which, and the way it opens). It opens when it's open already or when its lid's space
 * is free; its outline grows with the lid; a comparator reads it; a piston breaks it (it drops itself with its
 * contents); broken in survival it drops itself with its contents and name, in creative only if it holds something.
 */
public class ShulkerBoxBlock extends BlockContainer {

	/** 1.7.10's directions' steps (0 down, 1 up, 2 north, 3 south, 4 west, 5 east). */
	static final int[][] STEP = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

	private final IIcon[] icons = new IIcon[17];

	public ShulkerBoxBlock() {
		super(Material.rock);
		setStepSound(soundTypeStone);
		// 26.3's strength 2: hardness and blast resistance 2
		setHardness(2.0F);
		setResistance(2.0F / 3.0F * 5.0F);
		setBlockName(McFly.name("shulker_box"));
		setBlockTextureName("shulker_box");
		setCreativeTab(McFlyTab.TAB);
	}

	public static boolean isBox(ItemStack stack) {
		return stack != null && Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock;
	}

	/**
	 * 26.3's `Shulker.getProgressDeltaAabb` for a box (size 1, its feet at the given bottom middle): its cube stretched
	 * towards the face by the larger progress and cut from the other side by one plus the smaller (from -1: the whole
	 * cube stretched).
	 */
	static AxisAlignedBB lidBox(int face, float from, float to, double x, double y, double z) {
		double max = Math.max(from, to);
		double min = Math.min(from, to);
		double[] low = {-0.5, 0.0, -0.5};
		double[] high = {0.5, 1.0, 0.5};
		for (int axis = 0; axis < 3; axis++) {
			double expand = STEP[face][axis] * max;
			if (expand < 0.0) {
				low[axis] += expand;
			} else if (expand > 0.0) {
				high[axis] += expand;
			}
			double contract = -STEP[face][axis] * (1.0 + min);
			if (contract < 0.0) {
				low[axis] -= contract;
			} else if (contract > 0.0) {
				high[axis] -= contract;
			}
		}
		return AxisAlignedBB.getBoundingBox(low[0] + x, low[1] + y, low[2] + z, high[0] + x, high[1] + y, high[2] + z);
	}

	private static ShulkerBoxBlockEntity box(IBlockAccess world, int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile instanceof ShulkerBoxBlockEntity ? (ShulkerBoxBlockEntity) tile : null;
	}

	/** 26.3's canOpen: open already, or its lid's space (the half block out of its open side) free of every collision. */
	static boolean canOpen(World world, int x, int y, int z, ShulkerBoxBlockEntity box) {
		if (!box.isClosed()) {
			return true;
		}
		AxisAlignedBB lid = lidBox(box.facing(), 0.0F, 0.5F, x + 0.5, y, z + 0.5).contract(1.0E-6, 1.0E-6, 1.0E-6);
		List<AxisAlignedBB> hits = new ArrayList<>();
		for (int bx = (int) Math.floor(lid.minX); bx <= (int) Math.floor(lid.maxX); bx++) {
			for (int by = (int) Math.floor(lid.minY) - 1; by <= (int) Math.floor(lid.maxY); by++) {
				for (int bz = (int) Math.floor(lid.minZ); bz <= (int) Math.floor(lid.maxZ); bz++) {
					if (bx != x || by != y || bz != z) {
						world.getBlock(bx, by, bz).addCollisionBoxesToList(world, bx, by, bz, lid, hits, null);
					}
				}
			}
		}
		return hits.isEmpty();
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return true;
		}
		ShulkerBoxBlockEntity box = box(world, x, y, z);
		if (box != null && canOpen(world, x, y, z, box)) {
			player.openGui(EtFuturum.instance, GUIIDs.SHULKER_BOX, world, x, y, z);
		}
		return true;
	}

	/** Its colour, contents and name from the item; the way it opens: the side it was put on. */
	public static void placed(World world, int x, int y, int z, int side, ItemStack stack) {
		ShulkerBoxBlockEntity box = box(world, x, y, z);
		if (box != null) {
			box.setItemValues(stack);
			box.setFacing(side);
			world.markBlockForUpdate(x, y, z);
		}
	}

	/** 26.3's playerWillDestroy: in creative, a box holding something still drops itself (with what it holds). */
	@Override
	public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
		ShulkerBoxBlockEntity box = box(world, x, y, z);
		if (!world.isRemote && box != null) {
			if (player.capabilities.isCreativeMode) {
				if (!box.isEmpty()) {
					drop(world, x, y, z, box.createStack());
				}
			} else {
				drop(world, x, y, z, box.createStack());
			}
			// its contents went with it: nothing left to spill
			for (int i = 0; i < box.getSizeInventory(); i++) {
				box.setInventorySlotContents(i, null);
			}
		}
		super.onBlockHarvested(world, x, y, z, meta, player);
	}

	private static void drop(World world, int x, int y, int z, ItemStack stack) {
		EntityItem item = new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, stack);
		item.delayBeforeCanPickup = 10;
		world.spawnEntityInWorld(item);
	}

	/**
	 * Broken any other way (a piston, an explosion): itself with its contents, read while its tile entity is still
	 * there. A player's break dropped it already in `onBlockHarvested`.
	 */
	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
		ArrayList<ItemStack> drops = new ArrayList<>();
		ShulkerBoxBlockEntity box = box(world, x, y, z);
		if (box != null) {
			drops.add(box.createStack());
		}
		return drops;
	}

	/** Broken by a player its drop happened in `onBlockHarvested`; 1.7.10 calls this after with the tile entity gone. */
	@Override
	public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta) {
	}

	/** 26.3's push reaction DESTROY: a piston breaks it and it drops (with its contents, by `getDrops`). */
	@Override
	public int getMobilityFlag() {
		return 1;
	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
		ShulkerBoxBlockEntity box = box(world, x, y, z);
		if (box == null) {
			return super.getPickBlock(target, world, x, y, z, player);
		}
		ItemStack stack = new ItemStack(this);
		if (box.colour() > 0) {
			net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
			tag.setByte("Color", (byte) box.colour());
			stack.setTagCompound(tag);
		}
		return stack;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		ShulkerBoxBlockEntity box = box(world, x, y, z);
		if (box == null || box.isClosed()) {
			setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
			return;
		}
		AxisAlignedBB outline = box.outline();
		setBlockBounds((float) (outline.minX - x), (float) (outline.minY - y), (float) (outline.minZ - z),
			(float) (outline.maxX - x), (float) (outline.maxY - y), (float) (outline.maxZ - z));
	}

	@Override
	public void setBlockBoundsForItemRender() {
		setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		ShulkerBoxBlockEntity box = box(world, x, y, z);
		return box != null ? box.outline() : super.getCollisionBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		return getCollisionBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	public boolean hasComparatorInputOverride() {
		return true;
	}

	@Override
	public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile instanceof IInventory ? Container.calcRedstoneFromInventory((IInventory) tile) : 0;
	}

	/** Its icon for breaking particles: the box's colour's (26.3's `shulker_box`, `white_shulker_box`, ...). */
	@Override
	public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
		ShulkerBoxBlockEntity box = box(world, x, y, z);
		return icons[box == null ? 0 : Math.min(16, box.colour())];
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		return icons[0];
	}

	public IIcon iconFor(int colour) {
		return icons[Math.min(16, Math.max(0, colour))];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		icons[0] = blockIcon = register.registerIcon("shulker_box");
		for (io.github.asgerjon.mcfly.dye.DyeColor colour : io.github.asgerjon.mcfly.dye.DyeColor.values()) {
			icons[colour.id() + 1] = register.registerIcon(colour.getName() + "_shulker_box");
		}
	}

	/** Breaking particles in the box's colour (its tile entity read while it's still there). */
	@Override
	@SideOnly(Side.CLIENT)
	public boolean addDestroyEffects(World world, int x, int y, int z, int meta, net.minecraft.client.particle.EffectRenderer effects) {
		ShulkerBoxBlockEntity box = box(world, x, y, z);
		if (box == null) {
			return false;
		}
		IIcon icon = iconFor(box.colour());
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				for (int k = 0; k < 4; k++) {
					double px = x + (i + 0.5) / 4.0;
					double py = y + (j + 0.5) / 4.0;
					double pz = z + (k + 0.5) / 4.0;
					net.minecraft.client.particle.EntityDiggingFX dig = new net.minecraft.client.particle.EntityDiggingFX(world, px, py, pz,
						px - x - 0.5, py - y - 0.5, pz - z - 0.5, this, meta);
					dig.setParticleIcon(icon);
					effects.addEffect(dig.applyColourMultiplier(x, y, z));
				}
			}
		}
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new ShulkerBoxBlockEntity();
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
	public Item getItemDropped(int meta, java.util.Random random, int fortune) {
		return Item.getItemFromBlock(this);
	}
}
