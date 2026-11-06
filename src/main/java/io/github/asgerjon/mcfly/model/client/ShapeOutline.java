package io.github.asgerjon.mcfly.model.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyMultiface;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * The outline of a shape made of boxes, as 26.3 draws a block's selection (SPEC-modern-movement.md §90.3): the edges of
 * their union only, not each box's. The space is cut by every box's bounds into cells; an edge between four cells is
 * drawn where the filled ones don't make a flat face: one or three of them, or two across a diagonal (26.3's
 * `VoxelShape` edges). Used for McFly's multiface blocks (glow lichen's faces), whose 1.7.10 selection box is one box.
 */
@SideOnly(Side.CLIENT)
public final class ShapeOutline {

	public static final ShapeOutline INSTANCE = new ShapeOutline();

	private ShapeOutline() {
	}

	@SubscribeEvent
	public void onHighlight(DrawBlockHighlightEvent event) {
		MovingObjectPosition target = event.target;
		if (target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
			return;
		}
		World world = event.player.worldObj;
		if (!(world.getBlock(target.blockX, target.blockY, target.blockZ) instanceof McFlyMultiface)) {
			return;
		}
		List<AxisAlignedBB> boxes = McFlyMultiface.faceBoxes(McFlyMultiface.faces(world, target.blockX, target.blockY, target.blockZ));
		if (!boxes.isEmpty()) {
			draw(boxes, target.blockX, target.blockY, target.blockZ, event.player, event.partialTicks);
		}
		event.setCanceled(true);
	}

	/** The edges of the boxes' union (in the block's own space), as pairs of points: x1 y1 z1 x2 y2 z2 each. */
	public static List<double[]> edges(List<AxisAlignedBB> boxes) {
		double[] xs = bounds(boxes, 0), ys = bounds(boxes, 1), zs = bounds(boxes, 2);
		int nx = xs.length - 1, ny = ys.length - 1, nz = zs.length - 1;
		boolean[][][] filled = new boolean[nx][ny][nz];
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++) {
				for (int k = 0; k < nz; k++) {
					double cx = (xs[i] + xs[i + 1]) / 2, cy = (ys[j] + ys[j + 1]) / 2, cz = (zs[k] + zs[k + 1]) / 2;
					for (AxisAlignedBB box : boxes) {
						if (cx > box.minX && cx < box.maxX && cy > box.minY && cy < box.maxY && cz > box.minZ && cz < box.maxZ) {
							filled[i][j][k] = true;
							break;
						}
					}
				}
			}
		}
		List<double[]> edges = new ArrayList<>();
		// along x: the four cells round each line at (ys[j], zs[k]) over cell i
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j <= ny; j++) {
				for (int k = 0; k <= nz; k++) {
					if (drawn(at(filled, i, j - 1, k - 1), at(filled, i, j, k - 1), at(filled, i, j - 1, k), at(filled, i, j, k))) {
						edges.add(new double[]{xs[i], ys[j], zs[k], xs[i + 1], ys[j], zs[k]});
					}
				}
			}
		}
		for (int j = 0; j < ny; j++) {
			for (int i = 0; i <= nx; i++) {
				for (int k = 0; k <= nz; k++) {
					if (drawn(at(filled, i - 1, j, k - 1), at(filled, i, j, k - 1), at(filled, i - 1, j, k), at(filled, i, j, k))) {
						edges.add(new double[]{xs[i], ys[j], zs[k], xs[i], ys[j + 1], zs[k]});
					}
				}
			}
		}
		for (int k = 0; k < nz; k++) {
			for (int i = 0; i <= nx; i++) {
				for (int j = 0; j <= ny; j++) {
					if (drawn(at(filled, i - 1, j - 1, k), at(filled, i, j - 1, k), at(filled, i - 1, j, k), at(filled, i, j, k))) {
						edges.add(new double[]{xs[i], ys[j], zs[k], xs[i], ys[j], zs[k + 1]});
					}
				}
			}
		}
		return edges;
	}

	/** Four cells round a line, a b on one side, c d the other (a and d across, b and c across). */
	private static boolean drawn(boolean a, boolean b, boolean c, boolean d) {
		int count = (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0) + (d ? 1 : 0);
		return count == 1 || count == 3 || count == 2 && (a == d);
	}

	private static boolean at(boolean[][][] filled, int i, int j, int k) {
		return i >= 0 && j >= 0 && k >= 0 && i < filled.length && j < filled[0].length && k < filled[0][0].length && filled[i][j][k];
	}

	private static double[] bounds(List<AxisAlignedBB> boxes, int axis) {
		TreeSet<Double> values = new TreeSet<>();
		for (AxisAlignedBB box : boxes) {
			values.add(axis == 0 ? box.minX : axis == 1 ? box.minY : box.minZ);
			values.add(axis == 0 ? box.maxX : axis == 1 ? box.maxY : box.maxZ);
		}
		double[] out = new double[values.size()];
		int i = 0;
		for (double value : values) {
			out[i++] = value;
		}
		return out;
	}

	/** Drawn as 1.7.10's selection box is (black, 0.4, 2 wide), pushed a little out from the block's middle. */
	private static void draw(List<AxisAlignedBB> boxes, int x, int y, int z, EntityPlayer player, float partialTicks) {
		double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
		double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
		GL11.glEnable(GL11.GL_BLEND);
		OpenGlHelper.glBlendFunc(770, 771, 1, 0);
		GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
		GL11.glLineWidth(2.0F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDepthMask(false);
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawing(GL11.GL_LINES);
		for (double[] edge : edges(boxes)) {
			for (int end = 0; end < 2; end++) {
				tessellator.addVertex(x + out(edge[end * 3]) - px, y + out(edge[end * 3 + 1]) - py, z + out(edge[end * 3 + 2]) - pz);
			}
		}
		tessellator.draw();
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
	}

	private static double out(double coordinate) {
		return 0.5 + (coordinate - 0.5) * 1.004;
	}
}
