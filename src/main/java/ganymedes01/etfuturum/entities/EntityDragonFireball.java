package ganymedes01.etfuturum.entities;

import io.github.asgerjon.mcfly.network.McFlyNetwork;

import ganymedes01.etfuturum.EtFuturum;
import io.github.asgerjon.mcfly.network.DragonBreathBurstMessage;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/**
 * 26.3's dragon fireball (SPEC-modern-movement.md §76.8, `DragonFireball`): it speeds up along its aim ((v + 0.1 along v)
 * times 0.95 a tick, 0.8 in water), can't be hit, trails the dragon's breath, and bursts into a cloud of it (radius 3
 * growing to 7 over 600 ticks, instant damage II) on whatever it hits but its owner.
 */
public class EntityDragonFireball extends Entity {

	private EntityLivingBase owner;
	private UUID ownerId;

	public EntityDragonFireball(World world) {
		super(world);
		setSize(1.0F, 1.0F);
	}

	public EntityDragonFireball(World world, EntityLivingBase owner, double x, double y, double z, double dx, double dy, double dz) {
		this(world);
		this.owner = owner;
		this.ownerId = owner == null ? null : owner.getUniqueID();
		setLocationAndAngles(x, y, z, 0.0F, 0.0F);
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length > 0.0) {
			motionX = dx / length * 0.1;
			motionY = dy / length * 0.1;
			motionZ = dz / length * 0.1;
		}
	}

	@Override
	protected void entityInit() {
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		// 26.3's: gone when its owner is gone or its chunk isn't loaded
		EntityLivingBase shooter = owner();
		if (!worldObj.isRemote && (shooter != null && shooter.isDead || !worldObj.blockExists(MathHelper.floor_double(posX), 64, MathHelper.floor_double(posZ)))) {
			setDead();
			return;
		}
		// the speed first (26.3 changes it before moving): (v + 0.1 along v) times the drag
		double length = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
		float drag = isInWater() ? 0.8F : 0.95F;
		if (length > 0.0) {
			motionX = (motionX + motionX / length * 0.1) * drag;
			motionY = (motionY + motionY / length * 0.1) * drag;
			motionZ = (motionZ + motionZ / length * 0.1) * drag;
		}
		if (isInWater() && worldObj.isRemote) {
			for (int i = 0; i < 4; i++) {
				worldObj.spawnParticle("bubble", posX - motionX * 0.25, posY - motionY * 0.25, posZ - motionZ * 0.25, motionX, motionY, motionZ);
			}
		}
		MovingObjectPosition hit = trace();
		if (hit != null && !worldObj.isRemote) {
			impact(hit);
			if (isDead) {
				return;
			}
		}
		setPosition(posX + motionX, posY + motionY, posZ + motionZ);
		if (worldObj.isRemote) {
			ganymedes01.etfuturum.client.particle.DragonBreathFX.spawn(worldObj, posX, posY + 0.5, posZ, 0.0, 0.0, 0.0);
		}
	}

	/** What it would hit this tick: a block in its way, or an entity it can hit before that (not its owner or its parts). */
	private MovingObjectPosition trace() {
		Vec3 from = Vec3.createVectorHelper(posX, posY, posZ);
		Vec3 to = Vec3.createVectorHelper(posX + motionX, posY + motionY, posZ + motionZ);
		MovingObjectPosition block = worldObj.rayTraceBlocks(from, to);
		from = Vec3.createVectorHelper(posX, posY, posZ);
		to = block != null ? Vec3.createVectorHelper(block.hitVec.xCoord, block.hitVec.yCoord, block.hitVec.zCoord)
			: Vec3.createVectorHelper(posX + motionX, posY + motionY, posZ + motionZ);
		Entity nearest = null;
		double best = Double.MAX_VALUE;
		@SuppressWarnings("unchecked")
		List<Entity> around = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.addCoord(motionX, motionY, motionZ).expand(1.0, 1.0, 1.0));
		for (Entity entity : around) {
			if (!entity.canBeCollidedWith() || isOwnersOrPart(entity) || entity instanceof EntityDragonFireball) {
				continue;
			}
			AxisAlignedBB box = entity.boundingBox.expand(0.3, 0.3, 0.3);
			MovingObjectPosition through = box.calculateIntercept(from, to);
			if (through != null) {
				double d = from.distanceTo(through.hitVec);
				if (d < best) {
					best = d;
					nearest = entity;
				}
			}
		}
		return nearest != null ? new MovingObjectPosition(nearest) : block;
	}

	private boolean isOwnersOrPart(Entity entity) {
		EntityLivingBase shooter = owner();
		if (shooter == null) {
			return false;
		}
		return entity == shooter || entity instanceof EntityDragonPart part && part.entityDragonObj == shooter;
	}

	private EntityLivingBase owner() {
		if (owner == null && ownerId != null) {
			for (Object entity : worldObj.loadedEntityList) {
				if (entity instanceof EntityLivingBase living && ownerId.equals(living.getUniqueID())) {
					owner = living;
				}
			}
		}
		return owner;
	}

	/** 26.3's onHit: the cloud, moved onto the nearest living thing within 4, the burst; the fireball gone. */
	private void impact(MovingObjectPosition hit) {
		if (hit.entityHit != null && isOwnersOrPart(hit.entityHit)) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<EntityLivingBase> near = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, boundingBox.expand(4.0, 2.0, 4.0));
		EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(worldObj, posX, posY, posZ);
		cloud.setOwner(owner());
		cloud.setParticle(EntityAreaEffectCloud.DRAGON_BREATH);
		cloud.setRadius(3.0F);
		cloud.setDuration(600);
		cloud.setRadiusPerTick((7.0F - cloud.getRadius()) / cloud.getDuration());
		cloud.setPotionDurationScale(0.25F);
		cloud.addEffect(new PotionEffect(Potion.harm.id, 1, 1));
		for (EntityLivingBase entity : near) {
			if (getDistanceSqToEntity(entity) < 16.0) {
				cloud.setPosition(entity.posX, entity.posY - entity.yOffset, entity.posZ);
				break;
			}
		}
		int x = MathHelper.floor_double(posX);
		int y = MathHelper.floor_double(posY);
		int z = MathHelper.floor_double(posZ);
		McFlyNetwork.CHANNEL.sendToAllAround(new DragonBreathBurstMessage(x, y, z, true), new NetworkRegistry.TargetPoint(dimension, posX, posY, posZ, 64.0));
		worldObj.spawnEntityInWorld(cloud);
		setDead();
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean isBurning() {
		return false;
	}

	@Override
	public float getBrightness(float partialTicks) {
		return 1.0F;
	}

	@Override
	public int getBrightnessForRender(float partialTicks) {
		return 15728880;
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		if (tag.hasKey("Owner")) {
			ownerId = UUID.fromString(tag.getString("Owner"));
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		if (ownerId != null) {
			tag.setString("Owner", ownerId.toString());
		}
	}

	/** For the dragon's shot: whether the shooter is a dragon. */
	public boolean byDragon() {
		return owner() instanceof EntityDragon;
	}
}
