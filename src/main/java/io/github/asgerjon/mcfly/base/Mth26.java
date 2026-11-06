package io.github.asgerjon.mcfly.base;

import net.minecraft.util.Vec3;

/**
 * 26.3's `Mth.sin` and `Mth.cos` (a table of 65536 sines, indexed by the angle times 65536 over 2 pi through a long, so
 * not quite 1.7.10's `MathHelper`, whose index goes through a float) and `Entity.calculateViewVector`: where McFly
 * ports a 26.3 formula that uses them, its numbers come out as 26.3's to the last bit.
 */
public final class Mth26 {

	private static final double SIN_SCALE = 10430.378350470453;
	private static final float[] SIN = new float[65536];

	static {
		for (int i = 0; i < SIN.length; i++) {
			SIN[i] = (float) Math.sin(i / SIN_SCALE);
		}
	}

	private Mth26() {
	}

	public static float sin(double angle) {
		return SIN[(int) ((long) (angle * SIN_SCALE) & 65535L)];
	}

	public static float cos(double angle) {
		return SIN[(int) ((long) (angle * SIN_SCALE + 16384.0) & 65535L)];
	}

	/** 26.3's `Entity.calculateViewVector(xRot, yRot)`: the look for a pitch and yaw in degrees. */
	public static Vec3 viewVector(float pitch, float yaw) {
		float x = pitch * (float) (Math.PI / 180.0);
		float y = -yaw * (float) (Math.PI / 180.0);
		float yCos = cos(y);
		float ySin = sin(y);
		float xCos = cos(x);
		float xSin = sin(x);
		return Vec3.createVectorHelper(ySin * xCos, -xSin, yCos * xCos);
	}
}
