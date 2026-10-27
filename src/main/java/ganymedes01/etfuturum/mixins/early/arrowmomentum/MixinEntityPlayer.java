package ganymedes01.etfuturum.mixins.early.arrowmomentum;

import ganymedes01.etfuturum.movement.IKnownMovement;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** The player's known movement: its motion on the client, the last position packet's step on the server. */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer implements IKnownMovement {
	@Unique
	private double etfu$knownX, etfu$knownY, etfu$knownZ;

	@Override
	public double etfu$knownMotionX() {
		EntityPlayer self = (EntityPlayer) (Object) this;
		return self.worldObj.isRemote ? self.motionX : etfu$knownX;
	}

	@Override
	public double etfu$knownMotionY() {
		EntityPlayer self = (EntityPlayer) (Object) this;
		return self.worldObj.isRemote ? self.motionY : etfu$knownY;
	}

	@Override
	public double etfu$knownMotionZ() {
		EntityPlayer self = (EntityPlayer) (Object) this;
		return self.worldObj.isRemote ? self.motionZ : etfu$knownZ;
	}

	@Override
	public void etfu$setKnownMovement(double x, double y, double z) {
		etfu$knownX = x;
		etfu$knownY = y;
		etfu$knownZ = z;
	}
}
