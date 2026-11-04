package io.github.asgerjon.mcfly.mixins.early.inventoryfigure.client;

import ganymedes01.etfuturum.client.ModernInventoryFigure;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The player in the survival inventory where 26.3 draws it (SPEC-modern-movement.md §61.5): in the box 26..75 by 8..78
 * at size 30, centred at 50.5 sideways, the feet at the box's middle (43) plus half the height and 0.0625 times 30, and
 * looking toward the mouse from the box's middle. 1.7.10: centred at 51, feet at 75, looking from (51, 25).
 */
@Mixin(GuiInventory.class)
public abstract class MixinGuiInventory {

	@Redirect(method = "drawGuiContainerBackgroundLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/inventory/GuiInventory;func_147046_a(IIIFFLnet/minecraft/entity/EntityLivingBase;)V"))
	private void etfu$modernFigure(int x, int feet, int size, float yawRef, float pitchRef, EntityLivingBase entity, float partialTicks, int mouseX, int mouseY) {
		// 1.7.10 calls it at (guiLeft + 51, guiTop + 75)
		int guiLeft = x - 51;
		int guiTop = feet - 75;
		float centreX = guiLeft + 50.5F;
		float centreY = guiTop + 43.0F;
		ModernInventoryFigure.draw(centreX, centreY + (entity.height / 2.0F + 0.0625F) * size, size, centreX - mouseX, centreY - mouseY, entity);
	}
}
