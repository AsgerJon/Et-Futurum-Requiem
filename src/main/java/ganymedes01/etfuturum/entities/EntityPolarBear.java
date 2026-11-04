package ganymedes01.etfuturum.entities;

import io.github.asgerjon.mcfly.McFly;
import ganymedes01.etfuturum.spectator.SpectatorMode;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The polar bear (SPEC-modern-movement.md §60), the first mob this fork adds: a neutral animal of the snowy plains and
 * frozen oceans. It fights whoever hurts it (and the adults about join in), mothers go for a player near their cub,
 * adults hunt foxes, cubs panic. Before a swipe it stands up with a roar; it swims as 26.3's mobs do, with little drag.
 * 26.3's goals are written here as 1.7.10 tasks.
 */
public class EntityPolarBear extends EntityAnimal {

	private static final int STANDING_WATCHER = 16;
	private static final float WIDTH = 1.4F, HEIGHT = 1.4F, CUB_EYES = 0.34375F;
	/** The stand animation's length on a client, in ticks. */
	private static final float STAND_TICKS = 6.0F;
	/** 26.3's reach for a mob's melee: its box widened by sqrt(2.04) - 0.6 sideways. */
	private static final double ATTACK_REACH = Math.sqrt(2.04D) - 0.6D;
	/** Damage that sends a cub running (26.3's `panic_causes`; the one list, spec §63.4). */
	private static final Set<String> PANIC_CAUSES = PanicCauses.ALL;
	/** Damage that sends an adult running (26.3's `panic_environmental_causes`). */
	private static final Set<String> ENVIRONMENTAL_CAUSES = PanicCauses.ENVIRONMENTAL;

	private float standAnimationPrev, standAnimation;
	private int warningSoundTicks;
	/** The world time its anger ends, or -1 (26.3's `anger_end_time`). */
	private long angerEndTime = -1L;
	/** Whom it is angry at (26.3's `angry_at`). */
	private UUID angryAt;
	/** The last damage that landed and when: the panic reads it for 40 ticks, as 26.3's last damage source. */
	private DamageSource lastDamage;
	private long lastDamageTime;
	/** What killed it: a Fire Aspect weapon cooks its fish. */
	private DamageSource deathSource;

