package io.github.asgerjon.mcfly.mixins.early.elytra;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.elytra.ElytraAnimationState;
import io.github.asgerjon.mcfly.elytra.GlidingPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The gliding player (SPEC-modern-movement.md §17, §21, §85): 26.3's fall flying flag as a bit of 1.7.10's shared flags
 * (`elytraDataWatcherFlag`, 7 as 26.3's), saved as `FallFlying`; the ticks spent gliding, counted right after the living
 * tick as 26.3 counts them; on the client the wings' animation, from the motion at the tick's start. The box and the
 * eyes come from the pose system.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase implements GlidingPlayer {

	@Unique
	private int mcfly$glideTicks;

	@Unique
	private final ElytraAnimationState mcfly$wings = new ElytraAnimationState();

	public MixinEntityPlayer(World world) {
		super(world);
	}

	@Inject(method = "onUpdate", at = @At("HEAD"))
	private void mcfly$motionAtStart(CallbackInfo ci) {
		if (this.worldObj.isRemote) {
			mcfly$wings.startTick((EntityPlayer) (Object) this);
		}
	}

	@Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;onUpdate()V", shift = At.Shift.AFTER))
	private void mcfly$countGlide(CallbackInfo ci) {
		mcfly$glideTicks = etfu$isElytraFlying() ? mcfly$glideTicks + 1 : 0;
		if (this.worldObj.isRemote) {
			mcfly$wings.tick((EntityPlayer) (Object) this);
		}
	}

	@Override
	public boolean etfu$isElytraFlying() {
		return getFlag(McFlyConfig.elytraDataWatcherFlag);
	}

	@Override
	public void etfu$setElytraFlying(boolean flag) {
		setFlag(McFlyConfig.elytraDataWatcherFlag, flag);
	}

	@Override
	public int etfu$getFallFlyingTicks() {
		return mcfly$glideTicks;
	}

	@Override
	public ElytraAnimationState etfu$getElytraAnimation() {
		return mcfly$wings;
	}

	@Inject(method = "writeEntityToNBT", at = @At("TAIL"))
	private void mcfly$saveGlide(NBTTagCompound tag, CallbackInfo ci) {
		tag.setBoolean("FallFlying", etfu$isElytraFlying());
	}

	@Inject(method = "readEntityFromNBT", at = @At("TAIL"))
	private void mcfly$loadGlide(NBTTagCompound tag, CallbackInfo ci) {
		etfu$setElytraFlying(tag.getBoolean("FallFlying"));
	}
}
