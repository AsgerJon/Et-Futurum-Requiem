package ganymedes01.etfuturum.entities;

import io.github.asgerjon.mcfly.registry.McFlyItems;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModEnchantments;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.movement.IPosePlayer;
import ganymedes01.etfuturum.spectator.SpectatorMode;
import ganymedes01.etfuturum.trident.Impaling;
import ganymedes01.etfuturum.trident.Lightning;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/**
 * A thrown trident (SPEC-modern-movement.md §58.1): an arrow's flight that keeps its speed in water, 8 damage to the
 * first mob it hits and then none, sticking in blocks, picked up only by its thrower while the thrower is around.
 * With Loyalty (§58.4) it flies back to its thrower once it has dealt damage. Saved under modern's names.
 */
public class EntityThrownTrident extends Entity implements IProjectile, IEntityAdditionalSpawnData {

	public static final int PICKUP_DISALLOWED = 0, PICKUP_ALLOWED = 1, PICKUP_CREATIVE_ONLY = 2;
	private static final float DAMAGE = 8.0F;
	private static final float INERTIA = 0.99F, WATER_INERTIA = 0.99F;
	private static final double GRAVITY = 0.05D;
	private static final int DESPAWN_TICKS = 1200;
	private static final int FOIL_WATCHER = 16, LOYALTY_WATCHER = 17, NO_PHYSICS_WATCHER = 18;
	private static final String HIT_SOUND = Reference.MCAssetVer + ":item.trident.hit";
	private static final String HIT_GROUND_SOUND = Reference.MCAssetVer + ":item.trident.hit_ground";
	private static final String RETURN_SOUND = Reference.MCAssetVer + ":item.trident.return";

	private int xTile = -1, yTile = -1, zTile = -1;
	private Block inTile;
	private int inData;
	private boolean inGround;
	/** Ticks in the ground (modern's inGroundTime); after 4 it counts as having dealt damage. */
	private int inGroundTime;
	/** Ticks at rest, for despawning (modern's life). */
	private int life;
	private int ticksInAir;
	public int arrowShake;
	public int pickup = PICKUP_DISALLOWED;
	private boolean dealtDamage;
	private ItemStack item;
	private UUID ownerId;
	private Entity owner;
	/** The thrower's entity id, for the client, which never learns the UUID (-1: none). */
	private int ownerEntityId = -1;
	/** Returning (Loyalty): it passes through blocks and has no gravity. Synced. */
	private boolean noPhysics;
	/** Ticks spent returning (modern's clientSideReturnTridentTickCount). */
	private int returnTicks;

	public EntityThrownTrident(World world) {
		super(world);
		renderDistanceWeight = 10.0D;
		setSize(0.5F, 0.5F);
		yOffset = 0.0F;
	}

	/** Thrown by the owner, from its eyes. */
	public EntityThrownTrident(World world, EntityLivingBase owner, ItemStack item) {
		this(world);
		this.owner = owner;
		this.ownerId = owner.getUniqueID();
		this.pickup = owner instanceof EntityPlayer ? PICKUP_ALLOWED : PICKUP_DISALLOWED;
		setItem(item);
		setLocationAndAngles(owner.posX, owner.posY + owner.getEyeHeight() - 0.1D, owner.posZ, owner.rotationYaw, owner.rotationPitch);
	}

	@Override
	protected void entityInit() {
		dataWatcher.addObject(FOIL_WATCHER, (byte) 0);
		dataWatcher.addObject(LOYALTY_WATCHER, (byte) 0);
		dataWatcher.addObject(NO_PHYSICS_WATCHER, (byte) 0);
	}

	public void setItem(ItemStack stack) {
		item = stack.copy();
		dataWatcher.updateObject(FOIL_WATCHER, (byte) (item.isItemEnchanted() ? 1 : 0));
		int loyalty = ModEnchantments.loyalty == null ? 0 : EnchantmentHelper.getEnchantmentLevel(ModEnchantments.loyalty.effectId, item);
		dataWatcher.updateObject(LOYALTY_WATCHER, (byte) MathHelper.clamp_int(loyalty, 0, 127));
	}

	/** Its Loyalty level, synced. */
	public int loyalty() {
		return dataWatcher.getWatchableObjectByte(LOYALTY_WATCHER);
	}

	public boolean isNoPhysics() {
		return worldObj.isRemote ? dataWatcher.getWatchableObjectByte(NO_PHYSICS_WATCHER) != 0 : noPhysics;
	}

