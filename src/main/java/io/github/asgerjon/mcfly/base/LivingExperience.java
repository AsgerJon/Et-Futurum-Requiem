package io.github.asgerjon.mcfly.base;

/**
 * 26.3's experience of a living thing's death (`LivingEntity.getExperienceReward`, `shouldDropExperience`,
 * `wasExperienceConsumed`, `skipDropExperience`; SPEC-modern-movement.md §90.9, step H3), mixed into 1.7.10's
 * `EntityLivingBase` by `mixins.early.sculk.MixinEntityLivingBase`: a sculk catalyst takes it as charge, and its orbs
 * then don't drop.
 */
public interface LivingExperience {

	/** 1.7.10's `getExperiencePoints`, for the player that last hit it (or none). */
	int mcfly$experienceReward();

	/** 26.3's `shouldDropExperience`: 1.7.10's `func_146066_aG` (not a child). */
	boolean mcfly$shouldDropExperience();

	boolean mcfly$experienceConsumed();

	/** Its orbs won't drop (26.3's `skipDropExperience`). */
	void mcfly$skipDropExperience();
}
