package ganymedes01.etfuturum.client.gui.inventory;

import ganymedes01.etfuturum.inventory.ContainerSmithingTable;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.core.utils.Utils;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Collections;

/**
 * The smithing table's screen: 1.16's layout, or modern's with the netherite upgrade template on, with its empty-slot
 * hints (the template icon, then the gear and ingot icons once a template is in, the gear cycling every 30 ticks),
 * its error cross and the tooltips over them.
 */
public class GuiSmithingTable extends GuiContainer {
	public static final ResourceLocation texturePath = Utils.getResource(Reference.MOD_ID + ":textures/gui/container/smithing.png");
	public static final ResourceLocation templateTexturePath = Utils.getResource(Reference.MOD_ID + ":textures/gui/container/smithing_with_template.png");
	private static final ResourceLocation ERROR = Utils.getResource(Reference.MOD_ID + ":textures/gui/container/smithing_error.png");
	private static final ResourceLocation TEMPLATE_ICON = slotIcon("smithing_template_netherite_upgrade");
	private static final ResourceLocation INGOT_ICON = slotIcon("ingot");
	private static final ResourceLocation[] GEAR_ICONS = {slotIcon("helmet"), slotIcon("sword"), slotIcon("chestplate"), slotIcon("pickaxe"),
			slotIcon("leggings"), slotIcon("axe"), slotIcon("boots"), slotIcon("hoe"), slotIcon("shovel")};
	private static final int ICON_CYCLE_TICKS = 30;
	public final ContainerSmithingTable container;

	private static ResourceLocation slotIcon(String name) {
		return Utils.getResource(Reference.MOD_ID + ":textures/gui/container/slot/" + name + ".png");
	}

	public GuiSmithingTable(ContainerSmithingTable container) {
		super(container);
		this.inventorySlots = container;
		this.container = container;
		this.xSize = 176;
		this.ySize = 166;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		final int offsetX = (this.width - this.xSize) / 2;
		final int offsetY = (this.height - this.ySize) / 2;
		if (!container.withTemplate) {
			mc.getTextureManager().bindTexture(texturePath);
			drawTexturedModalRect(offsetX, offsetY, 0, 0, xSize, ySize);
			if (container.unable()) drawTexturedModalRect(offsetX + 99, offsetY + 45, xSize, 0, 28, 21);
			return;
		}
		mc.getTextureManager().bindTexture(templateTexturePath);
		drawTexturedModalRect(offsetX, offsetY, 0, 0, xSize, ySize);
		if (!container.templateSlot.getHasStack()) {
			drawIcon(TEMPLATE_ICON, offsetX + container.templateSlot.xDisplayPosition, offsetY + container.templateSlot.yDisplayPosition);
		} else {
			if (!container.applicant.getHasStack()) {
				int cycle = mc.thePlayer == null ? 0 : mc.thePlayer.ticksExisted / ICON_CYCLE_TICKS % GEAR_ICONS.length;
				drawIcon(GEAR_ICONS[cycle], offsetX + container.applicant.xDisplayPosition, offsetY + container.applicant.yDisplayPosition);
			}
			if (!container.ingot.getHasStack()) {
				drawIcon(INGOT_ICON, offsetX + container.ingot.xDisplayPosition, offsetY + container.ingot.yDisplayPosition);
			}
		}
		if (container.unable()) {
			mc.getTextureManager().bindTexture(ERROR);
			func_146110_a(offsetX + 65, offsetY + 46, 0, 0, 28, 21, 28, 21);
		}
	}

	private void drawIcon(ResourceLocation icon, int x, int y) {
		mc.getTextureManager().bindTexture(icon);
		func_146110_a(x, y, 0, 0, 16, 16, 16, 16);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		if (container.withTemplate) {
			fontRendererObj.drawString(I18n.format("gui.smithing.upgrade"), 44, 15, 0x404040);
			fontRendererObj.drawString(I18n.format("container.inventory"), 8, ySize - 94, 0x404040);
		} else {
			fontRendererObj.drawString(I18n.format("gui.smithing.upgrade"), 60, 18, 0x000000);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (!container.withTemplate) return;
		String text = null;
		if (container.unable() && func_146978_c(65, 46, 28, 21, mouseX, mouseY)) {
			text = I18n.format("gui.smithing.error");
		} else if (overEmptySlot(container.templateSlot, mouseX, mouseY)) {
			text = I18n.format("gui.smithing.missing_template");
		} else if (container.hasTemplate()) {
			if (overEmptySlot(container.applicant, mouseX, mouseY)) {
				text = I18n.format("item.etfuturum.smithing_template.netherite_upgrade.base_slot_description");
			} else if (overEmptySlot(container.ingot, mouseX, mouseY)) {
				text = I18n.format("item.etfuturum.smithing_template.netherite_upgrade.additions_slot_description");
			}
		}
		if (text != null) {
			func_146283_a(Collections.singletonList(text), mouseX, mouseY);
		}
	}

	private boolean overEmptySlot(Slot slot, int mouseX, int mouseY) {
		return !slot.getHasStack() && func_146978_c(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY);
	}
}
