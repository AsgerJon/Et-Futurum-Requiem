package io.github.asgerjon.mcfly.mixins.early.spectator.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A spectator inside blocks sees past them: the blocks it's in aren't drawn around it (spec §83). */
@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

	@WrapOperation(method = "updateRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getRenderType()I", ordinal = 0))
	private int mcfly$throughBlocks(Block block, Operation<Integer> original) {
		return Spectator.isSpectator(Minecraft.getMinecraft().thePlayer) ? -1 : original.call(block);
	}
}
