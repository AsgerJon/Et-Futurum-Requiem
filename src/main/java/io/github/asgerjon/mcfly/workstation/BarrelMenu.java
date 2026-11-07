package io.github.asgerjon.mcfly.workstation;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerChest;

/**
 * The barrel's menu on the server (SPEC-modern-movement.md 92.4): 1.7.10's three-row `ContainerChest` (26.3's
 * `ChestMenu.threeRows`), counting its player into the barrel's openers at its making and out at its closing (26.3's
 * `startOpen` and `stopOpen`, which have the player; 1.7.10's `openInventory` has none). The client's is 1.7.10's own
 * chest screen, the same 27 slots.
 */
public class BarrelMenu extends ContainerChest {

	private final BarrelTile barrel;

	public BarrelMenu(InventoryPlayer playerInventory, BarrelTile barrel) {
		super(playerInventory, barrel);
		this.barrel = barrel;
		barrel.startOpen(playerInventory.player);
	}

	@Override
	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);
		barrel.stopOpen(player);
	}
}
