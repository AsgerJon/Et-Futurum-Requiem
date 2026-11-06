package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.ModEnchantments;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/** Modern /enchant (spec §44), with the held item named in each test. */
@TasSuite("commands")
public class EnchantTests extends TasTestCase {

	private int level(Enchantment enchantment) {
		ItemStack held = held();
		return held == null ? 0 : EnchantmentHelper.getEnchantmentLevel(enchantment.effectId, held);
	}

	public void testSharpness() {
		setHeld(new ItemStack(Items.diamond_sword));
		assertSaid(command("/enchant @s sharpness 5"), "Applied enchantment Sharpness V to " + playerName() + "'s item");
		assertEqual(5, level(Enchantment.sharpness));
	}

	public void testAlreadyHasIt() {
		setHeld(new ItemStack(Items.diamond_sword));
		command("/enchant @s sharpness 5");
		assertError(command("/enchant @s sharpness 5"), "Diamond Sword cannot support that enchantment");
	}

	public void testSharpnessExcludesSmite() {
		setHeld(new ItemStack(Items.diamond_sword));
		command("/enchant @s sharpness 5");
		assertError(command("/enchant @s smite"), "Diamond Sword cannot support that enchantment");
		assertEqual(0, level(Enchantment.smite));
	}

	public void testAboveTheMaximum() {
		setHeld(new ItemStack(Items.diamond_sword));
		assertError(command("/enchant @s sharpness 6"), "6 is higher than the maximum level of 5 supported by that enchantment");
	}

	public void testEmptyHand() {
		assertError(command("/enchant @s unbreaking"), playerName() + " is not holding any item");
	}

	public void testStick() {
		setHeld(new ItemStack(Items.stick));
		assertError(command("/enchant @s silk_touch"), "Stick cannot support that enchantment");
	}

	public void testMendingShowsNoLevel() {
		if (ModEnchantments.mending == null) {
			skipTest("Mending is turned off");
		}
		setHeld(new ItemStack(Items.diamond_pickaxe));
		assertSaid(command("/enchant @s mending"), "Applied enchantment Mending to");
		assertEqual(1, level(ModEnchantments.mending));
	}

	public void testSweepingEdge() {
		if (ModEnchantments.sweepingEdge == null) {
			skipTest("Sweeping Edge is turned off");
		}
		setHeld(new ItemStack(Items.diamond_sword));
		assertNoError(command("/enchant @s sweeping_edge 3"));
		assertEqual(3, level(ModEnchantments.sweepingEdge));
		setHeld(new ItemStack(Items.diamond_sword));
		assertError(command("/enchant @s sweeping_edge 4"), "4 is higher than the maximum level of 3");
	}

	public void testZombiesHoldingSwords() {
		EntityZombie first = spawn(EntityZombie::new, 3.5, 4.0, 0.5);
		EntityZombie second = spawn(EntityZombie::new, -3.5, 4.0, 0.5);
		onServer(() -> {
			for (EntityZombie zombie : new EntityZombie[]{first, second}) {
				zombie.setCurrentItemOrArmor(0, new ItemStack(Items.iron_sword));
				zombie.setCurrentItemOrArmor(4, new ItemStack(Items.leather_helmet));
			}
		});
		assertSaid(command("/enchant @e[type=zombie] sharpness"), "Applied enchantment Sharpness I to 2 entities");
		assertEqual(1, onServer(() -> EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, first.getHeldItem())));
	}

	public void testLegacyForm() {
		setHeld(new ItemStack(Items.diamond_sword));
		command("/enchant " + playerName() + " 16 5");
		assertEqual(5, level(Enchantment.sharpness));
	}
}
