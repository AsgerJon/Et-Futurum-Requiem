package io.github.asgerjon.mcfly.network;

import io.github.asgerjon.mcfly.McFly;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityHugeExplodeFX;
import net.minecraft.world.World;

import java.util.Random;

/** 26.3's LevelEventHandler for the level events EFR sends (on the client's thread: 1.7.10 hands messages over on netty's). */
public class LevelEventHandler implements IMessageHandler<LevelEventMessage, IMessage> {

	@Override
	public IMessage onMessage(LevelEventMessage message, MessageContext ctx) {
		Minecraft.getMinecraft().func_152344_a(() -> handle(message));
		return null;
	}

	private static void handle(LevelEventMessage message) {
		Minecraft mc = Minecraft.getMinecraft();
		World world = mc.theWorld;
		if (world == null) {
			return;
		}
		Random random = world.rand;
		double x = message.x + 0.5;
		double y = message.y + 0.5;
		double z = message.z + 0.5;
		if (message.event == 3000) {
			// an end gateway appearing: the explosion emitter (always shown), the sound (10, 0.7 * (1 +- 0.2))
			mc.effectRenderer.addEffect(new EntityHugeExplodeFX(world, x, y, z, 0.0, 0.0, 0.0));
			world.playSound(x, y, z, McFly.MC_ASSETS + ":block.end_gateway.spawn", 10.0F, (1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F) * 0.7F, false);
		} else if (message.event == 2016) {
			// a shulker's teleport (26.3's singleBlockTeleportationParticles): 128 portal particles strewn along the line
			// from its new block to its old, each moving up to 0.1 each way
			int toX = message.x + (message.data >> 16 & 0xFF) - 8;
			int toY = message.y + (message.data >> 8 & 0xFF) - 8;
			int toZ = message.z + (message.data & 0xFF) - 8;
			for (int particle = 0; particle < 128; particle++) {
				double along = random.nextDouble();
				float speedX = (random.nextFloat() - 0.5F) * 0.2F;
				float speedY = (random.nextFloat() - 0.5F) * 0.2F;
				float speedZ = (random.nextFloat() - 0.5F) * 0.2F;
				double px = toX + (message.x - toX) * along + (random.nextDouble() - 0.5) + 0.5;
				double py = toY + (message.y - toY) * along + random.nextDouble() - 0.5;
				double pz = toZ + (message.z - toZ) * along + (random.nextDouble() - 0.5) + 0.5;
				world.spawnParticle("portal", px, py, pz, speedX, speedY, speedZ);
			}
		}
	}
}
