package ganymedes01.etfuturum.client.gui.options;

import io.github.asgerjon.mcfly.mixins.early.modernoptions.client.AccessorGuiControls;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.3's Key Binds (spec §54.4, §54.10): 1.7.10's key list without the option buttons it had above it (they are on
 * Mouse Settings now), the list moved up into their room, and 26.3's footer, Reset Keys left of Done.
 */
public class GuiKeyBinds extends GuiControls implements IOptionsLayout {

	private GuiButton resetKeys;
	private GuiButton done;

	public GuiKeyBinds(GuiScreen parent, GameSettings settings) {
		super(parent, settings);
	}

	@Override
	public void initGui() {
		super.initGui();
		resetKeys = null;
		done = null;
		for (Object entry : new ArrayList<Object>(buttonList)) {
			GuiButton button = (GuiButton) entry;
			if (button.id == 200) {
				done = button;
			} else if (button.id == 201) {
				resetKeys = button;
			} else if (button.id < 100) {
				// 1.7.10's Invert Mouse, Sensitivity and Touchscreen Mode
				buttonList.remove(button);
			}
		}
		if (resetKeys != null && done != null) {
			int x = done.xPosition;
			done.xPosition = resetKeys.xPosition;
			resetKeys.xPosition = x;
		}
		field_146495_a = I18n.format("controls.keybinds.title");
		GuiKeyBindingList keys = ((AccessorGuiControls) this).etfu$keyList();
		if (keys != null) {
			keys.top = 32;
		}
	}

	@Override
	public List<Object> etfu$entries() {
		List<Object> entries = new ArrayList<>();
		if (resetKeys != null) {
			entries.add(resetKeys);
		}
		if (done != null) {
			entries.add(done);
		}
		return entries;
	}
}
