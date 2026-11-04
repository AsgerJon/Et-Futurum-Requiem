package ganymedes01.etfuturum.client.gui.options;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 26.3's options list on 1.7.10's (spec §54.3): two columns of 150-wide buttons that scroll, rows 25 high, and section
 * headers (underlined, bold, centred). A click on a button goes first to the button itself (an option sets its value
 * there, as 1.7.10's option buttons and EFR's do) and then to the screen, for buttons that open another screen.
 */
public class OptionsRowList extends GuiListExtended {

	private final List<GuiListExtended.IGuiListEntry> rows = new ArrayList<>();
	private final List<Object> entries = new ArrayList<>();
	private final Minecraft minecraft;
	private final Consumer<GuiButton> onPress;
	private GuiButton pendingLeft;

	public OptionsRowList(Minecraft mc, int width, int height, int top, int bottom, Consumer<GuiButton> onPress) {
		super(mc, width, height, top, bottom, 25);
		minecraft = mc;
		this.onPress = onPress;
		field_148163_i = false;
	}

	/** Buttons two to a row, left then right, after what is already there. */
	public OptionsRowList add(GuiButton... buttons) {
		for (GuiButton button : buttons) {
			if (button == null) {
				continue;
			}
			entries.add(button);
			button.width = 150;
			if (pendingLeft == null) {
				button.xPosition = width / 2 - 155;
				pendingLeft = button;
				rows.add(new Row(button));
			} else {
				button.xPosition = width / 2 + 5;
				((Row) rows.get(rows.size() - 1)).right = button;
				pendingLeft = null;
			}
		}
		return this;
	}

	/** A section header on its own row; the next button starts a new row. */
	public OptionsRowList header(String text) {
		pendingLeft = null;
		entries.add(text);
		rows.add(new Header(text));
		return this;
	}

	/** The buttons and headers in order, for IOptionsLayout. */
	public List<Object> entries() {
		return new ArrayList<>(entries);
	}

	@Override
	public GuiListExtended.IGuiListEntry getListEntry(int index) {
		return rows.get(index);
	}

	@Override
	protected int getSize() {
		return rows.size();
	}

	@Override
	public int getListWidth() {
		return 400;
	}

	@Override
	protected int getScrollBarX() {
		return super.getScrollBarX() + 32;
	}

	private final class Row implements GuiListExtended.IGuiListEntry {
		private final GuiButton left;
		private GuiButton right;

		private Row(GuiButton left) {
			this.left = left;
		}

		@Override
		public void drawEntry(int index, int x, int y, int listWidth, int slotHeight, Tessellator tessellator, int mouseX, int mouseY, boolean selected) {
			for (GuiButton button : new GuiButton[] {left, right}) {
				if (button != null) {
					button.yPosition = y;
					button.drawButton(minecraft, mouseX, mouseY);
				}
			}
		}

		@Override
		public boolean mousePressed(int index, int mouseX, int mouseY, int mouseButton, int relativeX, int relativeY) {
			for (GuiButton button : new GuiButton[] {left, right}) {
				if (button != null && button.mousePressed(minecraft, mouseX, mouseY)) {
					button.func_146113_a(minecraft.getSoundHandler());
					onPress.accept(button);
					return true;
				}
			}
			return false;
		}

		@Override
		public void mouseReleased(int index, int mouseX, int mouseY, int mouseButton, int relativeX, int relativeY) {
			for (GuiButton button : new GuiButton[] {left, right}) {
				if (button != null) {
					button.mouseReleased(mouseX, mouseY);
				}
			}
		}
	}

	private final class Header implements GuiListExtended.IGuiListEntry {
		private final String text;

		private Header(String text) {
			this.text = text;
		}

		@Override
		public void drawEntry(int index, int x, int y, int listWidth, int slotHeight, Tessellator tessellator, int mouseX, int mouseY, boolean selected) {
			String shown = EnumChatFormatting.UNDERLINE + "" + EnumChatFormatting.BOLD + text;
			minecraft.fontRenderer.drawStringWithShadow(shown, width / 2 - minecraft.fontRenderer.getStringWidth(shown) / 2, y + (slotHeight - 4 - minecraft.fontRenderer.FONT_HEIGHT) / 2 + 2, 0xFFFFFF);
		}

		@Override
		public boolean mousePressed(int index, int mouseX, int mouseY, int mouseButton, int relativeX, int relativeY) {
			return false;
		}

		@Override
		public void mouseReleased(int index, int mouseX, int mouseY, int mouseButton, int relativeX, int relativeY) {
		}
	}
}
