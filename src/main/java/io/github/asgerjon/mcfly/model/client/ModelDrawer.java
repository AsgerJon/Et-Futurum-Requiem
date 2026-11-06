package io.github.asgerjon.mcfly.model.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.model.BlockModel;
import io.github.asgerjon.mcfly.model.BlockModel.Element;
import io.github.asgerjon.mcfly.model.BlockModel.Face;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import java.util.Map;

/**
 * Draws a 26.3 block model (SPEC-modern-movement.md §84) with 1.7.10's tessellator, baked as 26.3's `FaceBakery` bakes
 * it: each face's four corners in `FaceInfo`'s order, corner 0 at (minU, minV), 1 (minU, maxV), 2 (maxU, maxV), 3
 * (maxU, minV); a face without UVs takes 26.3's `defaultFaceUV` from its box. A blockstate's turn: x first, then y, each
 * a quarter turn the way 26.3 turns (the model's up to north for x 90, north to east for y 90). With uvlock a face
 * without UVs takes them from its turned box, so its texture stays put in the world, as 26.3's uvlock keeps it (faces
 * with their own UVs aren't uvlocked yet: none drawn so far has them). Light as 26.3's flat light: a face on the block's
 * side takes the neighbour's, any other the block's own; shaded 0.5 down, 1 up, 0.8 north and south, 0.6 east and west.
 */
@SideOnly(Side.CLIENT)
public final class ModelDrawer {

	/** The icon for a texture name ("block/chorus_plant") or a reference a template leaves open ("#wall"). */
	public interface Icons {
		IIcon icon(String texture);
	}

	/** Block light 15 and sky light 15, as 1.7.10 packs them. */
	private static final int FULL_BRIGHT = 15 << 20 | 15 << 4;

	private ModelDrawer() {
	}

	/** The model at (x, y, z) in the world, turned; faces culled against their neighbours as 1.7.10 culls a block's. */
	public static boolean drawInWorld(IBlockAccess world, int x, int y, int z, Block block, BlockModel model, int turnX, int turnY, boolean uvlock, Icons icons) {
		return drawInWorld(world, x, y, z, block, model, turnX, turnY, uvlock, icons, false);
	}

	/** The same, full bright when `emissive` (26.3's `emissiveRendering`: block and sky light 15 on every face). */
	public static boolean drawInWorld(IBlockAccess world, int x, int y, int z, Block block, BlockModel model, int turnX, int turnY, boolean uvlock, Icons icons,
		boolean emissive) {
		Tessellator tessellator = Tessellator.instance;
		int own = emissive ? FULL_BRIGHT : block.getMixedBrightnessForBlock(world, x, y, z);
		boolean drew = false;
		for (Element element : model.elements()) {
			for (Map.Entry<BlockModel.Side, Face> entry : element.faces.entrySet()) {
				Face face = entry.getValue();
				Quad quad = bake(element, entry.getKey(), face, turnX, turnY, uvlock);
				if (face.cull != null) {
					BlockModel.Side cull = turn(face.cull, turnX, turnY);
					if (!block.shouldSideBeRendered(world, x + cull.dx, y + cull.dy, z + cull.dz, cull.side)) {
						continue;
					}
				}
				IIcon icon = icons.icon(textureOf(model, face));
				if (icon == null) {
					continue;
				}
				tessellator.setBrightness(!emissive && quad.onBlockSide() ? block.getMixedBrightnessForBlock(world, x + quad.side.dx, y + quad.side.dy, z + quad.side.dz) : own);
				float shade = shade(quad.shadeSide != null ? quad.shadeSide : quad.side);
				int colour = block.colorMultiplier(world, x, y, z);
				tessellator.setColorOpaque_F(shade * (colour >> 16 & 255) / 255.0F, shade * (colour >> 8 & 255) / 255.0F, shade * (colour & 255) / 255.0F);
				quad.draw(tessellator, icon, x, y, z);
				drew = true;
			}
		}
		return drew;
	}

