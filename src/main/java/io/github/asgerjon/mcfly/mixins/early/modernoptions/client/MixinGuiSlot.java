package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.client.gui.IGuiScaleWheel;
import ganymedes01.etfuturum.client.options.ControlOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lists scroll by the wheel through Scroll Sensitivity and Discrete Scrolling (spec §54.12). Not while the wheel cycles
 * the GUI scale (guiscale.client.MixinGuiSlot): 26.3 steps a cycling option once per wheel event, whatever the
 * sensitivity.
 */
@Mixin(GuiSlot.class)
public abstract class MixinGuiSlot {

	@ModifyExpressionValue(method = "drawScreen", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
	private int etfu$scaledWheel(int wheel) {
		if (Minecraft.getMinecraft().currentScreen instanceof IGuiScaleWheel screen && screen.etfu$wheelChangesGuiScale()) {
			return wheel;
		}
		return ControlOptions.LISTS.screen(wheel);
	}
}
