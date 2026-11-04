package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.combat.ICombatPlayer;
import net.minecraft.network.NetHandlerPlayServer;

public class PunchHandler implements IMessageHandler<PunchMessage, IMessage> {

	/**
	 * Every left click restarts the player's attack and swap timers on the server (§46.1). 1.7.10's swing packet
	 * can't say this: it comes with every swing, mining included, and before the attack.
	 */
	@Override
	public IMessage onMessage(PunchMessage message, MessageContext ctx) {
		if (((NetHandlerPlayServer) ctx.netHandler).playerEntity instanceof ICombatPlayer player) {
			player.etfu$resetAttackStrength();
		}
		return null;
	}
}
