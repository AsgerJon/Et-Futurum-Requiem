package ganymedes01.etfuturum.client.gui.options;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 26.3's Options screen (spec §54.4, §54.10) on 1.7.10's: FOV and Difficulty on top as in 1.7.10, then 26.3's grid,
 * Skin Customization | Music & Sounds, Video Settings | Controls, Language | Chat Settings, Resource Packs |
 * Accessibility Settings, and Snooper Settings in 26.3's Telemetry place. Super Secret Settings and Broadcast Settings
 * go. 1.7.10 builds the buttons and acts on them; this screen only lays them out and adds Accessibility.
 */
public class GuiModernOptions extends GuiOptions implements IOptionsLayout {

	private static final int ACCESSIBILITY = 4100;
	/** 1.7.10's button ids (and EFR's Skin Customization, 110) in 26.3's grid order. */
	private static final int[] GRID = {110, 106, 101, 100, 102, 103, 105, ACCESSIBILITY, 104};

	private final List<GuiButton> layout = new ArrayList<>();

	public GuiModernOptions(GuiScreen parent, GameSettings settings) {
		super(parent, settings);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initGui() {
		super.initGui();
		List<GuiButton> top = new ArrayList<>();
		Map<Integer, GuiButton> others = new LinkedHashMap<>();
		GuiButton done = null;
		for (Object entry : new ArrayList<Object>(buttonList)) {
			GuiButton button = (GuiButton) entry;
			if (button.id == 8675309 || button.id == 107) {
				// Super Secret Settings, Broadcast Settings
				buttonList.remove(button);
			} else if (button.id < 100) {
				// FOV and Difficulty
				top.add(button);
			} else if (button.id == 200) {
				done = button;
			} else {
				others.put(button.id, button);
			}
		}
		GuiButton chat = others.get(103);
		if (chat != null) {
			// 1.7.10 calls it Multiplayer Settings...
			chat.displayString = I18n.format("options.chat");
		}
		GuiButton accessibility = new GuiButton(ACCESSIBILITY, 0, 0, 150, 20, I18n.format("options.accessibility"));
		buttonList.add(accessibility);
		others.put(ACCESSIBILITY, accessibility);

		layout.clear();
		top.sort(Comparator.comparingInt(button -> button.xPosition));
		layout.addAll(top);
		int slot = 0;
		for (int id : GRID) {
			GuiButton button = others.remove(id);
			if (button != null) {
				button.xPosition = width / 2 - 155 + slot % 2 * 160;
				button.yPosition = height / 6 + 42 + 24 * (slot / 2);
				button.width = 150;
				layout.add(button);
				slot++;
			}
		}
		// buttons another mod added keep their own places
		layout.addAll(others.values());
		if (done != null) {
			layout.add(done);
		}
	}

	@Override
	public List<Object> etfu$entries() {
		return new ArrayList<>(layout);
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.enabled && button.id == ACCESSIBILITY) {
			mc.gameSettings.saveOptions();
			mc.displayGuiScreen(new GuiAccessibilitySettings(this));
			return;
		}
		super.actionPerformed(button);
	}
}
