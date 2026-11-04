package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import ganymedes01.etfuturum.client.options.ControlOptions;
import ganymedes01.etfuturum.client.options.ToggleKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Hold/Toggle keys around screens, as 26.3's Gui.setScreen (spec §54.11): when a screen opens (each time, also
 * from another screen) the four are let go and the toggled ones marked, before 1.7.10 lets go of every key; when the
 * screen closes back to a loaded world the marked ones go down again. And the wheel over the game through Scroll
 * Sensitivity and Discrete Scrolling (§54.12).
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

	@Shadow
	public WorldClient theWorld;

	@Inject(method = "displayGuiScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setIngameNotInFocus()V"))
	private void etfu$screenOpens(GuiScreen screen, CallbackInfo ci) {
		ToggleKeys.screenOpened();
	}

	@Inject(method = "displayGuiScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setIngameFocus()V"))
	private void etfu$screenCloses(GuiScreen screen, CallbackInfo ci) {
		if (theWorld != null) {
			ToggleKeys.screenClosed();
		}
	}

	@Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;changeCurrentItem(I)V"))
	private void etfu$scrollTheHotbar(InventoryPlayer inventory, int wheel) {
		ControlOptions.scrollHotbar(inventory, wheel);
	}
}
