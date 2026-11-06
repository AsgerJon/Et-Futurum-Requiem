package io.github.asgerjon.mcfly.sculk;

import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.3's `WardenSpawnTracker` (SPEC-modern-movement.md §90.9, step H4): a player's warden warning, raised by a
 * shrieker that can summon. It drops a level each 12000 ticks; a warning raises the highest of the players about to 4
 * with 200 cooldown and copies it to them. Saved with the player as `warden_spawn_tracker`.
 */
public class WardenSpawnTracker {

	public static final int MAX_WARNING_LEVEL = 4;

	private int ticksSinceLastWarning;
	private int warningLevel;
	private int cooldownTicks;

	public void tick() {
		if (ticksSinceLastWarning >= 12000) {
			setWarningLevel(warningLevel - 1);
			ticksSinceLastWarning = 0;
		} else {
			ticksSinceLastWarning++;
		}
		if (cooldownTicks > 0) {
			cooldownTicks--;
		}
	}

	public void reset() {
		ticksSinceLastWarning = 0;
		warningLevel = 0;
		cooldownTicks = 0;
	}

	public int warningLevel() {
		return warningLevel;
	}

	public void setWarningLevel(int level) {
		warningLevel = Math.max(0, Math.min(MAX_WARNING_LEVEL, level));
	}

	private boolean onCooldown() {
		return cooldownTicks > 0;
	}

	private void increaseWarningLevel() {
		if (!onCooldown()) {
			ticksSinceLastWarning = 0;
			cooldownTicks = 200;
			setWarningLevel(warningLevel + 1);
		}
	}

	private void copyData(WardenSpawnTracker from) {
		warningLevel = from.warningLevel;
		cooldownTicks = from.cooldownTicks;
		ticksSinceLastWarning = from.ticksSinceLastWarning;
	}

	/**
	 * 26.3's `tryWarn`: none if a warden is within 48 (McFly has none yet) or a player about is on cooldown; else the
	 * highest warning of the players within 16 (and the trigger) goes up, copied to them all. The new level, or -1.
	 */
	public static int tryWarn(World world, int x, int y, int z, EntityPlayerMP trigger) {
		List<EntityPlayerMP> players = nearbyPlayers(world, x + 0.5D, y + 0.5D, z + 0.5D);
		if (!players.contains(trigger)) {
			players.add(trigger);
		}
		WardenSpawnTracker highest = null;
		for (EntityPlayerMP player : players) {
			WardenSpawnTracker tracker = of(player);
			if (tracker.onCooldown()) {
				return -1;
			}
			if (highest == null || tracker.warningLevel > highest.warningLevel) {
				highest = tracker;
			}
		}
		if (highest == null) {
			return -1;
		}
		highest.increaseWarningLevel();
		for (EntityPlayerMP player : players) {
			of(player).copyData(highest);
		}
		return highest.warningLevel;
	}

	private static List<EntityPlayerMP> nearbyPlayers(World world, double x, double y, double z) {
		List<EntityPlayerMP> players = new ArrayList<>();
		for (Object entry : world.playerEntities) {
			EntityPlayerMP player = (EntityPlayerMP) entry;
			double dx = player.posX - x, dy = player.boundingBox.minY - y, dz = player.posZ - z;
			if (!Spectator.isSpectator(player) && player.isEntityAlive() && dx * dx + dy * dy + dz * dz < 16.0D * 16.0D) {
				players.add(player);
			}
		}
		return players;
	}

	public static WardenSpawnTracker of(EntityPlayer player) {
		return ((WardenWarned) player).mcfly$wardenSpawnTracker();
	}

	public void writeToNBT(NBTTagCompound tag) {
		tag.setInteger("ticks_since_last_warning", ticksSinceLastWarning);
		tag.setInteger("warning_level", warningLevel);
		tag.setInteger("cooldown_ticks", cooldownTicks);
	}

	public void readFromNBT(NBTTagCompound tag) {
		ticksSinceLastWarning = Math.max(0, tag.getInteger("ticks_since_last_warning"));
		setWarningLevel(tag.getInteger("warning_level"));
		cooldownTicks = Math.max(0, tag.getInteger("cooldown_ticks"));
	}
}
