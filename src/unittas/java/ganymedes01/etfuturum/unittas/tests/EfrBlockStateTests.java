package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.Map;

/**
 * EFR's blocks of no vanilla shape in commands (SPEC-modern-movement.md §64.3, handoff upcoming 32): `/setblock` gives
 * 26.3's default state, given properties land in the meta, and `BlockIds.find` reads them back. Each at x 60.., z 60.
 */
@TasSuite("quick5")
public class EfrBlockStateTests extends TasTestCase {

	private static final int X = 60, Y = 4, Z = 60;

	@Override
	protected void tearDown() {
		onServer(() -> {
			for (int y = Y; y <= Y + 2; y++) {
				world().setBlockToAir(X, y, Z);
			}
		});
	}

	public void testLightningRod() {
		need(McFlyBlocks.LIGHTNING_ROD);
		assertEqual(1, meta("minecraft:lightning_rod", McFlyBlocks.LIGHTNING_ROD.get()), "the rod's default (up)");
		assertEqual(10, meta("minecraft:lightning_rod[facing=north,powered=true]", McFlyBlocks.LIGHTNING_ROD.get()), "north, powered");
		assertFound(McFlyBlocks.LIGHTNING_ROD.get(), 10, "facing", "north", "powered", "true");
		assertEqual(0, meta("minecraft:oxidized_lightning_rod[facing=down]", McFlyBlocks.OXIDIZED_LIGHTNING_ROD.get()), "an oxidized one down");
	}

	public void testEndRod() {
		need(io.github.asgerjon.mcfly.registry.McFlyBlocks.END_ROD);
		assertEqual(1, meta("minecraft:end_rod", io.github.asgerjon.mcfly.registry.McFlyBlocks.END_ROD.get()), "the end rod's default (up)");
		assertEqual(5, meta("minecraft:end_rod[facing=east]", io.github.asgerjon.mcfly.registry.McFlyBlocks.END_ROD.get()), "east");
		assertFound(io.github.asgerjon.mcfly.registry.McFlyBlocks.END_ROD.get(), 4, "facing", "west");
	}

	public void testObserver() {
		need(io.github.asgerjon.mcfly.registry.McFlyBlocks.OBSERVER);
		assertEqual(3, meta("minecraft:observer", io.github.asgerjon.mcfly.registry.McFlyBlocks.OBSERVER.get()), "the observer's default (south)");
		assertEqual(9, meta("minecraft:observer[facing=up,powered=true]", io.github.asgerjon.mcfly.registry.McFlyBlocks.OBSERVER.get()), "up, powered");
	}

	public void testBarrel() {
		need(McFlyBlocks.BARREL);
		assertEqual(2, meta("minecraft:barrel", McFlyBlocks.BARREL.get()), "the barrel's default (north)");
		assertEqual(8, meta("minecraft:barrel[facing=down,open=true]", McFlyBlocks.BARREL.get()), "down, open");
		assertFound(McFlyBlocks.BARREL.get(), 9, "facing", "up", "open", "true");
	}

	public void testWorkstations() {
		need(ModBlocks.STONECUTTER);
		assertEqual(0, meta("minecraft:stonecutter", ModBlocks.STONECUTTER.get()), "the stonecutter's default (north)");
		assertEqual(3, meta("minecraft:stonecutter[facing=east]", ModBlocks.STONECUTTER.get()), "east");
		need(ModBlocks.LOOM);
		assertEqual(1, meta("minecraft:loom[facing=south]", ModBlocks.LOOM.get()), "the loom south");
	}

	public void testGlazedTerracotta() {
		need(ModBlocks.WHITE_GLAZED_TERRACOTTA);
		assertEqual(2, meta("minecraft:white_glazed_terracotta", ModBlocks.WHITE_GLAZED_TERRACOTTA.get()), "the default (north)");
		assertEqual(1, meta("minecraft:white_glazed_terracotta[facing=west]", ModBlocks.WHITE_GLAZED_TERRACOTTA.get()), "west");
		assertFound(ModBlocks.WHITE_GLAZED_TERRACOTTA.get(), 0, "facing", "south");
	}

	public void testBeehive() {
		need(ModBlocks.BEEHIVE);
		assertEqual(2, meta("minecraft:beehive", ModBlocks.BEEHIVE.get()), "the beehive's default (north)");
		assertEqual(5, meta("minecraft:beehive[facing=east]", ModBlocks.BEEHIVE.get()), "east");
		assertEqual(8, meta("minecraft:beehive[honey_level=5]", ModBlocks.BEEHIVE.get()), "full of honey, north");
	}

	public void testChain() {
		String id = BlockIds.find(io.github.asgerjon.mcfly.registry.McFlyBlocks.CHAIN.get(), 0).type().id;
		assertEqual(0, meta(id, io.github.asgerjon.mcfly.registry.McFlyBlocks.CHAIN.get()), "the chain's default (y)");
		assertEqual(1, meta(id + "[axis=x]", io.github.asgerjon.mcfly.registry.McFlyBlocks.CHAIN.get()), "x");
		assertEqual(2, meta(id + "[axis=z]", io.github.asgerjon.mcfly.registry.McFlyBlocks.CHAIN.get()), "z");
	}

	public void testLantern() {
		onServer(() -> world().setBlock(X, Y + 1, Z, Blocks.stone));
		assertEqual(0, meta("minecraft:lantern", io.github.asgerjon.mcfly.registry.McFlyBlocks.LANTERN.get()), "the lantern's default (standing)");
		assertEqual(1, meta("minecraft:lantern[hanging=true]", io.github.asgerjon.mcfly.registry.McFlyBlocks.LANTERN.get()), "hanging");
	}

	public void testAmethystCluster() {
		need(McFlyBlocks.AMETHYST_CLUSTER_2);
		onServer(() -> world().setBlock(X, Y + 1, Z, Blocks.stone));
		Block cluster = McFlyBlocks.AMETHYST_CLUSTER_2.get();
		int up = meta("minecraft:amethyst_cluster", cluster);
		assertEqual(1, up % 6, "the cluster's default (up), meta " + up);
		int down = meta("minecraft:amethyst_cluster[facing=down]", cluster);
		assertEqual(0, down % 6, "down, meta " + down);
		assertEqual(up / 6, down / 6, "the same size");
	}

	// ---------------------------------------------------------------- helpers

	private static void need(ModBlocks block) {
		if (!block.isEnabled()) {
			skipTest(block.name() + " is off");
		}
	}

	private static void need(McFlyBlocks block) {
		if (!block.isEnabled()) {
			skipTest(block.name() + " is off");
		}
	}

	/** Puts the block in with /setblock (air first, so it always changes) and returns the meta there. */
	private int meta(String block, Block expected) {
		onServer(() -> world().setBlockToAir(X, Y, Z));
		assertNoError(command("setblock " + X + " " + Y + " " + Z + " " + block));
		Block placed = onServer(() -> world().getBlock(X, Y, Z));
		assertEqual(expected, placed, block + " put another block");
		return onServer(() -> world().getBlockMetadata(X, Y, Z));
	}

	/** The modern properties BlockIds reads from the block and meta include the given ones. */
	private static void assertFound(Block block, int meta, String... properties) {
		BlockIds.Found found = BlockIds.find(block, meta);
		assertIsNotNone(found, "nothing found for meta " + meta);
		Map<String, String> read = found.state().properties();
		for (int i = 0; i < properties.length; i += 2) {
			assertEqual(properties[i + 1], read.get(properties[i]), found.type().id + " meta " + meta + "'s " + properties[i] + " (all: " + read + ")");
		}
	}
}
