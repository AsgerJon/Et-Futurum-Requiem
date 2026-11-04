package ganymedes01.etfuturum.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ganymedes01.etfuturum.ModEnchantments;
import ganymedes01.etfuturum.ModItems;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 26.3's loot tables, the parts its end city chest uses (SPEC-modern-movement.md §76.12, 7e; the start of §71): pools
 * with their rolls, item and empty entries by weight, `set_count` and `enchant_with_levels` (26.3's enchanting, from
 * its own data: `enchanting_26_3.txt`), and `LootTable.fill` (the slots shuffled, stacks split, the items shuffled).
 * A roll is made in 26.3's items (so a draw for an item 1.7.10 lacks still happens), then made 1.7.10's: an item or
 * enchantment with none here left out.
 */
public final class ModernLoot {

	private static final Logger LOG = LogManager.getLogger("etfuturum");
	private static final Map<String, ModernLoot> TABLES = new HashMap<>();

	/** One of 26.3's stacks: its item, count and enchantments (in the order they were added). */
	public static final class Stack {
		public final String item;
		public int count;
		public final LinkedHashMap<String, Integer> enchantments = new LinkedHashMap<>();

		Stack(String item, int count) {
			this.item = item;
			this.count = count;
		}

		Stack split(int amount) {
			Stack part = new Stack(item, amount);
			part.enchantments.putAll(enchantments);
			count -= amount;
			return part;
		}
	}

	private interface Number {
		int getInt(Random random);
	}

	private interface Function {
		void run(Stack stack, Random random);
	}

	private static final class Entry {
		String item;
		int weight = 1;
		int quality;
		final List<Function> functions = new ArrayList<>();

		int weight(float luck) {
			return Math.max((int) Math.floor(weight + quality * luck), 0);
		}
	}

	private static final class Pool {
		Number rolls;
		final List<Entry> entries = new ArrayList<>();
	}

	private final List<Pool> pools = new ArrayList<>();

	private ModernLoot() {
	}

	/** A table by 26.3's name ("minecraft:chests/end_city_treasure": /data/loot_table/chests/end_city_treasure.json). */
	public static synchronized ModernLoot table(String name) {
		return TABLES.computeIfAbsent(name, ModernLoot::load);
	}

