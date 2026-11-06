package ganymedes01.etfuturum;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.compat.ModsList;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.configuration.configs.ConfigExperiments;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.configuration.configs.ConfigModCompat;
import ganymedes01.etfuturum.core.utils.Utils;

import ganymedes01.etfuturum.items.BaseFood;
import ganymedes01.etfuturum.items.BaseItem;
import ganymedes01.etfuturum.items.BaseSubtypesItem;
import ganymedes01.etfuturum.items.DebugTestItem;
import ganymedes01.etfuturum.items.ItemArmorStand;
import ganymedes01.etfuturum.items.ItemCoopersMallet;
import ganymedes01.etfuturum.items.ItemBarrelUpgrade;
import ganymedes01.etfuturum.items.ItemBeetrootSeeds;
import ganymedes01.etfuturum.items.ItemBeetrootSoup;
import ganymedes01.etfuturum.items.ItemEtFuturumRecord;
import ganymedes01.etfuturum.items.ItemHoneyBottle;
import ganymedes01.etfuturum.items.ItemRabbitStew;
import ganymedes01.etfuturum.items.ItemSuspiciousStew;
import ganymedes01.etfuturum.items.ItemSweetBerries;
import ganymedes01.etfuturum.items.rawore.modded.BaseRawOre;
import ganymedes01.etfuturum.items.rawore.modded.ItemGeneralModdedRawOre;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public enum ModItems {
	MUTTON_RAW(ConfigBlocksItems.enableMutton, new BaseFood(2, 0.3F, true).setNames("mutton")),
	MUTTON_COOKED(ConfigBlocksItems.enableMutton, new BaseFood(6, 0.8F, true).setNames("cooked_mutton")),
	PRISMARINE_SHARD(ConfigBlocksItems.enablePrismarine, new BaseItem("prismarine_shard")),
	PRISMARINE_CRYSTALS(ConfigBlocksItems.enablePrismarine, new BaseItem("prismarine_crystals")),
	WOODEN_ARMORSTAND(ConfigBlocksItems.enableArmourStand, new ItemArmorStand()),
	RABBIT_RAW(ConfigEntities.enableRabbit, new BaseFood(3, 0.3F, true).setNames("rabbit")),
	RABBIT_COOKED(ConfigEntities.enableRabbit, new BaseFood(5, 0.6F, true).setNames("cooked_rabbit")),
	RABBIT_FOOT(ConfigEntities.enableRabbit, new BaseItem("rabbit_foot").setPotionEffect("+0+1-2+3&4-4+13")),
	RABBIT_HIDE(ConfigEntities.enableRabbit, new BaseItem("rabbit_hide")),
	RABBIT_STEW(ConfigEntities.enableRabbit, new ItemRabbitStew()),
	BEETROOT(ConfigBlocksItems.enableBeetroot, new BaseFood(1, 0.6F, false).setNames("beetroot")),
	BEETROOT_SEEDS(ConfigBlocksItems.enableBeetroot, new ItemBeetrootSeeds()),
	BEETROOT_SOUP(ConfigBlocksItems.enableBeetroot, new ItemBeetrootSoup()),
	NUGGET_IRON(ConfigBlocksItems.enableIronNugget, new BaseItem("iron_nugget")),
	//modded_raw_ore(true, new ItemRawOre(true)),
	TOTEM_OF_UNDYING(ConfigBlocksItems.enableTotemUndying, new BaseItem("totem_of_undying").setMaxStackSize(1)),
	DYE(ConfigBlocksItems.enableNewDyes, new BaseSubtypesItem("white_dye", "blue_dye", "brown_dye", "black_dye").setNames("dye")),
	SUSPICIOUS_STEW(ConfigBlocksItems.enableSuspiciousStew, new ItemSuspiciousStew()),
	SWEET_BERRIES(ConfigBlocksItems.enableSweetBerryBushes, new ItemSweetBerries()),
	PIGSTEP_RECORD(ConfigBlocksItems.enablePigstep, new ItemEtFuturumRecord("pigstep")),
	OTHERSIDE_RECORD(ConfigBlocksItems.enableOtherside, new ItemEtFuturumRecord("otherside")),
	PRECIPICE_RECORD(ConfigBlocksItems.enablePrecipice, new ItemEtFuturumRecord("precipice")),
	CREATOR_MUSIC_BOX_RECORD(ConfigBlocksItems.enableCreatorMusicBox, new ItemEtFuturumRecord("creator_music_box")),
	CREATOR_RECORD(ConfigBlocksItems.enableCreator, new ItemEtFuturumRecord("creator")),
	TEARS_RECORD(ConfigBlocksItems.enableTears, new ItemEtFuturumRecord("tears")),
	LAVA_CHICKEN_RECORD(ConfigBlocksItems.enableLavaChicken, new ItemEtFuturumRecord("lava_chicken")),
	FIVE_RECORD(ConfigBlocksItems.enable5, new ItemEtFuturumRecord("5")),
	DISC_FRAGMENT_5(ConfigBlocksItems.enable5, new BaseItem("disc_fragment_5", true)),
	BARREL_UPGRADE(ModsList.IRON_CHEST.isLoaded() && ConfigModCompat.barrelIronChest, new ItemBarrelUpgrade()),
	COOPERS_MALLET(ConfigBlocksItems.enableBarrel && ConfigBlocksItems.enableCoopersMallet, new ItemCoopersMallet()),
	HONEYCOMB(ConfigBlocksItems.enableHoney, new BaseItem("honeycomb")),
	HONEY_BOTTLE(ConfigBlocksItems.enableHoney, new ItemHoneyBottle()),

	//Mod Support
	MODDED_RAW_ORE(Utils.enableModdedRawOres(), new ItemGeneralModdedRawOre("raw_aluminum", "raw_tin", "raw_silver", "raw_lead", "raw_nickel", "raw_platinum", "raw_mythril",
			"raw_uranium", "raw_thorium", "raw_tungsten", "raw_titanium", "raw_zinc", "raw_magnesium", "raw_boron")),
	RAW_ADAMANTIUM(Utils.enableModdedRawOres(ModsList.SIMPLEORES), new BaseRawOre("simpleores", "adamantium")),

	//Debug Item
	DEBUGGING_TOOL(Reference.DEV_ENVIRONMENT, new DebugTestItem());


	/*
	 * Stand-in static final fields because some mods incorrectly referenced my code directly.
	 * They should be using GameRegistry.findItem but it is what it is I guess.
	 */

	//D-Mod
	@Deprecated
	public static final Item sweet_berries = SWEET_BERRIES.get();

	public static final ModItems[] VALUES = values();

	public static void init() {
		for (ModItems item : VALUES) {
			if (item.isEnabled()) { //Honestly what do you think it's doing lmfao
				GameRegistry.registerItem(item.get(), item.name().toLowerCase());
			}
		}
	}

	final private boolean isEnabled;
	final private Item theItem;

	ModItems(boolean enabled, Item item) {
		isEnabled = enabled;
		theItem = item;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public Item get() {
		return theItem;
	}

	public ItemStack newItemStack() {
		return newItemStack(1);
	}

	public ItemStack newItemStack(int count) {
		return newItemStack(count, 0);
	}

	public ItemStack newItemStack(int count, int meta) {
		return new ItemStack(this.get(), count, meta);
	}
}
