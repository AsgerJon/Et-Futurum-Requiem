package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/**
 * McFly's own network channel (SPEC-modern-movement.md §79, B4), carrying our messages (EFR's channel keeps EFR's).
 * Their numbers are in the order EFR's channel had them.
 */
public final class McFlyNetwork {

	public static final String NAME = "mcfly";
	public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(NAME);

	private McFlyNetwork() {
	}

	/** Every message, both sides (pre-init). */
	public static void register() {
		CHANNEL.registerMessage(SwapHandsHandler.class, SwapHandsMessage.class, 0, Side.SERVER);
		CHANNEL.registerMessage(UseOffhandHandler.class, UseOffhandMessage.class, 1, Side.SERVER);
		CHANNEL.registerMessage(PlayerSettingsHandler.class, PlayerSettingsMessage.class, 2, Side.SERVER);
		CHANNEL.registerMessage(OfflineSkinHandler.class, OfflineSkinMessage.class, 3, Side.CLIENT);
		CHANNEL.registerMessage(CommandTreeHandler.class, CommandTreeMessage.class, 4, Side.CLIENT);
		CHANNEL.registerMessage(CommandSuggestionRequestHandler.class, CommandSuggestionRequestMessage.class, 5, Side.SERVER);
		CHANNEL.registerMessage(CommandSuggestionsHandler.class, CommandSuggestionsMessage.class, 6, Side.CLIENT);
		CHANNEL.registerMessage(PunchHandler.class, PunchMessage.class, 7, Side.SERVER);
		CHANNEL.registerMessage(ItemCooldownHandler.class, ItemCooldownMessage.class, 8, Side.CLIENT);
		CHANNEL.registerMessage(SweepParticleHandler.class, SweepParticleMessage.class, 9, Side.CLIENT);
		CHANNEL.registerMessage(OverlayMessageHandler.class, OverlayMessage.class, 10, Side.CLIENT);
		CHANNEL.registerMessage(FreezingHandler.class, FreezingMessage.class, 11, Side.CLIENT);
		CHANNEL.registerMessage(SnowflakeHandler.class, SnowflakeMessage.class, 12, Side.CLIENT);
		CHANNEL.registerMessage(StrayConversionHandler.class, StrayConversionMessage.class, 13, Side.CLIENT);
		CHANNEL.registerMessage(EffectTableHandler.class, EffectTableMessage.class, 14, Side.CLIENT);
		CHANNEL.registerMessage(EndBossBarHandler.class, EndBossBarMessage.class, 15, Side.CLIENT);
		CHANNEL.registerMessage(DragonBreathBurstHandler.class, DragonBreathBurstMessage.class, 16, Side.CLIENT);
		CHANNEL.registerMessage(LevelEventHandler.class, LevelEventMessage.class, 17, Side.CLIENT);
	}
}
