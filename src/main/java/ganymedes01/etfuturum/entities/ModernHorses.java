package ganymedes01.etfuturum.entities;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.entity.passive.EntityHorse;

/**
 * Horses in water and undead horses on leads as 26.3 has them (SPEC-modern-movement.md §64.6; `SkeletonHorse`,
 * `ZombieHorse`, `LivingEntity.travelInWater`): the numbers and sounds the {@code mixins.early.horses} hooks use.
 */
public final class ModernHorses {

	/** 1.7.10's horse types (`EntityHorse.getHorseType`). */
	public static final int ZOMBIE = 3, SKELETON = 4;

	/** 26.3 pushes every mob in water by 0.02 a tick (1.7.10 pushes AI mobs by 0.04). */
	public static final float WATER_PUSH = 0.02F;
	/** The skeleton horse's sideways drag in water (`getWaterSlowDown`); the others keep 1.7.10's (and 26.3's) 0.8. */
	public static final float SKELETON_WATER_DRAG = 0.96F;
	/** Sinking in water by gravity / 16 (1.7.10: 0.02). */
	public static final double WATER_SINKING = 0.08D / 16.0D;
	/** The ridden float: up 0.04 a tick in water deeper than this. */
	public static final double RIDDEN_FLOAT = 0.04D, FLOAT_DEPTH = 0.4D;

	public static final String AMBIENT_WATER = McFly.MC_ASSETS + ":entity.skeleton_horse.ambient_water";
	public static final String SWIM = McFly.MC_ASSETS + ":entity.skeleton_horse.swim";
	public static final String JUMP_WATER = McFly.MC_ASSETS + ":entity.skeleton_horse.jump_water";

	private ModernHorses() {
	}

	public static boolean isSkeletonHorse(Object entity) {
		return entity instanceof EntityHorse horse && horse.getHorseType() == SKELETON;
	}

	/**
	 * The sideways drag in water (1.7.10's 0.8 in `moveEntityWithHeading`'s water branch): the skeleton horse's 0.96.
	 * Asked by whichever hook holds that constant (`modernmovement.MixinEntityLivingBase` with modern swimming, else
	 * `horses.MixinEntityLivingBaseWater`): two `@ModifyConstant` on one constant don't chain, the second is skipped.
	 */
	public static double waterDrag(Object entity, double drag) {
		return McFlyConfig.modernHorsesInWater && isSkeletonHorse(entity) ? SKELETON_WATER_DRAG : drag;
	}

	/** Sinking in water (1.7.10's 0.02): horses sink by 0.005; asked as {@link #waterDrag} is. */
	public static double waterSinking(Object entity, double sinking) {
		return McFlyConfig.modernHorsesInWater && entity instanceof EntityHorse ? WATER_SINKING : sinking;
	}
}
