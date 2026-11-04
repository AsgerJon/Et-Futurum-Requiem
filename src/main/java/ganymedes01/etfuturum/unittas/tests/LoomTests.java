package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.McFly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.client.gui.inventory.GuiLoom;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.inventory.ContainerLoom;
import ganymedes01.etfuturum.tileentities.TileEntityBanner;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The loom's menu and screen, as 26.3's `LoomMenu` and `LoomScreen` (SPEC-modern-movement.md §66.5, stage 1): a banner,
 * a dye and maybe a pattern item in; the patterns offered; one picked puts the banner with that layer, in the dye's
 * colour, in the result; taking it uses one banner and one dye.
 */
public class LoomTests extends ScreenTestCase {

	private static final int X = 2;
	private static final int Y = 4;
	private static final int Z = 3;

	/** 26.3's `no_item_required` banner patterns, in its order (`data/minecraft/tags/banner_pattern`). */
	private static final List<String> NO_ITEM_REQUIRED = Arrays.asList("square_bottom_left", "square_bottom_right",
		"square_top_left", "square_top_right", "stripe_bottom", "stripe_top", "stripe_left", "stripe_right", "stripe_center",
		"stripe_middle", "stripe_downright", "stripe_downleft", "small_stripes", "cross", "straight_cross", "triangle_bottom",
		"triangle_top", "triangles_bottom", "triangles_top", "diagonal_left", "diagonal_up_right", "diagonal_up_left",
		"diagonal_right", "circle", "rhombus", "half_vertical", "half_horizontal", "half_vertical_right",
		"half_horizontal_bottom", "border", "gradient", "gradient_up");

	@Override
	protected void setUp() {
		if (!ConfigBlocksItems.enableLoom || !ConfigBlocksItems.enableBanners) {
			skipTest("the loom or banners are off");
		}
		onServer(() -> {
			world().setBlock(X, Y, Z, ModBlocks.LOOM.get());
		});
	}

	@Override
	protected void tearDown() {
		onClient(() -> {
			if (mc().thePlayer != null) {
				mc().thePlayer.closeScreen();
			}
		});
		ticks(2);
		onServer(() -> {
			world().setBlockToAir(X, Y, Z);
			player().inventory.clearInventory(null, -1);
		});
	}

	/** Right-clicking the loom opens its menu: the container on the server, the screen on the client. */
	public void testLoomOpensItsMenu() {
		open();
		assertTrue(onServer(() -> player().openContainer instanceof ContainerLoom), "the server's container");
		assertTrue(onClient(() -> mc().currentScreen instanceof GuiLoom), "the client's screen");
	}

	/** Each slot takes only its own: banners, dyes (1.7.10's and EFR's), pattern items (none yet), and the result nothing. */
	public void testSlotsTakeOnlyTheirItems() {
		open();
		List<String> wrong = onServer(() -> {
			ContainerLoom loom = loom();
			List<String> found = new ArrayList<>();
			check(found, loom.bannerSlot, banner(0), true);
			check(found, loom.bannerSlot, new ItemStack(Blocks.wool), false);
			check(found, loom.dyeSlot, new ItemStack(Items.dye, 1, 1), true);
			check(found, loom.dyeSlot, new ItemStack(Items.dye, 1, 15), true);
			if (ModItems.DYE.isEnabled()) {
				check(found, loom.dyeSlot, ModItems.DYE.newItemStack(1, 3), true);
			}
			check(found, loom.dyeSlot, new ItemStack(Blocks.wool), false);
			check(found, loom.patternSlot, banner(0), false);
			check(found, loom.patternSlot, new ItemStack(Items.paper), false);
			check(found, loom.resultSlot, banner(0), false);
			return found;
		});
		assertEqual("[]", wrong.toString(), "the slots' rules");
	}

	/** With a banner and a dye in and no pattern item, the 32 of 26.3's `no_item_required`, in its order; none without either. */
	public void testThe32PatternsOffered() {
		open();
		assertEqual("[]", offered(), "nothing in");
		put(0, banner(0));
		assertEqual("[]", offered(), "a banner only");
		put(1, new ItemStack(Items.dye, 1, 1));
		assertEqual(NO_ITEM_REQUIRED.toString(), offered(), "a banner and a dye");
	}

