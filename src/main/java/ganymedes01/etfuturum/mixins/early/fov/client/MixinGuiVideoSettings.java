package ganymedes01.etfuturum.mixins.early.fov.client;

import ganymedes01.etfuturum.client.gui.GuiFovEffectsSlider;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiOptionsRowList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The FOV Effects slider as one more row of the video settings list (modern keeps it with the accessibility settings). */
@Mixin(GuiVideoSettings.class)
public abstract class MixinGuiVideoSettings extends GuiScreen {
	@Shadow
	private GuiListExtended optionsRowList;

	@Inject(method = "initGui", at = @At("TAIL"))
	private void etfu$fovEffectsRow(CallbackInfo ci) {
		if (optionsRowList instanceof GuiOptionsRowList list) {
			((AccessorGuiOptionsRowList) list).etfu$rows().add(new GuiOptionsRowList.Row(new GuiFovEffectsSlider(1000, this.width / 2 - 155, 0), null));
		}
	}
}
