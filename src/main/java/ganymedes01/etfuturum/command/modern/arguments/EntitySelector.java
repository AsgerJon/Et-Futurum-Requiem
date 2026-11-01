package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/** Which entities a parsed target names, found when the command runs (spec §41.2, §43.4). */
public final class EntitySelector {

	public enum Order {
		ARBITRARY, NEAREST, FURTHEST, RANDOM
	}

	private final int maxResults;
	private final boolean includesEntities;
	private final boolean worldLimited;
	private final List<Predicate<Entity>> predicates;
	private final MinMaxBounds.Doubles distance;
	private final Double x;
	private final Double y;
	private final Double z;
	/** Relative to the origin; null without dx, dy or dz. */
	private final AxisAlignedBB box;
	private final Order order;
	private final boolean currentEntity;
	private final String playerName;
	private final UUID uuid;
	private final boolean usesSelector;

	EntitySelector(int maxResults, boolean includesEntities, boolean worldLimited, List<Predicate<Entity>> predicates,
				   MinMaxBounds.Doubles distance, Double x, Double y, Double z, AxisAlignedBB box, Order order,
				   boolean currentEntity, String playerName, UUID uuid, boolean usesSelector) {
		this.maxResults = maxResults;
		this.includesEntities = includesEntities;
		this.worldLimited = worldLimited;
		this.predicates = predicates;
		this.distance = distance;
		this.x = x;
		this.y = y;
		this.z = z;
		this.box = box;
		this.order = order;
		this.currentEntity = currentEntity;
		this.playerName = playerName;
		this.uuid = uuid;
		this.usesSelector = usesSelector;
	}

	public int maxResults() {
		return maxResults;
	}

	public boolean includesEntities() {
		return includesEntities;
	}

	public boolean isSelfSelector() {
		return currentEntity;
	}

	public List<Entity> findEntities(ModernCommandSource source) throws CommandSyntaxException {
		checkPermissions(source);
		if (!includesEntities) {
			return new ArrayList<>(findPlayers(source));
		}
		if (playerName != null) {
			EntityPlayerMP player = source.server().getConfigurationManager().func_152612_a(playerName);
			return player == null ? new ArrayList<>() : new ArrayList<>(Collections.singletonList(player));
		}
		if (uuid != null) {
			for (WorldServer world : source.server().worldServers) {
				for (Object loaded : world.loadedEntityList) {
					if (uuid.equals(((Entity) loaded).getUniqueID())) {
						return new ArrayList<>(Collections.singletonList((Entity) loaded));
					}
				}
			}
			return new ArrayList<>();
		}
		Vec3 origin = origin(source);
		Predicate<Entity> test = test(origin);
		if (currentEntity) {
			Entity self = source.entity();
			return self != null && test.test(self) ? new ArrayList<>(Collections.singletonList(self)) : new ArrayList<>();
		}
		List<Entity> result = new ArrayList<>();
		int limit = order == Order.ARBITRARY ? maxResults : Integer.MAX_VALUE;
		for (WorldServer world : worlds(source)) {
			for (Object loaded : world.loadedEntityList) {
				Entity entity = (Entity) loaded;
				if (test.test(entity)) {
					result.add(entity);
					if (result.size() >= limit) {
						return result;
					}
				}
			}
		}
		return sortAndLimit(origin, result);
	}

	@SuppressWarnings("unchecked")
	public List<EntityPlayerMP> findPlayers(ModernCommandSource source) throws CommandSyntaxException {
		checkPermissions(source);
		List<EntityPlayerMP> players = (List<EntityPlayerMP>) source.server().getConfigurationManager().playerEntityList;
		if (playerName != null) {
			EntityPlayerMP player = source.server().getConfigurationManager().func_152612_a(playerName);
			return player == null ? new ArrayList<>() : new ArrayList<>(Collections.singletonList(player));
		}
		if (uuid != null) {
			for (EntityPlayerMP player : players) {
				if (uuid.equals(player.getUniqueID())) {
					return new ArrayList<>(Collections.singletonList(player));
				}
			}
			return new ArrayList<>();
		}
		Vec3 origin = origin(source);
		Predicate<Entity> test = test(origin);
		if (currentEntity) {
			return source.entity() instanceof EntityPlayerMP self && test.test(self) ? new ArrayList<>(Collections.singletonList(self)) : new ArrayList<>();
		}
		if (worldLimited) {
			players = (List<EntityPlayerMP>) (List<?>) source.world().playerEntities;
		}
		List<EntityPlayerMP> result = new ArrayList<>();
		int limit = order == Order.ARBITRARY ? maxResults : Integer.MAX_VALUE;
		for (EntityPlayerMP player : players) {
			if (test.test(player)) {
				result.add(player);
				if (result.size() >= limit) {
					return result;
				}
			}
		}
		return sortAndLimit(origin, result);
	}

	/** The source's position with x, y and z put in. */
	private Vec3 origin(ModernCommandSource source) {
		Vec3 position = source.position();
		return Vec3.createVectorHelper(x != null ? x : position.xCoord, y != null ? y : position.yCoord, z != null ? z : position.zCoord);
	}

	private List<WorldServer> worlds(ModernCommandSource source) {
		return worldLimited ? Collections.singletonList(source.world()) : Arrays.asList(source.server().worldServers);
	}

	/** Every option's test, plus the box and the distance from the origin. */
	private Predicate<Entity> test(Vec3 origin) {
		List<Predicate<Entity>> all = new ArrayList<>(predicates);
		if (box != null) {
			AxisAlignedBB area = box.getOffsetBoundingBox(origin.xCoord, origin.yCoord, origin.zCoord);
			all.add(e -> area.intersectsWith(e.boundingBox));
		}
		if (distance != null) {
			all.add(e -> distance.matchesSqr(ModernCommandSource.positionOf(e).squareDistanceTo(origin)));
		}
		return e -> {
			for (Predicate<Entity> predicate : all) {
				if (!predicate.test(e)) {
					return false;
				}
			}
			return true;
		};
	}

	/** Nearest and furthest compare coordinates even across worlds, as modern does. */
	private <T extends Entity> List<T> sortAndLimit(Vec3 origin, List<T> result) {
		if (result.size() > 1) {
			switch (order) {
				case NEAREST -> result.sort((a, b) -> Double.compare(distanceSq(a, origin), distanceSq(b, origin)));
				case FURTHEST -> result.sort((a, b) -> Double.compare(distanceSq(b, origin), distanceSq(a, origin)));
				case RANDOM -> Collections.shuffle(result);
				case ARBITRARY -> {
				}
			}
		}
		return new ArrayList<>(result.subList(0, Math.min(maxResults, result.size())));
	}

	private static double distanceSq(Entity entity, Vec3 origin) {
		return ModernCommandSource.positionOf(entity).squareDistanceTo(origin);
	}

	private void checkPermissions(ModernCommandSource source) throws CommandSyntaxException {
		if (usesSelector && !source.allowsSelectors()) {
			throw EntitySelectorParser.SELECTORS_NOT_ALLOWED.create();
		}
	}
}
