package ganymedes01.etfuturum.command.modern.arguments;

import ganymedes01.etfuturum.entities.EntityNewBoat;
import ganymedes01.etfuturum.entities.EntityNewBoatWithChest;
import ganymedes01.etfuturum.items.ItemNewBoat;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Modern's entity type ids for 1.7.10's entities (spec §43.7), worked out from the entity itself, since some of
 * modern's types are variants of one 1.7.10 class. Built on first use, when every mod has registered its entities.
 */
public final class EntityTypeIds {

	public static final String PLAYER = "minecraft:player";

	/** 1.7.10's registered names and modern's types for them; variants are added by {@link #idOf}. */
	private static final String[][] VANILLA = {
		{"Item", "item"}, {"XPOrb", "experience_orb"}, {"LeashKnot", "leash_knot"}, {"Painting", "painting"},
		{"Arrow", "arrow"}, {"Snowball", "snowball"}, {"Fireball", "fireball"}, {"SmallFireball", "small_fireball"},
		{"ThrownEnderpearl", "ender_pearl"}, {"EyeOfEnderSignal", "eye_of_ender"}, {"ThrownPotion", "splash_potion"},
		{"ThrownExpBottle", "experience_bottle"}, {"ItemFrame", "item_frame"}, {"WitherSkull", "wither_skull"},
		{"PrimedTnt", "tnt"}, {"FallingSand", "falling_block"}, {"FireworksRocketEntity", "firework_rocket"},
		{"Boat", "oak_boat"}, {"MinecartRideable", "minecart"}, {"MinecartChest", "chest_minecart"},
		{"MinecartFurnace", "furnace_minecart"}, {"MinecartTNT", "tnt_minecart"}, {"MinecartHopper", "hopper_minecart"},
		{"MinecartSpawner", "spawner_minecart"}, {"MinecartCommandBlock", "command_block_minecart"},
		{"Creeper", "creeper"}, {"Skeleton", "skeleton"}, {"Spider", "spider"}, {"Giant", "giant"},
		{"Zombie", "zombie"}, {"Slime", "slime"}, {"Ghast", "ghast"}, {"PigZombie", "zombified_piglin"},
		{"Enderman", "enderman"}, {"CaveSpider", "cave_spider"}, {"Silverfish", "silverfish"}, {"Blaze", "blaze"},
		{"LavaSlime", "magma_cube"}, {"EnderDragon", "ender_dragon"}, {"WitherBoss", "wither"}, {"Bat", "bat"},
		{"Witch", "witch"}, {"Pig", "pig"}, {"Sheep", "sheep"}, {"Cow", "cow"}, {"Chicken", "chicken"},
		{"Squid", "squid"}, {"Wolf", "wolf"}, {"MushroomCow", "mooshroom"}, {"SnowMan", "snow_golem"},
		{"Ozelot", "ocelot"}, {"VillagerGolem", "iron_golem"}, {"EntityHorse", "horse"}, {"Villager", "villager"},
		{"EnderCrystal", "end_crystal"},
	};

	/** EFR's registered names (after `etfuturum.`) and modern's types; its boats go by their wood. */
	private static final String[][] EFR = {
		{"rabbit", "rabbit"}, {"wooden_armorstand", "armor_stand"}, {"endermite", "endermite"}, {"tipped_arrow", "arrow"},
		{"husk", "husk"}, {"stray", "stray"}, {"lingering_potion", "lingering_potion"},
		{"lingering_effect", "area_effect_cloud"}, {"villager_zombie", "zombie_villager"}, {"end_crystal", "end_crystal"},
		{"ender_dragon", "ender_dragon"}, {"snow_golem", "snow_golem"}, {"brown_mooshroom", "mooshroom"},
		{"fireproof_item", "item"}, {"shulker", "shulker"}, {"shulker_candy", "shulker_bullet"},
		{"boosting_firework_rocket", "firework_rocket"}, {"bee", "bee"}, {"fox", "fox"}, {"trident", "trident"},
		{"polar_bear", "polar_bear"}, {"glow_squid", "glow_squid"}, {"phantom", "phantom"},
	};

	/** Abstract bases and helpers that have no modern type. */
	private static final Set<String> NO_TYPE = new TreeSet<>(Arrays.asList("Mob", "Monster", Reference.MOD_ID + ".new_boat_seat"));
	private static final String EFR_BOAT = Reference.MOD_ID + ".new_boat";
	private static final String EFR_CHEST_BOAT = Reference.MOD_ID + ".chest_boat";
	private static final String[] HORSES = {"horse", "donkey", "mule", "zombie_horse", "skeleton_horse"};

	/** Modern types EFR has its own mob for, used by /summon over vanilla's (spec §47.3). */
	private static final Set<String> EFR_PREFERRED = new TreeSet<>(Arrays.asList("minecraft:snow_golem", "minecraft:zombie_villager"));
	private static final Set<String> NOT_SUMMONABLE = new TreeSet<>(Arrays.asList(PLAYER, "minecraft:fishing_bobber"));

