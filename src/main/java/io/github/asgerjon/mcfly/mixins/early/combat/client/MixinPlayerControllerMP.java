package io.github.asgerjon.mcfly.mixins.early.combat.client;

import ganymedes01.etfuturum.combat.ICombatPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The client's own charge restarts after an attack, and when it stops breaking a block before it breaks (§46.1). */
@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

	@Shadow
	@Final
	private Minecraft mc;

	@Shadow
	public boolean isHittingBlock;

	@Inject(method = "attackEntity", at = @At("TAIL"))
	private void etfu$attackRestarts(EntityPlayer player, Entity target, CallbackInfo ci) {
		((ICombatPlayer) player).etfu$resetAttackStrength();
	}

	@Inject(method = "resetBlockRemoving", at = @At("HEAD"))
	private void etfu$abortRestarts(CallbackInfo ci) {
		if (isHittingBlock && mc.thePlayer != null) {
			((ICombatPlayer) mc.thePlayer).etfu$resetAttackStrength();
		}
	}
}
