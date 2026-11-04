package ganymedes01.etfuturum.world.end.modern;

import io.github.asgerjon.mcfly.mixins.early.modernend.AccessorEntityInvulnerable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * The end crystal as 26.3's (SPEC-modern-movement.md §76.6, `EndCrystal`), on 1.7.10's `EntityEnderCrystal`: its beam
 * target and base in its data watcher, saved under 26.3's keys; one hit breaks it unless it's invulnerable (a creative
 * player's or the void's hits still do) and never the dragon's; broken, it explodes unless an explosion broke it, and
 * tells the fight; in the End it lights fire where it stands, only on air.
 */
public final class EndCrystals {

	/** Data watcher ids (1.7.10's crystal uses 8 for its health): the target's x, y and z, and the flags. */
	public static final int TARGET_X = 20;
	public static final int TARGET_Y = 22;
	public static final int TARGET_Z = 23;
	public static final int FLAGS = 21;
	private static final int HAS_TARGET = 1;
	private static final int NO_BOTTOM = 2;

	private EndCrystals() {
	}

	public static void define(EntityEnderCrystal crystal) {
		crystal.getDataWatcher().addObject(TARGET_X, 0);
		crystal.getDataWatcher().addObject(TARGET_Y, 0);
		crystal.getDataWatcher().addObject(TARGET_Z, 0);
		crystal.getDataWatcher().addObject(FLAGS, (byte) 0);
	}

	private static int flags(EntityEnderCrystal crystal) {
		return crystal.getDataWatcher().getWatchableObjectByte(FLAGS);
	}

	private static void setFlag(EntityEnderCrystal crystal, int flag, boolean on) {
		int flags = flags(crystal);
		crystal.getDataWatcher().updateObject(FLAGS, (byte) (on ? flags | flag : flags & ~flag));
	}

	public static void setBeamTarget(EntityEnderCrystal crystal, ChunkCoordinates target) {
		if (target != null) {
			crystal.getDataWatcher().updateObject(TARGET_X, target.posX);
			crystal.getDataWatcher().updateObject(TARGET_Y, target.posY);
			crystal.getDataWatcher().updateObject(TARGET_Z, target.posZ);
		}
		setFlag(crystal, HAS_TARGET, target != null);
	}

	public static ChunkCoordinates beamTarget(EntityEnderCrystal crystal) {
		if ((flags(crystal) & HAS_TARGET) == 0) {
			return null;
		}
		return new ChunkCoordinates(crystal.getDataWatcher().getWatchableObjectInt(TARGET_X), crystal.getDataWatcher().getWatchableObjectInt(TARGET_Y),
			crystal.getDataWatcher().getWatchableObjectInt(TARGET_Z));
	}

	public static void setShowBottom(EntityEnderCrystal crystal, boolean show) {
		setFlag(crystal, NO_BOTTOM, !show);
	}

	public static boolean showsBottom(EntityEnderCrystal crystal) {
		return (flags(crystal) & NO_BOTTOM) == 0;
	}

	public static void setInvulnerable(EntityEnderCrystal crystal, boolean invulnerable) {
		((AccessorEntityInvulnerable) crystal).etfu$setInvulnerable(invulnerable);
	}

	public static void save(EntityEnderCrystal crystal, NBTTagCompound tag) {
		ChunkCoordinates target = beamTarget(crystal);
		if (target != null) {
			tag.setTag("beam_target", new NBTTagIntArray(new int[] {target.posX, target.posY, target.posZ}));
		}
		tag.setBoolean("ShowBottom", showsBottom(crystal));
	}

	public static void load(EntityEnderCrystal crystal, NBTTagCompound tag) {
		int[] target = tag.getIntArray("beam_target");
		setBeamTarget(crystal, target.length == 3 ? new ChunkCoordinates(target[0], target[1], target[2]) : null);
		setShowBottom(crystal, !tag.hasKey("ShowBottom") || tag.getBoolean("ShowBottom"));
	}

	/** 26.3's tick: fire where it stands, in the End (where there's a fight), only on air. */
	public static void tick(EntityEnderCrystal crystal) {
		World world = crystal.worldObj;
		if (!world.isRemote && world.provider instanceof WorldProviderModernEnd) {
			int x = MathHelper.floor_double(crystal.posX);
			int y = MathHelper.floor_double(crystal.boundingBox.minY);
			int z = MathHelper.floor_double(crystal.posZ);
			if (world.isAirBlock(x, y, z)) {
				world.setBlock(x, y, z, Blocks.fire);
			}
		}
	}

	/** 26.3's hurt: what it returns is whether the hit counted. */
	public static boolean hurt(EntityEnderCrystal crystal, DamageSource source) {
		Entity attacker = source.getEntity();
		boolean creative = attacker instanceof EntityPlayer player && player.capabilities.isCreativeMode;
		boolean invulnerable = ((AccessorEntityInvulnerable) crystal).etfu$isInvulnerable();
		if (invulnerable && source != DamageSource.outOfWorld && !creative) {
			return false;
		}
		if (attacker instanceof EntityDragon || attacker instanceof EntityDragonPart) {
			return false;
		}
		if (crystal.isDead || crystal.worldObj.isRemote) {
			return !crystal.isDead;
		}
		crystal.setDead();
		// /kill and the void: removed, no explosion (26.3's kill); an explosion's hit: no second one (no chain)
		if (source != DamageSource.outOfWorld && !source.isExplosion()) {
			crystal.worldObj.createExplosion(crystal, crystal.posX, crystal.posY, crystal.posZ, 6.0F, true);
		}
		EnderDragonFight fight = EnderDragonFight.ifThere(crystal.worldObj);
		if (fight != null) {
			fight.onCrystalDestroyed(crystal, source);
		}
		return true;
	}
}