	private static Map<String, String> byName;
	/** Modern type to 1.7.10's registered name, for /summon. */
	private static Map<String, String> nameById;
	/** Set last, so a thread that sees it sees the names too. */
	private static volatile Set<String> ids;

	private EntityTypeIds() {
	}

	/** Every id, sorted. */
	public static Collection<String> ids() {
		build();
		return Collections.unmodifiableSet(ids);
	}

	public static boolean exists(String id) {
		build();
		return ids.contains(id);
	}

	/** Every type /summon takes: all but players and fishing bobbers. */
	public static Collection<String> summonableIds() {
		build();
		Set<String> summonable = new TreeSet<>(ids);
		summonable.removeAll(NOT_SUMMONABLE);
		return summonable;
	}

	public static boolean isSummonable(String id) {
		return exists(id) && !NOT_SUMMONABLE.contains(id);
	}

	/** How 1.7.10 makes an entity of a modern type: its registered name, the data that picks the variant, and a last touch. */
	public static final class Spawn {
		/** Null for lightning, which 1.7.10 doesn't register. */
		public final String name;
		public final NBTTagCompound variant;
		public final Consumer<Entity> after;

		private Spawn(String name, NBTTagCompound variant, Consumer<Entity> after) {
			this.name = name;
			this.variant = variant;
			this.after = after;
		}
	}

	/** The 1.7.10 entity for a modern type (spec §47.3); null for one with none. */
	public static Spawn spawnFor(String id, Random random) {
		build();
		NBTTagCompound variant = new NBTTagCompound();
		Consumer<Entity> nothing = entity -> {
		};
		switch (id) {
			case "minecraft:lightning_bolt":
				return new Spawn(null, variant, nothing);
			case "minecraft:wither_skeleton":
				variant.setByte("SkeletonType", (byte) 1);
				return new Spawn("Skeleton", variant, nothing);
			case "minecraft:zombie_villager":
				if (nameById.containsKey(id) && !"Zombie".equals(nameById.get(id))) {
					return new Spawn(nameById.get(id), variant, nothing);
				}
				variant.setBoolean("IsVillager", true);
				return new Spawn("Zombie", variant, nothing);
			case "minecraft:cat":
				variant.setInteger("CatType", 1 + random.nextInt(3));
				return new Spawn("Ozelot", variant, nothing);
			default:
				break;
		}
		for (int i = 0; i < HORSES.length; i++) {
			if (id.equals("minecraft:" + HORSES[i])) {
				variant.setInteger("Type", i);
				return new Spawn("EntityHorse", variant, nothing);
			}
		}
		if (EntityList.stringToClassMapping.containsKey(EFR_BOAT) && (id.endsWith("_boat") || id.endsWith("_raft"))) {
			int colon = id.indexOf(':');
			String domain = id.substring(0, colon);
			String path = id.substring(colon + 1);
			boolean chest = path.endsWith("_chest_boat") || path.endsWith("_chest_raft");
			String wood = path.substring(0, path.length() - (chest ? "_chest_boat".length() : "_boat".length()));
			return new Spawn(chest ? EFR_CHEST_BOAT : EFR_BOAT, variant, entity -> ((EntityNewBoat) entity).setBoatType(domain, wood));
		}
		String name = nameById.get(id);
		return name == null ? null : new Spawn(name, variant, nothing);
	}

	/**
	 * What a mob summoned without data gets (spec §47.3): 1.7.10's spawn treatment, except where that would undo the
	 * variant or do more than modern's. A horse keeps its type (1.7.10 rolls a new one); a wither skeleton gets a stone
	 * sword and no bow or armour (1.7.10 arms it so only in the Nether); ocelots and cats get only what every mob gets
	 * (1.7.10's ocelot brings two kittens one time in seven).
	 */
	public static void giveSpawnTreatment(String id, EntityLiving living) {
		if (living instanceof EntityHorse horse && horse.getHorseType() != 0) {
			living.onSpawnWithEgg(new EntityHorse.GroupData(horse.getHorseType(), horse.getHorseVariant()));
		} else if (living instanceof EntityOcelot) {
			living.getEntityAttribute(SharedMonsterAttributes.followRange)
				.applyModifier(new AttributeModifier("Random spawn bonus", living.getRNG().nextGaussian() * 0.05D, 1));
		} else {
			living.onSpawnWithEgg(null);
			if ("minecraft:wither_skeleton".equals(id)) {
				for (int slot = 1; slot <= 4; slot++) {
					living.setCurrentItemOrArmor(slot, null);
				}
				// the skeleton turns to melee once it holds no bow
				living.setCurrentItemOrArmor(0, new ItemStack(Items.stone_sword));
				living.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(4.0D);
			}
		}
	}

	/** The modern type of a 1.7.10 registered name (its plain variant), or null for one that has none. */
	public static String idOfName(String registeredName) {
		build();
		return registeredName == null ? null : byName.get(registeredName);
	}

