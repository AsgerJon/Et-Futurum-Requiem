package ganymedes01.etfuturum.entities;

import ganymedes01.etfuturum.client.particle.CustomParticles;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.potion.ModPotions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The shulker's bullet as 26.3's `ShulkerBullet` (with what it takes from `Projectile`), written anew
 * (SPEC-modern-movement.md §76.13): from its shooter's middle, up first, then legs along one axis at a time towards
 * its target, a new leg when the block ahead is solid, when it's level with the target on its axis, or after 10 to 50
 * steps; its motion eased towards the leg's speed (0.15, growing 2.5% a tick, at most 1); with no target it falls. An
 * entity it hits takes 4 and Levitation for 10 seconds; a block gets an explosion's puff; either way it's gone. Hit,
 * it bursts into crits. EFR's id (`shulker_candy`) stays for old saves.
 */
public class EntityShulkerBullet extends Entity {

	private static final String SOUND = Reference.MCAssetVer + ":entity.shulker_bullet.";
	private static final int[][] STEP = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

	private EntityLivingBase owner;
	private UUID ownerId;
	private Entity target;
	private UUID targetId;
	/** The leg's direction (26.3's sides: down, up, north, south, west, east), or -1. */
	private int direction = -1;
	private int flightSteps;
	private double targetDeltaX;
	private double targetDeltaY;
	private double targetDeltaZ;
	/** 26.3's Projectile: whether it has left its shooter's box, after which it can hit it too. */
	private boolean leftOwner;

	public EntityShulkerBullet(World world) {
		super(world);
		setSize(0.3125F, 0.3125F);
		noClip = true;
	}

