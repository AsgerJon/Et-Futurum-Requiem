package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.wood.Doors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/**
 * A wooden door of ours (SPEC-modern-movement.md §91.6, step D), 26.3's `DoorBlock` by its set type in 1.7.10's door
 * metas (and the upper half's bit 1 26.3's powered: {@link Doors}): 3 and 3, the axe's, no fire; opened by hand, by a
 * change of power, by a wind burst; the set type's sounds by the new state. 1.7.10's renderer draws it (26.3's doors are
 * the same 3/16 slab), from `<texture>_top` and `_bottom`. Its item is {@link Item}: 26.3's placing.
 */
public class McFlyDoor extends BlockDoor implements McFlySetType.Typed {

	private final McFlySetType type;
	private final MapColor colour;
	private final String texture;

	public McFlyDoor(String name, McFlySetType type, MapColor colour, String texture) {
		super(Material.wood);
		this.type = type;
		this.colour = colour;
		this.texture = texture;
		setBlockName(McFly.name(name));
		setBlockTextureName(texture);
		setHardness(3.0F);
		this.blockResistance = 3.0F * 5.0F;
		setStepSound(type.sound);
		setHarvestLevel("axe", 0);
		disableStats();
		setCreativeTab(McFlyTab.TAB);
	}

	@Override
	public McFlySetType setType() {
		return type;
	}

	/** The item's texture (`items/<wood>_door`). */
	public String itemTexture() {
		return texture;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (!type.byHand) {
			return false;
		}
		int lowerY = Doors.lowerY(world, x, y, z);
		Doors.setOpen(this, world, x, lowerY, z, !Doors.isOpen(world, x, lowerY, z), player);
		return true;
	}

	/** 1.7.10's open-or-close (villagers, other mods): 26.3's `setOpen`. */
	@Override
	public void func_150014_a(World world, int x, int y, int z, boolean open) {
		Doors.setOpen(this, world, x, Doors.lowerY(world, x, y, z), z, open, null);
	}

