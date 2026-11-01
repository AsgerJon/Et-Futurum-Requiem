package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.particle.CustomParticles;
import net.minecraft.client.Minecraft;

public class SweepParticleHandler implements IMessageHandler<SweepParticleMessage, IMessage> {

	@Override
	public IMessage onMessage(SweepParticleMessage message, MessageContext ctx) {
		handleMessage(message);
		return null;
	}

	@SideOnly(Side.CLIENT)
	private void handleMessage(SweepParticleMessage message) {
		CustomParticles.spawnSweepAttack(Minecraft.getMinecraft().theWorld, message.x, message.y, message.z);
	}
}
