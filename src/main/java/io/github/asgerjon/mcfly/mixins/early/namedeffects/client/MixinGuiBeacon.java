package io.github.asgerjon.mcfly.mixins.early.namedeffects.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.potion.EffectNumbers;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.inventory.GuiBeacon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The beacon screen sends the chosen effects as the server's numbers (spec §70.3, stage C). */
@Mixin(GuiBeacon.class)
public abstract class MixinGuiBeacon {

	@WrapOperation(method = "actionPerformed", at = @At(value = "INVOKE", target = "Lio/netty/buffer/ByteBuf;writeInt(I)Lio/netty/buffer/ByteBuf;"))
	private ByteBuf etfu$serverNumber(ByteBuf buffer, int effect, Operation<ByteBuf> original) {
		return original.call(buffer, EffectNumbers.toServer(effect));
	}
}
