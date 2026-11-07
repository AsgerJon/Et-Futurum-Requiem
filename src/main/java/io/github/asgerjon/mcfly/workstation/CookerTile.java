package io.github.asgerjon.mcfly.workstation;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A smoker's or blast furnace's block entity (SPEC-modern-movement.md 92.3; 26.3's `AbstractFurnaceBlockEntity`), under
 * EFR's ids (`etfuturum.smoker`, `etfuturum.blast_furnace`): input, fuel and result; lit while its fuel burns; 26.3's
 * fast cooking (a fuel's burn time halved, its speed 2, so 100 ticks an item); experience kept by recipe until taken or
 * broken; hoppers by face. EFR's `BurnTime` and `CookTime` read, 26.3's names written.
 */
public abstract class CookerTile extends TileEntity implements ISidedInventory {

	/** 26.3's `cooking/fast_burn_time_reduction_factor` and `fast_speed_multiplier` for both. */
	private static final int BURN_REDUCTION = 2;
	private static final float SPEED = 2.0F;
	/** Every 26.3 smoking and blasting recipe's time. */
	private static final int RECIPE_TIME = 200;
	private static final int[] UP = {0};
	private static final int[] DOWN = {2, 1};
	private static final int[] SIDES = {1};

	private ItemStack[] items = new ItemStack[3];
	int litTimeRemaining;
	int litTotalTime;
	int cookingTimer;
	int cookingTotalTime;
	private float speedMultiplier = 1.0F;
	private final Map<String, Integer> recipesUsed = new LinkedHashMap<>();
	private String customName;

	/** Its recipes' kind (`CookingRecipes.SMOKING` or `BLASTING`). */
	public abstract String kind();

	/** Its block when unlit, then lit. */
	abstract CookerBlock.Kind blockKind();

	public static final class Smoker extends CookerTile {
		@Override
		public String kind() {
			return CookingRecipes.SMOKING;
		}

		@Override
		CookerBlock.Kind blockKind() {
			return CookerBlock.Kind.SMOKER;
		}
	}

	public static final class BlastFurnace extends CookerTile {
		@Override
		public String kind() {
			return CookingRecipes.BLASTING;
		}

		@Override
		CookerBlock.Kind blockKind() {
			return CookerBlock.Kind.BLAST_FURNACE;
		}
	}

	public boolean isLit() {
		return litTimeRemaining > 0;
	}

	// ---------------------------------------------------------------- 26.3's serverTick

	@Override
	public void updateEntity() {
		if (worldObj == null || worldObj.isRemote) {
			return;
		}
		boolean changed = false;
		boolean wasLit;
		boolean lit;
		if (litTimeRemaining > 0) {
			wasLit = true;
			litTimeRemaining--;
			lit = litTimeRemaining > 0;
		} else {
			wasLit = false;
			lit = false;
		}
		ItemStack fuel = items[1];
		ItemStack ingredient = items[0];
		boolean hasIngredient = ingredient != null;
		boolean hasFuel = fuel != null;
		if (lit || hasFuel && hasIngredient) {
			if (hasIngredient) {
				CookingRecipes.Recipe recipe = CookingRecipes.find(kind(), ingredient);
				if (recipe != null) {
					ItemStack result = recipe.result();
					if (canBurn(result)) {
						if (!lit) {
							int newLitTime = burnDuration(fuel);
							litTimeRemaining = newLitTime;
							litTotalTime = newLitTime;
							// 26.3's fast blocks: speed 2 whatever the fuel
							speedMultiplier = SPEED;
							if (cookingTotalTime > 0 && cookingTimer < cookingTotalTime) {
								float done = (float) cookingTimer / cookingTotalTime;
								cookingTotalTime = totalCookTime();
								cookingTimer = (int) Math.ceil(done * cookingTotalTime);
							}
							if (newLitTime > 0) {
								consumeFuel(fuel);
								lit = true;
								changed = true;
							}
						}
						if (lit) {
							// an input that came by a path not through setInventorySlotContents has no total yet
							if (cookingTotalTime <= 0) {
								cookingTotalTime = totalCookTime();
							}
							cookingTimer++;
							if (cookingTimer >= cookingTotalTime) {
								cookingTimer = 0;
								cookingTotalTime = totalCookTime();
								burn(result);
								recipesUsed.merge(recipe.id, 1, Integer::sum);
								changed = true;
							}
						} else {
							cookingTimer = 0;
						}
					} else {
						cookingTimer = 0;
					}
				}
			} else {
				cookingTimer = 0;
			}
		} else if (cookingTimer > 0) {
			cookingTimer = MathHelper.clamp_int(cookingTimer - 2, 0, cookingTotalTime);
		}
		if (wasLit != lit) {
			changed = true;
			CookerBlock.setLit(worldObj, xCoord, yCoord, zCoord, lit);
		}
		if (changed) {
			markDirty();
		}
	}

	/** 1.7.10's fuel time (with McFly's fuels), halved as 26.3's fast blocks have it. */
	private static int burnDuration(ItemStack fuel) {
		return fuel == null ? 0 : TileEntityFurnace.getItemBurnTime(fuel) / BURN_REDUCTION;
	}

	private int totalCookTime() {
		return speedMultiplier > 0.0F ? (int) Math.ceil(RECIPE_TIME / speedMultiplier) : RECIPE_TIME;
	}

	/** One used; its container item (a lava bucket's bucket) in its place, or dropped beside it while more are left. */
	private void consumeFuel(ItemStack fuel) {
		ItemStack remainder = fuel.getItem().hasContainerItem(fuel) ? fuel.getItem().getContainerItem(fuel) : null;
		fuel.stackSize--;
		ItemStack left = fuel.stackSize > 0 ? fuel : null;
		if (remainder != null) {
			if (left == null) {
				left = remainder;
			} else {
				worldObj.spawnEntityInWorld(new EntityItem(worldObj, xCoord, yCoord, zCoord, remainder));
			}
		}
		items[1] = left;
	}

	private boolean canBurn(ItemStack result) {
		ItemStack there = items[2];
		if (there == null) {
			return true;
		}
		if (there.getItem() != result.getItem() || there.getItemDamage() != result.getItemDamage() || !ItemStack.areItemStackTagsEqual(there, result)) {
			return false;
		}
		return there.stackSize + result.stackSize <= Math.min(getInventoryStackLimit(), result.getMaxStackSize());
	}

	private void burn(ItemStack result) {
		if (items[2] == null) {
			items[2] = result.copy();
		} else {
			items[2].stackSize += result.stackSize;
		}
		items[0].stackSize--;
		if (items[0].stackSize <= 0) {
			items[0] = null;
		}
	}

	// ---------------------------------------------------------------- experience

	/** 26.3's `getRecipesToAwardAndPopExperience`: each recipe's experience times its count, a fraction by chance; then forgotten. */
	public void popExperience(World world, double x, double y, double z) {
		if (world.isRemote) {
			recipesUsed.clear();
			return;
		}
		for (Map.Entry<String, Integer> used : recipesUsed.entrySet()) {
			CookingRecipes.Recipe recipe = CookingRecipes.byId(kind(), used.getKey());
			if (recipe == null) {
				continue;
			}
			float total = used.getValue() * recipe.experience;
			int reward = MathHelper.floor_float(total);
			float fraction = total - reward;
			if (fraction != 0.0F && world.rand.nextFloat() < fraction) {
				reward++;
			}
			while (reward > 0) {
				int orb = EntityXPOrb.getXPSplit(reward);
				reward -= orb;
				world.spawnEntityInWorld(new EntityXPOrb(world, x, y, z, orb));
			}
		}
		recipesUsed.clear();
		markDirty();
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
		ItemStack stack = items[slot];
		if (stack == null) {
			return null;
		}
		if (stack.stackSize <= count) {
			items[slot] = null;
			return stack;
		}
		ItemStack taken = stack.splitStack(count);
		if (stack.stackSize == 0) {
			items[slot] = null;
		}
		return taken;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		ItemStack stack = items[slot];
		items[slot] = null;
		return stack;
	}

	/** 26.3's `setItem`: a different input restarts the cook. */
	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		ItemStack old = items[slot];
		boolean same = stack != null && old != null && stack.getItem() == old.getItem() && stack.getItemDamage() == old.getItemDamage()
			&& ItemStack.areItemStackTagsEqual(stack, old);
		items[slot] = stack;
		if (stack != null && stack.stackSize > getInventoryStackLimit()) {
			stack.stackSize = getInventoryStackLimit();
		}
		if (slot == 0 && !same && worldObj != null && !worldObj.isRemote) {
			cookingTotalTime = CookingRecipes.find(kind(), stack) != null ? totalCookTime() : RECIPE_TIME;
			cookingTimer = 0;
			markDirty();
		}
	}

	@Override
	public String getInventoryName() {
		return customName != null ? customName : "container." + blockKind().name;
	}

	@Override
	public boolean hasCustomInventoryName() {
		return customName != null;
	}

	public void setCustomName(String name) {
		customName = name;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	/** 26.3's `stillValid`: there, and within 4 more than the player's block reach (4.5, 5 in creative) of its block. */
	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return ContainerReach.stillValid(this, player);
	}

	@Override
	public void openInventory() {
	}

	@Override
	public void closeInventory() {
	}

	/** 26.3's `canPlaceItem`: never the result; the fuel slot fuel, or a bucket while it holds none. */
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if (slot == 2) {
			return false;
		}
		if (slot != 1) {
			return true;
		}
		return TileEntityFurnace.isItemFuel(stack) || stack.getItem() == Items.bucket && (items[1] == null || items[1].getItem() != Items.bucket);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return side == 0 ? DOWN : side == 1 ? UP : SIDES;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side) {
		return isItemValidForSlot(slot, stack);
	}

	/** From below, only an empty bucket comes out of the fuel slot (`#furnace_fuel_bottom_takeable`). */
	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return side != 0 || slot != 1 || stack.getItem() == Items.bucket;
	}

	// ---------------------------------------------------------------- saved

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		items = new ItemStack[3];
		NBTTagList list = tag.getTagList("Items", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound entry = list.getCompoundTagAt(i);
			int slot = entry.getByte("Slot") & 255;
			if (slot < items.length) {
				items[slot] = ItemStack.loadItemStackFromNBT(entry);
			}
		}
		if (tag.hasKey("cooking_time_spent")) {
			cookingTimer = tag.getInteger("cooking_time_spent");
			cookingTotalTime = tag.getInteger("cooking_total_time");
			litTimeRemaining = tag.getInteger("lit_time_remaining");
			litTotalTime = tag.getInteger("lit_total_time");
			speedMultiplier = tag.hasKey("speed_multiplier") ? tag.getFloat("speed_multiplier") : 1.0F;
		} else {
			// EFR's: the fuel left and the cook done, at its fixed 100 ticks an item and fast fuel
			litTimeRemaining = tag.getInteger("BurnTime");
			litTotalTime = litTimeRemaining;
			cookingTimer = tag.getInteger("CookTime");
			cookingTotalTime = RECIPE_TIME / (int) SPEED;
			speedMultiplier = litTimeRemaining > 0 ? SPEED : 1.0F;
		}
		recipesUsed.clear();
		NBTTagCompound used = tag.getCompoundTag("RecipesUsed");
		for (Object key : used.func_150296_c()) {
			NBTBase count = used.getTag((String) key);
			if (count instanceof NBTBase.NBTPrimitive primitive) {
				recipesUsed.put((String) key, primitive.func_150287_d());
			}
		}
		customName = tag.hasKey("CustomName", 8) ? tag.getString("CustomName") : null;
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("cooking_time_spent", cookingTimer);
		tag.setInteger("cooking_total_time", cookingTotalTime);
		tag.setInteger("lit_time_remaining", litTimeRemaining);
		tag.setInteger("lit_total_time", litTotalTime);
		tag.setFloat("speed_multiplier", speedMultiplier);
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
		NBTTagCompound used = new NBTTagCompound();
		for (Map.Entry<String, Integer> entry : recipesUsed.entrySet()) {
			used.setInteger(entry.getKey(), entry.getValue());
		}
		tag.setTag("RecipesUsed", used);
		if (customName != null) {
			tag.setString("CustomName", customName);
		}
	}
}
