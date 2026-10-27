package ganymedes01.etfuturum.client.skins;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;

/**
 * The local player's side of {@link ganymedes01.etfuturum.skins.OfflineSkins}: the skin data the server found for
 * this name is applied to the current player entity, whose skin is loaded again, and to every one made after it.
 */
@SideOnly(Side.CLIENT)
public final class OfflineSkinsClient {

	private static String name;
	private static Property textures;

	private OfflineSkinsClient() {
	}

	public static void apply(String forName, Property found) {
		Minecraft mc = Minecraft.getMinecraft();
		if (forName == null || found == null || !forName.equals(mc.getSession().getUsername())) {
			return;
		}
		name = forName;
		textures = found;
		EntityClientPlayerMP player = mc.thePlayer;
		if (player != null && forName.equals(player.getCommandSenderName())) {
			decorate(player.getGameProfile());
			PlayerModelManager.alexCache.remove(player.getUniqueID());
			mc.func_152342_ad().func_152790_a(player.getGameProfile(), player, true);
		}
	}

	/** Puts the skin data on a profile of the name it was found for. */
	public static GameProfile decorate(GameProfile profile) {
		if (textures != null && profile != null && name.equals(profile.getName())) {
			profile.getProperties().removeAll("textures");
			profile.getProperties().put("textures", textures);
		}
		return profile;
	}

	/** On leaving a server: the next one answers for itself. */
	public static void forget() {
		name = null;
		textures = null;
	}
}
