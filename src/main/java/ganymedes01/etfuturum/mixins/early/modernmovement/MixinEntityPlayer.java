package ganymedes01.etfuturum.mixins.early.modernmovement;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.movement.IPosePlayer;
import ganymedes01.etfuturum.movement.ModernMovement;
import ganymedes01.etfuturum.movement.PlayerPose;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase implements IPosePlayer {

	@Unique
	private PlayerPose etfu$appliedPose;

	@Unique
	private float etfu$swimAmount;

	@Unique
	private float etfu$swimAmountOld;

	@Unique
	private double etfu$waterHeight;

	@Unique
	private boolean etfu$eyeInWater;

	@Unique
	private boolean etfu$wasEyeInWater;

	@Unique
	private int etfu$waterStateTick = -1;

	public MixinEntityPlayer(World world) {
		super(world);
	}

	/**
	 * No walking bob while swimming (§13.2): the bob amount is 0 unless on the ground, alive and the swimming flag is
	 * off, as modern's player tick has it. 1.7.10 computes it in onLivingUpdate, whose first onGround read is that
	 * condition; the second, the camera pitch from falling, is left alone. Crawling bobs with its slow speed, swimming
	 * along the bottom does not.
	 */
	@ModifyExpressionValue(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;onGround:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
	private boolean etfu$noBobWhileSwimming(boolean onGround) {
		return onGround && !etfu$isSwimming();
	}

	@Inject(method = "entityInit", at = @At("TAIL"))
	private void etfu$registerPoseWatcher(CallbackInfo ci) {
		this.dataWatcher.addObject(ConfigFunctions.poseDataWatcherId, (byte) 0);
	}

	/**
	 * Chooses and applies the pose at the very end of the player tick (§3, §4). The server and the local player choose
	 * it themselves (isClientWorld is true for both); other players on the client only apply what the server synced.
	 */
	@Inject(method = "onUpdate", at = @At("TAIL"))
	private void etfu$updatePose(CallbackInfo ci) {
		etfu$updateSwimAmount();

		EntityPlayer player = (EntityPlayer) (Object) this;
		if (!ModernMovement.canUsePoses(player)) {
			if (this.isClientWorld()) {
				etfu$setPose(PlayerPose.STANDING);
			}
			etfu$appliedPose = null;
			return;
		}

		if (this.isClientWorld()) {
			etfu$setPose(ModernMovement.choosePose(player, etfu$getPose()));
		}
		etfu$applyPose(etfu$getPose());
	}

	/**
	 * Resizes the hitbox for the pose. setSize keeps the bottom of the hitbox where it is, so the feet stay put, and
	 * yOffset is never touched (the client player's posY is at eye level and the server checks it against the feet).
	 */
	@Unique
	private void etfu$applyPose(PlayerPose pose) {
		if (pose != etfu$appliedPose) {
			this.setSize(ModernMovement.PLAYER_WIDTH, pose.height);
			etfu$appliedPose = pose;
		}
	}

	/**
	 * Ramps the swim amount 0.09 per tick towards 1 while drawn swimming and towards 0 otherwise. Runs before this
	 * tick's pose choice, so it follows the pose chosen at the end of the previous tick, as in modern.
	 */
	@Unique
	private void etfu$updateSwimAmount() {
		etfu$swimAmountOld = etfu$swimAmount;
		if (ModernMovement.isVisuallySwimming((EntityPlayer) (Object) this)) {
			etfu$swimAmount = Math.min(1.0F, etfu$swimAmount + 0.09F);
		} else {
			etfu$swimAmount = Math.max(0.0F, etfu$swimAmount - 0.09F);
		}
	}

	@Override
	public float etfu$getSwimAmount(float partialTicks) {
		return etfu$swimAmountOld + (etfu$swimAmount - etfu$swimAmountOld) * partialTicks;
	}

	/**
	 * Pose and swimming flag share one synced byte: the pose in bits 0-2, swimming in bit 3. Bits 4-7 belong to the
	 * off-hand (main arm, using hand; §36.3) and are kept as they are.
	 */
	@Unique
	private void etfu$setSynced(PlayerPose pose, boolean swimming) {
		byte current = this.dataWatcher.getWatchableObjectByte(ConfigFunctions.poseDataWatcherId);
		byte value = (byte) ((current & 0xF0) | pose.ordinal() | (swimming ? 8 : 0));
		if (value != current) {
			this.dataWatcher.updateObject(ConfigFunctions.poseDataWatcherId, value);
		}
	}

	@Unique
	private void etfu$setPose(PlayerPose pose) {
		etfu$setSynced(pose, etfu$isSwimming());
	}

	@Override
	public void etfu$setSwimming(boolean swimming) {
		etfu$setSynced(etfu$getPose(), swimming);
	}

	@Override
	public PlayerPose etfu$getPose() {
		return PlayerPose.byId(this.dataWatcher.getWatchableObjectByte(ConfigFunctions.poseDataWatcherId) & 7);
	}

	@Override
	public boolean etfu$isSwimming() {
		return (this.dataWatcher.getWatchableObjectByte(ConfigFunctions.poseDataWatcherId) & 8) != 0;
	}

	@Override
	public void etfu$setWaterState(double waterHeight, boolean eyeInWater) {
		// The scan can run twice in a tick (fall handling repeats it); only the first one moves the old eye state.
		if (this.ticksExisted != etfu$waterStateTick) {
			etfu$waterStateTick = this.ticksExisted;
			etfu$wasEyeInWater = etfu$eyeInWater;
		}
		etfu$waterHeight = waterHeight;
		etfu$eyeInWater = eyeInWater;
	}

	@Override
	public double etfu$getWaterHeight() {
		return etfu$waterHeight;
	}

	@Override
	public boolean etfu$isEyeInWater() {
		return etfu$eyeInWater;
	}

	@Override
	public boolean etfu$wasEyeInWater() {
		return etfu$wasEyeInWater;
	}

	@Override
	public PlayerPose etfu$getAppliedPose() {
		return etfu$appliedPose;
	}

	/**
	 * Moves the eyes with the hitbox (§13.3): default eye height of this player class + the pose's offset from
	 * standing. Additive, so other mods that change eyeHeight keep working.
	 */
	@Inject(method = "getEyeHeight", at = @At("RETURN"), cancellable = true)
	private void etfu$poseEyeHeight(CallbackInfoReturnable<Float> cir) {
		if (etfu$appliedPose != null && etfu$appliedPose != PlayerPose.STANDING) {
			cir.setReturnValue(cir.getReturnValueF() + etfu$appliedPose.eyeHeightOffset());
		}
	}

	/**
	 * Swim steering (§10): while swimming, the vertical speed eases towards the look direction. Not applied when
	 * looking up near the surface without holding jump, so you skim along it instead of leaving the water.
	 */
	@Inject(method = "moveEntityWithHeading", at = @At("HEAD"))
	private void etfu$swimSteering(float strafe, float forward, CallbackInfo ci) {
		if (!ConfigMixins.enableModernSwimming || !etfu$isSwimming() || this.isRiding()) {
			return;
		}
		double lookY = this.getLookVec().yCoord;
		double strength = lookY < -0.2D ? 0.085D : 0.06D;
		int x = MathHelper.floor_double(this.posX);
		int y = MathHelper.floor_double(this.boundingBox.minY + 0.9D);
		int z = MathHelper.floor_double(this.posZ);
		if (lookY <= 0.0D || this.isJumping || this.worldObj.getBlock(x, y, z).getMaterial().isLiquid()) {
			this.motionY += (lookY - this.motionY) * strength;
		}
	}
}
