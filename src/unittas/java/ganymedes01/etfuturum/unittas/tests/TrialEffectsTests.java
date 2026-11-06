package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import ganymedes01.etfuturum.potion.BrewingTable;
import ganymedes01.etfuturum.potion.EffectRegistry;
import ganymedes01.etfuturum.potion.ModernEffects;
import ganymedes01.etfuturum.potion.NamedPotion;
import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.potion.PotionContents;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Potions by name, stage D (SPEC-modern-movement.md §69.11): the effects the last of 26.3's potions need: luck and bad
 * luck, wind charged, weaving, oozing and infested, as 26.3 makes them, with 26.3's icons; their potions offered, and
 * the three EFR has reagents for brewed.
 */
@TasSuite("effects")
public class TrialEffectsTests extends TasTestCase {

	private static final String[] NAMES = {"luck", "unluck", "wind_charged", "weaving", "oozing", "infested"};

	@Override
	protected void tearDown() {
		onServer(() -> world().getGameRules().setOrCreateGameRule("mobGriefing", "false"));
	}

	/** All six by 26.3's names, each with 26.3's icon. */
	public void testByNameWithIcons() {
		for (String name : NAMES) {
			Potion potion = EffectRegistry.get("minecraft:" + name);
			assertTrue(potion != null, "no effect " + name);
			assertTrue(onClient(() -> {
				try {
					mc().getResourceManager().getResource(new ResourceLocation("textures/mob_effect/" + name + ".png"));
					return true;
				} catch (java.io.IOException e) {
					return false;
				}
			}), "no icon for " + name);
		}
	}

	/** Luck adds its levels to fishing's luck (Luck of the Sea's), bad luck takes them away. */
	public void testLuckInFishing() {
		Potion luck = effect("luck");
		Potion unluck = effect("unluck");
		int[] seen = onServer(() -> {
			player().clearActivePotions();
			int none = ModernEffects.fishingLuck(player());
			player().addPotionEffect(new PotionEffect(luck.id, 200, 1));
			int lucky = ModernEffects.fishingLuck(player());
			player().clearActivePotions();
			player().addPotionEffect(new PotionEffect(unluck.id, 200, 0));
			int unlucky = ModernEffects.fishingLuck(player());
			player().clearActivePotions();
			return new int[]{none, lucky, unlucky};
		});
		assertEqual(0, seen[0], "no effect");
		assertEqual(2, seen[1], "luck II");
		assertEqual(-1, seen[2], "bad luck I");
	}

	/** Wind charged: killed, a wind burst knocks a mob near away without hurting it, and opens a wooden door. */
	public void testWindBurst() {
		Potion wind = effect("wind_charged");
		// the burst knocks the player back too, so the door's spot is fixed first
		int[] door = onServer(() -> new int[]{(int) Math.floor(player().posX) + 2, (int) Math.floor(player().posY), (int) Math.floor(player().posZ) + 3});
		EntityVillager villager = onServer(() -> {
			EntityVillager mob = new EntityVillager(world());
			mob.tasks.taskEntries.clear();
			mob.setLocationAndAngles(player().posX + 4.0D, player().posY, player().posZ + 2.0D, 0.0F, 0.0F);
			world().spawnEntityInWorld(mob);
			ItemDoor.placeDoorBlock(world(), door[0], door[1], door[2], 0, Blocks.wooden_door);
			return mob;
		});
		double[] before = onServer(() -> new double[]{villager.posX, villager.posZ, villager.getHealth()});
		EntityZombie zombie = killedWith(wind, 2.0D, 2.0D);
		serverTicks(30);
		double[] after = onServer(() -> new double[]{villager.posX, villager.posZ, villager.getHealth(), world().getBlockMetadata(door[0], door[1], door[2])});
		onServer(() -> {
			villager.setDead();
			zombie.setDead();
			world().setBlockToAir(door[0], door[1] + 1, door[2]);
			world().setBlockToAir(door[0], door[1], door[2]);
		});
		double moved = Math.hypot(after[0] - before[0], after[1] - before[1]);
		assertTrue(moved > 0.5D, "the villager moved " + moved + " from the burst");
		assertEqual(before[2], after[2], "the villager's health");
		assertTrue(((int) after[3] & 4) != 0, "the wooden door wasn't opened (its meta " + (int) after[3] + ")");
	}

	/**
	 * Weaving: a killed mob leaves 2 or 3 cobwebs around where it died, with mob griefing on; none with it off. 26.3 tries
	 * 15 random spots of the 27 around it, and on a flat floor only the 9 at its feet hold one, so about 1 kill in 35
	 * leaves fewer (26.3's own chance, seen in the full runs of 2026-11-04 and 2026-11-06). So up to three kills: never
	 * more than 3, and one of them 2 or 3.
	 */
	public void testWeavingCobwebs() {
		Potion weaving = effect("weaving");
		onServer(() -> world().getGameRules().setOrCreateGameRule("mobGriefing", "true"));
		List<Integer> counts = new ArrayList<>();
		int y = 0;
		for (int kill = 0; kill < 3 && (counts.isEmpty() || counts.get(counts.size() - 1) < 2); kill++) {
			EntityZombie zombie = killedWith(weaving, 6.0D, 0.0D);
			int x = onServer(() -> (int) Math.floor(zombie.posX));
			y = onServer(() -> (int) Math.floor(zombie.posY));
			int z = onServer(() -> (int) Math.floor(zombie.posZ));
			serverTicks(30);
			counts.add(cobwebsAround(x, y, z, true));
		}
		assertTrue(counts.stream().allMatch(webs -> webs <= 3) && counts.get(counts.size() - 1) >= 2, "cobwebs by kill " + counts + " (26.3: 2 or 3)");
		onServer(() -> world().getGameRules().setOrCreateGameRule("mobGriefing", "false"));
		EntityZombie second = killedWith(weaving, 6.0D, 4.0D);
		int x2 = onServer(() -> (int) Math.floor(second.posX));
		int z2 = onServer(() -> (int) Math.floor(second.posZ));
		serverTicks(30);
		assertEqual(0, cobwebsAround(x2, y, z2, true), "cobwebs with mob griefing off");
	}

