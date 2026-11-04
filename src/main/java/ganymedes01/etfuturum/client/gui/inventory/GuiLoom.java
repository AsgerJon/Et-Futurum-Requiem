package ganymedes01.etfuturum.client.gui.inventory;

import io.github.asgerjon.mcfly.dye.DyeColor;
import io.github.asgerjon.mcfly.banner.client.BannerCanvas;
import io.github.asgerjon.mcfly.banner.BannerBlockEntity;
import io.github.asgerjon.mcfly.banner.BannerPattern;
import io.github.asgerjon.mcfly.banner.Banners;
import io.github.asgerjon.mcfly.McFly;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.inventory.ContainerLoom;
import ganymedes01.etfuturum.recipes.ModRecipes;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

/**
 * The loom's screen, as 26.3's `LoomScreen` (SPEC-modern-movement.md §66.5): 26.3's background and sprites, the patterns
 * offered as a 4 by 4 grid of buttons (each pattern's front in white on grey; a scrollbar past 16), the result banner
 * drawn beside it, 26.3's error sprite for a banner of 6 layers.
 */
public class GuiLoom extends GuiContainer {

	private static final ResourceLocation BACKGROUND = texture("loom");
	private static final ResourceLocation BANNER_SLOT = texture("slot/banner");
	private static final ResourceLocation DYE_SLOT = texture("slot/dye");
	private static final ResourceLocation PATTERN_SLOT = texture("slot/banner_pattern");
	private static final ResourceLocation SCROLLER = texture("loom/scroller");
	private static final ResourceLocation SCROLLER_DISABLED = texture("loom/scroller_disabled");
	private static final ResourceLocation PATTERN = texture("loom/pattern");
	private static final ResourceLocation PATTERN_HIGHLIGHTED = texture("loom/pattern_highlighted");
	private static final ResourceLocation PATTERN_SELECTED = texture("loom/pattern_selected");
	private static final ResourceLocation ERROR = texture("loom/error");
	/** 26.3's `DyeColor.GRAY` texture colour, behind each pattern's button picture. */
	private static final int BUTTON_GREY = 0xFF474F52;

	private final ContainerLoom container;
	private boolean displayPatterns;
	private boolean hasMaxPatterns;
	private float scrollOffs;
	private boolean scrolling;
	private int startRow;

	private static ResourceLocation texture(String name) {
		return new net.minecraft.util.ResourceLocation(McFly.DOMAIN + ":textures/gui/container/" + name + ".png");
	}

	public GuiLoom(ContainerLoom container) {
		super(container);
		this.container = container;
		xSize = 176;
		ySize = 166;
		container.setListener(this::containerChanged);
	}

	public int left() {
		return guiLeft;
	}

	public int top() {
		return guiTop;
	}

	private int totalRowCount() {
		return (container.selectable().size() + 3) / 4;
	}

	private boolean isScrollBarActive() {
		return displayPatterns && container.selectable().size() > 16;
	}

