package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

import java.util.List;

/**
 * The smithing table as our own (SPEC-modern-movement.md §88.4, step C2), as 26.3's `SmithingMenu`: ours, EFR's classes
 * gone; 2.5 hard, an axe's, wood's sounds; the netherite upgrade (template, diamond gear, ingot) keeps the gear's damage,
 * enchantments and name, and takes one of each; a shift-click puts a template, gear and an ingot each in its slot; what's
 * left in it comes back on closing; 26.3's recipes (`smithing_recipes_26_3.txt`). Through the container (slot 0 the
 * template, 1 the base, 2 the addition, 3 the result), as a player's clicks reach it. At z 100 on the flat world.
 */
@TasSuite("smithing")
public class SmithingTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 100;
	private static final String[] EFR_CLASSES = {"blocks.BlockSmithingTable", "inventory.ContainerSmithingTable", "inventory.slot.SlotSmithingResult",
		"client.gui.inventory.GuiSmithingTable", "recipes.SmithingTableRecipes", "compat.cthandlers.CTSmithingTable"};

	@Override
	protected void setUp() {
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().inventory.clearInventory(null, -1);
			world().setBlock(X, Y, Z, block("smithing_table"), 0, 3);
			player().playerNetServerHandler.setPlayerLocation(X + 0.5, Y, Z + 2.5, 180.0F, 30.0F);
		});
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			player().closeScreen();
			player().inventory.clearInventory(null, -1);
			world().setBlock(X, Y, Z, Blocks.air, 0, 3);
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	private static Item item(String name) {
		Object item = Item.itemRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(item instanceof Item, "no " + McFly.DOMAIN + ":" + name);
		return (Item) item;
	}

	/** The table opened by the player's right click on it, on the server: its container. */
	private static Container open() {
		block("smithing_table").onBlockActivated(world(), X, Y, Z, player(), 1, 0.5F, 1.0F, 0.5F);
		Container container = player().openContainer;
		assertTrue(container != player().inventoryContainer, "the table didn't open");
		return container;
	}

	// ---------------------------------------------------------------- ours

	public void testTableIsOurs() throws Exception {
		StringBuilder wrong = new StringBuilder();
		String type = block("smithing_table").getClass().getName();
		if (!type.startsWith("io.github.asgerjon.mcfly.")) {
			wrong.append(type).append(' ');
		}
		String menu = onServer(() -> {
			String name = open().getClass().getName();
			player().closeScreen();
			return name;
		});
		if (!menu.startsWith("io.github.asgerjon.mcfly.")) {
			wrong.append(menu).append(' ');
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.append(name).append(" still there ");
			}
		}
		assertEqual("", wrong.toString().trim(), "the smithing table's classes");
	}

	/** 26.3's: 2.5, an axe, wood's sounds. */
	public void testStrength() {
		String got = onServer(() -> {
			Block table = block("smithing_table");
			return table.getBlockHardness(world(), X, Y, Z) + " " + table.getHarvestTool(0) + " " + table.stepSound.getBreakSound();
		});
		assertEqual("2.5 axe dig.wood", got, "hardness, tool, break sound");
	}

	// ---------------------------------------------------------------- the upgrade

	/**
	 * A worn, enchanted, named diamond pickaxe with the template and an ingot: a netherite pickaxe with the same damage,
	 * enchantments and name (26.3's `transmuteCopy`); taking it takes one of each input.
	 */
	public void testUpgradeKeepsTheGear() {
		String got = onServer(() -> {
			Container container = open();
			ItemStack pickaxe = new ItemStack(Items.diamond_pickaxe);
			pickaxe.setItemDamage(321);
			pickaxe.addEnchantment(Enchantment.efficiency, 3);
			pickaxe.setStackDisplayName("Old Faithful");
			container.getSlot(0).putStack(new ItemStack(item("netherite_upgrade_smithing_template"), 2));
			container.getSlot(1).putStack(pickaxe);
			container.getSlot(2).putStack(new ItemStack(item("netherite_ingot"), 2));
			container.detectAndSendChanges();
			ItemStack result = container.getSlot(3).getStack();
			StringBuilder out = new StringBuilder();
			if (result == null) {
				return "nothing";
			}
			out.append(Item.itemRegistry.getNameForObject(result.getItem()).replace(McFly.DOMAIN + ":", "")).append(" damage ").append(result.getItemDamage())
				.append(" efficiency ").append(net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, result))
				.append(" named ").append(result.getDisplayName());
			container.slotClick(3, 0, 0, player());
			ItemStack held = player().inventory.getItemStack();
			out.append("; taken ").append(held == null ? "nothing" : held.getDisplayName());
			out.append(", left ").append(count(container, 0)).append(' ').append(count(container, 1)).append(' ').append(count(container, 2));
			player().inventory.setItemStack(null);
			return out.toString();
		});
		assertEqual("netherite_pickaxe damage 321 efficiency 3 named Old Faithful; taken Old Faithful, left 1 0 1", got, "the upgrade");
	}

	private static int count(Container container, int slot) {
		ItemStack stack = container.getSlot(slot).getStack();
		return stack == null ? 0 : stack.stackSize;
	}

	/** Shift-clicked from the inventory: the template, the gear and the ingot each to its own slot (26.3's `canMoveIntoInputSlots`). */
	public void testShiftClickSortsThem() {
		String got = onServer(() -> {
			player().inventory.setInventorySlotContents(9, new ItemStack(Items.diamond_sword));
			player().inventory.setInventorySlotContents(10, new ItemStack(item("netherite_ingot")));
			player().inventory.setInventorySlotContents(11, new ItemStack(item("netherite_upgrade_smithing_template")));
			Container container = open();
			List<?> slots = container.inventorySlots;
			for (int i = 0; i < slots.size(); i++) {
				net.minecraft.inventory.Slot slot = (net.minecraft.inventory.Slot) slots.get(i);
				if (slot.inventory == player().inventory && slot.getSlotIndex() >= 9 && slot.getSlotIndex() <= 11) {
					container.transferStackInSlot(player(), i);
				}
			}
			StringBuilder out = new StringBuilder();
			for (int slot = 0; slot < 3; slot++) {
				ItemStack stack = container.getSlot(slot).getStack();
				out.append(stack == null ? "-" : Item.itemRegistry.getNameForObject(stack.getItem()).replace(McFly.DOMAIN + ":", "").replace("minecraft:", "")).append(' ');
			}
			return out.toString().trim();
		});
		assertEqual("netherite_upgrade_smithing_template diamond_sword netherite_ingot", got, "slots 0, 1, 2 after shift-clicks");
	}

	/** Closing the table gives back what's in it. */
	public void testClosingGivesItBack() {
		String got = onServer(() -> {
			Container container = open();
			container.getSlot(1).putStack(new ItemStack(Items.diamond_axe));
			player().closeScreen();
			return String.valueOf(player().inventory.hasItem(Items.diamond_axe));
		});
		assertEqual("true", got, "the axe back in the inventory");
	}

	/** 26.3's recipes for the table and the template's copying (its jar's, `smithing_recipes_26_3.txt`). */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("smithing_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(2, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}
}
