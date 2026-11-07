package io.github.asgerjon.mcfly.workstation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.McFlyGuis;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Random;

/**
 * The smoker and blast furnace (SPEC-modern-movement.md 92.3; 26.3's `AbstractFurnaceBlock`, `SmokerBlock`,
 * `BlastFurnaceBlock`), on EFR's four ids: lit is a block of its own, as 1.7.10's furnace; the facing in the meta (2 to
 * 5), placed facing the player; 3.5 and a pickaxe, stone's sound, light 13 lit; its menu by use; its contents and kept
 * experience out when broken, itself named as it was (92.4); the comparator by its fullness; lit, its smoke and sound.
 */
public class CookerBlock extends McFlyBlock {

	/** Which of the two: its name (ids, textures, its menu's title), its screen's id, its lit and unlit blocks. */
	public enum Kind {
		SMOKER("smoker", McFlyGuis.SMOKER, "block.smoker.smoke"),
		BLAST_FURNACE("blast_furnace", McFlyGuis.BLAST_FURNACE, "block.blastfurnace.fire_crackle");

		public final String name;
		final int gui;
		final String sound;

		Kind(String name, int gui, String sound) {
			this.name = name;
			this.gui = gui;
			this.sound = sound;
		}

		Block unlit() {
			return this == SMOKER ? McFlyBlocks.SMOKER.get() : McFlyBlocks.BLAST_FURNACE.get();
		}

		Block lit() {
			return this == SMOKER ? McFlyBlocks.LIT_SMOKER.get() : McFlyBlocks.LIT_BLAST_FURNACE.get();
		}
	}

	/** While a lit swap replaces it: its contents and experience stay with the moved tile entity. */
	private static boolean swapping;

	private final Kind kind;
	private final boolean lit;
	private IIcon top;
	private IIcon bottom;
	private IIcon front;

	public CookerBlock(Kind kind, boolean lit) {
		super(Material.rock);
		this.kind = kind;
		this.lit = lit;
		setBlockName(McFly.name(kind.name));
		strength(3.5F, 3.5F);
		tool("pickaxe", 0);
		sound(soundTypePiston);
		if (lit) {
			light(13);
			setCreativeTab(null);
		}
	}

	public Kind kind() {
		return kind;
	}

	/** 1.7.10's furnace's swap: the other block, the same facing, the same tile entity. */
	static void setLit(World world, int x, int y, int z, boolean lit) {
		Block block = world.getBlock(x, y, z);
		if (!(block instanceof CookerBlock cooker) || cooker.lit == lit) {
			return;
		}
		int meta = world.getBlockMetadata(x, y, z);
		TileEntity tile = world.getTileEntity(x, y, z);
		swapping = true;
		world.setBlock(x, y, z, lit ? cooker.kind.lit() : cooker.kind.unlit(), meta, 3);
		swapping = false;
		if (tile != null) {
			tile.validate();
			world.setTileEntity(x, y, z, tile);
		}
	}

	@Override
	public boolean hasTileEntity(int meta) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		return kind == Kind.SMOKER ? new CookerTile.Smoker() : new CookerTile.BlastFurnace();
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		// facing the player: 26.3's `getHorizontalDirection().getOpposite()`
		int quarter = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
		int[] metas = {2, 5, 3, 4};
		world.setBlockMetadataWithNotify(x, y, z, metas[quarter], 2);
		if (stack.hasDisplayName() && world.getTileEntity(x, y, z) instanceof CookerTile tile) {
			tile.setCustomName(stack.getDisplayName());
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (!world.isRemote && world.getTileEntity(x, y, z) instanceof CookerTile) {
			McFlyGuis.open(player, kind.gui, world, x, y, z);
		}
		return true;
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if (!swapping && world.getTileEntity(x, y, z) instanceof CookerTile tile) {
			NamedDrops.broken(world, x, y, z, tile);
			dropContents(world, x, y, z, tile);
			tile.popExperience(world, x + 0.5D, y + 0.5D, z + 0.5D);
			world.func_147453_f(x, y, z, block);
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	/** 26.3's `Containers.dropContents`: each stack in pieces of 10 to 30, round the block's middle. */
	static void dropContents(World world, int x, int y, int z, IInventory inventory) {
		double size = 0.25D;
		double centre = 1.0D - size;
		double half = size / 2.0D;
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack == null) {
				continue;
			}
			inventory.setInventorySlotContents(i, null);
			double dx = x + world.rand.nextDouble() * centre + half;
			double dy = y + world.rand.nextDouble() * centre;
			double dz = z + world.rand.nextDouble() * centre + half;
			while (stack.stackSize > 0) {
				EntityItem item = new EntityItem(world, dx, dy, dz, stack.splitStack(Math.min(stack.stackSize, world.rand.nextInt(21) + 10)));
				item.motionX = 0.11485000171139836D * (world.rand.nextDouble() - world.rand.nextDouble());
				item.motionY = 0.2D + 0.11485000171139836D * (world.rand.nextDouble() - world.rand.nextDouble());
				item.motionZ = 0.11485000171139836D * (world.rand.nextDouble() - world.rand.nextDouble());
				world.spawnEntityInWorld(item);
			}
		}
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return Item.getItemFromBlock(kind.unlit());
	}

	/** Itself (the unlit one), named as it was (26.3's loot copies `custom_name`). */
	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
		return NamedDrops.drops(world, x, y, z, kind.unlit());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		return Item.getItemFromBlock(kind.unlit());
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

	/** Lit: 26.3's `animateTick`, its sound a tenth of the time and its smoke. */
	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(World world, int x, int y, int z, Random random) {
		if (!lit) {
			return;
		}
		double cx = x + 0.5D;
		double cz = z + 0.5D;
		if (random.nextDouble() < 0.1D) {
			world.playSound(cx, y, cz, McFly.MC_ASSETS + ":" + kind.sound, 1.0F, 1.0F, false);
		}
		if (kind == Kind.SMOKER) {
			world.spawnParticle("smoke", cx, y + 1.1D, cz, 0.0D, 0.0D, 0.0D);
			return;
		}
		int meta = world.getBlockMetadata(x, y, z);
		double across = random.nextDouble() * 0.6D - 0.3D;
		double up = random.nextDouble() * 9.0D / 16.0D;
		double dx = meta == 4 ? -0.52D : meta == 5 ? 0.52D : across;
		double dz = meta == 2 ? -0.52D : meta == 3 ? 0.52D : across;
		world.spawnParticle("smoke", cx + dx, y + up, cz + dz, 0.0D, 0.0D, 0.0D);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister icons) {
		blockIcon = icons.registerIcon(kind.name + "_side");
		top = icons.registerIcon(kind.name + "_top");
		bottom = kind == Kind.SMOKER ? icons.registerIcon("smoker_bottom") : top;
		front = icons.registerIcon(kind.name + (lit ? "_front_on" : "_front"));
	}

	/** Its facing in the meta; an item (meta 0) shows its front south, as 1.7.10 draws the furnace's. */
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		int facing = meta < 2 || meta > 5 ? 3 : meta;
		return side == 1 ? top : side == 0 ? bottom : side == facing ? front : blockIcon;
	}
}
