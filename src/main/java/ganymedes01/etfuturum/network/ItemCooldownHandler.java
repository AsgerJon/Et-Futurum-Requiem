package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.combat.ICombatPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemCooldownHandler implements IMessageHandler<ItemCooldownMessage, IMessage> {

	/** The client starts the same cooldown, and stops using the item if it is in use (an axe lowered the shield). */
	@Override
	public IMessage onMessage(ItemCooldownMessage message, MessageContext ctx) {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		Item item = Item.getItemById(message.itemId);
		if (player instanceof ICombatPlayer combatPlayer && item != null) {
			combatPlayer.etfu$getCooldowns().add(item, message.ticks);
			ItemStack inUse = player.getItemInUse();
			if (inUse != null && inUse.getItem() == item) {
				player.clearItemInUse();
			}
		}
		return null;
	}
}
