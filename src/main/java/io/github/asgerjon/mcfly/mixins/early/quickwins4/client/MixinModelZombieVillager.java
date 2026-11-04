package io.github.asgerjon.mcfly.mixins.early.quickwins4.client;

import ganymedes01.etfuturum.client.model.MobArms;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelZombieVillager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.7.10's zombie villager model, with its own copy of the zombie's arms: as 26.3's (SPEC-modern-movement.md §63.9). */
@Mixin(ModelZombieVillager.class)
public abstract class MixinModelZombieVillager extends ModelBiped {

	@Inject(method = "setRotationAngles", at = @At("TAIL"))
	private void etfu$modernArms(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
		MobArms.pose(this, entity, age);
	}
}
