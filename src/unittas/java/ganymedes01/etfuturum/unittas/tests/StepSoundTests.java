package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Footsteps on thin blocks (SPEC-modern-movement.md §53.7, §56). 26.3, like 1.7.10, plays the sound of the block 0.2
 * below the feet, so a bottom-half trapdoor (3/16 thick) or a carpet sounds like the block under it, and over air like
 * nothing: MC-8820, open since 2013. EFR fixes it (thinBlockStepSoundFix): the block stood on gives the sound. A
 * top-half trapdoor sounds like itself either way. Carpets and lily pads follow 26.3's own rules for players
 * (modernStepSounds). The lanes run south, eight blocks long, each test walking one from its start. The iron trapdoor
 * sounds as 1.7.10's metal (step.stone), the grass and the lily pad as step.grass, carpet as step.cloth.
 */
@TasSuite("quick")
public class StepSoundTests extends TasTestCase {

	/** Bottom-half trapdoors on the grass, top-half ones in place of the grass, bottom-half ones over a pit, carpet. */
	private static final int ON_GRASS = 10, IN_GROUND = 12, OVER_AIR = 14, CARPET = 16, LILY_PADS = 18, LENGTH = 8;

	@Override
	protected void setUp() {
		if (!io.github.asgerjon.mcfly.registry.McFlyBlocks.IRON_TRAPDOOR.isEnabled()) {
			skipTest("no iron trapdoor");
		}
		onServer(() -> {
			for (int z = 0; z < LENGTH; z++) {
				world().setBlock(ON_GRASS, 4, z, io.github.asgerjon.mcfly.registry.McFlyBlocks.IRON_TRAPDOOR.get(), 0, 2);
				world().setBlock(IN_GROUND, 3, z, io.github.asgerjon.mcfly.registry.McFlyBlocks.IRON_TRAPDOOR.get(), 8, 2);
				world().setBlock(OVER_AIR, 3, z, Blocks.air, 0, 2);
				world().setBlock(OVER_AIR, 4, z, io.github.asgerjon.mcfly.registry.McFlyBlocks.IRON_TRAPDOOR.get(), 0, 2);
				world().setBlock(CARPET, 4, z, Blocks.carpet, 0, 2);
				world().setBlock(LILY_PADS, 3, z, Blocks.water, 0, 2);
				world().setBlock(LILY_PADS, 4, z, Blocks.waterlily, 0, 2);
			}
		});
	}

	/** MC-8820: 26.3 plays the grass under it. */
	public void testBottomHalfOnGrassSoundsLikeItself() {
		List<String> steps = walk(ON_GRASS, 4.1875D);
		assertIn(step(io.github.asgerjon.mcfly.registry.McFlyBlocks.IRON_TRAPDOOR.get()), steps);
		assertNotIn(step(Blocks.grass), steps);
	}

	public void testTopHalfSoundsLikeItself() {
		List<String> steps = walk(IN_GROUND, 4.0D);
		assertIn(step(io.github.asgerjon.mcfly.registry.McFlyBlocks.IRON_TRAPDOOR.get()), steps);
		assertNotIn(step(Blocks.grass), steps);
	}

	/** MC-8820: 26.3 plays nothing at all over air. */
	public void testBottomHalfOverAirSoundsLikeItself() {
		assertIn(step(io.github.asgerjon.mcfly.registry.McFlyBlocks.IRON_TRAPDOOR.get()), walk(OVER_AIR, 4.1875D));
	}

	/** 26.3's combination step sound (modernStepSounds): the carpet, and the grass under it muffled; 1.7.10 only the grass. */
	public void testCarpetSoundsWithTheBlockUnderIt() {
		List<String> steps = walk(CARPET, 4.0625D);
		assertIn(step(Blocks.carpet), steps);
		assertIn(step(Blocks.grass), steps);
	}

	/** 26.3's inside step sound (modernStepSounds): the lily pad itself; 1.7.10 heard the water under it, which is silent. */
	public void testLilyPadSoundsLikeItself() {
		assertIn(step(Blocks.waterlily), walk(LILY_PADS, 4.015625D));
	}

	/** With thinBlockStepSoundFix off, 26.3's (and 1.7.10's) silence over air. */
	public void testWithTheFixOffOverAirIsSilent() {
		boolean fix = onClient(() -> McFlyConfig.thinBlockStepSoundFix);
		onClient(() -> {
			McFlyConfig.thinBlockStepSoundFix = false;
		});
		try {
			assertEqual(new ArrayList<String>(), walk(OVER_AIR, 4.1875D));
		} finally {
			onClient(() -> {
				McFlyConfig.thinBlockStepSoundFix = fix;
			});
		}
	}

	/** The block's step sound as the sound handler names it (minecraft:step.grass). */
	private static String step(Block block) {
		return new ResourceLocation(block.stepSound.getStepResourcePath()).toString();
	}

	/**
	 * Stands on the lane's first block, walks south for 20 ticks (about four blocks) and returns the steps heard:
	 * 1.7.10's names (step.grass) and EFR's modern ones (block.wet_grass.step).
	 */
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
			if (sound.contains("step.") || sound.endsWith(".step")) {
				steps.add(sound);
			}
		}
		return steps;
	}
}
