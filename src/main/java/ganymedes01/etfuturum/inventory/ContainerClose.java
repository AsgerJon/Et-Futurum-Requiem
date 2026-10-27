package ganymedes01.etfuturum.inventory;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * What a closing container holds goes back into the player's inventory, as modern does: the stack on the cursor and
 * the inputs of crafting grids, anvils, enchanting and smithing tables. Only what does not fit drops, and everything
 * drops for a dead or disconnected player, whose inventory is no longer the place for it. 1.7.10 dropped all of it,
 * and its client spawned a copy of each drop of its own.
 */
public final class ContainerClose {

	private ContainerClose() {
	}

	/** Takes the stack back; false when the caller should drop it as before. */
	public static boolean giveBack(EntityPlayer player, ItemStack stack) {
		if (!ConfigMixins.itemsBackOnClose || stack == null) {
			return false;
		}
		if (player.worldObj.isRemote) {
			return true;
		}
		if (!player.isEntityAlive() || player instanceof EntityPlayerMP mp && (mp.playerNetServerHandler == null || !mp.playerNetServerHandler.netManager.isChannelOpen())) {
			return false;
		}
		player.inventory.addItemStackToInventory(stack);
		if (stack.stackSize > 0) {
			player.dropPlayerItemWithRandomChoice(stack, false);
		}
		return true;
	}
}
