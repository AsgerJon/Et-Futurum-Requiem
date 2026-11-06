package io.github.asgerjon.mcfly.model;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import java.util.List;

/**
 * A block drawn from 26.3's models (SPEC-modern-movement.md §84) by McFly's model renderer (its render id
 * `McFlyRenderIds.MODEL`): what its blockstate picks at a place, what its item shows, and its icons by texture name.
 */
public interface ModelledBlock {

	/** The models at the place, as its 26.3 blockstate picks them. */
	List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z);

	/** The models its item shows (26.3's item model). */
	List<ModelPart> itemParts(int meta);

	/** The icon for a texture name ("block/chorus_plant") or an open reference ("#wall"), registered with the block's own. */
	@SideOnly(Side.CLIENT)
	IIcon modelIcon(String texture);

	/** Its models' x and z offset at the place (26.3's `OffsetType.XZ`, as pointed dripstone's), or null for none. */
	default double[] modelOffset(IBlockAccess world, int x, int y, int z) {
		return null;
	}

	/** Whether it's drawn full bright at the place (26.3's `emissiveRendering`: an active sculk sensor). */
	default boolean emissive(IBlockAccess world, int x, int y, int z) {
		return false;
	}

	/** The icon for a texture name at a meta (a block of variants: spec §88.2); the meta's own by default. */
	@SideOnly(Side.CLIENT)
	default IIcon modelIcon(String texture, int meta) {
		return modelIcon(texture);
	}
}
