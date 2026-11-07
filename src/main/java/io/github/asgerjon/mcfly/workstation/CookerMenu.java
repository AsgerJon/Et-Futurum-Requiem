package io.github.asgerjon.mcfly.workstation;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.AchievementList;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.MathHelper;

/**
 * A smoker's or blast furnace's menu (SPEC-modern-movement.md 92.3; 26.3's `AbstractFurnaceMenu`): 1.7.10's furnace
 * slots, 26.3's shift-click (to the input when a recipe takes it, to the fuel when it burns, else across), the result
 * slot paying out the kept experience; its four numbers (lit time, its total, the cook, its total) sent to the client.
 */
public class CookerMenu extends Container {

	private final CookerTile cooker;
	/** The client's copies of the four numbers. */
	private final int[] data = new int[4];
	private final int[] sent = {-1, -1, -1, -1};

	public CookerMenu(InventoryPlayer playerInventory, CookerTile cooker) {
		this.cooker = cooker;
		addSlotToContainer(new Slot(cooker, 0, 56, 17));
		addSlotToContainer(new Slot(cooker, 1, 56, 53) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return cooker.isItemValidForSlot(1, stack);
			}

			/** 26.3's `FurnaceFuelSlot`: one bucket at a time (1.7.10's slot knows only the item held, not one passed). */
			@Override
			public int getSlotStackLimit() {
				ItemStack held = playerInventory.getItemStack();
				return held != null && held.getItem() == Items.bucket ? 1 : super.getSlotStackLimit();
			}
		});
		addSlotToContainer(new ResultSlot(playerInventory.player, cooker, 2, 116, 35));
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 142));
		}
	}

	public CookerTile cooker() {
		return cooker;
	}

	private int value(int id) {
		return switch (id) {
			case 0 -> cooker.litTimeRemaining;
			case 1 -> cooker.litTotalTime;
			case 2 -> cooker.cookingTimer;
			default -> cooker.cookingTotalTime;
		};
	}

	@Override
	public void addCraftingToCrafters(ICrafting crafter) {
		super.addCraftingToCrafters(crafter);
		for (int id = 0; id < 4; id++) {
			crafter.sendProgressBarUpdate(this, id, value(id));
		}
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		for (int id = 0; id < 4; id++) {
			int now = value(id);
			if (now != sent[id]) {
				for (Object crafter : crafters) {
					((ICrafting) crafter).sendProgressBarUpdate(this, id, now);
				}
				sent[id] = now;
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void updateProgressBar(int id, int value) {
		if (id >= 0 && id < 4) {
			data[id] = value;
		}
	}

	/** 26.3's `getBurnProgress`. */
	@SideOnly(Side.CLIENT)
	public float burnProgress() {
		return data[3] != 0 && data[2] != 0 ? MathHelper.clamp_float((float) data[2] / data[3], 0.0F, 1.0F) : 0.0F;
	}

	/** 26.3's `getLitProgress`: a fuel of no total counts 200. */
	@SideOnly(Side.CLIENT)
	public float litProgress() {
		int total = data[1] == 0 ? 200 : data[1];
		return MathHelper.clamp_float((float) data[0] / total, 0.0F, 1.0F);
	}

	@SideOnly(Side.CLIENT)
	public boolean isLit() {
		return data[0] > 0;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return cooker.isUseableByPlayer(player);
	}

	/** 26.3's `quickMoveStack`. */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		ItemStack clicked = null;
		Slot slot = (Slot) inventorySlots.get(index);
		if (slot != null && slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			clicked = stack.copy();
			if (index == 2) {
				if (!mergeItemStack(stack, 3, 39, true)) {
					return null;
				}
				slot.onSlotChange(stack, clicked);
			} else if (index != 1 && index != 0) {
				if (CookingRecipes.find(cooker.kind(), stack) != null) {
					if (!mergeItemStack(stack, 0, 1, false)) {
						return null;
					}
				} else if (TileEntityFurnace.isItemFuel(stack)) {
					if (!mergeItemStack(stack, 1, 2, false)) {
						return null;
					}
				} else if (index < 30) {
					if (!mergeItemStack(stack, 30, 39, false)) {
						return null;
					}
				} else if (index < 39 && !mergeItemStack(stack, 3, 30, false)) {
					return null;
				}
			} else if (!mergeItemStack(stack, 3, 39, false)) {
				return null;
			}
			if (stack.stackSize == 0) {
				slot.putStack(null);
			} else {
				slot.onSlotChanged();
			}
			if (stack.stackSize == clicked.stackSize) {
				return null;
			}
			slot.onPickupFromSlot(player, stack);
		}
		return clicked;
	}

	/** 26.3's `FurnaceResultSlot`: nothing goes in; taking pays out every recipe's kept experience (and 1.7.10's achievements). */
	private static final class ResultSlot extends Slot {

		private final EntityPlayer player;
		private int removed;

		ResultSlot(EntityPlayer player, CookerTile cooker, int index, int x, int y) {
			super(cooker, index, x, y);
			this.player = player;
		}

		@Override
		public boolean isItemValid(ItemStack stack) {
			return false;
		}

		@Override
		public ItemStack decrStackSize(int count) {
			if (getHasStack()) {
				removed += Math.min(count, getStack().stackSize);
			}
			return super.decrStackSize(count);
		}

		@Override
		public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
			onCrafting(stack);
			super.onPickupFromSlot(player, stack);
		}

		@Override
		protected void onCrafting(ItemStack stack, int count) {
			removed += count;
			onCrafting(stack);
		}

		@Override
		protected void onCrafting(ItemStack stack) {
			stack.onCrafting(player.worldObj, player, removed);
			if (!player.worldObj.isRemote) {
				((CookerTile) inventory).popExperience(player.worldObj, player.posX, player.posY + 0.5D, player.posZ);
			}
			removed = 0;
			FMLCommonHandler.instance().firePlayerSmeltedEvent(player, stack);
			if (stack.getItem() == Items.iron_ingot) {
				player.addStat(AchievementList.acquireIron, 1);
			}
			if (stack.getItem() == Items.cooked_fished) {
				player.addStat(AchievementList.cookFish, 1);
			}
		}
	}
}
