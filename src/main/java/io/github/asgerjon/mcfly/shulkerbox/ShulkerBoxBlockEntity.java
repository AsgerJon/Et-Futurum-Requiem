package io.github.asgerjon.mcfly.shulkerbox;

import io.github.asgerjon.mcfly.McFly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import java.util.List;

/**
 * A shulker box (SPEC-modern-movement.md §81; 26.3's `ShulkerBoxBlockEntity`): 27 slots, stored as EFR stored it
 * (`Items` by `Slot`, `Color` 0 plain or a dye colour's id plus one, `Facing` 0 to 5, `CustomName`). Its lid opens and
 * shuts over 10 ticks (block event 1, the number of players looking in), pushing what's in its way; neither hoppers nor
 * its own slots take a shulker box.
 */
public class ShulkerBoxBlockEntity extends TileEntity implements ISidedInventory {

	public static final int SIZE = 27;
	private static final int[] SLOTS = new int[SIZE];

	static {
		for (int i = 0; i < SIZE; i++) {
			SLOTS[i] = i;
		}
	}

	public enum AnimationStatus {
		CLOSED, OPENING, OPENED, CLOSING
	}

	private ItemStack[] items = new ItemStack[SIZE];
	private int colour;
	private int facing = 1;
	private String customName;
	private int openCount;
	private AnimationStatus status = AnimationStatus.CLOSED;
	private float progress;
	private float progressOld;

	/** 0 plain, else a dye colour's id plus one. */
	public int colour() {
		return colour;
	}

	/** The way its lid opens: 1.7.10's directions (0 down, 1 up, 2 north, 3 south, 4 west, 5 east). */
	public int facing() {
		return facing;
	}

	public void setFacing(int facing) {
		this.facing = facing & 7;
	}

	public AnimationStatus animationStatus() {
		return status;
	}

	public boolean isClosed() {
		return status == AnimationStatus.CLOSED;
	}

	/** How far open (0 to 1), between the last tick and this. */
	public float getProgress(float partialTicks) {
		return progressOld + (progress - progressOld) * partialTicks;
	}

	public boolean isEmpty() {
		for (ItemStack stack : items) {
			if (stack != null) {
				return false;
			}
		}
		return true;
	}

	/** Its colour, contents and name from a box item (placing one). */
	public void setItemValues(ItemStack stack) {
		items = new ItemStack[SIZE];
		colour = 0;
		if (stack.hasTagCompound()) {
			readItems(stack.getTagCompound(), items);
			colour = stack.getTagCompound().getByte("Color") & 31;
		}
		customName = stack.hasDisplayName() ? stack.getDisplayName() : null;
	}

	/** The box as an item: its colour, its contents (if any) and its name. */
	public ItemStack createStack() {
		ItemStack stack = new ItemStack(getBlockType());
		NBTTagCompound tag = new NBTTagCompound();
		if (!isEmpty()) {
			tag.setTag("Items", writeItems(items));
		}
		if (colour > 0) {
			tag.setByte("Color", (byte) colour);
		}
		if (!tag.hasNoTags()) {
			stack.setTagCompound(tag);
		}
		if (customName != null) {
			stack.setStackDisplayName(customName);
		}
		return stack;
	}

	static void readItems(NBTTagCompound tag, ItemStack[] into) {
		NBTTagList list = tag.getTagList("Items", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound one = list.getCompoundTagAt(i);
			int slot = one.getByte("Slot") & 255;
			if (slot < into.length) {
				into[slot] = ItemStack.loadItemStackFromNBT(one);
			}
		}
	}

