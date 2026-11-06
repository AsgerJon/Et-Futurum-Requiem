package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.ModEnchantments;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

/**
 * Anvil costs as 26.3's (SPEC-modern-movement.md §55.1), read from an anvil container on the server, in survival
 * (creative lifts the enchantment rules). Each case gives the cost shown and the result's new repair cost.
 */
@TasSuite("quick2")
public class AnvilTests extends TasTestCase {

	@Override
	protected void setUp() {
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
	}

	/** The user's report: a Mending book on a fresh pickaxe cost 4 (6 by 1.7.10's rules); 26.3: Mending's 4, halved. */
	public void testMendingBookOnAPickaxe() {
		needs(ModEnchantments.mending);
		assertAnvil(new ItemStack(Items.iron_pickaxe), book(ModEnchantments.mending, 1), null, 2, 1);
	}

	/** Sharpness 1, halved for a book to at least 1, times the level. */
	public void testSharpnessFiveBook() {
		assertAnvil(new ItemStack(Items.diamond_sword), book(Enchantment.sharpness, 5), null, 5, 1);
	}

	/** Unbreaking 2, halved: 1 a level. */
	public void testUnbreakingThreeBook() {
		assertAnvil(new ItemStack(Items.iron_pickaxe), book(Enchantment.unbreaking, 3), null, 3, 1);
	}

	/** Each enchantment of the book counts; 1.7.10 also charged for what the result already holds. */
	public void testTwoEnchantmentBook() {
		ItemStack book = book(Enchantment.efficiency, 2);
		Items.enchanted_book.addEnchantment(book, new EnchantmentData(Enchantment.unbreaking, 1));
		assertAnvil(new ItemStack(Items.iron_pickaxe), book, null, 3, 1);
	}

	/** Two Efficiency IV pickaxes make Efficiency V: 1 times the resulting level, no book halving, nothing to mend. */
	public void testTwoEnchantedPickaxes() {
		ItemStack left = new ItemStack(Items.diamond_pickaxe);
		left.addEnchantment(Enchantment.efficiency, 4);
		assertAnvil(left, left.copy(), null, 5, 1);
	}

	/** Renaming costs 1 (1.7.10: 7 for a damageable item). */
	public void testRenameOnly() {
		assertAnvil(new ItemStack(Items.stone_sword), null, "Bob", 1, 0);
	}

	/** The prior-work charge is the inputs' repair costs; the new one is the higher doubled plus one. */
	public void testPriorWork() {
		needs(ModEnchantments.mending);
		ItemStack left = new ItemStack(Items.iron_pickaxe);
		left.setRepairCost(3);
		assertAnvil(left, book(ModEnchantments.mending, 1), null, 5, 7);
	}

	/** Mending with material costs 1 a unit, however enchanted the item (1.7.10 added the number of enchantments). */
	public void testRepairWithAnIngot() {
		ItemStack left = new ItemStack(Items.iron_pickaxe);
		left.addEnchantment(Enchantment.efficiency, 3);
		left.setItemDamage(left.getMaxDamage() / 2);
		assertAnvil(left, new ItemStack(Items.iron_ingot), null, 1, 1);
	}

	/** 26.3's Mending and Frost Walker are rare (weight 2, anvil cost 4); EFR had them at 1. */
	public void testMendingAndFrostWalkerWeights() {
		needs(ModEnchantments.mending);
		assertEqual(2, ModEnchantments.mending.getWeight(), "Mending's weight");
		if (ModEnchantments.frostWalker != null) {
			assertEqual(2, ModEnchantments.frostWalker.getWeight(), "Frost Walker's weight");
		}
	}

	private static void needs(Enchantment enchantment) {
		if (enchantment == null) {
			skipTest("the enchantment is off in the config");
		}
	}

	private static ItemStack book(Enchantment enchantment, int level) {
		ItemStack book = new ItemStack(Items.enchanted_book);
		Items.enchanted_book.addEnchantment(book, new EnchantmentData(enchantment, level));
		return book;
	}

	/** Puts the stacks into an anvil (and the name, if any) and checks the cost and the result's repair cost. */
	private void assertAnvil(ItemStack left, ItemStack right, String name, int cost, int repairCost) {
		int[] result = onServer(() -> {
			ContainerRepair anvil = new ContainerRepair(player().inventory, world(), 0, 4, 0, player());
			anvil.getSlot(0).putStack(left.copy());
			if (right != null) {
				anvil.getSlot(1).putStack(right.copy());
			}
			if (name != null) {
				anvil.updateItemName(name);
			}
			ItemStack out = anvil.getSlot(2).getStack();
			return new int[]{anvil.maximumCost, out == null ? -1 : out.getRepairCost()};
		});
		assertEqual(cost, result[0], "the cost");
		assertEqual(repairCost, result[1], "the result's repair cost (-1: no result)");
	}
}
