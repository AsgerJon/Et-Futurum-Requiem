package ganymedes01.etfuturum.client.renderer.entity;

import ganymedes01.etfuturum.client.model.ModelPolarBear;
import ganymedes01.etfuturum.entities.EntityPolarBear;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

/** The polar bear (SPEC-modern-movement.md §60.4): 26.3's adult or cub model and texture; shadow 0.9, a cub's half. */
public class PolarBearRenderer extends RenderLiving {

	private static final ResourceLocation ADULT = new ResourceLocation("textures/entity/bear/polarbear.png");
	private static final ResourceLocation CUB = new ResourceLocation("textures/entity/bear/polarbear_baby.png");
	private final ModelPolarBear adultModel;
	private final ModelPolarBear cubModel = new ModelPolarBear(true);

	public PolarBearRenderer() {
		this(new ModelPolarBear(false));
	}

	private PolarBearRenderer(ModelPolarBear adult) {
		super(adult, 0.9F);
		adultModel = adult;
	}

	@Override
	public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
		boolean cub = entity.isChild();
		mainModel = cub ? cubModel : adultModel;
		shadowSize = cub ? 0.45F : 0.9F;
		super.doRender(entity, x, y, z, yaw, partialTicks);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return entity instanceof EntityPolarBear bear && bear.isChild() ? CUB : ADULT;
	}
}