	private void setNoPhysics(boolean noPhysics) {
		this.noPhysics = noPhysics;
		dataWatcher.updateObject(NO_PHYSICS_WATCHER, (byte) (noPhysics ? 1 : 0));
	}

	/** The trident item it carries, as thrown. */
	public ItemStack getItem() {
		if (item == null) {
			item = new ItemStack(McFlyItems.TRIDENT.get());
		}
		return item;
	}

	/** Drawn with the glint: its item is enchanted. */
	public boolean isFoil() {
		return dataWatcher.getWatchableObjectByte(FOIL_WATCHER) != 0;
	}

	public boolean isInGround() {
		return inGround;
	}

	/** Modern's shootFromRotation: along pitch and yaw, plus the thrower's own movement (up and down only off the ground). */
	public void shootFromRotation(Entity source, float pitch, float yaw, float pitchOffset, float power, float uncertainty) {
		float toRadians = (float) Math.PI / 180.0F;
		float x = -MathHelper.sin(yaw * toRadians) * MathHelper.cos(pitch * toRadians);
		float y = -MathHelper.sin((pitch + pitchOffset) * toRadians);
		float z = MathHelper.cos(yaw * toRadians) * MathHelper.cos(pitch * toRadians);
		setThrowableHeading(x, y, z, power, uncertainty);
		motionX += source.posX - source.prevPosX;
		motionZ += source.posZ - source.prevPosZ;
		if (!source.onGround) {
			motionY += source.posY - source.prevPosY;
		}
	}

