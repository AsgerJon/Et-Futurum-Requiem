package ganymedes01.etfuturum.world.structure.modern;

/** 26.3's Mirror: none, left-right (z negated), front-back (x negated). */
public enum ModernMirror {
	NONE, LEFT_RIGHT, FRONT_BACK;

	/** A horizontal facing mirrored: left-right swaps north and south, front-back east and west. */
	public String mirror(String facing) {
		return switch (this) {
			case LEFT_RIGHT -> facing.equals("north") ? "south" : facing.equals("south") ? "north" : facing;
			case FRONT_BACK -> facing.equals("east") ? "west" : facing.equals("west") ? "east" : facing;
			default -> facing;
		};
	}

	/** 26.3's Mirror.mirror(int, int). */
	public int mirror(int value, int range) {
		int half = range / 2;
		int unwound = value > half ? value - range : value;
		return switch (this) {
			case LEFT_RIGHT -> (half - unwound + range) % range;
			case FRONT_BACK -> (range - unwound) % range;
			default -> value;
		};
	}
}
