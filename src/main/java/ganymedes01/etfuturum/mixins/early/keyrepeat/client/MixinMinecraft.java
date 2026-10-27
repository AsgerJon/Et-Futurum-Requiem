package ganymedes01.etfuturum.mixins.early.keyrepeat.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A held keyboard key bound to Use fires at the keyboard's repeat rate (SPEC-modern-movement.md §35, modern since
 * 1.13, where every key event that isn't a release counts as a click). 1.7.10 keeps keyboard repeat events off
 * outside text screens and counts one press per key-down, so a held key gave the same four-tick rate as a held mouse
 * button. Repeat events are turned on while no screen is open; screens get vanilla's state, so text fields and
 * inventories behave as before. Only repeats of the Use key count: other keys' repeats are dropped, since 1.7.10 has
 * no attack cooldown and toggles such as F5 would flicker.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

	@Shadow
	public GameSettings gameSettings;

	@Shadow
	public GuiScreen currentScreen;

	@Inject(method = "displayGuiScreen", at = @At("HEAD"))
	private void etfu$vanillaRepeatStateForScreens(GuiScreen screen, CallbackInfo ci) {
		Keyboard.enableRepeatEvents(false);
	}

	@Inject(method = "displayGuiScreen", at = @At("RETURN"))
	private void etfu$repeatEventsInGame(GuiScreen screen, CallbackInfo ci) {
		if (currentScreen == null) {
			Keyboard.enableRepeatEvents(true);
		}
	}

	@ModifyExpressionValue(method = "runTick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;next()Z"))
	private boolean etfu$useKeyRepeats(boolean hasEvent) {
		while (hasEvent && Keyboard.isRepeatEvent()) {
			if (currentScreen == null && Keyboard.getEventKeyState() && Keyboard.getEventKey() == gameSettings.keyBindUseItem.getKeyCode()) {
				KeyBinding.onTick(Keyboard.getEventKey());
			}
			hasEvent = Keyboard.next();
		}
		return hasEvent;
	}
}
