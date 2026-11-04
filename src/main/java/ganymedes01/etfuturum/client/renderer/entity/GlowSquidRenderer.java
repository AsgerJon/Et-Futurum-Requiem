package ganymedes01.etfuturum.client.renderer.entity;

import net.minecraft.client.model.ModelSquid;
import net.minecraft.client.renderer.entity.RenderSquid;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * The glow squid (SPEC-modern-movement.md §67.1, 26.3's `GlowSquidRenderer`): 1.7.10's squid with 26.3's glow squid
 * texture (the same layout). Its light is the entity's own (`EntityGlowSquid.getBrightnessForRender`).
 */
public class GlowSquidRenderer extends RenderSquid {

	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/squid/glow_squid.png");

	public GlowSquidRenderer() {
		super(new ModelSquid(), 0.7F);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return TEXTURE;
	}
}
