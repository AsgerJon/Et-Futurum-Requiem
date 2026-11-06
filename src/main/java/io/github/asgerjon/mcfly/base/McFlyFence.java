package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.wood.FenceJoin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wooden fences of ours (SPEC-modern-movement.md §91.5, step C), 26.3's `FenceBlock`: one wood a meta, 2 and 3, the
 * axe's, the wood's sound, colour and fire; joining by 26.3's rule ({@link FenceJoin}); 1.7.10's shapes (26.3's
 * match); drawn from 26.3's models (`fence_post`, `fence_side`; bamboo's own). Render type 11 off the client thread, so
 * 1.7.10's pathing still sees a fence.
 */
public class McFlyFence extends BlockFence implements ModelledBlock, NamedMetas {

	/** A wood a fence or gate is made of: its name, its planks and their meta, whether it burns, its sounds, its colour. */
	public static final class Wood {
		public final String name;
		final Block planks;
		final int planksMeta;
		public final boolean burns;
		public final SoundType sound;
		/** The gate's sounds, `<this>.open` and `.close` ("block.fence_gate"). */
		public final String gateSound;
		final MapColor colour;
		/** Drawn from its own models (bamboo's `bamboo_fence_*`), not the templates with its planks. */
		final String custom;
		final boolean shown;

		public Wood(String name, Block planks, int planksMeta, boolean burns, SoundType sound, String gateSound, MapColor colour, String custom, boolean shown) {
			this.name = name;
			this.planks = planks;
			this.planksMeta = planksMeta;
			this.burns = burns;
			this.sound = sound;
			this.gateSound = gateSound;
			this.colour = colour;
			this.custom = custom;
			this.shown = shown;
		}

		@SideOnly(Side.CLIENT)
		IIcon planksIcon() {
			return (planks != null ? planks : Blocks.planks).getIcon(2, planks != null ? planksMeta : 0);
		}
	}

	private static final String[] SIDES = {"north", "east", "south", "west"};
	private static final ForgeDirection[] DIRECTIONS = {ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST};

	private final Wood[] woods;
	@SideOnly(Side.CLIENT)
	private IIcon[] customIcons;

	public McFlyFence(Wood... woods) {
		super("", Material.wood);
		this.woods = woods;
		setBlockName(McFly.name(woods[0].name + "_fence"));
		setHardness(2.0F);
		this.blockResistance = 3.0F * 5.0F;
		// one wood's own; several woods' the wood sound, the others' by EFR's per-variant table (crimson's nether wood)
		setStepSound(woods.length == 1 ? woods[0].sound : soundTypeWood);
		setHarvestLevel("axe", 0);
		setCreativeTab(McFlyTab.TAB);
	}

	public Wood wood(int meta) {
		return woods[meta >= 0 && meta < woods.length ? meta : 0];
	}

	@Override
	public String nameOf(int meta) {
		return wood(meta).name + "_fence";
	}

	@Override
	public boolean isVariant(int meta) {
		return meta >= 0 && meta < woods.length;
	}

	@Override
	public int damageDropped(int meta) {
		return meta >= 0 && meta < woods.length ? meta : 0;
	}

	@Override
	public int getDamageValue(World world, int x, int y, int z) {
		return damageDropped(world.getBlockMetadata(x, y, z));
	}

	@Override
	public MapColor getMapColor(int meta) {
		return wood(meta).colour;
	}

