package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnderChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An ender chest's first opener and last closer post `container_open` and `_close` (SPEC-modern-movement.md §90.9, step H1; 26.3's `ContainerOpenersCounter`). */
@Mixin(TileEntityEnderChest.class)
public abstract class MixinTileEntityEnderChest {

	/** 1.7.10's count of players looking in. */
	@Shadow
	public int field_145973_j;

	@Inject(method = "func_145969_a", at = @At("RETURN"))
	private void mcfly$opened(CallbackInfo ci) {
		TileEntity self = (TileEntity) (Object) this;
		if (self.getWorldObj() != null && !self.getWorldObj().isRemote && field_145973_j == 1) {
			GameEventHooks.container(self.getWorldObj(), self.xCoord, self.yCoord, self.zCoord, true);
		}
	}

	@Inject(method = "func_145970_b", at = @At("RETURN"))
	private void mcfly$closed(CallbackInfo ci) {
		TileEntity self = (TileEntity) (Object) this;
		if (self.getWorldObj() != null && !self.getWorldObj().isRemote && field_145973_j == 0) {
			GameEventHooks.container(self.getWorldObj(), self.xCoord, self.yCoord, self.zCoord, false);
		}
	}
}
