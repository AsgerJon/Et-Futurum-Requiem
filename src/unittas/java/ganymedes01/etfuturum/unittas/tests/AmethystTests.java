package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Amethyst as our own (SPEC-modern-movement.md §90.4, step B), as 26.3's: the block, budding amethyst, the buds and
 * cluster (EFR's two ids, meta facing + 6 x size), tinted glass, the shard. Ours, EFR's classes gone; EFR's stored form
 * by 26.3's names; 26.3's strength, light, sounds (the medium and large buds' swapped, as 26.3 has them) and colours;
 * budding amethyst's growth and drops; the cluster's drops (4 shards to a pickaxe, Fortune's `ore_drops`, 2 by hand, itself
 * to Silk Touch; buds by Silk Touch only); a bud gone with its support, no drops; placing facing the clicked face; tinted
 * glass dropping itself, blocking light and the beacon's beam; 26.3's recipes. At z 300, y 8 and up.
 */
@TasSuite("caves")
public class AmethystTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 300;
	private static final String[] BLOCKS = {"amethyst_block", "budding_amethyst", "amethyst_cluster_1", "amethyst_cluster_2", "tinted_glass"};
	private static final String[] EFR_CLASSES = {"blocks.BlockAmethystBlock", "blocks.BlockBuddingAmethyst", "blocks.BlockAmethystCluster",
		"blocks.itemblocks.ItemBlockAmethystCluster", "client.renderer.block.BlockAmethystClusterRenderer", "blocks.BlockTintedGlass"};

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

	private void clear() {
		for (int x = X - 2; x <= X + 2; x++) {
			for (int y = Y - 2; y <= Y + 3; y++) {
				for (int z = Z - 2; z <= Z + 2; z++) {
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
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 3, Y - 3, Z - 3, X + 4, Y + 5, Z + 4))) {
			if (!((EntityItem) entity).isDead) {
				found.add((EntityItem) entity);
			}
		}
		return found;
	}

	/** What lies dropped, as "item:damage xcount" summed, sorted. */
	private String dropped() {
		Map<String, Integer> counts = new java.util.TreeMap<>();
		for (EntityItem item : items()) {
			ItemStack stack = item.getEntityItem();
			counts.merge(Item.itemRegistry.getNameForObject(stack.getItem()).replace(McFly.DOMAIN + ":", "") + ":" + stack.getItemDamage(), stack.stackSize, Integer::sum);
		}
		return counts.toString();
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		String facing = found.state().properties().get("facing");
		return found.type().id.replace("minecraft:", "") + (facing != null ? "," + facing : "");
	}

	/** The break sound a block there plays: its own, or what the client's per-variant table gives it (on the server thread). */
	private String breakSound(int x, int y, int z) {
		Block block = world().getBlock(x, y, z);
		String own = block.stepSound.getBreakSound();
		Object variants = MultiBlockSoundRegistry.multiBlockSounds.get(block);
		if (variants == null) {
			return own;
		}
		String got = ((ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer) variants).getSound(world(), x, y, z, own,
			MultiBlockSoundRegistry.BlockSoundType.BREAK);
		return got != null ? got : own;
	}

	/** The player breaks (X, Y, Z) as survival does, with this held. */
	private void harvest(ItemStack held) {
		player().inventory.setInventorySlotContents(player().inventory.currentItem, held);
		player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
	}

	private static ItemStack enchanted(Item item, Enchantment enchantment, int level) {
		ItemStack stack = new ItemStack(item);
		stack.addEnchantment(enchantment, level);
		return stack;
	}

	public void testAmethystIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : BLOCKS) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		Object shard = Item.itemRegistry.getObject(McFly.DOMAIN + ":amethyst_shard");
		if (!(shard instanceof Item) || !shard.getClass().getName().startsWith("io.github.asgerjon.mcfly.")) {
			wrong.add("amethyst_shard " + (shard == null ? "missing" : shard.getClass().getName()));
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.add(name + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "amethyst's classes");
	}

	/** EFR's ids and metas by 26.3's names: the clusters' facing + 6 x size. */
	public void testStoredForm() {
		String got = onServer(() -> name(block("amethyst_block"), 0) + " " + name(block("budding_amethyst"), 0) + "; " + name(block("amethyst_cluster_1"), 1)
			+ " " + name(block("amethyst_cluster_1"), 2) + " " + name(block("amethyst_cluster_1"), 7) + " " + name(block("amethyst_cluster_2"), 0) + " "
			+ name(block("amethyst_cluster_2"), 11) + "; " + name(block("tinted_glass"), 0));
		assertEqual("amethyst_block budding_amethyst; small_amethyst_bud,up small_amethyst_bud,north medium_amethyst_bud,up large_amethyst_bud,down "
			+ "amethyst_cluster,east; tinted_glass", got, "26.3's names");
	}

	/**
	 * 26.3's: the block and budding 1.5 and 1.5, purple, AMETHYST; the buds and cluster 1.5 and 1.5, light 1, 2, 4, 5, their
	 * sounds (the medium bud the large's and the large the medium's, as 26.3); tinted glass 0.3, glass's sound, gray.
	 */
	public void testStrengthLightSounds() {
		String[][] want = {{"amethyst_block", "0", "1.5 1.5 0 block.amethyst_block.break"}, {"budding_amethyst", "0", "1.5 1.5 0 block.amethyst_block.break"},
			{"amethyst_cluster_1", "1", "1.5 1.5 1 block.small_amethyst_bud.break"}, {"amethyst_cluster_1", "7", "1.5 1.5 2 block.large_amethyst_bud.break"},
			{"amethyst_cluster_2", "1", "1.5 1.5 4 block.medium_amethyst_bud.break"}, {"amethyst_cluster_2", "7", "1.5 1.5 5 block.amethyst_cluster.break"}};
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 2);
			for (String[] entry : want) {
				Block block = block(entry[0]);
				world().setBlock(X, Y, Z, block, Integer.parseInt(entry[1]), 2);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null, world(), X, Y, Z, X, Y, Z) + " "
					+ block.getLightValue(world(), X, Y, Z) + " " + breakSound(X, Y, Z).replace(McFly.MC_ASSETS + ":", "");
				if (!entry[2].equals(got) || block.getMapColor(0) != MapColor.purpleColor) {
					found.add(entry[0] + ":" + entry[1] + " " + got + " colour " + block.getMapColor(0).colorIndex);
				}
			}
			Block glass = block("tinted_glass");
			world().setBlock(X, Y, Z, glass, 0, 2);
			String got = glass.getBlockHardness(world(), X, Y, Z) + " " + glass.getExplosionResistance(null, world(), X, Y, Z, X, Y, Z) + " "
				+ glass.stepSound.getBreakSound() + " " + glass.getMapColor(0).colorIndex;
			if (!("0.3 0.3 dig.glass " + MapColor.grayColor.colorIndex).equals(got)) {
				found.add("tinted_glass " + got);
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, resistance, light, sound, colour (want: as listed)");
	}

	/** Budding amethyst with one face open (up): random ticks grow a small bud there facing up, then a size at a time to a cluster. */
	public void testBuddingGrowsACluster() {
		String got = onServer(() -> {
			for (int[] side : new int[][]{{0, -1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}}) {
				world().setBlock(X + side[0], Y + side[1], Z + side[2], Blocks.stone, 0, 2);
			}
			Block budding = block("budding_amethyst");
			world().setBlock(X, Y, Z, budding, 0, 2);
			Random random = new Random(5);
			List<String> stages = new ArrayList<>();
			for (int i = 0; i < 3000; i++) {
				if (budding instanceof io.github.asgerjon.mcfly.base.RandomTicking ticking) {
					ticking.randomTick(world(), X, Y, Z, random);
				} else {
					budding.updateTick(world(), X, Y, Z, random);
				}
				String now = name(world().getBlock(X, Y + 1, Z), world().getBlockMetadata(X, Y + 1, Z));
				if (!world().isAirBlock(X, Y + 1, Z) && (stages.isEmpty() || !stages.get(stages.size() - 1).equals(now))) {
					stages.add(now);
				}
			}
			return stages.toString();
		});
		assertEqual("[small_amethyst_bud,up, medium_amethyst_bud,up, large_amethyst_bud,up, amethyst_cluster,up]", got, "what grew above it");
	}

	/** 26.3's loot: budding amethyst nothing (even to Silk Touch); the block itself to a pickaxe. */
	public void testBlockDrops() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (String name : new String[]{"budding_amethyst", "amethyst_block"}) {
				world().setBlock(X, Y, Z, block(name), 0, 2);
				harvest(enchanted(Items.iron_pickaxe, Enchantment.silkTouch, 1));
				out.append(name).append(" silk ").append(dropped()).append("; ");
				clear();
				world().setBlock(X, Y, Z, block(name), 0, 2);
				harvest(new ItemStack(Items.iron_pickaxe));
				out.append("pickaxe ").append(dropped()).append("; ");
				clear();
			}
			return out.toString();
		});
		assertEqual("budding_amethyst silk {}; pickaxe {}; amethyst_block silk {amethyst_block:0=1}; pickaxe {amethyst_block:0=1}; ", got, "the drops");
	}

	/** The cluster: 4 shards to a pickaxe, 2 by hand, itself to Silk Touch; a bud only to Silk Touch. */
	public void testClusterDrops() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			Object[][] cases = {{"amethyst_cluster_2", 7, new ItemStack(Items.iron_pickaxe)}, {"amethyst_cluster_2", 7, null},
				{"amethyst_cluster_2", 7, enchanted(Items.iron_pickaxe, Enchantment.silkTouch, 1)}, {"amethyst_cluster_1", 1, new ItemStack(Items.iron_pickaxe)},
				{"amethyst_cluster_1", 7, enchanted(Items.iron_pickaxe, Enchantment.silkTouch, 1)}};
			for (Object[] c : cases) {
				world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 2);
				world().setBlock(X, Y, Z, block((String) c[0]), (Integer) c[1], 2);
				harvest((ItemStack) c[2]);
				out.append(dropped()).append("; ");
				clear();
			}
			return out.toString();
		});
		assertEqual("{amethyst_shard:0=4}; {amethyst_shard:0=2}; {amethyst_cluster_2:6=1}; {}; {amethyst_cluster_1:6=1}; ", got,
			"pickaxe, hand, Silk Touch on the cluster; a pickaxe on a small bud, Silk Touch on a medium bud");
	}

	/** Fortune III on the cluster: 26.3's `ore_drops`, 4 x (1 + max(0, rand(5) - 1)), 8.8 shards a cluster on average (EFR's 6.4). */
	public void testClusterFortune() {
		double mean = onServer(() -> {
			int shards = 0, runs = 300;
			for (int i = 0; i < runs; i++) {
				world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 2);
				world().setBlock(X, Y, Z, block("amethyst_cluster_2"), 7, 2);
				harvest(enchanted(Items.iron_pickaxe, Enchantment.fortune, 3));
				for (EntityItem item : items()) {
					shards += item.getEntityItem().stackSize;
					item.setDead();
				}
			}
			return (double) shards / runs;
		});
		assertTrue(mean > 8.0 && mean < 9.6, "Fortune III's shards a cluster on average: " + mean);
	}

	/** A cluster facing up on stone: the stone gone, the cluster is air, nothing dropped (26.3's `updateShape`). */
	public void testClusterGoesWithItsSupport() {
		String got = onServer(() -> {
			world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 2);
			world().setBlock(X, Y, Z, block("amethyst_cluster_2"), 7, 2);
			world().setBlock(X, Y - 1, Z, Blocks.air, 0, 3);
			return (world().isAirBlock(X, Y, Z) ? "air" : "still there") + " " + dropped();
		});
		assertEqual("air {}", got, "the cluster after its support went");
	}

	/** A medium bud's item on the north face of stone: a medium bud north of it, facing north (meta 2 + 6). */
	public void testPlacingFacesTheClickedFace() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, Blocks.stone, 0, 2);
			ItemStack bud = new ItemStack(block("amethyst_cluster_1"), 1, 6);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, bud);
			bud.getItem().onItemUse(bud, player(), world(), X, Y, Z, 2, 0.5F, 0.5F, 0.0F);
			return name(world().getBlock(X, Y, Z - 1), world().getBlockMetadata(X, Y, Z - 1)) + " " + world().getBlockMetadata(X, Y, Z - 1);
		});
		assertEqual("medium_amethyst_bud,north 8", got, "the bud placed on stone's north face");
	}

	/** Tinted glass: dropped itself by hand (glass isn't), all light stopped, the beacon's beam too. */
	public void testTintedGlass() {
		String got = onServer(() -> {
			Block glass = block("tinted_glass");
			world().setBlock(X, Y, Z, glass, 0, 2);
			String beam = ganymedes01.etfuturum.core.utils.BeaconBeam.passes(world(), X, Y, Z) ? "passes" : "stopped";
			int opacity = glass.getLightOpacity(world(), X, Y, Z);
			harvest(null);
			return "opacity " + opacity + ", beam " + beam + ", by hand " + dropped();
		});
		assertEqual("opacity 255, beam stopped, by hand {tinted_glass:0=1}", got, "tinted glass");
	}

	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("amethyst_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(2, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}
}
