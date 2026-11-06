package io.github.asgerjon.mcfly.caves;

import io.github.asgerjon.mcfly.base.McFlyMultiface;
import io.github.asgerjon.mcfly.nether.NetherLightItem;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Glow lichen's item (SPEC-modern-movement.md §90.3), 26.3's placing: into the space clicked when it's a lichen with a
 * face free (or anything replaceable), else the space before the clicked face; the face is the first of the player's
 * nearest looking directions (26.3's `getNearestLookingDirections`: the one toward the clicked block first unless the
 * space clicked is taken) that is free and held. Shown as its flat sprite.
 */
public class GlowLichenItem extends NetherLightItem {

	private static final int[][] OFFSET = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

	public GlowLichenItem(Block block) {
		super(block);
	}

	/** The six faces ordered by how near each is to the player's look, nearest first (26.3's `Direction.orderedByNearest`). */
	public static Integer[] nearestLooking(EntityPlayer player) {
		float pitch = player.rotationPitch * (float) Math.PI / 180.0F, yaw = player.rotationYaw * (float) Math.PI / 180.0F;
		double lookX = -MathHelper.sin(yaw) * MathHelper.cos(pitch), lookY = -MathHelper.sin(pitch), lookZ = MathHelper.cos(yaw) * MathHelper.cos(pitch);
		Integer[] faces = {0, 1, 2, 3, 4, 5};
		Arrays.sort(faces, Comparator.comparingDouble(face -> -(OFFSET[face][0] * lookX + OFFSET[face][1] * lookY + OFFSET[face][2] * lookZ)));
		return faces;
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		McFlyMultiface lichen = (McFlyMultiface) field_150939_a;
		Block clicked = world.getBlock(x, y, z);
		boolean replaceClicked = clicked == lichen ? McFlyMultiface.hasVacantFace(McFlyMultiface.faces(world, x, y, z))
			: clicked == Blocks.snow_layer && (world.getBlockMetadata(x, y, z) & 7) < 1 || clicked.isReplaceable(world, x, y, z);
		if (!replaceClicked) {
			x += OFFSET[side][0];
			y += OFFSET[side][1];
			z += OFFSET[side][2];
		}
		if (stack.stackSize <= 0 || !player.canPlayerEdit(x, y, z, side, stack) || y < 0 || y >= world.getHeight()) {
			return false;
		}
		Block there = world.getBlock(x, y, z);
		if (there != lichen && !there.isReplaceable(world, x, y, z)) {
			return false;
		}
		Integer[] order = nearestLooking(player);
		if (!replaceClicked) {
			// the face toward the clicked block first
			int toward = side ^ 1;
			int index = Arrays.asList(order).indexOf(toward);
			System.arraycopy(order, 0, order, 1, index);
			order[0] = toward;
		}
		for (int face : order) {
			if (lichen.canPlaceFace(world, x, y, z, face)) {
				if (world.isRemote) {
					return true;
				}
				int old = there == lichen ? McFlyMultiface.faces(world, x, y, z) : 0;
				if (there != lichen && !world.setBlock(x, y, z, lichen, 0, 3)) {
					return false;
				}
				lichen.setFaces(world, x, y, z, old | 1 << face);
				world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, lichen.stepSound.func_150496_b(), (lichen.stepSound.getVolume() + 1.0F) / 2.0F,
					lichen.stepSound.getPitch() * 0.8F);
				stack.stackSize--;
				return true;
			}
		}
		return false;
	}
}