	/** Modern's shoot: the direction with a small triangular spread, at this speed. */
	@Override
	public void setThrowableHeading(double x, double y, double z, float power, float uncertainty) {
		double length = MathHelper.sqrt_double(x * x + y * y + z * z);
		double spread = 0.0172275D * uncertainty;
		x = x / length + spread * (rand.nextDouble() - rand.nextDouble());
		y = y / length + spread * (rand.nextDouble() - rand.nextDouble());
		z = z / length + spread * (rand.nextDouble() - rand.nextDouble());
		motionX = x * power;
		motionY = y * power;
		motionZ = z * power;
		faceMotion();
		prevRotationYaw = rotationYaw;
		prevRotationPitch = rotationPitch;
		life = 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int increments) {
		setPosition(x, y, z);
		setRotation(yaw, pitch);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void setVelocity(double x, double y, double z) {
		// no turning to face it: 26.3 has no snap (1.7.10's arrows have one, while their rotation is still 0, 0)
		motionX = x;
		motionY = y;
		motionZ = z;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (inGroundTime > 4) {
			dealtDamage = true;
		}
		if (!returnToThrower()) {
			return;
		}

		boolean physics = !isNoPhysics();
		Block block = worldObj.getBlock(xTile, yTile, zTile);
		if (physics && block.getMaterial() != Material.air) {
			block.setBlockBoundsBasedOnState(worldObj, xTile, yTile, zTile);
			AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(worldObj, xTile, yTile, zTile);
			if (box != null && box.isVecInside(Vec3.createVectorHelper(posX, posY, posZ))) {
				inGround = true;
			}
		}
		if (arrowShake > 0) {
			arrowShake--;
		}

		if (inGround && physics) {
			if (block == inTile && worldObj.getBlockMetadata(xTile, yTile, zTile) == inData) {
				inGroundTime++;
				if (!worldObj.isRemote) {
					tickDespawn();
				}
			} else {
				startFalling();
			}
			return;
		}

		inGroundTime = 0;
		ticksInAir++;
		if (!physics) {
			// returning: straight through blocks and mobs, no gravity, facing the way it came from
			posX += motionX;
			posY += motionY;
			posZ += motionZ;
			turnTowardMotion(true);
			float inertia = isInWater() ? WATER_INERTIA : INERTIA;
			motionX *= inertia;
			motionY *= inertia;
			motionZ *= inertia;
			setPosition(posX, posY, posZ);
			func_145775_I();
			return;
		}
		// turned before it moves, from the motion it starts the tick with: a hit stops it dead, and it keeps that turn
		turnTowardMotion(false);
		Vec3 from = Vec3.createVectorHelper(posX, posY, posZ);
		Vec3 to = Vec3.createVectorHelper(posX + motionX, posY + motionY, posZ + motionZ);
		MovingObjectPosition hit = worldObj.func_147447_a(from, to, false, true, false);
		from = Vec3.createVectorHelper(posX, posY, posZ);
		to = hit != null ? Vec3.createVectorHelper(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord)
			: Vec3.createVectorHelper(posX + motionX, posY + motionY, posZ + motionZ);
		Entity hitEntity = dealtDamage ? null : findHitEntity(from, to);
		if (hitEntity != null) {
			hit = new MovingObjectPosition(hitEntity);
		}

		if (hit != null && hit.entityHit != null) {
			// 26.3's: it is moved to where it hit the mob first, and stays there this tick (the bounce only turns its motion)
			posX = entityHitAt.xCoord;
			posY = entityHitAt.yCoord;
			posZ = entityHitAt.zCoord;
			onHitEntity(hit.entityHit);
		} else {
			if (hit != null) {
				onHitBlock(hit);
			}
			posX += motionX;
			posY += motionY;
			posZ += motionZ;
		}
		float inertia = INERTIA;
		if (isInWater()) {
			inertia = WATER_INERTIA;
			for (int i = 0; i < 4; i++) {
				worldObj.spawnParticle("bubble", posX - motionX * 0.25D, posY - motionY * 0.25D, posZ - motionZ * 0.25D, motionX, motionY, motionZ);
			}
		}
		motionX *= inertia;
		motionY *= inertia;
		motionZ *= inertia;
		if (!inGround) {
			motionY -= GRAVITY;
		}
		setPosition(posX, posY, posZ);
		func_145775_I();
	}

	/**
	 * Loyalty (modern's ThrownTrident.tick): once it has dealt damage, it heads for its thrower's eyes, rising by 0.015
	 * of the height between a level and pulling 0.05 a level toward them, through blocks, with its sound once. A dead
	 * or spectating thrower: it drops as an item (if it could be picked up) and is gone; a thrower that isn't a player
	 * takes it back within its width plus 1 of its eyes. A thrower not found (logged out) leaves it waiting.
	 * Whether it is still there to move on.
	 */
	private boolean returnToThrower() {
		int loyalty = loyalty();
		Entity thrower = getOwner();
		if (loyalty <= 0 || !(dealtDamage || isNoPhysics()) || thrower == null) {
			return true;
		}
		if (!thrower.isEntityAlive() || thrower instanceof EntityPlayer player && SpectatorMode.isSpectator(player)) {
			if (!worldObj.isRemote && pickup == PICKUP_ALLOWED) {
				entityDropItem(getItem().copy(), 0.1F);
			}
			setDead();
			return false;
		}
		double dx = thrower.posX - posX;
		double dy = eyeY(thrower) - posY;
		double dz = thrower.posZ - posZ;
		double distance = MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);
		if (!(thrower instanceof EntityPlayer) && distance < thrower.width + 1.0D) {
			setDead();
			return false;
		}
		setNoPhysics(true);
		posY += dy * 0.015D * loyalty;
		if (distance > 0.0D) {
			double pull = 0.05D * loyalty / distance;
			motionX = motionX * 0.95D + dx * pull;
			motionY = motionY * 0.95D + dy * pull;
			motionZ = motionZ * 0.95D + dz * pull;
		}
		if (returnTicks == 0 && !worldObj.isRemote) {
			playSound(RETURN_SOUND, 10.0F, 1.0F);
		}
		returnTicks++;
		return true;
	}

	/** Where the entity's eyes are: a player's by its pose (as bubble columns ask), else 1.7.10's eye height. */
	private static double eyeY(Entity entity) {
		if (entity instanceof IPosePlayer pose && pose.etfu$getAppliedPose() != null) {
			return entity.boundingBox.minY + pose.etfu$getAppliedPose().eyeHeight;
		}
		return entity.posY + entity.getEyeHeight();
	}

	/** The first mob in the way this tick (not its thrower for its first 5 ticks), or null. */
	/** Where the last mob found was hit: its box widened by 0.3 meets the flight line there. */
	private Vec3 entityHitAt;

