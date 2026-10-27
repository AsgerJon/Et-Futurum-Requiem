package ganymedes01.etfuturum.mixins.early.beacon;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.core.utils.BeaconBeam;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A beacon is on when its beam reaches the top of the world by modern's rule (BeaconBeam), not when the sky map says so. */
@Mixin(TileEntityBeacon.class)
public abstract class MixinTileEntityBeacon extends TileEntity {

	@ModifyExpressionValue(method = "func_146003_y", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;canBlockSeeTheSky(III)Z"))
	private boolean etfu$beamReachesTheSky(boolean skyMapSaysSo) {
		return BeaconBeam.reachesTheSky(worldObj, xCoord, yCoord, zCoord);
	}
}
