package ganymedes01.etfuturum.loot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 26.3's enchanting for loot (`EnchantmentHelper.enchantItem` with `#on_random_loot`; SPEC-modern-movement.md §76.12,
 * 7e), on 26.3's own data as its reference wrote it (`/assets/etfuturum/loot/enchanting_26_3.txt`): each item's
 * enchantability, and each enchantment of the tag in its order with its weight, its levels' costs, the items it's a
 * primary item of and those it excludes. It works on 26.3's items, so the draws are 26.3's whatever 1.7.10 has.
 */
public final class ModernEnchanting {

	private static final String DATA = "/assets/etfuturum/loot/enchanting_26_3.txt";

	private static final class Definition {
		String id;
		int maxLevel;
		int weight;
		int[] minCost;
		int[] maxCost;
		final Set<String> primary = new HashSet<>();
		final Set<String> exclusive = new HashSet<>();
	}

	private static final class Instance {
		final Definition enchantment;
		final int level;

		Instance(Definition enchantment, int level) {
			this.enchantment = enchantment;
			this.level = level;
		}
	}

	private static Map<String, Integer> enchantability;
	private static List<Definition> onRandomLoot;

	private ModernEnchanting() {
	}

	private static synchronized void load() {
		if (onRandomLoot != null) {
			return;
		}
		Map<String, Integer> items = new HashMap<>();
		List<Definition> definitions = new ArrayList<>();
		for (String line : ModernLoot.lines(DATA)) {
			String[] parts = line.split(" ");
			if (parts[0].equals("item")) {
				items.put(parts[1], Integer.parseInt(parts[2]));
			} else if (parts[0].equals("ench")) {
				Definition definition = new Definition();
				definition.id = parts[1];
				definition.maxLevel = Integer.parseInt(parts[2]);
				definition.weight = Integer.parseInt(parts[3]);
				String[] costs = parts[4].split(",");
				definition.minCost = new int[costs.length];
				definition.maxCost = new int[costs.length];
				for (int i = 0; i < costs.length; i++) {
					String[] range = costs[i].split("-");
					definition.minCost[i] = Integer.parseInt(range[0]);
					definition.maxCost[i] = Integer.parseInt(range[1]);
				}
				if (!parts[5].equals("-")) {
					definition.primary.addAll(Arrays.asList(parts[5].split(",")));
				}
				if (!parts[6].equals("-")) {
					definition.exclusive.addAll(Arrays.asList(parts[6].split(",")));
				}
				definitions.add(definition);
			}
		}
		enchantability = items;
		onRandomLoot = definitions;
	}

	/** 26.3's item stack size for the loot's items (tools, armour, saddles and horse armour are 1). */
	static int maxStackSize(String item) {
		load();
		if (enchantability.getOrDefault(item, 0) > 0 || item.endsWith("saddle") || item.endsWith("_horse_armor")) {
			return 1;
		}
		return 64;
	}

	/** 26.3's Enchantment.areCompatible. */
	private static boolean compatible(Definition a, Definition b) {
		return a != b && !a.exclusive.contains(b.id) && !b.exclusive.contains(a.id);
	}

	private static Instance weighted(Random random, List<Instance> list) {
		int total = 0;
		for (Instance instance : list) {
			total += instance.enchantment.weight;
		}
		if (total == 0) {
			return null;
		}
		int index = random.nextInt(total);
		for (Instance instance : list) {
			index -= instance.enchantment.weight;
			if (index < 0) {
				return instance;
			}
		}
		return null;
	}

	/** 26.3's enchantItem with `#on_random_loot`: the chosen enchantments put on the stack (a level kept if higher). */
	static void enchantItem(Random random, ModernLoot.Stack stack, int cost) {
		load();
		Integer value = enchantability.get(stack.item);
		if (value == null || value <= 0) {
			return;
		}
		cost += 1 + random.nextInt(value / 4 + 1) + random.nextInt(value / 4 + 1);
		float span = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
		cost = Math.max(Math.round(cost + cost * span), 1);
		List<Instance> available = new ArrayList<>();
		for (Definition definition : onRandomLoot) {
			if (!definition.primary.contains(stack.item)) {
				continue;
			}
			for (int level = definition.maxLevel; level >= 1; level--) {
				if (cost >= definition.minCost[level - 1] && cost <= definition.maxCost[level - 1]) {
					available.add(new Instance(definition, level));
					break;
				}
			}
		}
		List<Instance> results = new ArrayList<>();
		if (!available.isEmpty()) {
			Instance first = weighted(random, available);
			if (first != null) {
				results.add(first);
			}
			while (random.nextInt(50) <= cost) {
				if (!results.isEmpty()) {
					Instance last = results.get(results.size() - 1);
					available.removeIf(other -> !compatible(last.enchantment, other.enchantment));
				}
				if (available.isEmpty()) {
					break;
				}
				Instance next = weighted(random, available);
				if (next != null) {
					results.add(next);
				}
				cost /= 2;
			}
		}
		for (Instance instance : results) {
			stack.enchantments.merge(instance.enchantment.id, instance.level, Math::max);
		}
	}
}
