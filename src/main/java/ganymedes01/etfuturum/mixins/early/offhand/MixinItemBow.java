package ganymedes01.etfuturum.mixins.early.offhand;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** A bow takes its arrows from the off-hand first (SPEC-modern-movement.md §36.2), then the inventory as before. */
@Mixin(ItemBow.class)
public class MixinItemBow {

	@ModifyExpressionValue(method = {"onPlayerStoppedUsing", "onItemRightClick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;hasItem(Lnet/minecraft/item/Item;)Z"))
	private boolean etfu$arrowsInTheOffhandCount(boolean has, @Local(argsOnly = true) EntityPlayer player) {
		return has || etfu$holdsArrows(Offhand.getOffhand(player));
	}

	@WrapOperation(method = "onPlayerStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;consumeInventoryItem(Lnet/minecraft/item/Item;)Z"))
	private boolean etfu$offhandArrowsFirst(InventoryPlayer inventory, Item item, Operation<Boolean> original) {
		ItemStack offhand = Offhand.getOffhand(inventory.player);
		if (item == Items.arrow && etfu$holdsArrows(offhand)) {
			if (--offhand.stackSize <= 0) {
				Offhand.setOffhand(inventory.player, null);
			}
			return true;
		}
		return original.call(inventory, item);
	}

	@Unique
	private static boolean etfu$holdsArrows(ItemStack stack) {
		return stack != null && stack.getItem() == Items.arrow && stack.stackSize > 0;
	}
}
