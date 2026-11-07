package io.github.asgerjon.mcfly.workstation;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import java.util.ArrayList;
import java.util.List;

/**
 * The barrel's block entity (SPEC-modern-movement.md 92.4; 26.3's `BarrelBlockEntity` and `ContainerOpenersCounter`),
 * under EFR's `etfuturum.barrel`: 27 slots, open to hoppers on every face; its openers counted as 26.3's (a player in
 * at the menu's making, out at its closing, spectators not; at the first and the last its sound at the lid, the open
 * meta, McFly's game event; while open, a recheck every 5 ticks of who still has it open). EFR's `Items` and
 * `CustomName` read and written; its `Type` (Iron Chests' tiers) read past.
 */
public class BarrelTile extends TileEntity implements IInventory {

	public static final int SIZE = 27;
	/** 26.3's `CHECK_TICK_DELAY`. */
	static final int RECHECK_TICKS = 5;

	private final ItemStack[] items = new ItemStack[SIZE];
	private String customName;
	/** 26.3's `openCount` and `maxInteractionRange` (the server's). */
	private int openCount;
	private double maxInteractionRange;

	// ---------------------------------------------------------------- openers (the server's; 26.3's counter)

	/** A player opening it (from its menu): 26.3's `startOpen`. */
	void startOpen(EntityPlayer player) {
		if (isInvalid() || worldObj == null || worldObj.isRemote || Spectator.isSpectator(player)) {
			return;
		}
		int previous = openCount++;
		if (previous == 0) {
			opened(true, player);
			scheduleRecheck();
		}
		maxInteractionRange = Math.max(ContainerReach.range(player), maxInteractionRange);
	}

	/** A player closing it (from its menu): 26.3's `stopOpen`. */
	void stopOpen(EntityPlayer player) {
		if (isInvalid() || worldObj == null || worldObj.isRemote || Spectator.isSpectator(player)) {
			return;
		}
		openCount--;
		if (openCount == 0) {
			opened(false, player);
			maxInteractionRange = 0.0D;
		}
	}

	/** Its block's scheduled tick: 26.3's `recheckOpeners`, from who has it open now. */
	void recheckOpen() {
		if (isInvalid() || worldObj == null || worldObj.isRemote) {
			return;
		}
		List<EntityPlayer> users = usersNow();
		maxInteractionRange = 0.0D;
		for (EntityPlayer user : users) {
			maxInteractionRange = Math.max(ContainerReach.range(user), maxInteractionRange);
		}
		int count = users.size();
		int previous = openCount;
		if (previous != count) {
			if (count != 0 && previous == 0) {
				opened(true, null);
			} else if (count == 0) {
				opened(false, null);
			}
			openCount = count;
		}
		if (count > 0) {
			scheduleRecheck();
		}
	}

	/** The players near it whose open container is its own (26.3's `getEntitiesWithContainerOpen`), not spectators. */
	private List<EntityPlayer> usersNow() {
		double range = maxInteractionRange + 4.0D;
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(xCoord - range, yCoord - range, zCoord - range, xCoord + 1 + range, yCoord + 1 + range, zCoord + 1 + range);
		List<EntityPlayer> users = new ArrayList<>();
		for (Object entity : worldObj.getEntitiesWithinAABB(EntityPlayer.class, box)) {
			EntityPlayer player = (EntityPlayer) entity;
			if (!Spectator.isSpectator(player) && isOwnContainer(player)) {
				users.add(player);
			}
		}
		return users;
	}

	/** 26.3's `isOwnContainer`: a chest's menu over this barrel. */
	private boolean isOwnContainer(EntityPlayer player) {
		return player.openContainer instanceof ContainerChest chest && chest.getLowerChestInventory() == this;
	}

	private void scheduleRecheck() {
		worldObj.scheduleBlockUpdate(xCoord, yCoord, zCoord, getBlockType(), RECHECK_TICKS);
	}

