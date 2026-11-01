package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item;

/** An item went on cooldown for the player (spec §46.9), as modern's cooldown packet. */
public class ItemCooldownMessage implements IMessage {

	public int itemId;
	public int ticks;

	public ItemCooldownMessage() {
	}

	public ItemCooldownMessage(Item item, int ticks) {
		this.itemId = Item.getIdFromItem(item);
		this.ticks = ticks;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		itemId = buf.readInt();
		ticks = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(itemId);
		buf.writeInt(ticks);
	}
}