	static NBTTagList writeItems(ItemStack[] from) {
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < from.length; i++) {
			if (from[i] != null) {
				NBTTagCompound one = new NBTTagCompound();
				one.setByte("Slot", (byte) i);
				from[i].writeToNBT(one);
				list.appendTag(one);
			}
		}
		return list;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		items = new ItemStack[SIZE];
		readItems(nbt, items);
		colour = nbt.getByte("Color") & 31;
		facing = nbt.hasKey("Facing") ? nbt.getByte("Facing") & 7 : 1;
		customName = nbt.hasKey("CustomName", 8) ? nbt.getString("CustomName") : null;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setTag("Items", writeItems(items));
		nbt.setByte("Color", (byte) colour);
		nbt.setByte("Facing", (byte) facing);
		if (customName != null) {
			nbt.setString("CustomName", customName);
		}
	}

	/** To the client: its colour and facing (its contents go with its menu). */
	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte("Color", (byte) colour);
		nbt.setByte("Facing", (byte) facing);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		NBTTagCompound nbt = packet.func_148857_g();
		colour = nbt.getByte("Color") & 31;
		facing = nbt.getByte("Facing") & 7;
	}

	// ---------------------------------------------------------------- the lid (26.3's updateAnimation)

	@Override
	public void updateEntity() {
		progressOld = progress;
		switch (status) {
			case CLOSED -> progress = 0.0F;
			case OPENING -> {
				progress += 0.1F;
				if (progressOld == 0.0F) {
					neighbours();
				}
				if (progress >= 1.0F) {
					status = AnimationStatus.OPENED;
					progress = 1.0F;
					neighbours();
				}
				pushEntities();
			}
			case OPENED -> progress = 1.0F;
			case CLOSING -> {
				progress -= 0.1F;
				if (progressOld == 1.0F) {
					neighbours();
				}
				if (progress <= 0.0F) {
					status = AnimationStatus.CLOSED;
					progress = 0.0F;
					neighbours();
				}
			}
		}
	}

	private void neighbours() {
		if (worldObj != null) {
			worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
		}
	}

	/** Its outline as open as it is (26.3's getBoundingBox): its cube stretched out its open side by half the progress. */
	public AxisAlignedBB outline() {
		return ShulkerBoxBlock.lidBox(facing, -1.0F, 0.5F * getProgress(1.0F), xCoord + 0.5, yCoord, zCoord + 0.5);
	}

	/** What the lid has moved into since the last tick is pushed out of its way (26.3's moveCollidedEntities). */
	private void pushEntities() {
		AxisAlignedBB moved = ShulkerBoxBlock.lidBox(facing, progressOld * 0.5F, progress * 0.5F, xCoord + 0.5, yCoord, zCoord + 0.5);
		List<?> entities = worldObj.getEntitiesWithinAABBExcludingEntity(null, moved);
		int[] step = ShulkerBoxBlock.STEP[facing];
		for (Object found : entities) {
			Entity entity = (Entity) found;
			entity.moveEntity((moved.maxX - moved.minX + 0.01) * step[0], (moved.maxY - moved.minY + 0.01) * step[1], (moved.maxZ - moved.minZ + 0.01) * step[2]);
		}
	}

	@Override
	public boolean receiveClientEvent(int id, int count) {
		if (id == 1) {
			openCount = count;
			if (count == 0) {
				status = AnimationStatus.CLOSING;
			}
			if (count == 1) {
				status = AnimationStatus.OPENING;
			}
			return true;
		}
		return super.receiveClientEvent(id, count);
	}

	@Override
	public void openInventory() {
		if (worldObj == null || worldObj.isRemote) {
			return;
		}
		openCount = Math.max(0, openCount) + 1;
		worldObj.addBlockEvent(xCoord, yCoord, zCoord, getBlockType(), 1, openCount);
		if (openCount == 1) {
			worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, McFly.MC_ASSETS + ":block.shulker_box.open", 0.5F, worldObj.rand.nextFloat() * 0.1F + 0.9F);
			// 26.3's `container_open`, by the player opening it (§90.9, step H1)
			io.github.asgerjon.mcfly.gameevent.GameEventHooks.container(worldObj, xCoord, yCoord, zCoord, true);
		}
	}

	@Override
	public void closeInventory() {
		if (worldObj == null || worldObj.isRemote) {
			return;
		}
		boolean wasOpen = openCount > 0;
		openCount--;
		worldObj.addBlockEvent(xCoord, yCoord, zCoord, getBlockType(), 1, Math.max(0, openCount));
		if (openCount <= 0) {
			worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, McFly.MC_ASSETS + ":block.shulker_box.close", 0.5F, worldObj.rand.nextFloat() * 0.1F + 0.9F);
			if (wasOpen) {
				// 26.3's `container_close`, by the player closing it (§90.9, step H1)
				io.github.asgerjon.mcfly.gameevent.GameEventHooks.container(worldObj, xCoord, yCoord, zCoord, false);
			}
		}
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
		if (stack.stackSize <= count) {
			items[slot] = null;
			markDirty();
			return stack;
		}
		ItemStack taken = stack.splitStack(count);
		markDirty();
		return taken;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		return null;
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
	public String getInventoryName() {
		return customName != null ? customName : "container.shulkerBox";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return customName != null;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0;
	}

	/** No shulker box in a shulker box. */
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return !ShulkerBoxBlock.isBox(stack);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return SLOTS;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side) {
		return !ShulkerBoxBlock.isBox(stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return true;
	}
}
