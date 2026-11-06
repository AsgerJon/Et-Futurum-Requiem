package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.model.BlockModel;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

import java.util.HashMap;
import java.util.Map;

/**
 * McFly's plants in 1.7.10's flower pot (SPEC-modern-movement.md §90, step D2). 1.7.10's pot takes a fixed list
 * (`BlockFlowerPot.func_149928_a`); a mixin there takes a {@link McFlyPottable} too, by the item's block and damage.
 * Drawn (`model/client/PottedModels`) as 26.3's potted model, the pot in it: its `block/flower_pot` and `block/dirt`
 * are 1.7.10's own icons, the plant's textures registered with the plant's own icons.
 */
public final class McFlyPots {

	@SideOnly(Side.CLIENT)
	private static Map<Block, Map<String, IIcon>> icons;

	private McFlyPots() {
	}

	/** The potted model of the block at this damage, or null when it doesn't go in a pot. */
	public static String model(Block block, int meta) {
		return block instanceof McFlyPottable pottable ? pottable.pottedModel(meta) : null;
	}

	/** In a pottable's `registerBlockIcons`: the textures of its potted models at these damages (the pot's and dirt aside). */
	@SideOnly(Side.CLIENT)
	public static void registerIcons(Block plant, IIconRegister register, int... metas) {
		if (icons == null) {
			icons = new HashMap<>();
		}
		Map<String, IIcon> own = new HashMap<>();
		for (int meta : metas) {
			String model = model(plant, meta);
			if (model == null) {
				continue;
			}
			for (String texture : BlockModel.get(model).textureNames()) {
				String name = texture.substring(texture.lastIndexOf('/') + 1);
				if (!"flower_pot".equals(name) && !"dirt".equals(name)) {
					own.put(name, register.registerIcon(name));
				}
			}
		}
		icons.put(plant, own);
	}

	/** A potted model's texture ("block/potted_azalea_bush_top"): the pot's and dirt 1.7.10's, the rest the plant's. */
	@SideOnly(Side.CLIENT)
	public static IIcon icon(Block plant, String texture) {
		String name = texture.substring(texture.lastIndexOf('/') + 1);
		if ("flower_pot".equals(name)) {
			return Blocks.flower_pot.getIcon(0, 0);
		}
		if ("dirt".equals(name)) {
			return Blocks.dirt.getIcon(1, 0);
		}
		Map<String, IIcon> own = icons == null ? null : icons.get(plant);
		return own == null ? null : own.get(name);
	}
}
