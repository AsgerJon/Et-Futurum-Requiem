package io.github.asgerjon.mcfly.gameevent;

import io.github.asgerjon.mcfly.particle.McFlyParticles;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.UUID;

/**
 * Vibrations (SPEC-modern-movement.md §90.9, step H1), 26.3's `VibrationSystem`: a listener takes a vibration event it
 * may hear (in its tag; not a sneaking source's for the sneak-ignored events, not a spectator's, not a dampening item's
 * or state's; not through wool); of a tick's candidates the nearest (on a tie the higher frequency, 26.3's
 * `VibrationSelector`), chosen the next tick; it travels floor(distance) ticks, its particle flying to the listener;
 * then it's delivered.
 */
public final class VibrationSystem {

	private VibrationSystem() {
	}

	/** 26.3's `getRedstoneStrengthForDistance`: max(1, 15 - floor(15 / radius x distance)). */
	public static int redstoneStrengthForDistance(float distance, int radius) {
		double scale = 15.0D / radius;
		return Math.max(1, 15 - MathHelper.floor_double(scale * distance));
	}

	/** 26.3's `#dampens_vibrations`: wool, its carpets (1.7.10 has no wool slabs or stairs). */
	public static boolean dampens(Block block) {
		return block == Blocks.wool || block == Blocks.carpet;
	}

	/** 26.3's `#occludes_vibration_signals`: wool. */
	public static boolean occludes(Block block) {
		return block == Blocks.wool;
	}

	/** 26.3's `VibrationSystem.User`: what listens. */
	public interface User {
		int radius();

		/** Where it hears from (x, y, z), or null. */
		double[] position(World world);

		boolean canReceive(World world, int x, int y, int z, GameEvent event, GameEvent.Context context);

		void onReceive(World world, int x, int y, int z, GameEvent event, Entity source, float distance);

		/** The events it hears (26.3's `getListenableEvents`): `#vibrations` unless it says. */
		default String listenableTag() {
			return "minecraft:vibrations";
		}

		default int travelTime(float distance) {
			return (int) Math.floor(distance);
		}

		/** 26.3's `isValidVibration`. */
		default boolean isValid(GameEvent event, GameEvent.Context context) {
			if (!event.is(listenableTag())) {
				return false;
			}
			Entity source = context.source;
			if (source != null) {
				if (source instanceof EntityPlayer player && io.github.asgerjon.mcfly.spectator.Spectator.isSpectator(player)) {
					return false;
				}
				if (source.isSneaking() && event.is("minecraft:ignore_vibrations_sneaking")) {
					return false;
				}
				// 26.3's `dampensVibrations`: an item of `#dampens_vibrations` (wool, carpets)
				if (source instanceof EntityItem item && item.getEntityItem() != null && dampens(Block.getBlockFromItem(item.getEntityItem().getItem()))) {
					return false;
				}
			}
			return context.affected == null || !dampens(context.affected);
		}

		default void onDataChanged() {
		}

		/** 26.3's `requiresAdjacentChunksToBeTicking`: it takes a vibration only with the chunks round it loaded (a sensor's). */
		default boolean requiresAdjacentChunks() {
			return false;
		}
	}

	/** 26.3's `VibrationInfo`: the event, its distance, where it came from, who made it. */
	static final class Info {
		final GameEvent event;
		final float distance;
		final double x, y, z;
		final UUID source;

		Info(GameEvent event, float distance, double x, double y, double z, UUID source) {
			this.event = event;
			this.distance = distance;
			this.x = x;
			this.y = y;
			this.z = z;
			this.source = source;
		}

		NBTTagCompound write() {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("game_event", event.name);
			tag.setFloat("distance", distance);
			tag.setDouble("x", x);
			tag.setDouble("y", y);
			tag.setDouble("z", z);
			if (source != null) {
				tag.setString("source", source.toString());
			}
			return tag;
		}

		static Info read(NBTTagCompound tag) {
			GameEvent event = GameEvent.get(tag.getString("game_event"));
			if (event == null) {
				return null;
			}
			UUID source = tag.hasKey("source") ? UUID.fromString(tag.getString("source")) : null;
			return new Info(event, tag.getFloat("distance"), tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"), source);
		}

		Entity entity(World world) {
			if (source == null) {
				return null;
			}
			for (Object entry : world.loadedEntityList) {
				Entity entity = (Entity) entry;
				if (source.equals(entity.getUniqueID())) {
					return entity;
				}
			}
			return null;
		}
	}

	/** 26.3's `VibrationSystem.Data` with its `VibrationSelector`: the vibration in flight and its time left, this tick's candidate; saved with its listener. */
	public static final class Data {
		private Info current;
		private int travelTime;
		private Info candidate;
		private long candidateTick;
		private boolean reloadParticle = true;

		public void writeToNBT(NBTTagCompound tag) {
			tag.setInteger("event_delay", travelTime);
			if (current != null) {
				tag.setTag("event", current.write());
			}
			if (candidate != null) {
				NBTTagCompound selector = candidate.write();
				selector.setLong("tick", candidateTick);
				tag.setTag("selector", selector);
			}
		}

