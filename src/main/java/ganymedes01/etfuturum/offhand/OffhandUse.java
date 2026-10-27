package ganymedes01.etfuturum.offhand;

import cpw.mods.fml.common.eventhandler.Event;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.elytra.FallFlying;
import ganymedes01.etfuturum.network.UseOffhandMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.function.Supplier;

/**
 * Using the off-hand item (SPEC-modern-movement.md §36.2, §36.8). The main hand goes first, exactly as 1.7.10 does;
 * a click it didn't use goes to the off-hand: the same steps (entity, block, air) with the off-hand item, predicted
 * on the client (OffhandUseClient) and sent to the server as an EFR message, handled here. Both sides run vanilla's
 * own use code with the two stacks swapped for its duration, so items and mod hooks see "the held item" and need no
 * changes; an item use that starts during the swap is marked as the off-hand's afterwards. This class stays free of
 * client-only classes, since a dedicated server loads it.
 */
public final class OffhandUse {

	private OffhandUse() {
	}

	/** Runs the action with the off-hand item in the main-hand slot, then puts everything back. */
	public static <T> T withOffhandInMainHand(EntityPlayer player, Supplier<T> action) {
		InventoryPlayer inventory = player.inventory;
		int current = inventory.currentItem;
		ItemStack main = inventory.mainInventory[current];
		inventory.mainInventory[current] = Offhand.getOffhand(player);
		Offhand.setOffhand(player, main);
		try {
			return action.get();
		} finally {
			ItemStack usedOffhand = inventory.mainInventory[current];
			inventory.mainInventory[current] = Offhand.getOffhand(player);
			Offhand.setOffhand(player, usedOffhand);
			// Not EntityPlayer.getItemInUse(): that getter is client-only and a dedicated server has no such method.
			if (usedOffhand != null && player instanceof IOffhandPlayer p && p.etfu$getItemInUse() == usedOffhand) {
				p.etfu$setUsingHand(Hand.OFF);
			}
		}
	}

	/**
	 * Main-hand uses that take the click without changing the stack, which is all 1.7.10's client can see of an air
	 * use: the firework boost while gliding (§20; modern's FireworkRocketItem.use returns SUCCESS then). Mirrors the
	 * condition of ServerEventHandler.onPlayerInteractNonVanilla.
	 */
	public static boolean consumesClickWithoutChange(EntityPlayer player) {
		ItemStack held = player.getHeldItem();
		return ConfigMixins.enableElytra && held != null && held.getItem() == Items.fireworks && FallFlying.isFallFlying(player);
	}

	/** The server's side of a message: vanilla's checks, then vanilla's use code with the hands swapped. */
	public static void handleOnServer(EntityPlayerMP player, UseOffhandMessage message) {
		if (!Offhand.isEnabled() || Offhand.getOffhand(player) == null) {
			return;
		}
		WorldServer world = (WorldServer) player.worldObj;
		player.func_143004_u();
		switch (message.kind) {
			case UseOffhandMessage.ENTITY: {
				Entity entity = world.getEntityByID(message.entityId);
				if (entity == null) {
					return;
				}
				double reach = player.canEntityBeSeen(entity) ? 36.0D : 9.0D;
				if (player.getDistanceSqToEntity(entity) < reach) {
					withOffhandInMainHand(player, () -> player.interactWith(entity));
				}
				break;
			}
			case UseOffhandMessage.BLOCK: {
				int x = message.x, y = message.y, z = message.z, side = message.side;
				int buildLimit = player.mcServer.getBuildLimit();
				if (y >= buildLimit - 1 && (side == 1 || y >= buildLimit)) {
					ChatComponentTranslation text = new ChatComponentTranslation("build.tooHigh", buildLimit);
					text.getChatStyle().setColor(EnumChatFormatting.RED);
					player.addChatMessage(text);
				} else {
					double reach = player.theItemInWorldManager.getBlockReachDistance() + 1;
					if (player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) < reach * reach && !player.mcServer.isBlockProtected(world, x, y, z, player)) {
						withOffhandInMainHand(player, () -> player.theItemInWorldManager.activateBlockOrUseItem(player, world, player.inventory.getCurrentItem(), x, y, z, side, message.hitX, message.hitY, message.hitZ));
					}
				}
				player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(x, y, z, world));
				int nx = x + (side == 5 ? 1 : side == 4 ? -1 : 0), ny = y + (side == 1 ? 1 : side == 0 ? -1 : 0), nz = z + (side == 3 ? 1 : side == 2 ? -1 : 0);
				player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(nx, ny, nz, world));
				break;
			}
			case UseOffhandMessage.AIR:
			default:
				withOffhandInMainHand(player, () -> {
					PlayerInteractEvent event = ForgeEventFactory.onPlayerInteract(player, PlayerInteractEvent.Action.RIGHT_CLICK_AIR, 0, 0, 0, -1, world);
					if (event.useItem != Event.Result.DENY) {
						ItemStack held = player.inventory.getCurrentItem();
						if (held != null) {
							player.theItemInWorldManager.tryUseItem(player, world, held);
						}
					}
					return null;
				});
				break;
		}
		// A stack the use emptied leaves the hand. Vanilla's block use leaves a zero-count stack behind and its
		// placement packet handler clears it afterwards; this message is that handler for the off-hand.
		ItemStack offhand = Offhand.getOffhand(player);
		if (offhand != null && offhand.stackSize <= 0) {
			Offhand.setOffhand(player, null);
		}
		// As vanilla: no resend while a use is going on, since it would hand the client new stack objects and end
		// the use it is predicting (a bow draw, eating); the use's own end syncs the slots.
		if (!player.isUsingItem()) {
			player.sendContainerToPlayer(player.inventoryContainer);
		}
	}
}
