package ganymedes01.etfuturum.movement;

import net.minecraft.entity.Entity;

/**
 * The bubble column cells an entity touched this tick, two bits each in the order they were met, applied after the
 * entity's own movement step (§28). The order matters at the caps: the open top cell's higher cap must come last, as
 * it does when the cells are visited bottom to top.
 */
public final class BubbleColumnQueue {

	private static final int MAX_CELLS = 16;

	private int cells;
	private int count;
	private int tick = -1;

	public void queue(Entity entity, boolean drag, boolean above) {
		if (tick != entity.ticksExisted) {
			// Left over from a tick whose movement step was skipped: apply it now rather than never.
			apply(entity);
			tick = entity.ticksExisted;
		}
		if (count < MAX_CELLS) {
			cells |= ((drag ? 1 : 0) | (above ? 2 : 0)) << (2 * count++);
		}
	}

	public void apply(Entity entity) {
		for (int i = 0; i < count; i++) {
			int cell = cells >>> (2 * i);
			boolean drag = (cell & 1) != 0;
			if ((cell & 2) != 0) {
				BubbleColumns.pushAbove(entity, drag);
			} else {
				BubbleColumns.pushInside(entity, drag);
			}
		}
		cells = 0;
		count = 0;
	}
}
