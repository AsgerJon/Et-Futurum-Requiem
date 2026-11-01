package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.gui.OverlayMessageHud;
import net.minecraft.util.IChatComponent;

public class OverlayMessageHandler implements IMessageHandler<OverlayMessage, IMessage> {

	@Override
	public IMessage onMessage(OverlayMessage message, MessageContext ctx) {
		handleMessage(message);
		return null;
	}

	@SideOnly(Side.CLIENT)
	private void handleMessage(OverlayMessage message) {
		OverlayMessageHud.show(IChatComponent.Serializer.func_150699_a(message.json));
	}
}