		public void readFromNBT(NBTTagCompound tag) {
			travelTime = tag.getInteger("event_delay");
			current = tag.hasKey("event") ? Info.read(tag.getCompoundTag("event")) : null;
			candidate = tag.hasKey("selector") ? Info.read(tag.getCompoundTag("selector")) : null;
			candidateTick = tag.hasKey("selector") ? tag.getCompoundTag("selector").getLong("tick") : 0L;
			reloadParticle = true;
		}

		/** The event in flight, or null. */
		public GameEvent inFlight() {
			return current == null ? null : current.event;
		}

		public int travelTimeLeft() {
			return travelTime;
		}

		/** 26.3's `shouldReplaceVibration`: none yet; this tick's only, nearer; equally near, a higher frequency. */
		void addCandidate(Info info, long tick) {
			if (candidate == null) {
				candidate = info;
				candidateTick = tick;
				return;
			}
			if (tick != candidateTick) {
				return;
			}
			if (info.distance < candidate.distance || info.distance == candidate.distance && info.event.frequency > candidate.event.frequency) {
				candidate = info;
			}
		}

		/** 26.3's `chosenCandidate`: one from before this tick. */
		Info chosen(long now) {
			return candidate != null && candidateTick < now ? candidate : null;
		}
	}

	/** 26.3's `VibrationSystem.Listener`: the game event listener a user's vibrations come through (by distance). */
	public static final class Listener implements GameEventListener {
		private final Data data;
		private final User user;

		public Listener(Data data, User user) {
			this.data = data;
			this.user = user;
		}

		@Override
		public double[] position(World world) {
			return user.position(world);
		}

		@Override
		public int radius() {
			return user.radius();
		}

		@Override
		public boolean byDistance() {
			return true;
		}

		@Override
		public boolean handle(World world, GameEvent event, GameEvent.Context context, double x, double y, double z) {
			if (data.current != null || !user.isValid(event, context)) {
				return false;
			}
			double[] destination = user.position(world);
			if (destination == null || !user.canReceive(world, MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z), event, context)
				|| occluded(world, x, y, z, destination)) {
				return false;
			}
			double dx = destination[0] - x, dy = destination[1] - y, dz = destination[2] - z;
			data.addCandidate(new Info(event, (float) Math.sqrt(dx * dx + dy * dy + dz * dz), x, y, z, context.source == null ? null : context.source.getUniqueID()),
				world.getTotalWorldTime());
			return true;
		}

		/**
		 * 26.3's `forceScheduleVibration`: a candidate from (x, y, z), past the checks (a step on a sculk sensor). It's
		 * chosen with the tick's others, as any.
		 */
		public void forceSchedule(World world, GameEvent event, Entity source, double x, double y, double z) {
			double[] destination = user.position(world);
			if (destination == null) {
				return;
			}
			double dx = destination[0] - x, dy = destination[1] - y, dz = destination[2] - z;
			data.addCandidate(new Info(event, (float) Math.sqrt(dx * dx + dy * dy + dz * dz), x, y, z, source == null ? null : source.getUniqueID()),
				world.getTotalWorldTime());
		}
	}

	/** 26.3's `areAdjacentChunksTicking`: the chunk of (x, z) and the eight round it loaded. */
	static boolean chunksLoadedAround(World world, int x, int z) {
		int cx = x >> 4, cz = z >> 4;
		return world.checkChunksExist(cx * 16 - 16, 0, cz * 16 - 16, cx * 16 + 31, 0, cz * 16 + 31);
	}

	/**
	 * 26.3's `isOccluded`: from the source block's middle to the listener block's middle, each of six lines nudged
	 * 1e-5 a face's way; occluded when every line meets wool.
	 */
	static boolean occluded(World world, double x, double y, double z, double[] destination) {
		double fx = MathHelper.floor_double(x) + 0.5D, fy = MathHelper.floor_double(y) + 0.5D, fz = MathHelper.floor_double(z) + 0.5D;
		double tx = MathHelper.floor_double(destination[0]) + 0.5D, ty = MathHelper.floor_double(destination[1]) + 0.5D,
			tz = MathHelper.floor_double(destination[2]) + 0.5D;
		double[][] nudges = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
		for (double[] nudge : nudges) {
			if (!lineMeetsWool(world, fx + nudge[0] * 1.0E-5D, fy + nudge[1] * 1.0E-5D, fz + nudge[2] * 1.0E-5D, tx, ty, tz)) {
				return false;
			}
		}
		return true;
	}

