package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import ganymedes01.etfuturum.potion.BrewingTable;
import ganymedes01.etfuturum.potion.NamedPotion;
import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.potion.PotionContents;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * The reagents the last two potion recipes need (SPEC-modern-movement.md §69.13), as items to be had in creative
 * before their mobs come: the breeze rod (wind charged) and the turtle shell (turtle master), with turtle scutes, from
 * which 26.3 crafts and mends it. The turtle shell as 26.3's: a helmet of 2 armour, 275 uses, mended with scutes,
 * giving Water Breathing for 10 s while its wearer's head is out of water.
 */
@TasSuite("potions")
public class ReagentItemsTests extends TasTestCase {

	/** All three by 26.3's ids, in EFR's creative tab. */
	@SuppressWarnings("unchecked")
	public void testItemsInCreative() {
		String[] ids = {"breeze_rod", "turtle_scute", "turtle_helmet"};
		List<ItemStack> tab = onClient(() -> {
			List<ItemStack> stacks = new ArrayList<>();
			for (Object item : net.minecraft.item.Item.itemRegistry) {
				((net.minecraft.item.Item) item).getSubItems((net.minecraft.item.Item) item, io.github.asgerjon.mcfly.registry.McFlyTab.TAB, stacks);
			}
			return stacks;
		});
		for (String id : ids) {
			ItemIds.Entry entry = ItemIds.byId("minecraft:" + id);
			assertTrue(entry != null, "no item minecraft:" + id);
			boolean listed = false;
			for (ItemStack stack : tab) {
				listed |= stack.getItem() == entry.item() && entry.item().getCreativeTab() == io.github.asgerjon.mcfly.registry.McFlyTab.TAB;
			}
			assertTrue(listed, id + " isn't in McFly's creative tab");
		}
		assertNoError(command("/give @s breeze_rod"));
		assertNoError(command("/give @s turtle_helmet"));
	}

	/** Wind charging and turtle master brew now, as 26.3's start mix and mixes. */
	public void testTheirPotionsBrew() {
		String[][] mixes = {{"awkward", "breeze_rod", "wind_charged"}, {"water", "breeze_rod", "mundane"},
			{"awkward", "turtle_helmet", "turtle_master"}, {"turtle_master", "redstone", "long_turtle_master"},
			{"turtle_master", "glowstone_dust", "strong_turtle_master"}};
		for (String[] mix : mixes) {
			ItemIds.Entry reagent = ItemIds.byId("minecraft:" + mix[1]);
			assertTrue(reagent != null, "no item minecraft:" + mix[1]);
			ItemStack made = BrewingTable.brew(BrewingTable.create(BrewingTable.Container.POTION, NamedPotions.byId("minecraft:" + mix[0])), reagent.createStack(1));
			NamedPotion brewed = made == null ? null : PotionContents.of(made).potion();
			assertEqual("minecraft:" + mix[2], brewed == null ? null : brewed.id(), mix[0] + " + " + mix[1]);
		}
	}

	/** Five scutes make a turtle shell (26.3's recipe: a helmet's shape). */
	public void testShellFromScutes() {
		ItemStack made = onServer(() -> {
			InventoryCrafting grid = new InventoryCrafting(new Container() {
				@Override
				public boolean canInteractWith(EntityPlayer player) {
					return true;
				}
			}, 3, 3);
			for (int slot : new int[]{0, 1, 2, 3, 5}) {
				grid.setInventorySlotContents(slot, McFlyItems.TURTLE_SCUTE.newItemStack());
			}
			return CraftingManager.getInstance().findMatchingRecipe(grid, world());
		});
		assertTrue(made != null && made.getItem() == McFlyItems.TURTLE_HELMET.get(), "five scutes made " + made);
	}

	/** The shell as armour: a helmet, 2 armour points, 275 uses, mended with scutes. */
	public void testShellAsArmour() {
		ItemArmor shell = (ItemArmor) McFlyItems.TURTLE_HELMET.get();
		assertEqual(0, shell.armorType, "its slot (the head)");
		assertEqual(2, shell.damageReduceAmount, "its armour");
		assertEqual(275, shell.getMaxDamage(), "its uses");
		assertTrue(shell.getIsRepairable(McFlyItems.TURTLE_HELMET.newItemStack(), McFlyItems.TURTLE_SCUTE.newItemStack()), "not mended with a scute");
	}

	/** For a look: the shell worn, seen from the front, and the three items in the hotbar. */
	public void testTheirLook() {
		onServer(() -> {
			player().inventory.armorInventory[3] = McFlyItems.TURTLE_HELMET.newItemStack();
			player().inventory.mainInventory[0] = McFlyItems.BREEZE_ROD.newItemStack();
			player().inventory.mainInventory[1] = McFlyItems.TURTLE_SCUTE.newItemStack();
			player().inventory.mainInventory[2] = McFlyItems.TURTLE_HELMET.newItemStack();
			player().inventoryContainer.detectAndSendChanges();
		});
		onClient(() -> {
			mc().gameSettings.thirdPersonView = 2;
		});
		ticks(10);
		String shot = screenshot("reagents.look");
		onClient(() -> {
			mc().gameSettings.thirdPersonView = 0;
		});
		onServer(() -> {
			player().inventory.armorInventory[3] = null;
			for (int i = 0; i < 3; i++) {
				player().inventory.mainInventory[i] = null;
			}
			player().clearActivePotions();
		});
		assertTrue(shot != null, "no picture taken");
	}

	/** Worn with the head out of water: Water Breathing for 10 s, kept topped up (26.3's `turtleHelmetTick`). */
	public void testShellGivesWaterBreathing() {
		onServer(() -> {
			player().clearActivePotions();
			player().inventory.armorInventory[3] = McFlyItems.TURTLE_HELMET.newItemStack();
		});
		serverTicks(5);
		int duration = onServer(() -> {
			PotionEffect effect = player().getActivePotionEffect(Potion.waterBreathing);
			return effect == null ? 0 : effect.getDuration();
		});
		onServer(() -> {
			player().inventory.armorInventory[3] = null;
			player().clearActivePotions();
		});
		assertTrue(duration > 190 && duration <= 200, "Water Breathing for " + duration + " ticks (26.3: 200, topped up)");
	}
}