	public EntityPolarBear(World world) {
		super(world);
		setSize(WIDTH, HEIGHT);
		tasks.addTask(0, new EntityAISwimming(this));
		tasks.addTask(1, new AIAttack());
		tasks.addTask(1, new AIPanic());
		tasks.addTask(4, new EntityAIFollowParent(this, 1.25D));
		tasks.addTask(5, new EntityAIWander(this, 1.0D));
		tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
		tasks.addTask(7, new EntityAILookIdle(this));
		targetTasks.addTask(1, new AIHurtByTarget());
		targetTasks.addTask(2, new AIDefendCubs());
		targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 10, true, false,
			entity -> entity instanceof EntityLivingBase living && isAngryAt(living)));
		targetTasks.addTask(4, new EntityAINearestAttackableTarget(this, EntityFox.class, 10, true, true, entity -> !isChild()));
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(30.0D);
		getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(20.0D);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.25D);
		getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(6.0D);
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		dataWatcher.addObject(STANDING_WATCHER, (byte) 0);
	}

	@Override
	public boolean isAIEnabled() {
		return true;
	}

	// ---------------------------------------------------------------- breeding: none

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return false;
	}

	@Override
	public EntityAgeable createChild(EntityAgeable mate) {
		return new EntityPolarBear(worldObj);
	}

	/** A spawned group's first bear is an adult, every further one a cub (26.3's baby chance of 1). */
	@Override
	public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
		super.onSpawnWithEgg(data);
		if (data instanceof GroupData) {
			setGrowingAge(-24000);
			return data;
		}
		return new GroupData();
	}

	/**
	 * Its box kept about its position when its size changes for age (26.3's): 1.7.10 shrinks it from a corner, so a
	 * client that first met a cub at adult size drifted it by 0.35 blocks.
	 */
	@Override
	public void setScaleForAge(boolean child) {
		super.setScaleForAge(child);
		setPosition(posX, posY, posZ);
	}

	/** Marks a spawned group after its first bear. */
	public static final class GroupData implements IEntityLivingData {
	}

	// ---------------------------------------------------------------- standing

	public boolean isStanding() {
		return dataWatcher.getWatchableObjectByte(STANDING_WATCHER) != 0;
	}

	public void setStanding(boolean standing) {
		dataWatcher.updateObject(STANDING_WATCHER, (byte) (standing ? 1 : 0));
	}

	/** How far the stand animation is, 0 to 1, between ticks (on a client). */
	public float getStandingAnimationScale(float partialTicks) {
		return (standAnimationPrev + (standAnimation - standAnimationPrev) * partialTicks) / STAND_TICKS;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (worldObj.isRemote) {
			standAnimationPrev = standAnimation;
			standAnimation = MathHelper.clamp_float(standAnimation + (isStanding() ? 1.0F : -1.0F), 0.0F, STAND_TICKS);
			// 26.3's: while it stands, its box on this client is up to twice as tall (the server's never changes)
			float tall = (isChild() ? HEIGHT * 0.5F : HEIGHT) * (1.0F + standAnimation / STAND_TICKS);
			if (tall != height) {
				height = tall;
				boundingBox.maxY = boundingBox.minY + height;
			}
		}
		if (warningSoundTicks > 0) {
			warningSoundTicks--;
		}
		if (!worldObj.isRemote) {
			updateAnger();
		}
	}

	@Override
	public float getEyeHeight() {
		float eyes = isChild() ? CUB_EYES : HEIGHT * 0.85F;
		return worldObj != null && worldObj.isRemote ? eyes * (1.0F + standAnimation / STAND_TICKS) : eyes;
	}

	// ---------------------------------------------------------------- anger (26.3's NeutralMob)

	private void updateAnger() {
		EntityLivingBase target = getAttackTarget();
		if (target != null && target.getHealth() <= 0.0F && target.getUniqueID().equals(angryAt) && target instanceof EntityLiving) {
			stopBeingAngry();
			return;
		}
		if (target != null) {
			angryAt = target.getUniqueID();
			// it stays angry while its target is there
			angerEndTime = worldObj.getTotalWorldTime() + 400 + rand.nextInt(381);
		}
		if (angryAt != null && !isAngry() && (target == null || !isValidPlayerTarget(target))) {
			stopBeingAngry();
		}
		EntityPlayer angryAtPlayer = angryAt == null ? null : worldObj.func_152378_a(angryAt);
		if (angryAtPlayer != null && !isValidPlayerTarget(angryAtPlayer)) {
			stopBeingAngry();
		}
	}

	public boolean isAngry() {
		return angerEndTime > 0L && angerEndTime - worldObj.getTotalWorldTime() > 0L;
	}

	public boolean isAngryAt(EntityLivingBase entity) {
		return canAttackClass(entity.getClass()) && entity.getUniqueID().equals(angryAt);
	}

	private void stopBeingAngry() {
		setRevengeTarget(null);
		angryAt = null;
		setAttackTarget(null);
		angerEndTime = -1L;
	}

	/** A player it may go for: not in creative, not spectating, not on Peaceful. */
	private boolean isValidPlayerTarget(EntityLivingBase entity) {
		return entity instanceof EntityPlayer player && !player.capabilities.isCreativeMode && !SpectatorMode.isSpectator(player)
			&& worldObj.difficultySetting != EnumDifficulty.PEACEFUL;
	}

	/** Anything else it may target; a player only as above. */
	private boolean mayTarget(EntityLivingBase entity) {
		return !(entity instanceof EntityPlayer) || isValidPlayerTarget(entity);
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound tag) {
		super.writeEntityToNBT(tag);
		tag.setLong("anger_end_time", angerEndTime);
		if (angryAt != null) {
			long most = angryAt.getMostSignificantBits(), least = angryAt.getLeastSignificantBits();
			tag.setIntArray("angry_at", new int[]{(int) (most >> 32), (int) most, (int) (least >> 32), (int) least});
		}
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound tag) {
		super.readEntityFromNBT(tag);
		if (tag.hasKey("anger_end_time")) {
			angerEndTime = tag.getLong("anger_end_time");
		} else if (tag.hasKey("AngerTime")) {
			angerEndTime = worldObj.getTotalWorldTime() + tag.getInteger("AngerTime");
		} else {
			angerEndTime = -1L;
		}
		int[] uuid = tag.getIntArray("angry_at");
		angryAt = uuid.length == 4 ? new UUID((long) uuid[0] << 32 | uuid[1] & 0xFFFFFFFFL, (long) uuid[2] << 32 | uuid[3] & 0xFFFFFFFFL) : null;
		if (angryAt != null && worldObj != null && !worldObj.isRemote) {
			for (Object loaded : worldObj.loadedEntityList) {
				if (loaded instanceof EntityLivingBase living && angryAt.equals(living.getUniqueID())) {
					setAttackTarget(living);
					break;
				}
			}
		}
	}

	// ---------------------------------------------------------------- fighting

	@Override
	public boolean attackEntityAsMob(Entity target) {
		setLastAttacker(target);
		return target.attackEntityFrom(DamageSource.causeMobDamage(this), (float) getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue());
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		boolean hurt = super.attackEntityFrom(source, amount);
		if (hurt && !worldObj.isRemote) {
			lastDamage = source;
			lastDamageTime = worldObj.getTotalWorldTime();
		}
		return hurt;
	}

	private boolean inReach(EntityLivingBase target) {
		return boundingBox.expand(ATTACK_REACH, 0.0D, ATTACK_REACH).intersectsWith(target.boundingBox);
	}

	private void playWarningSound() {
		if (warningSoundTicks <= 0) {
			playSound(McFly.MC_ASSETS + ":entity.polar_bear.warning", getSoundVolume(), getSoundPitch());
			warningSoundTicks = 40;
		}
	}

	private boolean shouldPanic() {
		return lastDamage != null && worldObj.getTotalWorldTime() - lastDamageTime <= 40L
			&& (isChild() ? PANIC_CAUSES : ENVIRONMENTAL_CAUSES).contains(lastDamage.getDamageType());
	}

	// ---------------------------------------------------------------- water

	/** 26.3's swimming for a mob, with the polar bear's 0.98 sideways (1.7.10 mobs push with 0.04 and keep 0.8). */
	@Override
	public void moveEntityWithHeading(float strafe, float forward) {
		if (!isInWater()) {
			super.moveEntityWithHeading(strafe, forward);
			return;
		}
		boolean falling = motionY <= 0.0D;
		double startY = posY;
		moveFlying(strafe, forward, 0.02F);
		moveEntity(motionX, motionY, motionZ);
		if (isCollidedHorizontally && isOnLadder()) {
			motionY = 0.2D;
		}
		motionX *= 0.98D;
		motionY *= 0.8D;
		motionZ *= 0.98D;
		// sinking by gravity / 16, or a near hover at the top
		double gravity = 0.08D;
		motionY = falling && Math.abs(motionY - 0.005D) >= 0.003D && Math.abs(motionY - gravity / 16.0D) < 0.003D ? -0.003D : motionY - gravity / 16.0D;
		if (isCollidedHorizontally && isOffsetPositionInLiquid(motionX, motionY + 0.6D - posY + startY, motionZ)) {
			motionY = 0.3D;
		}
		// 1.7.10's limb swing, as its own moveEntityWithHeading ends
		prevLimbSwingAmount = limbSwingAmount;
		double dx = posX - prevPosX;
		double dz = posZ - prevPosZ;
		float swing = MathHelper.sqrt_double(dx * dx + dz * dz) * 4.0F;
		if (swing > 1.0F) {
			swing = 1.0F;
		}
		limbSwingAmount += (swing - limbSwingAmount) * 0.4F;
		limbSwing += limbSwingAmount;
	}

	// ---------------------------------------------------------------- spawning

	/** In a frozen ocean it spawns on ice in light above 8; elsewhere as any animal (grass, light above 8). */
	@Override
	public boolean getCanSpawnHere() {
		int x = MathHelper.floor_double(posX);
		int y = MathHelper.floor_double(boundingBox.minY);
		int z = MathHelper.floor_double(posZ);
		if (worldObj.getBiomeGenForCoords(x, z) != BiomeGenBase.frozenOcean) {
			return super.getCanSpawnHere();
		}
		return worldObj.getBlock(x, y - 1, z) == Blocks.ice && worldObj.getFullBlockLightValue(x, y, z) > 8
			&& worldObj.checkNoEntityCollision(boundingBox) && worldObj.getCollidingBoundingBoxes(this, boundingBox).isEmpty()
			&& !worldObj.isAnyLiquid(boundingBox);
	}

	// ---------------------------------------------------------------- loot and sounds

	@Override
	public void onDeath(DamageSource source) {
		deathSource = source;
		super.onDeath(source);
	}

	/** One roll: raw cod (3 in 4) or raw salmon, 0 to 2 and up to 1 more a Looting level; cooked if it burned or the weapon has Fire Aspect. */
	@Override
	protected void dropFewItems(boolean recentlyHit, int looting) {
		boolean salmon = rand.nextInt(4) == 0;
		int count = rand.nextInt(3) + Math.round(rand.nextFloat() * looting);
		boolean fireAspect = deathSource != null && deathSource.getSourceOfDamage() instanceof EntityLivingBase killer
			&& EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, killer.getHeldItem()) > 0;
		if (count > 0) {
			entityDropItem(new ItemStack(isBurning() || fireAspect ? Items.cooked_fished : Items.fish, count, salmon ? 1 : 0), 0.0F);
		}
	}

	@Override
	protected String getLivingSound() {
		return McFly.MC_ASSETS + ":entity.polar_bear." + (isChild() ? "ambient_baby" : "ambient");
	}

	@Override
	protected String getHurtSound() {
		return McFly.MC_ASSETS + ":entity.polar_bear.hurt";
	}

	@Override
	protected String getDeathSound() {
		return McFly.MC_ASSETS + ":entity.polar_bear.death";
	}

	@Override
	protected void func_145780_a(int x, int y, int z, Block block) {
		playSound(McFly.MC_ASSETS + ":entity.polar_bear.step", 0.15F, 1.0F);
	}

	// ---------------------------------------------------------------- the tasks

	/**
	 * 26.3's melee attack with the polar bear's stand: after its target at 1.25 times its speed, also when it can't see
	 * it; a hit when its cooldown is over and the target in reach; close but out of reach, it stands with its roar in
	 * the last 10 ticks before each try.
	 */
	private final class AIAttack extends EntityAIBase {
		private PathEntity path;
		private double pathedX, pathedY, pathedZ;
		private int ticksToRecalculate, ticksToAttack;
		private long lastCanUseCheck;

		AIAttack() {
			setMutexBits(3);
		}

		@Override
		public boolean shouldExecute() {
			long time = worldObj.getTotalWorldTime();
			if (time - lastCanUseCheck < 20L) {
				return false;
			}
			lastCanUseCheck = time;
			EntityLivingBase target = getAttackTarget();
			if (target == null || !target.isEntityAlive()) {
				return false;
			}
			path = getNavigator().getPathToEntityLiving(target);
			return path != null || inReach(target);
		}

		@Override
		public boolean continueExecuting() {
			EntityLivingBase target = getAttackTarget();
			if (target == null || !target.isEntityAlive()) {
				return false;
			}
			if (!isWithinHomeDistance(MathHelper.floor_double(target.posX), MathHelper.floor_double(target.posY), MathHelper.floor_double(target.posZ))) {
				return false;
			}
			return !(target instanceof EntityPlayer player && (player.capabilities.isCreativeMode || SpectatorMode.isSpectator(player)));
		}

		@Override
		public void startExecuting() {
			getNavigator().setPath(path, 1.25D);
			ticksToRecalculate = 0;
			ticksToAttack = 0;
		}

		@Override
		public void resetTask() {
			EntityLivingBase target = getAttackTarget();
			if (target instanceof EntityPlayer player && (player.capabilities.isCreativeMode || SpectatorMode.isSpectator(player))) {
				setAttackTarget(null);
			}
			getNavigator().clearPathEntity();
			setStanding(false);
		}

		@Override
		public void updateTask() {
			EntityLivingBase target = getAttackTarget();
			if (target == null) {
				return;
			}
			getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
			ticksToRecalculate = Math.max(ticksToRecalculate - 1, 0);
			if (ticksToRecalculate <= 0 && (pathedX == 0.0D && pathedY == 0.0D && pathedZ == 0.0D
				|| target.getDistanceSq(pathedX, pathedY, pathedZ) >= 1.0D || rand.nextFloat() < 0.05F)) {
				pathedX = target.posX;
				pathedY = target.boundingBox.minY;
				pathedZ = target.posZ;
				ticksToRecalculate = 4 + rand.nextInt(7);
				double distance = getDistanceSqToEntity(target);
				if (distance > 1024.0D) {
					ticksToRecalculate += 10;
				} else if (distance > 256.0D) {
					ticksToRecalculate += 5;
				}
				if (!getNavigator().tryMoveToEntityLiving(target, 1.25D)) {
					ticksToRecalculate += 15;
				}
			}
			ticksToAttack = Math.max(ticksToAttack - 1, 0);
			if (ticksToAttack <= 0 && inReach(target) && getEntitySenses().canSee(target)) {
				ticksToAttack = 20;
				attackEntityAsMob(target);
				setStanding(false);
			} else if (getDistanceSqToEntity(target) < (target.width + 3.0F) * (target.width + 3.0F)) {
				if (ticksToAttack <= 0) {
					setStanding(false);
					ticksToAttack = 20;
				}
				if (ticksToAttack <= 10) {
					setStanding(true);
					playWarningSound();
				}
			} else {
				ticksToAttack = 20;
				setStanding(false);
			}
		}
	}

	/**
	 * 26.3's panic: for 40 ticks after damage that scares it (any attack for a cub, only fire, lava and the like for an
	 * adult), it runs at twice its speed to a spot up to 5 blocks away, or, on fire, to water within 5.
	 */
	private final class AIPanic extends EntityAIBase {
		private double x, y, z;

		AIPanic() {
			setMutexBits(1);
		}

		@Override
		public boolean shouldExecute() {
			if (!shouldPanic()) {
				return false;
			}
			if (isBurning() && findWater()) {
				return true;
			}
			Vec3 spot = RandomPositionGenerator.findRandomTarget(EntityPolarBear.this, 5, 4);
			if (spot == null) {
				return false;
			}
			x = spot.xCoord;
			y = spot.yCoord;
			z = spot.zCoord;
			return true;
		}

		/** The nearest water (fewest steps along the axes) within 5 sideways and 1 up or down; not from inside a solid block. */
		private boolean findWater() {
			int bx = MathHelper.floor_double(posX), by = MathHelper.floor_double(boundingBox.minY), bz = MathHelper.floor_double(posZ);
			if (worldObj.getBlock(bx, by, bz).getCollisionBoundingBoxFromPool(worldObj, bx, by, bz) != null) {
				return false;
			}
			int best = Integer.MAX_VALUE;
			for (int dy = -1; dy <= 1; dy++) {
				for (int dx = -5; dx <= 5; dx++) {
					for (int dz = -5; dz <= 5; dz++) {
						int steps = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
						if (steps < best && worldObj.getBlock(bx + dx, by + dy, bz + dz).getMaterial() == Material.water) {
							best = steps;
							x = bx + dx;
							y = by + dy;
							z = bz + dz;
						}
					}
				}
			}
			return best != Integer.MAX_VALUE;
		}

		@Override
		public void startExecuting() {
			getNavigator().tryMoveToXYZ(x, y, z, 2.0D);
		}

		@Override
		public boolean continueExecuting() {
			return !getNavigator().noPath();
		}
	}

	/** Hurt: it targets its attacker and calls every adult bear about that has no target; a cub calls them and runs. */
	private final class AIHurtByTarget extends EntityAIHurtByTarget {
		AIHurtByTarget() {
			super(EntityPolarBear.this, false);
		}

		@Override
		public boolean shouldExecute() {
			EntityLivingBase attacker = getAITarget();
			return attacker != null && mayTarget(attacker) && super.shouldExecute();
		}

		@Override
		public void startExecuting() {
			super.startExecuting();
			EntityLivingBase attacker = getAITarget();
			double range = getEntityAttribute(SharedMonsterAttributes.followRange).getAttributeValue();
			List<?> bears = worldObj.getEntitiesWithinAABB(EntityPolarBear.class,
				AxisAlignedBB.getBoundingBox(posX, posY, posZ, posX + 1.0D, posY + 1.0D, posZ + 1.0D).expand(range, 10.0D, range));
			for (Object found : bears) {
				EntityPolarBear other = (EntityPolarBear) found;
				if (other != EntityPolarBear.this && !other.isChild() && other.getAttackTarget() == null && !other.isOnSameTeam(attacker)) {
					other.setAttackTarget(attacker);
				}
			}
			if (isChild()) {
				setAttackTarget(null);
			}
		}
	}

	/** An adult with a cub within 8 blocks (4 up and down) goes for a player within half its follow range. */
	private final class AIDefendCubs extends EntityAINearestAttackableTarget {
		AIDefendCubs() {
			super(EntityPolarBear.this, EntityPlayer.class, 20, true, true, entity -> entity instanceof EntityPlayer player && isValidPlayerTarget(player));
		}

		@Override
		public boolean shouldExecute() {
			if (isChild() || !super.shouldExecute()) {
				return false;
			}
			for (Object found : worldObj.getEntitiesWithinAABB(EntityPolarBear.class, boundingBox.expand(8.0D, 4.0D, 8.0D))) {
				if (((EntityPolarBear) found).isChild()) {
					return true;
				}
			}
			return false;
		}

		@Override
		protected double getTargetDistance() {
			return super.getTargetDistance() * 0.5D;
		}
	}
}
