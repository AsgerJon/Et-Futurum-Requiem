package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.loot.LootTableHolder;
import ganymedes01.etfuturum.loot.ModernLoot;
import ganymedes01.etfuturum.unittas.TasTestCase;
import ganymedes01.etfuturum.world.end.modern.EndCityMarkers;
import ganymedes01.etfuturum.world.end.modern.ModernEnd;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 26.3's loot as 26.3 rolls it (SPEC-modern-movement.md §76.12, 7e): the end city chest's table rolled into 27 slots
 * for seeds 1 to 24, slot by slot (item, count, enchantments) as 26.3's own rolls (`end_loot_26_3.txt`); and a chest
 * holding the table filled when first used, with 1.7.10's items.
 */
public class LootTests extends TasTestCase {

	private static final String DUMP = "/assets/etfuturum/unittas/end_loot_26_3.txt";

	@Override
	protected void setUp() {
		if (!ModernEnd.isActive()) {
			skipTest("modernEnd is off (the chest's loot table comes with it)");
		}
	}

	private static Map<Long, List<String>> readRolls() throws Exception {
		Map<Long, List<String>> rolls = new TreeMap<>();
		try (InputStream in = LootTests.class.getResourceAsStream(DUMP)) {
			assertTrue(in != null, "no " + DUMP);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			for (String line; (line = reader.readLine()) != null; ) {
				if (line.startsWith("roll ")) {
					String[] parts = line.split(" ", 4);
					rolls.computeIfAbsent(Long.parseLong(parts[1]), k -> new ArrayList<>()).add(parts[2] + " " + parts[3]);
				}
			}
		}
		return rolls;
	}

	private static List<String> rolled(long seed) {
		ModernLoot.Stack[] slots = ModernLoot.table(EndCityMarkers.TREASURE).roll(seed, 27, 0.0F);
		List<String> lines = new ArrayList<>();
		for (int slot = 0; slot < slots.length; slot++) {
			ModernLoot.Stack stack = slots[slot];
			if (stack == null) {
				continue;
			}
			StringBuilder line = new StringBuilder().append(slot).append(' ').append(stack.item).append(' ').append(stack.count);
			List<String> enchants = new ArrayList<>();
			for (Map.Entry<String, Integer> entry : stack.enchantments.entrySet()) {
				enchants.add(entry.getKey() + "=" + entry.getValue());
			}
			Collections.sort(enchants);
			for (String enchant : enchants) {
				line.append(' ').append(enchant);
			}
			lines.add(line.toString());
		}
		return lines;
	}

	/** Seeds 1 to 24 into 27 slots: every slot as 26.3's. */
	public void testEndCityTreasureAs26_3() throws Exception {
		Map<Long, List<String>> rolls = readRolls();
		assertEqual(24, rolls.size(), "seeds in the dump");
		List<String> wrong = new ArrayList<>();
		for (Map.Entry<Long, List<String>> entry : rolls.entrySet()) {
			List<String> got = rolled(entry.getKey());
			if (!got.equals(entry.getValue()) && wrong.size() < 4) {
				wrong.add("seed " + entry.getKey() + ": " + got + ", 26.3's " + entry.getValue());
			}
		}
		assertEqual("[]", wrong.toString(), "the rolls");
	}

	/** A chest with the table, first looked into: filled with the seed's roll as 1.7.10's items, the table gone. */
	public void testChestFilledWhenUsed() {
		String found = onServer(() -> {
			World world = player().worldObj;
			int x = (int) Math.floor(player().posX) + 3;
			int y = (int) Math.floor(player().posY);
			int z = (int) Math.floor(player().posZ);
			world.setBlock(x, y, z, Blocks.chest, 2, 3);
			TileEntityChest chest = (TileEntityChest) world.getTileEntity(x, y, z);
			((LootTableHolder) chest).etfu$setLootTable(EndCityMarkers.TREASURE, 2L);
			List<String> got = new ArrayList<>();
			for (int i = 0; i < chest.getSizeInventory(); i++) {
				ItemStack stack = chest.getStackInSlot(i);
				if (stack != null) {
					got.add(i + " " + stack.getItem().getUnlocalizedName() + " " + stack.stackSize + (stack.isItemEnchanted() ? " enchanted" : ""));
				}
			}
			ModernLoot.Stack[] want = ModernLoot.table(EndCityMarkers.TREASURE).roll(2L, 27, 0.0F);
			List<String> expected = new ArrayList<>();
			for (int i = 0; i < want.length; i++) {
				ItemStack stack = want[i] == null ? null : ModernLoot.toStack(want[i]);
				if (stack != null) {
					expected.add(i + " " + stack.getItem().getUnlocalizedName() + " " + stack.stackSize + (stack.isItemEnchanted() ? " enchanted" : ""));
				}
			}
			String table = ((LootTableHolder) chest).etfu$lootTable();
			world.setBlockToAir(x, y, z);
			return (got.equals(expected) ? "as rolled" : got + " not " + expected) + ", " + got.size() + " stacks, table " + table;
		});
		assertTrue(found.startsWith("as rolled, ") && !found.startsWith("as rolled, 0 ") && found.endsWith("table null"), found);
	}
}
