package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The screen 1.7.10's Options screen goes back to, for EFR's Options screen that takes its place (spec §54.10). */
@Mixin(GuiOptions.class)
public interface AccessorGuiOptions {

	@Accessor("field_146441_g")
	GuiScreen etfu$parent();
}
