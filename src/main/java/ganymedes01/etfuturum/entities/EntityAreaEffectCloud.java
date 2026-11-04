package ganymedes01.etfuturum.entities;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 26.3's area effect cloud (SPEC-modern-movement.md §76.8, `AreaEffectCloud`): a flat disc that waits, then grows, and
 * every 5 ticks gives its effects to the living things within it (each at most once in its reapplication delay); its
 * particles over the disc. The dragon's breath and fireballs make them (their owner the dragon, their particle its
 * breath).
 */
public class EntityAreaEffectCloud extends Entity {

	private static final int RADIUS = 20;
	private static final int WAITING = 21;
	private static final int PARTICLE = 22;
	/** Particle kinds: 1 the dragon's breath. */
	public static final int DRAGON_BREATH = 1;

	private final List<PotionEffect> effects = new ArrayList<>();
	private final Map<Entity, Integer> victims = new HashMap<>();
	private int duration = 600;
	private int waitTime = 20;
	private int reapplicationDelay = 20;
	private int durationOnUse;
	private float radiusOnUse;
	private float radiusPerTick;
	private float potionDurationScale = 1.0F;
	private EntityLivingBase owner;
	private UUID ownerId;

	public EntityAreaEffectCloud(World world) {
		super(world);
		noClip = true;
		setRadius(3.0F);
	}

	public EntityAreaEffectCloud(World world, double x, double y, double z) {
		this(world);
		setPosition(x, y, z);
	}

	@Override
	protected void entityInit() {
		dataWatcher.addObject(RADIUS, 3.0F);
		dataWatcher.addObject(WAITING, (byte) 0);
		dataWatcher.addObject(PARTICLE, 0);
	}

	public void setRadius(float radius) {
		if (!worldObj.isRemote) {
			dataWatcher.updateObject(RADIUS, MathHelper.clamp_float(radius, 0.0F, 32.0F));
		}
		setSize(radius * 2.0F, 0.5F);
		setPosition(posX, posY, posZ);
	}

	public float getRadius() {
		return dataWatcher.getWatchableObjectFloat(RADIUS);
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getDuration() {
		return duration;
	}

	public void setWaitTime(int waitTime) {
		this.waitTime = waitTime;
	}

	public void setRadiusPerTick(float radiusPerTick) {
		this.radiusPerTick = radiusPerTick;
	}

	public float getRadiusPerTick() {
		return radiusPerTick;
	}

	public void setPotionDurationScale(float scale) {
		this.potionDurationScale = scale;
	}

	public void addEffect(PotionEffect effect) {
		effects.add(effect);
	}

	public void setParticle(int kind) {
		dataWatcher.updateObject(PARTICLE, kind);
	}

	public int particle() {
		return dataWatcher.getWatchableObjectInt(PARTICLE);
	}

	public void setOwner(EntityLivingBase owner) {
		this.owner = owner;
		this.ownerId = owner == null ? null : owner.getUniqueID();
	}

	public EntityLivingBase getOwner() {
		if (owner == null && ownerId != null) {
			for (Object entity : worldObj.loadedEntityList) {
				if (entity instanceof EntityLivingBase living && ownerId.equals(living.getUniqueID())) {
					owner = living;
				}
			}
		}
		return owner;
	}

	/** The dragon made it (bottling takes dragon's breath only from those). */
	public boolean byDragon() {
		return getOwner() instanceof EntityDragon || particle() == DRAGON_BREATH;
	}

	private boolean waiting() {
		return dataWatcher.getWatchableObjectByte(WAITING) != 0;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		float radius = getRadius();
		if (Math.abs(width - radius * 2.0F) > 1.0E-4F) {
			setSize(radius * 2.0F, 0.5F);
			setPosition(posX, posY, posZ);
		}
		if (worldObj.isRemote) {
			clientParticles(radius);
			return;
		}
		if (ticksExisted >= waitTime + duration) {
			setDead();
			return;
		}
		boolean wait = ticksExisted < waitTime;
		if (wait != waiting()) {
			dataWatcher.updateObject(WAITING, (byte) (wait ? 1 : 0));
		}
		if (wait) {
			return;
		}
		if (radiusPerTick != 0.0F) {
			radius += radiusPerTick;
			if (radius < 0.5F) {
				setDead();
				return;
			}
			setRadius(radius);
		}
		if (ticksExisted % 5 == 0) {
			victims.entrySet().removeIf(entry -> ticksExisted >= entry.getValue());
			if (effects.isEmpty()) {
				victims.clear();
				return;
			}
			@SuppressWarnings("unchecked")
			List<EntityLivingBase> within = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, boundingBox);
			for (EntityLivingBase entity : within) {
				if (victims.containsKey(entity) || entity instanceof EntityDragon) {
					continue;
				}
				double dx = entity.posX - posX;
				double dz = entity.posZ - posZ;
				if (dx * dx + dz * dz > radius * radius) {
					continue;
				}
				victims.put(entity, ticksExisted + reapplicationDelay);
				for (PotionEffect effect : effects) {
					Potion potion = Potion.potionTypes[effect.getPotionID()];
					if (potion.isInstant()) {
						potion.affectEntity(getOwner(), entity, effect.getAmplifier(), 0.5);
					} else {
						entity.addPotionEffect(new PotionEffect(effect.getPotionID(), (int) (effect.getDuration() * potionDurationScale), effect.getAmplifier()));
					}
				}
				if (radiusOnUse != 0.0F) {
					radius += radiusOnUse;
					if (radius < 0.5F) {
						setDead();
						return;
					}
					setRadius(radius);
				}
				if (durationOnUse != 0) {
					duration += durationOnUse;
					if (duration <= 0) {
						setDead();
						return;
					}
				}
			}
		}
	}

