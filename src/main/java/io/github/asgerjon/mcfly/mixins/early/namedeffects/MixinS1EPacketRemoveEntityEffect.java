package io.github.asgerjon.mcfly.mixins.early.namedeffects;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S1EPacketRemoveEntityEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The packet that takes an effect away carries its number whole (spec §70.3, stage C): 1.7.10's unsigned byte stopped at 255. */
@Mixin(S1EPacketRemoveEntityEffect.class)
public abstract class MixinS1EPacketRemoveEntityEffect {

	@Shadow
	private int field_149079_a;

	@Shadow
	private int field_149078_b;

	@Inject(method = "readPacketData", at = @At("HEAD"), cancellable = true)
	private void etfu$read(PacketBuffer buffer, CallbackInfo ci) {
		field_149079_a = buffer.readInt();
		field_149078_b = buffer.readUnsignedShort();
		ci.cancel();
	}

	@Inject(method = "writePacketData", at = @At("HEAD"), cancellable = true)
	private void etfu$write(PacketBuffer buffer, CallbackInfo ci) {
		buffer.writeInt(field_149079_a);
		buffer.writeShort(field_149078_b);
		ci.cancel();
	}
}
