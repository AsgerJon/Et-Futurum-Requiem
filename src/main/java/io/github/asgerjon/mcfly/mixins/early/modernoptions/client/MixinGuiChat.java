package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.client.options.ControlOptions;
import net.minecraft.client.gui.GuiChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The chat scrolls by the wheel through Scroll Sensitivity and Discrete Scrolling (spec §54.12). */
@Mixin(GuiChat.class)
public abstract class MixinGuiChat {

	@ModifyExpressionValue(method = "handleMouseInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
	private int etfu$scaledWheel(int wheel) {
		return ControlOptions.CHAT.screen(wheel);
	}
}
