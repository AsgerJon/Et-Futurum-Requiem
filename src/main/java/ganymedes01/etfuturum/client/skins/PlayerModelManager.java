package ganymedes01.etfuturum.client.skins;

import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.skins.SkinTextures;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class PlayerModelManager {

	public static final String MODEL_KEY = Reference.MOD_ID + "_model";

	public static Map<UUID, Boolean> alexCache = new WeakHashMap<>();

	public static boolean isPlayerModelAlex(EntityPlayer player) {
		if (player == null || player.getUniqueID() == null)
			return false;

		Boolean isAlex = alexCache.get(player.getUniqueID());
		if (isAlex == null) {
			NBTTagCompound nbt = player.getEntityData();
			if (nbt.hasKey(MODEL_KEY, Constants.NBT.TAG_BYTE)) {
				nbt.removeTag(MODEL_KEY);
			}
			// The skin data on the profile (an online server's, or an offline server's lookup) says which model it is.
			Boolean fromProfile = SkinTextures.slim(player.getGameProfile());
			if (fromProfile != null) {
				alexCache.put(player.getUniqueID(), fromProfile);
				return fromProfile;
			}
			// Steve until the check answers: one check per player, not one thread per frame.
			alexCache.put(player.getUniqueID(), false);
			ThreadCheckAlex skinthread = new ThreadCheckAlex();
			skinthread.startWithArgs(player.getUniqueID());
			return false;
		}
		return isAlex;
	}
}