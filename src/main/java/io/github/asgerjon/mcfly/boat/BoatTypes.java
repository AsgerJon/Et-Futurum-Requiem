package io.github.asgerjon.mcfly.boat;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRecipes;
import net.minecraft.block.BlockDispenser;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The boats' woods (SPEC-modern-movement.md 91.12, step J): each one's domain and wood (a boat's `Domain` and `Type`),
 * whether it's a raft, its planks, its items (EFR's ids: `<wood>_boat`, `<wood>_chest_boat`, `mangrove_oak_boat`,
 * `bop_<wood>_boat`, `witchery_<wood>_boat`) and textures (`<domain>:textures/entity/boat/<wood>.png`,
 * `.../chest_boat/<wood>.png`).
 */
public final class BoatTypes {

	/** One wood's boats. */
	public static final class Type {
		public final String domain;
		public final String wood;
		public final boolean raft;
		private final Supplier<Item> planks;
		private final int planksMeta;
		private final BooleanSupplier enabled;
		private Item boat;
		private Item chestBoat;
		private final ResourceLocation texture;
		private final ResourceLocation chestTexture;

		Type(String domain, String wood, boolean raft, Supplier<Item> planks, int planksMeta, BooleanSupplier enabled) {
			this.domain = domain;
			this.wood = wood;
			this.raft = raft;
			this.planks = planks;
			this.planksMeta = planksMeta;
			this.enabled = enabled;
			this.texture = new ResourceLocation(domain, "textures/entity/boat/" + wood + ".png");
			this.chestTexture = new ResourceLocation(domain, "textures/entity/chest_boat/" + wood + ".png");
		}

		/** `domain:wood`, as a boat holds it. */
		public String key() {
			return domain + ":" + wood;
		}

		public boolean isEnabled() {
			return McFlyConfig.enableNewBoats && enabled.getAsBoolean();
		}

		/** Its plain boat's item: vanilla's boat for oak while `replaceOldBoats` is on. */
		public Item boatItem() {
			return this == OAK && McFlyConfig.replaceOldBoats ? Items.boat : boat;
		}

		public Item chestBoatItem() {
			return chestBoat;
		}

		public ResourceLocation texture(boolean chest) {
			return chest ? chestTexture : texture;
		}

		ItemStack planks() {
			Item item = planks.get();
			return item == null ? null : new ItemStack(item, 1, planksMeta);
		}
	}

	private static final Map<String, Type> BY_KEY = new LinkedHashMap<>();
	private static final List<Type> ALL = new ArrayList<>();

	public static final Type OAK = vanilla("oak", 0);
	public static final Type SPRUCE = vanilla("spruce", 1);
	public static final Type BIRCH = vanilla("birch", 2);
	public static final Type JUNGLE = vanilla("jungle", 3);
	public static final Type ACACIA = vanilla("acacia", 4);
	public static final Type DARK_OAK = vanilla("dark_oak", 5);
	public static final Type MANGROVE = add(new Type("minecraft", "mangrove", false, () -> McFlyBlocks.WOOD_PLANKS.getItem(), 2, () -> McFlyConfig.enableMangrove));
	public static final Type CHERRY = add(new Type("minecraft", "cherry", false, () -> McFlyBlocks.WOOD_PLANKS.getItem(), 3, () -> McFlyConfig.enableCherryBlocks));
	public static final Type BAMBOO = add(new Type("minecraft", "bamboo", true, () -> McFlyBlocks.WOOD_PLANKS.getItem(), 4, () -> McFlyConfig.enableBambooBlocks));

	static {
		String[] bop = {"sacredoak", "cherry", "dark", "fir", "ethereal", "magic", "mangrove", "palm", "redwood", "willow", "bamboo", "pine", "hellbark",
			"jacaranda", "mahogany"};
		for (int i = 0; i < bop.length; i++) {
			add(new Type("biomesoplenty", bop[i], bop[i].equals("bamboo"), () -> GameRegistry.findItem("BiomesOPlenty", "planks"), i,
				() -> Loader.isModLoaded("BiomesOPlenty")));
		}
		String[] witchery = {"rowan", "alder", "hawthorn"};
		for (int i = 0; i < witchery.length; i++) {
			add(new Type("witchery", witchery[i], false, () -> GameRegistry.findItem("witchery", "witchwood"), i, () -> Loader.isModLoaded("witchery")));
		}
	}

