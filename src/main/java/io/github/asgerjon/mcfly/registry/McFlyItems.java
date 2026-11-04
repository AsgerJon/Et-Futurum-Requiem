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
	NETHERITE_UPGRADE_SMITHING_TEMPLATE(ConfigBlocksItems.enableNetherite && ConfigBlocksItems.enableSmithingTable && McFlyConfig.netheriteUpgradeTemplate, new ItemNetheriteUpgradeTemplate()),
	// the combat update (spec §46) and the trident (§58)
	SHIELD(McFlyConfig.enableShield && McFlyConfig.modernCombat, new ItemShield()),
	TRIDENT(McFlyConfig.enableTrident && McFlyConfig.modernCombat, new ItemTrident()),
	// powder snow (spec §62)
	POWDER_SNOW_BUCKET(McFlyConfig.enablePowderSnow, new ItemPowderSnowBucket()),
	// the spyglass (spec §64.5); EFR's copper and amethyst must be on
	SPYGLASS(McFlyConfig.enableSpyglass && ConfigBlocksItems.enableCopper && ConfigBlocksItems.enableAmethyst, new ItemSpyglass()),
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
	TURTLE_SCUTE(McFlyConfig.enableTurtleShell, new McFlyItem("turtle_scute")),
	TURTLE_HELMET(McFlyConfig.enableTurtleShell, new ItemTurtleShell()),
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
