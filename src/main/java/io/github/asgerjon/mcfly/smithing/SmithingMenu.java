package io.github.asgerjon.mcfly.smithing;

import io.github.asgerjon.mcfly.McFly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * The smithing table's menu (SPEC-modern-movement.md §88.4, step C2), 26.3's `SmithingMenu`: the template at (8, 48),
 * the base at (26, 48), the addition at (44, 48), the result at (98, 48) (without templates, 1.16's: base (27, 47),
 * addition (76, 47), result (134, 47)); each input only takes what some recipe takes there; taking the result takes one
 * of each input and plays `block.smithing_table.use`; shift-clicks sort into the empty input that fits; the inputs come
 * back on closing; usable within 8 blocks of the table.
 */
public class SmithingMenu extends Container {

	public final boolean withTemplate;
	private final World world;
	private final int x, y, z;
	private final IInventory inputs;
	private final InventoryCraftResult result = new InventoryCraftResult();
	/** Null without templates. */
	public final Slot templateSlot;
	public final Slot baseSlot;
	public final Slot additionSlot;
	public final Slot resultSlot;

	public SmithingMenu(InventoryPlayer inventory, World world, int x, int y, int z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.withTemplate = SmithingRecipes.useTemplates();
		this.inputs = new InventoryBasic("smithing", false, 3) {
			@Override
			public void markDirty() {
				super.markDirty();
				onCraftMatrixChanged(this);
			}
		};
		if (withTemplate) {
			templateSlot = addSlotToContainer(new Input(0, 8, 48));
			baseSlot = addSlotToContainer(new Input(1, 26, 48));
			additionSlot = addSlotToContainer(new Input(2, 44, 48));
			resultSlot = addSlotToContainer(new Result(98, 48));
		} else {
			templateSlot = null;
			baseSlot = addSlotToContainer(new Input(1, 27, 47));
			additionSlot = addSlotToContainer(new Input(2, 76, 47));
			resultSlot = addSlotToContainer(new Result(134, 47));
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlotToContainer(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			addSlotToContainer(new Slot(inventory, column, 8 + column * 18, 142));
		}
	}

	/** An input slot: only what some recipe takes there (26.3's property sets). */
	private final class Input extends Slot {
		private final int kind;

		Input(int kind, int x, int y) {
			super(inputs, kind, x, y);
			this.kind = kind;
		}

		@Override
		public boolean isItemValid(ItemStack stack) {
			return kind == 0 ? SmithingRecipes.isTemplate(stack) : kind == 1 ? SmithingRecipes.isBase(stack) : SmithingRecipes.isAddition(stack);
		}
	}

	/** The result: taking it takes one of each input (26.3's `onTake`). */
	private final class Result extends Slot {
		Result(int x, int y) {
			super(result, 0, x, y);
		}

		@Override
		public boolean isItemValid(ItemStack stack) {
			return false;
		}

		@Override
		public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
			stack.onCrafting(world, player, stack.stackSize);
			for (int i = 0; i < 3; i++) {
				inputs.decrStackSize(i, 1);
			}
			if (!world.isRemote) {
				world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":block.smithing_table.use", 1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
			}
			super.onPickupFromSlot(player, stack);
		}
	}

	private ItemStack input(int i) {
		return inputs.getStackInSlot(i);
	}

	@Override
	public void onCraftMatrixChanged(IInventory changed) {
		if (changed == inputs) {
			result.setInventorySlotContents(0, SmithingRecipes.result(withTemplate ? input(0) : null, input(1), input(2)));
		}
		super.onCraftMatrixChanged(changed);
	}

	/** 26.3's `hasRecipeError`: every input filled (1.16's table: any) and nothing made. */
	public boolean unable() {
		boolean filled = withTemplate ? input(0) != null && input(1) != null && input(2) != null : input(1) != null || input(2) != null;
		return filled && result.getStackInSlot(0) == null;
	}

	public boolean hasTemplate() {
		return templateSlot != null && templateSlot.getHasStack();
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return world.getBlock(x, y, z) instanceof SmithingTableBlock && player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) <= 64.0D;
	}

	@Override
	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);
		if (!world.isRemote) {
			for (int i = 0; i < 3; i++) {
				ItemStack stack = inputs.getStackInSlotOnClosing(i);
				if (stack != null && !player.inventory.addItemStackToInventory(stack)) {
					player.dropPlayerItemWithRandomChoice(stack, false);
				}
			}
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		Slot slot = (Slot) inventorySlots.get(index);
		if (slot == null || !slot.getHasStack()) {
			return null;
		}
		ItemStack stack = slot.getStack();
		ItemStack before = stack.copy();
		int firstPlayer = resultSlot.slotNumber + 1;
		int firstHotbar = firstPlayer + 27;
		int end = firstHotbar + 9;
		boolean moved;
		if (index <= resultSlot.slotNumber) {
			moved = mergeItemStack(stack, firstPlayer, end, true);
			if (moved && slot == resultSlot) {
				slot.onSlotChange(stack, before);
			}
		} else {
			moved = intoInputs(stack) || (index < firstHotbar ? mergeItemStack(stack, firstHotbar, end, false) : mergeItemStack(stack, firstPlayer, firstHotbar, false));
		}
		if (!moved) {
			return null;
		}
		if (stack.stackSize == 0) {
			slot.putStack(null);
		} else {
			slot.onSlotChanged();
		}
		slot.onPickupFromSlot(player, stack);
		return before;
	}

	/** 26.3's `canMoveIntoInputSlots`: the template slot for a template, else the base, else the addition, each if empty. */
	private boolean intoInputs(ItemStack stack) {
		for (Slot slot : new Slot[]{templateSlot, baseSlot, additionSlot}) {
			if (slot != null && !slot.getHasStack() && slot.isItemValid(stack) && mergeItemStack(stack, slot.slotNumber, slot.slotNumber + 1, false)) {
				return true;
			}
		}
		return false;
	}
}
