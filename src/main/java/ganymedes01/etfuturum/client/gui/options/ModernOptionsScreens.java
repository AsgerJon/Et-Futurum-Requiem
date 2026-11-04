package ganymedes01.etfuturum.client.gui.options;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.asgerjon.mcfly.mixins.early.modernoptions.client.AccessorGuiControls;
import io.github.asgerjon.mcfly.mixins.early.modernoptions.client.AccessorGuiOptions;
import io.github.asgerjon.mcfly.mixins.early.modernoptions.client.AccessorGuiVideoSettings;
import io.github.asgerjon.mcfly.mixins.early.modernoptions.client.AccessorScreenChatOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.ScreenChatOptions;
import net.minecraftforge.client.event.GuiOpenEvent;

/**
 * EFR's modern options screens take the place of 1.7.10's as these open (spec §54.10), with the screen 1.7.10's would
 * go back to. Only 1.7.10's own classes are replaced: a mod's subclass of one stays as it is.
 */
public class ModernOptionsScreens {

	@SubscribeEvent
	public void onGuiOpen(GuiOpenEvent event) {
		GuiScreen screen = event.gui;
		if (screen == null) {
			return;
		}
		Class<?> type = screen.getClass();
		Minecraft mc = Minecraft.getMinecraft();
		if (type == GuiOptions.class) {
			event.gui = new GuiModernOptions(((AccessorGuiOptions) screen).etfu$parent(), mc.gameSettings);
		} else if (type == GuiVideoSettings.class) {
			event.gui = new GuiModernVideoSettings(((AccessorGuiVideoSettings) screen).etfu$parent(), mc.gameSettings);
		} else if (type == ScreenChatOptions.class) {
			event.gui = new GuiModernChatSettings(((AccessorScreenChatOptions) screen).etfu$parent(), mc.gameSettings);
		} else if (type == GuiControls.class) {
			event.gui = new GuiModernControls(((AccessorGuiControls) screen).etfu$parent());
		}
	}
}
