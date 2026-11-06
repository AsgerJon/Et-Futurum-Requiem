package io.github.asgerjon.mcfly.boat.client;

import io.github.asgerjon.mcfly.boat.McFlyBoat;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Boats and rafts drawn (SPEC-modern-movement.md 91.12; 26.3's `AbstractBoatRenderer`, `BoatRenderer`,
 * `RaftRenderer`): raised 0.375, turned by its yaw, the hurt wobble (`sin(hurt) * hurt * damage / 10` by the hurt
 * direction), the bubble column's rocking about x and z, 26.3's models with their paddles by the rowing time, a boat's
 * water patch (depth only, drawn before the water, so the water inside it doesn't show); a shadow of 0.8.
 */
public class BoatRenderer extends Render {

	private final BoatModels.Model plainBoat = new BoatModels.Model(false, false);
	private final BoatModels.Model chestBoat = new BoatModels.Model(false, true);
	private final BoatModels.Model raft = new BoatModels.Model(true, false);
	private final BoatModels.Model chestRaft = new BoatModels.Model(true, true);

	public BoatRenderer() {
		shadowSize = 0.8F;
	}

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
		if (!(entity instanceof McFlyBoat boat)) {
			return;
		}
		BoatModels.Model model = boat.isRaft() ? boat.isChest() ? chestRaft : raft : boat.isChest() ? chestBoat : plainBoat;
		GL11.glPushMatrix();
		GL11.glTranslated(x, y + 0.375D, z);
		GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
		float hurt = boat.getHurtTime() - partialTicks;
		float damage = Math.max(boat.getDamage() - partialTicks, 0.0F);
		if (hurt > 0.0F) {
			GL11.glRotatef(MathHelper.sin(hurt) * hurt * damage / 10.0F * boat.getHurtDir(), 1.0F, 0.0F, 0.0F);
		}
		float bubbleAngle = boat.getBubbleAngle(partialTicks);
		if (!boat.isUnderWater() && Math.abs(bubbleAngle) > 1.0E-5F) {
			GL11.glRotatef(bubbleAngle, 1.0F, 0.0F, 1.0F);
		}
		GL11.glScalef(-1.0F, -1.0F, 1.0F);
		GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
		bindEntityTexture(boat);
		model.render(boat.getRowingTime(0, partialTicks), boat.getRowingTime(1, partialTicks), 0.0625F);
		if (model.hasWaterPatch() && !boat.isUnderWater()) {
			GL11.glColorMask(false, false, false, false);
			model.renderWaterPatch(0.0625F);
			GL11.glColorMask(true, true, true, true);
		}
		GL11.glPopMatrix();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		McFlyBoat boat = (McFlyBoat) entity;
		return boat.type().texture(boat.isChest());
	}

	/** The seat draws nothing. */
	public static class Seat extends Render {

		@Override
		public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
		}

		@Override
		protected ResourceLocation getEntityTexture(Entity entity) {
			return null;
		}
	}
}
