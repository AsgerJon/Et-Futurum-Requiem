package ganymedes01.etfuturum.offhand;

import cpw.mods.fml.common.eventhandler.Event;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.network.UseOffhandMessage;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * The client's side of using the off-hand item (SPEC-modern-movement.md §36.2, §36.8): the local prediction of the
 * entity, block and air steps, each sent to the server as an EFR message. Kept apart from OffhandUse because this
 * class names client-only classes, which a dedicated server must never load.
 */
public final class OffhandUseClient {

	private OffhandUseClient() {
	}

	/**
	 * The off-hand try after a main-hand click that did nothing. Returns whether the off-hand used the click on a
	 * block, which is when the vanilla client swings for the main hand. Each step sends its message whether or not
	 * the prediction used the click, as the vanilla client does for the main hand; the server decides. A use that
	 * changed the off-hand stack drops its raise height, as vanilla's does for the main hand (§36.5).
	 */
	public static boolean tryOnClient(Minecraft mc, MovingObjectPosition hit) {
		EntityClientPlayerMP player = mc.thePlayer;
		ItemStack offhand = Offhand.getOffhand(player);
		if (offhand == null || hit == null) {
			return false;
		}
		World world = mc.theWorld;
		if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
			Entity entity = hit.entityHit;
			EtFuturum.networkWrapper.sendToServer(UseOffhandMessage.entity(entity.getEntityId()));
			return OffhandUse.withOffhandInMainHand(player, () -> player.interactWith(entity));
		}
		if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
			int x = hit.blockX, y = hit.blockY, z = hit.blockZ, side = hit.sideHit;
			if (!world.getBlock(x, y, z).isAir(world, x, y, z)) {
				float hx = (float) hit.hitVec.xCoord - x, hy = (float) hit.hitVec.yCoord - y, hz = (float) hit.hitVec.zCoord - z;
				int sizeBefore = offhand.stackSize;
				Boolean used = OffhandUse.withOffhandInMainHand(player, () -> {
					PlayerInteractEvent event = ForgeEventFactory.onPlayerInteract(player, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, x, y, z, side, world);
					if (event.isCanceled()) {
						return null;
					}
					EtFuturum.networkWrapper.sendToServer(UseOffhandMessage.block(x, y, z, side, hx, hy, hz));
					return useOnBlockLocally(mc, player, world, x, y, z, side, hx, hy, hz, event);
				});
				if (used == null) {
					return false;
				}
				if (offhand.stackSize != sizeBefore || mc.playerController.isInCreativeMode()) {
					resetOffhandEquippedProgress(mc);
				}
				if (used) {
					return true;
				}
			}
		}
		Boolean used = OffhandUse.withOffhandInMainHand(player, () -> {
			PlayerInteractEvent event = ForgeEventFactory.onPlayerInteract(player, PlayerInteractEvent.Action.RIGHT_CLICK_AIR, 0, 0, 0, -1, world);
			if (event.isCanceled()) {
				return null;
			}
			EtFuturum.networkWrapper.sendToServer(UseOffhandMessage.air());
			return useInAirLocally(mc, player, world);
		});
		if (used != null && used) {
			resetOffhandEquippedProgress(mc);
		}
		return false;
	}

	private static void resetOffhandEquippedProgress(Minecraft mc) {
		if (mc.entityRenderer.itemRenderer instanceof IOffhandItemRenderer) {
			((IOffhandItemRenderer) mc.entityRenderer.itemRenderer).etfu$resetOffhandEquippedProgress();
		}
	}

	/** The client's own prediction of a block use: the block with the item, then the item on the block (§36.2). */
	private static boolean useOnBlockLocally(Minecraft mc, EntityPlayer player, World world, int x, int y, int z, int side, float hx, float hy, float hz, PlayerInteractEvent event) {
		ItemStack stack = player.inventory.getCurrentItem();
		Block block = world.getBlock(x, y, z);
		boolean useBlock = !player.isSneaking() || stack == null || stack.getItem().doesSneakBypassUse(world, x, y, z, player);
		if (useBlock && event.useBlock != Event.Result.DENY && block.onBlockActivated(world, x, y, z, player, side, hx, hy, hz)) {
			return true;
		}
		if (stack == null || event.useItem == Event.Result.DENY) {
			return false;
		}
		int damage = stack.getItemDamage();
		int size = stack.stackSize;
		boolean placed = stack.tryPlaceItemIntoWorld(player, world, x, y, z, side, hx, hy, hz);
		if (mc.playerController.isInCreativeMode()) {
			stack.setItemDamage(damage);
			stack.stackSize = size;
		} else if (stack.stackSize <= 0) {
			player.inventory.mainInventory[player.inventory.currentItem] = null;
		}
		return placed;
	}

	/** The client's own prediction of an item used in the air, 1.7.10's sendUseItem without its packet. */
	private static boolean useInAirLocally(Minecraft mc, EntityPlayer player, World world) {
		ItemStack stack = player.inventory.getCurrentItem();
		if (stack == null) {
			return false;
		}
		int size = stack.stackSize;
		ItemStack result = stack.useItemRightClick(world, player);
		if (result == stack && (result == null || result.stackSize == size)) {
			return false;
		}
		player.inventory.mainInventory[player.inventory.currentItem] = result;
		if (result != null && result.stackSize <= 0) {
			player.inventory.mainInventory[player.inventory.currentItem] = null;
		}
		return true;
	}
}
