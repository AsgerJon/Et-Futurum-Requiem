package ganymedes01.etfuturum.client.model;

import ganymedes01.etfuturum.entities.EntityPhantom;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

/**
 * The phantom (SPEC-modern-movement.md §68.1, 26.3's `PhantomModel`, the same boxes, 64 x 64): a body with the head,
 * two wings of two parts and a tail of two; the wings flap 16 degrees and the tail by `cos`, at 7.448451 degrees a tick
 * from the phantom's own offset (`id x 3`).
 */
public class ModelPhantom extends ModelBase {

	private final ModelRenderer body, leftWingBase, leftWingTip, rightWingBase, rightWingTip, tailBase, tailTip;

	public ModelPhantom() {
		textureWidth = 64;
		textureHeight = 64;
		body = new ModelRenderer(this, 0, 8);
		body.addBox(-3.0F, -2.0F, -8.0F, 5, 3, 9);
		body.rotateAngleX = -0.1F;
		tailBase = new ModelRenderer(this, 3, 20);
		tailBase.addBox(-2.0F, 0.0F, 0.0F, 3, 2, 6);
		tailBase.setRotationPoint(0.0F, -2.0F, 1.0F);
		body.addChild(tailBase);
		tailTip = new ModelRenderer(this, 4, 29);
		tailTip.addBox(-1.0F, 0.0F, 0.0F, 1, 1, 6);
		tailTip.setRotationPoint(0.0F, 0.5F, 6.0F);
		tailBase.addChild(tailTip);
		leftWingBase = new ModelRenderer(this, 23, 12);
		leftWingBase.addBox(0.0F, 0.0F, 0.0F, 6, 2, 9);
		leftWingBase.setRotationPoint(2.0F, -2.0F, -8.0F);
		leftWingBase.rotateAngleZ = 0.1F;
		body.addChild(leftWingBase);
		leftWingTip = new ModelRenderer(this, 16, 24);
		leftWingTip.addBox(0.0F, 0.0F, 0.0F, 13, 1, 9);
		leftWingTip.setRotationPoint(6.0F, 0.0F, 0.0F);
		leftWingTip.rotateAngleZ = 0.1F;
		leftWingBase.addChild(leftWingTip);
		rightWingBase = new ModelRenderer(this, 23, 12);
		rightWingBase.mirror = true;
		rightWingBase.addBox(-6.0F, 0.0F, 0.0F, 6, 2, 9);
		rightWingBase.setRotationPoint(-3.0F, -2.0F, -8.0F);
		rightWingBase.rotateAngleZ = -0.1F;
		body.addChild(rightWingBase);
		rightWingTip = new ModelRenderer(this, 16, 24);
		rightWingTip.mirror = true;
		rightWingTip.addBox(-13.0F, 0.0F, 0.0F, 13, 1, 9);
		rightWingTip.setRotationPoint(-6.0F, 0.0F, 0.0F);
		rightWingTip.rotateAngleZ = -0.1F;
		rightWingBase.addChild(rightWingTip);
		ModelRenderer head = new ModelRenderer(this, 0, 0);
		head.addBox(-4.0F, -2.0F, -5.0F, 7, 3, 5);
		head.setRotationPoint(0.0F, 1.0F, -7.0F);
		head.rotateAngleX = 0.2F;
		body.addChild(head);
	}

	@Override
	public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch, float scale) {
		setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, scale, entity);
		body.render(scale);
	}

	/** unitTAS's comparison views: the wings held at this flap time in every phantom (26.3's run holds them at the same); NaN: live. */
	public static float heldFlapTime = Float.NaN;

	@Override
	public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch, float scale, Entity entity) {
		float flapTime = !Float.isNaN(heldFlapTime) ? heldFlapTime
			: (entity instanceof EntityPhantom phantom ? phantom.getUniqueFlapTickOffset() : 0) + ageInTicks;
		float anim = flapTime * EntityPhantom.FLAP_DEGREES_PER_TICK * (float) Math.PI / 180.0F;
		float flap = MathHelper.cos(anim) * 16.0F * (float) Math.PI / 180.0F;
		leftWingBase.rotateAngleZ = flap;
		leftWingTip.rotateAngleZ = flap;
		rightWingBase.rotateAngleZ = -flap;
		rightWingTip.rotateAngleZ = -flap;
		float tail = -(5.0F + MathHelper.cos(anim * 2.0F) * 5.0F) * (float) Math.PI / 180.0F;
		tailBase.rotateAngleX = tail;
		tailTip.rotateAngleX = tail;
	}
}
