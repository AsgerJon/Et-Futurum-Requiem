package ganymedes01.etfuturum.mixins.early.offhand.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A use-dependent icon belongs to the stack in use (SPEC-modern-movement.md §36.5): the bow's pull frames show only
 * on the bow being drawn, as modern's "pulling" predicate asks for the use item to be that very stack. 1.7.10 only
 * asked whether any item was in use, the same question while one hand held one item, so a bow in the off-hand showed
 * fully drawn whenever the main hand ate or drank. Client only: getItemIcon exists only there.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayerIcon {
	@Shadow
	private ItemStack itemInUse;

	@ModifyExpressionValue(method = "getItemIcon", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;itemInUse:Lnet/minecraft/item/ItemStack;"))
	private ItemStack etfu$useItemOnlyForTheStackInUse(ItemStack inUse, @Local(argsOnly = true) ItemStack stack) {
		return Offhand.isEnabled() && inUse != stack ? null : inUse;
	}

	@ModifyExpressionValue(method = "getItemIcon", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;itemInUseCount:I"))
	private int etfu$useCountOnlyForTheStackInUse(int count, @Local(argsOnly = true) ItemStack stack) {
		return Offhand.isEnabled() && this.itemInUse != stack ? 0 : count;
	}
}
