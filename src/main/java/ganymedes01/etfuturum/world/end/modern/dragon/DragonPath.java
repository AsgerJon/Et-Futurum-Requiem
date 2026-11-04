package ganymedes01.etfuturum.world.end.modern.dragon;

import java.util.List;

/** A path over the dragon's nodes (26.3's `Path`): node places, the next one to fly to. */
final class DragonPath {

	private final List<int[]> nodes;
	private int next;

	DragonPath(List<int[]> nodes) {
		this.nodes = nodes;
	}

	boolean isDone() {
		return next >= nodes.size();
	}

	int[] next() {
		return nodes.get(next);
	}

	void advance() {
		next++;
	}

	int size() {
		return nodes.size();
	}

	int[] get(int i) {
		return nodes.get(i);
	}
}