	/** The entity's modern type, or null for one that has none. */
	public static String idOf(Entity entity) {
		build();
		String name = (String) EntityList.classToStringMapping.get(entity.getClass());
		if (name == null) {
			if (entity instanceof EntityPlayer) {
				return PLAYER;
			}
			if (entity instanceof EntityFishHook) {
				return "minecraft:fishing_bobber";
			}
			return entity instanceof EntityLightningBolt ? "minecraft:lightning_bolt" : null;
		}
		switch (name) {
			case "Skeleton":
				return ((EntitySkeleton) entity).getSkeletonType() == 1 ? "minecraft:wither_skeleton" : "minecraft:skeleton";
			case "Zombie":
				return ((EntityZombie) entity).isVillager() ? "minecraft:zombie_villager" : "minecraft:zombie";
			case "EntityHorse":
				int horseType = ((EntityHorse) entity).getHorseType();
				return "minecraft:" + HORSES[horseType >= 0 && horseType < HORSES.length ? horseType : 0];
			case "Ozelot":
				// by the skin: taming gives one, and /summon cat gives one to a wild ocelot
				return ((EntityOcelot) entity).getTameSkin() != 0 ? "minecraft:cat" : "minecraft:ocelot";
			default:
				if (entity instanceof EntityNewBoat boat && (name.equals(EFR_BOAT) || name.equals(EFR_CHEST_BOAT))) {
					return boatId(boat.getResourceLocation().getResourceDomain(), boat.getBoatType(), boat instanceof EntityNewBoatWithChest, boat.isRaft());
				}
				return byName.get(name);
		}
	}

	private static String boatId(String domain, String wood, boolean chest, boolean raft) {
		return domain + ":" + wood + (chest ? "_chest" : "") + (raft ? "_raft" : "_boat");
	}

	private static synchronized void build() {
		if (ids != null) {
			return;
		}
		Map<String, String> names = new HashMap<>();
		for (String[] pair : VANILLA) {
			names.put(pair[0], "minecraft:" + pair[1]);
		}
		for (String[] pair : EFR) {
			names.put(Reference.MOD_ID + "." + pair[0], "minecraft:" + pair[1]);
		}
		for (Object registered : EntityList.stringToClassMapping.keySet()) {
			String name = (String) registered;
			if (!names.containsKey(name) && !NO_TYPE.contains(name) && !name.equals(EFR_BOAT) && !name.equals(EFR_CHEST_BOAT)) {
				names.put(name, fromRegisteredName(name));
			}
		}
		names.keySet().retainAll(EntityList.stringToClassMapping.keySet());

		Set<String> all = new TreeSet<>(names.values());
		all.add(PLAYER);
		all.add("minecraft:fishing_bobber");
		all.add("minecraft:lightning_bolt");
		all.add("minecraft:wither_skeleton");
		all.add("minecraft:zombie_villager");
		all.add("minecraft:cat");
		for (String horse : HORSES) {
			all.add("minecraft:" + horse);
		}
		if (EntityList.stringToClassMapping.containsKey(EFR_BOAT)) {
			for (Map.Entry<String, ItemNewBoat.BoatInfo> boat : ItemNewBoat.BOAT_INFO.entrySet()) {
				String key = boat.getKey();
				boolean chest = key.endsWith("_chest");
				String wood = chest ? key.substring(0, key.length() - "_chest".length()) : key;
				int colon = wood.indexOf(':');
				all.add(boatId(wood.substring(0, colon), wood.substring(colon + 1), chest, boat.getValue().isRaft()));
			}
		}
		Map<String, String> reverse = new HashMap<>();
		for (int pass = 0; pass < 3; pass++) {
			for (Map.Entry<String, String> entry : names.entrySet()) {
				String name = entry.getKey();
				int kind = name.indexOf('.') < 0 ? 0 : name.startsWith(Reference.MOD_ID + ".") ? 1 : 2;
				if (kind == pass && (!reverse.containsKey(entry.getValue()) || kind == 1 && EFR_PREFERRED.contains(entry.getValue()))) {
					reverse.put(entry.getValue(), name);
				}
			}
		}
		nameById = reverse;
		byName = names;
		ids = all;
	}

	/** Another mod's entity: `modid.SomeName` is `modid:some_name`; a name without a mod is `minecraft:`. */
	private static String fromRegisteredName(String name) {
		int dot = name.indexOf('.');
		String namespace = dot > 0 ? clean(name.substring(0, dot).toLowerCase(Locale.ROOT)) : "minecraft";
		String path = name.substring(dot + 1);
		StringBuilder snake = new StringBuilder();
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (Character.isUpperCase(c) && i > 0) {
				char before = path.charAt(i - 1);
				boolean wordStart = Character.isLowerCase(before) || Character.isDigit(before)
					|| Character.isUpperCase(before) && i + 1 < path.length() && Character.isLowerCase(path.charAt(i + 1));
				if (wordStart) {
					snake.append('_');
				}
			}
			snake.append(Character.toLowerCase(c));
		}
		String cleanPath = clean(snake.toString());
		return namespace + ":" + (cleanPath.isEmpty() ? "entity" : cleanPath);
	}

	private static String clean(String text) {
		StringBuilder clean = new StringBuilder();
		for (char c : text.toCharArray()) {
			clean.append(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-' || c == '/' ? c : '_');
		}
		return clean.toString();
	}
}
