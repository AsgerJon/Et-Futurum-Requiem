package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import ganymedes01.etfuturum.client.options.ToggleKeys;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.IntHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3's toggling keys (spec §54.11): for Sneak, Sprint, Attack/Destroy and Use Item/Place Block set to Toggle, a
 * key-down flips the key and a key-up does nothing. The click count (onTick) is untouched, so a press still counts as
 * a click, as in 26.3.
 */
@Mixin(KeyBinding.class)
public abstract class MixinKeyBinding {

	@Shadow
	@Final
	private static IntHashMap hash;

	@Inject(method = "setKeyBindState", at = @At("HEAD"), cancellable = true)
	private static void etfu$toggle(int keyCode, boolean pressed, CallbackInfo ci) {
		if (keyCode == 0) {
			return;
		}
		KeyBinding binding = (KeyBinding) hash.lookup(keyCode);
		if (binding != null && ToggleKeys.inToggle(binding)) {
			if (pressed) {
				ToggleKeys.flip(binding);
			}
			ci.cancel();
		}
	}
}
