package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import ganymedes01.etfuturum.client.gui.options.GuiAccessibilityButton;
import ganymedes01.etfuturum.client.gui.options.GuiAccessibilitySettings;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonLanguage;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

/**
 * The title screen's row of icon buttons as 26.3's (spec §54.10): Language and Accessibility, 20 x 20 and 4 apart,
 * centred, where 1.7.10 had Options... and Quit Game; those move 24 lower, under the row.
 */
@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu extends GuiScreen {

	@Unique
	private static final int ETFU_ACCESSIBILITY = 4100;
	@Unique
	private GuiAccessibilityButton etfu$accessibility;

	@Inject(method = "initGui", at = @At("TAIL"))
	@SuppressWarnings("unchecked")
	private void etfu$iconRow(CallbackInfo ci) {
		GuiButton language = null;
		GuiButton options = null;
		GuiButton quit = null;
		for (Object entry : buttonList) {
			GuiButton button = (GuiButton) entry;
			if (button instanceof GuiButtonLanguage) {
				language = button;
			} else if (button.id == 0) {
				options = button;
			} else if (button.id == 4) {
				quit = button;
			}
		}
		if (language == null || options == null || quit == null) {
			// another mod rebuilt the title screen; leave it as it is
			return;
		}
		// 1.7.10's Options... row sits 36 under the third menu row; the icon row takes 24 of that, as 26.3's
		int rowY = options.yPosition - 12;
		language.xPosition = width / 2 - 22;
		language.yPosition = rowY;
		options.yPosition = rowY + 24;
		quit.yPosition = rowY + 24;
		etfu$accessibility = new GuiAccessibilityButton(ETFU_ACCESSIBILITY, width / 2 + 2, rowY);
		buttonList.add(etfu$accessibility);
	}

	@Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
	private void etfu$openAccessibility(GuiButton button, CallbackInfo ci) {
		if (button.id == ETFU_ACCESSIBILITY) {
			mc.displayGuiScreen(new GuiAccessibilitySettings(this));
			ci.cancel();
		}
	}

	@Inject(method = "drawScreen", at = @At("TAIL"))
	private void etfu$accessibilityHoverText(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
		if (etfu$accessibility != null && etfu$accessibility.hovered(mouseX, mouseY)) {
			func_146283_a(Collections.singletonList(I18n.format("options.accessibility.tooltip")), mouseX, mouseY);
		}
	}
}
