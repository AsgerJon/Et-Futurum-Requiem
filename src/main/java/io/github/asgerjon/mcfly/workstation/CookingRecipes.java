package io.github.asgerjon.mcfly.workstation;

import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.oredict.OreDictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The smoker's and blast furnace's recipes (SPEC-modern-movement.md 92.3, step A): 26.3's from its jar
 * (`assets/etfuturum/recipes/cooking_26_3.txt`, by the command layer's item ids), then, with EFR's options
 * (`enableAutoAddSmoker`, `enableAutoAddBlastFurnace`), the furnace's own that 26.3's lists don't name: its food to the
 * smoker, its ores and metals (EFR's ore-name rule) to the blast furnace, with the furnace's experience. Those are made
 * as they're asked for, so other mods' furnace recipes count whenever they were added.
 */
public final class CookingRecipes {

	public static final String SMOKING = "smoking";
	public static final String BLASTING = "blasting";

	private static final Pattern RAW_ORE = Pattern.compile("^raw[A-Z].*");

	/** One recipe: its id (26.3's, or an auto one's), its input (any damage of a damageable item), result, experience. */
	public static final class Recipe {
		public final String id;
		private final Item input;
		private final int inputMeta;
		private final ItemStack result;
		public final float experience;

		Recipe(String id, Item input, int inputMeta, ItemStack result, float experience) {
			this.id = id;
			this.input = input;
			this.inputMeta = inputMeta;
			this.result = result;
			this.experience = experience;
		}

		boolean matches(ItemStack stack) {
			return stack != null && stack.getItem() == input
				&& (inputMeta == OreDictionary.WILDCARD_VALUE || input.isDamageable() || stack.getItemDamage() == inputMeta);
		}

		public ItemStack result() {
			return result.copy();
		}
	}

	private static Map<String, List<Recipe>> tables;

	private CookingRecipes() {
	}

	/** The kind's recipe for this input: 26.3's, else an auto one; null for none. */
	public static Recipe find(String kind, ItemStack input) {
		if (input == null) {
			return null;
		}
		for (Recipe recipe : tables().get(kind)) {
			if (recipe.matches(input)) {
				return recipe;
			}
		}
		return auto(kind, input);
	}

	/** Its result for the input (a copy), or null. */
	public static ItemStack result(String kind, ItemStack input) {
		Recipe recipe = find(kind, input);
		return recipe == null ? null : recipe.result();
	}

	/** Its experience for the input, 0 for none. */
	public static float experience(String kind, ItemStack input) {
		Recipe recipe = find(kind, input);
		return recipe == null ? 0.0F : recipe.experience;
	}

	/** By the id a block entity kept: one of 26.3's, or an auto one by the input in its id; null when it's gone. */
	public static Recipe byId(String kind, String id) {
		for (Recipe recipe : tables().get(kind)) {
			if (recipe.id.equals(id)) {
				return recipe;
			}
		}
		String prefix = autoPrefix(kind);
		int slash = id.lastIndexOf('/');
		if (id.startsWith(prefix) && slash > prefix.length()) {
			Object item = Item.itemRegistry.getObject(id.substring(prefix.length(), slash));
			try {
				int meta = Integer.parseInt(id.substring(slash + 1));
				return item instanceof Item ? auto(kind, new ItemStack((Item) item, 1, meta)) : null;
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static String autoPrefix(String kind) {
		return McFly.DOMAIN + ":auto_" + kind + "/";
	}

	/** EFR's rules over the furnace's list (`SmokerRecipes.canAdd`, `BlastFurnaceRecipes.canAdd`), with its option on. */
	private static Recipe auto(String kind, ItemStack input) {
		boolean on = SMOKING.equals(kind) ? McFlyConfig.enableAutoAddSmoker : McFlyConfig.enableAutoAddBlastFurnace;
		if (!on) {
			return null;
		}
		ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(input);
		if (result == null || result.getItem() == null) {
			return null;
		}
		if (SMOKING.equals(kind) ? !isFood(result) : !isOreOrMetal(input, result)) {
			return null;
		}
		String id = autoPrefix(kind) + Item.itemRegistry.getNameForObject(input.getItem()) + "/" + input.getItemDamage();
		return new Recipe(id, input.getItem(), input.getItemDamage(), result.copy(), FurnaceRecipes.smelting().func_151398_b(result));
	}

	private static boolean isFood(ItemStack result) {
		return result.getItem() instanceof ItemFood food && food.func_150905_g(result) > 0;
	}

	private static boolean isOreOrMetal(ItemStack input, ItemStack result) {
		for (int id : OreDictionary.getOreIDs(input)) {
			String name = OreDictionary.getOreName(id);
			if (name != null && (name.startsWith("ore") || RAW_ORE.matcher(name).matches() || name.startsWith("cluster") || name.startsWith("crushed"))) {
				return true;
			}
		}
		for (int id : OreDictionary.getOreIDs(result)) {
			String name = OreDictionary.getOreName(id);
			if (name != null && (name.startsWith("ingot") || name.startsWith("chunkGt") || name.startsWith("nugget") || name.equalsIgnoreCase("quicksilver"))) {
				return true;
			}
		}
		return false;
	}

	private static synchronized Map<String, List<Recipe>> tables() {
		if (tables == null) {
			tables = load();
		}
		return tables;
	}

	/** 26.3's lines (`name kind a|b -> count result experience`); a member the fork lacks is left out. */
	private static Map<String, List<Recipe>> load() {
		Map<String, List<Recipe>> made = new HashMap<>();
		made.put(SMOKING, new ArrayList<>());
		made.put(BLASTING, new ArrayList<>());
		try (InputStream in = CookingRecipes.class.getResourceAsStream("/assets/etfuturum/recipes/cooking_26_3.txt")) {
			if (in == null) {
				McFly.LOG.error("No cooking recipes (cooking_26_3.txt)");
				return made;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			for (String line; (line = reader.readLine()) != null; ) {
				if (line.startsWith("#") || line.trim().isEmpty()) {
					continue;
				}
				String[] parts = line.split(" ");
				List<Recipe> table = made.get(parts[1]);
				ItemIds.Entry result = ItemIds.byId("minecraft:" + parts[5]);
				if (table == null || result == null) {
					McFly.LOG.warn("Cooking recipe {} left out: no {}", parts[0], parts[5]);
					continue;
				}
				for (String member : parts[2].split("\\|")) {
					ItemIds.Entry input = ItemIds.byId("minecraft:" + member);
					if (input == null) {
						McFly.LOG.warn("Cooking recipe {}: no {}", parts[0], member);
						continue;
					}
					table.add(new Recipe("minecraft:" + parts[0], input.item(), input.meta(), result.createStack(Integer.parseInt(parts[4])), Float.parseFloat(parts[6])));
				}
			}
		} catch (IOException e) {
			McFly.LOG.error("Reading the cooking recipes", e);
		}
		return made;
	}
}
