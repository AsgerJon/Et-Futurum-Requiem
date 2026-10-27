package ganymedes01.etfuturum.skins;

import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Which parts of a player's skin are shown, kept in a synced byte on the player as modern keeps its model
 * customisation (1.8+): the server sets it from the player's settings message and every client reads it.
 */
public final class SkinParts {

	public static final int ALL = (1 << SkinPart.VALUES.length) - 1;

	private SkinParts() {
	}

	/** Whether the player shows this part; everything is shown while the byte is not there. */
	public static boolean shown(EntityPlayer player, SkinPart part) {
		try {
			return (player.getDataWatcher().getWatchableObjectByte(ConfigFunctions.skinPartsDataWatcherId) & part.mask()) != 0;
		} catch (NullPointerException | IllegalArgumentException e) {
			return true;
		}
	}

	/** Server side: the player's parts, as the message brought them. */
	public static void set(EntityPlayer player, int mask) {
		if (!player.worldObj.isRemote) {
			player.getDataWatcher().updateObject(ConfigFunctions.skinPartsDataWatcherId, (byte) (mask & ALL));
		}
	}

	/** The local settings as a byte. */
	public static int localMask() {
		int mask = 0;
		boolean[] shown = ConfigFunctions.skinPartsShown();
		for (SkinPart part : SkinPart.VALUES) {
			if (shown[part.ordinal()]) {
				mask |= part.mask();
			}
		}
		return mask;
	}
}