	/** Weaving: a cobweb holds it half as hard. */
	public void testWeavingInACobweb() {
		Potion weaving = effect("weaving");
		double[] factors = onServer(() -> {
			EntityZombie mob = new EntityZombie(world());
			double[] plain = ModernEffects.webFactors(mob);
			mob.addPotionEffect(new PotionEffect(weaving.id, 200, 0));
			double[] woven = ModernEffects.webFactors(mob);
			return new double[]{plain[0], plain[1], woven[0], woven[1], woven[2]};
		});
		assertAlmostEqual(0.25D, factors[0], 1.0E-6D);
		assertAlmostEqual(0.05D, factors[1], 1.0E-6D);
		assertAlmostEqual(0.5D, factors[2], 1.0E-6D);
		assertAlmostEqual(0.25D, factors[3], 1.0E-6D);
		assertAlmostEqual(0.5D, factors[4], 1.0E-6D);
	}

	/** Oozing: a killed mob leaves two medium slimes. */
	public void testOozingSlimes() {
		Potion oozing = effect("oozing");
		EntityZombie zombie = killedWith(oozing, -4.0D, 0.0D);
		serverTicks(30);
		String slimes = onServer(() -> {
			StringBuilder sizes = new StringBuilder();
			for (Object entity : world().getEntitiesWithinAABB(EntitySlime.class, zombie.boundingBox.expand(4.0D, 4.0D, 4.0D))) {
				EntitySlime slime = (EntitySlime) entity;
				sizes.append(slime.getSlimeSize()).append(' ');
				slime.setDead();
			}
			return sizes.toString().trim();
		});
		assertEqual("2 2", slimes, "the slimes left (their sizes)");
	}

	/** Infested: hurt 60 times, it lets out silverfish (1 in 10 each time). */
	public void testInfestedSilverfish() {
		Potion infested = effect("infested");
		int silverfish = onServer(() -> {
			EntityZombie mob = new EntityZombie(world());
			mob.tasks.taskEntries.clear();
			mob.setLocationAndAngles(player().posX - 4.0D, player().posY, player().posZ + 4.0D, 0.0F, 0.0F);
			world().spawnEntityInWorld(mob);
			mob.addPotionEffect(new PotionEffect(infested.id, 2000, 0));
			for (int i = 0; i < 60; i++) {
				mob.hurtResistantTime = 0;
				mob.setHealth(mob.getMaxHealth());
				mob.attackEntityFrom(DamageSource.generic, 1.0F);
			}
			int count = 0;
			for (Object entity : world().getEntitiesWithinAABB(EntitySilverfish.class, mob.boundingBox.expand(6.0D, 6.0D, 6.0D))) {
				((EntitySilverfish) entity).setDead();
				count++;
			}
			mob.setDead();
			return count;
		});
		assertTrue(silverfish > 0, "no silverfish from 60 hurts");
	}

	/** Their potions are offered, and oozing, infested and weaving brew from an awkward potion as 26.3's. */
	public void testPotionsBrew() {
		for (String name : new String[]{"luck", "wind_charged", "weaving", "oozing", "infested"}) {
			NamedPotion potion = NamedPotions.byId("minecraft:" + name);
			assertTrue(potion != null && potion.isAvailable(), name + "'s potion isn't offered");
		}
		String[][] mixes = {{"slime_block", "oozing"}, {"stone", "infested"}, {"cobweb", "weaving"}};
		for (String[] mix : mixes) {
			ItemIds.Entry reagent = ItemIds.byId("minecraft:" + mix[0]);
			if (reagent == null) {
				continue;
			}
			ItemStack made = BrewingTable.brew(BrewingTable.create(BrewingTable.Container.POTION, NamedPotions.byId("minecraft:awkward")), reagent.createStack(1));
			NamedPotion brewed = made == null ? null : PotionContents.of(made).potion();
			assertEqual("minecraft:" + mix[1], brewed == null ? null : brewed.id(), "awkward + " + mix[0]);
		}
	}

	// ---------------------------------------------------------------- helpers

	private static Potion effect(String name) {
		Potion potion = EffectRegistry.get("minecraft:" + name);
		assertTrue(potion != null, "no effect " + name);
		return potion;
	}

	/** A zombie with the effect, killed at this offset from the player; its death animation takes 20 ticks. */
	private EntityZombie killedWith(Potion potion, double dx, double dz) {
		return onServer(() -> {
			EntityZombie mob = new EntityZombie(world());
			mob.tasks.taskEntries.clear();
			mob.setLocationAndAngles(player().posX + dx, player().posY, player().posZ + dz, 0.0F, 0.0F);
			world().spawnEntityInWorld(mob);
			mob.addPotionEffect(new PotionEffect(potion.id, 400, 0));
			mob.attackEntityFrom(DamageSource.outOfWorld, 1000.0F);
			return mob;
		});
	}

	/** The cobwebs within a block of this spot, cleared if asked. */
	private int cobwebsAround(int x, int y, int z, boolean clear) {
		return onServer(() -> {
			int count = 0;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (world().getBlock(x + dx, y + dy, z + dz) == Blocks.web) {
							count++;
							if (clear) {
								world().setBlockToAir(x + dx, y + dy, z + dz);
							}
						}
					}
				}
			}
			return count;
		});
	}
}
