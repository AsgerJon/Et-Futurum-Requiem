package ganymedes01.etfuturum.network;

import com.mojang.authlib.properties.Property;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.skins.OfflineSkinsClient;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;

public class OfflineSkinHandler implements IMessageHandler<OfflineSkinMessage, IMessage> {

	@Override
	public IMessage onMessage(OfflineSkinMessage message, MessageContext ctx) {
		handleMessage(message);
		return null;
	}

	@SideOnly(Side.CLIENT)
	private void handleMessage(OfflineSkinMessage message) {
		if (ConfigMixins.offlineSkins) {
			OfflineSkinsClient.apply(message.name, new Property("textures", message.value, message.signature));
		}
	}
}
