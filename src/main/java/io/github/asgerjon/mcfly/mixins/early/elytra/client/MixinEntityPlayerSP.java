package io.github.asgerjon.mcfly.mixins.early.elytra.client;

import io.github.asgerjon.mcfly.elytra.FallFlying;
import io.github.asgerjon.mcfly.elytra.StartGlidingMessage;
import io.github.asgerjon.mcfly.elytra.client.ElytraFlightSound;
import io.github.asgerjon.mcfly.network.McFlyNetwork;
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
 * The client's takeoff (SPEC-modern-movement.md §18.2, §21.5, §85), as 26.3's `LocalPlayer.aiStep`: after the creative
 * flight toggle, a fresh press of jump in the air (not the one that toggled flight, not on a ladder) starts the glide
 * here and tells the server; the flight sound starts when gliding begins, from here or from the server. Each injection
 * names `localOnLivingUpdate` too: Player API moves `onLivingUpdate`'s body there.
 */
@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

	@Shadow
	public MovementInput movementInput;

	@Shadow
	protected Minecraft mc;

	@Unique
	private boolean mcfly$wasJumping;

	@Unique
	private boolean mcfly$toggledFlight;

	@Unique
	private boolean mcfly$wasGliding;

	@Dynamic("Player API moves the body of onLivingUpdate to localOnLivingUpdate")
	@Inject(method = {"onLivingUpdate", "localOnLivingUpdate"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V"))
	private void mcfly$beforeInput(CallbackInfo ci) {
		mcfly$wasJumping = this.movementInput.jump;
		mcfly$toggledFlight = false;
	}

	/** The double tap that toggles creative flight (ordinal 0; 1 is landing, which turns flight off). */
	@Dynamic("Player API moves the body of onLivingUpdate to localOnLivingUpdate")
	@Inject(method = {"onLivingUpdate", "localOnLivingUpdate"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;sendPlayerAbilities()V", ordinal = 0))
	private void mcfly$flightToggled(CallbackInfo ci) {
		mcfly$toggledFlight = true;
	}

	@Dynamic("Player API moves the body of onLivingUpdate to localOnLivingUpdate")
	@Inject(method = {"onLivingUpdate", "localOnLivingUpdate"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isRidingHorse()Z", ordinal = 0))
	private void mcfly$takeOff(CallbackInfo ci) {
		EntityPlayerSP player = (EntityPlayerSP) (Object) this;
		if (this.movementInput.jump && !mcfly$wasJumping && !mcfly$toggledFlight && !player.isOnLadder() && FallFlying.tryToStart(player)) {
			McFlyNetwork.CHANNEL.sendToServer(new StartGlidingMessage());
		}
		boolean gliding = FallFlying.isFallFlying(player);
		if (gliding && !mcfly$wasGliding) {
			mc.getSoundHandler().playSound(new ElytraFlightSound(player));
		}
		mcfly$wasGliding = gliding;
	}
}
