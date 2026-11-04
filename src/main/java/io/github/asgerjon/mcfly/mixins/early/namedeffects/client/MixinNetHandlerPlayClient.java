package io.github.asgerjon.mcfly.mixins.early.namedeffects.client;

import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.potion.EffectNumbers;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The client takes an effect packet's number whole (1.7.10 read the byte) and as this client's own number for the
 * server's effect of that name (spec §70.3, stage C).
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

	@ModifyArg(method = "handleEntityEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/potion/PotionEffect;<init>(III)V"), index = 0)
	private int etfu$effectGiven(int effect, @Local(argsOnly = true) S1DPacketEntityEffect packet) {
		return EffectNumbers.toClient(EffectNumbers.effectOf(packet));
	}

	@ModifyArg(method = "handleRemoveEntityEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;removePotionEffectClient(I)V"), index = 0)
	private int etfu$effectTaken(int effect) {
		return EffectNumbers.toClient(effect);
	}
}
