package io.github.asgerjon.mcfly.mixins.early.spyglass.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import ganymedes01.etfuturum.client.Spyglass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 26.3's zoom: scoping in first person the field of view is 0.1 of normal, the FOV effect scale aside (§64.5). */
@Mixin(value = EntityPlayerSP.class, priority = 1500)
public abstract class MixinEntityPlayerSP {

	@ModifyReturnValue(method = "getFOVMultiplier", at = @At("RETURN"))
	private float etfu$scopeZoom(float multiplier) {
		return (Object) this == Minecraft.getMinecraft().thePlayer && Spyglass.scopingInFirstPerson() ? 0.1F : multiplier;
	}
}
