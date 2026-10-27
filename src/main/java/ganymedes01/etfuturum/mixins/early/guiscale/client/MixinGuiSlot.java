package ganymedes01.etfuturum.mixins.early.guiscale.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.client.gui.IGuiScaleWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A list drains the wheel events a tick left over, in its draw: on an armed screen they cycle the GUI scale instead of scrolling. */
@Mixin(GuiSlot.class)
public abstract class MixinGuiSlot {
	@ModifyExpressionValue(method = "drawScreen", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
	private int etfu$wheelForTheGuiScale(int wheel) {
		if (wheel != 0 && Minecraft.getMinecraft().currentScreen instanceof IGuiScaleWheel screen && screen.etfu$wheelChangesGuiScale()) {
			screen.etfu$guiScaleWheel(wheel);
			return 0;
		}
		return wheel;
	}
}
