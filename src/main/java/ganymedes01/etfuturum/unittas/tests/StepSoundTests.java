package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Footsteps on iron trapdoors (SPEC-modern-movement.md §53.7). 26.3 plays the sound of the block 0.2 below the feet (in
 * the column of the block stood on), as 1.7.10 does: a top-half trapdoor sounds like itself, a bottom-half one (3/16
 * thick) like the block under it, and over air like nothing. The lanes run south, eight trapdoors long, each test
 * walking one from its start. The iron trapdoor sounds as 1.7.10's metal (step.stone), the grass as step.grass.
 */
public class StepSoundTests extends TasTestCase {

	/** Bottom-half trapdoors on the grass, top-half ones in place of the grass, bottom-half ones over a pit. */
	private static final int ON_GRASS = 10, IN_GROUND = 12, OVER_AIR = 14, LENGTH = 8;

	@Override
	protected void setUp() {
		if (!ModBlocks.IRON_TRAPDOOR.isEnabled()) {
			skipTest("no iron trapdoor");
		}
		onServer(() -> {
			for (int z = 0; z < LENGTH; z++) {
				world().setBlock(ON_GRASS, 4, z, ModBlocks.IRON_TRAPDOOR.get(), 0, 2);
				world().setBlock(IN_GROUND, 3, z, ModBlocks.IRON_TRAPDOOR.get(), 8, 2);
				world().setBlock(OVER_AIR, 3, z, Blocks.air, 0, 2);
				world().setBlock(OVER_AIR, 4, z, ModBlocks.IRON_TRAPDOOR.get(), 0, 2);
			}
		});
	}

	public void testBottomHalfOnGrassSoundsLikeTheGrass() {
		List<String> steps = walk(ON_GRASS, 4.1875D);
		assertIn(step(Blocks.grass), steps);
		assertNotIn(step(ModBlocks.IRON_TRAPDOOR.get()), steps);
	}

	public void testTopHalfSoundsLikeItself() {
		List<String> steps = walk(IN_GROUND, 4.0D);
		assertIn(step(ModBlocks.IRON_TRAPDOOR.get()), steps);
		assertNotIn(step(Blocks.grass), steps);
	}

	public void testBottomHalfOverAirIsSilent() {
		assertEqual(new ArrayList<String>(), walk(OVER_AIR, 4.1875D));
	}

	/** The block's step sound as the sound handler names it (minecraft:step.grass). */
	private static String step(Block block) {
		return new ResourceLocation(block.stepSound.getStepResourcePath()).toString();
	}

	/** Stands on the lane's first block, walks south for 20 ticks (about four blocks) and returns the steps heard. */
	private List<String> walk(int x, double y) {
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(x + 0.5D, y, 0.5D, 0.0F, 0.0F));
		look(0.0F, 0.0F);
		waitUntilOnClient("standing on the lane", () -> mc().thePlayer.onGround && Math.abs(mc().thePlayer.posX - (x + 0.5D)) < 0.01D, 40);
		clearSounds();
		hold(mc().gameSettings.keyBindForward, 20);
		ticks(2);
		assertTrue(onClient(() -> mc().thePlayer.posZ > 3.0D && mc().thePlayer.posZ < LENGTH), "walked along the lane");
		List<String> steps = new ArrayList<>();
		for (String sound : soundsHeard()) {
			if (sound.contains("step.")) {
				steps.add(sound);
			}
		}
		return steps;
	}
}
