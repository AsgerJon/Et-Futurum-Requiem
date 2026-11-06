package io.github.asgerjon.mcfly.wood.client;

import io.github.asgerjon.mcfly.network.McFlyNetwork;
import io.github.asgerjon.mcfly.network.SignUpdateMessage;
import io.github.asgerjon.mcfly.wood.SignSide;
import io.github.asgerjon.mcfly.wood.Signs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/**
 * 26.3's sign editor (SPEC-modern-movement.md 91.8; `SignEditScreen` on `AbstractSignEditScreen`): the sign's picture
 * (`gui/signs/<wood>.png` times 3.9; a wall sign's top 12 rows), its side's four lines centred and 10 apart in their
 * colour, a cursor blinking every 300 ms. Typing changes the sign as it goes (on this client); closing (Done, Esc, the
 * sign gone or out of reach) sends the lines.
 */
public class SignEditScreen extends GuiScreen {

	private static final float BACKGROUND_SCALE = 3.9F;
	private static final float TEXT_SCALE = 0.9765628F;

	protected final TileEntitySign sign;
	private final boolean front;
	private final String[] messages = new String[Signs.LINES];
	private final int textColor;
	protected final ResourceLocation texture;
	private final int displayedHeight;
	private final int lineHeight;
	private final SignTextField field;
	private int line;
	private long blinkStart;

	public SignEditScreen(TileEntitySign sign, boolean front) {
		this.sign = sign;
		this.front = front;
		SignSide side = ((Signs.Sided) sign).mcfly$side(front);
		for (int i = 0; i < Signs.LINES; i++) {
			messages[i] = side.line(i);
		}
		textColor = Signs.textColor(side.color, side.glowing);
		texture = new ResourceLocation(Signs.guiTexture(sign.getBlockType()));
		displayedHeight = Signs.standing(sign.getBlockType()) ? 26 : 12;
		lineHeight = Signs.lineHeight(sign);
		int maxWidth = Signs.maxWidth(sign);
		field = new SignTextField(() -> messages[line], this::setMessage, s -> Minecraft.getMinecraft().fontRenderer.getStringWidth(s) <= maxWidth);
	}

	/** McFly's open message (26.3's `handleOpenSignEditor`): the editor for the sign there, if one is. */
	public static void open(int x, int y, int z, boolean front) {
		Minecraft mc = Minecraft.getMinecraft();
		TileEntity tile = mc.theWorld == null ? null : mc.theWorld.getTileEntity(x, y, z);
		if (tile instanceof TileEntitySign sign && tile instanceof Signs.Sided) {
			mc.displayGuiScreen(Signs.hanging(sign) ? new HangingSignEditScreen(sign, front) : new SignEditScreen(sign, front));
		} else {
			LogManager.getLogger("McFly").warn("Ignoring openTextEdit on an invalid entity: {} at pos {} {} {}", tile, x, y, z);
		}
	}

