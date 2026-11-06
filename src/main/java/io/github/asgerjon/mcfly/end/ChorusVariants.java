package io.github.asgerjon.mcfly.end;

import java.util.Random;

/**
 * The chorus plant's choice among its armless models (SPEC-modern-movement.md §84): weighted (the first named in 26.3's
 * list for `noside` itself has 2, the rest 1) and fixed by the place, from 26.3's position seed (`Mth.getSeed`) and a
 * draw of the total weight. Whether each part's draw matches 26.3's `MultiPartModel` exactly is for the picture
 * comparison to show (unchecked so far).
 */
final class ChorusVariants {

	private ChorusVariants() {
	}

	static String pick(String[] models, int x, int y, int z) {
		int total = 0;
		for (String model : models) {
			total += weight(model);
		}
		int draw = new Random(seed(x, y, z)).nextInt(total);
		for (String model : models) {
			draw -= weight(model);
			if (draw < 0) {
				return model;
			}
		}
		return models[0];
	}

	private static int weight(String model) {
		return "chorus_plant_noside".equals(model) ? 2 : 1;
	}

	/** 26.3's `Mth.getSeed(x, y, z)`. */
	static long seed(int x, int y, int z) {
		long seed = (long) (x * 3129871) ^ z * 116129781L ^ y;
		seed = seed * seed * 42317861L + seed * 11L;
		return seed >> 16;
	}
}
