package io.github.asgerjon.mcfly.mixins.early.wood.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import io.github.asgerjon.mcfly.wood.Signs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A sign used without an applicator is taken without a swing (SPEC-modern-movement.md 91.8; 26.3's `CONSUME`): 1.7.10
 * swings for every block that takes a right click.
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftSignSwing {

	@Inject(method = "func_147121_ag", at = @At("HEAD"))
	private void mcfly$forgetTheLastSign(CallbackInfo ci) {
		Signs.noSwing = false;
	}

	@WrapWithCondition(method = "func_147121_ag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;swingItem()V"))
	private boolean mcfly$noSwingForASign(EntityClientPlayerMP player) {
		boolean swing = !Signs.noSwing;
		Signs.noSwing = false;
		return swing;
	}
}
