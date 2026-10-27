package ganymedes01.etfuturum.mixins.early.offhand.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.IOffhandPlayer;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Other players' item use on this client (SPEC-modern-movement.md §36.6): 1.7.10 starts a local use of their held
 * item while the synced "eating" flag is on, so the use pose shows. With the synced using hand on the off-hand, the
 * use is the off-hand's synced stack instead and is marked as the off-hand's, so the pose lands on the right arm.
 * The end of the use is vanilla's: the flag goes off, the use is cleared.
 */
@Mixin(EntityOtherPlayerMP.class)
public abstract class MixinEntityOtherPlayerMP {
	@Shadow
	private boolean isItemInUse;

	/** Vanilla's start, with the held item, only applies to a main-hand use. */
	@ModifyExpressionValue(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityOtherPlayerMP;isEating()Z", ordinal = 0))
	private boolean etfu$mainHandUseOnly(boolean eating) {
		return eating && !etfu$usesOffhand();
	}

	@Inject(method = "onUpdate", at = @At("TAIL"))
	private void etfu$startOffhandUse(CallbackInfo ci) {
		EntityOtherPlayerMP self = (EntityOtherPlayerMP) (Object) this;
		if (isItemInUse || !self.isEating() || !etfu$usesOffhand()) {
			return;
		}
		ItemStack stack = Offhand.getOffhandOf(self);
		if (stack != null) {
			self.setItemInUse(stack, stack.getItem().getMaxItemUseDuration(stack));
			((IOffhandPlayer) self).etfu$setUsingHand(Hand.OFF);
			isItemInUse = true;
		}
	}

	@org.spongepowered.asm.mixin.Unique
	private boolean etfu$usesOffhand() {
		return Offhand.isEnabled() && ((IOffhandPlayer) this).etfu$getUsingHand() == Hand.OFF;
	}
}
