package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * For EFR's Video Settings (spec §54.10): the screen 1.7.10's goes back to, and its list, which EFR's screen fills
 * with its own so that 1.7.10's clicks, drawing and GUI-scale handling (and EFR's wheel for it) work on it unchanged.
 */
@Mixin(GuiVideoSettings.class)
public interface AccessorGuiVideoSettings {

	@Accessor("parentGuiScreen")
	GuiScreen etfu$parent();

	@Accessor("optionsRowList")
	void etfu$setOptionsRowList(GuiListExtended list);
}
