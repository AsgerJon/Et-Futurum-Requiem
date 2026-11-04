package ganymedes01.etfuturum.client.renderer.entity;

import ganymedes01.etfuturum.client.model.ModelShulker;
import ganymedes01.etfuturum.entities.EntityShulker;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * 26.3's ShulkerRenderer (SPEC-modern-movement.md §76.13): its shell turned round (its body's yaw plus 180), then
 * turned by the face it opens towards about its middle; drawn where its slide from its old block has it; no shadow;
 * the colour's texture (26.3's), or the plain one.
 */
public class ShulkerRenderer extends RenderLiving {

	private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
		"purple", "blue", "brown", "green", "red", "black"};
	private static final ResourceLocation PLAIN = new ResourceLocation("textures/entity/shulker/shulker.png");
	private static final ResourceLocation[] COLORED = new ResourceLocation[16];

	static {
		for (int i = 0; i < 16; i++) {
			COLORED[i] = new ResourceLocation("textures/entity/shulker/shulker_" + COLORS[i] + ".png");
		}
	}

	public ShulkerRenderer() {
		super(new ModelShulker(), 0.0F);
	}

	@Override
	public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTick) {
		double[] offset = ((EntityShulker) entity).getRenderOffset(partialTick);
		if (offset != null) {
			x += offset[0];
			y += offset[1];
			z += offset[2];
		}
		super.doRender(entity, x, y, z, yaw, partialTick);
	}

	/** 26.3's setupRotations: the mob's (death flip and all) at its body's yaw plus 180, then the face's turn about y 0.5. */
	@Override
	protected void rotateCorpse(EntityLivingBase entity, float age, float bodyYaw, float partialTick) {
		super.rotateCorpse(entity, age, bodyYaw + 180.0F, partialTick);
		GL11.glTranslatef(0.0F, 0.5F, 0.0F);
		switch (((EntityShulker) entity).attachFace() ^ 1) {
			case 0 -> GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
			case 2 -> {
				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
			}
			case 3 -> GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
			case 4 -> {
				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
			}
			case 5 -> {
				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);
			}
			default -> {
			}
		}
		GL11.glTranslatef(0.0F, -0.5F, 0.0F);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		byte color = ((EntityShulker) entity).getColor();
		return color >= 0 && color < 16 ? COLORED[color] : PLAIN;
	}
}
