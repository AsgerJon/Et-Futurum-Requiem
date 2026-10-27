package ganymedes01.etfuturum.mixins.early.offhand;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The hotbar-key click (mode 2: swap the clicked slot with a player inventory index) also takes the off-hand's index
 * 40, as modern's swap click does (SPEC-modern-movement.md §36.1). Vanilla only allowed the nine hotbar indices; the
 * swap itself goes through the inventory's slot methods, which know the off-hand.
 */
@Mixin(Container.class)
public class MixinContainer {
	@ModifyExpressionValue(method = "slotClick", at = @At(value = "CONSTANT", args = "intValue=9"))
	private int etfu$offhandIsAHotbarKeyTarget(int hotbarSize, @Local(argsOnly = true, ordinal = 1) int clickedButton) {
		return Offhand.isEnabled() && clickedButton == Offhand.INVENTORY_INDEX ? Offhand.INVENTORY_INDEX + 1 : hotbarSize;
	}
}
