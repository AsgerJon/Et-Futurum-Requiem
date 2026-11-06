package io.github.asgerjon.mcfly.boat.client;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.asgerjon.mcfly.boat.BoatSeat;
import io.github.asgerjon.mcfly.boat.McFlyBoat;
import io.github.asgerjon.mcfly.boat.McFlyChestBoat;
import io.github.asgerjon.mcfly.boat.OpenBoatInventoryMessage;
import io.github.asgerjon.mcfly.network.McFlyNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * The boats' client half (SPEC-modern-movement.md 91.12): their renderer, the inventory key in a chest boat opening its
 * chest (26.3's `HasCustomInventoryScreen`), the server's correction of a boat this client steers.
 */
public final class BoatClient {

	public static final BoatClient INSTANCE = new BoatClient();

	private BoatClient() {
	}

	/** In init, on the client. */
	public static void register() {
		RenderingRegistry.registerEntityRenderingHandler(McFlyBoat.class, new BoatRenderer());
		RenderingRegistry.registerEntityRenderingHandler(McFlyChestBoat.class, new BoatRenderer());
		RenderingRegistry.registerEntityRenderingHandler(BoatSeat.class, new BoatRenderer.Seat());
		MinecraftForge.EVENT_BUS.register(INSTANCE);
	}

	@SubscribeEvent
	public void onOpenScreen(GuiOpenEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (event.gui instanceof GuiInventory && mc.thePlayer != null && mc.thePlayer.ridingEntity instanceof McFlyChestBoat) {
			event.setCanceled(true);
			McFlyNetwork.CHANNEL.sendToServer(new OpenBoatInventoryMessage());
		}
	}

	/** {@link io.github.asgerjon.mcfly.boat.BoatMovedBackMessage}: put there, if this client steers it. */
	public static void movedBack(int entityId, double x, double y, double z, float yaw, float pitch) {
		Minecraft mc = Minecraft.getMinecraft();
		Entity entity = mc.theWorld == null ? null : mc.theWorld.getEntityByID(entityId);
		if (entity instanceof McFlyBoat boat && boat.isAuthoritative()) {
			boat.snapTo(x, y, z, yaw, pitch);
		}
	}
}
