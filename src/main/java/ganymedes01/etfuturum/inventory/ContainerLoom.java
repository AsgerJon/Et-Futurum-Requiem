package ganymedes01.etfuturum.inventory;

import io.github.asgerjon.mcfly.McFly;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.ModernBanners;
import ganymedes01.etfuturum.tileentities.TileEntityBanner.EnumBannerPattern;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

/**
 * The loom's menu, as 26.3's `LoomMenu` (SPEC-modern-movement.md §66.5): a banner, a dye and maybe a pattern item in;
 * the patterns offered; the one picked (1.7.10's container button, the enchanting table's packet) puts one banner with
 * that layer, in the dye's colour, in the result. Taking it uses one banner and one dye.
 */
public class ContainerLoom extends Container {

	private final World world;
	private final int x;
	private final int y;
	private final int z;
	private final InventoryBasic input = new InventoryBasic("loom", false, 3) {
		@Override
		public void markDirty() {
			super.markDirty();
			onCraftMatrixChanged(this);
		}
	};
	private final InventoryCraftResult output = new InventoryCraftResult();
	public final Slot bannerSlot;
	public final Slot dyeSlot;
	public final Slot patternSlot;
	public final Slot resultSlot;
	private List<EnumBannerPattern> selectable = Collections.emptyList();
	private int selected = -1;
	private int sentSelected = -2;
	private long lastSoundTime = -1;
	/** The screen, told when the slots or the pick change (26.3's update listener). */
	private Runnable listener = () -> {
	};

