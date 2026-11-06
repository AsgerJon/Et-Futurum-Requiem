package io.github.asgerjon.mcfly.gameevent;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Posting game events and the listeners that hear them (SPEC-modern-movement.md §90.9, step H1), 26.3's
 * `GameEventDispatcher` and `EuclideanGameEventListenerRegistry`: a listener registry per world by chunk section (where
 * it was registered); an event visits the sections within its radius, and reaches each listener there within the
 * listener's own radius. A plain listener takes it as visited; a vibration listener (`byDistance`) after the plain ones,
 * nearest first (26.3's `DeliveryMode.BY_DISTANCE`). On the server only.
 */
public final class GameEvents {

	private static final Map<World, Map<Long, List<GameEventListener>>> LISTENERS = new WeakHashMap<>();

	private GameEvents() {
	}

	private static long section(int sx, int sy, int sz) {
		return ((long) sx & 0x3FFFFFL) << 42 | ((long) sy & 0xFFFFFL) << 22 | (long) sz & 0x3FFFFFL;
	}

	private static long sectionAt(double[] position) {
		return section(MathHelper.floor_double(position[0]) >> 4, MathHelper.floor_double(position[1]) >> 4, MathHelper.floor_double(position[2]) >> 4);
	}

	/** Where it listens from now; nothing if it can't be placed. */
	public static synchronized void register(World world, GameEventListener listener) {
		double[] position = listener.position(world);
		if (world == null || world.isRemote || position == null) {
			return;
		}
		LISTENERS.computeIfAbsent(world, w -> new HashMap<>()).computeIfAbsent(sectionAt(position), s -> new ArrayList<>()).add(listener);
	}

	public static synchronized void unregister(World world, GameEventListener listener) {
		Map<Long, List<GameEventListener>> sections = LISTENERS.get(world);
		if (sections == null) {
			return;
		}
		sections.values().removeIf(list -> list.remove(listener) && list.isEmpty());
	}

	/** 26.3's `level.gameEvent(event, pos, context)`: on the server only. */
	public static void post(World world, GameEvent event, double x, double y, double z, Entity source, Block affected, int affectedMeta) {
		if (world == null || world.isRemote || event == null) {
			return;
		}
		List<GameEventListener> plain = new ArrayList<>();
		List<Object[]> byDistance = new ArrayList<>();
		synchronized (GameEvents.class) {
			Map<Long, List<GameEventListener>> sections = LISTENERS.get(world);
			if (sections == null || sections.isEmpty()) {
				return;
			}
			int r = event.radius;
			int bx = MathHelper.floor_double(x), by = MathHelper.floor_double(y), bz = MathHelper.floor_double(z);
			for (int sx = (bx - r) >> 4; sx <= (bx + r) >> 4; sx++) {
				for (int sz = (bz - r) >> 4; sz <= (bz + r) >> 4; sz++) {
					for (int sy = (by - r) >> 4; sy <= (by + r) >> 4; sy++) {
						List<GameEventListener> here = sections.get(section(sx, sy, sz));
						if (here == null) {
							continue;
						}
						for (GameEventListener listener : here) {
							double[] at = listener.position(world);
							if (at == null) {
								continue;
							}
							double dx = at[0] - x, dy = at[1] - y, dz = at[2] - z;
							double distance = dx * dx + dy * dy + dz * dz;
							if (distance > (double) listener.radius() * listener.radius()) {
								continue;
							}
							if (listener.byDistance()) {
								byDistance.add(new Object[]{listener, distance});
							} else {
								plain.add(listener);
							}
						}
					}
				}
			}
		}
		GameEvent.Context context = new GameEvent.Context(source, affected, affectedMeta);
		for (GameEventListener listener : plain) {
			listener.handle(world, event, context, x, y, z);
		}
		if (!byDistance.isEmpty()) {
			byDistance.sort(Comparator.comparingDouble(entry -> (Double) entry[1]));
			for (Object[] entry : byDistance) {
				((GameEventListener) entry[0]).handle(world, event, context, x, y, z);
			}
		}
	}

	public static void post(World world, GameEvent event, double x, double y, double z, Entity source) {
		post(world, event, x, y, z, source, null, 0);
	}

	/** At a block's middle (26.3's `Vec3.atCenterOf(pos)`), about that block. */
	public static void postAtBlock(World world, String event, int x, int y, int z, Entity source, Block affected, int affectedMeta) {
		post(world, GameEvent.get(event), x + 0.5D, y + 0.5D, z + 0.5D, source, affected, affectedMeta);
	}

	/** At an entity, by it (26.3's `entity.gameEvent(event)`). */
	public static void postAt(Entity entity, String event) {
		if (entity != null) {
			post(entity.worldObj, GameEvent.get(event), entity.posX, entity.boundingBox.minY, entity.posZ, entity, null, 0);
		}
	}

	/** For the tests and the dead world: forget a world's listeners. */
	public static synchronized void forget(World world) {
		LISTENERS.remove(world);
	}

	/** How many listeners a world has (for the tests). */
	public static synchronized int count(World world) {
		Map<Long, List<GameEventListener>> sections = LISTENERS.get(world);
		return sections == null ? 0 : sections.values().stream().mapToInt(List::size).sum();
	}
}
