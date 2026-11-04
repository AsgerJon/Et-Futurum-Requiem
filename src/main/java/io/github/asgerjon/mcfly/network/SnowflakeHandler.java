package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.particle.CustomParticles;
import net.minecraft.client.Minecraft;

public class SnowflakeHandler implements IMessageHandler<SnowflakeMessage, IMessage> {

	@Override
	public IMessage onMessage(SnowflakeMessage message, MessageContext ctx) {
		handleMessage(message);
		return null;
	}

	@SideOnly(Side.CLIENT)
	private void handleMessage(SnowflakeMessage message) {
		if (Minecraft.getMinecraft().theWorld != null) {
			CustomParticles.spawnSnowflake(Minecraft.getMinecraft().theWorld, message.x, message.y, message.z,
					message.motionX, message.motionY, message.motionZ);
		}
	}
}
