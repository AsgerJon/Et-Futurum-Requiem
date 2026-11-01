package ganymedes01.etfuturum.mixins.early.unittas.client;

import ganymedes01.etfuturum.core.utils.UnitTasMode;
import net.minecraft.util.MouseHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** During a unitTAS run the game never takes the mouse pointer, and the real mouse doesn't turn the player. */
@Mixin(MouseHelper.class)
public abstract class MixinMouseHelper {

	@Shadow
	public int deltaX;

	@Shadow
	public int deltaY;

	@Inject(method = "grabMouseCursor", at = @At("HEAD"), cancellable = true)
	private void etfu$leavePointerFree(CallbackInfo ci) {
		if (UnitTasMode.running) {
			ci.cancel();
		}
	}

	@Inject(method = "mouseXYChange", at = @At("HEAD"), cancellable = true)
	private void etfu$noRealLooking(CallbackInfo ci) {
		if (UnitTasMode.running) {
			deltaX = 0;
			deltaY = 0;
			ci.cancel();
		}
	}
}
