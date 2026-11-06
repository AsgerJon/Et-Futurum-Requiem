package io.github.asgerjon.mcfly.registry;

import cpw.mods.fml.common.registry.EntityRegistry;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.entities.EntityAreaEffectCloud;
import ganymedes01.etfuturum.entities.EntityDragonFireball;
import ganymedes01.etfuturum.entities.EntityGlowItemFrame;
import ganymedes01.etfuturum.entities.EntityGlowSquid;
import ganymedes01.etfuturum.entities.EntityPhantom;
import ganymedes01.etfuturum.entities.EntityPolarBear;
import ganymedes01.etfuturum.entities.EntityShulker;
import ganymedes01.etfuturum.entities.EntityShulkerBullet;
import ganymedes01.etfuturum.entities.EntityThrownTrident;
import ganymedes01.etfuturum.entities.PhantomSpawning;
import ganymedes01.etfuturum.world.end.modern.ModernEnd;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Our entities (SPEC-modern-movement.md §79, B6), registered by McFly with FML under the names and mod entity ids EFR's
 * list gave them (in this fork the mod is EFR's: `etfuturum.polar_bear` and so on, which old saves hold), with spawn
 * eggs of our own.
 * <p>
 * A spawn egg's id is its item's damage in a save, and each egg takes the next free id from 500 in the order eggs are
 * made (EFR's list does the same). So the shulker's pair is registered at its old place in EFR's list, by
 * {@link #register(Object)}, and the rest after EFR's own, by {@link #registerAll(Object)}: the ids come out as before.
 */
public enum McFlyEntities {
	// the shulker and its bullet, rebuilt as 26.3's (spec §76.13); EFR's entries and option before
	SHULKER(EntityShulker.class, "shulker", 16, 80, 1, false, 0x946794, 0x4D3852, () -> ConfigEntities.enableShulker),
	SHULKER_BULLET(EntityShulkerBullet.class, "shulker_candy", 17, 64, 1, true, () -> ConfigEntities.enableShulker),
	// the thrown trident (spec §58)
	TRIDENT(EntityThrownTrident.class, "trident", 23, 64, 20, true, () -> McFlyItems.TRIDENT.isEnabled()),
	// the polar bear (spec §60): seen up to 10 chunks away as 26.3's
	POLAR_BEAR(EntityPolarBear.class, "polar_bear", 24, 160, 3, true, 0xF2F2F2, 0x959590, () -> McFlyConfig.enablePolarBears),
	// the phantom (spec §68.1): seen up to 8 chunks away as 26.3's
	PHANTOM(EntityPhantom.class, "phantom", 27, 128, 3, true, 0x43518A, 0x88FF00, () -> McFlyConfig.enablePhantoms),
	// the glow item frame (spec §67.4): tracked as 1.7.10 tracks frames (160 blocks, no movement updates)
	GLOW_ITEM_FRAME(EntityGlowItemFrame.class, "glow_item_frame", 26, 160, Integer.MAX_VALUE, false, () -> McFlyItems.GLOW_ITEM_FRAME.isEnabled()),
	// the glow squid (spec §67.1): seen up to 10 chunks away as 26.3's
	GLOW_SQUID(EntityGlowSquid.class, "glow_squid", 25, 160, 3, true, 0x095656, 0x85F1BC, () -> McFlyConfig.enableGlowSquids),
	// the End as 26.3 (spec §76.8): the dragon's breath
	// lingering potions thrown and tipped arrows shot (spec §82): EFR's names and ids
	THROWN_LINGERING_POTION(io.github.asgerjon.mcfly.potion.ThrownLingeringPotion.class, "lingering_potion", 6, 64, 10, true, () -> McFlyItems.LINGERING_POTION.isEnabled()),
	SHOT_TIPPED_ARROW(io.github.asgerjon.mcfly.potion.TippedArrow.class, "tipped_arrow", 2, 64, 20, true, () -> McFlyItems.TIPPED_ARROW.isEnabled()),
	// a firework rocket boosting a glider (spec §85), under EFR's id
	// EFR's old placed end crystal, read back as 26.3's crystal (spec §85)
	OLD_PLACED_CRYSTAL(io.github.asgerjon.mcfly.end.PlacedCrystal.class, "end_crystal", 9, 256, Integer.MAX_VALUE, false, () -> true),
	BOOSTING_ROCKET(io.github.asgerjon.mcfly.elytra.BoostingRocket.class, "boosting_firework_rocket", 20, 64, 1, true, () -> io.github.asgerjon.mcfly.config.McFlyConfig.enableElytra),
	AREA_EFFECT_CLOUD(EntityAreaEffectCloud.class, "area_effect_cloud", 28, 160, 10, false, ModernEnd::isActive),
	DRAGON_FIREBALL(EntityDragonFireball.class, "dragon_fireball", 29, 64, 10, true, ModernEnd::isActive),
	// a falling stalactite (spec §90.7), on the id EFR set aside for its unfinished one
	// the boats (spec 91.12, step J): EFR's names and ids, 26.3's 10 chunks and 3 ticks; no motion sent (a client steers)
	NEW_BOAT(io.github.asgerjon.mcfly.boat.McFlyBoat.class, "new_boat", 13, 160, 3, false, () -> io.github.asgerjon.mcfly.config.McFlyConfig.enableNewBoats),
	CHEST_BOAT(io.github.asgerjon.mcfly.boat.McFlyChestBoat.class, "chest_boat", 19, 160, 3, false, () -> io.github.asgerjon.mcfly.config.McFlyConfig.enableNewBoats),
	NEW_BOAT_SEAT(io.github.asgerjon.mcfly.boat.BoatSeat.class, "new_boat_seat", 14, 160, 3, false, () -> io.github.asgerjon.mcfly.config.McFlyConfig.enableNewBoats),
	FALLING_DRIPSTONE(io.github.asgerjon.mcfly.caves.DripstoneBlocks.Falling.class, "falling_dripstone", 18, 160, 20, true, () -> io.github.asgerjon.mcfly.config.McFlyConfig.enableDripstone);

	public static final McFlyEntities[] VALUES = values();

	private static int eggCounter = 499;
	private static final List<Integer> EGG_IDS = new ArrayList<>();
	private static final Map<Class<? extends Entity>, Integer> EGGS = new HashMap<>();

	private final Class<? extends Entity> type;
	private final String name;
	private final int id;
	private final int trackingRange;
	private final int updateFrequency;
	private final boolean sendsVelocityUpdates;
	private final int eggColour1;
	private final int eggColour2;
	private final boolean hasEgg;
	private final BooleanSupplier enabled;
	private boolean registered;

	McFlyEntities(Class<? extends Entity> type, String name, int id, int trackingRange, int updateFrequency, boolean sendsVelocityUpdates, BooleanSupplier enabled) {
		this(type, name, id, trackingRange, updateFrequency, sendsVelocityUpdates, -1, -1, false, enabled);
	}

	McFlyEntities(Class<? extends Entity> type, String name, int id, int trackingRange, int updateFrequency, boolean sendsVelocityUpdates, int eggColour1, int eggColour2, BooleanSupplier enabled) {
		this(type, name, id, trackingRange, updateFrequency, sendsVelocityUpdates, eggColour1, eggColour2, true, enabled);
	}

	McFlyEntities(Class<? extends Entity> type, String name, int id, int trackingRange, int updateFrequency, boolean sendsVelocityUpdates, int eggColour1, int eggColour2, boolean hasEgg, BooleanSupplier enabled) {
		this.type = type;
		this.name = name;
		this.id = id;
		this.trackingRange = trackingRange;
		this.updateFrequency = updateFrequency;
		this.sendsVelocityUpdates = sendsVelocityUpdates;
		this.eggColour1 = eggColour1;
		this.eggColour2 = eggColour2;
		this.hasEgg = hasEgg;
		this.enabled = enabled;
	}

	/** Registers this one for the mod given (its option read now), with its egg; once. */
	public void register(Object mod) {
		if (registered || !isEnabled()) {
			return;
		}
		registered = true;
		EntityRegistry.registerModEntity(type, name, id, mod, trackingRange, updateFrequency, sendsVelocityUpdates);
		if (hasEgg) {
			while (EntityList.getClassFromID(++eggCounter) != null) {
			}
			EntityList.IDtoClassMapping.put(eggCounter, type);
			EntityList.entityEggs.put(eggCounter, new EntityList.EntityEggInfo(eggCounter, eggColour1, eggColour2));
			EGG_IDS.add(eggCounter);
			EGGS.put(type, eggCounter);
		}
	}

	/** Every one not registered yet, then where ours spawn (their options read now). */
	public static void registerAll(Object mod) {
		for (McFlyEntities entity : VALUES) {
			entity.register(mod);
		}
		// the polar bear (spec §60): 26.3's snowy plains, ice spikes and frozen oceans
		if (POLAR_BEAR.registered && McFlyConfig.enablePolarBearSpawns) {
			List<BiomeGenBase> snowy = new ArrayList<>();
			for (BiomeGenBase biome : new BiomeGenBase[]{BiomeGenBase.icePlains, BiomeGenBase.getBiome(BiomeGenBase.icePlains.biomeID + 128),
				BiomeGenBase.iceMountains, BiomeGenBase.frozenOcean}) {
				if (biome != null) {
					snowy.add(biome);
				}
			}
			EntityRegistry.addSpawn(EntityPolarBear.class, 1, 1, 2, EnumCreatureType.creature, snowy.toArray(new BiomeGenBase[0]));
		}
		// the phantom (spec §68.3): spawned by its own spawner, not by biomes; the time since rest statistic turns it on
		if (PHANTOM.registered && McFlyConfig.enablePhantomSpawns) {
			PhantomSpawning.registerStat();
		}
		// the glow squid (spec §67.1): in every overworld biome's water list, at 26.3's cave spawn weight and groups,
		// kept to dark water at y 30 and below by GlowSquidSpawning (its mixin)
		if (GLOW_SQUID.registered && McFlyConfig.enableGlowSquidSpawns) {
			List<BiomeGenBase> overworld = new ArrayList<>();
			for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
				if (biome != null && !BiomeDictionary.isBiomeOfType(biome, Type.NETHER) && !BiomeDictionary.isBiomeOfType(biome, Type.END)) {
					overworld.add(biome);
				}
			}
			EntityRegistry.addSpawn(EntityGlowSquid.class, 10, 4, 6, EnumCreatureType.waterCreature, overworld.toArray(new BiomeGenBase[0]));
		}
	}

	/** Our spawn eggs' ids, in the order they were made. */
	public static List<Integer> eggIds() {
		return Collections.unmodifiableList(EGG_IDS);
	}

	/** The spawn egg of this entity's kind (for picking it in creative). */
	public static ItemStack eggFor(Entity entity) {
		Integer egg = EGGS.get(entity.getClass());
		return egg == null ? null : new ItemStack(Items.spawn_egg, 1, egg);
	}

	public boolean isEnabled() {
		return enabled.getAsBoolean();
	}

	public boolean isRegistered() {
		return registered;
	}

	public String entityName() {
		return name;
	}

	public Class<? extends Entity> entityClass() {
		return type;
	}

	public int id() {
		return id;
	}
}
