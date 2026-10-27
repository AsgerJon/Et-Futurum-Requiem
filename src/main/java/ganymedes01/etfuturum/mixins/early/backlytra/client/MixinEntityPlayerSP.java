package ganymedes01.etfuturum.mixins.early.backlytra.client;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.client.sound.ElytraSound;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.elytra.FallFlying;
import ganymedes01.etfuturum.network.StartElytraFlyingMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovementInput;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every injection into onLivingUpdate also names localOnLivingUpdate: Player API moves the body of onLivingUpdate
 * there and leaves a dispatcher without the injection points under the original name (see
 * modernmovement.client.MixinEntityPlayerSP).
 */
@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

	@Shadow
	public MovementInput movementInput;

	@Shadow
	protected Minecraft mc;

	@Unique
	private boolean etfu$wasJumping;

	@Unique
	private boolean etfu$justToggledFlight;

	@Unique
	private boolean etfu$wasFallFlying;

	@Dynamic("Player API moves the body of onLivingUpdate to localOnLivingUpdate")
	@Inject(method = {"onLivingUpdate", "localOnLivingUpdate"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V"))
	private void etfu$beforeInput(CallbackInfo ci) {
		etfu$wasJumping = this.movementInput.jump;
		etfu$justToggledFlight = false;
	}

	/**
	 * The double-tap that toggles creative flight (ordinal 0; ordinal 1 is landing, which turns flight off).
	 */
	@Dynamic("Player API moves the body of onLivingUpdate to localOnLivingUpdate")
	@Inject(method = {"onLivingUpdate", "localOnLivingUpdate"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;sendPlayerAbilities()V", ordinal = 0))
	private void etfu$flightToggled(CallbackInfo ci) {
		etfu$justToggledFlight = true;
	}

	/**
	 * Right after the creative flight toggle (§18.2): a fresh jump press in the air starts gliding here and asks the
	 * server, unless that press toggled flight or the player is on a ladder. The gliding sound starts when the flag
	 * turns on, from here or from the server (§21.5).
	 */
	@Dynamic("Player API moves the body of onLivingUpdate to localOnLivingUpdate")
	@Inject(method = {"onLivingUpdate", "localOnLivingUpdate"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isRidingHorse()Z", ordinal = 0))
	private void etfu$startFallFlying(CallbackInfo ci) {
		if (Utils.badBetterFPSAlgorithm()) {
			return;
		}
		EntityPlayerSP player = (EntityPlayerSP) (Object) this;
		if (this.movementInput.jump && !etfu$wasJumping && !etfu$justToggledFlight && !player.isOnLadder()
				&& FallFlying.tryToStart(player)) {
			EtFuturum.networkWrapper.sendToServer(new StartElytraFlyingMessage());
		}
		boolean fallFlying = FallFlying.isFallFlying(player);
		if (fallFlying && !etfu$wasFallFlying) {
			mc.getSoundHandler().playSound(new ElytraSound(player));
		}
		etfu$wasFallFlying = fallFlying;
	}
}
