package ganymedes01.etfuturum.mixins.early.modernmovement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.movement.IClientCamera;
import ganymedes01.etfuturum.movement.ModernWater;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.nio.FloatBuffer;

/**
 * Camera in water (§27), as modern's Camera.getFluidInCamera: the camera's own position against the liquid's surface.
 * This decides the water fog, its colour and the underwater FOV.
 */
@Mixin(ActiveRenderInfo.class)
public class MixinActiveRenderInfo {

	@Shadow
	private static FloatBuffer modelview;

	/**
	 * Vanilla tests a point 0.05 in front of the camera (the near plane) plus getEyeHeight() upwards. 1.7.10 puts the
	 * first-person camera 0.1 behind the eye, so that point is 0.05 behind the eye along the view direction and 0.12
	 * above it; the 1/9 liquid margin (below) cancels most of the 0.12 for a standing player. With the pose offset in
	 * getEyeHeight() the point sank 1.1 blocks under a swimmer's eyes. Modern tests the camera position itself: in
	 * first person the eye (feet + smoothed eye height, §13.1, no view bobbing), in third person the camera, read back
	 * from the modelview. Sleeping keeps vanilla's special camera.
	 */
	@ModifyExpressionValue(method = "getBlockAtEntityViewpoint", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;projectViewFromEntity(Lnet/minecraft/entity/EntityLivingBase;D)Lnet/minecraft/util/Vec3;"))
	private static Vec3 etfu$cameraPosition(Vec3 viewpoint, @Local(argsOnly = true) EntityLivingBase entity, @Local(argsOnly = true) float partialTicks) {
		Minecraft mc = Minecraft.getMinecraft();
		if (entity != mc.renderViewEntity || entity.isPlayerSleeping()) {
			return viewpoint;
		}
		double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
		double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
		double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
		if (mc.gameSettings.thirdPersonView == 0) {
			return Vec3.createVectorHelper(x, y - entity.yOffset + ((IClientCamera) mc.entityRenderer).etfu$getCameraEyeHeight(partialTicks), z);
		}
		// The camera relative to the entity's position: the modelview's rotation, transposed, applied to its translation.
		float m0 = modelview.get(0), m1 = modelview.get(1), m2 = modelview.get(2);
		float m4 = modelview.get(4), m5 = modelview.get(5), m6 = modelview.get(6);
		float m8 = modelview.get(8), m9 = modelview.get(9), m10 = modelview.get(10);
		float tx = modelview.get(12), ty = modelview.get(13), tz = modelview.get(14);
		return Vec3.createVectorHelper(
				x - (m0 * tx + m1 * ty + m2 * tz),
				y - (m4 * tx + m5 * ty + m6 * tz),
				z - (m8 * tx + m9 * ty + m10 * tz));
	}

	/**
	 * No margin above the surface: the liquid's threshold is (y + 1) - (depth from the top).
	 */
	@ModifyConstant(method = "getBlockAtEntityViewpoint", constant = @Constant(floatValue = 0.11111111F))
	private static float etfu$noMarginAboveSurface(float margin) {
		return 0.0F;
	}

	/**
	 * The depth from the top comes from the same surface height the water scan uses (§9.1): one minus it, so the
	 * threshold becomes y + surface height. This also covers Forge fluids, whose metadata vanilla reads as vanilla
	 * water levels.
	 */
	@ModifyExpressionValue(method = "getBlockAtEntityViewpoint", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockLiquid;getLiquidHeightPercent(I)F"))
	private static float etfu$depthFromSurface(float liquidHeightPercent, @Local(argsOnly = true) World world, @Local ChunkPosition pos, @Local Block block) {
		return (float) (1.0D - ModernWater.getSurfaceHeight(world, pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ, block, true));
	}
}
