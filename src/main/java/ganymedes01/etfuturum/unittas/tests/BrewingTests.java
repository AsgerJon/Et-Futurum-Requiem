package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.inventory.ContainerNewBrewingStand;
import ganymedes01.etfuturum.potion.BrewingTable;
import ganymedes01.etfuturum.potion.NamedPotion;
import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.potion.PotionContents;
import ganymedes01.etfuturum.tileentities.TileEntityNewBrewingStand;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionHelper;
import net.minecraft.tileentity.TileEntityBrewingStand;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Potions by name, stage B (SPEC-modern-movement.md §69.3, §69.5, §69.6): brewing by 26.3's table in EFR's brewing
 * stand and 1.7.10's; the table decides for a named potion and a reagent it has (§69.4, decision 1), 1.7.10's bits
 * for an unnamed potion; the slow falling potion (§68's stage E).
 */
public class BrewingTests extends TasTestCase {

	/** 26.3's `VanillaBrewingProvider.buildMixes` (the test's own copy): input reagent output; "start" also water -> mundane. */
	static final String[] MIXES_26_3 = {"water glowstone_dust thick", "water redstone mundane", "water nether_wart awkward",
		"start breeze_rod wind_charged", "start slime_block oozing", "start stone infested", "start cobweb weaving",
		"awkward golden_carrot night_vision", "night_vision redstone long_night_vision", "night_vision fermented_spider_eye invisibility",
		"long_night_vision fermented_spider_eye long_invisibility", "invisibility redstone long_invisibility",
		"start magma_cream fire_resistance", "fire_resistance redstone long_fire_resistance", "start rabbit_foot leaping",
		"leaping redstone long_leaping", "leaping glowstone_dust strong_leaping", "leaping fermented_spider_eye slowness",
		"long_leaping fermented_spider_eye long_slowness", "slowness redstone long_slowness", "slowness glowstone_dust strong_slowness",
		"awkward turtle_helmet turtle_master", "turtle_master redstone long_turtle_master", "turtle_master glowstone_dust strong_turtle_master",
		"swiftness fermented_spider_eye slowness", "long_swiftness fermented_spider_eye long_slowness", "start sugar swiftness",
		"swiftness redstone long_swiftness", "swiftness glowstone_dust strong_swiftness", "awkward pufferfish water_breathing",
		"water_breathing redstone long_water_breathing", "start glistering_melon_slice healing", "healing glowstone_dust strong_healing",
		"healing fermented_spider_eye harming", "strong_healing fermented_spider_eye strong_harming", "harming glowstone_dust strong_harming",
		"poison fermented_spider_eye harming", "long_poison fermented_spider_eye harming", "strong_poison fermented_spider_eye strong_harming",
		"start spider_eye poison", "poison redstone long_poison", "poison glowstone_dust strong_poison", "start ghast_tear regeneration",
		"regeneration redstone long_regeneration", "regeneration glowstone_dust strong_regeneration", "start blaze_powder strength",
		"strength redstone long_strength", "strength glowstone_dust strong_strength", "water fermented_spider_eye weakness",
		"weakness redstone long_weakness", "awkward phantom_membrane slow_falling", "slow_falling redstone long_slow_falling"};

	@Override
	protected void setUp() {
		if (!McFlyConfig.potionsByName) {
			skipTest("potions by name are off");
		}
	}

	// ---------------------------------------------------------------- the table

	/** Every 26.3 mix EFR can make (its potions offered, its reagent there), in each container. */
	public void testEveryMix() {
		int checked = 0;
		for (String[] mix : expandedMixes()) {
			NamedPotion input = NamedPotions.byId("minecraft:" + mix[0]);
			NamedPotion output = NamedPotions.byId("minecraft:" + mix[2]);
			ItemStack reagent = item(mix[1]);
			if (reagent == null || input == null || output == null || !input.isAvailable() || !output.isAvailable()) {
				continue;
			}
			for (BrewingTable.Container container : containers()) {
				ItemStack made = BrewingTable.brew(BrewingTable.create(container, input), reagent);
				String what = container + " " + mix[0] + " + " + mix[1];
				assertTrue(made != null, what + " brews nothing");
				assertEqual(container, BrewingTable.containerOf(made), what + "'s container");
				assertEqual("minecraft:" + mix[2], idOf(made), what);
				checked++;
			}
		}
		assertTrue(checked >= 120, "only " + checked + " mixes checked");
	}

	/** Gunpowder makes a splash potion of every potion a mix uses, dragon's breath a lingering one of a splash potion. */
	public void testContainerChanges() {
		for (String name : mixedPotions()) {
			NamedPotion potion = NamedPotions.byId("minecraft:" + name);
			if (potion == null || !potion.isAvailable()) {
				continue;
			}
			ItemStack splash = BrewingTable.brew(BrewingTable.create(BrewingTable.Container.POTION, potion), new ItemStack(Items.gunpowder));
			assertTrue(splash != null, name + " + gunpowder brews nothing");
			assertEqual(BrewingTable.Container.SPLASH, BrewingTable.containerOf(splash), name + " + gunpowder");
			assertEqual("minecraft:" + name, idOf(splash), name + " + gunpowder");
			if (ModItems.LINGERING_POTION.isEnabled() && ModItems.DRAGON_BREATH.isEnabled()) {
				ItemStack lingering = BrewingTable.brew(splash, ModItems.DRAGON_BREATH.newItemStack());
				assertTrue(lingering != null, "splash " + name + " + dragon's breath brews nothing");
				assertEqual(BrewingTable.Container.LINGERING, BrewingTable.containerOf(lingering), "splash " + name + " + dragon's breath");
				assertEqual("minecraft:" + name, idOf(lingering), "splash " + name + " + dragon's breath");
			}
		}
	}

	/** Where 26.3 has no recipe, nothing brews (1.7.10's bits would have made something of several). */
	public void testNoBrewWhere26_3HasNone() {
		String[] none = {"long_strength glowstone_dust", "strong_swiftness redstone", "awkward redstone", "water golden_carrot",
			"thick nether_wart", "mundane nether_wart", "slow_falling glowstone_dust", "long_poison glowstone_dust"};
		for (String entry : none) {
			String[] parts = entry.split(" ");
			ItemStack made = BrewingTable.brew(potion(parts[0], BrewingTable.Container.POTION), item(parts[1]));
			assertEqual(null, made == null ? null : idOf(made) + " " + made.getItemDamage(), parts[0] + " + " + parts[1]);
		}
		ItemStack splash = potion("slow_falling", BrewingTable.Container.SPLASH);
		assertEqual(null, BrewingTable.brew(splash, new ItemStack(Items.gunpowder)), "a splash potion + gunpowder");
	}

	/** A potion with no name brews by 1.7.10's bits as before (8289, an odd one, with redstone). */
	public void testUnnamedPotionsBrewByBits() {
		int before = 8289;
		int after = PotionHelper.applyIngredient(before, Items.redstone.getPotionEffect(new ItemStack(Items.redstone)));
		ItemStack made = BrewingTable.brew(new ItemStack(Items.potionitem, 1, before), new ItemStack(Items.redstone));
		if (after == before) {
			assertEqual(null, made, "8289 + redstone");
		} else {
			assertTrue(made != null, "8289 + redstone brews nothing (1.7.10 makes " + after + ")");
			assertEqual(after, made.getItemDamage(), "8289 + redstone");
		}
	}

	// ---------------------------------------------------------------- the stands

	/** EFR's stand brews three awkward potions and a membrane into slow falling, then redstone into the long one. */
	public void testSlowFallingInEfrsStand() {
		if (!ModBlocks.BREWING_STAND.isEnabled()) {
			skipTest("EFR's brewing stand is off");
		}
		TileEntityNewBrewingStand stand = onServer(() -> {
			world().setBlock(2, 4, 2, ModBlocks.BREWING_STAND.get());
			return (TileEntityNewBrewingStand) world().getTileEntity(2, 4, 2);
		});
		assertTrue(stand != null, "no brewing stand there");
		onServer(() -> {
			for (int i = 0; i < 3; i++) {
				stand.setInventorySlotContents(i, potion("awkward", BrewingTable.Container.POTION));
			}
			stand.setInventorySlotContents(3, item("phantom_membrane"));
			stand.setInventorySlotContents(4, new ItemStack(Items.blaze_powder));
		});
		brewNow(stand);
		assertEqual("minecraft:slow_falling minecraft:slow_falling minecraft:slow_falling", slots(stand), "after the membrane");
		assertEqual(null, onServer(() -> stand.getStackInSlot(3)), "the membrane left");
		onServer(() -> stand.setInventorySlotContents(3, new ItemStack(Items.redstone)));
		brewNow(stand);
		assertEqual("minecraft:long_slow_falling minecraft:long_slow_falling minecraft:long_slow_falling", slots(stand), "after redstone");
		onServer(() -> world().setBlockToAir(2, 4, 2));
	}

	/** EFR's stand's reagent slot, and its screen's, take the membrane (1.7.10's ingredient check doesn't). */
	public void testReagentSlotTakesTheTable() {
		if (!ModBlocks.BREWING_STAND.isEnabled()) {
			skipTest("EFR's brewing stand is off");
		}
		boolean[] takes = onServer(() -> {
			world().setBlock(2, 4, 4, ModBlocks.BREWING_STAND.get());
			TileEntityNewBrewingStand stand = (TileEntityNewBrewingStand) world().getTileEntity(2, 4, 4);
			ContainerNewBrewingStand container = new ContainerNewBrewingStand(player().inventory, stand);
			boolean slot = stand.isItemValidForSlot(3, item("phantom_membrane"));
			boolean screen = container.getSlot(3).isItemValid(item("phantom_membrane"));
			world().setBlockToAir(2, 4, 4);
			return new boolean[]{slot, screen};
		});
		assertTrue(takes[0], "the stand's reagent slot refuses the membrane");
		assertTrue(takes[1], "the screen's reagent slot refuses the membrane");
	}

	/** 1.7.10's own stand (when EFR's is off) brews by the table too. */
	public void testVanillaStand() throws Exception {
		String made = onServer(() -> {
			TileEntityBrewingStand stand = new TileEntityBrewingStand();
			stand.setWorldObj(world());
			stand.setInventorySlotContents(0, potion("awkward", BrewingTable.Container.POTION));
			stand.setInventorySlotContents(3, item("phantom_membrane"));
			boolean takes = stand.isItemValidForSlot(3, item("phantom_membrane"));
			Method canBrew = TileEntityBrewingStand.class.getDeclaredMethod("canBrew");
			Method brewPotions = TileEntityBrewingStand.class.getDeclaredMethod("brewPotions");
			canBrew.setAccessible(true);
			brewPotions.setAccessible(true);
			boolean can = (Boolean) canBrew.invoke(stand);
			brewPotions.invoke(stand);
			return takes + " " + can + " " + idOf(stand.getStackInSlot(0));
		});
		assertEqual("true true minecraft:slow_falling", made, "the membrane taken, brewable, brewed");
	}

	// ---------------------------------------------------------------- helpers

	/** The brew starts, then is cut short to its last tick (its 400 ticks aren't what's tested). */
	private void brewNow(TileEntityNewBrewingStand stand) {
		serverTicks(3);
		int time = onServer(stand::getBrewTime);
		assertTrue(time > 0, "the stand didn't start brewing");
		onServer(() -> stand.func_145938_d(1));
		serverTicks(3);
	}

	private String slots(TileEntityNewBrewingStand stand) {
		return onServer(() -> {
			List<String> ids = new ArrayList<>();
			for (int i = 0; i < 3; i++) {
				ids.add(idOf(stand.getStackInSlot(i)));
			}
			return String.join(" ", ids);
		});
	}

	private static List<BrewingTable.Container> containers() {
		List<BrewingTable.Container> list = new ArrayList<>();
		list.add(BrewingTable.Container.POTION);
		list.add(BrewingTable.Container.SPLASH);
		if (ModItems.LINGERING_POTION.isEnabled()) {
			list.add(BrewingTable.Container.LINGERING);
		}
		return list;
	}

	/** The mixes as 26.3's `BrewingProvider` makes them: a start mix is water -> mundane and awkward -> the potion. */
	private static List<String[]> expandedMixes() {
		List<String[]> list = new ArrayList<>();
		for (String line : MIXES_26_3) {
			String[] parts = line.split(" ");
			if (parts[0].equals("start")) {
				NamedPotion output = NamedPotions.byId("minecraft:" + parts[2]);
				if (output == null || !output.isAvailable() || item(parts[1]) == null) {
					continue;
				}
				list.add(new String[]{"water", parts[1], "mundane"});
				list.add(new String[]{"awkward", parts[1], parts[2]});
			} else {
				list.add(parts);
			}
		}
		return list;
	}

	private static Set<String> mixedPotions() {
		Set<String> names = new LinkedHashSet<>();
		for (String[] mix : expandedMixes()) {
			names.add(mix[0]);
			names.add(mix[2]);
		}
		return names;
	}

	private static ItemStack item(String id) {
		ItemIds.Entry entry = ItemIds.byId("minecraft:" + id);
		return entry == null ? null : entry.createStack(1);
	}

	private static ItemStack potion(String name, BrewingTable.Container container) {
		NamedPotion potion = NamedPotions.byId("minecraft:" + name);
		assertTrue(potion != null, "no potion " + name);
		ItemStack stack = BrewingTable.create(container, potion);
		assertTrue(stack != null, "no " + container + " of " + name);
		return stack;
	}

	private static String idOf(ItemStack stack) {
		NamedPotion potion = PotionContents.of(stack).potion();
		return potion == null ? null : potion.id();
	}
}
