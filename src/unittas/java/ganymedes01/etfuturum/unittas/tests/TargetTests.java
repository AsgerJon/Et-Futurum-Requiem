package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * The target and the lodestone as our own (SPEC-modern-movement.md §88.8, step G), as 26.3's: ours, EFR's classes gone
 * (the lodestone's four moved into McFly's package); the target's strength and sound; a hit in the middle of a face gives
 * 15, weak power only, for 20 ticks from an arrow and 8 from anything else (26.3's `TargetBlock`). At z 180.
 */
@TasSuite("target")
public class TargetTests extends TasTestCase {

	private static final int X = 40, Y = 6, Z = 180;
	private static final String[] EFR_CLASSES = {"blocks.BlockTarget", "blocks.BlockLodestone", "items.LodestoneCompass", "client.LodestoneNeedle",
		"world.LodestoneRecord"};

	@Override
	protected void setUp() {
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			world().setBlock(X, Y, Z, block("target"), 0, 3);
		});
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			world().setBlock(X, Y, Z, Blocks.air, 0, 3);
			for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
				if (entity instanceof EntityArrow || entity instanceof EntitySnowball) {
					((net.minecraft.entity.Entity) entity).setDead();
				}
			}
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	public void testTargetAndLodestoneAreOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : new String[]{"target", "lodestone"}) {
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
		assertEqual("[]", wrong.toString(), "the target's and the lodestone's classes");
	}

	/** 26.3's: 0.5, a hoe's, grass's sounds. */
	public void testStrength() {
		String got = onServer(() -> {
			Block target = block("target");
			return target.getBlockHardness(world(), X, Y, Z) + " " + target.getExplosionResistance(null, world(), X, Y, Z, X, Y, Z) + " "
				+ target.getHarvestTool(0) + " " + target.stepSound.getBreakSound();
		});
		assertEqual("0.5 0.5 hoe dig.grass", got, "hardness, resistance, tool, sound");
	}

	/** An arrow in the middle of the north face: 15, weak only, still on at 15 ticks, off by 25 (26.3's 20). */
	public void testArrowInTheMiddle() {
		// an arrow as it sticks in the middle of the north face (1.7.10's arrow then calls onEntityCollidedWithBlock)
		onServer(() -> {
			EntityArrow arrow = new EntityArrow(world(), X + 0.5, Y + 0.5, Z - 0.05);
			world().getBlock(X, Y, Z).onEntityCollidedWithBlock(world(), X, Y, Z, arrow);
		});
		String got = onServer(() -> {
			Block target = world().getBlock(X, Y, Z);
			return "power " + target.isProvidingWeakPower(world(), X, Y, Z, 2) + ", strong " + target.isProvidingStrongPower(world(), X, Y, Z, 2);
		});
		serverTicks(15);
		got += onServer(() -> ", at 15 " + world().getBlockMetadata(X, Y, Z));
		serverTicks(10);
		got += onServer(() -> ", at 25 " + world().getBlockMetadata(X, Y, Z));
		assertEqual("power 15, strong 0, at 15 15, at 25 0", got, "the target after an arrow");
	}

	/** A snowball in the middle: 15 for 8 ticks (off by 12). */
	public void testSnowballIsShort() {
		// a snowball as it strikes the middle of the north face (the fork's throwable fix then calls onEntityCollidedWithBlock)
		onServer(() -> {
			EntitySnowball ball = new EntitySnowball(world(), X + 0.5, Y + 0.5, Z - 0.05);
			world().getBlock(X, Y, Z).onEntityCollidedWithBlock(world(), X, Y, Z, ball);
		});
		String got = onServer(() -> "power " + world().getBlockMetadata(X, Y, Z));
		serverTicks(12);
		got += onServer(() -> ", at 12 " + world().getBlockMetadata(X, Y, Z));
		assertEqual("power 15, at 12 0", got, "the target after a snowball");
	}
}
