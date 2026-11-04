package ganymedes01.etfuturum.world.structure.modern;

import java.util.Random;

/** 26.3's Rotation: a quarter turn clockwise seen from above, counted from north. */
public enum ModernRotation {
	NONE, CLOCKWISE_90, CLOCKWISE_180, COUNTERCLOCKWISE_90;

	private static final String[] HORIZONTAL = {"north", "east", "south", "west"};

	/** 26.3's Rotation.getRotated: this turn, then the other. */
	public ModernRotation getRotated(ModernRotation other) {
		return values()[(ordinal() + other.ordinal()) % 4];
	}

	/** A horizontal facing turned. */
	public String rotate(String facing) {
		for (int i = 0; i < 4; i++) {
			if (HORIZONTAL[i].equals(facing)) {
				return HORIZONTAL[(i + ordinal()) % 4];
			}
		}
		return facing;
	}

	/** 26.3's Rotation.rotate(int, int): a rotation of a range (16 for banners and signs) turned. */
	public int rotate(int value, int range) {
		return switch (this) {
			case CLOCKWISE_90 -> (value + range / 4) % range;
			case CLOCKWISE_180 -> (value + range / 2) % range;
			case COUNTERCLOCKWISE_90 -> (value + range * 3 / 4) % range;
			default -> value;
		};
	}

	static boolean isHorizontal(String facing) {
		for (String name : HORIZONTAL) {
			if (name.equals(facing)) {
				return true;
			}
		}
		return false;
	}

	/** 26.3's Rotation.getRandom: one of the four (Util.getRandom over the values). */
	public static ModernRotation getRandom(Random random) {
		return values()[random.nextInt(4)];
	}
}
