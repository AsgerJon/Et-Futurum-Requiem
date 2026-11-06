package io.github.asgerjon.mcfly.boat;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * A chest boat or chest raft (SPEC-modern-movement.md 91.12; 26.3's `AbstractChestBoat`) on EFR's id
 * (`etfuturum.chest_boat`) and `Items`: 27 slots, one passenger 0.15 forward; opened by a sneaking player, by one when
 * it's full, or by its passenger's inventory key; its contents dropped when it goes (not when it changes dimension).
 * Broken, it drops its chest boat item (with `dropVehiclesTogether` off, a boat and a chest).
 */
public class McFlyChestBoat extends McFlyBoat implements IInventory {

	private ItemStack[] items = new ItemStack[27];

	public McFlyChestBoat(World world) {
		super(world);
	}

	@Override
	public boolean isChest() {
		return true;
	}

	@Override
	protected int maxPassengers() {
		return 1;
	}

	@Override
	protected float singlePassengerOffset() {
		return 0.15F;
	}

	@Override
	public boolean interactFirst(EntityPlayer player) {
		if (super.interactFirst(player)) {
			return true;
		}
		if (canAddPassenger(player) && !player.isSneaking()) {
			return false;
		}
		if (!worldObj.isRemote) {
			openFor(player);
		}
		return true;
	}

	/** Its chest's screen (`container_open`). */
	public void openFor(EntityPlayer player) {
		player.displayGUIChest(this);
		GameEvents.post(worldObj, GameEvent.get("container_open"), posX, posY, posZ, player);
	}

	@Override
	protected void destroy(DamageSource source) {
		if (McFlyConfig.dropVehiclesTogether) {
			super.destroy(source);
			return;
		}
		// 1.7.10's way: a boat and a chest apart
		setDead();
		BoatTypes.Type type = type().isEnabled() ? type() : BoatTypes.OAK;
		Item boat = type.boatItem();
		if (boat != null) {
			ItemStack stack = new ItemStack(boat);
			if (getCustomName() != null) {
				stack.setStackDisplayName(getCustomName());
			}
			entityDropItem(stack, 0.0F);
		}
		entityDropItem(new ItemStack(Blocks.chest), 0.0F);
	}

	@Override
	public void setDead() {
		if (!worldObj.isRemote && !isDead) {
			dropContents();
		}
		super.setDead();
	}

	/** 26.3's `Containers.dropContents`: each stack in pieces of 10 to 30, round its block's middle. */
	private void dropContents() {
		double size = 0.25D;
		double centre = 1.0D - size;
		double half = size / 2.0D;
		for (int i = 0; i < items.length; i++) {
			ItemStack stack = items[i];
			items[i] = null;
			if (stack == null) {
				continue;
			}
			double x = Math.floor(posX) + rand.nextDouble() * centre + half;
			double y = Math.floor(posY) + rand.nextDouble() * centre;
			double z = Math.floor(posZ) + rand.nextDouble() * centre + half;
			while (stack.stackSize > 0) {
				EntityItem item = new EntityItem(worldObj, x, y, z, stack.splitStack(Math.min(stack.stackSize, rand.nextInt(21) + 10)));
				item.motionX = triangle(0.0D, 0.11485000171139836D);
				item.motionY = triangle(0.2D, 0.11485000171139836D);
				item.motionZ = triangle(0.0D, 0.11485000171139836D);
				worldObj.spawnEntityInWorld(item);
			}
		}
	}

	private double triangle(double mean, double spread) {
		return mean + spread * (rand.nextDouble() - rand.nextDouble());
	}

	// ---------------------------------------------------------------- the chest

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
		ItemStack stack = items[slot];
		if (stack == null) {
			return null;
		}
		if (stack.stackSize <= count) {
			items[slot] = null;
			return stack;
		}
		return stack.splitStack(count);
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
	}

	/** Its name, or its item's (26.3's title is the entity's name). */
	@Override
	public String getInventoryName() {
		if (getCustomName() != null) {
			return getCustomName();
		}
		Item item = dropItem();
		return item == null ? "container.chest" : item.getUnlocalizedName() + ".name";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return getCustomName() != null;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public void markDirty() {
	}

	/** 26.3's `isChestVehicleStillValid`: there, and within the player's reach of entities and 4 more (7) of the eyes. */
	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		if (isDead) {
			return false;
		}
		double eyeY = player.posY + (worldObj.isRemote ? player.getEyeHeight() - player.getDefaultEyeHeight() : player.getEyeHeight());
		double dx = Math.max(Math.max(boundingBox.minX - player.posX, player.posX - boundingBox.maxX), 0.0D);
		double dy = Math.max(Math.max(boundingBox.minY - eyeY, eyeY - boundingBox.maxY), 0.0D);
		double dz = Math.max(Math.max(boundingBox.minZ - player.posZ, player.posZ - boundingBox.maxZ), 0.0D);
		return dx * dx + dy * dy + dz * dz < 49.0D;
	}

	@Override
	public void openInventory() {
	}

	@Override
	public void closeInventory() {
		if (!worldObj.isRemote) {
			GameEvents.post(worldObj, GameEvent.get("container_close"), posX, posY, posZ, null);
		}
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return true;
	}

	// ---------------------------------------------------------------- saved

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		super.readEntityFromNBT(tag);
		items = new ItemStack[27];
		NBTTagList list = tag.getTagList("Items", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound entry = list.getCompoundTagAt(i);
			int slot = entry.getByte("Slot") & 255;
			if (slot < items.length) {
				items[slot] = ItemStack.loadItemStackFromNBT(entry);
			}
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		super.writeEntityToNBT(tag);
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null) {
				NBTTagCompound entry = new NBTTagCompound();
				entry.setByte("Slot", (byte) i);
				items[i].writeToNBT(entry);
				list.appendTag(entry);
			}
		}
		tag.setTag("Items", list);
	}
}
