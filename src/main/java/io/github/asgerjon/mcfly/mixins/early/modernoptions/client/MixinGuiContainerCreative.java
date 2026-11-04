package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.client.gui.options.CreativeTabOperator;
import ganymedes01.etfuturum.client.options.ControlOptions;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The creative screen (spec §54.12): the Operator Utilities tab is drawn, hovered and clicked only when it is shown,
 * and a screen opening on it while hidden opens on the building blocks instead; its item list scrolls by the wheel
 * through Scroll Sensitivity and Discrete Scrolling.
 */
@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreative {

	private static boolean etfu$hidden(CreativeTabs tab) {
		return tab != null && tab == CreativeTabOperator.tab && !ControlOptions.operatorTabShown();
	}

	@Inject(method = "func_147051_a", at = @At("HEAD"), cancellable = true)
	private void etfu$drawOnlyWhenShown(CreativeTabs tab, CallbackInfo ci) {
		if (etfu$hidden(tab)) {
			ci.cancel();
		}
	}

	@Inject(method = "func_147049_a", at = @At("HEAD"), cancellable = true)
	private void etfu$clickOnlyWhenShown(CreativeTabs tab, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
		if (etfu$hidden(tab)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "renderCreativeInventoryHoveringText", at = @At("HEAD"), cancellable = true)
	private void etfu$nameOnlyWhenShown(CreativeTabs tab, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
		if (etfu$hidden(tab)) {
			cir.setReturnValue(false);
		}
	}

	@ModifyVariable(method = "setCurrentCreativeTab", at = @At("HEAD"), argsOnly = true)
	private CreativeTabs etfu$notTheHiddenTab(CreativeTabs tab) {
		return etfu$hidden(tab) ? CreativeTabs.tabBlock : tab;
	}

	@ModifyExpressionValue(method = "handleMouseInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
	private int etfu$scaledWheel(int wheel) {
		return ControlOptions.CREATIVE.screen(wheel);
	}
}
