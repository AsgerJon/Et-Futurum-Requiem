package ganymedes01.etfuturum.mixins.early.offhand;

import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The off-hand slot of the inventory screen (SPEC-modern-movement.md §36.1): container slot 45, inventory index
 * 40, at modern's place under the crafting grid. Shift-clicking it already lands in the inventory through the
 * container's last branch, and nothing shift-clicks into it.
 */
@Mixin(ContainerPlayer.class)
public abstract class MixinContainerPlayer extends Container {

	@Inject(method = "<init>", at = @At("TAIL"))
	private void etfu$addOffhandSlot(InventoryPlayer inventory, boolean local, EntityPlayer player, CallbackInfo ci) {
		if (Offhand.isEnabled()) {
			addSlotToContainer(new Slot(inventory, Offhand.INVENTORY_INDEX, Offhand.SCREEN_X, Offhand.SCREEN_Y));
		}
	}
}