	@Override
	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		blinkStart = Minecraft.getSystemTime();
		buttonList.clear();
		buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 144, 200, 20, I18n.format("gui.done")));
	}

	@Override
	public void updateScreen() {
		if (!valid()) {
			mc.displayGuiScreen(null);
		}
	}

	private boolean valid() {
		return mc.thePlayer != null && !sign.isInvalid() && mc.theWorld.getTileEntity(sign.xCoord, sign.yCoord, sign.zCoord) == sign
			&& Signs.inReach(mc.thePlayer, sign.xCoord, sign.yCoord, sign.zCoord);
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 0) {
			mc.displayGuiScreen(null);
		}
	}

	@Override
	protected void keyTyped(char c, int key) {
		if (key == Keyboard.KEY_UP) {
			line = line - 1 & 3;
			field.setCursorToEnd();
			return;
		}
		if (key == Keyboard.KEY_DOWN || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
			line = line + 1 & 3;
			field.setCursorToEnd();
			return;
		}
		if (field.keyPressed(key)) {
			return;
		}
		if (key == Keyboard.KEY_ESCAPE) {
			mc.displayGuiScreen(null);
			return;
		}
		field.charTyped(c);
	}

	private void setMessage(String message) {
		if (!messages[line].equals(message)) {
			messages[line] = message;
			((Signs.Sided) sign).mcfly$side(front).lines[line] = message;
		}
	}

	@Override
	public void onGuiClosed() {
		if (mc.getNetHandler() != null) {
			McFlyNetwork.CHANNEL.sendToServer(new SignUpdateMessage(sign.xCoord, sign.yCoord, sign.zCoord, front, messages));
		}
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	/** 26.3's `title` (`sign.edit`). */
	protected String title() {
		return I18n.format("gui.etfuturum.sign.edit");
	}

	/** 26.3's `getSignYOffset`. */
	protected float signYOffset() {
		return 90.0F;
	}

	/** 26.3's `getSignTextScale`. */
	protected float textScale() {
		return TEXT_SCALE;
	}

	/** 26.3's `SignEditScreen.extractSignBackground`: the picture times 3.9, a wall sign's top 12 rows. */
	protected void drawBackground() {
		GL11.glTranslatef(0.0F, 27.0F, 0.0F);
		GL11.glScalef(BACKGROUND_SCALE, BACKGROUND_SCALE, 1.0F);
		Gui.func_146110_a(-12, -13, 0.0F, 0.0F, 24, displayedHeight, 24.0F, 26.0F);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, title(), width / 2, 40, 0xFFFFFF);
		GL11.glPushMatrix();
		GL11.glTranslatef(width / 2.0F, signYOffset(), 0.0F);
		GL11.glPushMatrix();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		mc.getTextureManager().bindTexture(texture);
		drawBackground();
		GL11.glPopMatrix();
		GL11.glScalef(textScale(), textScale(), 1.0F);
		drawLines();
		GL11.glPopMatrix();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	/** 26.3's `extractSignText`: the lines, the cursor (`_` at the end, a bar inside), the selection. */
	private void drawLines() {
		boolean cursorShown = (Minecraft.getSystemTime() - blinkStart) / 300L % 2L == 0L;
		int cursor = field.cursor(), selection = field.selection();
		int middle = Signs.LINES * lineHeight / 2;
		int cursorY = line * lineHeight - middle;
		for (int i = 0; i < Signs.LINES; i++) {
			String text = messages[i];
			int x1 = -fontRendererObj.getStringWidth(text) / 2;
			fontRendererObj.drawString(text, x1, i * lineHeight - middle, textColor);
			if (i == line && cursor >= 0 && cursorShown && cursor >= text.length()) {
				int cursorX = fontRendererObj.getStringWidth(text.substring(0, Math.min(cursor, text.length()))) - fontRendererObj.getStringWidth(text) / 2;
				fontRendererObj.drawString("_", cursorX, cursorY, textColor);
			}
		}
		String text = messages[line];
		int at = Math.max(0, Math.min(cursor, text.length()));
		int cursorX = fontRendererObj.getStringWidth(text.substring(0, at)) - fontRendererObj.getStringWidth(text) / 2;
		if (cursor < text.length() && cursorShown) {
			drawRect(cursorX, cursorY - 1, cursorX + 1, cursorY + lineHeight, 0xFF000000 | textColor);
		}
		if (selection != cursor) {
			int start = Math.min(cursor, selection), end = Math.min(Math.max(cursor, selection), text.length());
			int fromX = fontRendererObj.getStringWidth(text.substring(0, start)) - fontRendererObj.getStringWidth(text) / 2;
			int toX = fontRendererObj.getStringWidth(text.substring(0, end)) - fontRendererObj.getStringWidth(text) / 2;
			highlight(Math.min(fromX, toX), cursorY, Math.max(fromX, toX), cursorY + lineHeight);
		}
	}

	/** 1.7.10's text field's selection (`GuiTextField.drawCursorVertical`): inverted, as 26.3's `textHighlight`. */
	private static void highlight(int x1, int y1, int x2, int y2) {
		Tessellator tessellator = Tessellator.instance;
		GL11.glColor4f(0.0F, 0.0F, 255.0F, 255.0F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
		GL11.glLogicOp(GL11.GL_OR_REVERSE);
		tessellator.startDrawingQuads();
		tessellator.addVertex(x1, y2, 0.0D);
		tessellator.addVertex(x2, y2, 0.0D);
		tessellator.addVertex(x2, y1, 0.0D);
		tessellator.addVertex(x1, y1, 0.0D);
		tessellator.draw();
		GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}
}
