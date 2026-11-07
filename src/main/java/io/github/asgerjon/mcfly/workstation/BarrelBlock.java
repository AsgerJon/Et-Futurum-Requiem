package io.github.asgerjon.mcfly.workstation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.McFlyGuis;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The barrel (SPEC-modern-movement.md 92.4; 26.3's `BarrelBlock`), on EFR's id: its facing in the meta (0 to 5, 1.7.10's
 * and 26.3's order), plus 8 while open; placed facing back at the player along their nearest look (1.7.10's piston rule,
 * 26.3's since §33); 2.5, an axe's, wood's sound; its menu by use; its contents out when broken, itself named as it was;
 * the comparator by its fullness; drawn from 26.3's models, turned by its facing.
 */
public class BarrelBlock extends McFlyBlock implements ModelledBlock {

	/** The meta's open bit (EFR's). */
	public static final int OPEN = 8;

	private IIcon top;
	private IIcon topOpen;
	private IIcon bottom;

	public BarrelBlock() {
		super(Material.wood);
		setBlockName(McFly.name("barrel"));
		strength(2.5F, 2.5F);
		tool("axe", 0);
		sound(soundTypeWood);
		colour(MapColor.woodColor);
	}

	/** The facing in a meta (0 to 5). */
	public static int facing(int meta) {
		int facing = meta & 7;
		return facing > 5 ? 1 : facing;
	}

	public static boolean open(int meta) {
		return (meta & OPEN) != 0;
	}

	@Override
	public boolean hasTileEntity(int meta) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		return new BarrelTile();
	}

	/** 26.3's `getNearestLookingDirection().getOpposite()` (1.7.10's piston rule, made 26.3's by §33). */
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		world.setBlockMetadataWithNotify(x, y, z, BlockPistonBase.determineOrientation(world, x, y, z, placer), 2);
		if (stack.hasDisplayName() && world.getTileEntity(x, y, z) instanceof BarrelTile tile) {
			tile.setCustomName(stack.getDisplayName());
		}
	}

	/** 26.3's `useWithoutItem`: its menu (its statistic and the piglins' anger aren't in the fork). */
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (!world.isRemote && world.getTileEntity(x, y, z) instanceof BarrelTile) {
			McFlyGuis.open(player, McFlyGuis.BARREL, world, x, y, z);
		}
		return true;
	}

	/** 26.3's `tick`: the openers' recheck. */
	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		if (world.getTileEntity(x, y, z) instanceof BarrelTile tile) {
			tile.recheckOpen();
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if (world.getTileEntity(x, y, z) instanceof BarrelTile tile) {
			NamedDrops.broken(world, x, y, z, tile);
			CookerBlock.dropContents(world, x, y, z, tile);
			world.func_147453_f(x, y, z, block);
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	/** Itself, named as it was (26.3's loot copies `custom_name`). */
	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
		return NamedDrops.drops(world, x, y, z, this);
	}

	@Override
	public int damageDropped(int meta) {
		return 0;
	}

	@Override
	public boolean hasComparatorInputOverride() {
		return true;
	}

	@Override
	public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile instanceof IInventory inventory ? Container.calcRedstoneFromInventory(inventory) : 0;
	}

	// ---------------------------------------------------------------- drawn from 26.3's models

	@Override
	public int getRenderType() {
		return McFlyRenderIds.MODEL;
	}

	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		return Collections.singletonList(part(world.getBlockMetadata(x, y, z)));
	}

	/** 26.3's blockstate: up as the model is, down x 180, the sides x 90 then y by the facing. */
	private static ModelPart part(int meta) {
		String model = open(meta) ? "barrel_open" : "barrel";
		switch (facing(meta)) {
			case 0:
				return new ModelPart(model, 180, 0, false);
			case 2:
				return new ModelPart(model, 90, 0, false);
			case 3:
				return new ModelPart(model, 90, 180, false);
			case 4:
				return new ModelPart(model, 90, 270, false);
			case 5:
				return new ModelPart(model, 90, 90, false);
			default:
				return new ModelPart(model);
		}
	}

	/** Its item: 26.3's `barrel` model, facing up. */
	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(part(1));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		if (texture.endsWith("barrel_top_open")) {
			return topOpen;
		}
		if (texture.endsWith("barrel_top")) {
			return top;
		}
		if (texture.endsWith("barrel_bottom")) {
			return bottom;
		}
		return blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister icons) {
		blockIcon = icons.registerIcon("barrel_side");
		top = icons.registerIcon("barrel_top");
		topOpen = icons.registerIcon("barrel_top_open");
		bottom = icons.registerIcon("barrel_bottom");
	}

	/** Its faces by facing, for what draws it as a plain block (its particles, other mods). */
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		int facing = facing(meta);
		if (side == facing) {
			return open(meta) ? topOpen : top;
		}
		return side == (facing ^ 1) ? bottom : blockIcon;
	}
}
