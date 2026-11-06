package io.github.asgerjon.mcfly.registry;

import io.github.asgerjon.mcfly.base.McFlyItem;
import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.items.ItemBannerPattern;
import ganymedes01.etfuturum.items.ItemDriedKelp;
import ganymedes01.etfuturum.items.ItemGlowItemFrame;
import ganymedes01.etfuturum.items.ItemNetheriteUpgradeTemplate;
import ganymedes01.etfuturum.items.ItemPowderSnowBucket;
import ganymedes01.etfuturum.items.ItemSpyglass;
import ganymedes01.etfuturum.items.equipment.ItemShield;
import ganymedes01.etfuturum.items.equipment.ItemTrident;
import ganymedes01.etfuturum.items.equipment.ItemTurtleShell;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Our items (SPEC-modern-movement.md §79, B6), registered by McFly in pre-init, each one that's on under its entry's
 * name in lower case: in this fork the mod being loaded is EFR's, so the ids are the ones EFR's list gave them
 * (`etfuturum:trident` and so on), which old saves hold. The same API as EFR's list had for them.
 */
public enum McFlyItems {
	// kelp (the aquatic update)
	DRIED_KELP(McFlyConfig.enableKelp, new ItemDriedKelp()),
	// the netherite upgrade template; EFR's netherite and smithing table must be on
	NETHERITE_UPGRADE_SMITHING_TEMPLATE(McFlyConfig.enableNetherite && McFlyConfig.enableSmithingTable && McFlyConfig.netheriteUpgradeTemplate, new ItemNetheriteUpgradeTemplate()),
	// the combat update (spec §46) and the trident (§58)
	SHIELD(McFlyConfig.enableShield && McFlyConfig.modernCombat, new ItemShield()),
	TRIDENT(McFlyConfig.enableTrident && McFlyConfig.modernCombat, new ItemTrident()),
	// powder snow (spec §62)
	POWDER_SNOW_BUCKET(McFlyConfig.enablePowderSnow, new ItemPowderSnowBucket()),
	// the spyglass (spec §64.5); EFR's copper and amethyst must be on
	SPYGLASS(McFlyConfig.enableSpyglass && McFlyConfig.enableCopper && McFlyConfig.enableAmethyst, new ItemSpyglass()),
	// the amethyst shard (spec §90.4), EFR's id
	AMETHYST_SHARD(McFlyConfig.enableAmethyst, new io.github.asgerjon.mcfly.base.McFlyItem("amethyst_shard")),
	// glow berries (spec §90.3), EFR's id
	GLOW_BERRIES_ITEM(McFlyConfig.enableGlowBerries, new io.github.asgerjon.mcfly.caves.CaveVines.Berries((io.github.asgerjon.mcfly.caves.CaveVines.Head) McFlyBlocks.CAVE_VINE.get(),
		(io.github.asgerjon.mcfly.caves.CaveVines.Body) McFlyBlocks.CAVE_VINE_PLANT.get())),
	// copper (spec §87): the ingot; the raw ores (copper, iron, gold) under EFR's one id
	COPPER_INGOT(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.base.McFlyItem("copper_ingot")),
	// netherite (spec §88.4), under EFR's ids
	NETHERITE_SCRAP(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteItems.Metal("netherite_scrap", false)),
	NETHERITE_INGOT(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteItems.Metal("netherite_ingot", true)),
	NETHERITE_HELMET(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteItems.Armor(0)),
	NETHERITE_CHESTPLATE(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteItems.Armor(1)),
	NETHERITE_LEGGINGS(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteItems.Armor(2)),
	NETHERITE_BOOTS(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteItems.Armor(3)),
	NETHERITE_PICKAXE(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteItems.Pickaxe()),
	NETHERITE_SPADE(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteItems.Shovel()),
	NETHERITE_AXE(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteItems.Axe()),
	NETHERITE_HOE(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteItems.Hoe()),
	NETHERITE_SWORD(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteItems.Sword()),
	RAW_ORE(McFlyConfig.enableRawOres, new io.github.asgerjon.mcfly.copper.RawOreItem()),
	// glow ink (spec §67) and the phantom's membrane (§68)
	GLOW_INK_SAC(McFlyConfig.enableGlowInkSac, new McFlyItem("glow_ink_sac")),
	PHANTOM_MEMBRANE(McFlyConfig.enablePhantoms, new McFlyItem("phantom_membrane")),
	// the last potion recipes' reagents (spec §69.13), to be had in creative before their mobs come
	BREEZE_ROD(McFlyConfig.enableBreezeRod, new McFlyItem("breeze_rod")),
	// 26.3's banner pattern items with recipes, for the loom (spec §66.6)
	FLOWER_BANNER_PATTERN(McFlyConfig.enableBannerPatterns, new ItemBannerPattern("flower_banner_pattern", "flower", EnumRarity.common)),
	CREEPER_BANNER_PATTERN(McFlyConfig.enableBannerPatterns, new ItemBannerPattern("creeper_banner_pattern", "creeper", EnumRarity.uncommon)),
	SKULL_BANNER_PATTERN(McFlyConfig.enableBannerPatterns, new ItemBannerPattern("skull_banner_pattern", "skull", EnumRarity.rare)),
	MOJANG_BANNER_PATTERN(McFlyConfig.enableBannerPatterns, new ItemBannerPattern("mojang_banner_pattern", "mojang", EnumRarity.rare)),
	FIELD_MASONED_BANNER_PATTERN(McFlyConfig.enableBannerPatterns, new ItemBannerPattern("field_masoned_banner_pattern", "bricks", EnumRarity.common)),
	BORDURE_INDENTED_BANNER_PATTERN(McFlyConfig.enableBannerPatterns, new ItemBannerPattern("bordure_indented_banner_pattern", "curly_border", EnumRarity.common)),
	// spec §66.11: no recipe, creative and /give for now (§66.4)
	GLOBE_BANNER_PATTERN(McFlyConfig.enableBannerPatterns, new ItemBannerPattern("globe_banner_pattern", "globe", EnumRarity.common)),
	PIGLIN_BANNER_PATTERN(McFlyConfig.enableBannerPatterns, new ItemBannerPattern("piglin_banner_pattern", "piglin", EnumRarity.uncommon)),
	FLOW_BANNER_PATTERN(McFlyConfig.enableBannerPatterns, new ItemBannerPattern("flow_banner_pattern", "flow", EnumRarity.rare)),
	GUSTER_BANNER_PATTERN(McFlyConfig.enableBannerPatterns, new ItemBannerPattern("guster_banner_pattern", "guster", EnumRarity.rare)),
	// the turtle shell (the aquatic update)
	// the shulker's shell (spec §81)
	SHULKER_SHELL(McFlyConfig.enableShulkerBoxes, new McFlyItem("shulker_shell")),
	// lingering potions and tipped arrows (spec §82), under EFR's ids
	LINGERING_POTION(McFlyConfig.enableLingeringPotions, new io.github.asgerjon.mcfly.potion.LingeringPotionItem()),
	TIPPED_ARROW(McFlyConfig.enableTippedArrows, new io.github.asgerjon.mcfly.potion.TippedArrowItem()),
	// chorus fruit and popped chorus fruit (spec §84), under EFR's ids
	CHORUS_FRUIT(McFlyConfig.enableChorusBlocks, new io.github.asgerjon.mcfly.end.ChorusFruitItem()),
	CHORUS_FRUIT_POPPED(McFlyConfig.enableChorusBlocks, new McFlyItem("popped_chorus_fruit")),
	// the elytra (spec §85), under EFR's id
	ELYTRA(McFlyConfig.enableElytra, new io.github.asgerjon.mcfly.elytra.ElytraItem()),
	// the end crystal and dragon's breath (spec §85), under EFR's ids; 26.3 always has the crystal
	END_CRYSTAL(true, new io.github.asgerjon.mcfly.end.EndCrystalItem()),
	DRAGON_BREATH(McFlyConfig.enableLingeringPotions, new io.github.asgerjon.mcfly.end.DragonBreathItem()),
	TURTLE_SCUTE(McFlyConfig.enableTurtleShell, new McFlyItem("turtle_scute")),
	TURTLE_HELMET(McFlyConfig.enableTurtleShell, new ItemTurtleShell()),
	// bamboo's item (spec 91.11, step I), EFR's id
	BAMBOO(McFlyConfig.enableBambooBlocks, new io.github.asgerjon.mcfly.wood.Bamboo.BambooItem()),
	// the vanilla woods' sign items (spec 91.8, step F), EFR's ids
	ITEM_SIGN_SPRUCE(McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldItem(0)),
	ITEM_SIGN_BIRCH(McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldItem(1)),
	ITEM_SIGN_JUNGLE(McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldItem(2)),
	ITEM_SIGN_ACACIA(McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldItem(3)),
	ITEM_SIGN_DARK_OAK(McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldItem(4)),
	// the boats (spec 91.12, step J), EFR's ids; vanilla's boat is oak's with replaceOldBoats
	OAK_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "oak") && !io.github.asgerjon.mcfly.config.McFlyConfig.replaceOldBoats, io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "oak", false)),
	OAK_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "oak"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "oak", true)),
	SPRUCE_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "spruce"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "spruce", false)),
	SPRUCE_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "spruce"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "spruce", true)),
	BIRCH_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "birch"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "birch", false)),
	BIRCH_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "birch"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "birch", true)),
	JUNGLE_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "jungle"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "jungle", false)),
	JUNGLE_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "jungle"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "jungle", true)),
	ACACIA_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "acacia"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "acacia", false)),
	ACACIA_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "acacia"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "acacia", true)),
	DARK_OAK_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "dark_oak"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "dark_oak", false)),
	DARK_OAK_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "dark_oak"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "dark_oak", true)),
	MANGROVE_OAK_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "mangrove"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "mangrove", false)),
	MANGROVE_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "mangrove"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "mangrove", true)),
	CHERRY_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "cherry"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "cherry", false)),
	CHERRY_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "cherry"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "cherry", true)),
	BAMBOO_RAFT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "bamboo"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "bamboo", false)),
	BAMBOO_CHEST_RAFT(io.github.asgerjon.mcfly.boat.BoatTypes.on("minecraft", "bamboo"), io.github.asgerjon.mcfly.boat.BoatTypes.item("minecraft", "bamboo", true)),
	BOP_SACREDOAK_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "sacredoak"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "sacredoak", false)),
	BOP_SACREDOAK_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "sacredoak"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "sacredoak", true)),
	BOP_CHERRY_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "cherry"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "cherry", false)),
	BOP_CHERRY_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "cherry"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "cherry", true)),
	BOP_DARK_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "dark"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "dark", false)),
	BOP_DARK_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "dark"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "dark", true)),
	BOP_FIR_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "fir"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "fir", false)),
	BOP_FIR_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "fir"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "fir", true)),
	BOP_ETHEREAL_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "ethereal"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "ethereal", false)),
	BOP_ETHEREAL_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "ethereal"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "ethereal", true)),
	BOP_MAGIC_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "magic"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "magic", false)),
	BOP_MAGIC_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "magic"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "magic", true)),
	BOP_MANGROVE_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "mangrove"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "mangrove", false)),
	BOP_MANGROVE_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "mangrove"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "mangrove", true)),
	BOP_PALM_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "palm"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "palm", false)),
	BOP_PALM_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "palm"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "palm", true)),
	BOP_REDWOOD_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "redwood"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "redwood", false)),
	BOP_REDWOOD_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "redwood"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "redwood", true)),
	BOP_WILLOW_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "willow"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "willow", false)),
	BOP_WILLOW_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "willow"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "willow", true)),
	BOP_BAMBOO_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "bamboo"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "bamboo", false)),
	BOP_BAMBOO_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "bamboo"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "bamboo", true)),
	BOP_PINE_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "pine"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "pine", false)),
	BOP_PINE_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "pine"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "pine", true)),
	BOP_HELLBARK_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "hellbark"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "hellbark", false)),
	BOP_HELLBARK_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "hellbark"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "hellbark", true)),
	BOP_JACARANDA_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "jacaranda"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "jacaranda", false)),
	BOP_JACARANDA_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "jacaranda"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "jacaranda", true)),
	BOP_MAHOGANY_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "mahogany"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "mahogany", false)),
	BOP_MAHOGANY_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("biomesoplenty", "mahogany"), io.github.asgerjon.mcfly.boat.BoatTypes.item("biomesoplenty", "mahogany", true)),
	WITCHERY_ROWAN_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("witchery", "rowan"), io.github.asgerjon.mcfly.boat.BoatTypes.item("witchery", "rowan", false)),
	WITCHERY_ROWAN_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("witchery", "rowan"), io.github.asgerjon.mcfly.boat.BoatTypes.item("witchery", "rowan", true)),
	WITCHERY_ALDER_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("witchery", "alder"), io.github.asgerjon.mcfly.boat.BoatTypes.item("witchery", "alder", false)),
	WITCHERY_ALDER_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("witchery", "alder"), io.github.asgerjon.mcfly.boat.BoatTypes.item("witchery", "alder", true)),
	WITCHERY_HAWTHORN_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("witchery", "hawthorn"), io.github.asgerjon.mcfly.boat.BoatTypes.item("witchery", "hawthorn", false)),
	WITCHERY_HAWTHORN_CHEST_BOAT(io.github.asgerjon.mcfly.boat.BoatTypes.on("witchery", "hawthorn"), io.github.asgerjon.mcfly.boat.BoatTypes.item("witchery", "hawthorn", true)),
	// the glow item frame (spec §67.4)
	GLOW_ITEM_FRAME(McFlyConfig.enableGlowItemFrames && McFlyConfig.enableGlowInkSac, new ItemGlowItemFrame());

	public static final McFlyItems[] VALUES = values();

	private final boolean enabled;
	private final Item item;

	McFlyItems(boolean enabled, Item item) {
		this.enabled = enabled;
		this.item = item;
	}

	/** In pre-init: each one that's on, under its entry's name. */
	public static void register() {
		for (McFlyItems entry : VALUES) {
			if (entry.enabled) {
				GameRegistry.registerItem(entry.item, entry.name().toLowerCase());
			}
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Item get() {
		return item;
	}

	public ItemStack newItemStack() {
		return newItemStack(1);
	}

	public ItemStack newItemStack(int count) {
		return newItemStack(count, 0);
	}

	public ItemStack newItemStack(int count, int meta) {
		return new ItemStack(item, count, meta);
	}
}
