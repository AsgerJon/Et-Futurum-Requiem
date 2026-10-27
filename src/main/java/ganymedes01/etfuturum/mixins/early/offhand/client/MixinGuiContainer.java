package ganymedes01.etfuturum.mixins.early.offhand.client;

import ganymedes01.etfuturum.offhand.Offhand;
import ganymedes01.etfuturum.offhand.OffhandClient;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The swap key over a slot of any container screen moves that slot's stack to the off-hand and the off-hand's stack
 * to the slot (SPEC-modern-movement.md §36.1), as the number keys do with the hotbar: it is the same hotbar-key
 * click with the off-hand's index, which the container's click code accepts (offhand.MixinContainer).
 */
@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen {
	@Shadow
	private Slot theSlot;

	@Shadow
	protected abstract void handleMouseClick(Slot slot, int slotId, int button, int mode);

	@Inject(method = "checkHotbarKeys", at = @At("HEAD"), cancellable = true)
	private void etfu$swapKeyToOffhand(int keyCode, CallbackInfoReturnable<Boolean> cir) {
		if (!Offhand.isEnabled() || OffhandClient.keySwap == null || keyCode != OffhandClient.keySwap.getKeyCode()) {
			return;
		}
		if (this.mc.thePlayer.inventory.getItemStack() == null && theSlot != null) {
			handleMouseClick(theSlot, theSlot.slotNumber, Offhand.INVENTORY_INDEX, 2);
			cir.setReturnValue(true);
		}
	}
}
