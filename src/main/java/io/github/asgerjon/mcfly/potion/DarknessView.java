package io.github.asgerjon.mcfly.potion;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.potion.ModPotions;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

/**
 * Darkness on the client (SPEC-modern-movement.md §90.9, step H4): 26.3's `LightmapRenderStateExtractor` (the
 * brightness option less the blend, and its pulsing darkness scale taken off the lightmap's colours) and
 * `DarknessFogEnvironment` (the fog from the render distance toward 15). By 26.3's `darknessEffectScale` option, 1
 * until McFly's options carry it.
 */
@SideOnly(Side.CLIENT)
public final class DarknessView {

	public static final float EFFECT_SCALE = 1.0F;

	private DarknessView() {
	}

	/** 26.3's `darknessEffectBrightnessModifier`: the camera's Darkness blend, by the option. */
	public static float brightnessModifier(float partialTicks) {
		EntityLivingBase camera = Minecraft.getMinecraft().renderViewEntity;
		return camera == null || ModPotions.darkness == null ? 0.0F : EffectBlend.factor(camera, ModPotions.darkness, partialTicks) * EFFECT_SCALE;
	}

	/** 26.3's `calculateDarknessScale`, by the option: max(0, cos((ticks - partial) x pi x 0.025) x 0.45 x modifier). */
	public static float darknessScale(float modifier, float partialTicks) {
		EntityLivingBase camera = Minecraft.getMinecraft().renderViewEntity;
		if (camera == null || modifier <= 0.0F) {
			return 0.0F;
		}
		return Math.max(0.0F, MathHelper.cos((camera.ticksExisted - partialTicks) * (float) Math.PI * 0.025F) * 0.45F * modifier) * EFFECT_SCALE;
	}

	/** 26.3's Darkness fog end: from the render distance toward 15 by the blend (its start 3/4 of it); the render distance without. */
	public static float fogEnd(float renderDistance, float partialTicks) {
		EntityLivingBase camera = Minecraft.getMinecraft().renderViewEntity;
		if (camera == null || ModPotions.darkness == null || camera.getActivePotionEffect(ModPotions.darkness) == null) {
			return renderDistance;
		}
		float blend = EffectBlend.factor(camera, ModPotions.darkness, partialTicks);
		return renderDistance + (15.0F - renderDistance) * blend;
	}
}
