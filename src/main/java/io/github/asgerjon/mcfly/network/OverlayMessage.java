package io.github.asgerjon.mcfly.network;

import io.github.asgerjon.mcfly.network.McFlyNetwork;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import ganymedes01.etfuturum.EtFuturum;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

/**
 * A message shown above the hotbar for three seconds, modern's overlay message (spec §53.3); 1.7.10's server can only
 * send chat. The text goes as a chat component, so the player's own language translates it.
 */
public class OverlayMessage implements IMessage {

	public String json;

	public OverlayMessage() {
	}

	public OverlayMessage(IChatComponent text) {
		json = IChatComponent.Serializer.func_150696_a(text);
	}

	/** Shows the text above the player's hotbar; nothing for a player who isn't on a server connection. */
	public static void send(EntityPlayer player, IChatComponent text) {
		if (player instanceof EntityPlayerMP mp) {
			McFlyNetwork.CHANNEL.sendTo(new OverlayMessage(text), mp);
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		json = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, json);
	}
}
