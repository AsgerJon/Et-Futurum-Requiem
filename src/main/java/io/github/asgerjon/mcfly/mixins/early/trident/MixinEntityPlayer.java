package io.github.asgerjon.mcfly.mixins.early.trident;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.trident.IRiptidePlayer;
import ganymedes01.etfuturum.trident.Riptide;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Riptide's spin on the player (SPEC-modern-movement.md §58.2): the count of ticks, the check after each move against
 * the space swept (modern's checkAutoSpinAttack), and base damage 8 for the spin's hit.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer implements IRiptidePlayer {

	@Unique
	private int etfu$spinTicks;
	@Unique
	private AxisAlignedBB etfu$beforeMove;

	@Override
	public int etfu$getSpinTicks() {
		return etfu$spinTicks;
	}

	@Override
	public void etfu$setSpinTicks(int ticks) {
		etfu$spinTicks = ticks;
	}

	@Inject(method = "moveEntityWithHeading", at = @At("HEAD"))
	private void etfu$beforeMove(float strafe, float forward, CallbackInfo ci) {
		if (etfu$spinTicks > 0) {
			etfu$beforeMove = ((EntityPlayer) (Object) this).boundingBox.copy();
		}
	}

	@Inject(method = "moveEntityWithHeading", at = @At("RETURN"))
	private void etfu$spinAfterMove(float strafe, float forward, CallbackInfo ci) {
		if (etfu$spinTicks > 0 && etfu$beforeMove != null) {
			Riptide.tick((EntityPlayer) (Object) this, etfu$beforeMove);
		}
	}

	/** Modern's spin hit takes the spin's damage as its base instead of the attack damage stat. */
	@ModifyExpressionValue(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/attributes/IAttributeInstance;getAttributeValue()D", ordinal = 0))
	private double etfu$spinHitBase(double base) {
		return Riptide.attackBase((EntityPlayer) (Object) this, (float) base);
	}
}