	/** Picking a pattern (the screen's button) puts one banner with that layer, in the dye's colour, in the result. */
	public void testPickingMakesTheResult() {
		open();
		put(0, banner(0));
		put(1, new ItemStack(Items.dye, 1, 1));
		clearSounds();
		pick(4);
		waitUntil("the pick on the server", () -> loom().selected() == 4, 40);
		ItemStack result = onServer(() -> loom().resultSlot.getStack());
		assertTrue(result != null && result.stackSize == 1, "the result " + result);
		assertEqual(0, TileEntityBanner.getBaseColor(result), "its base colour (white)");
		assertEqual("bs", BannerTests.layer(result, 0), "its layer (26.3's stripe_bottom)");
		assertEqual(14, colour(result, 0), "the layer's colour (red)");
		waitForSound(McFly.MC_ASSETS + ":ui.loom.select_pattern", 20);
	}

	/** Taking the result uses one banner and one dye, plays the loom's sound, and the pick goes when one runs out. */
	public void testTakingTheResult() {
		open();
		put(0, banner(0, 2));
		put(1, new ItemStack(Items.dye, 1, 1));
		pick(0);
		waitUntil("the result", () -> loom().resultSlot.getHasStack(), 40);
		clearSounds();
		// shift-click the result, as the mouse would
		onClient(() -> mc().playerController.windowClick(mc().thePlayer.openContainer.windowId, 3, 0, 1, mc().thePlayer));
		waitUntil("the banner taken", () -> countPatterned() == 1, 40);
		assertEqual(1, onServer(() -> loom().bannerSlot.getStack() == null ? 0 : loom().bannerSlot.getStack().stackSize), "banners left");
		assertTrue(onServer(() -> !loom().dyeSlot.getHasStack()), "the dye used");
		assertEqual(-1, onServer(() -> loom().selected()), "the pick, once the dye ran out");
		waitForSound(McFly.MC_ASSETS + ":ui.loom.take_result", 20);
	}

	/** A banner with 6 layers takes no more: no result, whatever is picked. */
	public void testSixLayersTakeNoMore() {
		open();
		ItemStack full = banner(0);
		for (int i = 0; i < 6; i++) {
			addLayer(full, "bs", 14);
		}
		put(0, full);
		put(1, new ItemStack(Items.dye, 1, 1));
		pick(2);
		ticks(5);
		assertTrue(onServer(() -> !loom().resultSlot.getHasStack()), "a result for a banner of 6 layers");
	}

	/** Shift-clicked from the inventory, a banner goes to the banner slot and a dye to the dye slot. */
	public void testShiftClickGoesToItsSlot() {
		open();
		onServer(() -> {
			player().inventory.setInventorySlotContents(9, banner(3));
			player().inventory.setInventorySlotContents(10, new ItemStack(Items.dye, 4, 2));
			player().inventoryContainer.detectAndSendChanges();
			player().openContainer.detectAndSendChanges();
		});
		ticks(3);
		// the inventory's slot 9 is the loom's slot 4 (its first player slot), slot 10 is 5
		onClient(() -> mc().playerController.windowClick(mc().thePlayer.openContainer.windowId, 4, 0, 1, mc().thePlayer));
		onClient(() -> mc().playerController.windowClick(mc().thePlayer.openContainer.windowId, 5, 0, 1, mc().thePlayer));
		ticks(3);
		assertTrue(onServer(() -> loom().bannerSlot.getHasStack()), "the banner in its slot");
		assertEqual(4, onServer(() -> loom().dyeSlot.getHasStack() ? loom().dyeSlot.getStack().stackSize : 0), "the dyes in theirs");
	}

