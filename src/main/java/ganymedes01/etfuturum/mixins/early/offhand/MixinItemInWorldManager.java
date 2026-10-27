package ganymedes01.etfuturum.mixins.early.offhand;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.ItemInWorldManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sneaking with an item in either hand skips the block's own use (SPEC-modern-movement.md §36.2); 1.7.10 only looked
 * at the main hand. The sneak rule's two reads of the held item see the off-hand item when the main hand is empty.
 */
@Mixin(ItemInWorldManager.class)
public class MixinItemInWorldManager {

	@ModifyExpressionValue(method = "activateBlockOrUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;getHeldItem()Lnet/minecraft/item/ItemStack;"))
	private ItemStack etfu$eitherHandForSneaking(ItemStack held, @Local(argsOnly = true) EntityPlayer player) {
		return held != null || !Offhand.isEnabled() ? held : Offhand.getOffhand(player);
	}
}
