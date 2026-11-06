package io.github.asgerjon.mcfly.boat.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.MathHelper;

/**
 * 26.3's `BoatModel` and `RaftModel` (SPEC-modern-movement.md 91.12), box by box, on 1.7.10's model parts (whose box
 * texture layout and face order are 26.3's `ModelPart.Cube`'s): the hull or the raft's two layers, the paddles (26.3's
 * `AbstractBoatModel.animatePaddle`), the chest of a chest boat or raft (its texture 128 by 128), and the water patch a
 * boat hides the water inside it with.
 */
public final class BoatModels {

	private static final float PI = (float) Math.PI;

	private BoatModels() {
	}

	/** One of the four: a boat or a raft, with a chest or not. */
	public static final class Model extends ModelBase {

		private final ModelRenderer[] body;
		private final ModelRenderer leftPaddle;
		private final ModelRenderer rightPaddle;
		private final ModelRenderer waterPatch;

		public Model(boolean raft, boolean chest) {
			textureWidth = 128;
			textureHeight = chest ? 128 : 64;
			ModelRenderer[] hull;
			if (raft) {
				ModelRenderer bottom = part(0, 0, 0.0F, -2.1F, 1.0F, 1.5708F, 0.0F, 0.0F);
				bottom.addBox(-14.0F, -11.0F, -4.0F, 28, 20, 4);
				bottom.addBox(-14.0F, -9.0F, -8.0F, 28, 16, 4);
				hull = new ModelRenderer[]{bottom};
				leftPaddle = paddle(0, 24, -4.0F, 9.0F, false);
				rightPaddle = paddle(40, 24, -4.0F, -9.0F, true);
				waterPatch = null;
			} else {
				ModelRenderer bottom = part(0, 0, 0.0F, 3.0F, 1.0F, PI / 2, 0.0F, 0.0F);
				bottom.addBox(-14.0F, -9.0F, -3.0F, 28, 16, 3);
				ModelRenderer back = part(0, 19, -15.0F, 4.0F, 4.0F, 0.0F, PI * 3.0F / 2.0F, 0.0F);
				back.addBox(-13.0F, -7.0F, -1.0F, 18, 6, 2);
				ModelRenderer front = part(0, 27, 15.0F, 4.0F, 0.0F, 0.0F, PI / 2, 0.0F);
				front.addBox(-8.0F, -7.0F, -1.0F, 16, 6, 2);
				ModelRenderer right = part(0, 35, 0.0F, 4.0F, -9.0F, 0.0F, PI, 0.0F);
				right.addBox(-14.0F, -7.0F, -1.0F, 28, 6, 2);
				ModelRenderer left = part(0, 43, 0.0F, 4.0F, 9.0F, 0.0F, 0.0F, 0.0F);
				left.addBox(-14.0F, -7.0F, -1.0F, 28, 6, 2);
				hull = new ModelRenderer[]{bottom, back, front, right, left};
				leftPaddle = paddle(62, 0, -5.0F, 9.0F, false);
				rightPaddle = paddle(62, 20, -5.0F, -9.0F, true);
				waterPatch = part(0, 0, 0.0F, -3.0F, 1.0F, PI / 2, 0.0F, 0.0F);
				waterPatch.addBox(-14.0F, -9.0F, -3.0F, 28, 16, 3);
			}
			if (chest) {
				// a raft's chest sits 5.1 higher
				float up = raft ? -5.1F : 0.0F;
				ModelRenderer chestBottom = part(0, 76, -2.0F, -5.0F + up, -6.0F, 0.0F, -PI / 2, 0.0F);
				chestBottom.addBox(0.0F, 0.0F, 0.0F, 12, 8, 12);
				ModelRenderer chestLid = part(0, 59, -2.0F, -9.0F + up, -6.0F, 0.0F, -PI / 2, 0.0F);
				chestLid.addBox(0.0F, 0.0F, 0.0F, 12, 4, 12);
				ModelRenderer chestLock = part(0, 59, -1.0F, -6.0F + up, -1.0F, 0.0F, -PI / 2, 0.0F);
				chestLock.addBox(0.0F, 0.0F, 0.0F, 2, 4, 1);
				ModelRenderer[] all = new ModelRenderer[hull.length + 3];
				System.arraycopy(hull, 0, all, 0, hull.length);
				all[hull.length] = chestBottom;
				all[hull.length + 1] = chestLid;
				all[hull.length + 2] = chestLock;
				hull = all;
			}
			body = hull;
		}

		private ModelRenderer part(int u, int v, float x, float y, float z, float xRot, float yRot, float zRot) {
			ModelRenderer part = new ModelRenderer(this, u, v);
			part.setTextureSize(textureWidth, textureHeight);
			part.setRotationPoint(x, y, z);
			part.rotateAngleX = xRot;
			part.rotateAngleY = yRot;
			part.rotateAngleZ = zRot;
			return part;
		}

		/** 26.3's paddle: a shaft 18 long and a blade 7 by 6, pivoted at 3 and the height given, a sixteenth of a turn about z. */
		private ModelRenderer paddle(int u, int v, float y, float z, boolean right) {
			ModelRenderer paddle = part(u, v, 3.0F, y, z, 0.0F, right ? PI : 0.0F, PI / 16);
			paddle.addBox(-1.0F, 0.0F, -5.0F, 2, 2, 18);
			paddle.addBox(right ? 0.001F : -1.001F, -3.0F, 8.0F, 1, 6, 7);
			return paddle;
		}

		/** The model, its paddles at the rowing times given. */
		public void render(float rowingLeft, float rowingRight, float scale) {
			animatePaddle(rowingLeft, 0, leftPaddle);
			animatePaddle(rowingRight, 1, rightPaddle);
			for (ModelRenderer part : body) {
				part.render(scale);
			}
			leftPaddle.render(scale);
			rightPaddle.render(scale);
		}

		public boolean hasWaterPatch() {
			return waterPatch != null;
		}

		public void renderWaterPatch(float scale) {
			if (waterPatch != null) {
				waterPatch.render(scale);
			}
		}

		private static void animatePaddle(float time, int side, ModelRenderer paddle) {
			paddle.rotateAngleX = lerp((MathHelper.sin(-time) + 1.0F) / 2.0F, -PI / 3, -PI / 12);
			paddle.rotateAngleY = lerp((MathHelper.sin(-time + 1.0F) + 1.0F) / 2.0F, -PI / 4, PI / 4);
			if (side == 1) {
				paddle.rotateAngleY = PI - paddle.rotateAngleY;
			}
		}

		private static float lerp(float factor, float min, float max) {
			return factor < 0.0F ? min : factor > 1.0F ? max : min + factor * (max - min);
		}
	}
}
