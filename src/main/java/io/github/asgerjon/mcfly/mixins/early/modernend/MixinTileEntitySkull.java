package io.github.asgerjon.mcfly.mixins.early.modernend;

import ganymedes01.etfuturum.ducks.IDragonHead;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * A dragon head (skull type 5) works its jaw while powered, as 26.3's SkullBlockEntity.animation (on the client, where
 * 26.3 ticks it; SPEC-modern-movement.md §76.12, 7d).
 */
@Mixin(TileEntitySkull.class)
public abstract class MixinTileEntitySkull extends TileEntity implements IDragonHead {

	@Shadow
	public abstract int func_145904_a();

	@Unique
	private int etfu$animationTickCount;
	@Unique
	private boolean etfu$animating;

	@Override
	public void updateEntity() {
		if (worldObj != null && worldObj.isRemote && func_145904_a() == 5) {
			if (worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord)) {
				etfu$animating = true;
				etfu$animationTickCount++;
			} else {
				etfu$animating = false;
			}
		}
	}

	@Override
	public float etfu$animation(float partialTicks) {
		return etfu$animating ? etfu$animationTickCount + partialTicks : etfu$animationTickCount;
	}
}
