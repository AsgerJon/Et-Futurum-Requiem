package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Puts a key down or up without a key event, for the Hold/Toggle keys (spec §54.11, client.options.ToggleKeys). */
@Mixin(KeyBinding.class)
public interface AccessorKeyBinding {

	@Accessor("pressed")
	void etfu$setPressed(boolean pressed);
}
