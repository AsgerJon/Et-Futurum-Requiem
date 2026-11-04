package ganymedes01.etfuturum.client.renderer.entity;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/** An entity drawn as nothing (26.3's area effect cloud is only its particles). */
public class RenderNothing extends Render {

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return null;
	}
}