	/** Shot by the owner (at its box's middle) at the target; its first leg not along the axis given (x 0, y 1, z 2). */
	public EntityShulkerBullet(World world, EntityLivingBase owner, Entity target, int avoidAxis) {
		this(world);
		this.owner = owner;
		if (owner != null) {
			ownerId = owner.getUniqueID();
			AxisAlignedBB box = owner.boundingBox;
			setLocationAndAngles((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0, rotationYaw, rotationPitch);
		}
		this.target = target;
		targetId = target == null ? null : target.getUniqueID();
		direction = 1;
		selectNextMoveDirection(avoidAxis, target);
	}

	@Override
	protected void entityInit() {
	}

	private static int axis(int side) {
		return side < 2 ? 1 : side < 4 ? 2 : 0;
	}

	/** 26.3's selectNextMoveDirection: the next leg, towards the target (the axis given left out), or a random side. */
	private void selectNextMoveDirection(int avoidAxis, Entity target) {
		double yOffset = 0.5;
		int targetX;
		int targetY;
		int targetZ;
		if (target == null) {
			targetX = MathHelper.floor_double(posX);
			targetY = MathHelper.floor_double(posY) - 1;
			targetZ = MathHelper.floor_double(posZ);
		} else {
			yOffset = target.height * 0.5;
			targetX = MathHelper.floor_double(target.posX);
			targetY = MathHelper.floor_double(target.posY + yOffset);
			targetZ = MathHelper.floor_double(target.posZ);
		}
		double wantedX = targetX + 0.5;
		double wantedY = targetY + yOffset;
		double wantedZ = targetZ + 0.5;
		int selection = -1;
		double cx = posX - (targetX + 0.5);
		double cy = posY - (targetY + 0.5);
		double cz = posZ - (targetZ + 0.5);
		if (!(cx * cx + cy * cy + cz * cz < 4.0)) {
			int x = MathHelper.floor_double(posX);
			int y = MathHelper.floor_double(posY);
			int z = MathHelper.floor_double(posZ);
			List<Integer> options = new ArrayList<>();
			if (avoidAxis != 0) {
				if (x < targetX && worldObj.isAirBlock(x + 1, y, z)) {
					options.add(5);
				} else if (x > targetX && worldObj.isAirBlock(x - 1, y, z)) {
					options.add(4);
				}
			}
			if (avoidAxis != 1) {
				if (y < targetY && worldObj.isAirBlock(x, y + 1, z)) {
					options.add(1);
				} else if (y > targetY && worldObj.isAirBlock(x, y - 1, z)) {
					options.add(0);
				}
			}
			if (avoidAxis != 2) {
				if (z < targetZ && worldObj.isAirBlock(x, y, z + 1)) {
					options.add(3);
				} else if (z > targetZ && worldObj.isAirBlock(x, y, z - 1)) {
					options.add(2);
				}
			}
			selection = rand.nextInt(6);
			if (options.isEmpty()) {
				for (int attempts = 5; !worldObj.isAirBlock(x + STEP[selection][0], y + STEP[selection][1], z + STEP[selection][2]) && attempts > 0; attempts--) {
					selection = rand.nextInt(6);
				}
			} else {
				selection = options.get(rand.nextInt(options.size()));
			}
			wantedX = posX + STEP[selection][0];
			wantedY = posY + STEP[selection][1];
			wantedZ = posZ + STEP[selection][2];
		}
		direction = selection;
		double dx = wantedX - posX;
		double dy = wantedY - posY;
		double dz = wantedZ - posZ;
		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (distance == 0.0) {
			targetDeltaX = targetDeltaY = targetDeltaZ = 0.0;
		} else {
			targetDeltaX = dx / distance * 0.15;
			targetDeltaY = dy / distance * 0.15;
			targetDeltaZ = dz / distance * 0.15;
		}
		flightSteps = 10 + rand.nextInt(5) * 10;
	}

	/** The target, found again by its id after loading. */
	private Entity target() {
		if (target != null && target.isDead) {
			target = null;
		}
		if (target == null && targetId != null) {
			target = byId(targetId);
			if (target == null) {
				targetId = null;
			}
		}
		return target;
	}

	private EntityLivingBase owner() {
		if (owner == null && ownerId != null && byId(ownerId) instanceof EntityLivingBase living) {
			owner = living;
		}
		return owner;
	}

	private Entity byId(UUID id) {
		for (Object object : worldObj.loadedEntityList) {
			if (!((Entity) object).isDead && id.equals(((Entity) object).getUniqueID())) {
				return (Entity) object;
			}
		}
		return null;
	}

	@Override
	public void onUpdate() {
		if (!worldObj.isRemote && worldObj.difficultySetting == EnumDifficulty.PEACEFUL) {
			setDead();
			return;
		}
		if (!leftOwner) {
			leftOwner = outsideOwner();
		}
		super.onUpdate();
		Entity finalTarget = !worldObj.isRemote ? target() : null;
		MovingObjectPosition hit = null;
		if (!worldObj.isRemote) {
			if (finalTarget == null || !finalTarget.isEntityAlive()) {
				motionY -= 0.04;
			} else {
				targetDeltaX = MathHelper.clamp_double(targetDeltaX * 1.025, -1.0, 1.0);
				targetDeltaY = MathHelper.clamp_double(targetDeltaY * 1.025, -1.0, 1.0);
				targetDeltaZ = MathHelper.clamp_double(targetDeltaZ * 1.025, -1.0, 1.0);
				motionX += (targetDeltaX - motionX) * 0.2;
				motionY += (targetDeltaY - motionY) * 0.2;
				motionZ += (targetDeltaZ - motionZ) * 0.2;
			}
			hit = hitOnMove();
		}
		double mx = motionX;
		double my = motionY;
		double mz = motionZ;
		setPosition(posX + mx, posY + my, posZ + mz);
		func_145775_I();
		if (hit != null && !isDead) {
			onHit(hit);
		}
		rotateTowardsMovement();
		if (worldObj.isRemote) {
			io.github.asgerjon.mcfly.particle.McFlyParticlesClient.add(io.github.asgerjon.mcfly.particle.McFlyParticles.END_ROD, posX - mx, posY - my + 0.15, posZ - mz, 0.0, 0.0, 0.0);
		} else if (finalTarget != null && !isDead) {
			if (flightSteps > 0) {
				flightSteps--;
				if (flightSteps == 0) {
					selectNextMoveDirection(direction < 0 ? -1 : axis(direction), finalTarget);
				}
			}
			if (direction >= 0) {
				int x = MathHelper.floor_double(posX);
				int y = MathHelper.floor_double(posY);
				int z = MathHelper.floor_double(posZ);
				int legAxis = axis(direction);
				int ax = x + STEP[direction][0];
				int ay = y + STEP[direction][1];
				int az = z + STEP[direction][2];
				if (worldObj.blockExists(ax, ay, az) && worldObj.isSideSolid(ax, ay, az, ForgeDirection.UP, false)) {
					selectNextMoveDirection(legAxis, finalTarget);
				} else {
					int tx = MathHelper.floor_double(finalTarget.posX);
					int ty = MathHelper.floor_double(finalTarget.posY);
					int tz = MathHelper.floor_double(finalTarget.posZ);
					if (legAxis == 0 && x == tx || legAxis == 2 && z == tz || legAxis == 1 && y == ty) {
						selectNextMoveDirection(legAxis, finalTarget);
					}
				}
			}
		}
	}

	/** 26.3's isOutsideOwnerCollisionRange: its box grown by its move and 1 meets nothing of its shooter's. */
	private boolean outsideOwner() {
		EntityLivingBase shooter = owner();
		if (shooter == null) {
			return true;
		}
		AxisAlignedBB area = boundingBox.addCoord(motionX, motionY, motionZ).expand(1.0, 1.0, 1.0);
		Entity root = shooter;
		while (root.ridingEntity != null) {
			root = root.ridingEntity;
		}
		for (Entity part = root; part != null; part = part.riddenByEntity) {
			if (part.canBeCollidedWith() && area.intersectsWith(part.boundingBox)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 26.3's ProjectileUtil.getHitResultOnMoveVector: blocks along its move (by their collision boxes), then the nearest
	 * entity it may hit before that, their boxes grown by its margin (0 to 0.3 over its first 8 ticks).
	 */
	private MovingObjectPosition hitOnMove() {
		Vec3 from = Vec3.createVectorHelper(posX, posY, posZ);
		Vec3 to = Vec3.createVectorHelper(posX + motionX, posY + motionY, posZ + motionZ);
		MovingObjectPosition hit = worldObj.func_147447_a(Vec3.createVectorHelper(posX, posY, posZ), Vec3.createVectorHelper(to.xCoord, to.yCoord, to.zCoord), false, true, false);
		if (hit != null) {
			to = Vec3.createVectorHelper(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord);
		}
		float margin = Math.max(0.0F, Math.min(0.3F, (ticksExisted - 2) / 20.0F));
		Entity nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (Object object : worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.addCoord(motionX, motionY, motionZ).expand(1.0, 1.0, 1.0))) {
			Entity entity = (Entity) object;
			if (!canHitEntity(entity)) {
				continue;
			}
			MovingObjectPosition clip = entity.boundingBox.expand(margin, margin, margin).calculateIntercept(from, to);
			if (clip != null) {
				double distance = from.squareDistanceTo(clip.hitVec);
				if (distance < nearestDistance) {
					nearestDistance = distance;
					nearest = entity;
				}
			}
		}
		return nearest != null ? new MovingObjectPosition(nearest) : hit;
	}

	/** 26.3: a living-enough thing that can be hit, not one without physics, and its shooter only once it has left it. */
	private boolean canHitEntity(Entity entity) {
		if (!entity.canBeCollidedWith() || !entity.isEntityAlive() || entity.noClip) {
			return false;
		}
		EntityLivingBase shooter = owner();
		return shooter == null || leftOwner || !sameVehicle(shooter, entity);
	}

	private static boolean sameVehicle(Entity a, Entity b) {
		Entity rootA = a;
		while (rootA.ridingEntity != null) {
			rootA = rootA.ridingEntity;
		}
		Entity rootB = b;
		while (rootB.ridingEntity != null) {
			rootB = rootB.ridingEntity;
		}
		return rootA == rootB;
	}

	private void onHit(MovingObjectPosition hit) {
		if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
			hitEntity(hit.entityHit);
		} else {
			((WorldServer) worldObj).func_147487_a("largeexplode", posX, posY, posZ, 2, 0.2, 0.2, 0.2, 0.0);
			playSound(SOUND + "hit", 1.0F, 1.0F);
		}
		setDead();
	}

	/** Hits the target directly (as an entity met on its move does), and is gone. */
	public void hit(Entity target) {
		hitEntity(target);
		setDead();
	}

	/** 26.3's onHitEntity: 4 as a mob's projectile; hurt, Levitation for 200 ticks (and the shooter's enchantments act). */
	private void hitEntity(Entity entity) {
		EntityLivingBase shooter = owner();
		DamageSource source = new EntityDamageSourceIndirect("mob", this, shooter).setProjectile();
		if (entity.attackEntityFrom(source, 4.0F)) {
			if (entity instanceof EntityLivingBase living) {
				if (shooter != null) {
					EnchantmentHelper.func_151384_a(living, shooter);
					EnchantmentHelper.func_151385_b(shooter, living);
				}
				if (ModPotions.levitation != null) {
					living.addPotionEffect(new PotionEffect(ModPotions.levitation.id, 200));
				}
			}
		}
	}

	/** 26.3's rotateTowardsMovement at 0.5: its turn halfway to its motion each tick (the drawing spins it too). */
	private void rotateTowardsMovement() {
		double length = motionX * motionX + motionY * motionY + motionZ * motionZ;
		if (length == 0.0) {
			return;
		}
		double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
		rotationYaw = (float) (Math.atan2(motionZ, motionX) * 180.0F / (float) Math.PI) + 90.0F;
		rotationPitch = (float) (Math.atan2(horizontal, motionY) * 180.0F / (float) Math.PI) - 90.0F;
		while (rotationPitch - prevRotationPitch < -180.0F) {
			prevRotationPitch -= 360.0F;
		}
		while (rotationPitch - prevRotationPitch >= 180.0F) {
			prevRotationPitch += 360.0F;
		}
		while (rotationYaw - prevRotationYaw < -180.0F) {
			prevRotationYaw -= 360.0F;
		}
		while (rotationYaw - prevRotationYaw >= 180.0F) {
			prevRotationYaw += 360.0F;
		}
		rotationPitch = prevRotationPitch + (rotationPitch - prevRotationPitch) * 0.5F;
		rotationYaw = prevRotationYaw + (rotationYaw - prevRotationYaw) * 0.5F;
	}

	/** Hit, it bursts into 15 crits and is gone (26.3's hurtServer). */
	@Override
	public boolean attackEntityFrom(DamageSource source, float damage) {
		if (!worldObj.isRemote) {
			playSound(SOUND + "hurt", 1.0F, 1.0F);
			((WorldServer) worldObj).func_147487_a("crit", posX, posY, posZ, 15, 0.2, 0.2, 0.2, 0.0);
			setDead();
		}
		return true;
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	@Override
	public boolean isBurning() {
		return false;
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		return distance < 16384.0;
	}

	@Override
	public float getBrightness(float partialTick) {
		return 1.0F;
	}

	@Override
	public int getBrightnessForRender(float partialTick) {
		return 15728880;
	}

	private static int[] uuidArray(UUID id) {
		long most = id.getMostSignificantBits();
		long least = id.getLeastSignificantBits();
		return new int[] {(int) (most >> 32), (int) most, (int) (least >> 32), (int) least};
	}

	private static UUID uuidFrom(int[] array) {
		if (array.length != 4) {
			return null;
		}
		return new UUID((long) array[0] << 32 | array[1] & 0xFFFFFFFFL, (long) array[2] << 32 | array[3] & 0xFFFFFFFFL);
	}

	/** Saved as 26.3's: `Owner`, `LeftOwner`, `Target`, `Dir`, `Steps`, `TXD`, `TYD`, `TZD`. */
	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		if (ownerId != null) {
			tag.setIntArray("Owner", uuidArray(ownerId));
		}
		if (leftOwner) {
			tag.setBoolean("LeftOwner", true);
		}
		if (targetId != null) {
			tag.setIntArray("Target", uuidArray(targetId));
		}
		if (direction >= 0) {
			tag.setInteger("Dir", direction);
		}
		tag.setInteger("Steps", flightSteps);
		tag.setDouble("TXD", targetDeltaX);
		tag.setDouble("TYD", targetDeltaY);
		tag.setDouble("TZD", targetDeltaZ);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		ownerId = tag.hasKey("Owner") ? uuidFrom(tag.getIntArray("Owner")) : null;
		leftOwner = tag.getBoolean("LeftOwner");
		targetId = tag.hasKey("Target") ? uuidFrom(tag.getIntArray("Target")) : null;
		direction = tag.hasKey("Dir") ? MathHelper.clamp_int(tag.getInteger("Dir"), 0, 5) : -1;
		flightSteps = tag.getInteger("Steps");
		targetDeltaX = tag.getDouble("TXD");
		targetDeltaY = tag.getDouble("TYD");
		targetDeltaZ = tag.getDouble("TZD");
	}
}
