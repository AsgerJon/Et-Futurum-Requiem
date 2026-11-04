package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.particle.McFlyParticlesClient;

/** The client's side of 26.3's level particles packet (SPEC-modern-movement.md §79, B7). */
public class ParticleHandler implements IMessageHandler<ParticleMessage, IMessage> {

	@Override
	public IMessage onMessage(ParticleMessage message, MessageContext ctx) {
		handleMessage(message);
		return null;
	}

	@SideOnly(Side.CLIENT)
	private void handleMessage(ParticleMessage message) {
		McFlyParticlesClient.handle(message);
	}
}
