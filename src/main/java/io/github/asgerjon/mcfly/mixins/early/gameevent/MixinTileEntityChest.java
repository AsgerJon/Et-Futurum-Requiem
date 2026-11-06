package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A chest's first opener and last closer post `container_open` and `_close` (SPEC-modern-movement.md §90.9, step H1; 26.3's `ContainerOpenersCounter`). */
@Mixin(TileEntityChest.class)
public abstract class MixinTileEntityChest {

	@Shadow
	public int numPlayersUsing;

	@Unique
	private int mcfly$usersBefore;

	@Inject(method = "openInventory", at = @At("RETURN"))
	private void mcfly$opened(CallbackInfo ci) {
		TileEntity self = (TileEntity) (Object) this;
		if (self.getWorldObj() != null && !self.getWorldObj().isRemote && numPlayersUsing == 1) {
			GameEventHooks.container(self.getWorldObj(), self.xCoord, self.yCoord, self.zCoord, true);
		}
	}

	@Inject(method = "closeInventory", at = @At("HEAD"))
	private void mcfly$closing(CallbackInfo ci) {
		mcfly$usersBefore = numPlayersUsing;
	}

	@Inject(method = "closeInventory", at = @At("RETURN"))
	private void mcfly$closed(CallbackInfo ci) {
		TileEntity self = (TileEntity) (Object) this;
		if (self.getWorldObj() != null && !self.getWorldObj().isRemote && mcfly$usersBefore > 0 && numPlayersUsing == 0) {
			GameEventHooks.container(self.getWorldObj(), self.xCoord, self.yCoord, self.zCoord, false);
		}
	}
}
