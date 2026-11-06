package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A tripwire hook's new state posts with its sound (SPEC-modern-movement.md §90.9, step H1; 26.3's
 * `TripWireHookBlock.emitState`, the same choice): `block_activate`, `_deactivate`, `block_attach` or `_detach`.
 */
@Mixin(BlockTripWireHook.class)
public abstract class MixinBlockTripWireHook {

	@Inject(method = "func_150135_a", at = @At("HEAD"))
	private void mcfly$emitState(World world, int x, int y, int z, boolean attached, boolean powered, boolean wasAttached, boolean wasPowered, CallbackInfo ci) {
		String event = powered && !wasPowered ? "block_activate" : !powered && wasPowered ? "block_deactivate" : attached && !wasAttached ? "block_attach"
			: !attached && wasAttached ? "block_detach" : null;
		if (event != null) {
			GameEventHooks.atBlock(world, event, x, y, z, null);
		}
	}
}
