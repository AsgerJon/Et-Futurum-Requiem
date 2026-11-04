package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.client.options.OptionsTxt;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.PrintWriter;

/**
 * EFR's client options kept in options.txt with 1.7.10's own (spec §54.10): read after 1.7.10 has read its lines, and
 * written before the file is closed. options.txt is first read when the game starts, after EFR's config, so its values
 * win over the config's.
 */
@Mixin(GameSettings.class)
public abstract class MixinGameSettings {

	@Shadow
	private File optionsFile;

	@Inject(method = "loadOptions", at = @At("TAIL"))
	private void etfu$loadEfrOptions(CallbackInfo ci) {
		OptionsTxt.read(optionsFile);
	}

	@WrapOperation(method = "saveOptions", at = @At(value = "INVOKE", target = "Ljava/io/PrintWriter;close()V", remap = false))
	private void etfu$saveEfrOptions(PrintWriter out, Operation<Void> close) {
		OptionsTxt.write(out);
		close.call(out);
	}
}
