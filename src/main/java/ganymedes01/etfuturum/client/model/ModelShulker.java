package ganymedes01.etfuturum.client.model;

import ganymedes01.etfuturum.entities.EntityShulker;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

/**
 * 26.3's ShulkerModel (SPEC-modern-movement.md §76.13): the base and the lid (all the shulker box draws), and the
 * head; the lid raised by the peek, wobbling once it's past fully open and turned as it opens; the head by its look.
 */
public class ModelShulker extends ModelBase {

	public final ModelRenderer base;
	public final ModelRenderer lid;
	public final ModelRenderer head;

	public ModelShulker() {
		textureWidth = 64;
		textureHeight = 64;
		lid = new ModelRenderer(this);
		lid.setTextureOffset(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16, 12, 16);
		lid.setRotationPoint(0.0F, 24.0F, 0.0F);
		base = new ModelRenderer(this);
		base.setTextureOffset(0, 28).addBox(-8.0F, -8.0F, -8.0F, 16, 8, 16);
		base.setRotationPoint(0.0F, 24.0F, 0.0F);
		head = new ModelRenderer(this);
		head.setTextureOffset(0, 52).addBox(-3.0F, 0.0F, -3.0F, 6, 6, 6);
		head.setRotationPoint(0.0F, 12.0F, 0.0F);
	}

	/** 26.3's setupAnim: the lid by the peek (0 to 1) and the age, the head by its yaw and pitch (degrees, on the shell). */
	public void setupAnim(float peek, float ageInTicks, float headYaw, float headPitch) {
		float bs = (0.5F + peek) * (float) Math.PI;
		float q = -1.0F + MathHelper.sin(bs);
		float extra = 0.0F;
		if (bs > (float) Math.PI) {
			extra = MathHelper.sin(ageInTicks * 0.1F) * 0.7F;
		}
		lid.setRotationPoint(0.0F, 16.0F + MathHelper.sin(bs) * 8.0F + extra, 0.0F);
		lid.rotateAngleY = peek > 0.3F ? q * q * q * q * (float) Math.PI * 0.125F : 0.0F;
		head.rotateAngleX = headPitch * ((float) Math.PI / 180.0F);
		head.rotateAngleY = headYaw * ((float) Math.PI / 180.0F);
	}

	/**
	 * A shulker: its peek between ticks, its head's yaw as 26.3's (its head's yaw less its body's, less 180: its shell
	 * is drawn turned round).
	 */
	@Override
	public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		if (entity instanceof EntityShulker shulker) {
			setupAnim(shulker.getClientPeekAmount(ageInTicks - shulker.ticksExisted), ageInTicks, netHeadYaw - 180.0F, headPitch);
		}
		lid.render(scale);
		base.render(scale);
		head.render(scale);
	}
}
