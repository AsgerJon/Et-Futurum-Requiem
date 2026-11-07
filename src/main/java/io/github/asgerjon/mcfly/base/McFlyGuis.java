package io.github.asgerjon.mcfly.base;

import ganymedes01.etfuturum.EtFuturum;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * McFly's screens (SPEC-modern-movement.md 92.3): ids from 100, each a server container and a client screen. 1.7.10
 * gives a mod one GUI handler and the mod is still EFR's (spec §79), so EFR's handler passes ids from 100 here; once
 * McFly is its own mod the handler is its own.
 */
public final class McFlyGuis {

	public static final int FIRST = 100;
	public static final int SHULKER_BOX = 100;
	public static final int SMITHING_TABLE = 101;
	public static final int SMOKER = 102;
	public static final int BLAST_FURNACE = 103;
	public static final int BARREL = 104;

	private McFlyGuis() {
	}

	/** Opens one of ours for the player (the server's call; the client follows). */
	public static void open(EntityPlayer player, int id, World world, int x, int y, int z) {
		player.openGui(EtFuturum.instance, id, world, x, y, z);
	}

	public static Object server(int id, EntityPlayer player, World world, int x, int y, int z) {
		switch (id) {
			case SHULKER_BOX:
				return new io.github.asgerjon.mcfly.shulkerbox.ShulkerBoxMenu(player.inventory, shulkerBoxAt(world, x, y, z));
			case SMITHING_TABLE:
				return new io.github.asgerjon.mcfly.smithing.SmithingMenu(player.inventory, world, x, y, z);
			case SMOKER:
			case BLAST_FURNACE:
				TileEntity cooker = world.getTileEntity(x, y, z);
				return cooker instanceof io.github.asgerjon.mcfly.workstation.CookerTile tile ? new io.github.asgerjon.mcfly.workstation.CookerMenu(player.inventory, tile) : null;
			case BARREL:
				TileEntity barrel = world.getTileEntity(x, y, z);
				return barrel instanceof io.github.asgerjon.mcfly.workstation.BarrelTile tile ? new io.github.asgerjon.mcfly.workstation.BarrelMenu(player.inventory, tile) : null;
			default:
				return null;
		}
	}

	/** The client's screen (its classes named only here, so a server never loads them). */
	public static Object client(int id, EntityPlayer player, World world, int x, int y, int z) {
		switch (id) {
			case SHULKER_BOX:
				return new io.github.asgerjon.mcfly.shulkerbox.client.ShulkerBoxScreen(player.inventory, shulkerBoxAt(world, x, y, z));
			case SMITHING_TABLE:
				return new io.github.asgerjon.mcfly.smithing.client.SmithingScreen(new io.github.asgerjon.mcfly.smithing.SmithingMenu(player.inventory, world, x, y, z));
			case SMOKER:
			case BLAST_FURNACE:
				TileEntity cooker = world.getTileEntity(x, y, z);
				return cooker instanceof io.github.asgerjon.mcfly.workstation.CookerTile tile
					? new io.github.asgerjon.mcfly.workstation.client.CookerScreen(new io.github.asgerjon.mcfly.workstation.CookerMenu(player.inventory, tile)) : null;
			case BARREL:
				// 1.7.10's chest screen over the client's barrel, or 27 empty slots before it has one (the menu brings the contents)
				TileEntity barrel = world.getTileEntity(x, y, z);
				return new net.minecraft.client.gui.inventory.GuiChest(player.inventory,
					barrel instanceof IInventory inventory ? inventory : new InventoryBasic("container.barrel", false, io.github.asgerjon.mcfly.workstation.BarrelTile.SIZE));
			default:
				return null;
		}
	}

	/**
	 * A shulker box's inventory for its menu: its tile entity, or 27 empty slots when this side hasn't got it yet (the
	 * client may not have its tile entity when the menu opens; the server sends the contents with the menu).
	 */
	private static IInventory shulkerBoxAt(World world, int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile instanceof IInventory inventory ? inventory : new InventoryBasic("container.shulkerBox", false, 27);
	}
}