	/**
	 * The model for an item, between 0 and 1 (the caller moves it as 1.7.10's inventory blocks are moved): each face
	 * with its normal, for the item light, and its own draw.
	 */
	public static void drawInventory(BlockModel model, int turnX, int turnY, Icons icons) {
		Tessellator tessellator = Tessellator.instance;
		for (Element element : model.elements()) {
			for (Map.Entry<BlockModel.Side, Face> entry : element.faces.entrySet()) {
				Face face = entry.getValue();
				IIcon icon = icons.icon(textureOf(model, face));
				if (icon == null) {
					continue;
				}
				Quad quad = bake(element, entry.getKey(), face, turnX, turnY, false);
				tessellator.startDrawingQuads();
				tessellator.setNormal(quad.side.dx, quad.side.dy, quad.side.dz);
				quad.draw(tessellator, icon, 0, 0, 0);
				tessellator.draw();
			}
		}
	}

	private static String textureOf(BlockModel model, Face face) {
		String name = model.texture(face.texture);
		return name != null ? name : face.texture;
	}

	private static float shade(BlockModel.Side side) {
		switch (side) {
			case DOWN:
				return 0.5F;
			case UP:
				return 1.0F;
			case NORTH:
			case SOUTH:
				return 0.8F;
			default:
				return 0.6F;
		}
	}

	/** One face baked: its four corners (0 to 16) with their UVs (0 to 16), and the side it faces now. */
	private static final class Quad {
		final float[][] corners = new float[4][];
		final float[][] uvs = new float[4][];
		BlockModel.Side side;
		/** The side its shade is taken from, when the element overrides it (26.3's `shade_direction_override`). */
		BlockModel.Side shadeSide;

		/** Whether it lies on the block's side it faces (26.3 lights those from the neighbour). */
		boolean onBlockSide() {
			float edge = side.dx + side.dy + side.dz > 0 ? 16.0F : 0.0F;
			int axis = side.dx != 0 ? 0 : side.dy != 0 ? 1 : 2;
			for (float[] corner : corners) {
				if (corner[axis] != edge) {
					return false;
				}
			}
			return true;
		}

		void draw(Tessellator tessellator, IIcon icon, int x, int y, int z) {
			for (int i = 0; i < 4; i++) {
				tessellator.addVertexWithUV(x + corners[i][0] / 16.0, y + corners[i][1] / 16.0, z + corners[i][2] / 16.0,
					icon.getInterpolatedU(uvs[i][0]), icon.getInterpolatedV(uvs[i][1]));
			}
		}
	}

