package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEventListener;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.mcfly.network.McFlyNetwork;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.wood.Signs;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The wood sets' signs as our own (SPEC-modern-movement.md 91.8, step F), as 26.3's: EFR's ids, metas and tile entity
 * (`etfuturum.sign`, McFly's class whatever the options) with its NBT read back; two sides (`back_text` new); placing by
 * 26.3's nearest looking directions; the editor on placing and on use, for the side faced, 90-pixel lines, McFly's
 * open and update messages (only the editor may, formatting stripped); waxed signs; dyes by side; the properties,
 * stacks of 16, fuel, recipes; vanilla's oak sign through the same code; drawn as 26.3's block models and text. At z 680,
 * y 8. Read through names, NBT and the screen's class, so it runs on EFR's code too (and fails there).
 */
@TasSuite("woods")
public class SignTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 680;
	private static final String MODERN = McFly.MC_ASSETS + ":";
	private static final String[] WOODS = {"crimson", "warped", "mangrove", "cherry", "bamboo"};
	private static final String[] OLD = {"spruce", "birch", "jungle", "acacia", "dark_oak"};

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
		onClient(() -> mc().displayGuiScreen(null));
	}

	@Override
	protected void tearDown() {
		onClient(() -> mc().displayGuiScreen(null));
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		});
	}

	/** x 34 to 46, y 8 to 12, z 674 to 686 emptied, a stone floor under it, its items gone. */
	private void clear() {
		for (int x = X - 6; x <= X + 6; x++) {
			for (int z = Z - 6; z <= Z + 6; z++) {
				for (int y = Y; y <= Y + 4; y++) {
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
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 7, Y - 2, Z - 7, X + 8, Y + 6, Z + 8))) {
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

	// ---------------------------------------------------------------- the sign's NBT, read through 1.7.10's own calls

	private static NBTTagCompound saved(TileEntity tile) {
		NBTTagCompound tag = new NBTTagCompound();
		tile.writeToNBT(tag);
		return tag;
	}

	/** A side's lines joined by `|`, then its colour and glow: the front 1.7.10's keys, the back 26.3's `back_text`. */
	private static String side(NBTTagCompound tag, boolean front) {
		StringBuilder lines = new StringBuilder();
		if (front) {
			for (int i = 1; i <= 4; i++) {
				lines.append(i == 1 ? "" : "|").append(tag.getString("Text" + i));
			}
			String color = tag.hasKey("Color") ? tag.getString("Color") : "black";
			return lines + " " + color + (tag.getBoolean("GlowingText") ? " glowing" : " plain");
		}
		if (!tag.hasKey("back_text", 10)) {
			return "no back_text";
		}
		NBTTagCompound back = tag.getCompoundTag("back_text");
		NBTTagList messages = back.getTagList("messages", 8);
		for (int i = 0; i < messages.tagCount(); i++) {
			lines.append(i == 0 ? "" : "|").append(messages.getStringTagAt(i));
		}
		return lines + " " + back.getString("color") + (back.getBoolean("has_glowing_text") ? " glowing" : " plain");
	}

	private String serverSide(boolean front) {
		return onServer(() -> side(saved(world().getTileEntity(X, Y, Z)), front));
	}

	/** EFR's sign tag as it wrote it: `Text1` to `Text4`, the fork's `Color`, `GlowingText`, `Waxed`. */
	private static NBTTagCompound efrTag(String id, String[] lines, String color, boolean glowing, boolean waxed) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("id", id);
		tag.setInteger("x", X);
		tag.setInteger("y", Y);
		tag.setInteger("z", Z);
		for (int i = 0; i < 4; i++) {
			tag.setString("Text" + (i + 1), lines[i]);
		}
		if (color != null) {
			tag.setString("Color", color);
			tag.setBoolean("GlowingText", glowing);
			tag.setBoolean("Waxed", waxed);
		}
		return tag;
	}

	private static NBTTagCompound backText(String[] lines, String color, boolean glowing) {
		NBTTagCompound back = new NBTTagCompound();
		NBTTagList messages = new NBTTagList();
		for (String line : lines) {
			messages.appendTag(new NBTTagString(line));
		}
		back.setTag("messages", messages);
		back.setString("color", color);
		back.setBoolean("has_glowing_text", glowing);
		return back;
	}

	/** A sign at the spot, its tile entity loaded from the tag (as a chunk loads it). */
	private void signFrom(Block block, int meta, NBTTagCompound tag) {
		onServer(() -> {
			world().setBlock(X, Y, Z, block, meta, 3);
			TileEntity tile = world().getTileEntity(X, Y, Z);
			assertTrue(tile instanceof TileEntitySign, "no sign at the spot");
			tile.readFromNBT(tag);
			tile.markDirty();
			world().markBlockForUpdate(X, Y, Z);
		});
	}

	private static String[] lines(String first) {
		return new String[]{first, "", "", ""};
	}


	// ---------------------------------------------------------------- the player, the editor

	private void standAt(double x, double z, float yaw) {
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(x, Y, z, yaw, 0.0F));
		serverTicks(3);
		ticks(3);
	}

	/** The player uses what they hold on the sign, as the server takes a click. */
	private void use() {
		onServer(() -> player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), player().getHeldItem(), X, Y, Z, 2, 0.5F, 0.5F, 0.5F));
	}

	private static boolean isEditor(GuiScreen screen) {
		return screen != null && screen.getClass().getName().startsWith("io.github.asgerjon.mcfly.") && screen.getClass().getSimpleName().contains("SignEdit");
	}

	private void waitForEditor() {
		waitUntilOnClient("McFly's sign editor open", () -> isEditor(mc().currentScreen), 40);
	}

	private void typeText(String text) {
		onClient(() -> {
			for (char c : text.toCharArray()) {
				keyTyped(mc().currentScreen, c, 0);
			}
		});
	}

	private void press(int key) {
		onClient(() -> keyTyped(mc().currentScreen, key == Keyboard.KEY_RETURN ? '\r' : key == Keyboard.KEY_BACK ? '\b' : '\0', key));
	}

	private void closeEditor() {
		onClient(() -> mc().displayGuiScreen(null));
		serverTicks(3);
	}

	// ---------------------------------------------------------------- ours, stored form

	public void testSignsAreOurs() {
		List<String> wrong = new ArrayList<>();
		List<String> names = new ArrayList<>();
		for (String wood : OLD) {
			names.add("sign_" + wood);
			names.add("wall_sign_" + wood);
		}
		for (String wood : WOODS) {
			names.add(wood + "_sign");
			names.add(wood + "_wall_sign");
		}
		for (String name : names) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String wood : OLD) {
			String type = item("item_sign_" + wood).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add("item_sign_" + wood + " " + type);
			}
		}
		for (String name : new String[]{"blocks.BlockWoodSign", "items.ItemWoodSign", "blocks.itemblocks.ItemBlockSign", "tileentities.TileEntityWoodSign",
			"client.renderer.tileentity.TileEntityWoodSignRenderer", "client.gui.inventory.GuiEditWoodSign", "network.WoodSignOpenMessage",
			"signs.ModernSigns", "signs.ISignText", "client.renderer.tileentity.SignTextDrawing"}) {
			try {
				if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
					wrong.add(name + " still there");
				}
			} catch (java.io.IOException e) {
				wrong.add(name + " " + e);
			}
		}
		assertEqual("[]", wrong.toString(), "the signs' classes");
	}

	/** `etfuturum.sign` is McFly's, and every McFly sign makes one; vanilla's `Sign` stays vanilla's. */
	public void testTheTileEntity() {
		String got = onServer(() -> {
			TileEntity loaded = TileEntity.createAndLoadEntity(efrTag("etfuturum.sign", lines("a"), null, false, false));
			TileEntity vanilla = TileEntity.createAndLoadEntity(efrTag("Sign", lines("a"), null, false, false));
			StringBuilder result = new StringBuilder(loaded == null ? "unregistered" : loaded.getClass().getName().startsWith("io.github.asgerjon.mcfly.") ? "mcfly's" : loaded.getClass().getName());
			result.append(" ").append(vanilla != null && vanilla.getClass() == TileEntitySign.class ? "vanilla's" : String.valueOf(vanilla));
			for (String name : new String[]{"sign_birch", "wall_sign_dark_oak", "cherry_sign", "warped_wall_sign"}) {
				world().setBlock(X, Y, Z, block(name), name.contains("wall") ? 2 : 0, 2);
				TileEntity tile = world().getTileEntity(X, Y, Z);
				if (tile == null || loaded == null || tile.getClass() != loaded.getClass()) {
					result.append(" ").append(name).append(":").append(tile == null ? "none" : tile.getClass().getSimpleName());
				}
			}
			return result.toString();
		});
		assertEqual("mcfly's vanilla's", got, "etfuturum.sign's class, Sign's, and any sign block not making McFly's");
	}

	/** EFR's metas by 26.3's names: standing 0 to 15 `rotation`, wall 2 to 5 north, south, west, east; vanilla's the same. */
	public void testStoredForm() {
		Object[][] metas = {{"sign_spruce", 4}, {"wall_sign_spruce", 5}, {"crimson_sign", 15}, {"crimson_wall_sign", 2}, {"wall_sign_jungle", 3},
			{"bamboo_wall_sign", 4}, {"minecraft:standing_sign", 8}, {"minecraft:wall_sign", 5}};
		String got = onServer(() -> {
			StringBuilder names = new StringBuilder();
			for (Object[] entry : metas) {
				names.append(names.length() == 0 ? "" : " ").append(name(block((String) entry[0]), (Integer) entry[1]));
			}
			return names.toString();
		});
		assertEqual("spruce_sign,rotation=4 spruce_wall_sign,facing=east crimson_sign,rotation=15 crimson_wall_sign,facing=north "
			+ "jungle_wall_sign,facing=south bamboo_wall_sign,facing=west oak_sign,rotation=8 oak_wall_sign,facing=east", got, "26.3's names");
	}

	/** EFR's tag read back: the front's lines, colour, glow and wax; the back empty; vanilla's `Sign` the same. */
	public void testEfrSignReadsBack() {
		String[] want = {"Hello|World||", "red glowing"};
		signFrom(block("sign_spruce"), 0, efrTag("etfuturum.sign", new String[]{"Hello", "World", "", ""}, "red", true, true));
		String[] got = onServer(() -> {
			NBTTagCompound tag = saved(world().getTileEntity(X, Y, Z));
			return new String[]{side(tag, true), side(tag, false), String.valueOf(tag.getBoolean("Waxed"))};
		});
		assertEqual(want[0] + " " + want[1], got[0], "the front");
		assertEqual("||| black plain", got[1], "the back, never written");
		assertEqual("true", got[2], "the wax");
		signFrom(Blocks.standing_sign, 0, efrTag("Sign", new String[]{"Old", "", "", "sign"}, null, false, false));
		assertEqual("Old|||sign black plain ||| black plain", serverSide(true) + " " + serverSide(false), "vanilla's sign, front and back");
	}

	/** A 30-character line stays whole through a save (1.7.10 cut it at 15). */
	public void testLongLinesKept() {
		String line = "abcdefghijklmnopqrstuvwxyz0123";
		signFrom(block("cherry_sign"), 0, efrTag("etfuturum.sign", lines(line), null, false, false));
		String got = onServer(() -> {
			TileEntity copy = TileEntity.createAndLoadEntity(saved(world().getTileEntity(X, Y, Z)));
			return copy == null ? "none" : side(saved(copy), true);
		});
		assertEqual(line + "||| black plain", got, "the front after a save and a load");
	}

	/** Both sides saved (the back as 26.3's `back_text`) and sent to the client. */
	public void testBothSidesSavedAndSent() {
		NBTTagCompound tag = efrTag("etfuturum.sign", lines("Front"), "lime", false, false);
		tag.setTag("back_text", backText(new String[]{"Back", "", "", "side"}, "blue", true));
		signFrom(block("sign_birch"), 0, tag);
		assertEqual("Back|||side blue glowing", serverSide(false), "the back, saved again");
		standAt(X + 0.5D, Z + 2.5D, 180.0F);
		waitUntilOnClient("the client's sign with both sides", () -> {
			TileEntity tile = mc().theWorld.getTileEntity(X, Y, Z);
			if (tile == null) {
				return false;
			}
			NBTTagCompound client = saved(tile);
			return side(client, true).equals("Front||| lime plain") && side(client, false).equals("Back|||side blue glowing");
		}, 40);
	}

	// ---------------------------------------------------------------- placing, holding, drops

	/**
	 * 26.3's `StandingAndWallBlockItem`: a top gives a standing sign turned by the yaw; a side a wall sign facing out; under
	 * a block, next to a wall, a wall sign on that wall (1.7.10 placed nothing). EFR's item, a new wood's, vanilla's.
	 */
	public void testPlacing() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (String name : new String[]{"item_sign_birch", "mangrove_sign", "minecraft:sign"}) {
				Item sign = item(name);
				// on the floor, looking south (yaw 0): rotation 8, facing the player
				player().rotationYaw = 0.0F;
				player().rotationPitch = 60.0F;
				result.append(result.length() == 0 ? "" : " ").append(place(sign, X, Y - 1, Z, 1));
				// on the north face of a block at z + 1: a wall sign facing north
				world().setBlock(X, Y, Z + 1, Blocks.stone, 0, 2);
				player().rotationPitch = 0.0F;
				result.append(" ").append(place(sign, X, Y, Z + 1, 2));
				// under a block at y + 1, the wall at z + 1 still there, looking south and a little up
				world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
				player().rotationPitch = -30.0F;
				result.append(" ").append(place(sign, X, Y + 1, Z, 0));
				world().setBlock(X, Y + 1, Z, Blocks.air, 0, 2);
				world().setBlock(X, Y, Z + 1, Blocks.air, 0, 2);
			}
			return result.toString();
		});
		assertEqual("birch_sign,rotation=8 birch_wall_sign,facing=north birch_wall_sign,facing=north "
			+ "mangrove_sign,rotation=8 mangrove_wall_sign,facing=north mangrove_wall_sign,facing=north "
			+ "oak_sign,rotation=8 oak_wall_sign,facing=north oak_wall_sign,facing=north", got, "placed on a top, a side, an underside beside a wall");
	}

	/** Uses the item on the face; what stands at the spot then (by 26.3's name), and the spot emptied. */
	private String place(Item sign, int x, int y, int z, int side) {
		ItemStack stack = new ItemStack(sign);
		player().inventory.setInventorySlotContents(player().inventory.currentItem, stack);
		player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), stack, x, y, z, side, 0.5F, side == 1 ? 1.0F : side == 0 ? 0.0F : 0.5F,
			side == 2 ? 0.0F : 0.5F);
		Block placed = world().getBlock(X, Y, Z);
		String name = placed == Blocks.air ? "none" : name(placed, world().getBlockMetadata(X, Y, Z));
		world().setBlock(X, Y, Z, Blocks.air, 0, 2);
		return name;
	}

	/** A standing sign falls with its floor, a wall sign with its wall; each drops its item (the old woods' `item_sign_`). */
	public void testHeldAndDrops() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			world().setBlock(X, Y, Z, block("sign_acacia"), 3, 3);
			world().setBlock(X, Y - 1, Z, Blocks.air, 0, 3);
			result.append(world().getBlock(X, Y, Z) == Blocks.air ? "fell " : "stood ").append(dropped());
			world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 2);
			world().setBlock(X, Y, Z + 1, Blocks.stone, 0, 2);
			world().setBlock(X, Y, Z, block("cherry_wall_sign"), 2, 3);
			world().setBlock(X, Y, Z + 1, Blocks.air, 0, 3);
			result.append(world().getBlock(X, Y, Z) == Blocks.air ? " fell " : " stood ").append(dropped());
			return result.toString();
		});
		assertEqual("fell {acacia_sign=1} fell {cherry_sign=1}", got, "without the floor, without the wall");
	}

	// ---------------------------------------------------------------- the editor

	/** Placing opens McFly's editor on the front; typing changes the sign as it goes; closing sends the lines. */
	public void testPlacingOpensTheEditor() {
		standAt(X + 0.5D, Z + 2.5D, 180.0F);
		setHeld(new ItemStack(item("item_sign_spruce")));
		ticks(2);
		look(180.0F, 39.0F);
		ticks(2);
		tap(mc().gameSettings.keyBindUseItem);
		waitForEditor();
		int[] at = onServer(() -> {
			for (int x = X - 2; x <= X + 2; x++) {
				for (int z = Z - 2; z <= Z + 3; z++) {
					if (world().getTileEntity(x, Y, z) instanceof TileEntitySign) {
						return new int[]{x, z};
					}
				}
			}
			return null;
		});
		assertIsNotNone(at, "no sign placed near the spot");
		typeText("Hi");
		waitUntilOnClient("the sign in the world as typed", () -> {
			TileEntity tile = mc().theWorld.getTileEntity(at[0], Y, at[1]);
			return tile != null && side(saved(tile), true).startsWith("Hi|");
		}, 20);
		press(Keyboard.KEY_RETURN);
		typeText("There");
		closeEditor();
		String got = onServer(() -> side(saved(world().getTileEntity(at[0], Y, at[1])), true));
		assertEqual("Hi|There|| black plain", got, "the server's front after closing");
	}

	/** Typing keeps a line within 90 pixels (26.3's `MAX_TEXT_LINE_WIDTH`; 1.7.10 cut at 15 characters), whole on the server. */
	public void testLinesUpTo90Pixels() {
		openEditorBy("", true);
		typeText("iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii");
		String line = onClient(() -> side(saved(mc().theWorld.getTileEntity(X, Y, Z)), true).split("\\|")[0]);
		int[] widths = onClient(() -> new int[]{mc().fontRenderer.getStringWidth(line), mc().fontRenderer.getStringWidth(line + "i")});
		assertTrue(widths[0] <= 90 && widths[1] > 90, "the line's width " + widths[0] + " (" + line.length() + " characters), one more " + widths[1]);
		closeEditor();
		assertEqual(line + "||| black plain", serverSide(true), "the server's line");
	}

	/** Enter and down go to the next line, up to the one before (wrapping); Backspace takes a character. */
	public void testLineKeys() {
		openEditorBy("", true);
		typeText("a");
		press(Keyboard.KEY_DOWN);
		typeText("b");
		press(Keyboard.KEY_UP);
		press(Keyboard.KEY_UP);
		typeText("d");
		press(Keyboard.KEY_RETURN);
		press(Keyboard.KEY_RETURN);
		typeText("xy");
		press(Keyboard.KEY_BACK);
		closeEditor();
		assertEqual("a|bx||d black plain", serverSide(true), "the lines");
	}

	/** Using a sign with text (no item) opens the editor for the side the player faces: in front the front, behind the back. */
	public void testUseOpensTheFacedSide() {
		NBTTagCompound tag = efrTag("etfuturum.sign", lines("F"), null, false, false);
		tag.setTag("back_text", backText(lines("B"), "black", false));
		signFrom(block("sign_jungle"), 0, tag);
		for (boolean front : new boolean[]{true, false}) {
			standAt(X + 0.5D, front ? Z + 2.5D : Z - 1.5D, front ? 180.0F : 0.0F);
			setHeld(null);
			use();
			waitForEditor();
			press(Keyboard.KEY_BACK);
			typeText(front ? "front" : "back");
			closeEditor();
		}
		assertEqual("front||| black plain back||| black plain", serverSide(true) + " " + serverSide(false), "front, back");
	}

	/** A stick isn't an applicator: the editor opens (26.3's `TRY_WITH_EMPTY_HAND`), without a swing (26.3's `CONSUME`). */
	public void testUseWithAStickOpensWithoutASwing() {
		signFrom(block("bamboo_sign"), 0, efrTag("etfuturum.sign", lines("x"), null, false, false));
		standAt(X + 0.5D, Z + 2.5D, 180.0F);
		setHeld(new ItemStack(Items.stick));
		ticks(2);
		look(180.0F, 20.0F);
		ticks(2);
		tap(mc().gameSettings.keyBindUseItem);
		waitForEditor();
		assertFalse(onClient(() -> mc().thePlayer.isSwingInProgress), "the arm swung");
		closeEditor();
		setHeld(new ItemStack(Items.dye, 1, 1));
		ticks(2);
		tap(mc().gameSettings.keyBindUseItem);
		ticks(1);
		assertTrue(onClient(() -> mc().thePlayer.isSwingInProgress), "no swing for a dye");
	}

	/** A waxed sign doesn't open: `block.sign.waxed_interact_fail`. */
	public void testWaxedDoesntOpen() {
		signFrom(block("sign_dark_oak"), 0, efrTag("etfuturum.sign", lines("x"), "black", false, true));
		standAt(X + 0.5D, Z + 2.5D, 180.0F);
		setHeld(null);
		clearSounds();
		use();
		waitForSound(MODERN + "block.sign.waxed_interact_fail", 20);
		ticks(5);
		assertFalse(onClient(() -> isEditor(mc().currentScreen)), "a waxed sign's editor opened");
	}

	/** Someone else editing: nothing opens and nothing applies; the editor is cleared once out of reach (4 past the reach). */
	public void testOneEditor() {
		signFrom(block("crimson_sign"), 0, efrTag("etfuturum.sign", lines("x"), null, false, false));
		standAt(X + 0.5D, Z + 2.5D, 180.0F);
		// another player's lock, and both uses in the same tick: 26.3 clears a lock whose player isn't in the world on the
		// next tick (`playerIsTooFarAwayToEdit`: no such player is too far)
		onServer(() -> {
			((Signs.Sided) world().getTileEntity(X, Y, Z)).mcfly$setEditor(UUID.randomUUID());
			ItemStack dye = new ItemStack(Items.dye, 1, 1);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, dye);
			player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), dye, X, Y, Z, 2, 0.5F, 0.5F, 0.5F);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), null, X, Y, Z, 2, 0.5F, 0.5F, 0.5F);
		});
		ticks(10);
		assertFalse(onClient(() -> isEditor(mc().currentScreen)), "the editor opened while another player edits");
		assertEqual("x||| black plain", serverSide(true), "the front while another player edits (no dye)");
		assertIsNone(onServer(() -> ((Signs.Sided) world().getTileEntity(X, Y, Z)).mcfly$editor()), "a lock whose player isn't in the world, after a tick");
		onServer(() -> ((Signs.Sided) world().getTileEntity(X, Y, Z)).mcfly$setEditor(player().getUniqueID()));
		standAt(X + 0.5D, Z + 10.5D, 180.0F);
		serverTicks(3);
		Object editor = onServer(() -> ((Signs.Sided) world().getTileEntity(X, Y, Z)).mcfly$editor());
		assertIsNone(editor, "the editor 10 blocks away");
	}

	/**
	 * McFly's update message: its side's lines set, formatting codes stripped, the editor cleared (a second is refused);
	 * refused from a player who isn't the editor, and with a line over 384 characters.
	 */
	public void testUpdateMessage() throws Exception {
		Class<?> message = Class.forName("io.github.asgerjon.mcfly.network.SignUpdateMessage");
		java.lang.reflect.Constructor<?> make = message.getConstructor(int.class, int.class, int.class, boolean.class, String[].class);
		signFrom(block("warped_sign"), 0, efrTag("etfuturum.sign", lines("old"), null, false, false));
		standAt(X + 0.5D, Z + 2.5D, 180.0F);
		// not the editor
		send(make.newInstance(X, Y, Z, true, new String[]{"nobody", "", "", ""}));
		assertEqual("old||| black plain", serverSide(true), "after a message from a player not editing");
		// the editor: stripped, then cleared
		setHeld(null);
		use();
		waitForEditor();
		send(make.newInstance(X, Y, Z, true, new String[]{"§cRed", "two", "", ""}));
		assertEqual("Red|two|| black plain", serverSide(true), "after the editor's message");
		send(make.newInstance(X, Y, Z, true, new String[]{"again", "", "", ""}));
		assertEqual("Red|two|| black plain", serverSide(true), "after a second message");
		onClient(() -> mc().displayGuiScreen(null));
		serverTicks(3);
		// too long
		use();
		waitForEditor();
		StringBuilder long385 = new StringBuilder();
		for (int i = 0; i < 385; i++) {
			long385.append('a');
		}
		send(make.newInstance(X, Y, Z, false, new String[]{long385.toString(), "", "", ""}));
		assertEqual("||| black plain", serverSide(false), "the back after a 385-character line");
	}

	private void send(Object message) {
		onClient(() -> McFlyNetwork.CHANNEL.sendToServer((cpw.mods.fml.common.network.simpleimpl.IMessage) message));
		serverTicks(3);
	}

	/** A sign with the text placed at the spot, the player in front (or behind), the editor open by a use. */
	private void openEditorBy(String text, boolean front) {
		signFrom(block("sign_birch"), 0, efrTag("etfuturum.sign", lines(text), null, false, false));
		standAt(X + 0.5D, front ? Z + 2.5D : Z - 1.5D, front ? 180.0F : 0.0F);
		setHeld(null);
		use();
		waitForEditor();
	}

	// ---------------------------------------------------------------- applicators by side

	/** A dye and a glow ink sac go to the side faced; `block_change` from the player. */
	public void testDyeBySide() {
		NBTTagCompound tag = efrTag("etfuturum.sign", lines("F"), null, false, false);
		tag.setTag("back_text", backText(lines("B"), "black", false));
		signFrom(block("sign_spruce"), 0, tag);
		standAt(X + 0.5D, Z - 1.5D, 0.0F);
		List<String> heard = new ArrayList<>();
		onServer(() -> {
			GameEventListener recorder = new GameEventListener() {
				@Override
				public double[] position(World world) {
					return new double[]{X + 0.5D, Y + 0.5D, Z + 0.5D};
				}

				@Override
				public int radius() {
					return 8;
				}

				@Override
				public boolean handle(World world, GameEvent event, GameEvent.Context context, double ex, double ey, double ez) {
					heard.add(event.name.replace("minecraft:", "") + (context.source == player() ? " by the player" : ""));
					return true;
				}
			};
			GameEvents.register(world(), recorder);
			try {
				player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.dye, 1, 1));
				player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), player().getHeldItem(), X, Y, Z, 2, 0.5F, 0.5F, 0.5F);
			} finally {
				GameEvents.unregister(world(), recorder);
			}
		});
		assertEqual("F||| black plain B||| red plain", serverSide(true) + " " + serverSide(false), "front, back after a red dye from behind");
		assertIn("block_change by the player", heard, "the dye's game events");
	}

	// ---------------------------------------------------------------- properties, items, recipes, drawing

	/** 26.3's: 1.0, no collision, no fire, the wood's sound and map colour; the shapes (1.7.10's). */
	public void testProperties() {
		String[] sounds = {"block.nether_wood.break", "block.nether_wood.break", "dig.wood", "block.cherry_wood.break", "block.bamboo_wood.break"};
		MapColor[] newColours = {MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.clothColor, MapColor.yellowColor};
		MapColor[] oldColours = {MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor, MapColor.brownColor};
		List<Object[]> want = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			want.add(new Object[]{WOODS[i] + "_sign", sounds[i], newColours[i]});
			want.add(new Object[]{WOODS[i] + "_wall_sign", sounds[i], newColours[i]});
			want.add(new Object[]{"sign_" + OLD[i], "dig.wood", oldColours[i]});
			want.add(new Object[]{"wall_sign_" + OLD[i], "dig.wood", oldColours[i]});
		}
		want.add(new Object[]{"minecraft:standing_sign", "dig.wood", MapColor.woodColor});
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			world().setBlock(X, Y, Z + 1, Blocks.stone, 0, 2);
			for (Object[] entry : want) {
				Block block = block((String) entry[0]);
				world().setBlock(X, Y, Z, block, ((String) entry[0]).contains("wall") ? 2 : 0, 2);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null) + " "
					+ block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP) + " " + block.getFlammability(world(), X, Y, Z, ForgeDirection.UP) + " "
					+ (block.getCollisionBoundingBoxFromPool(world(), X, Y, Z) == null ? "passable" : "solid") + " " + breakSound(X, Y, Z).replace(MODERN, "");
				if (!("1.0 1.0 0 0 passable " + entry[1]).equals(got) || block.getMapColor(0) != entry[2]) {
					found.add(entry[0] + " " + got + " colour " + block.getMapColor(0).colorIndex);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, resistance, fire, collision, sound, colour (want: as listed)");
	}

	/** The shapes: standing 4 to 12 across, 16 high; a wall sign facing north 4.5 to 12.5 high, z 14 to 16. */
	public void testShapes() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			world().setBlock(X, Y, Z + 1, Blocks.stone, 0, 2);
			for (Object[] entry : new Object[][]{{"cherry_sign", 0}, {"cherry_wall_sign", 2}, {"minecraft:wall_sign", 2}}) {
				Block block = block((String) entry[0]);
				world().setBlock(X, Y, Z, block, (Integer) entry[1], 2);
				block.setBlockBoundsBasedOnState(world(), X, Y, Z);
				result.append(result.length() == 0 ? "" : " ").append(String.format("%.1f-%.1f/%.1f-%.1f/%.1f-%.1f", block.getBlockBoundsMinX() * 16,
					block.getBlockBoundsMaxX() * 16, block.getBlockBoundsMinY() * 16, block.getBlockBoundsMaxY() * 16, block.getBlockBoundsMinZ() * 16,
					block.getBlockBoundsMaxZ() * 16));
			}
			return result.toString();
		});
		assertEqual("4.0-12.0/0.0-16.0/4.0-12.0 0.0-16.0/4.5-12.5/14.0-16.0 0.0-16.0/4.5-12.5/14.0-16.0", got, "x/y/z in pixels");
	}

	/** 26.3's: stacks of 16, fuel 200 (crimson's and warped's none), `signWood`; vanilla's oak sign too. */
	public void testStacksFuelAndOreNames() {
		StringBuilder got = new StringBuilder();
		for (String name : new String[]{"item_sign_spruce", "cherry_sign", "crimson_sign", "minecraft:sign"}) {
			ItemStack stack = new ItemStack(item(name));
			boolean ore = false;
			for (int id : OreDictionary.getOreIDs(stack)) {
				ore |= "signWood".equals(OreDictionary.getOreName(id));
			}
			got.append(got.length() == 0 ? "" : ", ").append(name).append(" ").append(stack.getMaxStackSize()).append(" ").append(TileEntityFurnace.getItemBurnTime(stack))
				.append(ore ? " signWood" : " no ore name");
		}
		assertEqual("item_sign_spruce 16 200 signWood, cherry_sign 16 200 signWood, crimson_sign 16 0 signWood, minecraft:sign 16 200 signWood", got.toString(),
			"stack size, burn time, ore name");
	}

	/** 26.3's (its jar's, `woods_signs_recipes_26_3.txt`). */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("woods_signs_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(11, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}

	public void testNames() {
		String got = onClient(() -> {
			StringBuilder names = new StringBuilder();
			for (String name : new String[]{"item_sign_spruce", "item_sign_dark_oak", "mangrove_sign", "bamboo_sign"}) {
				names.append(names.length() == 0 ? "" : ", ").append(new ItemStack(item(name)).getDisplayName());
			}
			return names.toString();
		});
		assertEqual("Spruce Sign, Dark Oak Sign, Mangrove Sign, Bamboo Sign", got, "names");
	}

	/** Drawn as 26.3 draws them: every sign a McFly model in the chunk (vanilla's too); the text by McFly's renderer. */
	public void testDrawing() {
		String got = onClient(() -> {
			StringBuilder result = new StringBuilder();
			for (String name : new String[]{"minecraft:standing_sign", "minecraft:wall_sign", "sign_spruce", "crimson_wall_sign"}) {
				if (block(name).getRenderType() != McFlyRenderIds.MODEL) {
					result.append(name).append(" render type ").append(block(name).getRenderType()).append("; ");
				}
			}
			Object renderer = TileEntityRendererDispatcher.instance.getSpecialRendererByClass(TileEntitySign.class);
			if (renderer == null || !renderer.getClass().getName().startsWith("io.github.asgerjon.mcfly.")) {
				result.append("the text renderer ").append(renderer == null ? "none" : renderer.getClass().getName());
			}
			return result.toString();
		});
		assertEqual("", got, "render types and the text renderer");
	}
}
