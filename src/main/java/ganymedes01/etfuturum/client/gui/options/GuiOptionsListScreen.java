package ganymedes01.etfuturum.client.gui.options;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.util.List;

/**
 * A modern options screen of EFR's own (spec §54.10): its title, its options list, and Done, which saves the options
 * and goes back, as Escape does in 26.3.
 */
public abstract class GuiOptionsListScreen extends GuiScreen implements IOptionsLayout {

	protected static final int DONE = 200;

	protected final GuiScreen parent;
	private final String titleKey;
	private OptionsRowList list;
	private GuiButton done;

	protected GuiOptionsListScreen(GuiScreen parent, String titleKey) {
		this.parent = parent;
		this.titleKey = titleKey;
	}

	/** Fills the list with the screen's options, in 26.3's order. */
	protected abstract void addOptions(OptionsRowList list);

	/** A list button that isn't an option of its own: one that opens another screen. */
	protected void pressed(GuiButton button) {
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initGui() {
		buttonList.clear();
		list = new OptionsRowList(mc, width, height, 32, height - 32, this::pressed);
		addOptions(list);
		done = new GuiButton(DONE, width / 2 - 100, height - 27, I18n.format("gui.done"));
		buttonList.add(done);
	}

	@Override
	public List<Object> etfu$entries() {
		List<Object> entries = list.entries();
		entries.add(done);
		return entries;
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.enabled && button.id == DONE) {
			close();
		}
	}

	@Override
	protected void keyTyped(char character, int keyCode) {
		if (keyCode == 1) {
			close();
		}
	}

	private void close() {
		mc.gameSettings.saveOptions();
		mc.displayGuiScreen(parent);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		list.func_148179_a(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
		super.mouseMovedOrUp(mouseX, mouseY, state);
		list.func_148181_b(mouseX, mouseY, state);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		list.drawScreen(mouseX, mouseY, partialTicks);
		drawCenteredString(fontRendererObj, I18n.format(titleKey), width / 2, 5, 0xFFFFFF);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