	// ---------------------------------------------------------------- drawing

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRendererObj.drawString(ModBlocks.LOOM.get().getLocalizedName(), 8, 4, 0x404040);
		fontRendererObj.drawString(I18n.format("container.inventory"), 8, ySize - 94, 0x404040);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int xo = guiLeft;
		int yo = guiTop;
		mc.getTextureManager().bindTexture(BACKGROUND);
		drawTexturedModalRect(xo, yo, 0, 0, xSize, ySize);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		emptySlotIcon(container.bannerSlot, BANNER_SLOT);
		emptySlotIcon(container.dyeSlot, DYE_SLOT);
		emptySlotIcon(container.patternSlot, PATTERN_SLOT);
		int sy = (int) (41.0F * scrollOffs);
		sprite(isScrollBarActive() ? SCROLLER : SCROLLER_DISABLED, xo + 119, yo + 13 + sy, 12, 15);
		// the result slot as it is now (the server's slot packet changes it without a word to the listener)
		ItemStack result = container.resultSlot.getStack();
		if (result != null && !hasMaxPatterns) {
			drawBanner(result, xo + 141, yo + 8);
		} else if (hasMaxPatterns) {
			sprite(ERROR, xo + container.resultSlot.xDisplayPosition - 5, yo + container.resultSlot.yDisplayPosition - 5, 26, 26);
		}
		if (displayPatterns) {
			int x = xo + 60;
			int y = yo + 13;
			List<BannerPattern> patterns = container.selectable();
			for (int row = 0; row < 4; row++) {
				for (int column = 0; column < 4; column++) {
					int index = (row + startRow) * 4 + column;
					if (index >= patterns.size()) {
						break;
					}
					int posX = x + column * 14;
					int posY = y + row * 14;
					boolean highlighted = mouseX >= posX && mouseY >= posY && mouseX < posX + 14 && mouseY < posY + 14;
					ResourceLocation button = index == container.selected() ? PATTERN_SELECTED : highlighted ? PATTERN_HIGHLIGHTED : PATTERN;
					sprite(button, posX, posY, 14, 14);
					drawPatternOnButton(patterns.get(index), posX, posY);
				}
			}
		}
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		// the hovered pattern's name in the dye's colour (26.3's tooltip)
		if (displayPatterns && mc.thePlayer.inventory.getItemStack() == null) {
			int index = patternAt(mouseX, mouseY);
			if (index >= 0) {
				int colour = Math.max(0, Banners.dyeColour(container.dyeSlot.getStack()));
				String key = "item.banner." + container.selectable().get(index).name() + "." + ModRecipes.dye_names[colour];
				func_146283_a(Collections.singletonList(StatCollector.translateToLocal(key)), mouseX, mouseY);
			}
		}
	}

	private void emptySlotIcon(Slot slot, ResourceLocation icon) {
		if (!slot.getHasStack()) {
			sprite(icon, guiLeft + slot.xDisplayPosition, guiTop + slot.yDisplayPosition, 16, 16);
		}
	}

	private void sprite(ResourceLocation texture, int x, int y, int width, int height) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(texture);
		func_146110_a(x, y, 0, 0, width, height, width, height);
	}

	/** A pattern's front (26.3: 21 by 40 texels from (0, 1) of its 64 by 64 texture) in 5 by 10 pixels, on grey. */
	private void drawPatternOnButton(BannerPattern pattern, int posX, int posY) {
		int x = posX + 4;
		int y = posY + 2;
		Gui.drawRect(x, y, x + 5, y + 10, BUTTON_GREY);
		// 26.3's pattern texture over the grey, as it is (26.3 blits it untinted, spec §66.11)
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(new ResourceLocation(pattern.texture()));
		quad(x, y, 5, 10, 0.0, 1.0 / 64.0, 21.0 / 64.0, 41.0 / 64.0);
	}

	/** The result banner's front (its base and layers) in 20 by 40 pixels: the banner texture's face, 20 by 40 from (1, 1). */
	private void drawBanner(ItemStack banner, int x, int y) {
		BannerBlockEntity preview = new BannerBlockEntity();
		preview.setItemValues(banner);
		ResourceLocation canvas = BannerCanvas.of(preview);
		if (canvas == null) {
			return;
		}
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(canvas);
		quad(x, y, 20, 40, 1.0 / 64.0, 1.0 / 64.0, 21.0 / 64.0, 41.0 / 64.0);
	}

	private void quad(int x, int y, int width, int height, double u0, double v0, double u1, double v1) {
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(x, y + height, zLevel, u0, v1);
		tessellator.addVertexWithUV(x + width, y + height, zLevel, u1, v1);
		tessellator.addVertexWithUV(x + width, y, zLevel, u1, v0);
		tessellator.addVertexWithUV(x, y, zLevel, u0, v0);
		tessellator.draw();
	}

	// ---------------------------------------------------------------- the mouse

	/** The offered pattern's index under the mouse, or -1. */
	private int patternAt(int mouseX, int mouseY) {
		int x = guiLeft + 60;
		int y = guiTop + 13;
		for (int row = 0; row < 4; row++) {
			for (int column = 0; column < 4; column++) {
				int dx = mouseX - (x + column * 14);
				int dy = mouseY - (y + row * 14);
				int index = (row + startRow) * 4 + column;
				if (dx >= 0 && dy >= 0 && dx < 14 && dy < 14 && index < container.selectable().size()) {
					return index;
				}
			}
		}
		return -1;
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (displayPatterns) {
			int index = patternAt(mouseX, mouseY);
			if (index >= 0 && container.enchantItem(mc.thePlayer, index)) {
				mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation(McFly.MC_ASSETS + ":ui.loom.select_pattern"), 1.0F));
				mc.playerController.sendEnchantPacket(container.windowId, index);
				return;
			}
			int xo = guiLeft + 119;
			int yo = guiTop + 9;
			if (mouseX >= xo && mouseX < xo + 12 && mouseY >= yo && mouseY < yo + 56) {
				scrolling = true;
			}
		}
		super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int button, long time) {
		int offscreenRows = totalRowCount() - 4;
		if (scrolling && displayPatterns && offscreenRows > 0) {
			int top = guiTop + 13;
			int bottom = top + 56;
			scrollOffs = Math.max(0.0F, Math.min(1.0F, (mouseY - top - 7.5F) / (bottom - top - 15.0F)));
			startRow = Math.max((int) (scrollOffs * offscreenRows + 0.5), 0);
			return;
		}
		super.mouseClickMove(mouseX, mouseY, button, time);
	}

	@Override
	protected void mouseMovedOrUp(int mouseX, int mouseY, int which) {
		if (which != -1) {
			scrolling = false;
		}
		super.mouseMovedOrUp(mouseX, mouseY, which);
	}

	@Override
	public void handleMouseInput() {
		super.handleMouseInput();
		int wheel = Mouse.getEventDWheel();
		int offscreenRows = totalRowCount() - 4;
		if (wheel != 0 && displayPatterns && offscreenRows > 0) {
			float delta = Math.signum(wheel) / (float) offscreenRows;
			scrollOffs = Math.max(0.0F, Math.min(1.0F, scrollOffs - delta));
			startRow = Math.max((int) (scrollOffs * offscreenRows + 0.5F), 0);
		}
	}

	/** 26.3's `containerChanged`: what to show, from the slots. */
	private void containerChanged() {
		ItemStack banner = container.bannerSlot.getStack();
		hasMaxPatterns = banner != null && Banners.count(banner) >= Banners.MAX_LAYERS;
		displayPatterns = banner != null && container.dyeSlot.getHasStack() && !hasMaxPatterns && !container.selectable().isEmpty();
		if (startRow >= totalRowCount()) {
			startRow = 0;
			scrollOffs = 0.0F;
		}
	}
}
