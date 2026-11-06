package io.github.asgerjon.mcfly.potion;

import ganymedes01.etfuturum.potion.ModPotions;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 26.3's effect blend (`MobEffectInstance.BlendState`; SPEC-modern-movement.md §90.9, step H4), kept on the client
 * for each entity's effects that blend: toward 1 while the effect lasts past its blend-out advance, toward 0 after, by
 * 1 / its blend ticks a tick, read between ticks. Only Darkness blends (26.3's `setBlendDuration(22)`: in, out and the
 * out's advance 22).
 */
public final class EffectBlend {

	private static final Map<EntityLivingBase, Map<Integer, float[]>> STATES = new WeakHashMap<>();

	private EffectBlend() {
	}

	/** 26.3's blend ticks of an effect (0: none). */
	public static int blendTicks(Potion potion) {
		return potion != null && potion == ModPotions.darkness ? 22 : 0;
	}

	/** 26.3's `BlendState.tick`, once a client tick for each blending effect the entity has; the gone ones forgotten. */
	public static synchronized void tick(EntityLivingBase entity) {
		Map<Integer, float[]> states = STATES.get(entity);
		for (Object entry : entity.getActivePotionEffects()) {
			PotionEffect effect = (PotionEffect) entry;
			int ticks = blendTicks(Potion.potionTypes[effect.getPotionID()]);
			if (ticks == 0) {
				continue;
			}
			if (states == null) {
				states = new HashMap<>();
				STATES.put(entity, states);
			}
			float[] state = states.computeIfAbsent(effect.getPotionID(), id -> new float[2]);
			state[1] = state[0];
			// 26.3's `hasEffect`: not ending within its blend-out advance
			float target = effect.getDuration() > ticks ? 1.0F : 0.0F;
			if (state[0] != target) {
				float step = 1.0F / ticks;
				state[0] += Math.max(-step, Math.min(step, target - state[0]));
			}
		}
		if (states != null) {
			for (Iterator<Integer> ids = states.keySet().iterator(); ids.hasNext(); ) {
				if (entity.getActivePotionEffect(Potion.potionTypes[ids.next()]) == null) {
					ids.remove();
				}
			}
		}
	}

	/** 26.3's `getBlendFactor`: 0 without the effect; between the last tick's and this one's. */
	public static synchronized float factor(EntityLivingBase entity, Potion potion, float partialTicks) {
		Map<Integer, float[]> states = STATES.get(entity);
		float[] state = potion == null || states == null ? null : states.get(potion.id);
		if (state == null) {
			return 0.0F;
		}
		if (entity.isDead) {
			state[1] = state[0];
		}
		return state[1] + (state[0] - state[1]) * partialTicks;
	}
}