	/** Closing the loom gives its inputs back to the player. */
	public void testClosingGivesTheItemsBack() {
		open();
		put(0, banner(5));
		put(1, new ItemStack(Items.dye, 2, 4));
		onClient(() -> mc().thePlayer.closeScreen());
		ticks(3);
		int[] back = onServer(() -> {
			int banners = 0, dyes = 0;
			for (ItemStack stack : player().inventory.mainInventory) {
				if (stack != null && stack.getItem() == banner(0).getItem()) {
					banners += stack.stackSize;
				}
				if (stack != null && stack.getItem() == Items.dye) {
					dyes += stack.stackSize;
				}
			}
			return new int[] {banners, dyes};
		});
		assertEqual("1 2", back[0] + " " + back[1], "the banner and dyes back");
	}

	/** For a look: a white banner and red dye in, the stripe picked; beside 26.3's in the comparison (`loom.screen`). */
	public void testScreenshots() {
		open();
		put(0, banner(0));
		put(1, new ItemStack(Items.dye, 1, 1));
		pick(4);
		ticks(10);
		String shot = screenshot("loom.screen");
		assertTrue(shot != null, "no picture taken");
	}

	// ---------------------------------------------------------------- stage 2: the pattern items (spec §66.6)

	/** 26.3's six pattern items with recipes: id, name, rarity, what goes with paper, the pattern (EFR's name). */
	private static final Object[][] PATTERN_ITEMS = {
		{"flower_banner_pattern", "Flower Charge Banner Pattern", EnumRarity.common, new ItemStack(Blocks.red_flower, 1, 8), "flower"},
		{"creeper_banner_pattern", "Creeper Charge Banner Pattern", EnumRarity.uncommon, new ItemStack(Items.skull, 1, 4), "creeper"},
		{"skull_banner_pattern", "Skull Charge Banner Pattern", EnumRarity.rare, new ItemStack(Items.skull, 1, 1), "skull"},
		{"mojang_banner_pattern", "Thing Banner Pattern", EnumRarity.rare, new ItemStack(Items.golden_apple, 1, 1), "mojang"},
		{"field_masoned_banner_pattern", "Field Masoned Banner Pattern", EnumRarity.common, new ItemStack(Blocks.brick_block), "bricks"},
		{"bordure_indented_banner_pattern", "Bordure Indented Banner Pattern", EnumRarity.common, new ItemStack(Blocks.vine), "curly_border"},
	};

	private static ItemStack patternItem(String id) {
		ItemIds.Entry entry = ItemIds.byId("minecraft:" + id);
		assertTrue(entry != null, "no item minecraft:" + id);
		return entry.createStack(1);
	}

	/** The six by 26.3's ids: a stack of one, 26.3's names (its .new ones) and rarities, in EFR's creative tab. */
	public void testPatternItems() {
		assertEqual("[]", wrongPatternItems(PATTERN_ITEMS).toString(), "the pattern items");
	}

	/** Each row's item (its id, name and rarity first): a stack of one, the name and rarity, in EFR's creative tab. */
	@SuppressWarnings("unchecked")
	private List<String> wrongPatternItems(Object[][] rows) {
		List<ItemStack> tab = onClient(() -> {
			List<ItemStack> stacks = new ArrayList<>();
			for (Object item : net.minecraft.item.Item.itemRegistry) {
				((net.minecraft.item.Item) item).getSubItems((net.minecraft.item.Item) item, EtFuturum.creativeTabItems, stacks);
			}
			return stacks;
		});
		List<String> wrong = new ArrayList<>();
		for (Object[] row : rows) {
			ItemStack stack = patternItem((String) row[0]);
			if (stack.getMaxStackSize() != 1) {
				wrong.add(row[0] + " stacks to " + stack.getMaxStackSize());
			}
			if (!row[1].equals(onClient(stack::getDisplayName))) {
				wrong.add(row[0] + " is named '" + onClient(stack::getDisplayName) + "'");
			}
			if (stack.getRarity() != row[2]) {
				wrong.add(row[0] + " is " + stack.getRarity());
			}
			boolean listed = false;
			for (ItemStack shown : tab) {
				listed |= shown.getItem() == stack.getItem();
			}
			if (!listed) {
				wrong.add(row[0] + " isn't in EFR's creative tab");
			}
		}
		return wrong;
	}

