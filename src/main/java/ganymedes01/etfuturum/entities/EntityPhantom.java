package ganymedes01.etfuturum.entities;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * The phantom (SPEC-modern-movement.md §68.1, 26.3's `Phantom`): an undead flyer that circles a point (its anchor),
 * and with a player target circles above them and swoops every 8 to 11 s, biting on touch; a cat near keeps it off;
 * burns in daylight. 26.3's goals and controls are its server update here (1.7.10's AI tasks are for walkers), with
 * 26.3's goal delays in real ticks (26.3 runs goals every other tick and halves their delays).
 */
public class EntityPhantom extends EntityFlying implements IMob {

	public static final String AMBIENT = McFly.MC_ASSETS + ":entity.phantom.ambient";
	public static final String HURT = McFly.MC_ASSETS + ":entity.phantom.hurt";
	public static final String DEATH = McFly.MC_ASSETS + ":entity.phantom.death";
	public static final String FLAP = McFly.MC_ASSETS + ":entity.phantom.flap";
	public static final String SWOOP = McFly.MC_ASSETS + ":entity.phantom.swoop";
	public static final String BITE = McFly.MC_ASSETS + ":entity.phantom.bite";

	public static final float FLAP_DEGREES_PER_TICK = 7.448451F;
	public static final int TICKS_PER_FLAP = MathHelper.ceiling_float_int(24.166098F);
	/** A mob's own DataWatcher id (1.7.10's flyers use none above 15). */
	private static final int SIZE = 16;
	private static final float WIDTH = 0.9F, HEIGHT = 0.5F;

	private enum Phase {
		CIRCLE, SWOOP
	}

	private double targetX, targetY, targetZ;
	private boolean hasAnchor;
	private int anchorX, anchorY, anchorZ;
	private Phase phase = Phase.CIRCLE;
	private EntityPlayer target;
	private int sizeApplied = -1;
	private boolean still;

	// the move control
	private float moveSpeed = 0.1F;
	// the target goal
	private int nextScanTick = 20;
	// the strategy goal
	private boolean strategyRunning;
	private int nextSweepTick;
	// the sweep goal
	private boolean sweepRunning, scaredOfCat;
	private int catSearchTick;
	// the circle goal
	private boolean circleRunning;
	private float circleAngle, circleDistance, circleHeight, clockwise;

