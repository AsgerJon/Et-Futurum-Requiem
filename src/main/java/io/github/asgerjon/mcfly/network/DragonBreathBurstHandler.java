package io.github.asgerjon.mcfly.network;

import io.github.asgerjon.mcfly.McFly;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.client.particle.DragonBreathFX;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/** 26.3's burst: 200 breath particles thrown out from the block (at the raw block coordinates, as 26.3), its sound. */
public class DragonBreathBurstHandler implements IMessageHandler<DragonBreathBurstMessage, IMessage> {

	@Override
	public IMessage onMessage(DragonBreathBurstMessage message, MessageContext ctx) {
		// on the client's thread (1.7.10 hands messages over on netty's; particles off it can break the particle list)
		Minecraft.getMinecraft().func_152344_a(() -> burst(message));
		return null;
	}

	private static void burst(DragonBreathBurstMessage message) {
		World world = Minecraft.getMinecraft().theWorld;
		if (world == null) {
			return;
		}
		Random random = world.rand;
		for (int i = 0; i < 200; i++) {
			float distance = random.nextFloat() * 4.0F;
			float angle = random.nextFloat() * (float) (Math.PI * 2);
			double vx = MathHelper.cos(angle) * distance;
			double vy = 0.01 + random.nextDouble() * 0.5;
			double vz = MathHelper.sin(angle) * distance;
			DragonBreathFX.spawn(world, message.x + vx * 0.1, message.y + 0.3, message.z + vz * 0.1, vx, vy, vz);
		}
		if (message.sound) {
			world.playSound(message.x + 0.5, message.y + 0.5, message.z + 0.5, McFly.MC_ASSETS + ":entity.dragon_fireball.explode", 1.0F, 0.9F + random.nextFloat() * 0.1F, false);
		}
	}
}