	/** Each from paper and its thing (26.3's shapeless recipes). */
	public void testPatternRecipes() {
		List<String> wrong = new ArrayList<>();
		for (Object[] row : PATTERN_ITEMS) {
			ItemStack thing = (ItemStack) row[3];
			ItemStack made = onServer(() -> BannerTests.craft(new ItemStack[] {new ItemStack(Items.paper), thing, null}, new ItemStack[3], new ItemStack[3]));
			if (made == null || made.getItem() != patternItem((String) row[0]).getItem()) {
				wrong.add("paper and " + thing + " made " + made);
			}
		}
		assertEqual("[]", wrong.toString(), "the pattern recipes");
	}

	/**
	 * A pattern item in the loom offers its pattern alone, picked by itself; the result has it; taking the result
	 * leaves the pattern item (26.3 doesn't use it up).
	 */
	public void testPatternItemInTheLoom() {
		open();
		put(0, banner(0, 2));
		put(1, new ItemStack(Items.dye, 2, 4));
		put(2, patternItem("flower_banner_pattern"));
		assertEqual("[flower]", offered(), "offered with the flower charge pattern in");
		assertEqual(0, onServer(() -> loom().selected()), "picked by itself");
		ItemStack result = onServer(() -> loom().resultSlot.getStack());
		assertEqual("flo", BannerTests.layer(result, 0), "the result's layer");
		onClient(() -> mc().playerController.windowClick(mc().thePlayer.openContainer.windowId, 3, 0, 1, mc().thePlayer));
		waitUntil("the banner taken", () -> countPatterned() >= 1, 40);
		assertTrue(onServer(() -> loom().patternSlot.getHasStack()), "the pattern item is still in");
	}

	/** For a look: a white banner, blue dye and the flower charge pattern in; beside 26.3's (`loom.pattern_item`). */
	public void testScreenshotsWithAPatternItem() {
		open();
		put(0, banner(0));
		put(1, new ItemStack(Items.dye, 1, 4));
		put(2, patternItem("flower_banner_pattern"));
		ticks(10);
		String shot = screenshot("loom.pattern_item");
		assertTrue(shot != null, "no picture taken");
	}

	// ---------------------------------------------------------------- stage 5: the four new patterns (spec §66.11)

	/** 26.3's four pattern items with no recipe: id, name (its .new), rarity, the pattern, EFR's short id. */
	private static final Object[][] NEW_PATTERN_ITEMS = {
		{"globe_banner_pattern", "Globe Banner Pattern", EnumRarity.common, "globe", "glb"},
		{"piglin_banner_pattern", "Snout Banner Pattern", EnumRarity.uncommon, "piglin", "pig"},
		{"flow_banner_pattern", "Flow Banner Pattern", EnumRarity.rare, "flow", "flw"},
		{"guster_banner_pattern", "Guster Banner Pattern", EnumRarity.rare, "guster", "gus"},
	};

	/** The four by 26.3's ids, as the six; no crafting recipe makes them (for now, §66.4: creative and /give). */
	public void testTheFourNewPatternItems() {
		List<String> wrong = wrongPatternItems(NEW_PATTERN_ITEMS);
		for (Object[] row : NEW_PATTERN_ITEMS) {
			net.minecraft.item.Item item = patternItem((String) row[0]).getItem();
			for (Object recipe : net.minecraft.item.crafting.CraftingManager.getInstance().getRecipeList()) {
				ItemStack output = ((net.minecraft.item.crafting.IRecipe) recipe).getRecipeOutput();
				if (output != null && output.getItem() == item) {
					wrong.add(row[0] + " is made by " + recipe);
				}
			}
		}
		assertEqual("[]", wrong.toString(), "the four new pattern items");
	}

