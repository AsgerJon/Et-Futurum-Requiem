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
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The wood sets' hanging signs, new (SPEC-modern-movement.md 91.9, step G), as 26.3's: `<wood>_hanging_sign` (rotation in
 * the meta, `attached` in the tile entity `etfuturum.hanging_sign`) and `<wood>_wall_hanging_sign` (facing) for the 11
 * woods; placing by 26.3's `HangingSignItem` (on a ceiling, attached when the block above has no full bottom face or the
 * player sneaks, chained under another, between walls); held by the ceiling, the wall sign staying (26.3's quirk);
 * 60-pixel lines and McFly's hanging sign editor; the properties, stacks, fuel, recipes and drawing. At z 700, y 8. Read
 * through names, NBT and the screen's class, so it runs on the code before G's (and fails there: no ids).
 */
@TasSuite("woods")
public class HangingSignTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 700;
	private static final String MODERN = McFly.MC_ASSETS + ":";
	private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"};

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().setSneaking(false);
		});
		onClient(() -> mc().displayGuiScreen(null));
	}

	@Override
	protected void tearDown() {
		onClient(() -> mc().displayGuiScreen(null));
		onServer(() -> {
			clear();
			player().setSneaking(false);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		});
	}

	/** x 34 to 46, y 8 to 13, z 694 to 706 emptied, a stone floor under it, its items gone. */
	private void clear() {
		for (int x = X - 6; x <= X + 6; x++) {
			for (int z = Z - 6; z <= Z + 6; z++) {
				for (int y = Y; y <= Y + 5; y++) {
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
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 7, Y - 2, Z - 7, X + 8, Y + 7, Z + 8))) {
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

	/** The 26.3 block it reads as, with its rotation or facing. */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		StringBuilder name = new StringBuilder(found.type().id.replace("minecraft:", ""));
		if (state.containsKey("rotation")) {
			name.append(",rotation=").append(state.get("rotation"));
		}
		if (state.containsKey("facing")) {
			name.append(",facing=").append(state.get("facing"));
		}
		return name.toString();
	}

	/** What stands at the spot by 26.3's name, and its tile entity's `attached` ("", "attached"). */
	private String at(int x, int y, int z) {
		Block there = world().getBlock(x, y, z);
		if (there == Blocks.air) {
			return "none";
		}
		TileEntity tile = world().getTileEntity(x, y, z);
		boolean attached = false;
		if (tile != null) {
			NBTTagCompound tag = new NBTTagCompound();
			tile.writeToNBT(tag);
			attached = tag.getBoolean("attached");
		}
		return name(there, world().getBlockMetadata(x, y, z)) + (attached ? ",attached" : "");
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

	/** Uses the item on the face (on the server, as a click comes), the player turned and sneaking as given. */
	private void useOn(Item item, int x, int y, int z, int side, float yaw, float pitch, boolean sneaking) {
		ItemStack stack = new ItemStack(item);
		player().rotationYaw = yaw;
		player().rotationPitch = pitch;
		player().setSneaking(sneaking);
		player().inventory.setInventorySlotContents(player().inventory.currentItem, stack);
		player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), stack, x, y, z, side, 0.5F, side == 1 ? 1.0F : side == 0 ? 0.0F : 0.5F,
			0.5F);
		player().setSneaking(false);
	}

	private void standAt(double x, double z, float yaw) {
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(x, Y, z, yaw, 0.0F));
		serverTicks(3);
		ticks(3);
	}

	private static boolean isHangingEditor(GuiScreen screen) {
		return screen != null && screen.getClass().getName().startsWith("io.github.asgerjon.mcfly.") && screen.getClass().getSimpleName().contains("HangingSignEdit");
	}

	// ---------------------------------------------------------------- ours, stored form

	/** The 22 ids, the 11 items, McFly's classes; `etfuturum.hanging_sign` McFly's, made by each. */
	public void testHangingSignsAreOurs() {
		String got = onServer(() -> {
			List<String> wrong = new ArrayList<>();
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("id", "etfuturum.hanging_sign");
			TileEntity loaded = TileEntity.createAndLoadEntity(tag);
			if (loaded == null || !loaded.getClass().getName().startsWith("io.github.asgerjon.mcfly.") || !(loaded instanceof TileEntitySign)) {
				wrong.add("etfuturum.hanging_sign " + (loaded == null ? "unregistered" : loaded.getClass().getName()));
			}
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
			for (String wood : WOODS) {
				for (String name : new String[]{wood + "_hanging_sign", wood + "_wall_hanging_sign"}) {
					Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
					if (!(block instanceof Block) || block == Blocks.air) {
						wrong.add("no " + name);
						continue;
					}
					if (!block.getClass().getName().startsWith("io.github.asgerjon.mcfly.")) {
						wrong.add(name + " " + block.getClass().getName());
					}
					world().setBlock(X, Y, Z, (Block) block, name.contains("wall") ? 2 : 0, 2);
					TileEntity tile = world().getTileEntity(X, Y, Z);
					if (loaded == null || tile == null || tile.getClass() != loaded.getClass()) {
						wrong.add(name + " makes " + (tile == null ? "none" : tile.getClass().getSimpleName()));
					}
				}
				if (!(Item.itemRegistry.getObject(McFly.DOMAIN + ":" + wood + "_hanging_sign") instanceof Item)) {
					wrong.add("no item " + wood + "_hanging_sign");
				}
			}
			return wrong.toString();
		});
		assertEqual("[]", got, "ids, items, classes, the tile entity");
	}

	/** Metas by 26.3's names: the ceiling sign's rotation, the wall sign's facing (2 north, 3 south, 4 west, 5 east). */
	public void testStoredForm() {
		Object[][] metas = {{"oak_hanging_sign", 0}, {"spruce_hanging_sign", 4}, {"cherry_hanging_sign", 15}, {"oak_wall_hanging_sign", 2},
			{"crimson_wall_hanging_sign", 5}, {"bamboo_wall_hanging_sign", 3}};
		String got = onServer(() -> {
			StringBuilder names = new StringBuilder();
			for (Object[] entry : metas) {
				names.append(names.length() == 0 ? "" : " ").append(name(block((String) entry[0]), (Integer) entry[1]));
			}
			return names.toString();
		});
		assertEqual("oak_hanging_sign,rotation=0 spruce_hanging_sign,rotation=4 cherry_hanging_sign,rotation=15 oak_wall_hanging_sign,facing=north "
			+ "crimson_wall_hanging_sign,facing=east bamboo_wall_hanging_sign,facing=south", got, "26.3's names");
	}

	/** `attached` and both sides read back from the tile entity's NBT. */
	public void testAttachedAndSidesReadBack() {
		String got = onServer(() -> {
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
			world().setBlock(X, Y, Z, block("birch_hanging_sign"), 3, 2);
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("id", "etfuturum.hanging_sign");
			tag.setInteger("x", X);
			tag.setInteger("y", Y);
			tag.setInteger("z", Z);
			tag.setString("Text1", "Front");
			tag.setBoolean("attached", true);
			NBTTagCompound back = new NBTTagCompound();
			net.minecraft.nbt.NBTTagList messages = new net.minecraft.nbt.NBTTagList();
			for (String line : new String[]{"Back", "", "", ""}) {
				messages.appendTag(new net.minecraft.nbt.NBTTagString(line));
			}
			back.setTag("messages", messages);
			back.setString("color", "red");
			tag.setTag("back_text", back);
			TileEntity tile = world().getTileEntity(X, Y, Z);
			if (tile == null) {
				return "no tile entity";
			}
			tile.readFromNBT(tag);
			NBTTagCompound saved = new NBTTagCompound();
			tile.writeToNBT(saved);
			return saved.getString("id") + " " + saved.getBoolean("attached") + " " + saved.getString("Text1") + " "
				+ saved.getCompoundTag("back_text").getTagList("messages", 8).getStringTagAt(0) + " " + saved.getCompoundTag("back_text").getString("color");
		});
		assertEqual("etfuturum.hanging_sign true Front Back red", got, "id, attached, front line, back line and colour, saved again");
	}

	// ---------------------------------------------------------------- placing, holding, chaining

	/**
	 * Under a full block, unattached, facing away (26.3: the nearest of four, opposite the player's): looking south 0,
	 * west 4. Sneaking, attached, turned by the yaw (30 degrees: 9). Under a fence (no full bottom face), attached.
	 */
	public void testPlacingOnACeiling() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			Item sign = item("oak_hanging_sign");
			Object[][] cases = {{Blocks.stone, 0.0F, false}, {Blocks.stone, 90.0F, false}, {Blocks.stone, 30.0F, true}, {Blocks.fence, 30.0F, false}};
			for (Object[] c : cases) {
				world().setBlock(X, Y + 1, Z, (Block) c[0], 0, 2);
				useOn(sign, X, Y + 1, Z, 0, (Float) c[1], -60.0F, (Boolean) c[2]);
				result.append(result.length() == 0 ? "" : " ").append(at(X, Y, Z));
				world().setBlock(X, Y, Z, Blocks.air, 0, 2);
				world().setBlock(X, Y + 1, Z, Blocks.air, 0, 2);
			}
			return result.toString();
		});
		assertEqual("oak_hanging_sign,rotation=0 oak_hanging_sign,rotation=4 oak_hanging_sign,rotation=9,attached oak_hanging_sign,rotation=9,attached", got,
			"under stone looking south, looking west, sneaking at 30 degrees; under a fence at 30 degrees");
	}

	/** Chaining: a hanging sign item on a ceiling sign's underside places one below it, hung from its chains (same axis). */
	public void testChaining() {
		String got = onServer(() -> {
			world().setBlock(X, Y + 3, Z, Blocks.stone, 0, 2);
			Item sign = item("spruce_hanging_sign");
			useOn(sign, X, Y + 3, Z, 0, 0.0F, -60.0F, false);
			String first = at(X, Y + 2, Z);
			useOn(sign, X, Y + 2, Z, 0, 0.0F, -60.0F, false);
			return first + " " + at(X, Y + 1, Z);
		});
		assertEqual("spruce_hanging_sign,rotation=0 spruce_hanging_sign,rotation=0", got, "the first under stone, the second under it");
	}

	/** Between walls: on a wall's side, a wall sign facing along the other axis; in the open, nothing. */
	public void testPlacingBetweenWalls() {
		String got = onServer(() -> {
			Item sign = item("cherry_hanging_sign");
			world().setBlock(X - 1, Y, Z, Blocks.stone, 0, 2);
			useOn(sign, X - 1, Y, Z, 5, 0.0F, 0.0F, false);
			String onTheWall = at(X, Y, Z);
			world().setBlock(X, Y, Z, Blocks.air, 0, 2);
			world().setBlock(X - 1, Y, Z, Blocks.air, 0, 2);
			useOn(sign, X, Y - 1, Z, 1, 0.0F, 60.0F, false);
			return onTheWall + " " + at(X, Y, Z);
		});
		assertEqual("cherry_wall_hanging_sign,facing=north none", got, "against a wall to the west looking south; on the open floor");
	}

	/** The ceiling sign drops its item when the block above goes; the wall sign stays when its wall goes (26.3's). */
	public void testHeldAndDrops() {
		String got = onServer(() -> {
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
			world().setBlock(X, Y, Z, block("acacia_hanging_sign"), 0, 3);
			world().setBlock(X, Y + 1, Z, Blocks.air, 0, 3);
			String ceiling = (world().getBlock(X, Y, Z) == Blocks.air ? "fell " : "stayed ") + dropped();
			world().setBlock(X, Y, Z, Blocks.air, 0, 2);
			world().setBlock(X - 1, Y, Z, Blocks.stone, 0, 2);
			world().setBlock(X, Y, Z, block("acacia_wall_hanging_sign"), 2, 3);
			world().setBlock(X - 1, Y, Z, Blocks.air, 0, 3);
			String wall = world().getBlock(X, Y, Z) == Blocks.air ? "fell" : "stayed";
			return ceiling + " " + wall;
		});
		assertEqual("fell {acacia_hanging_sign=1} stayed", got, "without the ceiling; without the wall");
	}

	// ---------------------------------------------------------------- the editor

	/** Placing opens McFly's hanging sign editor; a line holds 60 pixels (26.3's `HangingSignBlockEntity`). */
	public void testEditorAnd60Pixels() {
		onServer(() -> world().setBlock(X, Y + 2, Z, Blocks.stone, 0, 2));
		standAt(X + 0.5D, Z + 2.5D, 180.0F);
		setHeld(new ItemStack(item("jungle_hanging_sign")));
		ticks(2);
		onServer(() -> {
			ItemStack held = player().getHeldItem();
			player().rotationYaw = 180.0F;
			player().rotationPitch = -60.0F;
			player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), held, X, Y + 2, Z, 0, 0.5F, 0.0F, 0.5F);
		});
		waitUntilOnClient("McFly's hanging sign editor open", () -> isHangingEditor(mc().currentScreen), 40);
		onClient(() -> {
			for (int i = 0; i < 50; i++) {
				keyTyped(mc().currentScreen, 'i', 0);
			}
		});
		String line = onClient(() -> {
			TileEntity tile = mc().theWorld.getTileEntity(X, Y + 1, Z);
			NBTTagCompound tag = new NBTTagCompound();
			if (tile != null) {
				tile.writeToNBT(tag);
			}
			return tag.getString("Text1");
		});
		int[] widths = onClient(() -> new int[]{mc().fontRenderer.getStringWidth(line), mc().fontRenderer.getStringWidth(line + "i")});
		assertTrue(widths[0] <= 60 && widths[1] > 60, "the line's width " + widths[0] + " (" + line.length() + " characters), one more " + widths[1]);
		onClient(() -> keyTyped(mc().currentScreen, '\0', Keyboard.KEY_ESCAPE));
		serverTicks(3);
	}

	/** Waxed: `block.hanging_sign.waxed_interact_fail`, no editor. */
	public void testWaxedSound() {
		onServer(() -> {
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
			world().setBlock(X, Y, Z, block("warped_hanging_sign"), 0, 3);
			NBTTagCompound tag = new NBTTagCompound();
			TileEntity tile = world().getTileEntity(X, Y, Z);
			tile.writeToNBT(tag);
			tag.setString("Text1", "x");
			tag.setBoolean("Waxed", true);
			tile.readFromNBT(tag);
		});
		standAt(X + 0.5D, Z + 2.5D, 180.0F);
		setHeld(null);
		clearSounds();
		onServer(() -> player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), null, X, Y, Z, 2, 0.5F, 0.5F, 0.5F));
		waitForSound(MODERN + "block.hanging_sign.waxed_interact_fail", 20);
		ticks(5);
		assertFalse(onClient(() -> isHangingEditor(mc().currentScreen)), "a waxed sign's editor opened");
	}

	// ---------------------------------------------------------------- properties, items, recipes, drawing

	/** 26.3's: 1.0, no fire, the hanging sign sounds by wood, map colours by wood. */
	public void testProperties() {
		String[] sounds = {"block.hanging_sign.break", "block.hanging_sign.break", "block.hanging_sign.break", "block.hanging_sign.break",
			"block.hanging_sign.break", "block.hanging_sign.break", "block.hanging_sign.break", "block.cherry_wood_hanging_sign.break",
			"block.bamboo_wood_hanging_sign.break", "block.nether_wood_hanging_sign.break", "block.nether_wood_hanging_sign.break"};
		MapColor[] colours = {MapColor.woodColor, MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor, MapColor.brownColor,
			MapColor.redColor, MapColor.redColor, MapColor.yellowColor, MapColor.netherrackColor, MapColor.cyanColor};
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
			for (int i = 0; i < WOODS.length; i++) {
				Block block = block(WOODS[i] + "_hanging_sign");
				world().setBlock(X, Y, Z, block, 0, 2);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null) + " "
					+ block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP) + " " + block.getFlammability(world(), X, Y, Z, ForgeDirection.UP) + " "
					+ breakSound(X, Y, Z).replace(MODERN, "");
				if (!("1.0 1.0 0 0 " + sounds[i]).equals(got) || block.getMapColor(0) != colours[i]) {
					found.add(WOODS[i] + " " + got + " colour " + block.getMapColor(0).colorIndex);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, resistance, fire, sound, colour (want: as listed)");
	}

	/**
	 * Shapes: the ceiling sign at rotation 0 its board (x 1 to 15, y 0 to 10, z 7 to 9), at rotation 1 a 10-by-10 column;
	 * the wall sign facing north its plank and board (x 0 to 16, y 0 to 16, z 6 to 10). Collision: none for the ceiling
	 * sign, the plank (y 14 to 16) for the wall sign.
	 */
	public void testShapesAndCollision() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
			for (Object[] entry : new Object[][]{{"oak_hanging_sign", 0}, {"oak_hanging_sign", 1}, {"oak_wall_hanging_sign", 2}}) {
				Block block = block((String) entry[0]);
				world().setBlock(X, Y, Z, block, (Integer) entry[1], 2);
				block.setBlockBoundsBasedOnState(world(), X, Y, Z);
				result.append(result.length() == 0 ? "" : " ").append(String.format(java.util.Locale.ROOT, "%.0f-%.0f/%.0f-%.0f/%.0f-%.0f",
					block.getBlockBoundsMinX() * 16, block.getBlockBoundsMaxX() * 16, block.getBlockBoundsMinY() * 16, block.getBlockBoundsMaxY() * 16,
					block.getBlockBoundsMinZ() * 16, block.getBlockBoundsMaxZ() * 16));
				List<AxisAlignedBB> boxes = new ArrayList<>();
				block.addCollisionBoxesToList(world(), X, Y, Z, AxisAlignedBB.getBoundingBox(X - 1, Y - 1, Z - 1, X + 2, Y + 2, Z + 2), boxes, null);
				result.append(boxes.isEmpty() ? " passable" : String.format(java.util.Locale.ROOT, " solid %.0f-%.0f", (boxes.get(0).minY - Y) * 16,
					(boxes.get(0).maxY - Y) * 16));
			}
			return result.toString();
		});
		assertEqual("1-15/0-10/7-9 passable 3-13/0-16/3-13 passable 0-16/0-16/6-10 solid 14-16", got, "x/y/z in pixels, then collision's y");
	}

	/** 26.3's: stacks of 16, fuel 800 (crimson's and warped's none). */
	public void testStacksAndFuel() {
		StringBuilder got = new StringBuilder();
		for (String wood : new String[]{"oak", "bamboo", "crimson"}) {
			ItemStack stack = new ItemStack(item(wood + "_hanging_sign"));
			got.append(got.length() == 0 ? "" : ", ").append(wood).append(" ").append(stack.getMaxStackSize()).append(" ").append(TileEntityFurnace.getItemBurnTime(stack));
		}
		assertEqual("oak 16 800, bamboo 16 800, crimson 16 0", got.toString(), "stack size, burn time");
	}

	/** 26.3's (its jar's, `woods_hanging_signs_recipes_26_3.txt`): two chains over six stripped logs, 6. */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("woods_hanging_signs_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(11, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}

	public void testNames() {
		String got = onClient(() -> {
			StringBuilder names = new StringBuilder();
			for (String wood : new String[]{"oak", "dark_oak", "crimson"}) {
				names.append(names.length() == 0 ? "" : ", ").append(new ItemStack(item(wood + "_hanging_sign")).getDisplayName());
			}
			return names.toString();
		});
		assertEqual("Oak Hanging Sign, Dark Oak Hanging Sign, Crimson Hanging Sign", got, "names");
	}

	/** Drawn as 26.3's: McFly's models in the chunk, the text by McFly's renderer for its tile entity. */
	public void testDrawing() {
		String got = onClient(() -> {
			StringBuilder result = new StringBuilder();
			for (String name : new String[]{"oak_hanging_sign", "oak_wall_hanging_sign"}) {
				if (block(name).getRenderType() != McFlyRenderIds.MODEL) {
					result.append(name).append(" render type ").append(block(name).getRenderType()).append("; ");
				}
			}
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("id", "etfuturum.hanging_sign");
			TileEntity tile = TileEntity.createAndLoadEntity(tag);
			Object renderer = tile == null ? null : TileEntityRendererDispatcher.instance.getSpecialRenderer(tile);
			if (renderer == null || !renderer.getClass().getName().startsWith("io.github.asgerjon.mcfly.")) {
				result.append("the text renderer ").append(renderer == null ? "none" : renderer.getClass().getName());
			}
			return result.toString();
		});
		assertEqual("", got, "render types and the text renderer");
	}
}
