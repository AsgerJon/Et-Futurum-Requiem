package io.github.asgerjon.mcfly.smithing.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.smithing.SmithingMenu;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Collections;

/**
 * The smithing table's screen (SPEC-modern-movement.md §88.4, step C2), 26.3's `SmithingScreen` without the armour
 * stand: its background, the empty slots' hints (the template's icon; once a template is in, the gear's icons in turn
 * every 30 ticks and the ingot's), the error cross while nothing can be made, and the tooltips over them. Without
 * templates, 1.16's screen.
 */
@SideOnly(Side.CLIENT)
public class SmithingScreen extends GuiContainer {

	private static final ResourceLocation OLD = texture("smithing");
	private static final ResourceLocation BACKGROUND = texture("smithing_with_template");
	private static final ResourceLocation ERROR = texture("smithing_error");
	private static final ResourceLocation TEMPLATE_ICON = texture("slot/smithing_template_netherite_upgrade");
	private static final ResourceLocation INGOT_ICON = texture("slot/ingot");
	private static final ResourceLocation[] GEAR_ICONS = {texture("slot/helmet"), texture("slot/sword"), texture("slot/chestplate"),
		texture("slot/pickaxe"), texture("slot/leggings"), texture("slot/axe"), texture("slot/boots"), texture("slot/hoe"), texture("slot/shovel")};
	private static final int ICON_CYCLE_TICKS = 30;

	private final SmithingMenu menu;

	private static ResourceLocation texture(String name) {
		return new ResourceLocation(McFly.DOMAIN, "textures/gui/container/" + name + ".png");
	}

	public SmithingScreen(SmithingMenu menu) {
		super(menu);
		this.menu = menu;
		this.xSize = 176;
		this.ySize = 166;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int left = (width - xSize) / 2;
		int top = (height - ySize) / 2;
		if (!menu.withTemplate) {
			mc.getTextureManager().bindTexture(OLD);
			drawTexturedModalRect(left, top, 0, 0, xSize, ySize);
			if (menu.unable()) {
				drawTexturedModalRect(left + 99, top + 45, xSize, 0, 28, 21);
			}
			return;
		}
		mc.getTextureManager().bindTexture(BACKGROUND);
		drawTexturedModalRect(left, top, 0, 0, xSize, ySize);
		if (!menu.templateSlot.getHasStack()) {
			icon(TEMPLATE_ICON, left + menu.templateSlot.xDisplayPosition, top + menu.templateSlot.yDisplayPosition);
		} else {
			if (!menu.baseSlot.getHasStack()) {
				int cycle = mc.thePlayer == null ? 0 : mc.thePlayer.ticksExisted / ICON_CYCLE_TICKS % GEAR_ICONS.length;
				icon(GEAR_ICONS[cycle], left + menu.baseSlot.xDisplayPosition, top + menu.baseSlot.yDisplayPosition);
			}
			if (!menu.additionSlot.getHasStack()) {
				icon(INGOT_ICON, left + menu.additionSlot.xDisplayPosition, top + menu.additionSlot.yDisplayPosition);
			}
		}
		if (menu.unable()) {
			mc.getTextureManager().bindTexture(ERROR);
			func_146110_a(left + 65, top + 46, 0, 0, 28, 21, 28, 21);
		}
	}

	private void icon(ResourceLocation icon, int x, int y) {
		mc.getTextureManager().bindTexture(icon);
		func_146110_a(x, y, 0, 0, 16, 16, 16, 16);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		if (menu.withTemplate) {
			fontRendererObj.drawString(I18n.format("gui.smithing.upgrade"), 44, 15, 0x404040);
			fontRendererObj.drawString(I18n.format("container.inventory"), 8, ySize - 94, 0x404040);
		} else {
			fontRendererObj.drawString(I18n.format("gui.smithing.upgrade"), 60, 18, 0x000000);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (!menu.withTemplate) {
			return;
		}
		String text = null;
		if (menu.unable() && func_146978_c(65, 46, 28, 21, mouseX, mouseY)) {
			text = I18n.format("gui.smithing.error");
		} else if (overEmpty(menu.templateSlot, mouseX, mouseY)) {
			text = I18n.format("gui.smithing.missing_template");
		} else if (menu.hasTemplate()) {
			if (overEmpty(menu.baseSlot, mouseX, mouseY)) {
				text = I18n.format("item.etfuturum.smithing_template.netherite_upgrade.base_slot_description");
			} else if (overEmpty(menu.additionSlot, mouseX, mouseY)) {
				text = I18n.format("item.etfuturum.smithing_template.netherite_upgrade.additions_slot_description");
			}
		}
		if (text != null) {
			func_146283_a(Collections.singletonList(text), mouseX, mouseY);
		}
	}

	private boolean overEmpty(Slot slot, int mouseX, int mouseY) {
		return !slot.getHasStack() && func_146978_c(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY);
	}
}
