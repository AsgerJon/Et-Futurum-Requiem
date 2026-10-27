package ganymedes01.etfuturum.inventory;

import ganymedes01.etfuturum.inventory.slot.SlotSmithingResult;
import ganymedes01.etfuturum.recipes.SmithingTableRecipes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * The smithing table's container: 1.16's two inputs, or, with the netherite upgrade template on, modern's three
 * (template, base, addition) at modern's places, with a result slot the same way. The crafting matrix keeps the tool
 * at 0 and the material at 1 for the recipes and CraftTweaker; the template is index 2.
 */
public class ContainerSmithingTable extends Container {
	public final boolean withTemplate;
	private final World world;
	public final InventoryCrafting inputMatrix;
	private final InventoryCraftResult resultInventory = new InventoryCraftResult();
	/** Null without templates. */
	public final Slot templateSlot;
	public final Slot applicant;
	public final Slot ingot;
	private final Slot result;

	public ContainerSmithingTable(InventoryPlayer inv, World world) {
		this.world = world;
		withTemplate = SmithingTableRecipes.useTemplates();
		inputMatrix = new InventoryCrafting(this, withTemplate ? 3 : 2, 1);
		if (withTemplate) {
			templateSlot = addSlotToContainer(new Slot(inputMatrix, SmithingTableRecipes.TEMPLATE_SLOT, 8, 48) {
				@Override
				public boolean isItemValid(ItemStack stack) {
					return SmithingTableRecipes.getInstance().isTemplate(stack);
				}
			});
			applicant = addSlotToContainer(new Slot(inputMatrix, 0, 26, 48));
			ingot = addSlotToContainer(new Slot(inputMatrix, 1, 44, 48));
			result = addSlotToContainer(new SlotSmithingResult(inv.player, inputMatrix, resultInventory, 0, 98, 48));
		} else {
			templateSlot = null;
			applicant = addSlotToContainer(new Slot(inputMatrix, 0, 27, 47));
			ingot = addSlotToContainer(new Slot(inputMatrix, 1, 76, 47));
			result = addSlotToContainer(new SlotSmithingResult(inv.player, inputMatrix, resultInventory, 2, 134, 47));
		}
		for (int y = 0; y < 3; y++)
			for (int x = 0; x < 9; x++) addSlotToContainer(new Slot(inv, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
		for (int y = 0; y < 9; y++) addSlotToContainer(new Slot(inv, y, 8 + y * 18, 142));
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}

	@Override
	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);
		if (!world.isRemote) {
			for (int i = 0; i < inputMatrix.getSizeInventory(); i++) {
				final ItemStack stack = inputMatrix.getStackInSlotOnClosing(i);
				if (stack != null && !ContainerClose.giveBack(player, stack)) player.dropPlayerItemWithRandomChoice(stack, false);
			}
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		final Object s = inventorySlots.get(index);
		if (!(s instanceof Slot)) return null;
		final Slot slot = (Slot) s;
		if (!slot.getHasStack()) return null;
		final ItemStack newStack = slot.getStack();
		final ItemStack oldStack = newStack.copy();
		final int resultIndex = result.slotNumber;
		final int firstPlayerSlot = resultIndex + 1;
		final int firstHotbarSlot = firstPlayerSlot + 27;
		final int end = firstHotbarSlot + 9;
		boolean merged = false;
		if (index <= resultIndex) {
			merged = mergeItemStack(newStack, firstPlayerSlot, end, true);
		} else if (index < firstHotbarSlot) {
			merged = tryUpper(newStack) || mergeItemStack(newStack, firstHotbarSlot, end, false);
		} else {
			merged = tryUpper(newStack) || mergeItemStack(newStack, firstPlayerSlot, firstHotbarSlot, false);
		}
		if (!merged) return null;
		if (newStack.stackSize == 0) {
			slot.putStack(null);
		} else slot.onSlotChanged();
		slot.onPickupFromSlot(player, newStack);
		return oldStack;
	}

	@Override
	public void onCraftMatrixChanged(IInventory inv) {
		resultInventory.setInventorySlotContents(0, SmithingTableRecipes.getInstance().findMatchingRecipe(this.inputMatrix, this.world));
		super.onCraftMatrixChanged(inv);
	}

	/** A shift-clicked stack goes to the empty input slot it fits: the template slot for a template, else the tool, else the material. */
	private boolean tryUpper(ItemStack newStack) {
		if (templateSlot != null && !templateSlot.getHasStack() && templateSlot.isItemValid(newStack)) {
			return mergeItemStack(newStack, templateSlot.slotNumber, templateSlot.slotNumber + 1, false);
		}
		return !applicant.getHasStack() && mergeItemStack(newStack, applicant.slotNumber, applicant.slotNumber + 1, false)
				|| !ingot.getHasStack() && mergeItemStack(newStack, ingot.slotNumber, ingot.slotNumber + 1, false);
	}

	public boolean hasTemplate() {
		return templateSlot != null && templateSlot.getHasStack();
	}

	/** Inputs that make nothing: modern shows its error once all three slots are filled, 1.16's table as soon as one is. */
	public boolean unable() {
		if (withTemplate) {
			return templateSlot.getHasStack() && applicant.getHasStack() && ingot.getHasStack() && !result.getHasStack();
		}
		return (applicant.getHasStack() || ingot.getHasStack()) && !result.getHasStack();
	}
}
