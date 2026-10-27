package ganymedes01.etfuturum.mixins.early.offhand.client;

import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.inventory.Container;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The off-hand slot's frame in the inventory screen (SPEC-modern-movement.md §36.1). 1.7.10's inventory texture has
 * no slot there, so the frame is the screen's own armour-slot frame drawn again at modern's place.
 */
@Mixin(GuiInventory.class)
public abstract class MixinGuiInventory extends InventoryEffectRenderer {

	public MixinGuiInventory(Container container) {
		super(container);
	}

	@Inject(method = "drawGuiContainerBackgroundLayer", at = @At("TAIL"))
	private void etfu$offhandSlotFrame(float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
		if (!Offhand.isEnabled()) {
			return;
		}
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(field_147001_a);
		drawTexturedModalRect(this.guiLeft + Offhand.SCREEN_X - 1, this.guiTop + Offhand.SCREEN_Y - 1, 7, 7, 18, 18);
	}
}
