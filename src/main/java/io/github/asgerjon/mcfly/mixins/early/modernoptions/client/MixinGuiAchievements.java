package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.client.options.ControlOptions;
import net.minecraft.client.gui.achievement.GuiAchievements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The achievement map zooms by the wheel through Scroll Sensitivity and Discrete Scrolling (spec §54.12). */
@Mixin(GuiAchievements.class)
public abstract class MixinGuiAchievements {

	@ModifyExpressionValue(method = "drawScreen", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getDWheel()I", remap = false))
	private int etfu$scaledWheel(int wheel) {
		return ControlOptions.ACHIEVEMENTS.screen(wheel);
	}
}