	/** 26.3's `onOpen` and `onClose`: its sound at the lid, the open meta, the game event (by who did it, or none). */
	private void opened(boolean open, EntityPlayer player) {
		int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
		int facing = BarrelBlock.facing(meta);
		double x = xCoord + 0.5D + net.minecraft.util.Facing.offsetsXForSide[facing] / 2.0D;
		double y = yCoord + 0.5D + net.minecraft.util.Facing.offsetsYForSide[facing] / 2.0D;
		double z = zCoord + 0.5D + net.minecraft.util.Facing.offsetsZForSide[facing] / 2.0D;
		worldObj.playSoundEffect(x, y, z, McFly.MC_ASSETS + (open ? ":block.barrel.open" : ":block.barrel.close"), 0.5F, worldObj.rand.nextFloat() * 0.1F + 0.9F);
		worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, facing | (open ? BarrelBlock.OPEN : 0), 3);
		GameEventHooks.atBlock(worldObj, open ? "container_open" : "container_close", xCoord, yCoord, zCoord, player);
	}

	/** The 1.7.10 container calls (no player): its menu counts the player instead. */
	@Override
	public void openInventory() {
	}

	@Override
	public void closeInventory() {
	}

	// ---------------------------------------------------------------- its 27 slots

	@Override
	public int getSizeInventory() {
		return SIZE;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return items[slot];
	}

	@Override
	public ItemStack decrStackSize(int slot, int count) {
		ItemStack stack = items[slot];
		if (stack == null) {
			return null;
		}
		ItemStack taken;
		if (stack.stackSize <= count) {
			taken = stack;
			items[slot] = null;
		} else {
			taken = stack.splitStack(count);
		}
		markDirty();
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
		if (stack != null && stack.stackSize > getInventoryStackLimit()) {
			stack.stackSize = getInventoryStackLimit();
		}
		markDirty();
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return true;
	}

	/** 26.3's `stillValid`. */
	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return ContainerReach.stillValid(this, player);
	}

	/** Its title: its custom name, or 26.3's `container.barrel`. */
	@Override
	public String getInventoryName() {
		return customName != null ? customName : "container.barrel";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return customName != null;
	}

	public void setCustomName(String name) {
		customName = name;
		if (worldObj != null && !worldObj.isRemote) {
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
	}

	/** 26.3's barrel never ticks: its recheck is its block's scheduled tick. */
	@Override
	public boolean canUpdate() {
		return false;
	}

	/** Its block changing is its own meta (open), not a new block: the tile entity stays. */
	@Override
	public boolean shouldRefresh(Block oldBlock, Block newBlock, int oldMeta, int newMeta, net.minecraft.world.World world, int x, int y, int z) {
		return oldBlock != newBlock;
	}

	// ---------------------------------------------------------------- stored form

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		// EFR's `Type` (Iron Chests' tiers) is read past: a metal barrel's first 27 slots are kept, as EFR did without them
		java.util.Arrays.fill(items, null);
		NBTTagList list = tag.getTagList("Items", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound entry = list.getCompoundTagAt(i);
			int slot = entry.getByte("Slot") & 255;
			if (slot < SIZE) {
				items[slot] = ItemStack.loadItemStackFromNBT(entry);
			}
		}
		customName = tag.hasKey("CustomName", 8) && !tag.getString("CustomName").isEmpty() ? tag.getString("CustomName") : null;
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < SIZE; i++) {
			if (items[i] != null) {
				NBTTagCompound entry = new NBTTagCompound();
				entry.setByte("Slot", (byte) i);
				items[i].writeToNBT(entry);
				list.appendTag(entry);
			}
		}
		tag.setTag("Items", list);
		if (customName != null) {
			tag.setString("CustomName", customName);
		}
	}

	/** The client's copy: its name only (its screen's title); the contents come with its menu. */
	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound tag = new NBTTagCompound();
		if (customName != null) {
			tag.setString("CustomName", customName);
		}
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		NBTTagCompound tag = packet.func_148857_g();
		customName = tag.hasKey("CustomName", 8) ? tag.getString("CustomName") : null;
	}
}
