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
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The wood sets' doors as our own (SPEC-modern-movement.md §91.6, step D), as 26.3's: EFR's ids and metas (the upper
 * half's bit 1 26.3's powered), strength, sounds, colours; use, sounds by the set type and the new state, redstone by
 * the powered bit, a wind burst, the hinge on placing; vanilla's wooden and iron doors the same; drops, recipes, fuel,
 * stacks, ore names and names. At z 600, y 8.
 */
@TasSuite("woods")
public class WoodDoorTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 600;
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

	/** x 36 to 44, y 7 to 11, z 596 to 604 emptied, a stone floor under it, its items gone. */
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

	/** The 26.3 block it reads as: a facing but north, the upper half (its hinge if right), open, powered. */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		StringBuilder name = new StringBuilder(found.type().id.replace("minecraft:", ""));
		if (state.containsKey("facing") && !"north".equals(state.get("facing"))) {
			name.append(",facing=").append(state.get("facing"));
		}
		if ("upper".equals(state.get("half"))) {
			name.append(",upper");
			if ("right".equals(state.get("hinge"))) {
				name.append(",hinge=right");
			}
		}
		if ("true".equals(state.get("open"))) {
			name.append(",open");
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

	/** A whole door at X, Y, Z: its lower half (facing east, closed), its upper (hinge left). */
	private void door(Block door) {
		world().setBlock(X, Y, Z, door, 0, 2);
		world().setBlock(X, Y + 1, Z, door, 8, 2);
	}

	private void use(int x, int y, int z) {
		player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		world().getBlock(x, y, z).onBlockActivated(world(), x, y, z, player(), 1, 0.5F, 0.5F, 0.5F);
	}

	private String metas() {
		return world().getBlockMetadata(X, Y, Z) + " " + world().getBlockMetadata(X, Y + 1, Z);
	}

	// ---------------------------------------------------------------- ours, stored form, properties

	public void testDoorsAreOurs() {
		List<String> wrong = new ArrayList<>();
		for (String wood : OLD) {
			check(wrong, "door_" + wood);
		}
		for (String wood : WOODS) {
			check(wrong, wood + "_door");
		}
		for (String name : new String[]{"blocks.BaseDoor", "blocks.itemblocks.ItemBlockNewDoor"}) {
			try {
				if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
					wrong.add(name + " still there");
				}
			} catch (java.io.IOException e) {
				wrong.add(name + " " + e);
			}
		}
		assertEqual("[]", wrong.toString(), "the doors' classes");
	}

	private static void check(List<String> wrong, String name) {
		String type = block(name).getClass().getName();
		if (!type.startsWith("io.github.asgerjon.mcfly.")) {
			wrong.add(name + " " + type);
		}
	}

	/** EFR's metas by 26.3's names; the upper half's 2 is 26.3's powered (1.12's row). */
	public void testStoredForm() {
		Object[][] metas = {{"door_birch", 0}, {"door_birch", 4}, {"door_birch", 8}, {"door_birch", 9}, {"door_birch", 10}, {"crimson_door", 3},
			{"bamboo_door", 11}};
		String got = onServer(() -> {
			StringBuilder names = new StringBuilder();
			for (Object[] entry : metas) {
				names.append(names.length() == 0 ? "" : " ").append(name(block((String) entry[0]), (Integer) entry[1]));
			}
			return names.toString();
		});
		assertEqual("birch_door,facing=east birch_door,facing=east,open birch_door,facing=east,upper birch_door,facing=east,upper,hinge=right "
			+ "birch_door,facing=east,upper,powered crimson_door bamboo_door,facing=east,upper,hinge=right,powered", got, "26.3's names");
	}

	/** 26.3's: 3 and 3, an axe, no fire; the wood's sound; the planks' colour. */
	public void testProperties() {
		String[] newSounds = {"block.nether_wood.break", "block.nether_wood.break", "dig.wood", "block.cherry_wood.break", "block.bamboo_wood.break"};
		MapColor[] newColours = {MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.clothColor, MapColor.yellowColor};
		MapColor[] oldColours = {MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor, MapColor.brownColor};
		List<Object[]> want = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			want.add(new Object[]{WOODS[i] + "_door", newSounds[i], newColours[i]});
			want.add(new Object[]{"door_" + OLD[i], "dig.wood", oldColours[i]});
		}
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (Object[] entry : want) {
				Block block = block((String) entry[0]);
				door(block);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null) + " "
					+ block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP) + " " + block.getFlammability(world(), X, Y, Z, ForgeDirection.UP) + " "
					+ block.getHarvestTool(0) + " " + breakSound(X, Y, Z).replace(MODERN, "");
				if (!("3.0 3.0 0 0 axe " + entry[1]).equals(got) || block.getMapColor(0) != entry[2]) {
					found.add(entry[0] + " " + got + " colour " + block.getMapColor(0).colorIndex);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, resistance, fire, tool, sound, colour (want: as listed)");
	}

	// ---------------------------------------------------------------- use, sounds, redstone, wind

	/** Used from either half: open, then closed; the lower half holds it. */
	public void testUse() {
		String got = onServer(() -> {
			door(block("cherry_door"));
			use(X, Y + 1, Z);
			String opened = metas();
			use(X, Y, Z);
			return opened + ", " + metas();
		});
		assertEqual("4 8, 0 8", got, "lower and upper metas: opened from the top, closed from the bottom");
	}

	/** The set type's sounds by the new state (EFR's chose at random): cherry's, the oak set's for vanilla's wooden door. */
	public void testSounds() {
		for (String[] door : new String[][]{{"cherry_door", "block.cherry_wood_door."}, {"minecraft:wooden_door", "block.wooden_door."}}) {
			onServer(() -> {
				player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z + 2.5D, 180.0F, 0.0F);
				door(block(door[0]));
			});
			serverTicks(2);
			for (String want : new String[]{"open", "close", "open", "close"}) {
				clearSounds();
				onServer(() -> use(X, Y, Z));
				waitForSound(MODERN + door[1] + want, 20);
				String other = MODERN + door[1] + (want.equals("open") ? "close" : "open");
				assertFalse(soundsHeard().contains(other), door[0] + "'s " + want + " also played " + other + ": " + soundsHeard());
			}
		}
	}

	/**
	 * 26.3's powered bit (the upper half's 2): a redstone block by the lower half opens it, powered; the player closes it,
	 * powered still; a neighbour's update leaves it; the power gone, it's closed and not powered. Vanilla's wooden and iron
	 * doors the same (iron's not by hand: closed by the power going).
	 */
	public void testRedstone() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (String name : new String[]{"mangrove_door", "minecraft:wooden_door"}) {
				door(block(name));
				world().setBlock(X + 1, Y, Z, Blocks.redstone_block, 0, 3);
				String powered = metas();
				use(X, Y, Z);
				String closedByHand = metas();
				world().setBlock(X, Y, Z - 1, Blocks.stone, 0, 3);
				String afterUpdate = metas();
				world().setBlock(X + 1, Y, Z, Blocks.air, 0, 3);
				result.append(name).append(": ").append(powered).append(", ").append(closedByHand).append(", ").append(afterUpdate).append(", ").append(metas())
					.append("; ");
				world().setBlock(X, Y, Z - 1, Blocks.air, 0, 3);
			}
			door(Blocks.iron_door);
			use(X, Y, Z);
			String byHand = metas();
			world().setBlock(X + 1, Y, Z, Blocks.redstone_block, 0, 3);
			String powered = metas();
			world().setBlock(X + 1, Y, Z, Blocks.air, 0, 3);
			return result + "iron: " + byHand + ", " + powered + ", " + metas();
		});
		assertEqual("mangrove_door: 4 10, 0 10, 0 10, 0 8; minecraft:wooden_door: 4 10, 0 10, 0 10, 0 8; iron: 0 8, 4 10, 0 8", got,
			"lower and upper metas: powered, closed by hand, after an update, the power gone");
	}

	/** A wind burst toggles an unpowered wooden door, not a powered one. */
	public void testWindBurst() throws Exception {
		java.lang.reflect.Method trigger = ganymedes01.etfuturum.potion.ModernEffects.class.getDeclaredMethod("trigger", World.class, int.class, int.class, int.class);
		trigger.setAccessible(true);
		String got = onServer(() -> {
			try {
				door(block("door_acacia"));
				trigger.invoke(null, world(), X, Y, Z);
				String toggled = metas();
				world().setBlock(X + 1, Y, Z, Blocks.redstone_block, 0, 3);
				String powered = metas();
				trigger.invoke(null, world(), X, Y, Z);
				return toggled + ", " + powered + ", " + metas();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		assertEqual("4 8, 4 10, 4 10", got, "metas: toggled open; powered; a burst on the powered door");
	}

	/**
	 * 26.3's hinge (`DoorBlock.getHinge`), placed facing north: a solid block on the left (west) puts it left, on the
	 * right (east) right; with neither, the click: left of the middle left, right of it right.
	 */
	public void testHinge() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z + 2.5D, 180.0F, 0.0F);
			player().rotationYaw = 180.0F;
			StringBuilder result = new StringBuilder();
			Object[][] cases = {{X - 1, 0.5F}, {X + 1, 0.5F}, {null, 0.25F}, {null, 0.75F}};
			for (Object[] entry : cases) {
				if (entry[0] != null) {
					world().setBlock((Integer) entry[0], Y, Z, Blocks.stone, 0, 2);
					world().setBlock((Integer) entry[0], Y + 1, Z, Blocks.stone, 0, 2);
				}
				ItemStack door = new ItemStack(Item.getItemFromBlock(block("cherry_door")));
				player().inventory.setInventorySlotContents(player().inventory.currentItem, door);
				player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), door, X, Y - 1, Z, 1, (Float) entry[1], 1.0F, 0.5F);
				result.append(result.length() == 0 ? "" : " ").append(name(world().getBlock(X, Y + 1, Z), world().getBlockMetadata(X, Y + 1, Z)));
				for (int y = Y; y <= Y + 1; y++) {
					world().setBlock(X, y, Z, Blocks.air, 0, 2);
					world().setBlock(X - 1, y, Z, Blocks.air, 0, 2);
					world().setBlock(X + 1, y, Z, Blocks.air, 0, 2);
				}
			}
			return result.toString();
		});
		assertEqual("cherry_door,facing=east,upper cherry_door,facing=east,upper,hinge=right cherry_door,facing=east,upper "
			+ "cherry_door,facing=east,upper,hinge=right", got, "the upper halves: wall left, wall right, clicked left, clicked right");
	}

	// ---------------------------------------------------------------- drops, recipes, fuel, stacks, ore names, names

	/** One door from either half. */
	public void testDrops() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (Object[] entry : new Object[][]{{"door_jungle", Y}, {"warped_door", Y + 1}}) {
				door(block((String) entry[0]));
				player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.iron_axe));
				player().theItemInWorldManager.tryHarvestBlock(X, (Integer) entry[1], Z);
				result.append(result.length() == 0 ? "" : " ").append(dropped());
			}
			return result.toString();
		});
		assertEqual("{jungle_door=1} {warped_door=1}", got, "drops from the lower and upper halves");
	}

	/** 26.3's (its jar's, `woods_doors_recipes_26_3.txt`): doors 3, trapdoors 2, iron's trapdoor. */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("woods_doors_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(23, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}

	/** 26.3's fuel (doors 200, crimson's and warped's none) and stacks of 64. */
	public void testFuelAndStacks() {
		StringBuilder got = new StringBuilder();
		for (String name : new String[]{"crimson_door", "cherry_door", "door_spruce", "minecraft:wooden_door"}) {
			Item item = name.startsWith("minecraft:") ? Items.wooden_door : Item.getItemFromBlock(block(name));
			got.append(got.length() == 0 ? "" : " ").append(TileEntityFurnace.getItemBurnTime(new ItemStack(item))).append("/").append(item.getItemStackLimit());
		}
		assertEqual("0/64 200/64 200/64 200/64", got.toString(), "burn times and stack sizes");
	}

	public void testOreNames() {
		List<String> missing = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			for (String name : new String[]{WOODS[i] + "_door", "door_" + OLD[i]}) {
				ItemStack stack = new ItemStack(block(name));
				boolean has = false;
				for (int id : OreDictionary.getOreIDs(stack)) {
					has |= "doorWood".equals(OreDictionary.getOreName(id));
				}
				if (!has) {
					missing.add(name);
				}
			}
		}
		assertEqual("[]", missing.toString(), "doors without doorWood");
	}

	public void testNames() {
		String got = onClient(() -> {
			StringBuilder names = new StringBuilder();
			for (String name : new String[]{"door_spruce", "door_dark_oak", "crimson_door", "bamboo_door"}) {
				names.append(names.length() == 0 ? "" : ", ").append(new ItemStack(block(name)).getDisplayName());
			}
			return names.toString();
		});
		assertEqual("Spruce Door, Dark Oak Door, Crimson Door, Bamboo Door", got, "names");
	}
}
