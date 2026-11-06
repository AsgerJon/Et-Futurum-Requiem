package io.github.asgerjon.mcfly.wood;

import io.github.asgerjon.mcfly.base.McFlyShelf;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.LinkedList;
import java.util.List;

/**
 * Shelves (SPEC-modern-movement.md 91.10, step H; 26.3's `ShelfBlockEntity`, `SideChainPartBlock`, `SideChainPart`): the
 * tile entity (three items, `align_items_to_bottom`, and 26.3's `side_chain`, which can't be worked out from the
 * neighbours: a shelf joins the chains beside it as it powers up, while the chain stays at 3 or fewer), and the chain's
 * rules on 1.7.10's positions. A shelf's meta: facing `& 3` (0 north, 1 south, 2 west, 3 east), `+4` powered.
 */
public final class Shelves {

	/** 26.3's `SideChainPart` by its order. */
	public static final int UNCONNECTED = 0, RIGHT = 1, CENTER = 2, LEFT = 3;
	public static final int MAX_CHAIN = 3;
	/** A facing's 1.7.10 side: north 2, south 3, west 4, east 5. */
	private static final int[] SIDE = {2, 3, 4, 5};
	private static final int[][] STEP = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

	private Shelves() {
	}

	/** The shelf's tile entity, `etfuturum.shelf`: an inventory of 3 (hoppers and comparators see it), sent with the block. */
	public static class Tile extends TileEntity implements IInventory {

		private final ItemStack[] items = new ItemStack[3];
		public boolean alignItemsToBottom;
		public int sideChain;

		/** 26.3's `swapItemNoUpdate`: the slot's stack out, the given in. */
		public ItemStack swap(int slot, ItemStack in) {
			ItemStack out = items[slot];
			items[slot] = in != null && in.stackSize > 0 ? in : null;
			return out;
		}

		/** 26.3's `setChanged(event)`: saved, sent, and the event posted. */
		public void changed(String event, EntityPlayer by) {
			markDirty();
			if (worldObj != null) {
				if (event != null) {
					GameEvents.postAtBlock(worldObj, event, xCoord, yCoord, zCoord, by, getBlockType(), getBlockMetadata());
				}
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
				worldObj.func_147453_f(xCoord, yCoord, zCoord, getBlockType());
			}
		}

		@Override
		public void writeToNBT(NBTTagCompound tag) {
			super.writeToNBT(tag);
			writeContents(tag);
		}

		@Override
		public void readFromNBT(NBTTagCompound tag) {
			super.readFromNBT(tag);
			readContents(tag);
		}

		private void writeContents(NBTTagCompound tag) {
			NBTTagList list = new NBTTagList();
			for (int slot = 0; slot < items.length; slot++) {
				if (items[slot] != null) {
					NBTTagCompound entry = new NBTTagCompound();
					entry.setByte("Slot", (byte) slot);
					items[slot].writeToNBT(entry);
					list.appendTag(entry);
				}
			}
			tag.setTag("Items", list);
			tag.setBoolean("align_items_to_bottom", alignItemsToBottom);
			tag.setInteger("side_chain", sideChain);
		}

		private void readContents(NBTTagCompound tag) {
			for (int slot = 0; slot < items.length; slot++) {
				items[slot] = null;
			}
			NBTTagList list = tag.getTagList("Items", 10);
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound entry = list.getCompoundTagAt(i);
				int slot = entry.getByte("Slot") & 255;
				if (slot < items.length) {
					items[slot] = ItemStack.loadItemStackFromNBT(entry);
				}
			}
			alignItemsToBottom = tag.getBoolean("align_items_to_bottom");
			sideChain = tag.getInteger("side_chain") & 3;
		}

		@Override
		public Packet getDescriptionPacket() {
			NBTTagCompound tag = new NBTTagCompound();
			writeContents(tag);
			return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
		}

