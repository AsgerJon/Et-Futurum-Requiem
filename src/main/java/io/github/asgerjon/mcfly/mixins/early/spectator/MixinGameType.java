package io.github.asgerjon.mcfly.mixins.early.spectator;

import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A spectator's abilities (26.3's `GameType.updatePlayerAbilities`): flying always, unhurt, editing nothing. */
@Mixin(WorldSettings.GameType.class)
public class MixinGameType {

	@Inject(method = "configurePlayerCapabilities", at = @At("HEAD"), cancellable = true)
	private void mcfly$spectatorAbilities(PlayerCapabilities abilities, CallbackInfo ci) {
		if ((Object) this == Spectator.GAME_TYPE) {
			ci.cancel();
			abilities.allowFlying = true;
			abilities.isFlying = true;
			abilities.isCreativeMode = false;
			abilities.disableDamage = true;
			abilities.allowEdit = false;
		}
	}
}
