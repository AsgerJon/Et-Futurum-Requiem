package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * For EFR's Controls and Key Binds (spec §54.10): the screen 1.7.10's Controls goes back to, and its key list, which
 * Key Binds moves up into the room 1.7.10's option buttons took.
 */
@Mixin(GuiControls.class)
public interface AccessorGuiControls {

	@Accessor("parentScreen")
	GuiScreen etfu$parent();

	@Accessor("keyBindingList")
	GuiKeyBindingList etfu$keyList();
}
