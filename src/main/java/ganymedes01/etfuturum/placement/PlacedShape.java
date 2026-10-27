package ganymedes01.etfuturum.placement;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

/**
 * The collision box a block will have once placed, for the placement check: modern tests the placed state's shape
 * (1.13+), while 1.7.10 asked the block for its box before it existed, which for a block whose box depends on its
 * metadata is whatever it last computed, often a full cube. Known here: ladders (orientation from the side) and
 * trapdoors (orientation from the side, half from the click height, and open when the spot is powered, as modern
 * places them). Any other block takes the vanilla check.
 */
public final class PlacedShape {

	private PlacedShape() {
	}

	/** The box of the block as it would be placed, or null for a block this doesn't know. */
	public static AxisAlignedBB boxAsPlaced(Block block, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
		if (block instanceof BlockLadder ladder) {
			int meta = ladder.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, metadata);
			if (meta < 2 || meta > 5) {
				return null;
			}
			ladder.func_149797_b(meta);
		} else if (block instanceof BlockTrapDoor trapdoor) {
			trapdoor.func_150117_b(placedTrapdoorMeta(trapdoor, world, x, y, z, side, hitX, hitY, hitZ, metadata));
		} else {
			return null;
		}
		return AxisAlignedBB.getBoundingBox(x + block.getBlockBoundsMinX(), y + block.getBlockBoundsMinY(), z + block.getBlockBoundsMinZ(),
				x + block.getBlockBoundsMaxX(), y + block.getBlockBoundsMaxY(), z + block.getBlockBoundsMaxZ());
	}

	/** A trapdoor's placed metadata: vanilla's orientation and half, open when the spot is powered (modern's placement state). */
	public static int placedTrapdoorMeta(BlockTrapDoor trapdoor, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
		int meta = trapdoor.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, metadata);
		return world.isBlockIndirectlyGettingPowered(x, y, z) ? meta | 4 : meta;
	}

	/** The entity the server's check leaves out: never the placing player once the modern block use packets count it. */
	public static Entity excluded(Entity placer) {
		return ConfigMixins.modernBlockUsePackets && placer instanceof EntityPlayer ? null : placer;
	}
}