	public EntityPhantom(World world) {
		super(world);
		setSize(WIDTH, HEIGHT);
		experienceValue = 5;
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		dataWatcher.addObject(SIZE, 0);
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage);
		getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(6.0D);
	}

	// ---------------------------------------------------------------- size and anchor

	public int getPhantomSize() {
		return dataWatcher.getWatchableObjectInt(SIZE);
	}

	public void setPhantomSize(int size) {
		dataWatcher.updateObject(SIZE, MathHelper.clamp_int(size, 0, 64));
		applySize();
	}

	/** 26.3's `updatePhantomSizeInfo`: the box `1 + 0.15 size` times, the attack `6 + size`. */
	private void applySize() {
		int size = getPhantomSize();
		if (size == sizeApplied) {
			return;
		}
		sizeApplied = size;
		float scale = 1.0F + 0.15F * size;
		setSize(WIDTH * scale, HEIGHT * scale);
		getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(6 + size);
	}

	/** The block it circles (x, y, z), or null. */
	public int[] getAnchor() {
		return hasAnchor ? new int[]{anchorX, anchorY, anchorZ} : null;
	}

	private void setAnchor(int x, int y, int z) {
		hasAnchor = true;
		anchorX = x;
		anchorY = y;
		anchorZ = z;
	}

	/** For tests and views: no moves of its own (26.3's `NoAI`). */
	public void setStill(boolean still) {
		this.still = still;
		if (still) {
			motionX = motionY = motionZ = 0.0D;
		}
	}

	/** 26.3's `finalizeSpawn`: the anchor 5 above where it spawns, size 0. */
	@Override
	public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
		setAnchor(MathHelper.floor_double(posX), MathHelper.floor_double(posY) + 5, MathHelper.floor_double(posZ));
		setPhantomSize(0);
		return super.onSpawnWithEgg(data);
	}

	// ---------------------------------------------------------------- ticking

	@Override
	public void onUpdate() {
		applySize();
		super.onUpdate();
		if (!worldObj.isRemote && worldObj.difficultySetting == EnumDifficulty.PEACEFUL) {
			setDead();
		}
		if (worldObj.isRemote) {
			clientEffects();
		}
	}

	public int getUniqueFlapTickOffset() {
		return getEntityId() * 3;
	}

	/** 26.3's `Phantom.tick` on the client: the flap sound as a wing comes down, mycelium specks at the wing tips. */
	@SideOnly(Side.CLIENT)
	private void clientEffects() {
		float flap = (getUniqueFlapTickOffset() + ticksExisted) * FLAP_DEGREES_PER_TICK * (float) Math.PI / 180.0F + (float) Math.PI;
		float anim = MathHelper.cos(flap);
		float next = MathHelper.cos(flap + FLAP_DEGREES_PER_TICK * (float) Math.PI / 180.0F);
		if (anim > 0.0F && next <= 0.0F) {
			worldObj.playSound(posX, posY, posZ, FLAP, 0.95F + rand.nextFloat() * 0.05F, 0.95F + rand.nextFloat() * 0.05F, false);
		}
		float across = width * 1.48F;
		float c = MathHelper.cos(rotationYaw * (float) Math.PI / 180.0F) * across;
		float s = MathHelper.sin(rotationYaw * (float) Math.PI / 180.0F) * across;
		float up = (0.3F + anim * 0.45F) * height * 2.5F;
		worldObj.spawnParticle("townaura", posX + c, posY + up, posZ + s, 0.0D, 0.0D, 0.0D);
		worldObj.spawnParticle("townaura", posX - c, posY + up, posZ - s, 0.0D, 0.0D, 0.0D);
	}

	@Override
	public void onLivingUpdate() {
		if (!worldObj.isRemote && burnsInTheSun()) {
			setFire(8);
		}
		super.onLivingUpdate();
	}

	/** 26.3's `Mob.isSunBurnTick` (it has no helmet to take it): by day, bright, not wet, under the sky. */
	private boolean burnsInTheSun() {
		if (!worldObj.isDaytime()) {
			return false;
		}
		float bright = getBrightness(1.0F);
		return bright > 0.5F && rand.nextFloat() * 30.0F < (bright - 0.4F) * 2.0F && !isWet()
			&& worldObj.canBlockSeeTheSky(MathHelper.floor_double(posX), MathHelper.floor_double(posY + getEyeHeight()), MathHelper.floor_double(posZ));
	}

	/** 26.3's goals (target, strategy, sweep, circle) and the move control, a tick at a time. */
	@Override
	protected void updateEntityActionState() {
		despawnEntity();
		if (still) {
			motionX = motionY = motionZ = 0.0D;
			return;
		}
		targetGoal();
		strategyGoal();
		boolean sweep = sweepGoal();
		circleGoal(!sweep);
		moveControl();
		rotationYawHead = renderYawOffset = rotationYaw;
	}

	// ---------------------------------------------------------------- goals

	/** 26.3's `canAttack` with `TargetingConditions.DEFAULT`: alive, not creative or spectating, seen. */
	private boolean canAttack(EntityPlayer player) {
		return player != null && player.isEntityAlive() && !player.capabilities.disableDamage && !player.isInvisible() && canEntityBeSeen(player);
	}

	/** `PhantomAttackPlayerTargetGoal`: every 60 ticks (the first after 20), players within 16 sideways and 64 up and down, the highest first. */
	private void targetGoal() {
		if (target != null) {
			if (!canAttack(target)) {
				target = null;
			}
			return;
		}
		if (nextScanTick > 0) {
			nextScanTick--;
			return;
		}
		nextScanTick = 60;
		List<EntityPlayer> players = new ArrayList<>();
		for (Object found : worldObj.getEntitiesWithinAABB(EntityPlayer.class, boundingBox.expand(16.0D, 64.0D, 16.0D))) {
			EntityPlayer player = (EntityPlayer) found;
			if (getDistanceSqToEntity(player) <= 64.0D * 64.0D) {
				players.add(player);
			}
		}
		players.sort((a, b) -> Double.compare(b.posY, a.posY));
		for (EntityPlayer player : players) {
			if (canAttack(player)) {
				target = player;
				return;
			}
		}
	}

	/** `PhantomAttackStrategyGoal`: with a target, circling, then a swoop every 8 to 11 s; without one, the anchor 10 to 29 above the ground. */
	private void strategyGoal() {
		boolean use = target != null;
		if (use && !strategyRunning) {
			strategyRunning = true;
			nextSweepTick = 10;
			phase = Phase.CIRCLE;
			setAnchorAboveTarget();
		} else if (!use && strategyRunning) {
			strategyRunning = false;
			if (hasAnchor) {
				setAnchor(anchorX, worldObj.getHeightValue(anchorX, anchorZ) + 10 + rand.nextInt(20), anchorZ);
			}
		}
		if (strategyRunning && phase == Phase.CIRCLE && --nextSweepTick <= 0) {
			phase = Phase.SWOOP;
			setAnchorAboveTarget();
			nextSweepTick = (8 + rand.nextInt(4)) * 20;
			playSound(SWOOP, 10.0F, 0.95F + rand.nextFloat() * 0.1F);
		}
	}

	private void setAnchorAboveTarget() {
		if (hasAnchor && target != null) {
			int y = MathHelper.floor_double(target.posY) + 20 + rand.nextInt(20);
			setAnchor(MathHelper.floor_double(target.posX), Math.max(y, 63 + 1), MathHelper.floor_double(target.posZ));
		}
	}

	/** `PhantomSweepAttackGoal`: at the target's middle; touching it, a bite; a cat within 16 blocks ends it. Whether it ran. */
	private boolean sweepGoal() {
		boolean use = target != null && phase == Phase.SWOOP;
		if (use && sweepRunning) {
			use = target.isEntityAlive() && !target.capabilities.disableDamage;
			if (use && ticksExisted > catSearchTick) {
				catSearchTick = ticksExisted + 20;
				scaredOfCat = false;
				for (Object found : worldObj.getEntitiesWithinAABB(EntityOcelot.class, boundingBox.expand(16.0D, 16.0D, 16.0D))) {
					if (((EntityOcelot) found).isTamed() && ((EntityOcelot) found).isEntityAlive()) {
						scaredOfCat = true;
					}
				}
			}
			use &= !scaredOfCat;
		}
		if (!use) {
			if (sweepRunning) {
				sweepRunning = false;
				target = null;
				phase = Phase.CIRCLE;
			}
			return false;
		}
		sweepRunning = true;
		targetX = target.posX;
		targetY = target.boundingBox.minY + target.height * 0.5D;
		targetZ = target.posZ;
		if (boundingBox.expand(0.2D, 0.2D, 0.2D).intersectsWith(target.boundingBox)) {
			attackEntityAsMob(target);
			phase = Phase.CIRCLE;
			worldObj.playSoundEffect(posX, posY, posZ, BITE, 0.3F, rand.nextFloat() * 0.1F + 0.9F);
		} else if (isCollidedHorizontally || hurtTime > 0) {
			phase = Phase.CIRCLE;
		}
		return true;
	}

	/** `PhantomCircleAroundAnchorGoal`: round the anchor 5 to 15 away, -4 to 5 up, a 15 degree step a time. */
	private void circleGoal(boolean free) {
		boolean use = free && (target == null || phase == Phase.CIRCLE);
		if (!use) {
			circleRunning = false;
			return;
		}
		if (!circleRunning) {
			circleRunning = true;
			circleDistance = 5.0F + rand.nextFloat() * 10.0F;
			circleHeight = -4.0F + rand.nextFloat() * 9.0F;
			clockwise = rand.nextBoolean() ? 1.0F : -1.0F;
			selectNext();
		}
		if (rand.nextInt(350) == 0) {
			circleHeight = -4.0F + rand.nextFloat() * 9.0F;
		}
		if (rand.nextInt(250) == 0) {
			circleDistance++;
			if (circleDistance > 15.0F) {
				circleDistance = 5.0F;
				clockwise = -clockwise;
			}
		}
		if (rand.nextInt(450) == 0) {
			circleAngle = rand.nextFloat() * 2.0F * (float) Math.PI;
			selectNext();
		}
		if (touchingTarget()) {
			selectNext();
		}
		int x = MathHelper.floor_double(posX), y = MathHelper.floor_double(posY), z = MathHelper.floor_double(posZ);
		if (targetY < posY && !worldObj.isAirBlock(x, y - 1, z)) {
			circleHeight = Math.max(1.0F, circleHeight);
			selectNext();
		}
		if (targetY > posY && !worldObj.isAirBlock(x, y + 1, z)) {
			circleHeight = Math.min(-1.0F, circleHeight);
			selectNext();
		}
	}

	private boolean touchingTarget() {
		double dx = targetX - posX, dy = targetY - posY, dz = targetZ - posZ;
		return dx * dx + dy * dy + dz * dz < 4.0D;
	}

	private void selectNext() {
		if (!hasAnchor) {
			setAnchor(MathHelper.floor_double(posX), MathHelper.floor_double(posY), MathHelper.floor_double(posZ));
		}
		circleAngle += clockwise * 15.0F * (float) Math.PI / 180.0F;
		targetX = anchorX + circleDistance * MathHelper.cos(circleAngle);
		targetY = anchorY - 4.0F + circleHeight;
		targetZ = anchorZ + circleDistance * MathHelper.sin(circleAngle);
	}

	// ---------------------------------------------------------------- the move control

	/** `PhantomMoveControl`, 26.3's arithmetic as is. */
	private void moveControl() {
		if (isCollidedHorizontally) {
			rotationYaw += 180.0F;
			moveSpeed = 0.1F;
		}
		double tdx = targetX - posX, tdy = targetY - posY, tdz = targetZ - posZ;
		double sd = Math.sqrt(tdx * tdx + tdz * tdz);
		if (Math.abs(sd) <= 1.0E-5F) {
			return;
		}
		double yRelativeScale = 1.0D - Math.abs(tdy * 0.7F) / sd;
		tdx *= yRelativeScale;
		tdz *= yRelativeScale;
		sd = Math.sqrt(tdx * tdx + tdz * tdz);
		double sd2 = Math.sqrt(tdx * tdx + tdz * tdz + tdy * tdy);
		float previous = rotationYaw;
		float angle = (float) Math.atan2(tdz, tdx);
		float a = MathHelper.wrapAngleTo180_float(rotationYaw + 90.0F);
		float b = MathHelper.wrapAngleTo180_float(angle * (180.0F / (float) Math.PI));
		rotationYaw = approachDegrees(a, b, 4.0F) - 90.0F;
		renderYawOffset = rotationYaw;
		if (Math.abs(MathHelper.wrapAngleTo180_float(rotationYaw - previous)) < 3.0F) {
			moveSpeed = approach(moveSpeed, 1.8F, 0.005F * (1.8F / moveSpeed));
		} else {
			moveSpeed = approach(moveSpeed, 0.2F, 0.025F);
		}
		float pitch = (float) -(Math.atan2(-tdy, sd) * 180.0F / (float) Math.PI);
		rotationPitch = pitch;
		float moveAngle = rotationYaw + 90.0F;
		double txd = moveSpeed * MathHelper.cos(moveAngle * (float) Math.PI / 180.0F) * Math.abs(tdx / sd2);
		double tzd = moveSpeed * MathHelper.sin(moveAngle * (float) Math.PI / 180.0F) * Math.abs(tdz / sd2);
		double tyd = moveSpeed * MathHelper.sin(pitch * (float) Math.PI / 180.0F) * Math.abs(tdy / sd2);
		motionX += (txd - motionX) * 0.2D;
		motionY += (tyd - motionY) * 0.2D;
		motionZ += (tzd - motionZ) * 0.2D;
	}

	/** 26.3's `Mth.approach`. */
	private static float approach(float current, float target, float step) {
		return current < target ? Math.min(current + step, target) : Math.max(current - step, target);
	}

	/** 26.3's `Mth.approachDegrees`. */
	private static float approachDegrees(float current, float target, float step) {
		return approach(current, current + MathHelper.wrapAngleTo180_float(target - current), step);
	}

	// ---------------------------------------------------------------- the rest

	@Override
	public boolean attackEntityAsMob(Entity entity) {
		return entity.attackEntityFrom(DamageSource.causeMobDamage(this), (float) getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue());
	}

	@Override
	public EnumCreatureAttribute getCreatureAttribute() {
		return EnumCreatureAttribute.UNDEAD;
	}

	@Override
	public boolean isOnLadder() {
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isInRangeToRenderDist(double distance) {
		return true;
	}

	@Override
	protected String getLivingSound() {
		return AMBIENT;
	}

	@Override
	protected String getHurtSound() {
		return HURT;
	}

	@Override
	protected String getDeathSound() {
		return DEATH;
	}

	/** 26.3's loot (`entities/phantom`): killed by a player, 0 to 1 membranes, plus round(Looting level x random). */
	@Override
	protected void dropFewItems(boolean recentlyHit, int looting) {
		if (!recentlyHit || !McFlyItems.PHANTOM_MEMBRANE.isEnabled()) {
			return;
		}
		int count = rand.nextInt(2) + Math.round(looting * rand.nextFloat());
		if (count > 0) {
			entityDropItem(McFlyItems.PHANTOM_MEMBRANE.newItemStack(count), 0.0F);
		}
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound tag) {
		super.writeEntityToNBT(tag);
		if (hasAnchor) {
			tag.setIntArray("anchor_pos", new int[]{anchorX, anchorY, anchorZ});
		}
		tag.setInteger("size", getPhantomSize());
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound tag) {
		super.readEntityFromNBT(tag);
		int[] anchor = tag.getIntArray("anchor_pos");
		if (anchor.length == 3) {
			setAnchor(anchor[0], anchor[1], anchor[2]);
		}
		setPhantomSize(tag.getInteger("size"));
	}

	/** The target it is after (for tests). */
	public EntityLivingBase getPhantomTarget() {
		return target;
	}
}
