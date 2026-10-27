package ganymedes01.etfuturum.entities;

import ganymedes01.etfuturum.elytra.FallFlying;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * A firework rocket attached to a gliding player (§20). It boosts them while they glide and stays with them until it
 * explodes.
 */
public class EntityBoostingFireworkRocket extends EntityFireworkRocket {
	private static final int BOOSTED_ENTITY_ID = 9;
	private EntityLivingBase boostedEntity;

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataWatcher.addObject(BOOSTED_ENTITY_ID, 0);
	}

	public EntityBoostingFireworkRocket(World world) {
		super(world);
	}

	public EntityBoostingFireworkRocket(World world, ItemStack stack, EntityLivingBase entityToBoost) {
		super(world, entityToBoost.posX, entityToBoost.posY, entityToBoost.posZ, stack);
		this.dataWatcher.updateObject(BOOSTED_ENTITY_ID, entityToBoost.getEntityId());
		this.boostedEntity = entityToBoost;
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		return false;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (this.isAttachedToEntity()) {
			if (this.boostedEntity == null) {
				Entity entity = this.worldObj.getEntityByID(this.dataWatcher.getWatchableObjectInt(BOOSTED_ENTITY_ID));

				if (entity instanceof EntityLivingBase) {
					this.boostedEntity = (EntityLivingBase) entity;
				}
			}

			if (this.boostedEntity != null) {
				if (FallFlying.isFallFlying(this.boostedEntity)) {
					FallFlying.applyRocketBoost(this.boostedEntity);
				}

				this.setPosition(this.boostedEntity.posX, this.boostedEntity.posY, this.boostedEntity.posZ);
				this.motionX = this.boostedEntity.motionX;
				this.motionY = this.boostedEntity.motionY;
				this.motionZ = this.boostedEntity.motionZ;
			}
		}
	}

	public boolean isAttachedToEntity() {
		return this.dataWatcher.getWatchableObjectInt(BOOSTED_ENTITY_ID) > 0;
	}
}
