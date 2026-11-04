package io.github.asgerjon.mcfly.mixins.early.namedeffects;

import ganymedes01.etfuturum.ducks.IWideEffectPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The packet that gives an entity an effect carries its number whole (SPEC-modern-movement.md §70.3, stage C): 1.7.10
 * kept it in a byte, so past 127 the client got a negative number and crashed ticking it. Sent as an unsigned short.
 */
@Mixin(S1DPacketEntityEffect.class)
public abstract class MixinS1DPacketEntityEffect implements IWideEffectPacket {

	@Shadow
	private int field_149434_a;

	@Shadow
	private byte field_149432_b;

	@Shadow
	private byte field_149433_c;

	@Shadow
	private short field_149431_d;

	@Unique
	private int etfu$effect;

	@Inject(method = "<init>(ILnet/minecraft/potion/PotionEffect;)V", at = @At("RETURN"))
	private void etfu$wide(int entity, PotionEffect effect, CallbackInfo ci) {
		etfu$effect = effect.getPotionID();
	}

	@Inject(method = "readPacketData", at = @At("HEAD"), cancellable = true)
	private void etfu$read(PacketBuffer buffer, CallbackInfo ci) {
		field_149434_a = buffer.readInt();
		etfu$effect = buffer.readUnsignedShort();
		field_149432_b = (byte) etfu$effect;
		field_149433_c = buffer.readByte();
		field_149431_d = buffer.readShort();
		ci.cancel();
	}

	@Inject(method = "writePacketData", at = @At("HEAD"), cancellable = true)
	private void etfu$write(PacketBuffer buffer, CallbackInfo ci) {
		buffer.writeInt(field_149434_a);
		buffer.writeShort(etfu$effect);
		buffer.writeByte(field_149433_c);
		buffer.writeShort(field_149431_d);
		ci.cancel();
	}

	@Override
	public int etfu$effect() {
		return etfu$effect;
	}
}
