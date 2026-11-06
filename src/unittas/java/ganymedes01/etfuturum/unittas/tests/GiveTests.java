package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.ModEnchantments;
import io.github.asgerjon.unittas.TasCommand;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSettings;

import java.util.List;

/** Modern /give with item components (spec §45). With an empty inventory the first stack given is in slot 0. */
@TasSuite("commands")
public class GiveTests extends TasTestCase {

	private ItemStack given() {
		List<ItemStack> stacks = inventory();
		assertTrue(!stacks.isEmpty(), "nothing was given");
		return stacks.get(0);
	}

	private static int level(ItemStack stack, Enchantment enchantment) {
		return EnchantmentHelper.getEnchantmentLevel(enchantment.effectId, stack);
	}

	public void testSuggestsItems() {
		List<String> shown = type("/give @s diamond_a").suggestions();
		assertTrue(shown.stream().anyMatch(id -> id.contains("diamond_axe")), "no diamond_axe in " + shown);
	}

	public void testSuggestsComponentsAfterBracket() {
		assertIn("[", type("/give @s diamond_axe").suggestions());
		List<String> shown = type("/give @s diamond_axe[").suggestions();
		assertIn("!", shown);
		assertTrue(shown.stream().anyMatch(name -> name.contains("enchantments=")), "no enchantments= in " + shown);
	}

	public void testEnchantedAxe() {
		TasCommand give = command("/give " + playerName() + " diamond_axe[enchantments={mending:1, unbreaking:3, efficiency:5, silk_touch:1, smite:5}]");
		assertSaid(give, "Gave 1 [Diamond Axe] to " + playerName());
		ItemStack axe = given();
		assertEqual("minecraft:diamond_axe:0", id(axe));
		assertEqual(3, level(axe, Enchantment.unbreaking));
		assertEqual(5, level(axe, Enchantment.efficiency));
		assertEqual(1, level(axe, Enchantment.silkTouch));
		assertEqual(5, level(axe, Enchantment.smite));
		if (ModEnchantments.mending != null) {
			assertEqual(1, level(axe, ModEnchantments.mending));
		}
	}

	public void testHoeWithNameAndKnockback() {
		assertNoError(command("/give @s wooden_hoe[enchantments={knockback:69,}, custom_name=\"Asgers roe hakker\"]"));
		ItemStack hoe = given();
		assertEqual(69, level(hoe, Enchantment.knockback));
		assertEqual("Asgers roe hakker", hoe.getDisplayName());
		// 1.7.10 itself shows a custom name in italics, in the tooltip
		String tooltipName = onClient(() -> (String) hoe.getTooltip(mc().thePlayer, false).get(0));
		assertIn("§oAsgers roe hakker", tooltipName);
	}

	public void testRedUprightName() {
		command("/give @s stick[custom_name={text:\"Red\",color:\"red\",italic:false}]");
		String name = given().getDisplayName();
		assertIn("§cRed", name);
		assertNotIn("§o", name);
	}

	public void testNameInParts() {
		command("/give @s stick[custom_name=[\"\",{text:\"Asgers \",color:\"gold\"},{text:\"roe hakker\",bold:true,italic:false}]]");
		String name = given().getDisplayName();
		assertIn("Asgers ", name);
		assertIn("roe hakker", name);
		assertIn("§6", name);
		assertIn("§l", name);
	}

	public void testLore() {
		command("/give @s stick[lore=[\"line one\",{text:\"gold line\",color:\"gold\"}]]");
		ItemStack stick = given();
		NBTTagList lore = stick.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
		assertEqual(2, lore.tagCount());
		// 1.7.10's tooltip shows lore in purple italics itself; a colour cancels the italics, so it comes again after it
		List<?> tooltip = onClient(() -> stick.getTooltip(mc().thePlayer, false));
		assertIn("§5§oline one", tooltip);
		assertIn("§5§o§6§ogold line", tooltip);
	}

	public void testDyedLeather() {
		command("/give @s leather_chestplate[dyed_color=16711680]");
		ItemStack red = given();
		assertEqual(0xFF0000, ((ItemArmor) red.getItem()).getColor(red));
		onServer(() -> player().inventory.clearInventory(null, -1));
		command("/give @s leather_chestplate[dyed_color=[0f,1f,0f]]");
		ItemStack green = given();
		assertEqual(0x00FF00, ((ItemArmor) green.getItem()).getColor(green));
	}

	public void testDamage() {
		command("/give @s diamond_sword[damage=1000]");
		assertEqual(1000, given().getItemDamage());
		assertError(command("/give @s diamond_sword[damage=99999]"), "Item damage 99999 is higher than the maximum of 1561");
	}

	public void testUnbreakable() {
		command("/give @s diamond_sword[unbreakable={}]");
		assertTrue(given().getTagCompound().getBoolean("Unbreakable"), "not unbreakable");
	}

	public void testNetheriteHelmet() {
		command("/give @s netherite_helmet");
		assertEqual("Netherite Helmet", given().getDisplayName());
	}

	public void testTenStacks() {
		command("/give @s stone 640");
		List<ItemStack> stacks = inventory();
		assertEqual(10, stacks.size());
		for (ItemStack stack : stacks) {
			assertEqual("minecraft:stone:0", id(stack));
			assertEqual(64, stack.stackSize);
		}
	}

	public void testTooMany() {
		assertError(command("/give @s stone 6401"), "Can't give more than 6400 of [Stone]");
	}

	/** In survival: in creative a full inventory takes the items and they are gone, in modern too. */
	public void testFullInventoryDrops() {
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		onServer(() -> {
			for (int slot = 0; slot < player().inventory.mainInventory.length; slot++) {
				player().inventory.mainInventory[slot] = new ItemStack(Blocks.dirt, 64);
			}
			player().inventoryContainer.detectAndSendChanges();
		});
		command("/give @s stone 64");
		ticks(2);
		boolean dropped = onServer(() -> {
			for (EntityItem item : entitiesNear(EntityItem.class, 4.0)) {
				if (item.getEntityItem().getItem() == Item.getItemFromBlock(Blocks.stone)) {
					return true;
				}
			}
			return false;
		});
		assertTrue(dropped, "no stone dropped at the player");
	}

	public void testComponentErrors() {
		assertError(command("/give @s diamond_axe[enchantments={sharpness:0}]"),
			"Malformed 'minecraft:enchantments' component: 'Value 0 outside of range [1:255]'");
		assertError(command("/give @s diamond_axe[foo=1]"), "Unknown item component 'minecraft:foo'");
		assertError(command("/give @s diamond_axe[container=[]]"), "Item component 'minecraft:container' is not supported yet");
		assertError(command("/give @s diamond_axe[damage=1,damage=2]"),
			"Item component 'minecraft:damage' was repeated, but only one value can be specified");
	}

	public void testLegacyNumericId() {
		command("/give " + playerName() + " 278");
		assertEqual("minecraft:diamond_pickaxe:0", id(given()));
	}

	public void testLegacyWool() {
		command("/give " + playerName() + " wool 3 14");
		ItemStack wool = given();
		assertEqual("minecraft:wool:14", id(wool));
		assertEqual(3, wool.stackSize);
	}
}