	@Override
	public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return wood(world.getBlockMetadata(x, y, z)).burns;
	}

	@Override
	public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return isFlammable(world, x, y, z, face) ? 20 : 0;
	}

	@Override
	public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return isFlammable(world, x, y, z, face) ? 5 : 0;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		for (int i = 0; i < woods.length; i++) {
			if (woods[i].shown) {
				list.add(new ItemStack(item, 1, i));
			}
		}
	}

	@Override
	public boolean canPlaceTorchOnTop(World world, int x, int y, int z) {
		return true;
	}

	// ---------------------------------------------------------------- joining, shapes

	/** 1.7.10's question with no side given (other mods' callers): 26.3's rule, any gate joining. */
	@Override
	public boolean canConnectFenceTo(IBlockAccess world, int x, int y, int z) {
		return FenceJoin.joins(this, world, x, y, z, null);
	}

	private boolean joins(IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
		return FenceJoin.joins(this, world, x + direction.offsetX, y, z + direction.offsetZ, direction);
	}

	/** 1.7.10's outline (26.3's shape): the post and its arms. */
	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		boolean north = joins(world, x, y, z, ForgeDirection.NORTH), south = joins(world, x, y, z, ForgeDirection.SOUTH);
		boolean west = joins(world, x, y, z, ForgeDirection.WEST), east = joins(world, x, y, z, ForgeDirection.EAST);
		setBlockBounds(west ? 0.0F : 0.375F, 0.0F, north ? 0.0F : 0.375F, east ? 1.0F : 0.625F, 1.0F, south ? 1.0F : 0.625F);
	}

	/** 1.7.10's collision (26.3's): 1.5 high, a bar along each axis it joins on (or the post alone). */
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
		boolean north = joins(world, x, y, z, ForgeDirection.NORTH), south = joins(world, x, y, z, ForgeDirection.SOUTH);
		boolean west = joins(world, x, y, z, ForgeDirection.WEST), east = joins(world, x, y, z, ForgeDirection.EAST);
		if (north || south) {
			add(list, mask, AxisAlignedBB.getBoundingBox(x + 0.375D, y, z + (north ? 0.0D : 0.375D), x + 0.625D, y + 1.5D, z + (south ? 1.0D : 0.625D)));
		}
		if (west || east || !north && !south) {
			add(list, mask, AxisAlignedBB.getBoundingBox(x + (west ? 0.0D : 0.375D), y, z + 0.375D, x + (east ? 1.0D : 0.625D), y + 1.5D, z + 0.625D));
		}
		setBlockBoundsBasedOnState(world, x, y, z);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void add(List list, AxisAlignedBB mask, AxisAlignedBB box) {
		if (mask.intersectsWith(box)) {
			list.add(box);
		}
	}

	// ---------------------------------------------------------------- the look

	@Override
	public int getRenderType() {
		return FMLCommonHandler.instance().getEffectiveSide().isClient() ? McFlyRenderIds.MODEL : 11;
	}

	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		Wood wood = wood(world.getBlockMetadata(x, y, z));
		List<ModelPart> parts = new ArrayList<>();
		parts.add(new ModelPart(wood.custom != null ? wood.custom + "_fence_post" : "fence_post"));
		for (int i = 0; i < 4; i++) {
			if (joins(world, x, y, z, DIRECTIONS[i])) {
				parts.add(wood.custom != null ? new ModelPart(wood.custom + "_fence_side_" + SIDES[i]) : new ModelPart("fence_side", 0, 90 * i, true));
			}
		}
		return parts;
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		Wood wood = wood(meta);
		return Collections.singletonList(new ModelPart(wood.custom != null ? wood.custom + "_fence_inventory" : "fence_inventory"));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		return modelIcon(texture, 0);
	}

	/** The templates' `#texture` is the wood's planks; a custom wood's own textures by name (`block/bamboo_fence`). */
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture, int meta) {
		Wood wood = wood(meta);
		if (wood.custom != null && customIcons != null) {
			return texture.endsWith("_particle") ? customIcons[1] : customIcons[0];
		}
		return wood.planksIcon();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		Wood wood = wood(meta);
		return wood.custom != null && customIcons != null ? customIcons[1] : wood.planksIcon();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		for (Wood wood : woods) {
			if (wood.custom != null) {
				customIcons = new IIcon[]{register.registerIcon(wood.custom + "_fence"), register.registerIcon(wood.custom + "_fence_particle")};
			}
		}
	}
}
