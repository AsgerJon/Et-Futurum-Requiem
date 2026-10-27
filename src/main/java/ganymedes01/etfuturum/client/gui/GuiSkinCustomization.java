package ganymedes01.etfuturum.client.gui;

import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.core.handlers.ClientEventHandler;
import ganymedes01.etfuturum.skins.SkinPart;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.util.Arrays;

/**
 * Modern's Skin Customization screen (1.8+; the main hand since 1.9): an on/off button per skin part and the main
 * hand. Closing it saves the choices to functions.cfg and sends them to the server, which shows them to everyone.
 */
public class GuiSkinCustomization extends GuiScreen {

	private static final int MAIN_HAND = 100, DONE = 200;
	private final GuiScreen parent;
	private final boolean[] shown = ConfigFunctions.skinPartsShown();
	private final boolean[] shownBefore = ConfigFunctions.skinPartsShown();
	private final boolean leftHandedBefore = ConfigFunctions.mainHandLeft;
	private boolean leftHanded = ConfigFunctions.mainHandLeft;

	public GuiSkinCustomization(GuiScreen parent) {
		this.parent = parent;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initGui() {
		buttonList.clear();
		int i = 0;
		for (SkinPart part : SkinPart.VALUES) {
			buttonList.add(new GuiButton(part.ordinal(), width / 2 - 155 + i % 2 * 160, height / 6 + 24 * (i >> 1), 150, 20, partLabel(part)));
			i++;
		}
		buttonList.add(new GuiButton(MAIN_HAND, width / 2 - 155 + i % 2 * 160, height / 6 + 24 * (i >> 1), 150, 20, mainHandLabel()));
		buttonList.add(new GuiButton(DONE, width / 2 - 100, height / 6 + 168, I18n.format("gui.done")));
	}

	private String partLabel(SkinPart part) {
		return I18n.format(part.translationKey()) + ": " + I18n.format(shown[part.ordinal()] ? "options.on" : "options.off");
	}

	private String mainHandLabel() {
		return I18n.format("options.etfuturum.mainHand") + ": " + I18n.format(leftHanded ? "options.etfuturum.mainHand.left" : "options.etfuturum.mainHand.right");
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (!button.enabled) {
			return;
		}
		if (button.id < SkinPart.VALUES.length) {
			shown[button.id] = !shown[button.id];
			button.displayString = partLabel(SkinPart.VALUES[button.id]);
		} else if (button.id == MAIN_HAND) {
			leftHanded = !leftHanded;
			button.displayString = mainHandLabel();
		} else if (button.id == DONE) {
			mc.displayGuiScreen(parent);
		}
	}

	/** Leaving the screen in any way keeps the choices, as modern applies them at once. */
	@Override
	public void onGuiClosed() {
		if (!Arrays.equals(shown, shownBefore) || leftHanded != leftHandedBefore) {
			ConfigFunctions.saveSkinOptions(shown, leftHanded);
			ClientEventHandler.sendPlayerSettings();
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, I18n.format("options.etfuturum.skinCustomisation.title"), width / 2, 20, 0xFFFFFF);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
