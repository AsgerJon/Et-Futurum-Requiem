package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.entities.Freezing;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class FreezingHandler implements IMessageHandler<FreezingMessage, IMessage> {

	@Override
	public IMessage onMessage(FreezingMessage message, MessageContext ctx) {
		handleMessage(message);
		return null;
	}

	@SideOnly(Side.CLIENT)
	private void handleMessage(FreezingMessage message) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.theWorld != null) {
			Entity entity = mc.theWorld.getEntityByID(message.entityId);
			if (entity != null) {
				Freezing.setTicksFrozen(entity, message.ticks);
			}
		}
	}
}
