package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * The Nether's stones as our own (SPEC-modern-movement.md §88.2, §88.3), as 26.3's: ours, EFR's classes gone; the stored
 * form EFR wrote, read as before (26.3's names through the modern command layer); 26.3's strength, resistance, tools and
 * sounds; gilded blackstone's and nether gold ore's drops and experience; crying obsidian's light, tears and its stand
 * against pistons; the polished blackstone plate pressed by mobs only; the button's 20 ticks; 26.3's recipes
 * (`netherstones_recipes_26_3.txt`, from its jar). A row at z 60 on the flat world.
 */
@TasSuite("netherstones")
public class NetherStoneTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 60;
	private static final String[] EFR_CLASSES = {"blocks.BlockBlackstone", "blocks.BlockBlackstoneSlab", "blocks.BlockBlackstoneWall",
		"blocks.BlockPolishedBlackstoneButton", "blocks.BlockPolishedBlackstonePressurePlate", "blocks.BlockGildedBlackstone", "blocks.BlockBasalt",
		"blocks.ores.BlockOreNetherGold", "blocks.BlockNewNetherBrick"};

	@Override
	protected void setUp() {
		clear();
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
	}

	@Override
	protected void tearDown() {
		clear();
		onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
	}

	private void clear() {
		onServer(() -> {
			for (int x = X - 3; x <= X + 6; x++) {
				for (int y = Y; y <= Y + 4; y++) {
					for (int z = Z - 3; z <= Z + 3; z++) {
						world().setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
			for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
				if (entity instanceof EntityItem || entity instanceof EntityPig) {
					((net.minecraft.entity.Entity) entity).setDead();
				}
			}
		});
	}

	// ---------------------------------------------------------------- helpers

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	/** 26.3's name's state here: the block and meta the command layer gives it. */
	private static BlockIds.State state(String modern) {
		BlockIds.Type type = BlockIds.byId("minecraft:" + modern);
		assertTrue(type != null, "no block for minecraft:" + modern);
		return type.defaultState();
	}

	private static void put(int x, int y, int z, String modern) {
		BlockIds.State state = state(modern);
		world().setBlock(x, y, z, state.block(), state.meta(), 3);
	}

	private static void check(List<String> wrong, String id, int meta, String modern, String properties) {
		BlockIds.Found found = BlockIds.find(block(id), meta);
		String got = found == null ? "nothing" : found.type().id.replace("minecraft:", "");
		Map<String, String> props = found == null ? null : found.state().properties();
		boolean propsRight = true;
		if (properties != null && props != null) {
			for (String pair : properties.split(",")) {
				String[] kv = pair.split("=");
				propsRight &= kv[1].equals(props.get(kv[0]));
			}
		}
		if (!modern.equals(got) || !propsRight) {
			wrong.add(id + ":" + meta + " " + got + (props == null ? "" : props.toString()) + " not " + modern + (properties == null ? "" : "[" + properties + "]"));
		}
	}

	/** The command layer's properties for a vanilla block's meta (stairs and buttons keep vanilla's metas). */
	private static String vanillaProperties(Block vanilla, int meta) {
		BlockIds.Found found = BlockIds.find(vanilla, meta);
		StringBuilder out = new StringBuilder();
		for (Map.Entry<String, String> entry : found.state().properties().entrySet()) {
			out.append(out.length() == 0 ? "" : ",").append(entry.getKey()).append('=').append(entry.getValue());
		}
		return out.toString();
	}

	// ---------------------------------------------------------------- ours

	/** Every stone of the family McFly's; none of EFR's classes for them left. */
	public void testStonesAreOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : new String[]{"blackstone", "gilded_blackstone", "blackstone_slab", "double_blackstone_slab", "blackstone_stairs",
			"polished_blackstone_stairs", "polished_blackstone_brick_stairs", "blackstone_wall", "polished_blackstone_pressure_plate",
			"polished_blackstone_button", "basalt", "smooth_basalt", "nether_gold_ore", "crying_obsidian", "red_netherbrick", "red_netherbrick_slab",
			"double_red_netherbrick_slab", "red_netherbrick_stairs", "red_nether_brick_wall", "quartz_bricks"}) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.add(name + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "the stones' classes");
	}

	// ---------------------------------------------------------------- the stored form

	/** Each id and meta EFR wrote is read as before, by 26.3's names and states. */
	public void testStoredForm() {
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			String[] blackstones = {"blackstone", "polished_blackstone", "polished_blackstone_bricks", "cracked_polished_blackstone_bricks",
				"chiseled_polished_blackstone"};
			for (int meta = 0; meta < 5; meta++) {
				check(found, "blackstone", meta, blackstones[meta], null);
			}
			String[] slabs = {"blackstone_slab", "polished_blackstone_slab", "polished_blackstone_brick_slab"};
			String[] walls = {"blackstone_wall", "polished_blackstone_wall", "polished_blackstone_brick_wall"};
			for (int meta = 0; meta < 3; meta++) {
				check(found, "blackstone_slab", meta, slabs[meta], "type=bottom");
				check(found, "blackstone_slab", meta + 8, slabs[meta], "type=top");
				check(found, "double_blackstone_slab", meta, slabs[meta], "type=double");
				check(found, "blackstone_wall", meta, walls[meta], null);
			}
			String[] stairs = {"blackstone_stairs", "polished_blackstone_stairs", "polished_blackstone_brick_stairs"};
			for (String stair : stairs) {
				for (int meta = 0; meta < 8; meta++) {
					check(found, stair, meta, stair, vanillaProperties(Blocks.stone_stairs, meta));
				}
			}
			for (int meta = 0; meta < 8; meta++) {
				check(found, "red_netherbrick_stairs", meta, "red_nether_brick_stairs", vanillaProperties(Blocks.stone_stairs, meta));
			}
			for (int meta : new int[]{1, 2, 3, 4, 9, 10, 11, 12}) {
				check(found, "polished_blackstone_button", meta, "polished_blackstone_button", vanillaProperties(Blocks.stone_button, meta));
			}
			check(found, "polished_blackstone_button", 0, "polished_blackstone_button", "face=ceiling,powered=false");
			check(found, "polished_blackstone_button", 5, "polished_blackstone_button", "face=floor,powered=false");
			check(found, "polished_blackstone_button", 13, "polished_blackstone_button", "face=floor,powered=true");
			check(found, "polished_blackstone_pressure_plate", 0, "polished_blackstone_pressure_plate", "powered=false");
			check(found, "polished_blackstone_pressure_plate", 1, "polished_blackstone_pressure_plate", "powered=true");
			String[] axes = {"y", "x", "z"};
			for (int axis = 0; axis < 3; axis++) {
				check(found, "basalt", axis * 4, "basalt", "axis=" + axes[axis]);
				check(found, "basalt", axis * 4 + 1, "polished_basalt", "axis=" + axes[axis]);
			}
			String[] bricks = {"red_nether_bricks", "cracked_nether_bricks", "chiseled_nether_bricks"};
			for (int meta = 0; meta < 3; meta++) {
				check(found, "red_netherbrick", meta, bricks[meta], null);
			}
			check(found, "red_netherbrick_slab", 0, "red_nether_brick_slab", "type=bottom");
			check(found, "red_netherbrick_slab", 8, "red_nether_brick_slab", "type=top");
			check(found, "double_red_netherbrick_slab", 0, "red_nether_brick_slab", "type=double");
			check(found, "red_nether_brick_wall", 0, "red_nether_brick_wall", null);
			for (String single : new String[]{"gilded_blackstone", "smooth_basalt", "nether_gold_ore", "crying_obsidian", "quartz_bricks"}) {
				check(found, single, 0, single, null);
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "the stored form, as 26.3's names");
	}

	// ---------------------------------------------------------------- strength and sounds

	/**
	 * 26.3's strength (hardness, then explosion resistance as 26.3 gives it), the tool and its level for drops ("-" where
	 * 26.3 has no tool rule), and 26.3's sounds ("stone" 1.7.10's own).
	 */
	public void testStrengthAndSounds() {
		String[][] blocks = {{"blackstone", "1.5 6.0 pickaxe 0", "stone"}, {"polished_blackstone", "2.0 6.0 pickaxe 0", "stone"},
			{"polished_blackstone_bricks", "1.5 6.0 pickaxe 0", "stone"}, {"cracked_polished_blackstone_bricks", "1.5 6.0 pickaxe 0", "stone"},
			{"chiseled_polished_blackstone", "1.5 6.0 pickaxe 0", "stone"}, {"gilded_blackstone", "1.5 6.0 pickaxe 0", "gilded_blackstone"},
			{"blackstone_slab", "2.0 6.0 pickaxe 0", "stone"}, {"polished_blackstone_slab", "2.0 6.0 pickaxe 0", "stone"},
			{"polished_blackstone_brick_slab", "2.0 6.0 pickaxe 0", "stone"}, {"blackstone_stairs", "1.5 6.0 pickaxe 0", "stone"},
			{"polished_blackstone_stairs", "2.0 6.0 pickaxe 0", "stone"}, {"polished_blackstone_brick_stairs", "1.5 6.0 pickaxe 0", "stone"},
			{"blackstone_wall", "1.5 6.0 pickaxe 0", "stone"}, {"polished_blackstone_wall", "2.0 6.0 pickaxe 0", "stone"},
			{"polished_blackstone_brick_wall", "1.5 6.0 pickaxe 0", "stone"}, {"polished_blackstone_pressure_plate", "0.5 0.5 -", "stone"},
			{"polished_blackstone_button", "0.5 0.5 -", "stone"}, {"basalt", "1.25 4.2 pickaxe 0", "basalt"},
			{"polished_basalt", "1.25 4.2 pickaxe 0", "basalt"}, {"smooth_basalt", "1.25 4.2 pickaxe 0", "basalt"},
			{"nether_gold_ore", "3.0 3.0 pickaxe 0", "nether_gold_ore"}, {"crying_obsidian", "50.0 1200.0 pickaxe 3", "stone"},
			{"red_nether_bricks", "2.0 6.0 pickaxe 0", "nether_bricks"}, {"cracked_nether_bricks", "2.0 6.0 pickaxe 0", "nether_bricks"},
			{"chiseled_nether_bricks", "2.0 6.0 pickaxe 0", "nether_bricks"}, {"red_nether_brick_slab", "2.0 6.0 pickaxe 0", "nether_bricks"},
			{"red_nether_brick_stairs", "2.0 6.0 pickaxe 0", "nether_bricks"}, {"red_nether_brick_wall", "2.0 6.0 pickaxe 0", "nether_bricks"},
			{"quartz_bricks", "0.8 0.8 pickaxe 0", "stone"}};
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (String[] entry : blocks) {
				BlockIds.State state = state(entry[0]);
				world().setBlock(X, Y, Z, state.block(), state.meta(), 3);
				Block block = state.block();
				int meta = world().getBlockMetadata(X, Y, Z);
				String tool = entry[1].endsWith("-") ? "-" : block.getHarvestTool(meta) + " " + block.getHarvestLevel(meta);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null, world(), X, Y, Z, X, Y, Z) + " " + tool;
				String sound = block.stepSound.getBreakSound();
				String wantSound = "stone".equals(entry[2]) ? "dig.stone" : McFly.MC_ASSETS + ":block." + entry[2] + ".break";
				if (!entry[1].equals(got) || !wantSound.equals(sound)) {
					found.add(entry[0] + " " + got + " " + sound);
				}
			}
			world().setBlock(X, Y, Z, Blocks.air, 0, 3);
			return found;
		});
		assertEqual("[]", wrong.toString(), "strength, resistance, the tool and sounds (want: as listed)");
	}

	// ---------------------------------------------------------------- drops

	/**
	 * Gilded blackstone (26.3's loot): 2 to 5 gold nuggets at 0.1, 0.143, 0.25 and 1 by Fortune's level, else itself.
	 * Nether gold ore: 2 to 6 nuggets, Fortune's ore bonus on top; 0 or 1 experience (EFR's 2 to 5).
	 */
	public void testDrops() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			Block gilded = block("gilded_blackstone");
			world().setBlock(X, Y, Z, gilded, 0, 3);
			for (int fortune = 0; fortune < 4; fortune++) {
				int nuggets = 0;
				TreeSet<Integer> counts = new TreeSet<>();
				int tries = 2000;
				for (int i = 0; i < tries; i++) {
					List<ItemStack> drops = gilded.getDrops(world(), X, Y, Z, 0, fortune);
					if (drops.size() == 1 && drops.get(0).getItem() == Items.gold_nugget) {
						nuggets++;
						counts.add(drops.get(0).stackSize);
					} else if (drops.size() != 1 || drops.get(0).getItem() != net.minecraft.item.Item.getItemFromBlock(gilded)) {
						out.append("gilded fortune ").append(fortune).append(" dropped ").append(drops).append("; ");
						break;
					}
				}
				double[] chance = {0.1, 0.14285715, 0.25, 1.0};
				double rate = nuggets / (double) tries;
				double sd = Math.sqrt(chance[fortune] * (1 - chance[fortune]) / tries);
				if (Math.abs(rate - chance[fortune]) > 4.5 * sd + 1e-9 || (nuggets > 0 && (counts.first() < 2 || counts.last() > 5))) {
					out.append("gilded fortune ").append(fortune).append(": nuggets ").append(rate).append(" in ").append(counts).append("; ");
				}
			}
			Block ore = block("nether_gold_ore");
			world().setBlock(X, Y, Z, ore, 0, 3);
			TreeSet<Integer> nuggets = new TreeSet<>();
			TreeSet<Integer> experience = new TreeSet<>();
			for (int i = 0; i < 500; i++) {
				// 1.7.10's getDrops: a stack a nugget
				int count = 0;
				for (ItemStack drop : ore.getDrops(world(), X, Y, Z, 0, 0)) {
					count += drop.getItem() == Items.gold_nugget ? drop.stackSize : 1000;
				}
				if (count >= 1000) {
					out.append("ore dropped other than nuggets; ");
					break;
				}
				nuggets.add(count);
				experience.add(ore.getExpDrop(world(), 0, 0));
			}
			out.append("ore nuggets ").append(nuggets.isEmpty() ? "none" : nuggets.first() + "-" + nuggets.last());
			out.append(", experience ").append(experience);
			world().setBlock(X, Y, Z, Blocks.air, 0, 3);
			return out.toString();
		});
		assertEqual("ore nuggets 2-6, experience [0, 1]", got, "the drops");
	}

	// ---------------------------------------------------------------- crying obsidian

	/** Crying obsidian: light 10, and a piston can't push it (26.3's `PistonBaseBlock.isPushable`), as obsidian. */
	public void testCryingObsidianStandsAndGlows() {
		String got = onServer(() -> {
			Block crying = block("crying_obsidian");
			StringBuilder out = new StringBuilder("light " + crying.getLightValue());
			world().setBlock(X - 1, Y, Z, Blocks.redstone_block, 0, 3);
			world().setBlock(X + 1, Y, Z, crying, 0, 3);
			world().setBlock(X, Y, Z, Blocks.piston, 5, 3);
			return out.toString();
		});
		serverTicks(6);
		got += onServer(() -> ", after a push: " + world().getBlock(X + 1, Y, Z).getUnlocalizedName().replace("tile.", "")
			+ " then " + world().getBlock(X + 2, Y, Z).getUnlocalizedName().replace("tile.", ""));
		assertEqual("light 10, after a push: " + McFly.name("crying_obsidian") + " then air", got, "crying obsidian");
	}

	/** Its tears (26.3's `animateTick`): in 1 of 5 display ticks, a tear on a random side but the top, if that side is open. */
	public void testCryingObsidianTears() throws Exception {
		onServer(() -> {
			world().setBlock(X, Y + 2, Z, block("crying_obsidian"), 0, 3);
			player().playerNetServerHandler.setPlayerLocation(X + 0.5, Y, Z + 3.5, 180.0F, 0.0F);
		});
		waitUntilOnClient("arriving", () -> Math.abs(mc().thePlayer.posZ - (Z + 3.5)) < 0.01D, 60);
		ticks(2);
		Block crying = block("crying_obsidian");
		int made = onClient(() -> {
			java.util.Random random = new java.util.Random(1);
			int before = particleCount("Tear");
			for (int i = 0; i < 300; i++) {
				crying.randomDisplayTick(mc().theWorld, X, Y + 2, Z, random);
			}
			return particleCount("Tear") - before;
		});
		// 1 in 5, then 5 of the 6 sides: 50 of 300 at the mean
		assertTrue(made > 25 && made < 80, "tears from 300 display ticks (26.3: 1 in 6 with every side open): " + made);
	}

	@SuppressWarnings("rawtypes")
	private static int particleCount(String name) {
		try {
			java.lang.reflect.Field field;
			try {
				field = net.minecraft.client.particle.EffectRenderer.class.getDeclaredField("fxLayers");
			} catch (NoSuchFieldException e) {
				field = net.minecraft.client.particle.EffectRenderer.class.getDeclaredField("field_78876_b");
			}
			field.setAccessible(true);
			int count = 0;
			for (List layer : (List[]) field.get(mc().effectRenderer)) {
				for (Object particle : layer) {
					if (particle.getClass().getSimpleName().contains(name)) {
						count++;
					}
				}
			}
			return count;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	// ---------------------------------------------------------------- the plate and the button

	/** The polished blackstone plate (26.3's `BlockSetType.POLISHED_BLACKSTONE`, mobs only): an item doesn't press it, a pig does. */
	public void testPlatePressedByMobsOnly() {
		onServer(() -> {
			world().setBlock(X, Y, Z, block("polished_blackstone_pressure_plate"), 0, 3);
			EntityItem item = new EntityItem(world(), X + 0.5, Y + 0.05, Z + 0.5, new ItemStack(Blocks.cobblestone));
			item.motionX = item.motionY = item.motionZ = 0;
			item.delayBeforeCanPickup = 1000;
			world().spawnEntityInWorld(item);
		});
		serverTicks(25);
		String got = onServer(() -> "an item " + world().getBlockMetadata(X, Y, Z));
		onServer(() -> {
			for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
				if (entity instanceof EntityItem) {
					((EntityItem) entity).setDead();
				}
			}
		});
		serverTicks(25);
		onServer(() -> {
			EntityPig pig = new EntityPig(world());
			pig.setLocationAndAngles(X + 0.5, Y + 0.05, Z + 0.5, 0, 0);
			world().spawnEntityInWorld(pig);
		});
		serverTicks(5);
		got += onServer(() -> ", a pig " + world().getBlockMetadata(X, Y, Z));
		assertEqual("an item 0, a pig 1", got, "the plate's meta (1 pressed)");
	}

	/** The polished blackstone button: 26.3's stone set, pressed for 20 ticks. */
	public void testButtonHoldsTwentyTicks() {
		onServer(() -> {
			world().setBlock(X, Y, Z, Blocks.stone, 0, 3);
			world().setBlock(X, Y + 1, Z, block("polished_blackstone_button"), 5, 3);
			Block button = world().getBlock(X, Y + 1, Z);
			button.onBlockActivated(world(), X, Y + 1, Z, player(), 1, 0.5F, 0.0F, 0.5F);
		});
		serverTicks(15);
		String got = onServer(() -> "at 15 " + (world().getBlockMetadata(X, Y + 1, Z) & 8));
		serverTicks(10);
		got += onServer(() -> ", at 25 " + (world().getBlockMetadata(X, Y + 1, Z) & 8));
		assertEqual("at 15 8, at 25 0", got, "the button's pressed bit");
	}

	// ---------------------------------------------------------------- recipes

	/** 26.3's recipes for the stones (its jar's, `netherstones_recipes_26_3.txt`), each giving 26.3's result here. */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("netherstones_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(32, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}
}
