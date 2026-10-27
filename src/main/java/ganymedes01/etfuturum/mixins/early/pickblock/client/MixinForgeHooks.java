package ganymedes01.etfuturum.mixins.early.pickblock.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pick block from the whole inventory (SPEC-modern-movement.md §32, modern since 1.8): a block that is only in the
 * main inventory is swapped into the hotbar through an ordinary hotbar-swap click, which the server performs too.
 * When the block is in the hotbar, or nowhere, Forge's own code runs as before.
 */
@Mixin(value = ForgeHooks.class, remap = false)
public class MixinForgeHooks {

	@Inject(method = "onPickBlock", at = @At("HEAD"), cancellable = true)
	private static void etfu$pickFromInventory(MovingObjectPosition target, EntityPlayer player, World world, CallbackInfoReturnable<Boolean> cir) {
		if (target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
			return;
		}
		Block block = world.getBlock(target.blockX, target.blockY, target.blockZ);
		if (block.isAir(world, target.blockX, target.blockY, target.blockZ)) {
			return;
		}
		ItemStack result = block.getPickBlock(target, world, target.blockX, target.blockY, target.blockZ, player);
		if (result == null) {
			return;
		}
		InventoryPlayer inventory = player.inventory;
		int found = -1;
		for (int i = 0; i < inventory.mainInventory.length; i++) {
			ItemStack stack = inventory.mainInventory[i];
			if (stack != null && stack.isItemEqual(result) && ItemStack.areItemStackTagsEqual(stack, result)) {
				found = i;
				break;
			}
		}
		if (found < 9) {
			return;
		}
		int hotbar = etfu$suitableHotbarSlot(inventory);
		Minecraft.getMinecraft().playerController.windowClick(player.inventoryContainer.windowId, found, hotbar, 2, player);
		inventory.currentItem = hotbar;
		cir.setReturnValue(true);
	}

	/**
	 * Modern's choice of hotbar slot for the swap: the first empty one from the selected slot on, else the first
	 * without enchantments, else the selected slot.
	 */
	@Unique
	private static int etfu$suitableHotbarSlot(InventoryPlayer inventory) {
		for (int i = 0; i < 9; i++) {
			int slot = (inventory.currentItem + i) % 9;
			if (inventory.mainInventory[slot] == null) {
				return slot;
			}
		}
		for (int i = 0; i < 9; i++) {
			int slot = (inventory.currentItem + i) % 9;
			if (!inventory.mainInventory[slot].isItemEnchanted()) {
				return slot;
			}
		}
		return inventory.currentItem;
	}
}