		@Override
		public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
			int was = sideChain;
			readContents(packet.func_148857_g());
			if (was != sideChain && worldObj != null) {
				worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
			}
		}

		@Override
		public boolean canUpdate() {
			return false;
		}

		// ---------------------------------------------------------------- the inventory

		@Override
		public int getSizeInventory() {
			return items.length;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return items[slot];
		}

		@Override
		public ItemStack decrStackSize(int slot, int count) {
			if (items[slot] == null) {
				return null;
			}
			ItemStack taken = items[slot].splitStack(count);
			if (items[slot].stackSize <= 0) {
				items[slot] = null;
			}
			changed("block_activate", null);
			return taken;
		}

		@Override
		public ItemStack getStackInSlotOnClosing(int slot) {
			ItemStack stack = items[slot];
			items[slot] = null;
			return stack;
		}

		@Override
		public void setInventorySlotContents(int slot, ItemStack stack) {
			items[slot] = stack;
			changed("block_activate", null);
		}

		@Override
		public String getInventoryName() {
			return "container.etfuturum.shelf";
		}

		@Override
		public boolean hasCustomInventoryName() {
			return false;
		}

		@Override
		public int getInventoryStackLimit() {
			return 64;
		}

		@Override
		public boolean isUseableByPlayer(EntityPlayer player) {
			return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
		}

		@Override
		public void openInventory() {
		}

		@Override
		public void closeInventory() {
		}

		@Override
		public boolean isItemValidForSlot(int slot, ItemStack stack) {
			return true;
		}
	}

	// ---------------------------------------------------------------- 26.3's `SideChainPartBlock`

	public static int facing(int meta) {
		return meta & 3;
	}

	public static int sideOf(int meta) {
		return SIDE[meta & 3];
	}

	/** The facing a shelf faces when the player looks along a side (26.3's `getHorizontalDirection().getOpposite()`). */
	public static int facingAgainst(int lookSide) {
		return (lookSide ^ 1) - 2;
	}

	/** 26.3's `isConnectable`: a powered shelf. */
	private static boolean connectable(World world, int x, int y, int z) {
		return world.getBlock(x, y, z) instanceof McFlyShelf && (world.getBlockMetadata(x, y, z) & 4) != 0;
	}

	private static Tile tile(World world, int x, int y, int z) {
		return world.getTileEntity(x, y, z) instanceof Tile tile ? tile : null;
	}

	/** The part of the shelf at a place that faces the same way and is powered, else -1. */
	private static int partOf(World world, int[] pos, int facing) {
		if (!connectable(world, pos[0], pos[1], pos[2]) || facing(world.getBlockMetadata(pos[0], pos[1], pos[2])) != facing) {
			return -1;
		}
		Tile tile = tile(world, pos[0], pos[1], pos[2]);
		return tile == null ? -1 : tile.sideChain;
	}

	private static void setPart(World world, int[] pos, int part) {
		Tile tile = tile(world, pos[0], pos[1], pos[2]);
		if (tile != null && tile.sideChain != part) {
			tile.sideChain = part;
			tile.changed(null, null);
		}
	}

	/** The place `steps` to the shelf's left (its facing turned clockwise) or right. */
	private static int[] beside(int x, int y, int z, int facing, boolean left, int steps) {
		int side = SIDE[facing];
		// clockwise seen from above: north to east, east to south, south to west, west to north
		int turned = left ? new int[]{5, 4, 2, 3}[side - 2] : new int[]{4, 5, 3, 2}[side - 2];
		int[] step = STEP[turned - 2];
		return new int[]{x + step[0] * steps, y, z + step[2] * steps};
	}

	private static boolean connectsTowards(int part, int end) {
		return part == CENTER || part == end;
	}

	private static boolean chainEnd(int part) {
		return part != CENTER;
	}

	private static int whenConnectedToTheRight(int part) {
		return part == UNCONNECTED || part == LEFT ? LEFT : CENTER;
	}

	private static int whenConnectedToTheLeft(int part) {
		return part == UNCONNECTED || part == RIGHT ? RIGHT : CENTER;
	}

	private static int whenDisconnectedFromTheRight(int part) {
		return part == UNCONNECTED || part == LEFT ? UNCONNECTED : RIGHT;
	}

	private static int whenDisconnectedFromTheLeft(int part) {
		return part == UNCONNECTED || part == RIGHT ? UNCONNECTED : LEFT;
	}

	/** 26.3's `getAllBlocksConnectedTo`: the chain through this shelf, its left end first. */
	public static List<int[]> chain(World world, int x, int y, int z) {
		List<int[]> results = new LinkedList<>();
		if (!connectable(world, x, y, z)) {
			return results;
		}
		int facing = facing(world.getBlockMetadata(x, y, z));
		results.add(new int[]{x, y, z});
		for (int steps = 1; steps < MAX_CHAIN; steps++) {
			int[] pos = beside(x, y, z, facing, true, steps);
			int part = partOf(world, pos, facing);
			if (part >= 0 && connectsTowards(part, LEFT)) {
				results.add(0, pos);
			}
			if (part < 0 || chainEnd(part)) {
				break;
			}
		}
		for (int steps = 1; steps < MAX_CHAIN; steps++) {
			int[] pos = beside(x, y, z, facing, false, steps);
			int part = partOf(world, pos, facing);
			if (part >= 0 && connectsTowards(part, RIGHT)) {
				results.add(pos);
			}
			if (part < 0 || chainEnd(part)) {
				break;
			}
		}
		return results;
	}

	/** 26.3's `updateSelfAndNeighborsOnPoweringUp`: join the chains beside it while the whole stays at 3 or fewer. */
	public static void poweredUp(World world, int x, int y, int z) {
		if (!connectable(world, x, y, z)) {
			return;
		}
		int facing = facing(world.getBlockMetadata(x, y, z));
		int[] left = beside(x, y, z, facing, true, 1);
		int[] right = beside(x, y, z, facing, false, 1);
		int leftPart = partOf(world, left, facing), rightPart = partOf(world, right, facing);
		int onTheLeft = leftPart >= 0 ? chain(world, left[0], left[1], left[2]).size() : 0;
		int onTheRight = rightPart >= 0 ? chain(world, right[0], right[1], right[2]).size() : 0;
		int part = UNCONNECTED;
		int length = 1;
		if (onTheLeft > 0 && length + onTheLeft <= MAX_CHAIN) {
			part = whenConnectedToTheLeft(part);
			setPart(world, left, whenConnectedToTheRight(leftPart));
			length += onTheLeft;
		}
		if (onTheRight > 0 && length + onTheRight <= MAX_CHAIN) {
			part = whenConnectedToTheRight(part);
			setPart(world, right, whenConnectedToTheLeft(rightPart));
		}
		setPart(world, new int[]{x, y, z}, part);
	}

	/** 26.3's `updateNeighborsAfterPoweringDown`: the shelves beside it let go of it. */
	public static void poweredDown(World world, int x, int y, int z, int meta) {
		int facing = facing(meta);
		int[] left = beside(x, y, z, facing, true, 1);
		int[] right = beside(x, y, z, facing, false, 1);
		int leftPart = partOf(world, left, facing);
		if (leftPart >= 0) {
			setPart(world, left, whenDisconnectedFromTheRight(leftPart));
		}
		int rightPart = partOf(world, right, facing);
		if (rightPart >= 0) {
			setPart(world, right, whenDisconnectedFromTheLeft(rightPart));
		}
	}
}
