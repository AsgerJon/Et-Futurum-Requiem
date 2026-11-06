package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEventListener;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
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
 * The wood sets' fences and gates as our own (SPEC-modern-movement.md §91.5, step C), as 26.3's: EFR's ids and metas
 * (the gate's powered bit new), strength, sounds, colours, fire and fuel; 26.3's joining (vanilla's fences too); the
 * gate's use, sounds by wood type, game events, redstone by the powered bit, a wind burst; lowered in a wall; drops,
 * recipes, ore names and names. At z 580, y 8.
 */
@TasSuite("woods")
public class WoodFenceTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 580;
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

	/** x 36 to 44, y 7 to 10, z 576 to 584 emptied, its items gone. */
	private void clear() {
		for (int x = X - 4; x <= X + 4; x++) {
			for (int y = Y - 1; y <= Y + 2; y++) {
				for (int z = Z - 4; z <= Z + 4; z++) {
					world().setBlock(x, y, z, Blocks.air, 0, 2);
				}
			}
		}
		for (EntityItem item : items()) {
			item.setDead();
		}
	}

	private List<EntityItem> items() {
		List<EntityItem> found = new ArrayList<>();
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 5, Y - 2, Z - 5, X + 6, Y + 4, Z + 6))) {
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

	/** The 26.3 block it reads as, with a facing but south, open, powered. */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		StringBuilder name = new StringBuilder(found.type().id.replace("minecraft:", ""));
		if (state.containsKey("facing") && !"south".equals(state.get("facing"))) {
			name.append(",facing=").append(state.get("facing"));
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

	private void use(int x, int y, int z) {
		player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		world().getBlock(x, y, z).onBlockActivated(world(), x, y, z, player(), 1, 0.5F, 0.5F, 0.5F);
	}

	// ---------------------------------------------------------------- ours, stored form, properties

	public void testFencesAreOurs() {
		List<String> wrong = new ArrayList<>();
		List<String> names = new ArrayList<>();
		names.add("wood_fence");
		for (String wood : OLD) {
			names.add("fence_" + wood);
			names.add("fence_gate_" + wood);
		}
		for (String wood : WOODS) {
			names.add(wood + "_fence_gate");
		}
		for (String name : names) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : new String[]{"blocks.BlockWoodFence", "blocks.BlockModernWoodFence", "blocks.BlockModernWoodFenceCompat", "blocks.BlockWoodFenceGate",
			"client.renderer.block.BlockNewFenceRenderer", "mixins.early.fencewallconnect.MixinBlockFence"}) {
			try {
				if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
					wrong.add(name + " still there");
				}
			} catch (java.io.IOException e) {
				wrong.add(name + " " + e);
			}
		}
		assertEqual("[]", wrong.toString(), "the fences' and gates' classes");
	}

	/** EFR's ids and metas by 26.3's names; a gate's 8 is 26.3's powered (1.12's row). */
	public void testStoredForm() {
		Object[][] metas = {{"wood_fence", 0}, {"wood_fence", 1}, {"wood_fence", 2}, {"wood_fence", 3}, {"wood_fence", 4}, {"fence_spruce", 0},
			{"fence_dark_oak", 0}, {"fence_gate_birch", 0}, {"fence_gate_birch", 5}, {"cherry_fence_gate", 10}, {"crimson_fence_gate", 15}};
		String got = onServer(() -> {
			StringBuilder names = new StringBuilder();
			for (Object[] entry : metas) {
				names.append(names.length() == 0 ? "" : " ").append(name(block((String) entry[0]), (Integer) entry[1]));
			}
			return names.toString();
		});
		assertEqual("crimson_fence warped_fence mangrove_fence cherry_fence bamboo_fence spruce_fence dark_oak_fence birch_fence_gate "
			+ "birch_fence_gate,facing=west,open cherry_fence_gate,facing=north,powered crimson_fence_gate,facing=east,open,powered", got, "26.3's names");
	}

	/**
	 * 26.3's: 2 and 3, an axe; the wood's sound; the planks' colour (1.7.10's ids or nearest); fire 5 and 20 but crimson
	 * and warped.
	 */
	public void testProperties() {
		String[] newSounds = {"block.nether_wood.break", "block.nether_wood.break", "dig.wood", "block.cherry_wood.break", "block.bamboo_wood.break"};
		MapColor[] newColours = {MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.clothColor, MapColor.yellowColor};
		MapColor[] oldColours = {MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor, MapColor.brownColor};
		List<Object[]> want = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			String fire = i < 2 ? "0 0" : "5 20";
			want.add(new Object[]{"wood_fence", i, fire, newSounds[i], newColours[i]});
			want.add(new Object[]{WOODS[i] + "_fence_gate", 0, fire, newSounds[i], newColours[i]});
			want.add(new Object[]{"fence_" + OLD[i], 0, "5 20", "dig.wood", oldColours[i]});
			want.add(new Object[]{"fence_gate_" + OLD[i], 0, "5 20", "dig.wood", oldColours[i]});
		}
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (Object[] entry : want) {
				Block block = block((String) entry[0]);
				int meta = (Integer) entry[1];
				world().setBlock(X, Y, Z, block, meta, 2);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null) + " "
					+ block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP) + " " + block.getFlammability(world(), X, Y, Z, ForgeDirection.UP) + " "
					+ block.getHarvestTool(meta) + " " + breakSound(X, Y, Z).replace(MODERN, "");
				String wantText = "2.0 3.0 " + entry[2] + " axe " + entry[3];
				if (!wantText.equals(got) || block.getMapColor(meta) != entry[4]) {
					found.add(entry[0] + ":" + meta + " " + got + " colour " + block.getMapColor(meta).colorIndex);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, resistance, fire, tool, sound, colour (want: as listed)");
	}

	// ---------------------------------------------------------------- joining

	/** Whether the fence at X, Y, Z reaches east (its box past 0.625, as 1.7.10 sets it from its joins). */
	private boolean joinsEast() {
		Block fence = world().getBlock(X, Y, Z);
		fence.setBlockBoundsBasedOnState(world(), X, Y, Z);
		return fence.getBlockBoundsMaxX() > 0.7D;
	}

	/**
	 * 26.3's `FenceBlock.connectsTo`, with a cherry fence and vanilla's oak fence: another wooden fence, not a nether
	 * brick fence; a gate across (facing south: it spans x) and not along (facing east); a sturdy face (stone), not glass,
	 * leaves, a pumpkin or a melon.
	 */
	public void testJoining() {
		Object[][] neighbours = {{"minecraft:fence", 0}, {"minecraft:nether_brick_fence", 0}, {"minecraft:fence_gate", 0}, {"minecraft:fence_gate", 3},
			{"minecraft:stone", 0}, {"minecraft:glass", 0}, {"minecraft:leaves", 4}, {"minecraft:pumpkin", 0}, {"minecraft:melon_block", 0}};
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (Object[] fence : new Object[][]{{"wood_fence", 3}, {"minecraft:fence", 0}}) {
				result.append(result.length() == 0 ? "" : "; ").append(fence[0]).append(":");
				for (Object[] neighbour : neighbours) {
					world().setBlock(X, Y, Z, block((String) fence[0]), (Integer) fence[1], 2);
					world().setBlock(X + 1, Y, Z, block((String) neighbour[0]), (Integer) neighbour[1], 2);
					result.append(" ").append(joinsEast() ? "1" : "0");
				}
			}
			return result.toString();
		});
		assertEqual("wood_fence: 1 0 1 0 1 0 0 0 0; minecraft:fence: 1 0 1 0 1 0 0 0 0", got,
			"joins east to: oak fence, nether brick fence, gate across, gate along, stone, glass, leaves, pumpkin, melon");
	}

	// ---------------------------------------------------------------- the gate

	/** Used: opens turned away from the player (facing north, the gate south: it turns north), then closes; meta 0, 6, 2. */
	public void testGateUse() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z + 2.5D, 180.0F, 0.0F);
			world().setBlock(X, Y, Z, block("cherry_fence_gate"), 0, 2);
			use(X, Y, Z);
			int opened = world().getBlockMetadata(X, Y, Z);
			use(X, Y, Z);
			return "0 " + opened + " " + world().getBlockMetadata(X, Y, Z);
		});
		assertEqual("0 6 2", got, "the gate's metas: closed facing south, opened (turned north), closed");
	}

	/** 26.3's wood type's gate sounds by the new state: cherry's open, then its close (EFR's chose at random). */
	public void testGateSounds() {
		onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z + 2.5D, 180.0F, 0.0F);
			world().setBlock(X, Y, Z, block("cherry_fence_gate"), 0, 2);
		});
		serverTicks(2);
		for (String want : new String[]{"open", "close", "open", "close"}) {
			clearSounds();
			onServer(() -> use(X, Y, Z));
			waitForSound(MODERN + "block.cherry_wood_fence_gate." + want, 20);
			List<String> heard = soundsHeard();
			String other = MODERN + "block.cherry_wood_fence_gate." + (want.equals("open") ? "close" : "open");
			assertFalse(heard.contains(other), "the gate's " + want + " also played " + other + ": " + heard);
		}
	}

	/** `block_open` and `block_close` from the player, once each. */
	public void testGateEvents() {
		List<String> heard = new ArrayList<>();
		onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z + 2.5D, 180.0F, 0.0F);
			world().setBlock(X, Y, Z, block("bamboo_fence_gate"), 0, 2);
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
				use(X, Y, Z);
				use(X, Y, Z);
			} finally {
				GameEvents.unregister(world(), recorder);
			}
		});
		assertEqual("[block_open by the player, block_close by the player]", heard.toString(), "the gate's game events");
	}

	/**
	 * 26.3's powered bit: a redstone block beside opens it (powered); the player closes it, powered still; a neighbour's
	 * update leaves it closed; the power gone, it stays closed, not powered.
	 */
	public void testGateRedstone() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, block("mangrove_fence_gate"), 0, 3);
			world().setBlock(X + 1, Y, Z, Blocks.redstone_block, 0, 3);
			int powered = world().getBlockMetadata(X, Y, Z);
			use(X, Y, Z);
			int closedByHand = world().getBlockMetadata(X, Y, Z);
			world().setBlock(X - 1, Y, Z, Blocks.stone, 0, 3);
			int afterUpdate = world().getBlockMetadata(X, Y, Z);
			world().setBlock(X + 1, Y, Z, Blocks.air, 0, 3);
			return powered + " " + closedByHand + " " + afterUpdate + " " + world().getBlockMetadata(X, Y, Z);
		});
		assertEqual("12 8 8 0", got, "metas: powered open, closed by hand, after a neighbour's update, the power gone");
	}

	/** A wind burst (26.3's explosion trigger) toggles an unpowered gate, not a powered one. */
	public void testGateWindBurst() throws Exception {
		java.lang.reflect.Method trigger = ganymedes01.etfuturum.potion.ModernEffects.class.getDeclaredMethod("trigger", World.class, int.class, int.class, int.class);
		trigger.setAccessible(true);
		String got = onServer(() -> {
			try {
				world().setBlock(X, Y, Z, block("fence_gate_birch"), 0, 3);
				trigger.invoke(null, world(), X, Y, Z);
				int toggled = world().getBlockMetadata(X, Y, Z);
				world().setBlock(X + 1, Y, Z, Blocks.redstone_block, 0, 3);
				int powered = world().getBlockMetadata(X, Y, Z);
				trigger.invoke(null, world(), X, Y, Z);
				return toggled + " " + powered + " " + world().getBlockMetadata(X, Y, Z);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		assertEqual("4 12 12", got, "metas: toggled open; powered; a burst on the powered gate");
	}

	/** In a wall (walls either side across it), 26.3's shape is lowered: its top at 13/16; not in a wall, 1. */
	public void testGateInWall() {
		String got = onServer(() -> {
			Block gate = block("fence_gate_spruce");
			world().setBlock(X, Y, Z, gate, 0, 3);
			gate.setBlockBoundsBasedOnState(world(), X, Y, Z);
			double alone = gate.getBlockBoundsMaxY();
			world().setBlock(X - 1, Y, Z, Blocks.cobblestone_wall, 0, 3);
			world().setBlock(X + 1, Y, Z, Blocks.cobblestone_wall, 0, 3);
			gate.setBlockBoundsBasedOnState(world(), X, Y, Z);
			double inWall = gate.getBlockBoundsMaxY();
			use(X, Y, Z);
			boolean noCollision = gate.getCollisionBoundingBoxFromPool(world(), X, Y, Z) == null;
			return alone + " " + inWall + " " + noCollision;
		});
		assertEqual("1.0 0.8125 true", got, "the top alone, in a wall; no collision open");
	}

	// ---------------------------------------------------------------- drops, recipes, fuel, ore names, names

	public void testDrops() {
		Object[][] metas = {{"wood_fence", 4}, {"fence_jungle", 0}, {"crimson_fence_gate", 6}, {"fence_gate_acacia", 9}};
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (Object[] entry : metas) {
				world().setBlock(X, Y, Z, block((String) entry[0]), (Integer) entry[1], 2);
				player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.iron_axe));
				player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
				result.append(result.length() == 0 ? "" : " ").append(dropped());
			}
			return result.toString();
		});
		assertEqual("{bamboo_fence=1} {jungle_fence=1} {crimson_fence_gate=1} {acacia_fence_gate=1}", got, "drops");
	}

	/** 26.3's (its jar's, `woods_fences_recipes_26_3.txt`): the eleven woods' fences and gates. */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("woods_fences_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(22, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}

	/** 26.3's fuel: 300, crimson's and warped's none. */
	public void testFuel() {
		Object[][] stacks = {{"wood_fence", 0}, {"wood_fence", 3}, {"fence_birch", 0}, {"warped_fence_gate", 0}, {"bamboo_fence_gate", 0}, {"fence_gate_dark_oak", 0}};
		StringBuilder got = new StringBuilder();
		for (Object[] entry : stacks) {
			got.append(got.length() == 0 ? "" : " ").append(TileEntityFurnace.getItemBurnTime(new ItemStack(block((String) entry[0]), 1, (Integer) entry[1])));
		}
		assertEqual("0 300 300 0 300 300", got.toString(), "burn times");
	}

	public void testOreNames() {
		List<String> missing = new ArrayList<>();
		for (int meta = 0; meta < 5; meta++) {
			checkOre(missing, "fenceWood", "wood_fence", meta);
			checkOre(missing, "fenceGateWood", WOODS[meta] + "_fence_gate", 0);
			checkOre(missing, "fenceWood", "fence_" + OLD[meta], 0);
			checkOre(missing, "fenceGateWood", "fence_gate_" + OLD[meta], 0);
		}
		assertEqual("[]", missing.toString(), "without their ore name");
	}

	private static void checkOre(List<String> missing, String ore, String name, int meta) {
		ItemStack stack = new ItemStack(block(name), 1, meta);
		for (int id : OreDictionary.getOreIDs(stack)) {
			if (ore.equals(OreDictionary.getOreName(id))) {
				return;
			}
		}
		missing.add(name + ":" + meta + " " + ore);
	}

	public void testNames() {
		String got = onClient(() -> {
			StringBuilder names = new StringBuilder();
			Object[][] stacks = {{"wood_fence", 0}, {"wood_fence", 4}, {"fence_dark_oak", 0}, {"fence_gate_spruce", 0}, {"cherry_fence_gate", 0}};
			for (Object[] entry : stacks) {
				names.append(names.length() == 0 ? "" : ", ").append(new ItemStack(Item.getItemFromBlock(block((String) entry[0])), 1, (Integer) entry[1]).getDisplayName());
			}
			return names.toString();
		});
		assertEqual("Crimson Fence, Bamboo Fence, Dark Oak Fence, Spruce Fence Gate, Cherry Fence Gate", got, "names");
	}
}
