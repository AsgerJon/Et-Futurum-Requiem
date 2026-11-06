package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.world.World;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 26.3's recipes tried here, one line of a list made from its jar (`unittas_mcfly-sims/recipes_26_3.py`): name, kind
 * (`shaped`, `shapeless`, `smelting`), the pattern rows ('.' empty) and keys or the ingredients, `->`, the count and the
 * result, a smelting's experience. Items by 26.3's names (the command layer's); `a|b` is a tag's or a list's members,
 * each tried in turn.
 */
final class Recipes26_3 {

	private Recipes26_3() {
	}

	/** A list's lines (no comments, no blanks). */
	static List<String> lines(String resource) throws IOException {
		List<String> lines = new ArrayList<>();
		try (InputStream in = Recipes26_3.class.getResourceAsStream("/assets/etfuturum/unittas/" + resource)) {
			if (in == null) {
				throw new AssertionError("no " + resource);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			for (String line; (line = reader.readLine()) != null; ) {
				if (!line.startsWith("#") && !line.trim().isEmpty()) {
					lines.add(line);
				}
			}
		}
		return lines;
	}

	/** What's wrong with each line here (on the server thread), as "name: what"; empty when all are 26.3's. */
	static List<String> wrong(List<String> lines, World world) {
		List<String> found = new ArrayList<>();
		for (String line : lines) {
			String result = recipe(line, world);
			if (result != null) {
				found.add(result);
			}
		}
		return found;
	}

	/** The first 20 of them, and how many more. */
	static String shown(List<String> wrong) {
		return wrong.size() > 20 ? wrong.subList(0, 20) + " and " + (wrong.size() - 20) + " more" : wrong.toString();
	}

	/** One line tried here: null when it gives 26.3's result with every alternative, else what's wrong. */
	static String recipe(String line, World world) {
		String[] parts = line.split(" ");
		String name = parts[0];
		String kind = parts[1];
		int arrow = Arrays.asList(parts).indexOf("->");
		int count = Integer.parseInt(parts[arrow + 1]);
		ItemStack want;
		try {
			want = stack(parts[arrow + 2], count);
		} catch (AssertionError e) {
			return name + ": no " + parts[arrow + 2];
		}
		int alternatives = 1;
		for (int i = 2; i < arrow; i++) {
			alternatives = Math.max(alternatives, parts[i].split("\\|").length);
		}
		for (int alternative = 0; alternative < alternatives; alternative++) {
			String wrong = tryOne(name, kind, parts, arrow, want, alternative, world);
			if (wrong != null) {
				return wrong;
			}
		}
		return null;
	}

	private static String tryOne(String name, String kind, String[] parts, int arrow, ItemStack want, int alternative, World world) {
		ItemStack got;
		try {
			if ("smelting".equals(kind)) {
				got = FurnaceRecipes.smelting().getSmeltingResult(stack(pick(parts[2], alternative), 1));
				float experience = FurnaceRecipes.smelting().func_151398_b(want);
				if (got != null && Math.abs(experience - Float.parseFloat(parts[arrow + 3])) > 0.01F) {
					return name + ": experience " + experience;
				}
			} else {
				InventoryCrafting grid = new InventoryCrafting(new Container() {
					@Override
					public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
						return false;
					}
				}, 3, 3);
				if ("shaped".equals(kind)) {
					String[] rows = parts[2].split("/");
					Map<Character, String> keys = new HashMap<>();
					for (int i = 3; i < arrow; i++) {
						keys.put(parts[i].charAt(0), parts[i].substring(2));
					}
					for (int row = 0; row < rows.length; row++) {
						for (int col = 0; col < rows[row].length(); col++) {
							char c = rows[row].charAt(col);
							if (c != '.') {
								grid.setInventorySlotContents(row * 3 + col, stack(pick(keys.get(c), alternative), 1));
							}
						}
					}
				} else {
					for (int i = 2; i < arrow; i++) {
						grid.setInventorySlotContents(i - 2, stack(pick(parts[i], alternative), 1));
					}
				}
				got = CraftingManager.getInstance().findMatchingRecipe(grid, world);
			}
		} catch (AssertionError e) {
			return name + ": " + e.getMessage();
		}
		String with = alternative > 0 ? " (alternative " + alternative + ")" : "";
		if (got == null) {
			return name + with + ": nothing";
		}
		if (got.getItem() != want.getItem() || got.getItemDamage() != want.getItemDamage() || got.stackSize != want.stackSize) {
			return name + with + ": " + got.stackSize + " " + nameOf(got);
		}
		return null;
	}

	/** An ingredient's alternative: the last one when it has fewer. */
	private static String pick(String ingredient, int alternative) {
		String[] members = ingredient.split("\\|");
		return members[Math.min(alternative, members.length - 1)];
	}

	static ItemStack stack(String modern, int count) {
		ItemIds.Entry entry = ItemIds.byId("minecraft:" + modern);
		if (entry == null) {
			throw new AssertionError("no item for minecraft:" + modern);
		}
		return entry.createStack(count);
	}

	/** An item stack's 26.3 name, by the command layer's ids (the first that makes it). */
	static String nameOf(ItemStack stack) {
		for (String id : ItemIds.ids()) {
			ItemIds.Entry entry = ItemIds.byId(id);
			if (entry != null && entry.item() == stack.getItem() && entry.meta() == stack.getItemDamage() && id.startsWith("minecraft:")) {
				return id.substring("minecraft:".length());
			}
		}
		return Item.itemRegistry.getNameForObject(stack.getItem()) + ":" + stack.getItemDamage();
	}
}
