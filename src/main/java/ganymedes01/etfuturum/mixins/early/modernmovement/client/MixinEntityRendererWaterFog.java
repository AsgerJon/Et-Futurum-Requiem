package ganymedes01.etfuturum.mixins.early.modernmovement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.movement.IClientPosePlayer;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.potion.Potion;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modern underwater fog (§27): the eyes adjust to the water over 30 seconds instead of Respiration giving clear vision.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererWaterFog {

	@Unique
	private static final float etfu$WATER_FOG_START = -8.0F;

	@Unique
	private static final float etfu$WATER_FOG_END = 96.0F;

	@Shadow
	private Minecraft mc;

	@Shadow
	private boolean cloudFog;

	@Shadow
	float fogColorRed;

	@Shadow
	float fogColorGreen;

	@Shadow
	float fogColorBlue;

	/**
	 * Respiration no longer brightens and thins the underwater fog, as in 1.13+. In 1.7.10 each level added 0.2 to the
	 * fog colour and took 0.03 off its density; it still extends breathing time.
	 */
	@ModifyExpressionValue(method = {"updateFogColor", "setupFog"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;getRespiration(Lnet/minecraft/entity/EntityLivingBase;)I"))
	private int etfu$noRespirationVision(int respiration) {
		return 0;
	}

	/**
	 * Under water the fog colour is brightened by how far the eyes have adjusted, towards the same colour at full
	 * brightness. This replaces Night Vision's fog brightening under water, as in modern.
	 */
	@Inject(method = "updateFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z", ordinal = 1))
	private void etfu$brightenWaterFog(float partialTicks, CallbackInfo ci, @Local Block block) {
		if (block.getMaterial() != Material.water || fogColorRed == 0.0F || fogColorGreen == 0.0F || fogColorBlue == 0.0F) {
			return;
		}
		float brighten = mc.renderViewEntity instanceof IClientPosePlayer ? ((IClientPosePlayer) mc.renderViewEntity).etfu$getWaterVision() : 1.0F;
		float scale = 1.0F / Math.max(fogColorRed, Math.max(fogColorGreen, fogColorBlue));
		fogColorRed += (fogColorRed * scale - fogColorRed) * brighten;
		fogColorGreen += (fogColorGreen * scale - fogColorGreen) * brighten;
		fogColorBlue += (fogColorBlue * scale - fogColorBlue) * brighten;
	}

	@ModifyExpressionValue(method = "updateFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z", ordinal = 1))
	private boolean etfu$noNightVisionInWaterFog(boolean nightVision, @Local Block block) {
		return nightVision && block.getMaterial() != Material.water;
	}

	/**
	 * Under water the fog is linear from -8 to 96 blocks, with the far end pulled in to as little as a quarter while
	 * the eyes haven't adjusted yet. Replaces 1.7.10's dense exponential water fog (blindness and cloud fog still win).
	 */
	@Inject(method = "setupFog", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColorMaterial(II)V"))
	private void etfu$modernWaterFogDistance(int startCoords, float partialTicks, CallbackInfo ci, @Local Block block) {
		if (block.getMaterial() != Material.water || cloudFog || mc.renderViewEntity.isPotionActive(Potion.blindness)) {
			return;
		}
		float vision = mc.renderViewEntity instanceof IClientPosePlayer ? ((IClientPosePlayer) mc.renderViewEntity).etfu$getWaterVision() : 1.0F;
		GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
		GL11.glFogf(GL11.GL_FOG_START, etfu$WATER_FOG_START);
		GL11.glFogf(GL11.GL_FOG_END, etfu$WATER_FOG_END * Math.max(0.25F, vision));
		if (GLContext.getCapabilities().GL_NV_fog_distance) {
			GL11.glFogi(34138, 34139);
		}
	}
}
