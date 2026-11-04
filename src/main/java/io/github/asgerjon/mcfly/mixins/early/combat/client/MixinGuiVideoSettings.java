package io.github.asgerjon.mcfly.mixins.early.combat.client;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.client.gui.GuiAttackIndicatorButton;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import io.github.asgerjon.mcfly.mixins.early.fov.client.AccessorGuiOptionsRowList;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiOptionsRowList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Attack Indicator option as a row of the video settings list (spec §46.11), where modern has it. With FOV Effects
 * on, it sits beside that slider in its row instead (fov.client.MixinGuiVideoSettings).
 */
@Mixin(GuiVideoSettings.class)
public abstract class MixinGuiVideoSettings extends GuiScreen {
	@Shadow
	private GuiListExtended optionsRowList;

	@Inject(method = "initGui", at = @At("TAIL"))
	private void etfu$attackIndicatorRow(CallbackInfo ci) {
		if (!McFlyConfig.fovEffects && optionsRowList instanceof GuiOptionsRowList list) {
			((AccessorGuiOptionsRowList) list).etfu$rows().add(new GuiOptionsRowList.Row(new GuiAttackIndicatorButton(1001, this.width / 2 - 155, 0), null));
		}
	}
}