	/** A walk through the blocks a line crosses (26.3's `BlockGetter.traverseBlocks`), the last (the listener's) left out as 26.3's clip ends there. */
	private static boolean lineMeetsWool(World world, double x0, double y0, double z0, double x1, double y1, double z1) {
		int bx = MathHelper.floor_double(x0), by = MathHelper.floor_double(y0), bz = MathHelper.floor_double(z0);
		int ex = MathHelper.floor_double(x1), ey = MathHelper.floor_double(y1), ez = MathHelper.floor_double(z1);
		double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
		int stepX = dx > 0 ? 1 : dx < 0 ? -1 : 0, stepY = dy > 0 ? 1 : dy < 0 ? -1 : 0, stepZ = dz > 0 ? 1 : dz < 0 ? -1 : 0;
		double tDeltaX = stepX == 0 ? Double.MAX_VALUE : stepX / dx, tDeltaY = stepY == 0 ? Double.MAX_VALUE : stepY / dy,
			tDeltaZ = stepZ == 0 ? Double.MAX_VALUE : stepZ / dz;
		double tMaxX = stepX == 0 ? Double.MAX_VALUE : tDeltaX * (stepX > 0 ? 1.0D - (x0 - bx) : x0 - bx);
		double tMaxY = stepY == 0 ? Double.MAX_VALUE : tDeltaY * (stepY > 0 ? 1.0D - (y0 - by) : y0 - by);
		double tMaxZ = stepZ == 0 ? Double.MAX_VALUE : tDeltaZ * (stepZ > 0 ? 1.0D - (z0 - bz) : z0 - bz);
		for (int guard = 0; guard < 256; guard++) {
			if (occludes(world.getBlock(bx, by, bz))) {
				return true;
			}
			if (bx == ex && by == ey && bz == ez) {
				return false;
			}
			if (tMaxX < tMaxY && tMaxX < tMaxZ) {
				if (tMaxX > 1.0D) {
					return false;
				}
				bx += stepX;
				tMaxX += tDeltaX;
			} else if (tMaxY < tMaxZ) {
				if (tMaxY > 1.0D) {
					return false;
				}
				by += stepY;
				tMaxY += tDeltaY;
			} else {
				if (tMaxZ > 1.0D) {
					return false;
				}
				bz += stepZ;
				tMaxZ += tDeltaZ;
			}
		}
		return false;
	}

	/** 26.3's `VibrationSystem.Ticker.tick`, once a server tick by the listener's owner. */
	public static void tick(World world, Data data, User user) {
		if (!(world instanceof WorldServer server)) {
			return;
		}
		if (data.current == null) {
			Info chosen = data.chosen(world.getTotalWorldTime());
			if (chosen != null) {
				data.current = chosen;
				data.travelTime = user.travelTime(chosen.distance);
				sendParticle(server, chosen.x, chosen.y, chosen.z, user.position(world), data.travelTime);
				data.reloadParticle = false;
				user.onDataChanged();
				data.candidate = null;
			}
		}
		if (data.current == null) {
			return;
		}
		boolean changed = data.travelTime > 0;
		if (data.reloadParticle) {
			// a vibration read from a save: its particle again, from where it would be by now
			double[] to = user.position(world);
			int initial = Math.max(1, user.travelTime(data.current.distance));
			double alpha = 1.0D - (double) data.travelTime / initial;
			if (to != null) {
				sendParticle(server, data.current.x + (to[0] - data.current.x) * alpha, data.current.y + (to[1] - data.current.y) * alpha,
					data.current.z + (to[2] - data.current.z) * alpha, to, data.travelTime);
			}
			data.reloadParticle = false;
		}
		data.travelTime = Math.max(0, data.travelTime - 1);
		if (data.travelTime <= 0) {
			Info info = data.current;
			int ox = MathHelper.floor_double(info.x), oy = MathHelper.floor_double(info.y), oz = MathHelper.floor_double(info.z);
			double[] to = user.position(world);
			int tx = to == null ? ox : MathHelper.floor_double(to[0]), ty = to == null ? oy : MathHelper.floor_double(to[1]),
				tz = to == null ? oz : MathHelper.floor_double(to[2]);
			float blocks = (float) Math.sqrt((double) (tx - ox) * (tx - ox) + (double) (ty - oy) * (ty - oy) + (double) (tz - oz) * (tz - oz));
			if (!user.requiresAdjacentChunks() || chunksLoadedAround(world, tx, tz)) {
				user.onReceive(world, ox, oy, oz, info.event, info.entity(world), blocks);
				data.current = null;
				changed = true;
			}
		}
		if (changed) {
			user.onDataChanged();
		}
	}

	/**
	 * 26.3's vibration particle: from where the vibration is toward the listener (`minecraft:vibration`). The packet's
	 * speeds carry the way to go; the client times it by that way's length, as 26.3's travel time is the distance's floor.
	 */
	private static void sendParticle(WorldServer world, double x, double y, double z, double[] to, int ticks) {
		if (to == null || !McFlyParticles.exists(McFlyParticles.VIBRATION)) {
			return;
		}
		McFlyParticles.send(world, McFlyParticles.VIBRATION, false, false, x, y, z, 0, 1.0D, 1.0D, 1.0D, to[0] - x, to[1] - y, to[2] - z, McFlyParticles.DEFAULT);
	}

}