	/** Each in the loom offers its pattern alone, picked by itself; the result has it. */
	public void testTheFourInTheLoom() {
		open();
		List<String> wrong = new ArrayList<>();
		for (Object[] row : NEW_PATTERN_ITEMS) {
			put(0, banner(0));
			put(1, new ItemStack(Items.dye, 1, 1));
			put(2, patternItem((String) row[0]));
			String offered = offered();
			if (!offered.equals("[" + row[3] + "]")) {
				wrong.add(row[0] + " offers " + offered);
				continue;
			}
			int selected = onServer(() -> loom().selected());
			ItemStack result = onServer(() -> loom().resultSlot.getStack());
			if (selected != 0 || !row[4].equals(BannerTests.layer(result, 0))) {
				wrong.add(row[0] + ": picked " + selected + ", the result's layer " + BannerTests.layer(result, 0));
			}
		}
		assertEqual("[]", wrong.toString(), "the four in the loom");
	}

	// ---------------------------------------------------------------- helpers

	private void open() {
		onServer(() -> {
			player().setPositionAndUpdate(X + 0.5, Y, Z - 1.5);
			ModBlocks.LOOM.get().onBlockActivated(world(), X, Y, Z, player(), 2, 0.5F, 0.5F, 0.0F);
		});
		waitUntil("the loom's container", () -> player().openContainer instanceof ContainerLoom, 40);
		waitUntilOnClient("the loom's screen", () -> mc().currentScreen instanceof GuiLoom, 40);
	}

	private static ContainerLoom loom() {
		return (ContainerLoom) player().openContainer;
	}

	/** A stack into one of the loom's slots on the server, then sent to the client. */
	private void put(int slot, ItemStack stack) {
		onServer(() -> {
			loom().getSlot(slot).putStack(stack);
			loom().detectAndSendChanges();
		});
		ticks(3);
	}

	/** The patterns offered, by 26.3's names (the server's). */
	private String offered() {
		return onServer(() -> {
			List<String> names = new ArrayList<>();
			for (TileEntityBanner.EnumBannerPattern pattern : loom().selectable()) {
				names.add(pattern.getPatternName());
			}
			return names.toString();
		});
	}

	/** Clicks the screen's pattern button of that index (the first rows shown), as the mouse would. */
	private void pick(int index) {
		onClient(() -> {
			GuiLoom gui = (GuiLoom) mc().currentScreen;
			int x = gui.left() + 60 + (index % 4) * 14 + 7;
			int y = gui.top() + 13 + (index / 4) * 14 + 7;
			call(gui, new String[] {"mouseClicked", "func_73864_a"}, x, y, 0);
			call(gui, new String[] {"mouseMovedOrUp", "func_146286_b"}, x, y, 0);
		});
		ticks(3);
	}

	private int countPatterned() {
		int count = 0;
		for (ItemStack stack : player().inventory.mainInventory) {
			if (stack != null && BannerTests.layer(stack, 0) != null) {
				count += stack.stackSize;
			}
		}
		return count;
	}

	private static void check(List<String> found, Slot slot, ItemStack stack, boolean takes) {
		if (slot.isItemValid(stack) != takes) {
			found.add("slot " + slot.slotNumber + (takes ? " refuses " : " takes ") + stack);
		}
	}

	private static ItemStack banner(int colour) {
		return banner(colour, 1);
	}

	private static ItemStack banner(int colour, int count) {
		return ModBlocks.BANNER.newItemStack(count, colour);
	}

	private static void addLayer(ItemStack banner, String pattern, int colour) {
		NBTTagCompound entity = banner.hasTagCompound() ? banner.getTagCompound().getCompoundTag("BlockEntityTag") : new NBTTagCompound();
		NBTTagList layers = entity.getTagList("Patterns", 10);
		NBTTagCompound layer = new NBTTagCompound();
		layer.setString("Pattern", pattern);
		layer.setInteger("Color", colour);
		layers.appendTag(layer);
		entity.setTag("Patterns", layers);
		entity.setInteger("Base", banner.getItemDamage());
		banner.setTagInfo("BlockEntityTag", entity);
	}

	private static int colour(ItemStack banner, int index) {
		NBTTagList layers = banner.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10);
		return index < layers.tagCount() ? layers.getCompoundTagAt(index).getInteger("Color") : -1;
	}
}
