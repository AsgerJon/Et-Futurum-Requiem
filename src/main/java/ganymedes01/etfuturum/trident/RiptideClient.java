package ganymedes01.etfuturum.trident;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

/** The client's part of Riptide's spin: only the local player's own client sends its spin hit, as an attack. */
@SideOnly(Side.CLIENT)
final class RiptideClient {

	private RiptideClient() {
	}

	/** True when this tick of the local player's spin hit a mob or a wall (the spin is over). */
	static boolean spinOfTheLocalPlayer(EntityPlayer player, AxisAlignedBB before) {
		Minecraft mc = Minecraft.getMinecraft();
		return player == mc.thePlayer && Riptide.hitFirstMob(player, before, target -> mc.playerController.attackEntity(player, target));
	}
}