	private static ModernLoot load(String name) {
		String path = "/data/loot_table/" + name.substring(name.indexOf(':') + 1) + ".json";
		ModernLoot table = new ModernLoot();
		try (InputStream in = ModernLoot.class.getResourceAsStream(path)) {
			if (in == null) {
				LOG.warn("No loot table {}", path);
				return table;
			}
			JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement poolElement : root.getAsJsonArray("pools")) {
				JsonObject poolJson = poolElement.getAsJsonObject();
				Pool pool = new Pool();
				pool.rolls = number(poolJson.get("rolls"));
				for (JsonElement entryElement : poolJson.getAsJsonArray("entries")) {
					JsonObject entryJson = entryElement.getAsJsonObject();
					Entry entry = new Entry();
					String type = entryJson.get("type").getAsString();
					entry.item = type.endsWith("item") ? entryJson.get("name").getAsString() : null;
					if (entryJson.has("weight")) {
						entry.weight = entryJson.get("weight").getAsInt();
					}
					if (entryJson.has("quality")) {
						entry.quality = entryJson.get("quality").getAsInt();
					}
					for (String key : new String[] {"modifier", "functions"}) {
						if (entryJson.has(key)) {
							JsonElement functions = entryJson.get(key);
							if (functions.isJsonArray()) {
								for (JsonElement function : functions.getAsJsonArray()) {
									entry.functions.add(function(function.getAsJsonObject()));
								}
							} else {
								entry.functions.add(function(functions.getAsJsonObject()));
							}
						}
					}
					pool.entries.add(entry);
				}
				table.pools.add(pool);
			}
		} catch (Exception e) {
			LOG.warn("Can't read loot table {}", path, e);
		}
		return table;
	}

	/** 26.3's Mth.nextInt. */
	private static int nextInt(Random random, int min, int max) {
		return min >= max ? min : random.nextInt(max - min + 1) + min;
	}

	private static Number number(JsonElement json) {
		if (json.isJsonPrimitive()) {
			int value = json.getAsInt();
			return random -> value;
		}
		JsonObject object = json.getAsJsonObject();
		String type = object.has("type") ? object.get("type").getAsString() : "minecraft:constant";
		if (type.endsWith("uniform")) {
			Number min = number(object.get("min"));
			Number max = number(object.get("max"));
			return random -> nextInt(random, min.getInt(random), max.getInt(random));
		}
		int value = object.get("value").getAsInt();
		return random -> value;
	}

	private static Function function(JsonObject json) {
		String type = json.get("type").getAsString();
		if (type.endsWith("set_count")) {
			Number count = number(json.get("count"));
			boolean add = json.has("add") && json.get("add").getAsBoolean();
			return (stack, random) -> stack.count = (add ? stack.count : 0) + count.getInt(random);
		}
		if (type.endsWith("enchant_with_levels")) {
			Number levels = number(json.get("levels"));
			return (stack, random) -> ModernEnchanting.enchantItem(random, stack, levels.getInt(random));
		}
		LOG.warn("Loot function {} isn't known: left out", type);
		return (stack, random) -> {
		};
	}

	/** 26.3's getRandomItems: every pool's rolls, each a weighted entry with its functions (stacks split to their size). */
	private List<Stack> randomItems(Random random, float luck) {
		List<Stack> items = new ArrayList<>();
		for (Pool pool : pools) {
			int rolls = pool.rolls.getInt(random);
			for (int i = 0; i < rolls; i++) {
				List<Entry> valid = new ArrayList<>();
				int total = 0;
				for (Entry entry : pool.entries) {
					int weight = entry.weight(luck);
					if (weight > 0) {
						valid.add(entry);
						total += weight;
					}
				}
				if (total == 0 || valid.isEmpty()) {
					continue;
				}
				Entry chosen = null;
				if (valid.size() == 1) {
					chosen = valid.get(0);
				} else {
					int index = random.nextInt(total);
					for (Entry entry : valid) {
						index -= entry.weight(luck);
						if (index < 0) {
							chosen = entry;
							break;
						}
					}
				}
				if (chosen != null && chosen.item != null) {
					Stack stack = new Stack(chosen.item, 1);
					for (Function function : chosen.functions) {
						function.run(stack, random);
					}
					// 26.3's stack splitter: past its size, a stack goes in as several
					int max = ModernEnchanting.maxStackSize(stack.item);
					if (stack.count < max) {
						items.add(stack);
					} else {
						for (int count = stack.count; count > 0; ) {
							Stack part = new Stack(stack.item, Math.min(max, count));
							part.enchantments.putAll(stack.enchantments);
							count -= part.count;
							items.add(part);
						}
					}
				}
			}
		}
		return items;
	}

	/** 26.3's Util.shuffle (Collections.shuffle's steps). */
	private static <T> void shuffle(List<T> list, Random random) {
		for (int i = list.size(); i > 1; i--) {
			int swapTo = random.nextInt(i);
			list.set(i - 1, list.set(swapTo, list.get(i - 1)));
		}
	}

	/**
	 * 26.3's LootTable.fill on a container of `size` slots, all empty: what goes in each (null for none), in 26.3's
	 * items. The random is the seed's (26.3's `RandomSource.create(seed)` is java.util.Random's).
	 */
	public Stack[] roll(long seed, int size, float luck) {
		Random random = seed == 0L ? new Random() : new Random(seed);
		List<Stack> items = randomItems(random, luck);
		List<Integer> slots = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			slots.add(i);
		}
		shuffle(slots, random);
		// shuffleAndSplitItems
		List<Stack> splittable = new ArrayList<>();
		for (Iterator<Stack> iterator = items.iterator(); iterator.hasNext(); ) {
			Stack stack = iterator.next();
			if (stack.count <= 0) {
				iterator.remove();
			} else if (stack.count > 1) {
				splittable.add(stack);
				iterator.remove();
			}
		}
		while (slots.size() - items.size() - splittable.size() > 0 && !splittable.isEmpty()) {
			Stack stack = splittable.remove(nextInt(random, 0, splittable.size() - 1));
			int remove = nextInt(random, 1, stack.count / 2);
			Stack copy = stack.split(remove);
			if (stack.count > 1 && random.nextBoolean()) {
				splittable.add(stack);
			} else {
				items.add(stack);
			}
			if (copy.count > 1 && random.nextBoolean()) {
				splittable.add(copy);
			} else {
				items.add(copy);
			}
		}
		items.addAll(splittable);
		shuffle(items, random);
		Stack[] out = new Stack[size];
		for (Stack stack : items) {
			if (slots.isEmpty()) {
				LOG.warn("Tried to over-fill a container");
				break;
			}
			out[slots.remove(slots.size() - 1)] = stack;
		}
		return out;
	}

	/** The roll into a 1.7.10 container (its slots emptied first): 26.3's items as 1.7.10's. */
	public void fill(IInventory inventory, long seed, float luck) {
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			inventory.setInventorySlotContents(i, null);
		}
		Stack[] rolled = roll(seed, inventory.getSizeInventory(), luck);
		for (int i = 0; i < rolled.length; i++) {
			if (rolled[i] != null) {
				inventory.setInventorySlotContents(i, toStack(rolled[i]));
			}
		}
	}

	/** A 26.3 stack as 1.7.10's, or null if 1.7.10 (with EFR) has no such item. */
	public static ItemStack toStack(Stack stack) {
		Item item = item(stack.item);
		if (item == null) {
			return null;
		}
		ItemStack out = new ItemStack(item, stack.count, 0);
		for (Map.Entry<String, Integer> entry : stack.enchantments.entrySet()) {
			Enchantment enchantment = enchantment(entry.getKey());
			if (enchantment != null) {
				out.addEnchantment(enchantment, entry.getValue());
			}
		}
		return out;
	}

	private static Item item(String id) {
		if (id.equals("minecraft:beetroot_seeds")) {
			return ModItems.BEETROOT_SEEDS.isEnabled() ? ModItems.BEETROOT_SEEDS.get() : null;
		}
		Object item = Item.itemRegistry.getObject(id);
		return item instanceof Item found ? found : null;
	}

	/** 26.3's enchantment ids as 1.7.10's and EFR's. */
	private static final Map<String, Object> ENCHANTMENTS = new HashMap<>();

	private static Enchantment enchantment(String id) {
		synchronized (ENCHANTMENTS) {
			if (ENCHANTMENTS.isEmpty()) {
				Object[][] known = {{"protection", Enchantment.protection}, {"fire_protection", Enchantment.fireProtection},
					{"feather_falling", Enchantment.featherFalling}, {"blast_protection", Enchantment.blastProtection},
					{"projectile_protection", Enchantment.projectileProtection}, {"respiration", Enchantment.respiration},
					{"aqua_affinity", Enchantment.aquaAffinity}, {"thorns", Enchantment.thorns}, {"sharpness", Enchantment.sharpness},
					{"smite", Enchantment.smite}, {"bane_of_arthropods", Enchantment.baneOfArthropods}, {"knockback", Enchantment.knockback},
					{"fire_aspect", Enchantment.fireAspect}, {"looting", Enchantment.looting}, {"efficiency", Enchantment.efficiency},
					{"silk_touch", Enchantment.silkTouch}, {"unbreaking", Enchantment.unbreaking}, {"fortune", Enchantment.fortune},
					{"power", Enchantment.power}, {"punch", Enchantment.punch}, {"flame", Enchantment.flame}, {"infinity", Enchantment.infinity},
					{"luck_of_the_sea", Enchantment.field_151370_z}, {"lure", Enchantment.field_151369_A},
					{"mending", ModEnchantments.mending}, {"frost_walker", ModEnchantments.frostWalker}, {"depth_strider", ModEnchantments.depthStrider},
					{"sweeping_edge", ModEnchantments.sweepingEdge}, {"swift_sneak", ModEnchantments.swiftSneak}, {"riptide", ModEnchantments.riptide},
					{"channeling", ModEnchantments.channeling}, {"loyalty", ModEnchantments.loyalty}, {"impaling", ModEnchantments.impaling}};
				for (Object[] pair : known) {
					ENCHANTMENTS.put("minecraft:" + pair[0], pair[1] == null ? Collections.emptyList() : pair[1]);
				}
			}
			Object found = ENCHANTMENTS.get(id);
			return found instanceof Enchantment enchantment ? enchantment : null;
		}
	}

	/** Reads one of the mod's 26.3 data files, line by line. */
	static List<String> lines(String path) {
		List<String> out = new ArrayList<>();
		try (InputStream in = ModernLoot.class.getResourceAsStream(path)) {
			if (in == null) {
				LOG.warn("No {}", path);
				return out;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			for (String line; (line = reader.readLine()) != null; ) {
				if (!line.isEmpty() && !line.startsWith("#")) {
					out.add(line);
				}
			}
		} catch (Exception e) {
			LOG.warn("Can't read {}", path, e);
		}
		return out;
	}
}
