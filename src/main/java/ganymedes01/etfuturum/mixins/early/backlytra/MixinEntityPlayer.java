package ganymedes01.etfuturum.mixins.early.backlytra;

import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.elytra.ElytraAnimationState;
import ganymedes01.etfuturum.elytra.IElytraPlayer;
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
 * The fall flying flag (a shared entity flag, synced and saved as "FallFlying"), the gliding tick counter and the
 * client-side wing animation (§17, §21). The hitbox and eye height come from the pose system.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase implements IElytraPlayer {

	@Unique
	private int etfu$fallFlyingTicks;

	@Unique
	private final ElytraAnimationState etfu$elytraAnimation = new ElytraAnimationState();

	public MixinEntityPlayer(World world) {
		super(world);
	}

	@Inject(method = "onUpdate", at = @At("HEAD"))
	private void etfu$rememberMotion(CallbackInfo ci) {
		if (this.worldObj.isRemote) {
			etfu$elytraAnimation.startTick((EntityPlayer) (Object) this);
		}
	}

	/**
	 * Right after the living-entity tick (movement included), like modern: count the gliding ticks and ease the wings.
	 */
	@Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;onUpdate()V", shift = At.Shift.AFTER))
	private void etfu$tickFallFlying(CallbackInfo ci) {
		if (etfu$isElytraFlying()) {
			etfu$fallFlyingTicks++;
		} else {
			etfu$fallFlyingTicks = 0;
		}
		if (this.worldObj.isRemote) {
			etfu$elytraAnimation.tick((EntityPlayer) (Object) this);
		}
	}

	@Override
	public boolean etfu$isElytraFlying() {
		return getFlag(ConfigFunctions.elytraDataWatcherFlag);
	}

	@Override
	public void etfu$setElytraFlying(boolean flag) {
		setFlag(ConfigFunctions.elytraDataWatcherFlag, flag);
	}

	@Override
	public int etfu$getFallFlyingTicks() {
		return etfu$fallFlyingTicks;
	}

	@Override
	public ElytraAnimationState etfu$getElytraAnimation() {
		return etfu$elytraAnimation;
	}

	@Inject(method = "writeEntityToNBT", at = @At("TAIL"))
	private void etfu$writeFallFlying(NBTTagCompound tagCompound, CallbackInfo ci) {
		tagCompound.setBoolean("FallFlying", etfu$isElytraFlying());
	}

	@Inject(method = "readEntityFromNBT", at = @At("TAIL"))
	private void etfu$readFallFlying(NBTTagCompound tagCompound, CallbackInfo ci) {
		etfu$setElytraFlying(tagCompound.getBoolean("FallFlying"));
	}
}
