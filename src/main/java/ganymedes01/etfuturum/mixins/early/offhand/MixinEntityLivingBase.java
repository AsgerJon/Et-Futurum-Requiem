package ganymedes01.etfuturum.mixins.early.offhand;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.IOffhandSwinger;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A swing belongs to a hand (SPEC-modern-movement.md §36.4). 1.7.10 has one swing animation per entity; the hand it
 * belongs to is remembered beside it when the swing (re)starts, as modern's swing state does, and the server tells
 * the trackers with the animation packet's type: 0 is vanilla's main-hand swing, 6 the off-hand's (Offhand explains
 * why not modern's 3).
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase implements IOffhandSwinger {
	@Unique
	private Hand etfu$swingingHand = Hand.MAIN;
	@Unique
	private Hand etfu$nextSwingHand = Hand.MAIN;

	@Override
	public Hand etfu$getSwingingHand() {
		return etfu$swingingHand;
	}

	@Override
	public Hand etfu$getNextSwingHand() {
		return etfu$nextSwingHand;
	}

	@Override
	public void etfu$setNextSwingHand(Hand hand) {
		etfu$nextSwingHand = hand;
	}

	/** The item's own swing hook sees the item of the swinging hand. */
	@ModifyExpressionValue(method = "swingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;getHeldItem()Lnet/minecraft/item/ItemStack;"))
	private ItemStack etfu$itemOfTheSwingingHand(ItemStack held) {
		if (etfu$nextSwingHand == Hand.OFF && (Object) this instanceof EntityPlayer) {
			return Offhand.getOffhandOf((EntityPlayer) (Object) this);
		}
		return held;
	}

	/** The swing (re)started: it belongs to the requested hand from now on. */
	@Inject(method = "swingItem", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;isSwingInProgress:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
	private void etfu$swingStarted(CallbackInfo ci) {
		etfu$swingingHand = etfu$nextSwingHand;
	}

	@Inject(method = "swingItem", at = @At("RETURN"))
	private void etfu$swingRequestDone(CallbackInfo ci) {
		etfu$nextSwingHand = Hand.MAIN;
	}

	/** The trackers learn the hand from the animation type. */
	@ModifyArg(method = "swingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/S0BPacketAnimation;<init>(Lnet/minecraft/entity/Entity;I)V"), index = 1)
	private int etfu$animationTypeOfTheHand(int type) {
		return etfu$swingingHand == Hand.OFF ? Offhand.OFFHAND_SWING_ANIMATION : type;
	}
}
