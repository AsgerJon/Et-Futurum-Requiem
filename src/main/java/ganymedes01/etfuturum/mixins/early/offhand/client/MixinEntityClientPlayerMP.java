package ganymedes01.etfuturum.mixins.early.offhand.client;

import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.IOffhandSwinger;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.client.entity.EntityClientPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The local player's swing request carries the hand (SPEC-modern-movement.md §36.4): 1 is vanilla's main-hand
 * swing, 2 the off-hand's. The hand is read before the swing itself consumes it.
 */
@Mixin(EntityClientPlayerMP.class)
public class MixinEntityClientPlayerMP {
	@Unique
	private Hand etfu$requestedHand = Hand.MAIN;

	@Inject(method = "swingItem", at = @At("HEAD"))
	private void etfu$rememberRequestedHand(CallbackInfo ci) {
		etfu$requestedHand = ((IOffhandSwinger) this).etfu$getNextSwingHand();
	}

	@ModifyArg(method = "swingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/client/C0APacketAnimation;<init>(Lnet/minecraft/entity/Entity;I)V"), index = 1)
	private int etfu$requestTypeOfTheHand(int type) {
		return Offhand.isEnabled() && etfu$requestedHand == Hand.OFF ? Offhand.OFFHAND_SWING_REQUEST : type;
	}
}
