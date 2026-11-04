package io.github.asgerjon.mcfly.mixins.early.coordinatesuggestions;

import ganymedes01.etfuturum.command.CoordinateSuggestions;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandSetBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** /setblock's x, y and z complete with the coordinates of the block the player looks at (CoordinateSuggestions). */
@Mixin(CommandSetBlock.class)
public class MixinCommandSetBlock {

	@Inject(method = "addTabCompletionOptions", at = @At("HEAD"), cancellable = true)
	private void etfu$suggestLookedAtCoordinates(ICommandSender sender, String[] args, CallbackInfoReturnable<List<String>> cir) {
		if (args.length <= 3) {
			cir.setReturnValue(CoordinateSuggestions.blockPosition(sender, args, 0));
		}
	}
}
