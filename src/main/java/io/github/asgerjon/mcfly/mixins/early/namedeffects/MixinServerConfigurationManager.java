package io.github.asgerjon.mcfly.mixins.early.namedeffects;

import io.github.asgerjon.mcfly.network.McFlyNetwork;

import ganymedes01.etfuturum.EtFuturum;
import io.github.asgerjon.mcfly.network.EffectTableMessage;
import ganymedes01.etfuturum.potion.EffectNumbers;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.management.ServerConfigurationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A joining player gets this server's effect numbers by name (spec §70.3, stage C) before the packets of its own
 * effects, which 1.7.10 sends next in this method (FML's PlayerLoggedInEvent would come after them).
 */
@Mixin(ServerConfigurationManager.class)
public abstract class MixinServerConfigurationManager {

	@Inject(method = "initializeConnectionToPlayer", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/player/EntityPlayerMP;getActivePotionEffects()Ljava/util/Collection;"))
	private void etfu$sendEffectTable(NetworkManager manager, EntityPlayerMP player, NetHandlerPlayServer handler, CallbackInfo ci) {
		McFlyNetwork.CHANNEL.sendTo(new EffectTableMessage(EffectNumbers.serverTable()), player);
	}
}
