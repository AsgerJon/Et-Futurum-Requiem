package ganymedes01.etfuturum.client.model;

import ganymedes01.etfuturum.entities.EntityPolarBear;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * 26.3's polar bear models (SPEC-modern-movement.md §60.4): the adult (texture 128 × 64, drawn 1.2 times as big with
 * its feet on the ground) or the cub's own model (64 × 64, at its own size). Both walk as a quadruped, turn the head
 * with the look and share the stand: body tipped back, front legs up, head raised.
 */
public class ModelPolarBear extends ModelBase {

	private final boolean cub;
	private final ModelRenderer head;
	private final ModelRenderer body;
	private final ModelRenderer rightHindLeg;
	private final ModelRenderer leftHindLeg;
	private final ModelRenderer rightFrontLeg;
	private final ModelRenderer leftFrontLeg;
	/** The parts' rest: rotation point x, y, z and turn about x, per part, in the order above. */
	private final float[][] rest;
	private float standScale;

	public ModelPolarBear(boolean cub) {
		this.cub = cub;
		if (cub) {
			textureWidth = 64;
			textureHeight = 64;
			body = part(0, 9, 0.0F, 17.5F, 0.0F);
			body.addBox(-4.0F, -3.5F, -6.0F, 8, 7, 12);
			head = part(0, 0, 0.0F, 18.625F, -5.75F);
			head.addBox(-3.0F, -2.625F, -4.25F, 6, 5, 4);
			head.setTextureOffset(20, 3).addBox(-2.0F, 0.375F, -6.25F, 4, 2, 2);
			head.setTextureOffset(20, 0).addBox(-4.0F, -3.625F, -2.75F, 2, 2, 1);
			head.setTextureOffset(26, 0).addBox(2.0F, -3.625F, -2.75F, 2, 2, 1);
			rightHindLeg = part(0, 34, -2.5F, 21.5F, 4.5F);
			rightHindLeg.addBox(-1.5F, -0.5F, -1.5F, 3, 3, 3);
			leftHindLeg = part(12, 34, 2.5F, 21.5F, 4.5F);
			leftHindLeg.addBox(-1.5F, -0.5F, -1.5F, 3, 3, 3);
			rightFrontLeg = part(0, 28, -2.5F, 21.5F, -4.5F);
			rightFrontLeg.addBox(-1.5F, -0.5F, -1.5F, 3, 3, 3);
			leftFrontLeg = part(12, 28, 2.5F, 21.5F, -4.5F);
			leftFrontLeg.addBox(-1.5F, -0.5F, -1.5F, 3, 3, 3);
			rest = new float[][]{{0.0F, 18.625F, -5.75F, 0.0F}, {0.0F, 17.5F, 0.0F, 0.0F}, {-2.5F, 21.5F, 4.5F, 0.0F}, {2.5F, 21.5F, 4.5F, 0.0F},
				{-2.5F, 21.5F, -4.5F, 0.0F}, {2.5F, 21.5F, -4.5F, 0.0F}};
		} else {
			textureWidth = 128;
			textureHeight = 64;
			head = part(0, 0, 0.0F, 10.0F, -16.0F);
			head.addBox(-3.5F, -3.0F, -3.0F, 7, 7, 7);
			head.setTextureOffset(0, 44).addBox(-2.5F, 1.0F, -6.0F, 5, 3, 3);
			head.setTextureOffset(26, 0).addBox(-4.5F, -4.0F, -1.0F, 2, 2, 1);
			head.mirror = true;
			head.setTextureOffset(26, 0).addBox(2.5F, -4.0F, -1.0F, 2, 2, 1);
			head.mirror = false;
			body = part(0, 19, -2.0F, 9.0F, 12.0F);
			body.addBox(-5.0F, -13.0F, -7.0F, 14, 14, 11);
			body.setTextureOffset(39, 0).addBox(-4.0F, -25.0F, -7.0F, 12, 12, 10);
			rightHindLeg = part(50, 22, -4.5F, 14.0F, 6.0F);
			rightHindLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 10, 8);
			leftHindLeg = part(50, 22, 4.5F, 14.0F, 6.0F);
			leftHindLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 10, 8);
			rightFrontLeg = part(50, 40, -3.5F, 14.0F, -8.0F);
			rightFrontLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 10, 6);
			leftFrontLeg = part(50, 40, 3.5F, 14.0F, -8.0F);
			leftFrontLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 10, 6);
			rest = new float[][]{{0.0F, 10.0F, -16.0F, 0.0F}, {-2.0F, 9.0F, 12.0F, (float) Math.PI / 2.0F}, {-4.5F, 14.0F, 6.0F, 0.0F},
				{4.5F, 14.0F, 6.0F, 0.0F}, {-3.5F, 14.0F, -8.0F, 0.0F}, {3.5F, 14.0F, -8.0F, 0.0F}};
		}
	}

	private ModelRenderer part(int u, int v, float x, float y, float z) {
		ModelRenderer part = new ModelRenderer(this, u, v);
		part.setTextureSize(textureWidth, textureHeight);
		part.setRotationPoint(x, y, z);
		return part;
	}

	private ModelRenderer[] parts() {
		return new ModelRenderer[]{head, body, rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg};
	}

	@Override
	public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
		standScale = entity instanceof EntityPolarBear bear ? bear.getStandingAnimationScale(partialTicks) : 0.0F;
	}

	@Override
	public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
		ModelRenderer[] parts = parts();
		for (int i = 0; i < parts.length; i++) {
			parts[i].setRotationPoint(rest[i][0], rest[i][1], rest[i][2]);
			parts[i].rotateAngleX = rest[i][3];
			parts[i].rotateAngleY = 0.0F;
			parts[i].rotateAngleZ = 0.0F;
		}
		head.rotateAngleX = headPitch / (180.0F / (float) Math.PI);
		head.rotateAngleY = netHeadYaw / (180.0F / (float) Math.PI);
		rightHindLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		leftHindLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
		rightFrontLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
		leftFrontLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		float stand = standScale * standScale;
		float age = cub ? 0.5F : 1.0F;
		body.rotateAngleX -= stand * (float) Math.PI * 0.35F;
		body.rotationPointY += stand * age * 2.0F;
		rightFrontLeg.rotationPointY -= stand * age * 20.0F;
		rightFrontLeg.rotationPointZ += stand * age * 4.0F;
		rightFrontLeg.rotateAngleX -= stand * (float) Math.PI * 0.45F;
		leftFrontLeg.rotationPointY = rightFrontLeg.rotationPointY;
		leftFrontLeg.rotationPointZ = rightFrontLeg.rotationPointZ;
		leftFrontLeg.rotateAngleX -= stand * (float) Math.PI * 0.45F;
		head.rotationPointY -= stand * 24.0F;
		head.rotationPointZ += stand * 13.0F;
		head.rotateAngleX += stand * (float) Math.PI * 0.15F;
	}

	@Override
	public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
		GL11.glPushMatrix();
		if (!cub) {
			// 26.3's 1.2 times, about a point lifted so the feet stay on the ground (24.016 * (1 - 1.2) pixels)
			GL11.glTranslatef(0.0F, 24.016F * (1.0F - 1.2F) * scale, 0.0F);
			GL11.glScalef(1.2F, 1.2F, 1.2F);
		}
		for (ModelRenderer part : parts()) {
			part.render(scale);
		}
		GL11.glPopMatrix();
	}
}
