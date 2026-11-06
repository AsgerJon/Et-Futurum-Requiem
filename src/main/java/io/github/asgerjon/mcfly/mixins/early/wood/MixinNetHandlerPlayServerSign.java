package io.github.asgerjon.mcfly.mixins.early.wood;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C12PacketUpdateSign;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7.10's own sign update (`C12PacketUpdateSign`) refused for every sign (SPEC-modern-movement.md 91.8): signs are
 * edited by McFly's `SignUpdateMessage` alone, from the player the sign holds as its editor; 1.7.10's editing player is
 * never set any more. Its 15-character limit and `!?` go with it.
 */
@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServerSign {

	@Shadow
	public EntityPlayerMP playerEntity;

	@Inject(method = "processUpdateSign", at = @At("HEAD"), cancellable = true)
	private void mcfly$refuse1_7_10Update(C12PacketUpdateSign packet, CallbackInfo ci) {
		LogManager.getLogger("McFly").warn("Player {} just tried to change non-editable sign", playerEntity.getCommandSenderName());
		ci.cancel();
	}
}
