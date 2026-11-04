package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import ganymedes01.etfuturum.client.options.ToggleKeys;
import net.minecraft.client.entity.EntityClientPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A respawn lets go of the Hold/Toggle keys, as 26.3's LocalPlayer.respawn (spec §54.11). */
@Mixin(EntityClientPlayerMP.class)
public abstract class MixinEntityClientPlayerMP {

	@Inject(method = "respawnPlayer", at = @At("TAIL"))
	private void etfu$letGoOfTheToggleKeys(CallbackInfo ci) {
		ToggleKeys.respawned();
	}
}
