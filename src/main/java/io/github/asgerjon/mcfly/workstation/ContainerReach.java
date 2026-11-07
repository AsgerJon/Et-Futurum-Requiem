package io.github.asgerjon.mcfly.workstation;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

/**
 * A block container's reach (SPEC-modern-movement.md 92.4): 26.3's player block interaction range (4.5, 5 in creative:
 * `Player.blockInteractionRange`, `ServerPlayer`'s creative modifier) and its `Container.stillValidBlockEntity` (the eye
 * within that plus 4 of the block's box).
 */
final class ContainerReach {

	private ContainerReach() {
	}

	/** 26.3's block interaction range. */
	static double range(EntityPlayer player) {
		return player.capabilities.isCreativeMode ? 5.0D : 4.5D;
	}

	/** 26.3's `stillValidBlockEntity`: the tile still there, the player's eye within the range plus 4 of its box. */
	static boolean stillValid(TileEntity tile, EntityPlayer player) {
		if (tile.getWorldObj() == null || tile.getWorldObj().getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile) {
			return false;
		}
		// the client's own player stands at its eyes (1.7.10's yOffset 1.62)
		double eyeY = player.posY + (tile.getWorldObj().isRemote ? player.getEyeHeight() - player.getDefaultEyeHeight() : player.getEyeHeight());
		double dx = Math.max(Math.max(tile.xCoord - player.posX, player.posX - (tile.xCoord + 1)), 0.0D);
		double dy = Math.max(Math.max(tile.yCoord - eyeY, eyeY - (tile.yCoord + 1)), 0.0D);
		double dz = Math.max(Math.max(tile.zCoord - player.posZ, player.posZ - (tile.zCoord + 1)), 0.0D);
		double reach = range(player) + 4.0D;
		return dx * dx + dy * dy + dz * dz < reach * reach;
	}
}
