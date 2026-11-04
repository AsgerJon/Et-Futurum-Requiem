package ganymedes01.etfuturum.entities;

import io.github.asgerjon.mcfly.registry.McFlyItems;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.client.particle.CustomParticles;
import ganymedes01.etfuturum.core.utils.EntityEvents;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.block.material.Material;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

/**
 * The glow squid (SPEC-modern-movement.md §67.1, 26.3's `GlowSquid`): a squid that glows. Hurt, it goes dark for 100
 * ticks (synced) and its light comes back over the last 10; a glow particle every tick; hurt by a mob it squirts glow
 * ink; it drops 1 to 3 glow ink sacs (Looting adds up to its level); it may spawn only in dark water at y 30 or below.
 */
public class EntityGlowSquid extends EntitySquid {

	public static final String AMBIENT = Reference.MCAssetVer + ":entity.glow_squid.ambient";
	public static final String HURT = Reference.MCAssetVer + ":entity.glow_squid.hurt";
	public static final String DEATH = Reference.MCAssetVer + ":entity.glow_squid.death";
	public static final String SQUIRT = Reference.MCAssetVer + ":entity.glow_squid.squirt";

	/** 1.7.10's squids use no DataWatcher id above 11; 16 is a mob's own, as other mobs' first. */
	private static final int DARK_TICKS = 16;
	public static final int DARK_WHEN_HURT = 100;
	/** 26.3's sea level - 33, with 1.7.10's sea level 63. */
	public static final int HIGHEST_SPAWN = 63 - 33;

	public EntityGlowSquid(World world) {
		super(world);
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		dataWatcher.addObject(DARK_TICKS, 0);
	}

	public int getDarkTicks() {
		return dataWatcher.getWatchableObjectInt(DARK_TICKS);
	}

	private void setDarkTicks(int ticks) {
		dataWatcher.updateObject(DARK_TICKS, ticks);
	}

	/**
	 * The block light it's drawn with (26.3's `GlowSquidRenderer.getBlockLightLevel`): 15 x (1 - dark / 10), clamped to
	 * 0..15 and cut to a whole number; full stays full, else the place's when that is more.
	 */
	public static int glowLight(int darkTicks, int blockLight) {
		int glow = (int) MathHelper.clamp_float(15.0F * (1.0F - darkTicks / 10.0F), 0.0F, 15.0F);
		return glow == 15 ? 15 : Math.max(glow, blockLight);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getBrightnessForRender(float partialTicks) {
		int lit = super.getBrightnessForRender(partialTicks);
		int block = glowLight(getDarkTicks(), lit >> 4 & 15);
		return (lit & ~0xFFFF) | block << 4;
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		int dark = getDarkTicks();
		if (dark > 0 && !worldObj.isRemote) {
			setDarkTicks(dark - 1);
		}
		if (worldObj.isRemote) {
			CustomParticles.spawnGlowSquidGlow(worldObj,
				posX + width * (2.0D * rand.nextDouble() - 1.0D) * 0.6D,
				boundingBox.minY + height * rand.nextDouble(),
				posZ + width * (2.0D * rand.nextDouble() - 1.0D) * 0.6D);
		}
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float damage) {
		boolean hurt = super.attackEntityFrom(source, damage);
		if (hurt && !worldObj.isRemote) {
			setDarkTicks(DARK_WHEN_HURT);
			// 26.3's Squid.hurtServer: ink only when a mob hurt it (getLastHurtByMob)
			if (getAITarget() != null) {
				playSound(SQUIRT, getSoundVolume(), getSoundPitch());
				worldObj.setEntityState(this, EntityEvents.GLOW_SQUID_INK);
			}
		}
		return hurt;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void handleHealthUpdate(byte event) {
		if (event == EntityEvents.GLOW_SQUID_INK) {
			spawnInk();
		} else {
			super.handleHealthUpdate(event);
		}
	}

	/** 26.3's `Squid.spawnInk`: 30 puffs from below its body, turned as the body is, out along it. */
	@SideOnly(Side.CLIENT)
	private void spawnInk() {
		Vec3 at = turned(0.0D, -1.0D, 0.0D);
		for (int i = 0; i < 30; i++) {
			Vec3 dir = turned(rand.nextFloat() * 0.6D - 0.3D, -1.0D, rand.nextFloat() * 0.6D - 0.3D);
			double scale = (0.3D + rand.nextFloat() * 2.0D) * 0.1D;
			CustomParticles.spawnGlowSquidInk(worldObj, posX + at.xCoord, posY + at.yCoord + 0.5D, posZ + at.zCoord,
				dir.xCoord * scale, dir.yCoord * scale, dir.zCoord * scale);
		}
	}

	/** The vector turned by the body's pitch, then its yaw (26.3's `rotateVector`: xBodyRot, then -yBodyRot). */
	private Vec3 turned(double x, double y, double z) {
		Vec3 v = Vec3.createVectorHelper(x, y, z);
		v.rotateAroundX(squidPitch * (float) Math.PI / 180.0F);
		v.rotateAroundY(-renderYawOffset * (float) Math.PI / 180.0F);
		return v;
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

	/** 26.3's loot (`entities/glow_squid`): 1 to 3 glow ink sacs, plus round(Looting level x random). */
	@Override
	protected void dropFewItems(boolean recentlyHit, int looting) {
		if (!McFlyItems.GLOW_INK_SAC.isEnabled()) {
			return;
		}
		int count = 1 + rand.nextInt(3) + Math.round(looting * rand.nextFloat());
		entityDropItem(McFlyItems.GLOW_INK_SAC.newItemStack(count), 0.0F);
	}

	/** 26.3's `checkGlowSquidSpawnRules`: at y 30 or below, in water, with no light of either kind. */
	@Override
	public boolean getCanSpawnHere() {
		int x = MathHelper.floor_double(posX), y = MathHelper.floor_double(boundingBox.minY), z = MathHelper.floor_double(posZ);
		return canSpawnAt(worldObj, x, y, z) && worldObj.checkNoEntityCollision(boundingBox);
	}

	public static boolean canSpawnAt(World world, int x, int y, int z) {
		return y <= HIGHEST_SPAWN && world.getBlock(x, y, z).getMaterial() == Material.water
			&& world.getSavedLightValue(EnumSkyBlock.Sky, x, y, z) == 0 && world.getSavedLightValue(EnumSkyBlock.Block, x, y, z) == 0;
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound tag) {
		super.writeEntityToNBT(tag);
		tag.setInteger("DarkTicksRemaining", getDarkTicks());
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound tag) {
		super.readEntityFromNBT(tag);
		setDarkTicks(tag.getInteger("DarkTicksRemaining"));
	}
}
