package ganymedes01.etfuturum.core.utils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Whether a player has seen the end credits (modern's seenCredits, §53.2). Kept in Forge's persisted player data, which
 * is saved with the player and carried over to the new entity on every respawn, the End's exit included.
 */
public final class EndCredits {

	/** The value of the credits notice (game state 4) that tells the client to skip them and respawn at once. */
	public static final float SKIP = 1.0F;

	private static final String SEEN = "etfuturum:seenCredits";

	private EndCredits() {
	}

	public static boolean hasSeen(EntityPlayer player) {
		NBTTagCompound data = player.getEntityData();
		return data.hasKey(EntityPlayer.PERSISTED_NBT_TAG) && data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getBoolean(SEEN);
	}

	public static void setSeen(EntityPlayer player, boolean seen) {
		NBTTagCompound data = player.getEntityData();
		NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		persisted.setBoolean(SEEN, seen);
		data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
	}
}
