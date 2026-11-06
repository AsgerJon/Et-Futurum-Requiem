package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The wood sets' shelves, new (SPEC-modern-movement.md 91.10, step H), as 26.3's: `<wood>_shelf` for the 11 woods, facing
 * and powered in the meta, the three items and 26.3's `side_chain` in the tile entity `etfuturum.shelf`; placing facing
 * the player; using it (a slot by the third of the front hit; put in, take out, swap; the whole chain against the hotbar
 * when powered); chains of up to 3 joined in the order powered; the comparator; drops, properties, fuel, recipes, names,
 * drawing. At z 720, y 8; the shelves face north, so a chain's left is east. Read through names and NBT, so it runs on
 * the code before H's (and fails there: no ids).
 */
@TasSuite("woods")
public class ShelfTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 720;
	private static final String MODERN = McFly.MC_ASSETS + ":";
	private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "crimson", "warped", "mangrove", "cherry", "bamboo"};
	/** 26.3's `SideChainPart` by its order. */
	private static final String[] CHAIN = {"unconnected", "right", "center", "left"};

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().inventory.clearInventory(null, -1);
			player().inventory.currentItem = 0;
			// in front of the shelves (they face north), so their sounds are heard
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z - 2.5D, 0.0F, 0.0F);
		});
		serverTicks(2);
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear();
			player().inventory.clearInventory(null, -1);
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	/** x 34 to 46, y 8 to 10, z 714 to 726 emptied, a stone floor under it, its items gone. */
	private void clear() {
		for (int x = X - 6; x <= X + 6; x++) {
			for (int z = Z - 6; z <= Z + 6; z++) {
				for (int y = Y; y <= Y + 2; y++) {
					world().setBlock(x, y, z, Blocks.air, 0, 2);
				}
				world().setBlock(x, Y - 1, z, Blocks.stone, 0, 2);
			}
		}
		for (EntityItem item : items()) {
			item.setDead();
		}
	}

	private List<EntityItem> items() {
		List<EntityItem> found = new ArrayList<>();
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 7, Y - 2, Z - 7, X + 8, Y + 4, Z + 8))) {
			if (!((EntityItem) entity).isDead) {
				found.add((EntityItem) entity);
			}
		}
		return found;
	}

	private String dropped() {
		Map<String, Integer> counts = new java.util.TreeMap<>();
		for (EntityItem item : items()) {
			ItemStack stack = item.getEntityItem();
			counts.merge(Recipes26_3.nameOf(stack), stack.stackSize, Integer::sum);
			item.setDead();
		}
		return counts.toString();
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(name.contains(":") ? name : McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + name);
		return (Block) block;
	}

	private static Item item(String name) {
		Object item = Item.itemRegistry.getObject(name.contains(":") ? name : McFly.DOMAIN + ":" + name);
		assertTrue(item instanceof Item, "no item " + name);
		return (Item) item;
	}

	/** The 26.3 block it reads as, with its facing and powered. */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		StringBuilder name = new StringBuilder(found.type().id.replace("minecraft:", ""));
		if (state.containsKey("facing")) {
			name.append(",facing=").append(state.get("facing"));
		}
		if ("true".equals(state.get("powered"))) {
			name.append(",powered");
		}
		return name.toString();
	}

	private static NBTTagCompound saved(TileEntity tile) {
		NBTTagCompound tag = new NBTTagCompound();
		tile.writeToNBT(tag);
		return tag;
	}

	/** The shelf's three slots by 26.3's item names ("-" empty), from its NBT. */
	private String slots(int x) {
		return onServer(() -> {
			TileEntity tile = world().getTileEntity(x, Y, Z);
			if (tile == null) {
				return "no tile entity";
			}
			String[] names = {"-", "-", "-"};
			NBTTagList items = saved(tile).getTagList("Items", 10);
			for (int i = 0; i < items.tagCount(); i++) {
				NBTTagCompound entry = items.getCompoundTagAt(i);
				int slot = entry.getByte("Slot") & 255;
				ItemStack stack = ItemStack.loadItemStackFromNBT(entry);
				if (slot < 3 && stack != null) {
					names[slot] = Recipes26_3.nameOf(stack);
				}
			}
			return String.join(" ", names);
		});
	}

	/** The shelf's 26.3 `side_chain`, from its NBT. */
	private String chain(int x) {
		return onServer(() -> {
			TileEntity tile = world().getTileEntity(x, Y, Z);
			int part = tile == null ? -1 : saved(tile).getInteger("side_chain");
			return part >= 0 && part < 4 ? CHAIN[part] : "none";
		});
	}

	/** An oak shelf facing north (meta 0) at x. */
	private void shelf(int x) {
		onServer(() -> world().setBlock(x, Y, Z, block("oak_shelf"), 0, 3));
	}

	/** Powers or unpowers the shelf at x by a redstone block under it. */
	private void power(int x, boolean on) {
		onServer(() -> world().setBlock(x, Y - 1, Z, on ? Blocks.redstone_block : Blocks.stone, 0, 3));
		serverTicks(2);
	}

	/** The player uses what's in their hand on the shelf's front (north) face, at a point across it (0 west to 1 east). */
	private void useFront(int x, float across) {
		onServer(() -> player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), player().getHeldItem(), x, Y, Z, 2, across, 0.5F, 0.0F));
	}

	private void hold(ItemStack stack) {
		onServer(() -> player().inventory.setInventorySlotContents(player().inventory.currentItem, stack));
	}

	private String heldName() {
		return onServer(() -> player().getHeldItem() == null ? "-" : Recipes26_3.nameOf(player().getHeldItem()));
	}

	private String breakSound(int x, int y, int z) {
		Block block = world().getBlock(x, y, z);
		String own = block.stepSound.getBreakSound();
		Object variants = MultiBlockSoundRegistry.multiBlockSounds.get(block);
		if (variants == null) {
			return own;
		}
		String got = ((MultiBlockSoundContainer) variants).getSound(world(), x, y, z, own, MultiBlockSoundRegistry.BlockSoundType.BREAK);
		return got != null ? got : own;
	}

	// ---------------------------------------------------------------- ours, stored form

	public void testShelvesAreOurs() {
		String got = onServer(() -> {
			List<String> wrong = new ArrayList<>();
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("id", "etfuturum.shelf");
			TileEntity loaded = TileEntity.createAndLoadEntity(tag);
			if (loaded == null || !loaded.getClass().getName().startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add("etfuturum.shelf " + (loaded == null ? "unregistered" : loaded.getClass().getName()));
			}
			for (String wood : WOODS) {
				Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + wood + "_shelf");
				if (!(block instanceof Block) || block == Blocks.air) {
					wrong.add("no " + wood + "_shelf");
					continue;
				}
				if (!block.getClass().getName().startsWith("io.github.asgerjon.mcfly.")) {
					wrong.add(wood + "_shelf " + block.getClass().getName());
				}
				world().setBlock(X, Y, Z, (Block) block, 0, 2);
				TileEntity tile = world().getTileEntity(X, Y, Z);
				if (loaded == null || tile == null || tile.getClass() != loaded.getClass()) {
					wrong.add(wood + "_shelf makes " + (tile == null ? "none" : tile.getClass().getSimpleName()));
				}
				if (!(Item.itemRegistry.getObject(McFly.DOMAIN + ":" + wood + "_shelf") instanceof Item)) {
					wrong.add("no item " + wood + "_shelf");
				}
			}
			return wrong.toString();
		});
		assertEqual("[]", got, "ids, items, classes, the tile entity");
	}

	/** Metas by 26.3's names: 0 north, 1 south, 2 west, 3 east, +4 powered. */
	public void testStoredForm() {
		String got = onServer(() -> {
			StringBuilder names = new StringBuilder();
			for (int meta : new int[]{0, 1, 2, 3, 4, 7}) {
				names.append(names.length() == 0 ? "" : " ").append(name(block("cherry_shelf"), meta));
			}
			return names.toString();
		});
		assertEqual("cherry_shelf,facing=north cherry_shelf,facing=south cherry_shelf,facing=west cherry_shelf,facing=east cherry_shelf,facing=north,powered "
			+ "cherry_shelf,facing=east,powered", got, "26.3's names");
	}

	/** The tile entity's items and `side_chain` read back. */
	public void testNbtReadBack() {
		shelf(X);
		String got = onServer(() -> {
			TileEntity tile = world().getTileEntity(X, Y, Z);
			if (tile == null) {
				return "no tile entity";
			}
			NBTTagCompound tag = saved(tile);
			NBTTagList items = new NBTTagList();
			NBTTagCompound entry = new NBTTagCompound();
			new ItemStack(Items.diamond).writeToNBT(entry);
			entry.setByte("Slot", (byte) 2);
			items.appendTag(entry);
			tag.setTag("Items", items);
			tag.setInteger("side_chain", 2);
			tile.readFromNBT(tag);
			return "ok";
		});
		assertEqual("ok", got, "the shelf");
		assertEqual("- - diamond", slots(X), "the slots");
		assertEqual("center", chain(X), "side_chain");
	}

	// ---------------------------------------------------------------- placing, using

	/** Facing the player (looking south: facing north, 0); powered when placed beside a signal. */
	public void testPlacing() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (boolean powered : new boolean[]{false, true}) {
				if (powered) {
					world().setBlock(X + 1, Y, Z, Blocks.redstone_block, 0, 3);
				}
				ItemStack stack = new ItemStack(item("spruce_shelf"));
				player().rotationYaw = 0.0F;
				player().inventory.setInventorySlotContents(player().inventory.currentItem, stack);
				player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), stack, X, Y - 1, Z, 1, 0.5F, 1.0F, 0.5F);
				Block there = world().getBlock(X, Y, Z);
				result.append(result.length() == 0 ? "" : " ").append(there == Blocks.air ? "none" : name(there, world().getBlockMetadata(X, Y, Z)));
				world().setBlock(X, Y, Z, Blocks.air, 0, 3);
			}
			return result.toString();
		});
		assertEqual("spruce_shelf,facing=north spruce_shelf,facing=north,powered", got, "placed looking south; beside a redstone block");
	}

	/**
	 * Unpowered: a stack put in, swapped and taken out by the third of the front hit (26.3's `getHitSlot`: across the
	 * north face, its east third slot 0 to its west third slot 2, left to right as seen from the north); the sounds; an
	 * empty hand on an empty slot and a use on the side change nothing.
	 */
	public void testUseUnpowered() {
		shelf(X);
		hold(new ItemStack(Items.diamond));
		clearSounds();
		useFront(X, 0.9F);
		waitForSound(MODERN + "block.shelf.place_item", 20);
		String afterPut = slots(X) + " held " + heldName();
		hold(new ItemStack(Items.emerald));
		clearSounds();
		useFront(X, 0.9F);
		waitForSound(MODERN + "block.shelf.single_swap", 20);
		String afterSwap = slots(X) + " held " + heldName();
		hold(null);
		clearSounds();
		useFront(X, 0.9F);
		waitForSound(MODERN + "block.shelf.take_item", 20);
		String afterTake = slots(X) + " held " + heldName();
		hold(new ItemStack(Items.gold_ingot));
		useFront(X, 0.1F);
		useFront(X, 0.5F);
		String sides = slots(X);
		onServer(() -> player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), player().getHeldItem(), X, Y, Z, 4, 0.0F, 0.5F, 0.5F));
		assertEqual("diamond - - held - | emerald - - held diamond | - - - held emerald | - - gold_ingot", afterPut + " | " + afterSwap + " | " + afterTake + " | " + sides
			+ (slots(X).equals(sides) ? "" : " (a side use changed it: " + slots(X) + ")"), "put in, swapped, taken out (the east of the north face is slot 0); then the west third");
	}

	// ---------------------------------------------------------------- power and chains

	/** Powering: the sounds; three in a row powered west to east chain right, center, left; a fourth stays apart. */
	public void testChains() {
		for (int dx = -1; dx <= 2; dx++) {
			shelf(X + dx);
		}
		clearSounds();
		power(X - 1, true);
		waitForSound(MODERN + "block.shelf.activate", 20);
		power(X, true);
		power(X + 1, true);
		power(X + 2, true);
		String chained = chain(X - 1) + " " + chain(X) + " " + chain(X + 1) + " " + chain(X + 2);
		String metas = onServer(() -> world().getBlockMetadata(X - 1, Y, Z) + "," + world().getBlockMetadata(X + 2, Y, Z));
		clearSounds();
		power(X, false);
		waitForSound(MODERN + "block.shelf.deactivate", 20);
		String unpowered = chain(X - 1) + " " + chain(X) + " " + chain(X + 1);
		assertEqual("right center left unconnected 4,4 unconnected unconnected unconnected", chained + " " + metas + " " + unpowered,
			"x-1, x, x+1, x+2 powered in that order; their metas; then x unpowered");
	}

	/** Powered, a chain of 2 swaps with the hotbar's last 6 slots, three a shelf in the chain's order; `multi_swap`. */
	public void testPoweredSwap() {
		shelf(X - 1);
		shelf(X);
		power(X - 1, true);
		power(X, true);
		Item[] items = {Items.diamond, Items.emerald, Items.gold_ingot, Items.iron_ingot, Items.coal, Items.redstone};
		onServer(() -> {
			for (int i = 0; i < 6; i++) {
				player().inventory.setInventorySlotContents(3 + i, new ItemStack(items[i]));
			}
			player().inventory.currentItem = 0;
		});
		clearSounds();
		useFront(X, 0.5F);
		waitForSound(MODERN + "block.shelf.multi_swap", 20);
		String hotbar = onServer(() -> {
			StringBuilder slots = new StringBuilder();
			for (int i = 3; i < 9; i++) {
				slots.append(player().inventory.getStackInSlot(i) == null ? "-" : "x");
			}
			return slots.toString();
		});
		assertEqual("diamond emerald gold_ingot | iron_ingot coal redstone | ------", slots(X) + " | " + slots(X - 1) + " | " + hotbar,
			"x (the chain's left end, first) took slots 3 to 5, x-1 took 6 to 8");
	}

	/** A comparator behind it reads the three slots as bits: slots 0 and 2 filled by the player, 5. */
	public void testComparator() {
		shelf(X);
		// behind it (south), reading from the north (1.7.10's diode meta 2); then the items, which it must hear of
		onServer(() -> world().setBlock(X, Y, Z + 1, Blocks.unpowered_comparator, 2, 3));
		serverTicks(2);
		hold(new ItemStack(Items.diamond));
		useFront(X, 0.9F);
		hold(new ItemStack(Items.diamond));
		useFront(X, 0.1F);
		serverTicks(6);
		int signal = onServer(() -> {
			TileEntity comparator = world().getTileEntity(X, Y, Z + 1);
			return comparator instanceof TileEntityComparator c ? c.getOutputSignal() : -1;
		});
		assertEqual(5, signal, "the comparator's signal");
	}

	/** Broken, it drops itself and its items. */
	public void testDrops() {
		shelf(X);
		hold(new ItemStack(Items.diamond));
		useFront(X, 0.5F);
		String got = onServer(() -> {
			player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
			return dropped();
		});
		assertEqual("{diamond=1, oak_shelf=1}", got, "drops by hand");
	}

	// ---------------------------------------------------------------- properties, items, recipes, drawing

	/** 26.3's: 2.0 and 3.0, fire 30/20 (crimson's and warped's none), the shelf's sound, its planks' colour. */
	public void testProperties() {
		MapColor[] colours = {MapColor.woodColor, MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor, MapColor.brownColor,
			MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.clothColor, MapColor.yellowColor};
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (int i = 0; i < WOODS.length; i++) {
				Block block = block(WOODS[i] + "_shelf");
				world().setBlock(X, Y, Z, block, 0, 2);
				boolean burns = !WOODS[i].equals("crimson") && !WOODS[i].equals("warped");
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null) + " "
					+ block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP) + " " + block.getFlammability(world(), X, Y, Z, ForgeDirection.UP) + " "
					+ breakSound(X, Y, Z).replace(MODERN, "");
				if (!("2.0 3.0 " + (burns ? "30 20" : "0 0") + " block.shelf.break").equals(got) || block.getMapColor(0) != colours[i]) {
					found.add(WOODS[i] + " " + got + " colour " + block.getMapColor(0).colorIndex);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, resistance, fire, sound, colour (want: as listed)");
	}

	/** 26.3's: stacks of 64, fuel 300 (crimson's and warped's none). */
	public void testStacksAndFuel() {
		StringBuilder got = new StringBuilder();
		for (String wood : new String[]{"oak", "bamboo", "warped"}) {
			ItemStack stack = new ItemStack(item(wood + "_shelf"));
			got.append(got.length() == 0 ? "" : ", ").append(wood).append(" ").append(stack.getMaxStackSize()).append(" ").append(TileEntityFurnace.getItemBurnTime(stack));
		}
		assertEqual("oak 64 300, bamboo 64 300, warped 64 0", got.toString(), "stack size, burn time");
	}

	/** 26.3's (its jar's, `woods_shelves_recipes_26_3.txt`): six stripped logs, a row between, 6. */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("woods_shelves_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(11, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}

	public void testNames() {
		String got = onClient(() -> {
			StringBuilder names = new StringBuilder();
			for (String wood : new String[]{"oak", "dark_oak", "warped"}) {
				names.append(names.length() == 0 ? "" : ", ").append(new ItemStack(item(wood + "_shelf")).getDisplayName());
			}
			return names.toString();
		});
		assertEqual("Oak Shelf, Dark Oak Shelf, Warped Shelf", got, "names");
	}

	/** Drawn as 26.3's: McFly's model in the chunk, its items by McFly's renderer. */
	public void testDrawing() {
		String got = onClient(() -> {
			StringBuilder result = new StringBuilder();
			if (block("oak_shelf").getRenderType() != McFlyRenderIds.MODEL) {
				result.append("render type ").append(block("oak_shelf").getRenderType()).append("; ");
			}
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("id", "etfuturum.shelf");
			TileEntity tile = TileEntity.createAndLoadEntity(tag);
			Object renderer = tile == null ? null : TileEntityRendererDispatcher.instance.getSpecialRenderer(tile);
			if (renderer == null || !renderer.getClass().getName().startsWith("io.github.asgerjon.mcfly.")) {
				result.append("the item renderer ").append(renderer == null ? "none" : renderer.getClass().getName());
			}
			return result.toString();
		});
		assertEqual("", got, "render type and the renderer");
	}
}
