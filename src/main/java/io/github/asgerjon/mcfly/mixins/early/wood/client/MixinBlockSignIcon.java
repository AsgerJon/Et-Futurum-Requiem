package io.github.asgerjon.mcfly.mixins.early.wood.client;

import io.github.asgerjon.mcfly.wood.Signs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.client.renderer.texture.IIconRegister;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A sign's wood's 26.3 block texture (`block/<wood>_sign`), for its models (SPEC-modern-movement.md 91.8). */
@Mixin(BlockSign.class)
public abstract class MixinBlockSignIcon {

	@Inject(method = "registerBlockIcons", at = @At("HEAD"))
	private void mcfly$signTexture(IIconRegister register, CallbackInfo ci) {
		((Signs.Kind) this).mcfly$setSignIcon(register.registerIcon(Signs.blockTexture((Block) (Object) this)));
	}
}
