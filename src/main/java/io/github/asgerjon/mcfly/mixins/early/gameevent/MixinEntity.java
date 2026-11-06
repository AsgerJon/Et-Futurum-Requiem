package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An entity's own game events (SPEC-modern-movement.md §90.9, step H1), where 26.3's `Entity` posts them: `step` or
 * `swim` when 1.7.10's `moveEntity` takes a step (it moved `nextStepDistance` on, 1.7.10's or McFly's step sounds),
 * `flap` for a flapping one moving with air under it (26.3's `processFlappingMovement`), `hit_ground` landing with any
 * fall distance (`updateFallState`, after a living entity's `onFallenUpon` as 26.3's
 * `fallOn`), `splash` into water (where 1.7.10 plays the splash), `entity_mount` and `entity_dismount`.
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

	@Shadow
	public World worldObj;
	@Shadow
	public Entity ridingEntity;
	@Shadow
	public float fallDistance;
	@Shadow
	private int nextStepDistance;

	@Unique
	private int mcfly$stepBefore;
	@Unique
	private Entity mcfly$ridingBefore;
	@Unique
	private final double[] mcfly$from = new double[3];

	@Inject(method = "moveEntity", at = @At("HEAD"))
	private void mcfly$beforeMove(double x, double y, double z, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		mcfly$stepBefore = nextStepDistance;
		mcfly$from[0] = self.posX;
		mcfly$from[1] = self.posY;
		mcfly$from[2] = self.posZ;
	}

	@Inject(method = "moveEntity", at = @At("RETURN"))
	private void mcfly$afterMove(double x, double y, double z, CallbackInfo ci) {
		if (worldObj.isRemote) {
			return;
		}
		Entity self = (Entity) (Object) this;
		if (nextStepDistance != mcfly$stepBefore) {
			GameEventHooks.stepped(self, y);
		} else if (self instanceof io.github.asgerjon.mcfly.gameevent.Flapping) {
			double dx = self.posX - mcfly$from[0], dy = self.posY - mcfly$from[1], dz = self.posZ - mcfly$from[2];
			GameEventHooks.flew(self, (float) (Math.sqrt(dx * dx + dy * dy + dz * dz) * 0.6D));
		}
	}

	@Inject(method = "updateFallState", at = @At("HEAD"))
	private void mcfly$landing(double fallen, boolean onGround, CallbackInfo ci) {
		if (onGround && fallDistance > 0.0F) {
			GameEventHooks.landed((Entity) (Object) this);
		}
	}

	@Inject(method = "handleWaterMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getSplashSound()Ljava/lang/String;"))
	private void mcfly$splash(CallbackInfoReturnable<Boolean> cir) {
		GameEventHooks.splashed((Entity) (Object) this);
	}

	@Inject(method = "mountEntity", at = @At("HEAD"))
	private void mcfly$beforeRiding(Entity vehicle, CallbackInfo ci) {
		mcfly$ridingBefore = ridingEntity;
	}

	@Inject(method = "mountEntity", at = @At("RETURN"))
	private void mcfly$afterRiding(Entity vehicle, CallbackInfo ci) {
		Entity before = mcfly$ridingBefore;
		mcfly$ridingBefore = null;
		GameEventHooks.rode((Entity) (Object) this, before, ridingEntity);
	}
}
