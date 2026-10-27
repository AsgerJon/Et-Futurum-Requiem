package ganymedes01.etfuturum.mixins.early.offhand.client;

import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The off-hand slot in the creative survival tab (SPEC-modern-movement.md §36.1): the tab copies the player's
 * container slots and places them itself, which would put slot 45 on top of the first hotbar slot; it goes to modern's
 * place between the armour columns instead, with a frame drawn the same way as in the inventory screen.
 */
@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreative extends InventoryEffectRenderer {

	@Shadow
	private static int selectedTabIndex;
	@Shadow
	private static InventoryBasic field_147060_v;

	public MixinGuiContainerCreative(Container container) {
		super(container);
	}

	@Inject(method = "setCurrentCreativeTab", at = @At("TAIL"))
	private void etfu$placeOffhandSlot(CreativeTabs tab, CallbackInfo ci) {
		if (!Offhand.isEnabled() || tab != CreativeTabs.tabInventory || this.inventorySlots.inventorySlots.size() <= Offhand.CONTAINER_SLOT) {
			return;
		}
		Slot slot = (Slot) this.inventorySlots.inventorySlots.get(Offhand.CONTAINER_SLOT);
		slot.xDisplayPosition = Offhand.CREATIVE_X;
		slot.yDisplayPosition = Offhand.CREATIVE_Y;
	}

	/**
	 * The swap key over an item of a tab's list puts a full stack of it into the off-hand (§36.1), as the number keys
	 * do for the hotbar; the player container's own crafter sync sends the slot to the server. The survival tab's
	 * slots are the player container's and take the normal path (offhand.MixinContainer).
	 */
	@Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true)
	private void etfu$swapKeyToOffhandFromTab(Slot slot, int slotId, int button, int mode, CallbackInfo ci) {
		if (!Offhand.isEnabled() || mode != 2 || button != Offhand.INVENTORY_INDEX || slot == null || slot.inventory != field_147060_v || !slot.getHasStack()) {
			return;
		}
		ItemStack stack = slot.getStack().copy();
		stack.stackSize = stack.getMaxStackSize();
		this.mc.thePlayer.inventory.setInventorySlotContents(Offhand.INVENTORY_INDEX, stack);
		this.mc.thePlayer.inventoryContainer.detectAndSendChanges();
		ci.cancel();
	}

	@Inject(method = "drawGuiContainerBackgroundLayer", at = @At("TAIL"))
	private void etfu$offhandSlotFrame(float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
		if (!Offhand.isEnabled() || selectedTabIndex != CreativeTabs.tabInventory.getTabIndex()) {
			return;
		}
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(GuiContainer.field_147001_a);
		drawTexturedModalRect(this.guiLeft + Offhand.CREATIVE_X - 1, this.guiTop + Offhand.CREATIVE_Y - 1, 7, 7, 18, 18);
	}
}
