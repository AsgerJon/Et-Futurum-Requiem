package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScreenChatOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The screen 1.7.10's Chat Settings goes back to, for EFR's Chat Settings that takes its place (spec §54.10). */
@Mixin(ScreenChatOptions.class)
public interface AccessorScreenChatOptions {

	@Accessor("field_146396_g")
	GuiScreen etfu$parent();
}