	public ContainerLoom(InventoryPlayer inventory, World world, int x, int y, int z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		bannerSlot = addSlotToContainer(new Slot(input, 0, 13, 26) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return ModernBanners.isBanner(stack);
			}
		});
		dyeSlot = addSlotToContainer(new Slot(input, 1, 33, 26) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return ModernBanners.isDye(stack);
			}
		});
		patternSlot = addSlotToContainer(new Slot(input, 2, 23, 45) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return ModernBanners.isPatternItem(stack);
			}
		});
		resultSlot = addSlotToContainer(new Slot(output, 0, 143, 57) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return false;
			}

			@Override
			public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
				bannerSlot.decrStackSize(1);
				dyeSlot.decrStackSize(1);
				if (!bannerSlot.getHasStack() || !dyeSlot.getHasStack()) {
					selected = -1;
				}
				if (!ContainerLoom.this.world.isRemote) {
					long time = ContainerLoom.this.world.getTotalWorldTime();
					if (lastSoundTime != time) {
						ContainerLoom.this.world.playSoundEffect(ContainerLoom.this.x + 0.5, ContainerLoom.this.y + 0.5,
							ContainerLoom.this.z + 0.5, McFly.MC_ASSETS + ":ui.loom.take_result", 1.0F, 1.0F);
						lastSoundTime = time;
					}
				}
				super.onPickupFromSlot(player, stack);
			}
		});
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlotToContainer(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			addSlotToContainer(new Slot(inventory, column, 8 + column * 18, 142));
		}
	}

	/** The patterns offered now. */
	public List<EnumBannerPattern> selectable() {
		return selectable;
	}

	/** The picked pattern's index in {@link #selectable}, or -1. */
	public int selected() {
		return selected;
	}

	public void setListener(Runnable listener) {
		this.listener = listener;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return world.getBlock(x, y, z) == ModBlocks.LOOM.get() && player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64.0;
	}

	/** A pattern picked (26.3's `clickMenuButton`): the screen calls it on the client, the button's packet on the server. */
	@Override
	public boolean enchantItem(EntityPlayer player, int index) {
		if (index >= 0 && index < selectable.size()) {
			selected = index;
			setupResult(selectable.get(index));
			listener.run();
			return true;
		}
		return false;
	}

	@Override
	public void onCraftMatrixChanged(IInventory inventory) {
		ItemStack banner = bannerSlot.getStack();
		ItemStack dye = dyeSlot.getStack();
		if (banner != null && dye != null) {
			boolean valid = selected >= 0 && selected < selectable.size();
			List<EnumBannerPattern> before = selectable;
			selectable = ModernBanners.selectable(patternSlot.getStack());
			EnumBannerPattern shown;
			if (selectable.size() == 1) {
				selected = 0;
				shown = selectable.get(0);
			} else if (!valid) {
				selected = -1;
				shown = null;
			} else {
				EnumBannerPattern was = before.get(selected);
				int now = selectable.indexOf(was);
				selected = now;
				shown = now != -1 ? was : null;
			}
			if (shown == null) {
				output.setInventorySlotContents(0, null);
			} else if (ModernBanners.layers(banner) >= ModernBanners.MAX_LAYERS) {
				selected = -1;
				output.setInventorySlotContents(0, null);
			} else {
				setupResult(shown);
			}
			detectAndSendChanges();
		} else {
			output.setInventorySlotContents(0, null);
			selectable = Collections.emptyList();
			selected = -1;
		}
		listener.run();
	}

	private void setupResult(EnumBannerPattern pattern) {
		ItemStack banner = bannerSlot.getStack();
		ItemStack dye = dyeSlot.getStack();
		ItemStack result = null;
		if (banner != null && dye != null && ModernBanners.layers(banner) < ModernBanners.MAX_LAYERS) {
			int colour = ModernBanners.dyeColour(dye);
			if (colour >= 0) {
				result = ModernBanners.withLayer(banner, pattern, colour);
			}
		}
		if (!ItemStack.areItemStacksEqual(result, output.getStackInSlot(0))) {
			output.setInventorySlotContents(0, result);
		}
	}

	// ---------------------------------------------------------------- the pick, synced as a progress bar

	@Override
	public void addCraftingToCrafters(ICrafting crafting) {
		super.addCraftingToCrafters(crafting);
		crafting.sendProgressBarUpdate(this, 0, selected);
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		if (sentSelected != selected) {
			for (Object crafting : crafters) {
				((ICrafting) crafting).sendProgressBarUpdate(this, 0, selected);
			}
			sentSelected = selected;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void updateProgressBar(int id, int value) {
		if (id == 0) {
			selected = value;
			listener.run();
		}
	}

	// ---------------------------------------------------------------- shift-clicks and closing

	/** 26.3's `quickMoveStack`: the result into the inventory; a banner, dye or pattern item into its slot; else across. */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		Slot slot = (Slot) inventorySlots.get(index);
		if (slot == null || !slot.getHasStack()) {
			return null;
		}
		ItemStack stack = slot.getStack();
		ItemStack clicked = stack.copy();
		int firstPlayer = 4;
		int firstHotbar = 31;
		int end = 40;
		if (index == resultSlot.slotNumber) {
			if (!mergeItemStack(stack, firstPlayer, end, true)) {
				return null;
			}
			slot.onSlotChange(stack, clicked);
		} else if (index != dyeSlot.slotNumber && index != bannerSlot.slotNumber && index != patternSlot.slotNumber) {
			if (ModernBanners.isBanner(stack)) {
				if (!mergeItemStack(stack, bannerSlot.slotNumber, bannerSlot.slotNumber + 1, false)) {
					return null;
				}
			} else if (ModernBanners.isDye(stack)) {
				if (!mergeItemStack(stack, dyeSlot.slotNumber, dyeSlot.slotNumber + 1, false)) {
					return null;
				}
			} else if (ModernBanners.isPatternItem(stack)) {
				if (!mergeItemStack(stack, patternSlot.slotNumber, patternSlot.slotNumber + 1, false)) {
					return null;
				}
			} else if (index < firstHotbar) {
				if (!mergeItemStack(stack, firstHotbar, end, false)) {
					return null;
				}
			} else if (!mergeItemStack(stack, firstPlayer, firstHotbar, false)) {
				return null;
			}
		} else if (!mergeItemStack(stack, firstPlayer, end, false)) {
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
		return clicked;
	}

	@Override
	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);
		if (!world.isRemote) {
			for (int i = 0; i < input.getSizeInventory(); i++) {
				ItemStack stack = input.getStackInSlotOnClosing(i);
				if (stack != null && !ContainerClose.giveBack(player, stack)) {
					player.dropPlayerItemWithRandomChoice(stack, false);
				}
			}
		}
	}
}
