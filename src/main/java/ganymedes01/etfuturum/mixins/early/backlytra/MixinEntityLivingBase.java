package ganymedes01.etfuturum.mixins.early.backlytra;

import ganymedes01.etfuturum.elytra.FallFlying;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {

	@Shadow
	public float prevLimbSwingAmount;

	@Shadow
	public float limbSwingAmount;

	@Shadow
	public float limbSwing;

	@Shadow
	public abstract boolean isClientWorld();

	public MixinEntityLivingBase(World world) {
		super(world);
	}

	/**
	 * The travel step for gliding players (§18.3, §19), on the side that moves them (the server, and the client for its
	 * own player; isClientWorld is misnamed). Fluid movement goes first; on a ladder the normal movement runs and
	 * gliding stops.
	 */
	@Inject(method = "moveEntityWithHeading", at = @At("HEAD"), cancellable = true)
	private void etfu$fallFlyingTravel(float strafe, float forward, CallbackInfo ci) {
		if (!((Object) this instanceof EntityPlayer) || !this.isClientWorld() || !FallFlying.isFallFlying(this)) {
			return;
		}
		EntityPlayer player = (EntityPlayer) (Object) this;
		FallFlying.beforeTravel(player);
		if (!FallFlying.isFallFlying(player) || this.isInWater() || this.handleLavaMovement()) {
			return;
		}
		if (FallFlying.travel(player)) {
			etfu$updateLimbSwing();
			ci.cancel();
		}
	}

	/**
	 * The walk animation update from the end of moveEntityWithHeading, which the cancelled travel step skips. Modern
	 * keeps updating it while gliding and slows the limbs down when drawing instead (§21.2).
	 */
	@Unique
	private void etfu$updateLimbSwing() {
		this.prevLimbSwingAmount = this.limbSwingAmount;
		double dx = this.posX - this.prevPosX;
		double dz = this.posZ - this.prevPosZ;
		float distance = Math.min(MathHelper.sqrt_double(dx * dx + dz * dz) * 4.0F, 1.0F);
		this.limbSwingAmount += (distance - this.limbSwingAmount) * 0.4F;
		this.limbSwing += this.limbSwingAmount;
	}
}
