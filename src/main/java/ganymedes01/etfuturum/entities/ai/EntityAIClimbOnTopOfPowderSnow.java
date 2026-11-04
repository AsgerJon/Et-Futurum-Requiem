package ganymedes01.etfuturum.entities.ai;

import ganymedes01.etfuturum.entities.Freezing;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.MathHelper;

/**
 * 26.3's {@code ClimbOnTopOfPowderSnowGoal} (SPEC-modern-movement.md §62.4), for EFR's fox and rabbit: a walker in
 * powder snow (this tick or last) jumps every tick while the block above it is powder snow or has no collision, so the
 * climb (0.2 a tick, {@code powdersnow.MixinEntityLivingBase}) takes it out on top. Jump only (1.7.10's mutex 4).
 */
public class EntityAIClimbOnTopOfPowderSnow extends EntityAIBase {

	private final EntityLiving mob;

	public EntityAIClimbOnTopOfPowderSnow(EntityLiving mob) {
		this.mob = mob;
		setMutexBits(4);
	}

	@Override
	public boolean shouldExecute() {
		if (!(Freezing.isInPowderSnow(mob) || Freezing.wasInPowderSnow(mob)) || !Freezing.canWalkOnPowderSnow(mob)) {
			return false;
		}
		int x = MathHelper.floor_double(mob.posX);
		int y = MathHelper.floor_double(mob.posY) + 1;
		int z = MathHelper.floor_double(mob.posZ);
		Block above = mob.worldObj.getBlock(x, y, z);
		return Freezing.isPowderSnow(above) || above.getCollisionBoundingBoxFromPool(mob.worldObj, x, y, z) == null;
	}

	@Override
	public void updateTask() {
		mob.getJumpHelper().setJumping();
		// the jump as 26.3's travel sees it: EFR's fox and rabbit jump through helpers of their own (the rabbit's hop)
		((ganymedes01.etfuturum.ducks.IFreezingEntity) mob).etfu$requestClimb();
	}
}