	private static Quad bake(Element element, BlockModel.Side side, Face face, int turnX, int turnY, boolean uvlock) {
		Quad quad = new Quad();
		if (uvlock && face.uv == null && (turnX != 0 || turnY != 0)) {
			// the box turned first, then baked as an unturned one: its default UVs come from where it is now
			float[] a = turnPoint(element.from, turnX, turnY);
			float[] b = turnPoint(element.to, turnX, turnY);
			float[] from = {Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2])};
			float[] to = {Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])};
			BlockModel.Side now = turn(side, turnX, turnY);
			fill(quad, from, to, now, defaultUv(from, to, now), face.rotation);
			quad.side = now;
			return quad;
		}
		fill(quad, element.from, element.to, side, face.uv != null ? face.uv : defaultUv(element.from, element.to, side), face.rotation);
		for (int i = 0; i < 4; i++) {
			quad.corners[i] = turnPoint(rotate(quad.corners[i], element), turnX, turnY);
		}
		quad.side = turn(side, turnX, turnY);
		quad.shadeSide = element.shadeSide == null ? null : turn(element.shadeSide, turnX, turnY);
		return quad;
	}

	/**
	 * 26.3's element rotation of a corner (`FaceBakery.applyElementRotation`): about the origin, on the axis, by the
	 * angle (right-handed, as JOML's), the other two axes stretched by 1 / cos(angle) with `rescale`.
	 */
	private static float[] rotate(float[] corner, Element element) {
		if (element.rotationAxis < 0 || element.rotationAngle == 0.0F) {
			return corner;
		}
		double radians = Math.toRadians(element.rotationAngle);
		double cos = Math.cos(radians), sin = Math.sin(radians);
		float[] o = element.rotationOrigin;
		double x = corner[0] - o[0], y = corner[1] - o[1], z = corner[2] - o[2];
		double nx = x, ny = y, nz = z;
		switch (element.rotationAxis) {
			case 0:
				ny = y * cos - z * sin;
				nz = y * sin + z * cos;
				break;
			case 1:
				nx = x * cos + z * sin;
				nz = -x * sin + z * cos;
				break;
			default:
				nx = x * cos - y * sin;
				ny = x * sin + y * cos;
				break;
		}
		if (element.rescale) {
			double scale = 1.0D / Math.cos(radians);
			if (element.rotationAxis != 0) {
				nx *= scale;
			}
			if (element.rotationAxis != 1) {
				ny *= scale;
			}
			if (element.rotationAxis != 2) {
				nz *= scale;
			}
		}
		return new float[]{(float) (nx + o[0]), (float) (ny + o[1]), (float) (nz + o[2])};
	}

	/**
	 * The corners in 26.3's `FaceInfo` order, and their UVs as 26.3's `CuboidFace.UVs` gives them out: corner i takes
	 * corner (i + rotation / 90) % 4's (26.3's `Quadrant.rotateVertexIndex`).
	 */
	private static void fill(Quad quad, float[] from, float[] to, BlockModel.Side side, float[] uv, int rotation) {
		float x0 = from[0], y0 = from[1], z0 = from[2], x1 = to[0], y1 = to[1], z1 = to[2];
		float[][] corners;
		switch (side) {
			case DOWN:
				corners = new float[][]{{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
				break;
			case UP:
				corners = new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
				break;
			case NORTH:
				corners = new float[][]{{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}};
				break;
			case SOUTH:
				corners = new float[][]{{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}};
				break;
			case WEST:
				corners = new float[][]{{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}};
				break;
			default:
				corners = new float[][]{{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}};
				break;
		}
		for (int i = 0; i < 4; i++) {
			quad.corners[i] = corners[i];
			int c = (i + rotation / 90) % 4;
			quad.uvs[i] = new float[]{c == 0 || c == 1 ? uv[0] : uv[2], c == 0 || c == 3 ? uv[1] : uv[3]};
		}
	}

	/** 26.3's `FaceBakery.defaultFaceUV`. */
	private static float[] defaultUv(float[] from, float[] to, BlockModel.Side side) {
		switch (side) {
			case DOWN:
				return new float[]{from[0], 16.0F - to[2], to[0], 16.0F - from[2]};
			case UP:
				return new float[]{from[0], from[2], to[0], to[2]};
			case NORTH:
				return new float[]{16.0F - to[0], 16.0F - to[1], 16.0F - from[0], 16.0F - from[1]};
			case SOUTH:
				return new float[]{from[0], 16.0F - to[1], to[0], 16.0F - from[1]};
			case WEST:
				return new float[]{from[2], 16.0F - to[1], to[2], 16.0F - from[1]};
			default:
				return new float[]{16.0F - to[2], 16.0F - to[1], 16.0F - from[2], 16.0F - from[1]};
		}
	}

	/**
	 * A point (0 to 16) turned about the block's middle: x quarter turns about the x axis (up toward north), then y about
	 * the y axis (north toward east), as 26.3's blockstate rotations.
	 */
	private static float[] turnPoint(float[] point, int turnX, int turnY) {
		float x = point[0] - 8.0F, y = point[1] - 8.0F, z = point[2] - 8.0F;
		for (int i = 0; i < (turnX / 90 & 3); i++) {
			// up (0, 1, 0) to north (0, 0, -1)
			float ny = z;
			float nz = -y;
			y = ny;
			z = nz;
		}
		for (int i = 0; i < (turnY / 90 & 3); i++) {
			// north (0, 0, -1) to east (1, 0, 0)
			float nx = -z;
			float nz = x;
			x = nx;
			z = nz;
		}
		return new float[]{x + 8.0F, y + 8.0F, z + 8.0F};
	}

	/** A direction turned as the points are. */
	public static BlockModel.Side turn(BlockModel.Side side, int turnX, int turnY) {
		float[] step = turnPoint(new float[]{8.0F + side.dx, 8.0F + side.dy, 8.0F + side.dz}, turnX, turnY);
		return BlockModel.Side.byStep(Math.round(step[0] - 8.0F), Math.round(step[1] - 8.0F), Math.round(step[2] - 8.0F));
	}
}
