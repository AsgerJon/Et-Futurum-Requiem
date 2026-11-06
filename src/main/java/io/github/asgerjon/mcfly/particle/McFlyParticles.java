package io.github.asgerjon.mcfly.particle;

import io.github.asgerjon.mcfly.network.McFlyNetwork;
import io.github.asgerjon.mcfly.network.ParticleMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Our particles by name, as 26.3 keeps them (SPEC-modern-movement.md §79, B7; 26.3's `ParticleTypes`): each name with
 * whether its type overrides the limiter (shown whatever the particle setting and however far away), and the server's
 * way to show one to the players near it (26.3's `ServerLevel.sendParticles`, its level particles packet on our
 * channel). The client's half, each name's particle, is {@link McFlyParticlesClient}.
 */
public final class McFlyParticles {

	public static final String SWEEP_ATTACK = "minecraft:sweep_attack";
	public static final String TOTEM_OF_UNDYING = "minecraft:totem_of_undying";
	public static final String SNOWFLAKE = "minecraft:snowflake";
	public static final String GLOW_SQUID_INK = "minecraft:glow_squid_ink";
	public static final String GLOW = "minecraft:glow";
	public static final String ELECTRIC_SPARK = "minecraft:electric_spark";
	public static final String DRAGON_BREATH = "minecraft:dragon_breath";
	public static final String END_ROD = "minecraft:end_rod";
	/** 26.3's falling leaves of a colour (`ColorParticleOption` on `tinted_leaves`): azalea's (spec §90, step D2). */
	public static final String TINTED_LEAVES = "minecraft:tinted_leaves";
	/** Cherry's falling petals (spec §90.6). */
	public static final String CHERRY_LEAVES = "minecraft:cherry_leaves";
	/** Pointed dripstone's drips (spec §90.7): hanging, then falling and landing, as one particle. */
	public static final String DRIPPING_DRIPSTONE_WATER = "minecraft:dripping_dripstone_water";
	public static final String DRIPPING_DRIPSTONE_LAVA = "minecraft:dripping_dripstone_lava";
	/** The spore blossom's (spec §90.8). */
	public static final String FALLING_SPORE_BLOSSOM = "minecraft:falling_spore_blossom";
	public static final String SPORE_BLOSSOM_AIR = "minecraft:spore_blossom_air";
	/** A vibration on its way (spec §90.9). */
	public static final String VIBRATION = "minecraft:vibration";

	/** 26.3's randomization kinds of the packet's spread (`ClientboundLevelParticlesPacket.RandomizationType`). */
	public static final int DEFAULT = 0;
	public static final int ALTERNATIVE = 1;
	public static final int ALTERNATIVE_WITH_SPEED = 2;

	private static final Map<String, Boolean> TYPES = new LinkedHashMap<>();

	static {
		// each with 26.3's own overrideLimiter (`ParticleTypes.register(name, overrideLimiter)`)
		TYPES.put(DRAGON_BREATH, false);
		TYPES.put(SWEEP_ATTACK, true);
		TYPES.put(TOTEM_OF_UNDYING, false);
		TYPES.put(SNOWFLAKE, false);
		TYPES.put(GLOW_SQUID_INK, true);
		TYPES.put(GLOW, true);
		TYPES.put(ELECTRIC_SPARK, true);
		// copper's (spec §87): honeycomb's wax, the axe's wax off and scrape
		TYPES.put(io.github.asgerjon.mcfly.copper.Weathering.WAX_ON, false);
		TYPES.put(io.github.asgerjon.mcfly.copper.Weathering.WAX_OFF, false);
		TYPES.put(io.github.asgerjon.mcfly.copper.Weathering.SCRAPE, false);
		// crying obsidian's tear (spec §88.3): its hanging stage, which falls and lands by itself
		TYPES.put(io.github.asgerjon.mcfly.nether.CryingObsidianBlock.TEAR, false);
		// the soul torch's flame (spec §88.5)
		TYPES.put(io.github.asgerjon.mcfly.nether.SoulTorchBlock.FLAME, false);
		// falling leaves (spec §90, step D2)
		TYPES.put(TINTED_LEAVES, false);
		TYPES.put(CHERRY_LEAVES, false);
		TYPES.put(DRIPPING_DRIPSTONE_WATER, false);
		TYPES.put(DRIPPING_DRIPSTONE_LAVA, false);
		TYPES.put(FALLING_SPORE_BLOSSOM, false);
		TYPES.put(SPORE_BLOSSOM_AIR, false);
		// 26.3's vibration overrides the limiter
		TYPES.put(VIBRATION, true);
		// a blooming sculk catalyst's souls (spec §90.9, step H3c)
		TYPES.put(io.github.asgerjon.mcfly.sculk.SculkParticles.SCULK_SOUL, false);
	}

	private McFlyParticles() {
	}

	/** Our particle names, with each type's own overrideLimiter. */
	public static Map<String, Boolean> types() {
		return Collections.unmodifiableMap(TYPES);
	}

	public static boolean exists(String name) {
		return TYPES.containsKey(name);
	}

	/** Whether this type is shown whatever the limiter says (26.3's `ParticleType.getOverrideLimiter`). */
	public static boolean overridesLimiter(String name) {
		Boolean override = TYPES.get(name);
		return override != null && override;
	}

	/** As 26.3's `sendParticles(particle, x, y, z, count, xDist, yDist, zDist, speed)`. */
	public static int send(WorldServer world, String name, double x, double y, double z, int count, double xDist, double yDist, double zDist, double speed) {
		return send(world, name, false, false, x, y, z, count, xDist, yDist, zDist, speed);
	}

	/** As 26.3's `sendParticles(particle, overrideLimiter, alwaysShow, x, y, z, count, xDist, yDist, zDist, speed)`. */
	public static int send(WorldServer world, String name, boolean overrideLimiter, boolean alwaysShow, double x, double y, double z, int count,
		double xDist, double yDist, double zDist, double speed) {
		return send(world, name, overrideLimiter, alwaysShow, x, y, z, count, xDist, yDist, zDist, speed, speed, speed, DEFAULT);
	}

	/**
	 * 26.3's whole `sendParticles`: the packet to each player in this world whose block's middle is within 32 blocks of
	 * the spot (512 when overrideLimiter is asked for); how many it went to.
	 */
	public static int send(WorldServer world, String name, boolean overrideLimiter, boolean alwaysShow, double x, double y, double z, int count,
		double xDist, double yDist, double zDist, double xSpeed, double ySpeed, double zSpeed, int randomization) {
		if (!exists(name)) {
			throw new IllegalArgumentException("no particle " + name);
		}
		ParticleMessage message = new ParticleMessage(name, overrideLimiter, alwaysShow, x, y, z, (float) xDist, (float) yDist, (float) zDist,
			(float) xSpeed, (float) ySpeed, (float) zSpeed, count, randomization);
		double range = overrideLimiter ? 512.0D : 32.0D;
		int sent = 0;
		for (Object entry : world.playerEntities) {
			EntityPlayerMP player = (EntityPlayerMP) entry;
			double dx = MathHelper.floor_double(player.posX) + 0.5D - x;
			double dy = MathHelper.floor_double(player.boundingBox.minY) + 0.5D - y;
			double dz = MathHelper.floor_double(player.posZ) + 0.5D - z;
			if (dx * dx + dy * dy + dz * dz < range * range) {
				McFlyNetwork.CHANNEL.sendTo(message, player);
				sent++;
			}
		}
		return sent;
	}
}
