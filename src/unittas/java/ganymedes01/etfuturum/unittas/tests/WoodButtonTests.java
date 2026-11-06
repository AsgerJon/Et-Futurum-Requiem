package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The wood sets' buttons as our own (SPEC-modern-movement.md §91.7, step E), as 26.3's: EFR's ids and metas, with 26.3's
 * other floor and ceiling facing in 1.7.10's spare metas (6 a floor button facing east or west, 7 a ceiling one) for
 * every button, vanilla's too; strength, no fire, sounds, no map colour; pressed by hand for the set's time, its clicks by the
 * set type; the floor and ceiling shapes by facing, placing by the player's facing, power through the block; drops,
 * recipes, fuel, ore names and names. At z 640, y 8.
 */
@TasSuite("woods")
public class WoodButtonTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 640;
	private static final String MODERN = McFly.MC_ASSETS + ":";
	private static final String[] WOODS = {"crimson", "warped", "mangrove", "cherry", "bamboo"};
	private static final String[] OLD = {"spruce", "birch", "jungle", "acacia", "dark_oak"};

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
		});
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		});
	}

	/** x 36 to 44, y 7 to 11, z 636 to 644 emptied, a stone floor under it, its items gone. */
	private void clear() {
		for (int x = X - 4; x <= X + 4; x++) {
			for (int z = Z - 4; z <= Z + 4; z++) {
				for (int y = Y; y <= Y + 3; y++) {
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
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 5, Y - 2, Z - 5, X + 6, Y + 5, Z + 6))) {
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

	/** The 26.3 block it reads as: its face but wall, a facing but north, powered. */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		StringBuilder name = new StringBuilder(found.type().id.replace("minecraft:", ""));
		if (state.containsKey("face") && !"wall".equals(state.get("face"))) {
			name.append(",face=").append(state.get("face"));
		}
		if (state.containsKey("facing") && !"north".equals(state.get("facing"))) {
			name.append(",facing=").append(state.get("facing"));
		}
		if ("true".equals(state.get("powered"))) {
			name.append(",powered");
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

	private void press(int x, int y, int z) {
		player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		world().getBlock(x, y, z).onBlockActivated(world(), x, y, z, player(), 1, 0.5F, 0.5F, 0.5F);
	}

	private int meta() {
		return world().getBlockMetadata(X, Y, Z);
	}

	// ---------------------------------------------------------------- ours, stored form, properties

	public void testButtonsAreOurs() {
		List<String> wrong = new ArrayList<>();
		List<String> names = new ArrayList<>();
		for (String wood : OLD) {
			names.add("button_" + wood);
		}
		for (String wood : WOODS) {
			names.add(wood + "_button");
		}
		for (String name : names) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : new String[]{"blocks.BlockWoodButton", "mixins.early.floorceilbutton.MixinBlockButton"}) {
			try {
				if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
					wrong.add(name + " still there");
				}
			} catch (java.io.IOException e) {
				wrong.add(name + " " + e);
			}
		}
		assertEqual("[]", wrong.toString(), "the buttons' classes");
	}

	/** EFR's metas by 26.3's names; 6 and 7 (new) the floor's and ceiling's other facing; vanilla's stone button the same. */
	public void testStoredForm() {
		Object[][] metas = {{"button_birch", 1}, {"button_birch", 4}, {"button_birch", 0}, {"button_birch", 5}, {"button_birch", 6}, {"button_birch", 7},
			{"button_birch", 13}, {"button_birch", 14}, {"minecraft:stone_button", 6}, {"minecraft:wooden_button", 15}};
		String got = onServer(() -> {
			StringBuilder names = new StringBuilder();
			for (Object[] entry : metas) {
				names.append(names.length() == 0 ? "" : " ").append(name(block((String) entry[0]), (Integer) entry[1]));
			}
			return names.toString();
		});
		assertEqual("birch_button,facing=east birch_button birch_button,face=ceiling birch_button,face=floor birch_button,face=floor,facing=east "
			+ "birch_button,face=ceiling,facing=east birch_button,face=floor,powered birch_button,face=floor,facing=east,powered "
			+ "stone_button,face=floor,facing=east oak_button,face=ceiling,facing=east,powered", got, "26.3's names");
	}

	/** 26.3's: 0.5, the axe's (needed for nothing), no fire; the wood's sound; no map colour (`buttonProperties`: NONE). */
	public void testProperties() {
		String[] newSounds = {"block.nether_wood.break", "block.nether_wood.break", "dig.wood", "block.cherry_wood.break", "block.bamboo_wood.break"};
		List<Object[]> want = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			want.add(new Object[]{WOODS[i] + "_button", newSounds[i], MapColor.airColor});
			want.add(new Object[]{"button_" + OLD[i], "dig.wood", MapColor.airColor});
		}
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (Object[] entry : want) {
				Block block = block((String) entry[0]);
				world().setBlock(X, Y, Z, block, 5, 2);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null) + " "
					+ block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP) + " " + block.getFlammability(world(), X, Y, Z, ForgeDirection.UP) + " "
					+ block.getHarvestTool(5) + " " + breakSound(X, Y, Z).replace(MODERN, "");
				if (!("0.5 0.5 0 0 axe " + entry[1]).equals(got) || block.getMapColor(5) != entry[2]) {
					found.add(entry[0] + " " + got + " colour " + block.getMapColor(5).colorIndex);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, resistance, fire, tool, sound, colour (want: as listed)");
	}

	// ---------------------------------------------------------------- pressing, sounds, shapes, placing, power

	/** Pressed by hand: a wooden one down 30 ticks, vanilla's stone one 20 (26.3's sets). */
	public void testPressedTime() {
		String[][] buttons = {{"cherry_button", "25", "10"}, {"minecraft:stone_button", "15", "10"}};
		StringBuilder got = new StringBuilder();
		for (String[] button : buttons) {
			onServer(() -> {
				world().setBlock(X, Y, Z, block(button[0]), 5, 3);
				press(X, Y, Z);
			});
			serverTicks(Integer.parseInt(button[1]));
			boolean still = onServer(() -> (meta() & 8) != 0);
			serverTicks(Integer.parseInt(button[2]));
			boolean released = onServer(() -> (meta() & 8) == 0);
			got.append(button[0]).append(still ? " down" : " up").append(released ? " then up; " : " still down; ");
		}
		assertEqual("cherry_button down then up; minecraft:stone_button down then up; ", got.toString(), "after the times, then a bit more");
	}

	/** The set type's clicks: cherry's; the stone set's for vanilla's stone button. */
	public void testClicks() {
		for (String[] button : new String[][]{{"cherry_button", "block.cherry_wood_button."}, {"minecraft:stone_button", "block.stone_button."}}) {
			onServer(() -> {
				player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z + 2.5D, 180.0F, 0.0F);
				world().setBlock(X, Y, Z, block(button[0]), 5, 3);
			});
			serverTicks(2);
			clearSounds();
			onServer(() -> press(X, Y, Z));
			waitForSound(MODERN + button[1] + "click_on", 20);
			waitForSound(MODERN + button[1] + "click_off", 45);
		}
	}

	/**
	 * 26.3's shapes: on the floor 6 by 4 (meta 5 long along x, facing north or south; 6 along z, east or west), 2 high, 1
	 * pressed; on the ceiling the same, at the top.
	 */
	public void testFloorAndCeilingShapes() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			Block button = block("button_jungle");
			for (int meta : new int[]{5, 6, 7, 14}) {
				world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
				world().setBlock(X, Y, Z, button, meta, 2);
				button.setBlockBoundsBasedOnState(world(), X, Y, Z);
				result.append(result.length() == 0 ? "" : " ").append(Math.round((button.getBlockBoundsMaxX() - button.getBlockBoundsMinX()) * 16)).append("x")
					.append(Math.round((button.getBlockBoundsMaxZ() - button.getBlockBoundsMinZ()) * 16)).append("x")
					.append(Math.round((button.getBlockBoundsMaxY() - button.getBlockBoundsMinY()) * 16)).append(button.getBlockBoundsMaxY() == 1.0D ? "@top" : "@bottom");
			}
			return result.toString();
		});
		assertEqual("6x4x2@bottom 4x6x2@bottom 4x6x2@top 4x6x1@bottom", got, "x by z by height: floor 5, floor 6, ceiling 7, floor 6 pressed");
	}

	/** Placed on a floor by the player's facing: facing north 5, facing east 6; under a ceiling facing east 7; vanilla's the same. */
	public void testPlacing() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (String name : new String[]{"cherry_button", "minecraft:wooden_button"}) {
				for (Object[] look : new Object[][]{{180.0F, 1}, {270.0F, 1}, {270.0F, 0}}) {
					player().rotationYaw = (Float) look[0];
					int side = (Integer) look[1];
					if (side == 0) {
						world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
					}
					ItemStack button = new ItemStack(block(name));
					player().inventory.setInventorySlotContents(player().inventory.currentItem, button);
					player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), button, X, side == 1 ? Y - 1 : Y + 1, Z, side, 0.5F, side == 1 ? 1.0F : 0.0F,
						0.5F);
					result.append(result.length() == 0 ? "" : " ").append(world().getBlock(X, Y, Z) == block(name) ? String.valueOf(meta()) : "none");
					world().setBlock(X, Y, Z, Blocks.air, 0, 2);
					world().setBlock(X, Y + 1, Z, Blocks.air, 0, 2);
				}
			}
			return result.toString();
		});
		assertEqual("5 6 7 5 6 7", got, "metas: floor facing north, floor facing east, ceiling facing east (ours, vanilla's)");
	}

	/** Pressed, 15 strong through the block it's on: a floor button (6) into the floor, a ceiling one (7) into the ceiling. */
	public void testPower() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, block("bamboo_button"), 14, 3);
			int floor = world().getBlockPowerInput(X, Y - 1, Z);
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 3);
			world().setBlock(X, Y, Z, block("bamboo_button"), 15, 3);
			int ceiling = world().getBlockPowerInput(X, Y + 1, Z);
			return floor + " " + ceiling;
		});
		assertEqual("15 15", got, "strong power into the floor and the ceiling");
	}

	// ---------------------------------------------------------------- drops, recipes, fuel, ore names, names

	public void testDrops() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (Object[] entry : new Object[][]{{"button_acacia", 5}, {"warped_button", 7}}) {
				world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
				world().setBlock(X, Y, Z, block((String) entry[0]), (Integer) entry[1], 2);
				player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
				player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
				result.append(result.length() == 0 ? "" : " ").append(dropped());
			}
			return result.toString();
		});
		assertEqual("{acacia_button=1} {warped_button=1}", got, "drops by hand");
	}

	/** 26.3's (its jar's, `woods_buttons_recipes_26_3.txt`): buttons and plates. */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("woods_buttons_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(22, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}

	/** 26.3's fuel: 100 (`WOOD_ITEMS_EXTRA_SMALL`), crimson's and warped's none; vanilla's oak button too. */
	public void testFuel() {
		StringBuilder got = new StringBuilder();
		for (String name : new String[]{"crimson_button", "cherry_button", "button_spruce", "minecraft:wooden_button"}) {
			got.append(got.length() == 0 ? "" : " ").append(TileEntityFurnace.getItemBurnTime(new ItemStack(block(name))));
		}
		assertEqual("0 100 100 100", got.toString(), "burn times");
	}

	public void testOreNames() {
		List<String> missing = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			for (String name : new String[]{WOODS[i] + "_button", "button_" + OLD[i]}) {
				boolean has = false;
				for (int id : OreDictionary.getOreIDs(new ItemStack(block(name)))) {
					has |= "buttonWood".equals(OreDictionary.getOreName(id));
				}
				if (!has) {
					missing.add(name);
				}
			}
		}
		assertEqual("[]", missing.toString(), "buttons without buttonWood");
	}

	public void testNames() {
		String got = onClient(() -> {
			StringBuilder names = new StringBuilder();
			for (String name : new String[]{"button_spruce", "button_dark_oak", "mangrove_button", "minecraft:wooden_button"}) {
				names.append(names.length() == 0 ? "" : ", ").append(new ItemStack(block(name)).getDisplayName());
			}
			return names.toString();
		});
		assertEqual("Spruce Button, Dark Oak Button, Mangrove Button, Oak Button", got, "names");
	}
}
