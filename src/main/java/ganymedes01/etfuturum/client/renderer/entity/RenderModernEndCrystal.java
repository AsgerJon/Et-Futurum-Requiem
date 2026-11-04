package ganymedes01.etfuturum.client.renderer.entity;

import ganymedes01.etfuturum.world.end.modern.EndCrystals;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelEnderCrystal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * The end crystal as 26.3 draws it (SPEC-modern-movement.md §76.6, `EndCrystalRenderer`): 1.7.10's model with its base
 * or without (`ShowBottom`), and its beam: from 2 above the middle of its target (narrow, black) to its floating middle
 * (wide, white), 8 sides, its texture scrolling with the crystal's time.
 */
public class RenderModernEndCrystal extends Render {

	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/endercrystal/endercrystal.png");
	private static final ResourceLocation BEAM = new ResourceLocation("textures/entity/endercrystal/endercrystal_beam.png");
	private final ModelBase withBase = new ModelEnderCrystal(0.0F, true);
	private final ModelBase withoutBase = new ModelEnderCrystal(0.0F, false);

	public RenderModernEndCrystal() {
		shadowSize = 0.5F;
	}

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
		EntityEnderCrystal crystal = (EntityEnderCrystal) entity;
		float time = crystal.innerRotation + partialTicks;
		GL11.glPushMatrix();
		GL11.glTranslatef((float) x, (float) y, (float) z);
		bindTexture(TEXTURE);
		float bob = MathHelper.sin(time * 0.2F) / 2.0F + 0.5F;
		bob += bob * bob;
		(EndCrystals.showsBottom(crystal) ? withBase : withoutBase).render(crystal, 0.0F, time * 3.0F, bob * 0.2F, 0.0F, 0.0F, 0.0625F);
		GL11.glPopMatrix();
		ChunkCoordinates target = EndCrystals.beamTarget(crystal);
		if (target != null) {
			// in the world: the beam's start 2 above the target's middle, its end the crystal's floating middle
			double feetY = crystal.lastTickPosY + (crystal.posY - crystal.lastTickPosY) * partialTicks - crystal.yOffset;
			double entityX = crystal.lastTickPosX + (crystal.posX - crystal.lastTickPosX) * partialTicks;
			double entityZ = crystal.lastTickPosZ + (crystal.posZ - crystal.lastTickPosZ) * partialTicks;
			double entityY = crystal.lastTickPosY + (crystal.posY - crystal.lastTickPosY) * partialTicks;
			double startX = target.posX + 0.5;
			double startY = target.posY + 0.5 + 2.0;
			double startZ = target.posZ + 0.5;
			float crystalY = floatY(time);
			float dx = (float) (entityX - startX);
			float dy = (float) (feetY + 2.0 + crystalY - startY);
			float dz = (float) (entityZ - startZ);
			drawBeam(x + (startX - entityX), y + (startY - entityY), z + (startZ - entityZ), dx, dy, dz, time);
		}
	}

	/** 26.3's floating height of a crystal's middle under its two blocks. */
	private static float floatY(float time) {
		float hh = MathHelper.sin(time * 0.2F) / 2.0F + 0.5F;
		hh = (hh * hh + hh) * 0.4F;
		return hh - 1.4F;
	}

	/** The beam from (x, y, z) along (dx, dy, dz), as 26.3's submitCrystalBeams (its 2 up already in the start). */
	static void drawBeam(double x, double y, double z, float dx, float dy, float dz, float time) {
		float horizontal = MathHelper.sqrt_float(dx * dx + dz * dz);
		float length = MathHelper.sqrt_float(dx * dx + dy * dy + dz * dz);
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef((float) Math.toDegrees(-Math.atan2(dz, dx)) - 90.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef((float) Math.toDegrees(-Math.atan2(horizontal, dy)) - 90.0F, 1.0F, 0.0F, 0.0F);
		net.minecraft.client.Minecraft.getMinecraft().getTextureManager().bindTexture(BEAM);
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		float v0 = -time * 0.01F;
		float v1 = length / 32.0F - time * 0.01F;
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		float lastSin = 0.0F;
		float lastCos = 0.75F;
		float lastU = 0.0F;
		for (int i = 1; i <= 8; i++) {
			float sin = MathHelper.sin(i * (float) (Math.PI * 2) / 8.0F) * 0.75F;
			float cos = MathHelper.cos(i * (float) (Math.PI * 2) / 8.0F) * 0.75F;
			float u = i / 8.0F;
			tessellator.setColorOpaque_I(0);
			tessellator.addVertexWithUV(lastSin * 0.2F, lastCos * 0.2F, 0.0, lastU, v0);
			tessellator.setColorOpaque_I(0xFFFFFF);
			tessellator.addVertexWithUV(lastSin, lastCos, length, lastU, v1);
			tessellator.addVertexWithUV(sin, cos, length, u, v1);
			tessellator.setColorOpaque_I(0);
			tessellator.addVertexWithUV(sin * 0.2F, cos * 0.2F, 0.0, u, v0);
			lastSin = sin;
			lastCos = cos;
			lastU = u;
		}
		tessellator.draw();
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glShadeModel(GL11.GL_FLAT);
		RenderHelper.enableStandardItemLighting();
		GL11.glPopMatrix();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return TEXTURE;
	}
}
