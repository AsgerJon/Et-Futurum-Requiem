package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.ArrayList;
import java.util.List;

/**
 * EFR's experiment options gone at the wood sets' end (SPEC-modern-movement.md 91.13, step K): crimson's, warped's
 * and mangrove's experiments, with EFR's dead copies of the Nether options McFly took (`masterNetherToggle` and the six
 * it switched); the crimson and warped forests made by McFly's nylium, as their siblings go by their own blocks. Read
 * by reflection and ids, so it compiles on the code before.
 */
@TasSuite("woods")
public class WoodOptionTests extends TasTestCase {

	private static final String CONFIGS = "ganymedes01.etfuturum.configuration.configs.";

	public void testEfrsExperimentsGone() throws Exception {
		String[][] fields = {
			{CONFIGS + "ConfigExperiments", "enableCrimsonBlocks", "enableWarpedBlocks", "enableMangroveBlocks"},
			{CONFIGS + "ConfigBlocksItems", "woodVariants", "enableNetherwartBlock", "enableBlackstone", "enableSoulSoil",
				"enableSoulLighting", "enableBasalt"},
			{"ganymedes01.etfuturum.recipes.ModRecipes", "modernWoodTypesEnabled", "modernWoodTypes"}};
		List<String> left = new ArrayList<>();
		for (String[] row : fields) {
			Class<?> type = Class.forName(row[0]);
			for (int i = 1; i < row.length; i++) {
				try {
					type.getDeclaredField(row[i]);
					left.add(type.getSimpleName() + "." + row[i]);
				} catch (NoSuchFieldException gone) {
					// as it should be
				}
			}
		}
		assertEqual("[]", left.toString(), "EFR's fields still there");
	}

	/**
	 * At their ids in EFR's world config (200 and 201 unless set), named as EFR names them, with the nylium on. EFR makes
	 * its Nether biomes only with its own Nether provider (`netherDimensionProvider`, an experiment, off): without it
	 * there are none to see.
	 */
	public void testNetherForestsByTheNylium() {
		if (!ganymedes01.etfuturum.configuration.configs.ConfigExperiments.netherDimensionProvider) {
			skipTest("EFR's Nether provider is off (netherDimensionProvider): EFR makes no Nether biomes");
		}
		if (!McFlyBlocks.NYLIUM.isEnabled()) {
			skipTest("the nylium is off (enableNetherPlants)");
		}
		if (ConfigWorld.crimsonForestID < 0 || ConfigWorld.warpedForestID < 0) {
			skipTest("a forest's id is -1 in world.cfg");
		}
		String got = onServer(() -> name(ConfigWorld.crimsonForestID) + ", " + name(ConfigWorld.warpedForestID));
		assertEqual("Crimson Forest, Warped Forest", got, "the biomes at the forests' ids");
	}

	private static String name(int id) {
		BiomeGenBase biome = id < BiomeGenBase.getBiomeGenArray().length ? BiomeGenBase.getBiomeGenArray()[id] : null;
		return biome == null ? "none" : biome.biomeName;
	}
}