	private BoatTypes() {
	}

	private static Type vanilla(String wood, int planksMeta) {
		return add(new Type("minecraft", wood, false, () -> Item.getItemFromBlock(Blocks.planks), planksMeta, () -> true));
	}

	private static Type add(Type type) {
		BY_KEY.put(type.key(), type);
		ALL.add(type);
		return type;
	}

	public static List<Type> all() {
		return Collections.unmodifiableList(ALL);
	}

	/** By `domain:wood`; null when there's no such wood. */
	public static Type byKey(String key) {
		return BY_KEY.get(key);
	}

	public static Type of(String domain, String wood) {
		return BY_KEY.get(domain + ":" + wood);
	}

	/**
	 * A `McFlyItems` entry's item (made as the enum is): the domain and wood, the chest's. Its entry is on with
	 * `type.isEnabled()` (the plain oak boat also needs `replaceOldBoats` off).
	 */
	public static Item item(String domain, String wood, boolean chest) {
		Type type = of(domain, wood);
		BoatItem item = new BoatItem(type, chest);
		if (chest) {
			type.chestBoat = item;
		} else {
			type.boat = item;
		}
		return item;
	}

	/** Whether the entry for this wood's item is on. */
	public static boolean on(String domain, String wood) {
		return of(domain, wood).isEnabled();
	}

	/** In init, after the planks: recipes, ore names, dispensers, vanilla's boat as oak's (with `replaceOldBoats`). */
	public static void register() {
		if (!McFlyConfig.enableNewBoats) {
			return;
		}
		for (Type type : ALL) {
			if (!type.isEnabled()) {
				continue;
			}
			Item boat = type.boatItem();
			ItemStack planks = type.planks();
			// 26.3's: five planks, `# #` over `###`; a chest and the boat, shapeless (any wooden chest, as EFR's)
			if (planks != null && planks.getItem() != null && boat != null) {
				McFlyRecipes.shaped(new ItemStack(boat), "# #", "###", '#', planks);
			}
			if (type.chestBoat != null && boat != null) {
				McFlyRecipes.shapeless(new ItemStack(type.chestBoat), "chestWood", new ItemStack(boat));
			}
			if (boat != null) {
				OreDictionary.registerOre("boatWood", new ItemStack(boat));
				BlockDispenser.dispenseBehaviorRegistry.putObject(boat, BoatItem.DISPENSING);
			}
			if (type.chestBoat != null) {
				OreDictionary.registerOre("boatChestWood", new ItemStack(type.chestBoat));
				BlockDispenser.dispenseBehaviorRegistry.putObject(type.chestBoat, BoatItem.DISPENSING);
			}
		}
		if (McFlyConfig.replaceOldBoats) {
			// vanilla's boat is oak's: 26.3's name and icon
			Items.boat.setUnlocalizedName(McFly.name("oak_boat"));
			Items.boat.setTextureName("oak_boat");
		}
		// vanilla's boat: of any plankWood no wood's own recipe takes (after ours: with the option off, oak's planks make
		// oak's new boat)
		removeVanillaRecipeFor(Items.boat);
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Items.boat), "# #", "###", '#', "plankWood"));
	}

	@SuppressWarnings("unchecked")
	private static void removeVanillaRecipeFor(Item item) {
		Iterator<IRecipe> recipes = CraftingManager.getInstance().getRecipeList().iterator();
		while (recipes.hasNext()) {
			IRecipe recipe = recipes.next();
			ItemStack made = recipe.getRecipeOutput();
			if (recipe instanceof ShapedRecipes && made != null && made.getItem() == item) {
				recipes.remove();
				return;
			}
		}
	}
}
