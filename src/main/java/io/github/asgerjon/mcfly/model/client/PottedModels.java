package io.github.asgerjon.mcfly.model.client;

import io.github.asgerjon.mcfly.base.McFlyPots;
import io.github.asgerjon.mcfly.model.BlockModel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.world.IBlockAccess;

/**
 * A flower pot holding one of McFly's plants drawn as 26.3's potted model, the pot with it (SPEC-modern-movement.md
 * §90, step D2): from 1.7.10's `RenderBlocks.renderBlockFlowerpot` (a mixin), in its place.
 */
public final class PottedModels {

	private PottedModels() {
	}

	/** Drawn when the pot at (x, y, z) holds a McFly plant with a potted model; false leaves the pot to 1.7.10. */
	public static boolean draw(IBlockAccess world, BlockFlowerPot pot, int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		if (!(tile instanceof TileEntityFlowerPot potTile)) {
			return false;
		}
		Item item = potTile.getFlowerPotItem();
		Block plant = item == null ? null : Block.getBlockFromItem(item);
		String model = plant == null ? null : McFlyPots.model(plant, potTile.getFlowerPotData());
		if (model == null) {
			return false;
		}
		ModelDrawer.drawInWorld(world, x, y, z, pot, BlockModel.get(model), 0, 0, false, texture -> McFlyPots.icon(plant, texture));
		return true;
	}
}
