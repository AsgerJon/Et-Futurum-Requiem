package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.offhand.Offhand;
import ganymedes01.etfuturum.skins.SkinParts;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;

/** The server stores the player's shown skin parts and main hand on the entity, where every client reads them. */
public class PlayerSettingsHandler implements IMessageHandler<PlayerSettingsMessage, IMessage> {
	@Override
	public IMessage onMessage(PlayerSettingsMessage message, MessageContext ctx) {
		EntityPlayer player = ((NetHandlerPlayServer) ctx.netHandler).playerEntity;
		if (ConfigMixins.skinCustomization) {
			SkinParts.set(player, message.skinParts);
		}
		if (Offhand.isEnabled()) {
			Offhand.setPoseBit(player, Offhand.LEFT_HANDED_BIT, message.leftHanded);
		}
		return null;
	}
}
