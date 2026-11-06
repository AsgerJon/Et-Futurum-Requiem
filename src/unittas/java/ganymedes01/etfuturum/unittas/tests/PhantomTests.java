package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.entities.EntityPhantom;
import ganymedes01.etfuturum.potion.ModPotions;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldSettings;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Phantoms, their membranes and the Slow Falling effect (SPEC-modern-movement.md §68.1, §68.2, §68.4; 26.3's
 * `Phantom`, its loot table, `LivingEntity.getEffectiveGravity`). Phantoms are spawned 12 above the spawn; the player
 * is in creative (no phantom targets one) unless a test says survival.
 */
@TasSuite("phantom")
public class PhantomTests extends TasTestCase {

	private static final int GROUND = 4;
	private static final String MODERN = McFly.MC_ASSETS + ":";

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	@Override
	protected void setUp() {
		if (!McFlyConfig.enablePhantoms) {
			skipTest("enablePhantoms is off");
		}
		assertTrue(EntityList.classToStringMapping.containsKey(EntityPhantom.class), "the phantom isn't registered");
		onServer(() -> world().setWorldTime(18000L));
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			for (EntityPhantom phantom : entitiesNear(EntityPhantom.class, 200.0D)) {
				phantom.setDead();
			}
			for (EntityOcelot cat : entitiesNear(EntityOcelot.class, 60.0D)) {
				cat.setDead();
			}
			for (EntityItem item : entitiesNear(EntityItem.class, 60.0D)) {
				item.setDead();
			}
			for (int x = -2; x <= 2; x++) {
				for (int z = -2; z <= 2; z++) {
					world().setBlockToAir(x, 20, z);
				}
			}
			world().setWorldTime(6000L);
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().setHealth(player().getMaxHealth());
		});
	}

	// ---------------------------------------------------------------- the phantom

	/** `/summon phantom` makes one. */
	public void testSummonedById() {
		assertNoError(command("summon phantom 0 " + (GROUND + 12) + " 0"));
		assertTrue(onServer(() -> !entitiesNear(EntityPhantom.class, 80.0D).isEmpty()), "no phantom after /summon");
	}

	/** It flies: 2 s later it is still up in the air, unhurt, with no fall counted. */
	public void testFlies() {
		EntityPhantom phantom = phantom(0.5D, GROUND + 12, 0.5D);
		serverTicks(40);
		double[] seen = onServer(() -> new double[]{phantom.posY, phantom.fallDistance, phantom.getHealth(), phantom.isDead ? 1 : 0});
		assertTrue(seen[0] > GROUND + 4, "it came down to " + seen[0]);
		assertEqual(0.0D, seen[1], "its fall distance");
		assertEqual(20.0D, seen[2], "its health");
	}

	/** Its size (0 to 64): the box 0.9 x 0.5 times 1 + 0.15 size, the attack 6 + size; saved and read with its anchor. */
	public void testSizeSavedAndRead() {
		EntityPhantom phantom = phantom(0.5D, GROUND + 12, 0.5D);
		double[] small = onServer(() -> new double[]{phantom.width, phantom.height, phantom.getEntityAttribute(SharedMonsterAttributes.attackDamage).getBaseValue()});
		assertAlmostEqual(0.9D, small[0], 1.0E-4D);
		assertAlmostEqual(0.5D, small[1], 1.0E-4D);
		assertAlmostEqual(6.0D, small[2], 1.0E-6D);
		int[] read = onServer(() -> {
			phantom.setPhantomSize(10);
			NBTTagCompound tag = new NBTTagCompound();
			phantom.writeToNBT(tag);
			EntityPhantom copy = new EntityPhantom(world());
			copy.readFromNBT(tag);
			int[] anchor = copy.getAnchor();
			return new int[]{copy.getPhantomSize(), anchor == null ? -999 : anchor[1], tag.getInteger("size"), tag.hasKey("anchor_pos") ? 1 : 0};
		});
		assertEqual(10, read[0], "the size read back");
		assertEqual(10, read[2], "the saved size");
		assertEqual(1, read[3], "anchor_pos saved");
		double[] big = onServer(() -> new double[]{phantom.width, phantom.height, phantom.getEntityAttribute(SharedMonsterAttributes.attackDamage).getBaseValue()});
		assertAlmostEqual(0.9D * 2.5D, big[0], 1.0E-4D);
		assertAlmostEqual(0.5D * 2.5D, big[1], 1.0E-4D);
		assertAlmostEqual(16.0D, big[2], 1.0E-6D);
	}

	/** With no target it circles its anchor: 5 to 15 out (and a little more as it swings), -8 to +5 up and down. */
	public void testCirclesItsAnchor() {
		EntityPhantom phantom = phantom(0.5D, GROUND + 15, 0.5D);
		int[] anchor = onServer(phantom::getAnchor);
		assertIsNotNone(anchor, "no anchor");
		double farthest = 0.0D, highest = -99.0D, lowest = 99.0D;
		for (int i = 0; i < 20; i++) {
			serverTicks(10);
			double[] at = onServer(() -> new double[]{phantom.posX, phantom.posY, phantom.posZ});
			double dx = at[0] - anchor[0], dz = at[2] - anchor[2];
			farthest = Math.max(farthest, Math.sqrt(dx * dx + dz * dz));
			highest = Math.max(highest, at[1] - anchor[1]);
			lowest = Math.min(lowest, at[1] - anchor[1]);
		}
		String seen = "farthest " + farthest + ", from " + lowest + " to " + highest + " up";
		assertTrue(farthest > 2.0D && farthest < 22.0D, seen + ": not circling 5 to 15 out");
		assertTrue(lowest > -12.0D && highest < 8.0D, seen + ": not at the anchor's height");
	}

	/** A survival player under it is swooped at (`entity.phantom.swoop`) and bitten (`entity.phantom.bite`, hurt by it). */
	public void testSwoopsAndBites() {
		survival();
		EntityPhantom phantom = phantom(0.5D, GROUND + 12, 0.5D);
		clearSounds();
		waitUntil("the player bitten", () -> player().getHealth() < player().getMaxHealth(), 600);
		assertTrue(onServer(() -> player().getAITarget() == phantom), "the player hurt by something else");
		waitForSound(MODERN + "entity.phantom.swoop", 10);
		waitForSound(MODERN + "entity.phantom.bite", 10);
	}

	/** A cat (1.7.10's tamed ocelot) within 16 blocks keeps it off: no bite in 15 s. */
	public void testCatsKeepItOff() {
		survival();
		onServer(() -> {
			EntityOcelot cat = new EntityOcelot(world());
			cat.setTamed(true);
			cat.setTameSkin(1);
			cat.tasks.taskEntries.clear();
			cat.setLocationAndAngles(2.5D, GROUND, 2.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(cat);
		});
		phantom(0.5D, GROUND + 12, 0.5D);
		serverTicks(300);
		assertEqual(onServer(() -> player().getMaxHealth()), onServer(() -> player().getHealth()), "the player's health with a cat near");
	}

	/** In daylight under the sky it catches fire; under a roof it doesn't. */
	public void testBurnsInDaylight() {
		onServer(() -> world().setWorldTime(6000L));
		EntityPhantom open = phantom(0.5D, GROUND + 12, 0.5D);
		onServer(() -> open.setStill(true));
		waitUntil("the phantom burning in daylight", () -> open.isBurning(), 300);
		onServer(open::setDead);
		onServer(() -> {
			for (int x = -2; x <= 2; x++) {
				for (int z = -2; z <= 2; z++) {
					world().setBlock(x, 20, z, Blocks.stone);
				}
			}
		});
		EntityPhantom roofed = phantom(0.5D, GROUND + 12, 0.5D);
		onServer(() -> roofed.setStill(true));
		serverTicks(200);
		assertFalse(onServer(() -> roofed.isBurning()), "the phantom burned under a roof");
	}

	/** Its ambient, hurt and death sounds; its flap on the client as a wing comes down. */
	public void testSounds() {
		EntityPhantom phantom = phantom(0.5D, GROUND + 8, 0.5D);
		onServer(() -> phantom.setStill(true));
		clearSounds();
		waitForSound(MODERN + "entity.phantom.flap", 60);
		onServer(phantom::playLivingSound);
		waitForSound(MODERN + "entity.phantom.ambient", 10);
		onServer(() -> phantom.attackEntityFrom(DamageSource.generic, 1.0F));
		waitForSound(MODERN + "entity.phantom.hurt", 10);
		// past the hurt cooldown: 1.7.10 plays no sound for a hit within it
		onServer(() -> {
			phantom.hurtResistantTime = 0;
			phantom.attackEntityFrom(DamageSource.outOfWorld, 100.0F);
		});
		waitForSound(MODERN + "entity.phantom.death", 10);
	}

	/** Mycelium specks at its wing tips on the client (1.7.10's mycelium particle, `townaura`). */
	public void testWingParticles() throws Exception {
		EntityPhantom phantom = phantom(0.5D, GROUND + 8, 0.5D);
		onServer(() -> phantom.setStill(true));
		ticks(10);
		assertGreater(onClient(() -> particles("EntityAuraFX")), 3);
	}

	/** Killed by a player: 0 to 1 membranes, Looting adding up to its level; killed otherwise, none. */
	public void testLoot() {
		if (!McFlyItems.PHANTOM_MEMBRANE.isEnabled()) {
			skipTest("the phantom membrane is off");
		}
		int[] plain = kills(40, 0, true);
		int[] looting = kills(40, 3, true);
		int[] notByAPlayer = kills(10, 0, false);
		String seen = "plain " + plain[0] + ".." + plain[1] + ", Looting III " + looting[0] + ".." + looting[1] + ", not by a player up to " + notByAPlayer[1];
		assertTrue(plain[0] == 0 && plain[1] == 1, seen + " (wanted 0..1)");
		assertTrue(looting[1] > 1 && looting[1] <= 4, seen + " (wanted up to 4 with Looting III)");
		assertEqual(0, notByAPlayer[1], seen);
	}

	// ---------------------------------------------------------------- the membrane

	/** An anvil mends elytra with a phantom membrane (26.3's), not with leather (1.9's, EFR's before). */
	public void testMembraneMendsElytra() {
		if (!McFlyItems.PHANTOM_MEMBRANE.isEnabled() || !io.github.asgerjon.mcfly.registry.McFlyItems.ELYTRA.isEnabled()) {
			skipTest("the membrane or the elytra is off");
		}
		survival();
		onServer(() -> player().addExperienceLevel(30));
		ItemStack[] out = onServer(() -> {
			ItemStack elytra = io.github.asgerjon.mcfly.registry.McFlyItems.ELYTRA.newItemStack();
			elytra.setItemDamage(300);
			ItemStack[] results = new ItemStack[2];
			int i = 0;
			for (ItemStack repair : new ItemStack[]{McFlyItems.PHANTOM_MEMBRANE.newItemStack(), new ItemStack(Items.leather)}) {
				ContainerRepair anvil = new ContainerRepair(player().inventory, world(), 0, GROUND, 0, player());
				anvil.getSlot(0).putStack(elytra.copy());
				anvil.getSlot(1).putStack(repair);
				anvil.updateRepairOutput();
				results[i++] = anvil.getSlot(2).getStack();
			}
			return results;
		});
		assertIsNotNone(out[0], "no mended elytra from a membrane");
		assertTrue(out[0].getItemDamage() < 300, "the membrane didn't mend: damage " + out[0].getItemDamage());
		assertIsNone(out[1]);
	}

	// ---------------------------------------------------------------- Slow Falling

	/** With Slow Falling a falling mob's gravity is 0.01 (26.3's `getEffectiveGravity`): `v' = (v - 0.01) x 0.98`; the fall never builds up, no fall damage. */
	public void testSlowFalling() {
		if (ModPotions.slowFalling == null) {
			skipTest("Slow Falling is off");
		}
		EntityZombie zombie = onServer(() -> {
			EntityZombie made = new EntityZombie(world());
			made.tasks.taskEntries.clear();
			made.targetTasks.taskEntries.clear();
			made.setLocationAndAngles(5.5D, GROUND + 30, 5.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(made);
			made.addPotionEffect(new PotionEffect(ModPotions.slowFalling.id, 2400, 0));
			made.addPotionEffect(new PotionEffect(net.minecraft.potion.Potion.fireResistance.id, 2400, 0));
			return made;
		});
		serverTicks(5);
		double[] v = new double[4];
		long[] t = new long[4];
		for (int i = 0; i < v.length; i++) {
			serverTicks(1);
			double[] sample = onServer(() -> new double[]{zombie.motionY, world().getTotalWorldTime(), zombie.fallDistance});
			v[i] = sample[0];
			t[i] = (long) sample[1];
			// 26.3 also resets it before each tick's move (`resetFallDistance` in `aiStep`): a tick's fall, never more
			assertTrue(sample[2] < 1.0D, "the fall distance built up to " + sample[2]);
		}
		for (int i = 0; i + 1 < v.length; i++) {
			if (t[i + 1] - t[i] == 1L) {
				assertTrue(Math.abs((v[i] - 0.01D) * 0.98D - v[i + 1]) <= 1.0E-6D, "tick " + i + ": want " + (v[i] - 0.01D) * 0.98D + ", the samples "
					+ java.util.Arrays.toString(v) + " at " + java.util.Arrays.toString(t));
			}
		}
		waitUntil("on the ground", () -> zombie.onGround, 400);
		assertEqual(onServer(zombie::getMaxHealth), onServer(zombie::getHealth), "the zombie's health after a slow fall");
	}

	// ---------------------------------------------------------------- helpers

	private void survival() {
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().setHealth(player().getMaxHealth());
		});
	}

	private EntityPhantom phantom(double x, double y, double z) {
		return onServer(() -> {
			EntityPhantom phantom = new EntityPhantom(world());
			phantom.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
			phantom.onSpawnWithEgg(null);
			world().spawnEntityInWorld(phantom);
			return phantom;
		});
	}

	/** Kills this many phantoms (by the player holding a sword with this Looting, or not by a player): fewest and most membranes. */
	private int[] kills(int times, int looting, boolean byThePlayer) {
		ItemStack sword = new ItemStack(Items.diamond_sword);
		if (looting > 0) {
			sword.addEnchantment(Enchantment.looting, looting);
		}
		setHeld(sword);
		int fewest = Integer.MAX_VALUE, most = 0;
		for (int i = 0; i < times; i++) {
			EntityPhantom phantom = phantom(0.5D, GROUND + 3, 3.5D);
			int dropped = onServer(() -> {
				phantom.setStill(true);
				phantom.attackEntityFrom(byThePlayer ? DamageSource.causePlayerDamage(player()) : DamageSource.outOfWorld, 1000.0F);
				int membranes = 0;
				for (EntityItem item : entitiesNear(EntityItem.class, 80.0D)) {
					if (item.getEntityItem().getItem() == McFlyItems.PHANTOM_MEMBRANE.get()) {
						membranes += item.getEntityItem().stackSize;
					}
					item.setDead();
				}
				return membranes;
			});
			fewest = Math.min(fewest, dropped);
			most = Math.max(most, dropped);
			serverTicks(1);
		}
		return new int[]{fewest, most};
	}

	@SuppressWarnings("rawtypes")
	private static int particles(String name) throws ReflectiveOperationException {
		Field field;
		try {
			field = EffectRenderer.class.getDeclaredField("fxLayers");
		} catch (NoSuchFieldException e) {
			field = EffectRenderer.class.getDeclaredField("field_78876_b");
		}
		field.setAccessible(true);
		int count = 0;
		for (List layer : (List[]) field.get(mc().effectRenderer)) {
			for (Object particle : layer) {
				if (particle.getClass().getSimpleName().contains(name)) {
					count++;
				}
			}
		}
		return count;
	}
}
