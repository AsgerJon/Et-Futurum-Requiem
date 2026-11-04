package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import ganymedes01.etfuturum.potion.EffectRegistry;
import ganymedes01.etfuturum.potion.ModPotions;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/**
 * Named effects, stage A (SPEC-modern-movement.md §70.3, §70.5): every effect by its name, both ways, as 26.3's
 * registry; numbers only inside a running game, chosen at startup, the table growing as effects register; EFR's
 * config ids gone; an effect built on a taken number moved to a free one.
 */
public class EffectsTests extends TasTestCase {

	/** 1.7.10's effects 1 to 23, by 26.3's names. */
	static final String[] VANILLA = {null, "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health",
		"instant_damage", "jump_boost", "nausea", "regeneration", "resistance", "fire_resistance", "water_breathing",
		"invisibility", "blindness", "night_vision", "hunger", "weakness", "poison", "wither", "health_boost", "absorption",
		"saturation"};

	/** Test effects, made once a run (an effect can't be taken back out of 1.7.10). */
	private static Potion far;
	private static Potion farther;

	/** Every 1.7.10 effect by 26.3's name, and back. */
	public void testVanillaByName() {
		for (int id = 1; id < VANILLA.length; id++) {
			Potion potion = Potion.potionTypes[id];
			assertEqual(potion, EffectRegistry.get("minecraft:" + VANILLA[id]), VANILLA[id]);
			assertEqual("minecraft:" + VANILLA[id], EffectRegistry.key(potion), "effect " + id + "'s name");
		}
	}

	/** EFR's effects by their names. */
	public void testEfrByName() {
		if (ModPotions.levitation != null) {
			assertEqual(ModPotions.levitation, EffectRegistry.get("minecraft:levitation"), "levitation");
			assertEqual("minecraft:levitation", EffectRegistry.key(ModPotions.levitation), "levitation's name");
		}
		if (ModPotions.slowFalling != null) {
			assertEqual(ModPotions.slowFalling, EffectRegistry.get("minecraft:slow_falling"), "slow falling");
			assertEqual("minecraft:slow_falling", EffectRegistry.key(ModPotions.slowFalling), "slow falling's name");
		}
	}

	/** No effect numbers in EFR's config any more: they are chosen at startup. */
	public void testNoConfigIds() {
		for (String field : new String[]{"levitationID", "slowFallingID"}) {
			boolean there;
			try {
				ConfigEnchantsPotions.class.getField(field);
				there = true;
			} catch (NoSuchFieldException e) {
				there = false;
			}
			assertTrue(!there, "ConfigEnchantsPotions still has " + field);
		}
	}

	/** Effects past the old 32 slots and past a byte's 127 register, by name, and work on a mob. */
	public void testRoomPastTheOldTable() {
		onServer(() -> {
			if (far == null) {
				far = new TestEffect(40, "far");
				farther = new TestEffect(200, "farther");
			}
		});
		for (Potion potion : new Potion[]{far, farther}) {
			assertTrue(Potion.potionTypes.length > potion.id && Potion.potionTypes[potion.id] == potion, "effect " + potion.id + " in the table");
			String name = "unittas:" + (potion == far ? "far" : "farther");
			assertEqual(potion, EffectRegistry.get(name), name);
			assertEqual(name, EffectRegistry.key(potion), name + "'s name");
		}
		EntityZombie zombie = onServer(() -> {
			EntityZombie mob = new EntityZombie(world());
			mob.tasks.taskEntries.clear();
			mob.setLocationAndAngles(player().posX + 3.0D, player().posY, player().posZ, 0.0F, 0.0F);
			world().spawnEntityInWorld(mob);
			mob.addPotionEffect(new PotionEffect(farther.id, 100, 0));
			return mob;
		});
		serverTicks(10);
		int left = onServer(() -> {
			PotionEffect effect = zombie.getActivePotionEffect(farther);
			zombie.setDead();
			return effect == null ? -1 : effect.getDuration();
		});
		assertTrue(left > 0 && left < 100, "the far effect on the zombie, ticking: " + left);
	}

	/** An effect built on a taken number gets a free one; the one there keeps its number. */
	public void testTakenNumberMoves() {
		Potion speed = Potion.potionTypes[1];
		Potion[] made = new Potion[1];
		try {
			onServer(() -> {
				made[0] = new TestEffect(1, "on_speeds_number");
			});
		} finally {
			// on the seam the new one is put over speed: put speed back for the rest of the run
			if (Potion.potionTypes[1] != speed) {
				Potion.potionTypes[1] = speed;
			}
		}
		assertTrue(made[0].id != 1, "the new effect kept the taken number 1");
		assertEqual(speed, Potion.potionTypes[1], "speed's place");
		assertEqual(made[0], Potion.potionTypes[made[0].id], "the new effect at its own number");
	}

	/** A test effect: neither good nor bad, white. */
	private static final class TestEffect extends Potion {
		TestEffect(int id, String name) {
			super(id, false, 0xFFFFFF);
			setPotionName("potion.unittas." + name);
		}
	}
}