	/** 26.3's: waiting, 2 within 0.2 half the ticks; else ceil(pi r^2) over the disc. */
	@SideOnly(Side.CLIENT)
	private void clientParticles(float radius) {
		int count;
		float spread;
		if (waiting()) {
			if (!rand.nextBoolean()) {
				return;
			}
			count = 2;
			spread = 0.2F;
		} else {
			count = MathHelper.ceiling_float_int((float) Math.PI * radius * radius);
			spread = radius;
		}
		for (int i = 0; i < count; i++) {
			float angle = rand.nextFloat() * (float) (Math.PI * 2);
			float distance = MathHelper.sqrt_float(rand.nextFloat()) * spread;
			double x = posX + MathHelper.cos(angle) * distance;
			double z = posZ + MathHelper.sin(angle) * distance;
			if (particle() == DRAGON_BREATH) {
				ganymedes01.etfuturum.client.particle.DragonBreathFX.spawn(worldObj, x, posY, z, (0.5 - rand.nextDouble()) * 0.15, 0.01F, (0.5 - rand.nextDouble()) * 0.15);
			} else {
				worldObj.spawnParticle("mobSpell", x, posY, z, 0.5, 0.5, 0.5);
			}
		}
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	@Override
	public void moveEntity(double x, double y, double z) {
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		ticksExisted = tag.getInteger("Age");
		duration = tag.getInteger("Duration");
		waitTime = tag.getInteger("WaitTime");
		reapplicationDelay = tag.getInteger("ReapplicationDelay");
		durationOnUse = tag.getInteger("DurationOnUse");
		radiusOnUse = tag.getFloat("RadiusOnUse");
		radiusPerTick = tag.getFloat("RadiusPerTick");
		potionDurationScale = tag.hasKey("potion_duration_scale") ? tag.getFloat("potion_duration_scale") : 1.0F;
		setRadius(tag.getFloat("Radius"));
		setParticle(tag.getInteger("etfuturum_particle"));
		if (tag.hasKey("Owner")) {
			ownerId = UUID.fromString(tag.getString("Owner"));
		}
		effects.clear();
		NBTTagList list = tag.getTagList("effects", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
			if (effect != null) {
				effects.add(effect);
			}
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		tag.setInteger("Age", ticksExisted);
		tag.setInteger("Duration", duration);
		tag.setInteger("WaitTime", waitTime);
		tag.setInteger("ReapplicationDelay", reapplicationDelay);
		tag.setInteger("DurationOnUse", durationOnUse);
		tag.setFloat("RadiusOnUse", radiusOnUse);
		tag.setFloat("RadiusPerTick", radiusPerTick);
		tag.setFloat("potion_duration_scale", potionDurationScale);
		tag.setFloat("Radius", getRadius());
		tag.setInteger("etfuturum_particle", particle());
		if (ownerId != null) {
			tag.setString("Owner", ownerId.toString());
		}
		NBTTagList list = new NBTTagList();
		for (PotionEffect effect : effects) {
			list.appendTag(effect.writeCustomPotionEffectToNBT(new NBTTagCompound()));
		}
		tag.setTag("effects", list);
	}
}
