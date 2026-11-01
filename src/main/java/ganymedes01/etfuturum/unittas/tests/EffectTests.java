package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.unittas.TasCommand;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/** Modern /effect (spec §41.4). */
public class EffectTests extends TasTestCase {

	/** A copy of the player's effect, or null. */
	private PotionEffect effect(Potion potion) {
		return onServer(() -> {
			PotionEffect effect = player().getActivePotionEffect(potion);
			return effect == null ? null : new PotionEffect(effect);
		});
	}

	public void testGiveSpeedForThirtySeconds() {
		assertSaid(command("/effect give @s speed"), "Applied effect Speed to " + playerName());
		PotionEffect speed = effect(Potion.moveSpeed);
		assertIsNotNone(speed, "no Speed");
		assertEqual(0, speed.getAmplifier());
		assertTrue(speed.getDuration() > 550 && speed.getDuration() <= 600, "duration " + speed.getDuration());
	}

	public void testGiveSecondsAndAmplifier() {
		command("/effect give @s speed 10 2");
		PotionEffect speed = effect(Potion.moveSpeed);
		assertIsNotNone(speed, "no Speed");
		assertEqual(2, speed.getAmplifier());
		assertTrue(speed.getDuration() > 150 && speed.getDuration() <= 200, "duration " + speed.getDuration());
	}

	public void testInfinite() {
		command("/effect give @s night_vision infinite");
		PotionEffect vision = effect(Potion.nightVision);
		assertIsNotNone(vision, "no Night Vision");
		assertGreater(vision.getDuration(), 1000000000);
	}

	public void testInstantHealthHeals() {
		onServer(() -> player().setHealth(5.0F));
		command("/effect give @s instant_health");
		ticks(2);
		assertGreater(onServer(() -> (double) player().getHealth()), 5.0);
	}

	public void testClearOne() {
		command("/effect give @s speed");
		command("/effect give @s haste");
		assertSaid(command("/effect clear @s speed"), "Removed effect Speed from " + playerName());
		assertIsNone(effect(Potion.moveSpeed));
		assertIsNotNone(effect(Potion.digSpeed), "Haste went too");
	}

	public void testClearAllThenNothingLeft() {
		command("/effect give @s speed");
		command("/effect give @s haste");
		assertSaid(command("/effect clear @s"), "Removed every effect from " + playerName());
		assertIsNone(effect(Potion.moveSpeed));
		TasCommand again = command("/effect clear @s");
		assertError(again, "Target has no effects to remove");
	}

	public void testLegacyForm() {
		command("/effect " + playerName() + " 1 30");
		PotionEffect speed = effect(Potion.moveSpeed);
		assertIsNotNone(speed, "1.7.10's form gave no Speed");
		assertTrue(speed.getDuration() > 550 && speed.getDuration() <= 600, "duration " + speed.getDuration());
	}
}
