package ganymedes01.etfuturum.mixins.early.potionglint.client;

import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Potion bottles don't glint (modern since 1.9; 1.7.10 gave every potion with an effect the enchantment glint). An
 * actually enchanted stack still glints, as any item does. Client only: hasEffect is a client-side method and does
 * not exist on a dedicated server (the mixin crashed one before it moved here).
 */
@Mixin(ItemPotion.class)
public class MixinItemPotion {

	@Inject(method = "hasEffect(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void etfu$noGlint(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(stack.isItemEnchanted());
	}
}
