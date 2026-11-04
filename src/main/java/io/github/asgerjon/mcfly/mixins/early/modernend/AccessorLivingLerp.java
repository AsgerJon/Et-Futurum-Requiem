package io.github.asgerjon.mcfly.mixins.early.modernend;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.7.10's client-side following of a mob's server places (the dragon's own tick does it, as 1.7.10's did). */
@Mixin(EntityLivingBase.class)
public interface AccessorLivingLerp {

	@Accessor("newPosRotationIncrements")
	int etfu$getNewPosRotationIncrements();

	@Accessor("newPosRotationIncrements")
	void etfu$setNewPosRotationIncrements(int steps);

	@Accessor("newPosX")
	double etfu$getNewPosX();

	@Accessor("newPosY")
	double etfu$getNewPosY();

	@Accessor("newPosZ")
	double etfu$getNewPosZ();

	@Accessor("newRotationYaw")
	double etfu$getNewRotationYaw();

	@Accessor("newRotationPitch")
	double etfu$getNewRotationPitch();
}
