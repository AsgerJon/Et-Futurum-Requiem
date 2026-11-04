package io.github.asgerjon.mcfly.mixins.early.modernchat.client;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScreenChatOptions;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modern's "Command Suggestions" in Chat Settings (spec §40.5): on, the suggestion list opens as you type a command;
 * off, Tab opens it. It takes the free place beside Chat Width.
 */
@Mixin(ScreenChatOptions.class)
public abstract class MixinScreenChatOptions extends GuiScreen {

	@Unique
	private static final int ETFU_COMMAND_SUGGESTIONS = 4041;

	@Inject(method = "initGui", at = @At("TAIL"))
	private void etfu$addCommandSuggestions(CallbackInfo ci) {
		buttonList.add(new GuiButton(ETFU_COMMAND_SUGGESTIONS, width / 2 + 5, height / 6 + 24 * 4, 150, 20, etfu$label()));
	}

	@Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
	private void etfu$toggleCommandSuggestions(GuiButton button, CallbackInfo ci) {
		if (button.id == ETFU_COMMAND_SUGGESTIONS) {
			McFlyConfig.saveCommandSuggestions(!McFlyConfig.commandSuggestions);
			button.displayString = etfu$label();
			ci.cancel();
		}
	}

	@Unique
	private static String etfu$label() {
		return I18n.format("options.autoSuggestCommands") + ": " + I18n.format(McFlyConfig.commandSuggestions ? "options.on" : "options.off");
	}
}
