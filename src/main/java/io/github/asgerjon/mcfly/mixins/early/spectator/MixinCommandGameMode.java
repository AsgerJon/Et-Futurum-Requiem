package io.github.asgerjon.mcfly.mixins.early.spectator;

import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.command.CommandGameMode;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.7.10's `/gamemode` knows spectator by its name and short name (spec §83); its number 3 it reads itself. */
@Mixin(CommandGameMode.class)
public class MixinCommandGameMode {

	@Inject(method = "getGameModeFromCommand", at = @At("HEAD"), cancellable = true)
	private void mcfly$spectatorByName(ICommandSender sender, String name, CallbackInfoReturnable<WorldSettings.GameType> cir) {
		if (Spectator.GAME_TYPE != null && ("sp".equalsIgnoreCase(name) || "spectator".equalsIgnoreCase(name))) {
			cir.setReturnValue(Spectator.GAME_TYPE);
		}
	}
}
