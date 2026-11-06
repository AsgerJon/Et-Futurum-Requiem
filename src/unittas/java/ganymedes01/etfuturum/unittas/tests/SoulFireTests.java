package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Soul soil, soul fire and basalt from lava as our own (SPEC-modern-movement.md §88.6, step E), as 26.3's: ours, EFR's
 * classes and soul fire mixins gone; soul soil's strength and sound; fire on soul sand or soul soil becomes McFly's soul
 * fire (26.3's `BaseFireBlock.getState`), placed or, as EFR stored it (`minecraft:fire` over a soul base), on its next
 * tick; soul fire's light 10, its 2 damage, never spreading or ageing, gone without its base; lava over soul soil beside
 * blue ice becomes basalt (26.3's `LiquidBlock`), not over soul sand. At z 140 on the flat world.
 */
@TasSuite("soulfire")
public class SoulFireTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 140;
	private static final String[] EFR_CLASSES = {"blocks.BlockSoulSoil", "mixins.early.soulfire.MixinBlockFire", "mixins.early.soulfire.MixinEntity",
		"mixins.early.soulfire.MixinRenderBlocks", "mixins.early.soulfire.MixinBlockFireRPLE", "ducks.ISoulFireInfo"};

	@Override
	protected void setUp() {
		clear();
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			world().getWorldInfo().setRaining(false);
		});
	}

	@Override
	protected void tearDown() {
		clear();
		onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
	}

	private void clear() {
		onServer(() -> {
			for (int x = X - 3; x <= X + 8; x++) {
				for (int y = Y - 1; y <= Y + 3; y++) {
					for (int z = Z - 3; z <= Z + 3; z++) {
						world().setBlock(x, y, z, y == Y - 1 ? Blocks.stone : Blocks.air, 0, 2);
					}
				}
			}
			for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
				if (entity instanceof EntityPig || entity instanceof net.minecraft.entity.item.EntityItem) {
					((net.minecraft.entity.Entity) entity).setDead();
				}
			}
		});
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	private static Block blueIce() {
		BlockIds.Type type = BlockIds.byId("minecraft:blue_ice");
		assertTrue(type != null, "no blue ice");
		return type.defaultState().block();
	}

	private static String at(int x, int y, int z) {
		Block block = world().getBlock(x, y, z);
		return Block.blockRegistry.getNameForObject(block).replace("minecraft:", "").replace(McFly.DOMAIN + ":", "");
	}

	// ---------------------------------------------------------------- ours

	public void testSoulFireIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : new String[]{"soul_soil", "soul_fire"}) {
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
		assertEqual("[]", wrong.toString(), "soul fire's classes");
	}

	/** Soul soil: 0.5 and 0.5, a shovel's (by hand too), its sound; 26.3's names for both. */
	public void testSoulSoil() {
		String got = onServer(() -> {
			Block soil = block("soul_soil");
			world().setBlock(X, Y, Z, soil, 0, 3);
			String out = soil.getBlockHardness(world(), X, Y, Z) + " " + soil.getExplosionResistance(null, world(), X, Y, Z, X, Y, Z) + " "
				+ soil.getHarvestTool(0) + " " + soil.canHarvestBlock(player(), 0) + " " + soil.stepSound.getBreakSound() + " "
				+ BlockIds.find(soil, 0).type().id + " " + BlockIds.find(block("soul_fire"), 0).type().id;
			world().setBlock(X, Y, Z, Blocks.air, 0, 3);
			return out;
		});
		assertEqual("0.5 0.5 shovel true " + McFly.MC_ASSETS + ":block.soul_soil.break minecraft:soul_soil minecraft:soul_fire", got, "soul soil");
	}

	// ---------------------------------------------------------------- becoming soul fire

	/** Fire put on soul sand or soul soil becomes soul fire; on stone it stays fire. */
	public void testFireOnASoulBaseIsSoulFire() {
		String got = onServer(() -> {
			world().setBlock(X, Y - 1, Z, Blocks.soul_sand, 0, 3);
			world().setBlock(X + 2, Y - 1, Z, block("soul_soil"), 0, 3);
			world().setBlock(X, Y, Z, Blocks.fire, 0, 3);
			world().setBlock(X + 2, Y, Z, Blocks.fire, 0, 3);
			world().setBlock(X + 4, Y, Z, Blocks.fire, 0, 3);
			return at(X, Y, Z) + " " + at(X + 2, Y, Z) + " " + at(X + 4, Y, Z);
		});
		assertEqual("soul_fire soul_fire fire", got, "fire on soul sand, soul soil, stone");
	}

	/** EFR's soul fire, `minecraft:fire` over a soul base as saved (no placing): soul fire on its next tick. */
	public void testEfrSoulFireBecomesOursOnItsTick() {
		String got = onServer(() -> {
			world().setBlock(X, Y - 1, Z, Blocks.soul_sand, 0, 3);
			// as a chunk loads it: the block in its storage, no placing callbacks
			ExtendedBlockStorage storage = world().getChunkFromBlockCoords(X, Z).getBlockStorageArray()[Y >> 4];
			storage.func_150818_a(X & 15, Y & 15, Z & 15, Blocks.fire);
			String before = at(X, Y, Z);
			Blocks.fire.updateTick(world(), X, Y, Z, new Random(1));
			return before + " then " + at(X, Y, Z);
		});
		assertEqual("fire then soul_fire", got, "EFR's soul fire, ticked");
	}

	// ---------------------------------------------------------------- soul fire

	/** Light 10; no ageing or spreading in 200 ticks beside planks; gone when its base is. */
	public void testSoulFireStaysAndGoes() {
		onServer(() -> {
			world().setBlock(X, Y - 1, Z, Blocks.soul_sand, 0, 3);
			world().setBlock(X + 1, Y, Z, Blocks.planks, 0, 3);
			world().setBlock(X, Y, Z, block("soul_fire"), 0, 3);
		});
		serverTicks(200);
		String got = onServer(() -> {
			Block fire = world().getBlock(X, Y, Z);
			String out = "light " + fire.getLightValue() + ", " + at(X, Y, Z) + " meta " + world().getBlockMetadata(X, Y, Z) + ", planks " + at(X + 1, Y, Z)
				+ ", above them " + at(X + 1, Y + 1, Z);
			world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 3);
			return out + "; base gone: " + at(X, Y, Z);
		});
		assertEqual("light 10, soul_fire meta 0, planks planks, above them air; base gone: air", got, "soul fire");
	}

	/** In soul fire a pig takes 26.3's 2 a hit (fire's 1). */
	public void testSoulFireHurtsTwo() {
		float[] health = new float[2];
		onServer(() -> {
			world().setBlock(X, Y - 1, Z, Blocks.soul_sand, 0, 3);
			world().setBlock(X, Y, Z, block("soul_fire"), 0, 3);
			EntityPig pig = new EntityPig(world());
			pig.setLocationAndAngles(X + 0.5, Y, Z + 0.5, 0, 0);
			world().spawnEntityInWorld(pig);
			health[0] = pig.getHealth();
		});
		waitUntil("the pig hurt", () -> {
			for (Object entity : world().loadedEntityList) {
				if (entity instanceof EntityPig && ((EntityPig) entity).hurtTime > 0) {
					health[1] = ((EntityPig) entity).getHealth();
					return true;
				}
			}
			return false;
		}, 100);
		assertEqual(2.0F, health[0] - health[1], "the first hit's damage");
	}

	// ---------------------------------------------------------------- basalt

	/** Lava over soul soil with blue ice beside it: basalt. Without the ice, or over soul sand: lava. */
	public void testBasaltFromLava() {
		String got = onServer(() -> {
			world().setBlock(X, Y - 1, Z, block("soul_soil"), 0, 3);
			world().setBlock(X + 1, Y, Z, blueIce(), 0, 3);
			world().setBlock(X, Y, Z, Blocks.lava, 0, 3);
			world().setBlock(X + 4, Y - 1, Z, block("soul_soil"), 0, 3);
			world().setBlock(X + 4, Y, Z, Blocks.lava, 0, 3);
			world().setBlock(X + 7, Y - 1, Z, Blocks.soul_sand, 0, 3);
			world().setBlock(X + 8, Y, Z, blueIce(), 0, 3);
			world().setBlock(X + 7, Y, Z, Blocks.lava, 0, 3);
			return at(X, Y, Z) + " " + at(X + 4, Y, Z) + " " + at(X + 7, Y, Z);
		});
		assertEqual("basalt lava lava", got, "with ice over soul soil, without ice, over soul sand");
	}
}
