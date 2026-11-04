package ganymedes01.etfuturum.client.model;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.ducks.IAggressive;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGiantZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Items;
import net.minecraft.util.MathHelper;

/**
 * Zombies' and skeletons' arms as 26.3's, by entity, not model (1.7.10's skeleton model is a zombie's, and EFR's stray
 * clothes are a zombie model on a skeleton). Zombies (SPEC-modern-movement.md §63.9; 26.3
 * {@code AnimationUtils.animateZombieArms}, then {@code HumanoidModel}'s arm bob): raised to -pi/2.25, -pi/1.5 when
 * aggressive, not at all for a baby holding something; the attack swing lowers them. Skeletons (§64.1; 26.3
 * {@code SkeletonModel}): the humanoid's arms, aimed with a bow while aggressive, 1.7.10's zombie arms only while
 * aggressive without one.
 */
public final class MobArms {

	private MobArms() {
	}

	/** A skeleton the modern way: its bow aimed while aggressive (1.7.10's {@code aimedBow}, 26.3's BOW_AND_ARROW). */
	public static boolean isModernSkeleton(Entity entity) {
		return McFlyConfig.modernMobArms && entity instanceof EntitySkeleton;
	}

	public static boolean aimsBow(Entity entity) {
		EntityLivingBase living = (EntityLivingBase) entity;
		return isAggressive(entity) && living.getHeldItem() != null && living.getHeldItem().getItem() == Items.bow;
	}

	/** 26.3 raises a skeleton's arms as 1.7.10's zombie only while it's aggressive without a bow. */
	public static boolean raisesLikeZombie(Entity entity) {
		return isAggressive(entity) && !aimsBow(entity);
	}

	public static boolean isAggressive(Entity entity) {
		return entity instanceof IAggressive && ((IAggressive) entity).etfu$isAggressive();
	}

	public static boolean applies(Entity entity) {
		return McFlyConfig.modernMobArms && (entity instanceof EntityZombie || entity instanceof EntityGiantZombie);
	}

	/** At the end of the model's {@code setRotationAngles}: the arms set again, then bobbed. */
	public static void pose(ModelBiped model, Entity entity, float age) {
		if (!applies(entity)) {
			return;
		}
		EntityLivingBase living = (EntityLivingBase) entity;
		boolean raise = !living.isChild() || living.getHeldItem() == null;
		boolean aggressive = isAggressive(entity);
		float raised = raise ? (float) (-Math.PI / (aggressive ? 1.5D : 2.25D)) : 0.0F;
		float swing = model.onGround;
		float s = (raise ? 1.0F : -1.0F) * MathHelper.sin(swing * (float) Math.PI);
		float t = MathHelper.sin((1.0F - (1.0F - swing) * (1.0F - swing)) * (float) Math.PI);
		float x = raised + s * 1.2F - t * 0.4F;
		float y = 0.1F - s * 0.6F;
		model.bipedRightArm.rotateAngleX = x;
		model.bipedRightArm.rotateAngleY = raise ? -y : y;
		model.bipedRightArm.rotateAngleZ = 0.0F;
		model.bipedLeftArm.rotateAngleX = x;
		model.bipedLeftArm.rotateAngleY = raise ? y : -y;
		model.bipedLeftArm.rotateAngleZ = 0.0F;
		float bobZ = MathHelper.cos(age * 0.09F) * 0.05F + 0.05F;
		float bobX = MathHelper.sin(age * 0.067F) * 0.05F;
		model.bipedRightArm.rotateAngleZ += bobZ;
		model.bipedLeftArm.rotateAngleZ -= bobZ;
		model.bipedRightArm.rotateAngleX += bobX;
		model.bipedLeftArm.rotateAngleX -= bobX;
	}
}