	private Entity findHitEntity(Vec3 from, Vec3 to) {
		Entity found = null;
		double closest = 0.0D;
		List<?> entities = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.addCoord(motionX, motionY, motionZ).expand(1.0D, 1.0D, 1.0D));
		Entity thrower = getOwner();
		for (Object candidate : entities) {
			Entity entity = (Entity) candidate;
			if (!entity.canBeCollidedWith() || entity == thrower && ticksInAir < 5) {
				continue;
			}
			MovingObjectPosition intercept = entity.boundingBox.expand(0.3D, 0.3D, 0.3D).calculateIntercept(from, to);
			if (intercept != null) {
				double distance = from.distanceTo(intercept.hitVec);
				if (found == null || distance < closest) {
					found = entity;
					closest = distance;
					entityHitAt = intercept.hitVec;
				}
			}
		}
		if (found instanceof EntityPlayer player && (player.capabilities.disableDamage
			|| thrower instanceof EntityPlayer throwingPlayer && !throwingPlayer.canAttackPlayer(player))) {
			return null;
		}
		return found;
	}

	/** 8 damage, once; then it bounces off and drops (a mob that dodges projectiles lets it fly on). */
	private void onHitEntity(Entity entity) {
		dealtDamage = true;
		if (worldObj.isRemote) {
			bounceOff();
			return;
		}
		Entity thrower = getOwner();
		DamageSource source = new EntityDamageSourceIndirect("trident", this, thrower == null ? this : thrower).setProjectile();
		boolean hurt = entity.attackEntityFrom(source, DAMAGE + Impaling.bonus(getItem(), entity));
		if (hurt) {
			Lightning.channel(getItem(), entity);
		}
		if (hurt || !(entity instanceof EntityEnderman)) {
			playSound(HIT_SOUND, 1.0F, 1.0F);
			bounceOff();
		}
	}

	/** Modern's reverse deflection with the trident's power: -0.01 of the speed sideways, -0.1 up and down, turned round. */
	private void bounceOff() {
		motionX *= -0.01D;
		motionY *= -0.1D;
		motionZ *= -0.01D;
		float turn = 170.0F + rand.nextFloat() * 20.0F;
		rotationYaw += turn;
		prevRotationYaw += turn;
	}

	/**
	 * Sticks where it hit, pulled back 0.05 on each axis it was moving along (26.3; 1.7.10's arrows pull back 0.05 along
	 * the flight line), and stops dead; shaking, with modern's hit-ground sound.
	 */
	private void onHitBlock(MovingObjectPosition hit) {
		xTile = hit.blockX;
		yTile = hit.blockY;
		zTile = hit.blockZ;
		inTile = worldObj.getBlock(xTile, yTile, zTile);
		inData = worldObj.getBlockMetadata(xTile, yTile, zTile);
		posX = hit.hitVec.xCoord - Math.signum(motionX) * 0.05D;
		posY = hit.hitVec.yCoord - Math.signum(motionY) * 0.05D;
		posZ = hit.hitVec.zCoord - Math.signum(motionZ) * 0.05D;
		motionX = 0.0D;
		motionY = 0.0D;
		motionZ = 0.0D;
		if (!worldObj.isRemote) {
			playSound(HIT_GROUND_SOUND, 1.0F, 1.2F / (rand.nextFloat() * 0.2F + 0.9F));
			Lightning.channelAtBlock(getItem(), worldObj, xTile, yTile, zTile, hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord);
		}
		inGround = true;
		arrowShake = 7;
		if (inTile.getMaterial() != Material.air) {
			inTile.onEntityCollidedWithBlock(worldObj, xTile, yTile, zTile, this);
		}
	}

	/** Its block is gone: it falls again, nearly still. */
	private void startFalling() {
		inGround = false;
		motionX *= rand.nextFloat() * 0.2F;
		motionY *= rand.nextFloat() * 0.2F;
		motionZ *= rand.nextFloat() * 0.2F;
		life = 0;
		ticksInAir = 0;
	}

	/** 1200 ticks at rest, then it is gone; never with Loyalty, when it could be picked up. */
	private void tickDespawn() {
		if (loyalty() > 0 && pickup == PICKUP_ALLOWED) {
			return;
		}
		if (++life >= DESPAWN_TICKS) {
			setDead();
		}
	}

	private void faceMotion() {
		float horizontal = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ);
		rotationYaw = (float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
		rotationPitch = (float) (Math.atan2(motionY, horizontal) * 180.0D / Math.PI);
	}

	/** 1.7.10's arrow turning: toward the motion (or away from it, returning), by a fifth of the way a tick. */
	private void turnTowardMotion(boolean backward) {
		float horizontal = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ);
		rotationYaw = (float) (Math.atan2(backward ? -motionX : motionX, backward ? -motionZ : motionZ) * 180.0D / Math.PI);
		rotationPitch = (float) (Math.atan2(motionY, horizontal) * 180.0D / Math.PI);
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
		rotationPitch = prevRotationPitch + (rotationPitch - prevRotationPitch) * 0.2F;
		rotationYaw = prevRotationYaw + (rotationYaw - prevRotationYaw) * 0.2F;
	}

	/** Its thrower, found again by UUID after loading (on the client, by the entity id it spawned with). */
	public Entity getOwner() {
		if (worldObj.isRemote) {
			return ownerEntityId < 0 ? null : worldObj.getEntityByID(ownerEntityId);
		}
		if (ownerId == null) {
			return null;
		}
		if (owner != null && !owner.isDead && ownerId.equals(owner.getUniqueID())) {
			return owner;
		}
		owner = worldObj.func_152378_a(ownerId);
		if (owner == null) {
			for (Object loaded : worldObj.loadedEntityList) {
				if (ownerId.equals(((Entity) loaded).getUniqueID())) {
					owner = (Entity) loaded;
					break;
				}
			}
		}
		return owner;
	}

	private boolean isOwnedBy(EntityPlayer player) {
		return ownerId != null && ownerId.equals(player.getUniqueID());
	}

	/**
	 * Its thrower picks it up once it lies still or is returning, anyone when it has no thrower around (none, or one
	 * not found: logged out), as modern's playerTouch. A returning one goes to its thrower however it was thrown.
	 */
	@Override
	public void onCollideWithPlayer(EntityPlayer player) {
		if (worldObj.isRemote || !(inGround || isNoPhysics()) || arrowShake > 0 || !isOwnedBy(player) && getOwner() != null) {
			return;
		}
		boolean taken = switch (pickup) {
			case PICKUP_ALLOWED -> player.inventory.addItemStackToInventory(getItem().copy());
			case PICKUP_CREATIVE_ONLY -> player.capabilities.isCreativeMode;
			default -> false;
		};
		if (!taken && isNoPhysics() && isOwnedBy(player)) {
			taken = player.inventory.addItemStackToInventory(getItem().copy());
		}
		if (taken) {
			playSound("random.pop", 0.2F, ((rand.nextFloat() - rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
			player.onItemPickup(this, 1);
			setDead();
		}
	}

	@Override
	protected boolean canTriggerWalking() {
		return false;
	}

	@Override
	public boolean canAttackWithItem() {
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public float getShadowSize() {
		return 0.0F;
	}

	/** Modern draws a thrown trident at any distance. */
	@Override
	@SideOnly(Side.CLIENT)
	public boolean isInRangeToRenderDist(double distanceSquared) {
		return true;
	}

	/** The thrower's entity id, so the client can steer a returning trident too. */
	@Override
	public void writeSpawnData(ByteBuf buffer) {
		Entity thrower = getOwner();
		buffer.writeInt(thrower == null ? -1 : thrower.getEntityId());
	}

	@Override
	public void readSpawnData(ByteBuf buffer) {
		ownerEntityId = buffer.readInt();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		tag.setShort("xTile", (short) xTile);
		tag.setShort("yTile", (short) yTile);
		tag.setShort("zTile", (short) zTile);
		tag.setInteger("inTile", inTile == null ? 0 : Block.getIdFromBlock(inTile));
		tag.setByte("inData", (byte) inData);
		tag.setByte("shake", (byte) arrowShake);
		tag.setBoolean("inGround", inGround);
		tag.setShort("life", (short) life);
		tag.setByte("pickup", (byte) pickup);
		tag.setBoolean("DealtDamage", dealtDamage);
		tag.setTag("item", getItem().writeToNBT(new NBTTagCompound()));
		if (ownerId != null) {
			long most = ownerId.getMostSignificantBits();
			long least = ownerId.getLeastSignificantBits();
			tag.setIntArray("Owner", new int[] {(int) (most >> 32), (int) most, (int) (least >> 32), (int) least});
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		xTile = tag.getShort("xTile");
		yTile = tag.getShort("yTile");
		zTile = tag.getShort("zTile");
		inTile = Block.getBlockById(tag.getInteger("inTile"));
		inData = tag.getByte("inData") & 255;
		arrowShake = tag.getByte("shake") & 255;
		inGround = tag.getBoolean("inGround");
		life = tag.getShort("life");
		pickup = tag.getByte("pickup");
		dealtDamage = tag.getBoolean("DealtDamage");
		ItemStack saved = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("item"));
		setItem(saved != null ? saved : new ItemStack(McFlyItems.TRIDENT.get()));
		int[] uuid = tag.getIntArray("Owner");
		ownerId = uuid.length == 4 ? new UUID((long) uuid[0] << 32 | uuid[1] & 0xFFFFFFFFL, (long) uuid[2] << 32 | uuid[3] & 0xFFFFFFFFL) : null;
		owner = null;
	}
}
