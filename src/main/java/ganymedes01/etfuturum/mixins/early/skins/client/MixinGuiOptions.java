package ganymedes01.etfuturum.mixins.early.skins.client;

import ganymedes01.etfuturum.client.gui.GuiSkinCustomization;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The "Skin Customization..." button of the options screen, in the slot 1.8 gave it, to the left of 1.7.10's empty row. */
@Mixin(GuiOptions.class)
public abstract class MixinGuiOptions extends GuiScreen {
	@Unique
	private static final int SKIN_CUSTOMISATION = 110;

	@Inject(method = "initGui", at = @At("TAIL"))
	@SuppressWarnings("unchecked")
	private void etfu$skinCustomisationButton(CallbackInfo ci) {
		this.buttonList.add(new GuiButton(SKIN_CUSTOMISATION, this.width / 2 - 155, this.height / 6 + 48 - 6, 150, 20, I18n.format("options.etfuturum.skinCustomisation")));
	}

	@Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
	private void etfu$openSkinCustomisation(GuiButton button, CallbackInfo ci) {
		if (button.enabled && button.id == SKIN_CUSTOMISATION) {
			this.mc.displayGuiScreen(new GuiSkinCustomization((GuiScreen) (Object) this));
			ci.cancel();
		}
	}
}
