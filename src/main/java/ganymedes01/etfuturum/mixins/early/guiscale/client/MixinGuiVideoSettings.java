package ganymedes01.etfuturum.mixins.early.guiscale.client;

import ganymedes01.etfuturum.client.gui.IGuiScaleWheel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After the first click on GUI Scale, the mouse wheel cycles the GUI scale until the screen is closed: a click
 * lays the screen out again for the new scale and the button moves away from the cursor, the wheel does not. Wheel
 * events reach a screen two ways in 1.7.10, through the tick's handleMouseInput and through the list's own draw
 * (MixinGuiSlot); both land here, and the change is applied before the next frame as a click would apply it.
 */
@Mixin(GuiVideoSettings.class)
public abstract class MixinGuiVideoSettings extends GuiScreen implements IGuiScaleWheel {
	@Unique
	private boolean etfu$wheelArmed;
	@Unique
	private int etfu$wheelSteps;
	@Unique
	private int etfu$scaleBeforeClick;

	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void etfu$rememberTheScale(int mouseX, int mouseY, int button, CallbackInfo ci) {
		etfu$scaleBeforeClick = this.mc.gameSettings.guiScale;
	}

	@Inject(method = "mouseClicked", at = @At("TAIL"))
	private void etfu$armTheWheel(int mouseX, int mouseY, int button, CallbackInfo ci) {
		if (this.mc.gameSettings.guiScale != etfu$scaleBeforeClick) {
			etfu$wheelArmed = true;
		}
	}

	@Override
	public void handleMouseInput() {
		super.handleMouseInput();
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0 && etfu$wheelArmed) {
			etfu$guiScaleWheel(wheel);
		}
	}

	@Override
	public boolean etfu$wheelChangesGuiScale() {
		return etfu$wheelArmed;
	}

	@Override
	public void etfu$guiScaleWheel(int wheel) {
		etfu$wheelSteps += wheel > 0 ? 1 : -1;
	}

	@Inject(method = "drawScreen", at = @At("HEAD"))
	private void etfu$applyTheWheel(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
		if (etfu$wheelSteps != 0) {
			this.mc.gameSettings.setOptionValue(GameSettings.Options.GUI_SCALE, etfu$wheelSteps);
			etfu$wheelSteps = 0;
			ScaledResolution resolution = new ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
			this.setWorldAndResolution(this.mc, resolution.getScaledWidth(), resolution.getScaledHeight());
		}
	}
}