	/** 1.7.10's halves and support, 26.3's power. */
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		int meta = world.getBlockMetadata(x, y, z);
		if ((meta & Doors.UPPER) != 0) {
			if (world.getBlock(x, y - 1, z) != this) {
				world.setBlockToAir(x, y, z);
			} else if (neighbour != this) {
				onNeighborBlockChange(world, x, y - 1, z, neighbour);
			}
			return;
		}
		boolean broken = false;
		if (world.getBlock(x, y + 1, z) != this) {
			world.setBlockToAir(x, y, z);
			broken = true;
		}
		if (!World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)) {
			world.setBlockToAir(x, y, z);
			broken = true;
			if (world.getBlock(x, y + 1, z) == this) {
				world.setBlockToAir(x, y + 1, z);
			}
		}
		if (broken) {
			if (!world.isRemote) {
				dropBlockAsItem(world, x, y, z, meta, 0);
			}
		} else if (neighbour != this) {
			Doors.redstone(this, world, x, y, z);
		}
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return (meta & Doors.UPPER) != 0 ? null : Item.getItemFromBlock(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		return Item.getItemFromBlock(this);
	}

	@Override
	public MapColor getMapColor(int meta) {
		return colour;
	}

	/** 1.7.10's door asks for `<texture>_upper` and `_lower`; 26.3's are `_top` and `_bottom`. */
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		super.registerBlockIcons(name -> register.registerIcon(name.replace("_upper", "_top").replace("_lower", "_bottom")));
	}

	// ---------------------------------------------------------------- the item

	/** 26.3's `DoubleHighBlockItem` with `DoorBlock.getStateForPlacement` and `getHinge`. */
	public static class DoorItem extends ItemBlock {

		@SideOnly(Side.CLIENT)
		private IIcon icon;

		public DoorItem(Block block) {
			super(block);
		}

		@Override
		public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
			Block clicked = world.getBlock(x, y, z);
			if (!clicked.isReplaceable(world, x, y, z)) {
				x += net.minecraft.util.Facing.offsetsXForSide[side];
				y += net.minecraft.util.Facing.offsetsYForSide[side];
				z += net.minecraft.util.Facing.offsetsZForSide[side];
				hitX = side == 4 ? 1.0F : side == 5 ? 0.0F : hitX;
				hitZ = side == 2 ? 1.0F : side == 3 ? 0.0F : hitZ;
			}
			Block door = field_150939_a;
			if (y >= 255 || !player.canPlayerEdit(x, y, z, side, stack) || !player.canPlayerEdit(x, y + 1, z, side, stack)
				|| !world.getBlock(x, y, z).isReplaceable(world, x, y, z) || !world.getBlock(x, y + 1, z).isReplaceable(world, x, y + 1, z)
				|| !World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)) {
				return false;
			}
			// 1.7.10's facing: 0 east, 1 south, 2 west, 3 north (the player's)
			int facing = MathHelper.floor_double((player.rotationYaw + 180.0F) * 4.0F / 360.0F - 0.5D) & 3;
			boolean powered = world.isBlockIndirectlyGettingPowered(x, y, z) || world.isBlockIndirectlyGettingPowered(x, y + 1, z);
			boolean right = hingeRight(world, x, y, z, facing, hitX, hitZ);
			world.setBlock(x, y, z, door, facing | (powered ? Doors.OPEN : 0), 2);
			world.setBlock(x, y + 1, z, door, Doors.UPPER | (right ? Doors.HINGE_RIGHT : 0) | (powered ? Doors.POWERED : 0), 2);
			world.notifyBlocksOfNeighborChange(x, y, z, door);
			world.notifyBlocksOfNeighborChange(x, y + 1, z, door);
			world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, door.stepSound.func_150496_b(), (door.stepSound.getVolume() + 1.0F) / 2.0F,
				door.stepSound.getPitch() * 0.8F);
			stack.stackSize--;
			return true;
		}

		/**
		 * 26.3's `getHinge`: by the full blocks and doors to the player's left and right (counter-clockwise and clockwise of
		 * its facing), then by where the click landed.
		 */
		static boolean hingeRight(World world, int x, int y, int z, int facing, float hitX, float hitZ) {
			// the facing's step: 0 east (+x), 1 south (+z), 2 west (-x), 3 north (-z)
			int stepX = facing == 0 ? 1 : facing == 2 ? -1 : 0;
			int stepZ = facing == 1 ? 1 : facing == 3 ? -1 : 0;
			// counter-clockwise (left) of (stepX, stepZ) is (stepZ, -stepX); clockwise (right) (-stepZ, stepX)
			int lx = x + stepZ, lz = z - stepX, rx = x - stepZ, rz = z + stepX;
			int balance = (world.getBlock(lx, y, lz).isNormalCube() ? -1 : 0) + (world.getBlock(lx, y + 1, lz).isNormalCube() ? -1 : 0)
				+ (world.getBlock(rx, y, rz).isNormalCube() ? 1 : 0) + (world.getBlock(rx, y + 1, rz).isNormalCube() ? 1 : 0);
			boolean doorLeft = world.getBlock(lx, y, lz) instanceof BlockDoor && (world.getBlockMetadata(lx, y, lz) & Doors.UPPER) == 0;
			boolean doorRight = world.getBlock(rx, y, rz) instanceof BlockDoor && (world.getBlockMetadata(rx, y, rz) & Doors.UPPER) == 0;
			if (doorLeft && !doorRight || balance > 0) {
				return true;
			}
			if (doorRight && !doorLeft || balance < 0) {
				return false;
			}
			boolean left = (stepX >= 0 || !(hitZ < 0.5F)) && (stepX <= 0 || !(hitZ > 0.5F)) && (stepZ >= 0 || !(hitX > 0.5F)) && (stepZ <= 0 || !(hitX < 0.5F));
			return !left;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
			icon = register.registerIcon(((McFlyDoor) field_150939_a).itemTexture());
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIconFromDamage(int damage) {
			return icon;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public int getSpriteNumber() {
			return 1;
		}
	}
}
