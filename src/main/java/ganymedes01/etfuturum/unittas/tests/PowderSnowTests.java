package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.McFly;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.entities.EntityEndermite;
import ganymedes01.etfuturum.entities.EntityFox;
import ganymedes01.etfuturum.entities.EntityPolarBear;
import ganymedes01.etfuturum.entities.EntityRabbit;
import ganymedes01.etfuturum.entities.EntityStray;
import ganymedes01.etfuturum.entities.Freezing;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Powder snow, stage 1 (SPEC-modern-movement.md §62.1 to §62.6, §62.11): the block, moving in it, freezing, walking on
 * top, the bucket, and how it sits in the world. Each test builds on its own spot (x and z from 100 on, 12 apart, out of
 * the other suites' way) and the spots are cleared afterwards: the test world's blocks last through a whole run.
 */
public class PowderSnowTests extends TasTestCase {

	/** The flat world's grass top: blocks stand from y 4 up. */
	private static final int GROUND = 4;

	private final List<int[]> spotsUsed = new ArrayList<>();

	@Override
	protected void tearDown() {
		onServer(() -> {
			world().getGameRules().setOrCreateGameRule("mobGriefing", "false");
			world().getGameRules().setOrCreateGameRule("freezeDamage", "true");
			Freezing.setTicksFrozen(player(), 0);
			for (int slot = 1; slot <= 4; slot++) {
				player().setCurrentItemOrArmor(slot, null);
			}
			player().inventoryContainer.detectAndSendChanges();
			for (int[] spot : spotsUsed) {
				for (int x = spot[0] - 8; x <= spot[0] + 8; x++) {
					for (int z = spot[1] - 8; z <= spot[1] + 8; z++) {
						for (int y = GROUND + 12; y >= GROUND; y--) {
							world().setBlockToAir(x, y, z);
						}
						world().setBlock(x, GROUND - 1, z, Blocks.grass);
					}
				}
			}
			for (EntityItem item : entitiesNear(EntityItem.class, 400.0D)) {
				item.setDead();
			}
		});
		spotsUsed.clear();
	}

	// ---------------------------------------------------------------- the block (§62.1)

	/** The names: the block and the bucket, `/setblock` and `/give`; the block has no item of its own (26.3). */
	public void testNamesAndCommands() {
		int[] at = spot(0);
		snow();
		bucket();
		assertNoError(command("/setblock " + at[0] + " " + GROUND + " " + at[1] + " minecraft:powder_snow"));
		assertEqual(snow(), onServer(() -> world().getBlock(at[0], GROUND, at[1])));
		assertNoError(command("/give @s minecraft:powder_snow_bucket"));
		assertError(command("/give @s minecraft:powder_snow"), "Unknown item 'minecraft:powder_snow'");
	}

	/** Strength 0.25, no drop (silk touch neither), takes 1 off the light, no suffocation, pick block gives the bucket. */
	public void testBlockBasics() {
		int[] at = spot(1);
		Block snow = snow();
		place(at[0], GROUND + 6, at[1], snow);
		Object[] seen = onServer(() -> new Object[]{
				snow.getBlockHardness(world(), at[0], GROUND + 6, at[1]),
				snow.getDrops(world(), at[0], GROUND + 6, at[1], 0, 0).size(),
				snow.canSilkHarvest(world(), player(), at[0], GROUND + 6, at[1], 0),
				snow.getLightOpacity(),
				world().getSavedLightValue(EnumSkyBlock.Sky, at[0], GROUND + 7, at[1]),
				world().getSavedLightValue(EnumSkyBlock.Sky, at[0], GROUND + 5, at[1]),
				snow.isNormalCube()});
		assertAlmostEqual(0.25D, ((Number) seen[0]).doubleValue(), 1.0E-6D);
		assertEqual(0, seen[1], "drops");
		assertEqual(false, seen[2], "silk touch takes it");
		assertEqual(1, seen[3], "light opacity");
		assertEqual(15, seen[4], "sky light above");
		assertEqual(14, seen[5], "sky light below");
		assertEqual(false, seen[6], "a normal cube (suffocates)");
		ItemStack picked = onServer(() -> snow.getPickBlock(new MovingObjectPosition(at[0], GROUND + 6, at[1], 1,
				Vec3.createVectorHelper(at[0] + 0.5D, GROUND + 7, at[1] + 0.5D)), world(), at[0], GROUND + 6, at[1]));
		assertIsNotNone(picked, "pick block gave nothing");
		assertEqual(bucket(), picked.getItem(), "pick block");
	}

	/** Broken in survival by hand: nothing drops. */
	public void testBrokenByHandDropsNothing() {
		int[] at = spot(2);
		place(at[0], GROUND, at[1], snow());
		survival();
		onServer(() -> {
			player().theItemInWorldManager.tryHarvestBlock(at[0], GROUND, at[1]);
		});
		ticks(5);
		assertEqual(Blocks.air, onServer(() -> world().getBlock(at[0], GROUND, at[1])), "not broken");
		assertEqual(0, onServer(() -> entitiesNear(EntityItem.class, 400.0D).size()), "items dropped");
	}

	// ---------------------------------------------------------------- moving in it (§62.2)

	/** A player in it sinks at (0 - 0.08) * 0.98 * 1.5 = 0.1176 a tick and doesn't suffocate. */
	public void testPlayerSinks() {
		int[] at = spot(3);
		column(at[0], at[1], GROUND, 3);
		survival();
		stand(at[0] + 0.5D, GROUND + 3, at[1] + 0.5D, 0.0F, 0.0F);
		waitUntilOnClient("in the snow", () -> feet() < GROUND + 2.9D, 40);
		ticks(2);
		double[] first = onClient(() -> new double[]{feet(), mc().thePlayer.ticksExisted});
		ticks(5);
		double[] second = onClient(() -> new double[]{feet(), mc().thePlayer.ticksExisted});
		assertTrue(second[0] > GROUND, "already at the bottom: " + second[0]);
		// per tick of the player's own (a sample can fall either side of a tick)
		assertAlmostEqual(0.1176D, (first[0] - second[0]) / (second[1] - first[1]), 0.003D);
		waitUntilOnClient("at the bottom", () -> mc().thePlayer.onGround, 40);
		ticks(20);
		assertEqual(player().getMaxHealth(), onServer(() -> player().getHealth()), "hurt at the bottom (suffocation?)");
	}

	/** Leather boots: stands on top; sneaking sinks in. */
	public void testLeatherBootsStandOnTop() {
		int[] at = spot(4);
		column(at[0], at[1], GROUND, 3);
		survival();
		wear(1, new ItemStack(Items.leather_boots));
		stand(at[0] + 0.5D, GROUND + 3, at[1] + 0.5D, 0.0F, 0.0F);
		ticks(20);
		assertAlmostEqual(GROUND + 3, onClient(PowderSnowTests::feet), 0.001D);
		hold(mc().gameSettings.keyBindSneak, 20);
		assertTrue(onClient(PowderSnowTests::feet) < GROUND + 2.5D, "sneaking didn't sink in");
	}

	/** Walkers stand on top: the rabbit, the fox, the endermite, the silverfish; a zombie sinks to the bottom. */
	public void testWalkersStandOnTopZombiesSink() {
		int[] at = spot(5);
		snow();
		List<EntityLiving> walkers = new ArrayList<>();
		walkers.add(onTop(at, 0, EntityRabbit::new));
		walkers.add(onTop(at, 2, EntityFox::new));
		walkers.add(onTop(at, 4, EntityEndermite::new));
		walkers.add(onTop(at, 6, EntitySilverfish::new));
		EntityLiving zombie = onTop(at, 8, EntityZombie::new);
		serverTicks(40);
		for (EntityLiving walker : walkers) {
			double y = onServer(() -> walker.posY);
			assertTrue(y > GROUND + 2 - 0.05D, walker.getClass().getSimpleName() + " sank to " + y);
		}
		assertAlmostEqual(GROUND, onServer(() -> zombie.posY), 0.01D);
	}

	/** A 10 block fall onto it: no damage (26.3's fallOn deals none), and then it sinks in. */
	public void testFallOntoItDoesNoDamage() {
		int[] at = spot(6);
		column(at[0], at[1], GROUND, 2);
		survival();
		stand(at[0] + 0.5D, GROUND + 12, at[1] + 0.5D, 0.0F, 0.0F);
		clearSounds();
		waitUntilOnClient("in the snow", () -> feet() < GROUND + 1.5D, 80);
		ticks(10);
		assertEqual(player().getMaxHealth(), onServer(() -> player().getHealth()), "fall damage");
		// a fall of 7 or more: the big fall sound (26.3 fallOn), though no damage
		boolean big = false;
		for (String sound : soundsHeard()) {
			big |= sound.contains("fall") && sound.contains("big");
		}
		assertTrue(big, "no big fall sound in " + soundsHeard());
	}

	// ---------------------------------------------------------------- freezing (§62.3)

	/** In it 140 ticks: fully frozen; then 1 freeze damage at the next 40th tick, none before. */
	public void testFreezingBuildsUpAndHurts() {
		int[] at = spot(7);
		column(at[0], at[1], GROUND, 2);
		survival();
		HurtLog hurts = new HurtLog();
		MinecraftForge.EVENT_BUS.register(hurts);
		try {
			stand(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
			// 1 a tick, though the player is in two blocks (feet and head): 26.3 freezes once a tick
			int before = onServer(() -> Freezing.getTicksFrozen(player()));
			serverTicks(20);
			int after = onServer(() -> Freezing.getTicksFrozen(player()));
			assertAlmostEqual(20, after - before, 3.0D);
			waitUntil("fully frozen", () -> Freezing.isFullyFrozen(player()), 200);
			waitUntil("hurt by freezing", () -> !hurts.freezeHurtsOf(player()).isEmpty(), 50);
			// only when fully frozen (it may be the very tick it gets there, if that is a 40th: 26.3 too)
			assertEqual(Collections.emptyList(), hurts.frozenTicksBelowFull(player()), "hurt before fully frozen, at frozen ticks");
			assertEqual(1.0F, hurts.freezeHurtsOf(player()).get(0), "freeze damage");
			assertEqual(Freezing.TICKS_TO_FREEZE, onServer(() -> Freezing.getTicksFrozen(player())), "frozen ticks past 140");
		} finally {
			MinecraftForge.EVENT_BUS.unregister(hurts);
		}
	}

	/** The game rule freezeDamage off: fully frozen, but no damage for the player. */
	public void testFreezeDamageRuleOff() {
		int[] at = spot(8);
		column(at[0], at[1], GROUND, 2);
		survival();
		onServer(() -> world().getGameRules().setOrCreateGameRule("freezeDamage", "false"));
		stand(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
		waitUntil("fully frozen", () -> Freezing.isFullyFrozen(player()), 200);
		serverTicks(50);
		assertEqual(player().getMaxHealth(), onServer(() -> player().getHealth()), "hurt with freezeDamage false");
	}

	/** Out of it, frozen ticks go down 2 a tick. */
	public void testThawsTwoATick() {
		int[] at = spot(9);
		snow();
		stand(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
		// the player's in-the-snow flags still say where it stood before (the last test's snow) for its first update here:
		// wait for two of its own updates (they come with its movement packets, not every server tick)
		int[] settled = {onServer(() -> player().ticksExisted)};
		waitUntil("two of the player's own updates", () -> player().ticksExisted >= settled[0] + 2, 40);
		int[] start = onServer(() -> {
			Freezing.setTicksFrozen(player(), Freezing.TICKS_TO_FREEZE);
			return new int[]{Freezing.getTicksFrozen(player()), player().ticksExisted};
		});
		assertEqual(Freezing.TICKS_TO_FREEZE, start[0], "frozen ticks not kept");
		serverTicks(10);
		// a 1.7.10 player's own update runs once for each movement packet, not once a server tick (26.3's), so 10
		// server ticks can hold 12 of them: the thaw is counted against the player's own updates (ticksExisted)
		int[] end = onServer(() -> new int[]{Freezing.getTicksFrozen(player()), player().ticksExisted});
		int updates = end[1] - start[1];
		assertTrue(updates >= 5, "only " + updates + " player updates in 10 server ticks");
		assertEqual(Freezing.TICKS_TO_FREEZE - 2 * updates, end[0], "thawed 2 for each of the player's " + updates + " updates");
	}

	/** One leather piece (a helmet) and the player doesn't freeze. */
	public void testLeatherPieceStopsFreezing() {
		int[] at = spot(10);
		column(at[0], at[1], GROUND, 2);
		survival();
		wear(4, new ItemStack(Items.leather_helmet));
		stand(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
		serverTicks(60);
		assertEqual(true, onServer(() -> Freezing.isInPowderSnow(player())), "not in the snow");
		assertEqual(0, onServer(() -> Freezing.getTicksFrozen(player())), "froze in a leather helmet");
	}

	/** Who can freeze (26.3's canFreeze): not the stray, polar bear, snow golem, wither, skeleton, a leather wearer. */
	public void testWhoCanFreeze() {
		snow();
		assertTrue(onServer(() -> Freezing.canFreeze(new EntityZombie(world()))), "a zombie can't freeze");
		assertTrue(onServer(() -> Freezing.canFreeze(new EntityBlaze(world()))), "a blaze can't freeze");
		assertTrue(onServer(() -> {
			EntitySkeleton wither = new EntitySkeleton(world());
			wither.setSkeletonType(1);
			return Freezing.canFreeze(wither);
		}), "a wither skeleton can't freeze");
		assertFalse(onServer(() -> Freezing.canFreeze(new EntityStray(world()))), "a stray freezes");
		assertFalse(onServer(() -> Freezing.canFreeze(new EntityPolarBear(world()))), "a polar bear freezes");
		assertFalse(onServer(() -> Freezing.canFreeze(new EntitySnowman(world()))), "a snow golem freezes");
		assertFalse(onServer(() -> Freezing.canFreeze(new EntityWither(world()))), "the wither freezes");
		assertFalse(onServer(() -> Freezing.canFreeze(new EntitySkeleton(world()))), "a skeleton freezes");
		assertFalse(onServer(() -> {
			EntityZombie booted = new EntityZombie(world());
			booted.setCurrentItemOrArmor(1, new ItemStack(Items.leather_boots));
			return Freezing.canFreeze(booted);
		}), "a zombie in leather boots freezes");
	}

	/** In the snow: a stray stays at 0, a zombie next to it freezes. */
	public void testStrayStaysUnfrozenZombieFreezes() {
		int[] at = spot(11);
		snow();
		EntityLiving stray = inCell(at, 0, 2, EntityStray::new);
		EntityLiving zombie = inCell(at, 3, 2, EntityZombie::new);
		serverTicks(60);
		assertEqual(0, onServer(() -> Freezing.getTicksFrozen(stray)), "the stray froze");
		assertTrue(onServer(() -> Freezing.getTicksFrozen(zombie)) > 40, "the zombie didn't freeze");
	}

	/** Fully frozen, a blaze takes 5 (freeze_hurts_extra_types). */
	public void testBlazeTakesFive() {
		int[] at = spot(12);
		snow();
		HurtLog hurts = new HurtLog();
		MinecraftForge.EVENT_BUS.register(hurts);
		try {
			EntityLiving blaze = inCell(at, 0, 3, EntityBlaze::new);
			onServer(() -> Freezing.setTicksFrozen(blaze, Freezing.TICKS_TO_FREEZE));
			waitUntil("the blaze hurt by freezing", () -> !hurts.freezeHurtsOf(blaze).isEmpty(), 60);
			assertEqual(5.0F, hurts.freezeHurtsOf(blaze).get(0), "a blaze's freeze damage");
		} finally {
			MinecraftForge.EVENT_BUS.unregister(hurts);
		}
	}

	/** The frost slows: movement speed base - 0.05 * percent frozen, standing on a block; gone once thawed. */
	public void testSlownessWhileFrozen() {
		int[] at = spot(13);
		snow();
		survival();
		stand(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
		onServer(() -> Freezing.setTicksFrozen(player(), Freezing.TICKS_TO_FREEZE));
		serverTicks(1);
		double[] seen = onServer(() -> {
			IAttributeInstance speed = player().getEntityAttribute(SharedMonsterAttributes.movementSpeed);
			return new double[]{Freezing.getTicksFrozen(player()), speed.getBaseValue(), speed.getAttributeValue()};
		});
		assertTrue(seen[0] > 0 && seen[0] < Freezing.TICKS_TO_FREEZE, "frozen ticks " + seen[0]);
		assertAlmostEqual(seen[1] - 0.05D * seen[0] / Freezing.TICKS_TO_FREEZE, seen[2], 1.0E-4D);
		serverTicks(80);
		double[] thawed = onServer(() -> {
			IAttributeInstance speed = player().getEntityAttribute(SharedMonsterAttributes.movementSpeed);
			return new double[]{speed.getBaseValue(), speed.getAttributeValue()};
		});
		assertAlmostEqual(thawed[0], thawed[1], 1.0E-6D);
	}

	/** Set on fire: frozen ticks 0 (26.3 igniteForTicks). */
	public void testFireClearsFreezing() {
		snow();
		survival();
		int[] seen = onServer(() -> {
			Freezing.setTicksFrozen(player(), 100);
			int kept = Freezing.getTicksFrozen(player());
			player().setFire(2);
			return new int[]{kept, Freezing.getTicksFrozen(player())};
		});
		assertEqual(100, seen[0], "frozen ticks not kept");
		assertEqual(0, seen[1], "still frozen after catching fire");
	}

	/** Its death message: "froze to death". */
	public void testDeathMessage() {
		snow();
		assertIsNotNone(Freezing.FREEZE, "no freeze damage type");
		String said = onServer(() -> Freezing.FREEZE.func_151519_b(player()).getUnformattedText());
		assertIn("froze to death", said);
	}

	// ---------------------------------------------------------------- fire in it (§62.1)

	/** A burning player in it: the block at its feet melts, its fire goes out. */
	public void testBurningPlayerMeltsIt() {
		int[] at = spot(14);
		column(at[0], at[1], GROUND, 1);
		survival();
		stand(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
		onServer(() -> player().setFire(5));
		waitUntil("the snow melting", () -> world().getBlock(at[0], GROUND, at[1]) == Blocks.air, 20);
		assertFalse(onServer(() -> player().isBurning()), "still burning");
	}

	/** A burning zombie: the fire goes out; the block melts only with mob griefing on. */
	public void testBurningZombieMeltsOnlyWithMobGriefing() {
		int[] at = spot(15);
		snow();
		EntityLiving kept = inCell(at, 0, 1, EntityZombie::new);
		onServer(() -> kept.setFire(5));
		serverTicks(10);
		assertEqual(snow(), onServer(() -> world().getBlock(at[0], GROUND, at[1])), "melted with mob griefing off");
		assertFalse(onServer(() -> kept.isBurning()), "the zombie still burns");
		onServer(() -> world().getGameRules().setOrCreateGameRule("mobGriefing", "true"));
		EntityLiving melting = inCell(at, 3, 1, EntityZombie::new);
		onServer(() -> melting.setFire(5));
		waitUntil("the snow melting", () -> world().getBlock(at[0] + 3, GROUND, at[1]) == Blocks.air, 20);
	}

	/** At noon under the open sky a zombie in it doesn't catch fire; one beside it on the grass does. */
	public void testUndeadDontBurnInIt() {
		int[] at = spot(16);
		snow();
		EntityLiving inSnow = inCell(at, 0, 1, EntityZombie::new);
		EntityLiving onGrass = inCell(at, 3, 0, EntityZombie::new);
		waitUntil("the zombie on the grass burning", onGrass::isBurning, 300);
		assertFalse(onServer(() -> inSnow.isBurning()), "the zombie in the snow burns");
	}

	// ---------------------------------------------------------------- the bucket (§62.6)

	/** An empty bucket takes it (a powder snow bucket, air left); the powder snow bucket puts it back and empties. */
	public void testBucketTakesAndPlaces() {
		int[] at = spot(17);
		Block snow = snow();
		place(at[0], GROUND, at[1] + 1, snow);
		survival();
		stand(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 48.2F);
		ItemStack filled = onServer(() -> {
			ItemStack empty = new ItemStack(Items.bucket);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, empty);
			ItemStack result = empty.useItemRightClick(world(), player());
			player().inventory.setInventorySlotContents(player().inventory.currentItem, result);
			return result;
		});
		assertEqual(bucket(), filled.getItem(), "the bucket");
		assertEqual(Blocks.air, onServer(() -> world().getBlock(at[0], GROUND, at[1] + 1)), "taken");
		ItemStack after = onServer(() -> {
			ItemStack held = player().getHeldItem();
			held.tryPlaceItemIntoWorld(player(), world(), at[0] + 2, GROUND - 1, at[1], 1, 0.5F, 1.0F, 0.5F);
			return player().getHeldItem();
		});
		assertEqual(snow, onServer(() -> world().getBlock(at[0] + 2, GROUND, at[1])), "not placed");
		assertEqual(Items.bucket, after.getItem(), "the bucket after placing");
	}

	/** In creative the powder snow bucket stays; it can go where the player stands (no placement collision). */
	public void testBucketInCreativeAndWhereThePlayerStands() {
		int[] at = spot(18);
		Block snow = snow();
		stand(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 90.0F);
		ItemStack after = onServer(() -> {
			player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(bucket()));
			player().getHeldItem().tryPlaceItemIntoWorld(player(), world(), at[0], GROUND - 1, at[1], 1, 0.5F, 1.0F, 0.5F);
			return player().getHeldItem();
		});
		assertEqual(snow, onServer(() -> world().getBlock(at[0], GROUND, at[1])), "not placed where the player stands");
		assertEqual(bucket(), after.getItem(), "creative lost the powder snow bucket");
	}

	// ---------------------------------------------------------------- in the world (§62.11)

	/** Flowing water washes it away (no item), and lava too. */
	public void testFluidsWashItAway() {
		int[] water = spot(19);
		place(water[0], GROUND, water[1], snow());
		place(water[0] + 1, GROUND, water[1], Blocks.flowing_water);
		int[] lava = spot(20);
		place(lava[0], GROUND, lava[1], snow());
		place(lava[0] + 1, GROUND, lava[1], Blocks.flowing_lava);
		waitUntil("the water washing it away", () -> world().getBlock(water[0], GROUND, water[1]) != snow(), 40);
		waitUntil("the lava washing it away", () -> world().getBlock(lava[0], GROUND, lava[1]) != snow(), 120);
		assertEqual(0, onServer(() -> entitiesNear(EntityItem.class, 400.0D).size()), "items dropped");
	}

	/** Grass under it shows the snowy side, as under a snow block. */
	public void testGrassUnderItLooksSnowy() {
		int[] at = spot(21);
		stand(at[0] + 0.5D, GROUND, at[1] - 3.5D, 0.0F, 0.0F);
		place(at[0], GROUND, at[1], snow());
		place(at[0] + 2, GROUND, at[1], Blocks.snow);
		ticks(5);
		IIcon[] sides = onClient(() -> new IIcon[]{
				Blocks.grass.getIcon(mc().theWorld, at[0], GROUND - 1, at[1], 2),
				Blocks.grass.getIcon(mc().theWorld, at[0] + 2, GROUND - 1, at[1], 2),
				Blocks.grass.getIcon(mc().theWorld, at[0] + 4, GROUND - 1, at[1], 2)});
		assertNotEqual(sides[2], sides[1]);
		assertEqual(sides[1], sides[0], "grass under powder snow isn't snowy");
	}

	/** No natural spawn in it unless immune: a pig can't, a polar bear can; a pig on bare grass can. */
	public void testNoSpawningInIt() {
		int[] at = spot(22);
		column(at[0], at[1], GROUND, 2);
		boolean[] seen = onServer(() -> {
			EntityPig pig = new EntityPig(world());
			pig.setLocationAndAngles(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
			EntityPolarBear bear = new EntityPolarBear(world());
			bear.setLocationAndAngles(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
			EntityPig onGrass = new EntityPig(world());
			onGrass.setLocationAndAngles(at[0] + 4.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
			return new boolean[]{pig.getCanSpawnHere(), bear.getCanSpawnHere(), onGrass.getCanSpawnHere()};
		});
		assertTrue(seen[2], "a pig can't spawn on bare grass (the test's own check)");
		assertFalse(seen[0], "a pig can spawn in powder snow");
		assertTrue(seen[1], "a polar bear can't spawn in powder snow");
	}

	/** An enderman's teleport into it fails (26.3's enderman_does_not_teleport_to). */
	public void testEndermanWontTeleportIntoIt() throws Exception {
		int[] at = spot(23);
		column(at[0], at[1], GROUND, 2);
		EntityEnderman enderman = spawn(EntityEnderman::new, at[0] + 6.5D, GROUND, at[1] + 0.5D);
		ticks(2);
		Method teleport = method(EntityEnderman.class, new String[]{"teleportTo", "func_70825_j"}, double.class, double.class, double.class);
		Object[] seen = onServer(() -> {
			double before = enderman.posX;
			Object result = teleport.invoke(enderman, at[0] + 0.5D, (double) GROUND, at[1] + 0.5D);
			return new Object[]{result, before, enderman.posX};
		});
		assertEqual(false, seen[0], "teleported into powder snow");
		assertAlmostEqual((Double) seen[1], (Double) seen[2], 0.01D);
	}

	/** A tamed wolf far from its owner doesn't teleport into powder snow round the owner; once it's gone, it does. */
	public void testWolfWontTeleportIntoIt() {
		int[] at = spot(24);
		Block snow = snow();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				place(at[0] + dx, GROUND, at[1] + dz, snow);
				place(at[0] + dx, GROUND + 1, at[1] + dz, snow);
			}
		}
		stand(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
		spotsUsed.add(new int[]{at[0] + 16, at[1]});
		EntityLiving wolf = cellAt(at[0] + 20, at[1], 0, w -> {
			EntityWolf made = new EntityWolf(w);
			made.setTamed(true);
			made.func_152115_b(player().getUniqueID().toString());
			return made;
		});
		serverTicks(60);
		assertTrue(onServer(() -> wolf.posX) > at[0] + 15, "the wolf teleported into the powder snow");
		onServer(() -> {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					if (dx != 0 || dz != 0) {
						world().setBlockToAir(at[0] + dx, GROUND, at[1] + dz);
						world().setBlockToAir(at[0] + dx, GROUND + 1, at[1] + dz);
					}
				}
			}
		});
		waitUntil("the wolf teleporting once the snow is gone (the test's own check)", () -> wolf.posX < at[0] + 5, 80);
	}

	/** A snow layer can't stand on it; falling sand lands on top of it; a piston pushes it. */
	public void testSnowLayerSandAndPiston() {
		int[] at = spot(25);
		Block snow = snow();
		column(at[0], at[1], GROUND, 2);
		assertFalse(onServer(() -> Blocks.snow_layer.canPlaceBlockAt(world(), at[0], GROUND + 2, at[1])), "a snow layer stands on it");
		onServer(() -> {
			EntityFallingBlock sand = new EntityFallingBlock(world(), at[0] + 0.5D, GROUND + 6.5D, at[1] + 0.5D, Blocks.sand);
			sand.field_145812_b = 1;
			world().spawnEntityInWorld(sand);
		});
		waitUntil("the sand landing", () -> world().getBlock(at[0], GROUND + 2, at[1]) == Blocks.sand, 60);
		assertEqual(snow, onServer(() -> world().getBlock(at[0], GROUND + 1, at[1])), "the sand went into the snow");
		place(at[0] + 3, GROUND, at[1], Blocks.piston, 5);
		place(at[0] + 4, GROUND, at[1], snow);
		place(at[0] + 2, GROUND, at[1], Blocks.redstone_block);
		waitUntil("the piston pushing it", () -> world().getBlock(at[0] + 5, GROUND, at[1]) == snow, 20);
	}

	/** Walking inside it plays powder snow's steps, not the grass under it (26.3 inside_step_sound_blocks). */
	public void testStepsInsideArePowderSnows() {
		int[] at = spot(26);
		Block snow = snow();
		for (int dz = 0; dz < 8; dz++) {
			place(at[0], GROUND, at[1] + dz, snow);
		}
		survival();
		stand(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
		look(0.0F, 0.0F);
		ticks(5);
		clearSounds();
		hold(mc().gameSettings.keyBindForward, 30);
		ticks(2);
		assertTrue(onClient(() -> mc().thePlayer.posZ) > at[1] + 1.5D, "didn't walk");
		List<String> steps = new ArrayList<>();
		for (String sound : soundsHeard()) {
			if (sound.contains("step.") || sound.endsWith(".step")) {
				steps.add(sound);
			}
		}
		assertIn(new ResourceLocation(snow.stepSound.getStepResourcePath()).toString(), steps);
		assertNotIn(new ResourceLocation(Blocks.grass.stepSound.getStepResourcePath()).toString(), steps);
		// moving through it the server sent snowflakes (26.3: half the moving ticks)
		assertTrue(onClient(PowderSnowTests::snowflakesShown) > 0, "no snowflakes while walking in it");
	}

	/** Foxes and rabbits get 26.3's ClimbOnTopOfPowderSnowGoal and get out on top; a zombie stays at the bottom. */
	public void testFoxAndRabbitClimbOut() {
		int[] at = spot(28);
		snow();
		EntityLiving fox = inCell(at, 0, 2, EntityFox::new);
		EntityLiving rabbit = inCell(at, 3, 2, EntityRabbit::new);
		for (EntityLiving mob : new EntityLiving[]{fox, rabbit}) {
			boolean hasGoal = onServer(() -> {
				for (Object entry : mob.tasks.taskEntries) {
					if (((net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry) entry).action.getClass().getSimpleName().equals("EntityAIClimbOnTopOfPowderSnow")) {
						return true;
					}
				}
				return false;
			});
			assertTrue(hasGoal, mob.getClass().getSimpleName() + " has no climb-out goal");
		}
		serverTicks(100);
		String where = onServer(() -> "fox at " + fox.posY + ", rabbit at " + rabbit.posY);
		assertTrue(onServer(() -> fox.posY > GROUND + 2 - 0.05D && rabbit.posY > GROUND + 2 - 0.05D), "not both on top: " + where);
	}

	/** In leather boots, jumping gets the player out on top (26.3's climb at 0.2); without them it doesn't. */
	public void testLeatherBootsClimbOut() {
		int[] at = spot(29);
		column(at[0], at[1], GROUND, 2);
		column(at[0] + 4, at[1], GROUND, 2);
		survival();
		wear(1, new ItemStack(Items.leather_boots));
		stand(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
		hold(mc().gameSettings.keyBindJump, 40);
		ticks(10);
		assertAlmostEqual(GROUND + 2, onClient(PowderSnowTests::feet), 0.01D);
		wear(1, null);
		stand(at[0] + 4.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
		hold(mc().gameSettings.keyBindJump, 40);
		ticks(20);
		assertTrue(onClient(PowderSnowTests::feet) < GROUND + 1.0D, "jumped out without leather boots");
	}

	/** A zombie's path goes round a powder snow wall, never through it (26.3's path type POWDER_SNOW, malus -1). */
	public void testMobsPathAroundIt() {
		int[] at = spot(30);
		Block snow = snow();
		for (int dz = -2; dz <= 2; dz++) {
			place(at[0] + 3, GROUND, at[1] + dz, snow);
			place(at[0] + 3, GROUND + 1, at[1] + dz, snow);
		}
		ensureNear(at);
		EntityLiving zombie = spawn(EntityZombie::new, at[0] + 0.5D, GROUND, at[1] + 0.5D);
		ticks(2);
		String through = onServer(() -> {
			net.minecraft.pathfinding.PathEntity path = zombie.getNavigator().getPathToXYZ(at[0] + 6.5D, GROUND, at[1] + 0.5D);
			if (path == null) {
				return "no path";
			}
			for (int i = 0; i < path.getCurrentPathLength(); i++) {
				net.minecraft.pathfinding.PathPoint point = path.getPathPointFromIndex(i);
				if (world().getBlock(point.xCoord, point.yCoord, point.zCoord) == snow
						|| world().getBlock(point.xCoord, point.yCoord + 1, point.zCoord) == snow) {
					return "through the snow at " + point;
				}
			}
			return "";
		});
		assertEqual("", through, "the zombie's path");
	}

	/** Getting off a mount, the player isn't put into powder snow round it (26.3 DismountHelper). */
	public void testNoDismountIntoIt() {
		int[] at = spot(31);
		Block snow = snow();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx != 0 || dz != 0) {
					for (int y = GROUND; y < GROUND + 3; y++) {
						place(at[0] + dx, y, at[1] + dz, snow);
					}
				}
			}
		}
		ensureNear(at);
		EntityLiving pig = spawn(EntityPig::new, at[0] + 0.5D, GROUND, at[1] + 0.5D);
		ticks(2);
		double[] off = onServer(() -> {
			player().setPosition(at[0] + 0.5D, GROUND + 0.9D, at[1] + 0.5D);
			player().dismountEntity(pig);
			return new double[]{player().posX - pig.posX, player().posZ - pig.posZ};
		});
		assertAlmostEqual(0.0D, off[0], 0.3D);
		assertAlmostEqual(0.0D, off[1], 0.3D);
	}

	// ---------------------------------------------------------------- the client (§62.3, §62.5)

	/** The client knows its own player's frozen ticks, and that a zombie is fully frozen (for the shaking). */
	public void testClientKnowsFreezing() {
		int[] at = spot(27);
		column(at[0], at[1], GROUND, 2);
		survival();
		stand(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
		serverTicks(60);
		int server = onServer(() -> Freezing.getTicksFrozen(player()));
		int client = onClient(() -> Freezing.getTicksFrozen(mc().thePlayer));
		assertTrue(server > 40, "the player didn't freeze: " + server);
		assertAlmostEqual(server, client, 3.0D);
		EntityLiving zombie = inCell(at, 3, 2, EntityZombie::new);
		onServer(() -> Freezing.setTicksFrozen(zombie, Freezing.TICKS_TO_FREEZE));
		int id = zombie.getEntityId();
		waitUntilOnClient("the zombie fully frozen on the client", () -> {
			Entity seen = mc().theWorld.getEntityByID(id);
			return seen != null && Freezing.isFullyFrozen(seen);
		}, 20);
	}

	// ---------------------------------------------------------------- helpers

	/** Snowflakes among the client's particles (1.7.10's EffectRenderer keeps them in four layers). */
	private static int snowflakesShown() throws Exception {
		java.lang.reflect.Field layers = null;
		for (String name : new String[]{"fxLayers", "field_78876_b"}) {
			try {
				layers = net.minecraft.client.particle.EffectRenderer.class.getDeclaredField(name);
				break;
			} catch (NoSuchFieldException ignored) {
			}
		}
		if (layers == null) {
			fail("1.7.10's EffectRenderer has no fxLayers");
		}
		layers.setAccessible(true);
		int found = 0;
		for (List<?> layer : (List<?>[]) layers.get(mc().effectRenderer)) {
			for (Object particle : layer) {
				if (particle.getClass().getSimpleName().equals("SnowflakeFX")) {
					found++;
				}
			}
		}
		return found;
	}

	/** The client player's feet: 1.7.10's client posY is 1.62 above them. */
	private static double feet() {
		return mc().thePlayer.boundingBox.minY;
	}

	private static Block snow() {
		Block block = GameRegistry.findBlock(McFly.DOMAIN, "powder_snow");
		assertIsNotNone(block, "EFR has no powder snow block");
		return block;
	}

	private static Item bucket() {
		Item item = GameRegistry.findItem(McFly.DOMAIN, "powder_snow_bucket");
		assertIsNotNone(item, "EFR has no powder snow bucket");
		return item;
	}

	/** Spot n: its own x/z, 12 apart from x/z 100 on; cleared after the test. */
	private int[] spot(int n) {
		int[] at = {100 + n % 8 * 12, 100 + n / 8 * 12};
		spotsUsed.add(at);
		return at;
	}

	private void place(int x, int y, int z, Block block) {
		place(x, y, z, block, 0);
	}

	private void place(int x, int y, int z, Block block, int meta) {
		onServer(() -> {
			world().setBlock(x, y, z, block, meta, 3);
		});
	}

	private void column(int x, int z, int bottom, int height) {
		Block snow = snow();
		for (int y = bottom; y < bottom + height; y++) {
			place(x, y, z, snow);
		}
	}

	private void survival() {
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
	}

	/** Puts on armour (1 boots .. 4 helmet) on the server and sends it to the client. */
	private void wear(int slot, ItemStack armour) {
		onServer(() -> {
			player().setCurrentItemOrArmor(slot, armour);
			player().inventoryContainer.detectAndSendChanges();
		});
		ticks(2);
	}

	private void stand(double x, double y, double z, float yaw, float pitch) {
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch));
		waitUntilOnClient("arriving", () -> Math.abs(mc().thePlayer.posX - x) < 0.01D && Math.abs(mc().thePlayer.posZ - z) < 0.01D
				&& mc().theWorld.getChunkFromBlockCoords((int) Math.floor(x), (int) Math.floor(z)).isChunkLoaded, 60);
		ticks(2);
	}

	/**
	 * A mob in a glass cell (1 x 1, 4 high) at the spot's x + dx, standing on the grass inside a column of powder snow
	 * `height` high (0: none), so it can't walk off. The player is brought near so it ticks.
	 */
	private EntityLiving inCell(int[] at, int dx, int height, Function<net.minecraft.world.World, EntityLiving> make) {
		ensureNear(at);
		return cellAt(at[0] + dx, at[1], height, make);
	}

	/** {@link #inCell} without bringing the player. */
	private EntityLiving cellAt(int x, int z, int height, Function<net.minecraft.world.World, EntityLiving> make) {
		for (int y = GROUND; y < GROUND + height + 4; y++) {
			place(x + 1, y, z, Blocks.glass);
			place(x - 1, y, z, Blocks.glass);
			place(x, y, z + 1, Blocks.glass);
			place(x, y, z - 1, Blocks.glass);
		}
		if (height > 0) {
			column(x, z, GROUND, height);
		}
		EntityLiving mob = spawn(make, x + 0.5D, GROUND, z + 0.5D);
		ticks(2);
		return mob;
	}

	/** A mob standing on a 2 high column of powder snow in a glass cell (so it can't walk off the top). */
	private EntityLiving onTop(int[] at, int dx, Function<net.minecraft.world.World, EntityLiving> make) {
		int x = at[0] + dx;
		int z = at[1];
		for (int y = GROUND; y < GROUND + 5; y++) {
			place(x + 1, y, z, Blocks.glass);
			place(x - 1, y, z, Blocks.glass);
			place(x, y, z + 1, Blocks.glass);
			place(x, y, z - 1, Blocks.glass);
		}
		column(x, z, GROUND, 2);
		ensureNear(at);
		EntityLiving mob = spawn(make, x + 0.5D, GROUND + 2, z + 0.5D);
		ticks(2);
		return mob;
	}

	/** Brings the player within 10 blocks of the spot (mobs far from every player don't tick), unless already there. */
	private void ensureNear(int[] at) {
		double distance = onServer(() -> player().getDistance(at[0] + 0.5D, GROUND, at[1] + 0.5D));
		if (distance > 12.0D) {
			stand(at[0] + 0.5D, GROUND, at[1] - 6.5D, 0.0F, 0.0F);
		}
	}

	private static Method method(Class<?> owner, String[] names, Class<?>... parameters) throws NoSuchMethodException {
		for (String name : names) {
			try {
				Method found = owner.getDeclaredMethod(name, parameters);
				found.setAccessible(true);
				return found;
			} catch (NoSuchMethodException ignored) {
			}
		}
		throw new NoSuchMethodException(owner.getSimpleName() + "." + names[0]);
	}

	/** Every freeze hurt while registered, by entity. Public, for Forge's event bus. */
	public static class HurtLog {
		private final List<Object[]> hurts = Collections.synchronizedList(new ArrayList<>());

		@SubscribeEvent
		public void onHurt(LivingHurtEvent event) {
			if ("freeze".equals(event.source.damageType)) {
				hurts.add(new Object[]{event.entityLiving, event.ammount, Freezing.getTicksFrozen(event.entityLiving)});
			}
		}

		/** The frozen ticks at each freeze hurt of the entity that weren't fully frozen. */
		List<Integer> frozenTicksBelowFull(EntityLivingBase entity) {
			List<Integer> below = new ArrayList<>();
			synchronized (hurts) {
				for (Object[] hurt : hurts) {
					if (hurt[0] == entity && (Integer) hurt[2] < Freezing.TICKS_TO_FREEZE) {
						below.add((Integer) hurt[2]);
					}
				}
			}
			return below;
		}

		List<Float> freezeHurtsOf(EntityLivingBase entity) {
			List<Float> amounts = new ArrayList<>();
			synchronized (hurts) {
				for (Object[] hurt : hurts) {
					if (hurt[0] == entity) {
						amounts.add((Float) hurt[1]);
					}
				}
			}
			return amounts;
		}
	}
}
