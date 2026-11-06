package io.github.asgerjon.mcfly.elytra;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 26.3's firework rocket attached to an entity (`FireworkRocketEntity` with `attachedToEntity`; SPEC-modern-movement.md
 * §20, §85): 1.7.10's rocket, but it stays with the one who used it and, while that one glides, pushes it along its
 * look each tick (`FallFlying.applyRocketBoost`) until it bursts. Which entity it boosts is synced (datawatcher 9: its
 * id plus 1, 0 for none; an id of 0 is real, the run's first entity, often a fresh world's player, which a plain id with
 * 0 for none never boosted; 26.3 keeps an `OptionalInt`); not drawn (26.3 draws it at the hand, which 1.7.10's rocket renderer can't put it in). Under EFR's entity id,
 * `boosting_firework_rocket`.
 */
public class BoostingRocket extends EntityFireworkRocket {

	private static final int BOOSTED = 9;

	private EntityLivingBase boosted;

	public BoostingRocket(World world) {
		super(world);
	}

	public BoostingRocket(World world, ItemStack stack, EntityLivingBase toBoost) {
		super(world, toBoost.posX, toBoost.posY, toBoost.posZ, stack);
		this.dataWatcher.updateObject(BOOSTED, toBoost.getEntityId() + 1);
		this.boosted = toBoost;
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataWatcher.addObject(BOOSTED, 0);
	}

	public boolean isAttached() {
		return this.dataWatcher.getWatchableObjectInt(BOOSTED) > 0;
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		return false;
	}

	/** 1.7.10's rocket tick (its life, its sparks, its burst), then 26.3's attached part: the boost, then with it. */
	@Override
	public void onUpdate() {
		super.onUpdate();
		if (!isAttached()) {
			return;
		}
		if (boosted == null) {
			Entity entity = this.worldObj.getEntityByID(this.dataWatcher.getWatchableObjectInt(BOOSTED) - 1);
			if (entity instanceof EntityLivingBase) {
				boosted = (EntityLivingBase) entity;
			}
		}
		if (boosted != null) {
			if (FallFlying.isFallFlying(boosted)) {
				FallFlying.applyRocketBoost(boosted);
			}
			setPosition(boosted.posX, boosted.posY, boosted.posZ);
			this.motionX = boosted.motionX;
			this.motionY = boosted.motionY;
			this.motionZ = boosted.motionZ;
		}
	}
}
