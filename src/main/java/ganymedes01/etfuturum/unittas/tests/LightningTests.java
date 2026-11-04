package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.combat.AttackCooldown;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import ganymedes01.etfuturum.command.modern.arguments.EnchantmentIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import ganymedes01.etfuturum.entities.EntityArmourStand;
import ganymedes01.etfuturum.entities.EntityBrownMooshroom;
import ganymedes01.etfuturum.entities.EntityThrownTrident;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.trident.Lightning;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Direction;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Channeling and lightning as 26.3 has them, the trident's stage 3 (SPEC-modern-movement.md §58.3): the bolt Channeling
 * calls, what a bolt does to what it strikes, heads from charged creeper kills, copper cleaned, the lightning rod,
 * where natural lightning strikes and the skeleton horse trap. Channeling and the rod are found by their modern ids;
 * natural lightning's target through `trident.Lightning`, a stand-in until the code.
 */
public class LightningTests extends TasTestCase {

	private static final String TRIDENT = "minecraft:trident";
	private static final String THUNDER_SOUND = Reference.MCAssetVer + ":item.trident.thunder";
	/** Past creative's 6-block mob reach: right-clicking a villager in reach trades. */
	private static final double TARGET_Z = 7.5D;

	@Override
	protected void tearDown() {
		weather(false, false);
	}

	// ---------------------------------------------------------------- the enchantment

	/** Level I only, weight 1, enchanting cost 25 to 50, tridents only, never with Riptide. */
	public void testEnchantment() {
		Enchantment channeling = enchantment("minecraft:channeling");
		Enchantment riptide = enchantment("minecraft:riptide");
		assertEqual(1, channeling.getMaxLevel());
		assertEqual(1, channeling.getWeight());
		assertEqual(25, channeling.getMinEnchantability(1));
		assertEqual(50, channeling.getMaxEnchantability(1));
		assertTrue(channeling.canApply(trident(channeling)), "can't go on a trident");
		assertFalse(channeling.canApply(new ItemStack(Items.iron_sword)), "goes on a sword");
		assertFalse(channeling.canApplyTogether(riptide), "goes with Riptide");
		assertFalse(riptide.canApplyTogether(channeling), "Riptide goes with it");
	}

	// ---------------------------------------------------------------- Channeling

	/** A thrown Channeling trident at a mob under the open sky in a thunderstorm: a bolt there, and its thunder. */
	public void testChannelingStrikesTheMobHit() {
		weather(true, true);
		EntityVillager villager = stillMob(EntityVillager::new, 0.5D, TARGET_Z);
		throwChanneling();
		waitUntil("a bolt at the villager", () -> boltsNear(0.5D, TARGET_Z, 2.0D) > 0, 20);
		waitForSound(THUNDER_SOUND, 20);
	}

	public void testNoChannelingWithoutAThunderstorm() {
		weather(true, false);
		EntityVillager villager = stillMob(EntityVillager::new, 0.5D, TARGET_Z);
		throwChanneling();
		waitUntil("the villager hit", () -> villager.getHealth() < villager.getMaxHealth(), 20);
		serverTicks(5);
		assertEqual(0, onServer(() -> boltsNear(0.5D, TARGET_Z, 3.0D)), "a bolt in plain rain");
	}

	/** The mob's block must see the sky: an opaque roof stops it (glass doesn't, in 26.3 as in 1.7.10). */
	public void testNoChannelingUnderARoof() {
		onServer(() -> world().setBlock(0, 7, 7, Blocks.stone, 0, 3));
		try {
			weather(true, true);
			EntityVillager villager = stillMob(EntityVillager::new, 0.5D, TARGET_Z);
			throwChanneling();
			waitUntil("the villager hit", () -> villager.getHealth() < villager.getMaxHealth(), 20);
			serverTicks(5);
			assertEqual(0, onServer(() -> boltsNear(0.5D, TARGET_Z, 3.0D)), "a bolt under a roof");
		} finally {
			onServer(() -> world().setBlock(0, 7, 7, Blocks.air, 0, 3));
		}
	}

	/** Only a thrown trident channels; a melee hit doesn't. */
	public void testNoChannelingInMelee() {
		weather(true, true);
		EntityVillager villager = stillMob(EntityVillager::new, 0.5D, 2.0D);
		holdTrident(trident(enchantment("minecraft:channeling")));
		int id = onServer(villager::getEntityId);
		look(0.0F, 0.0F);
		waitUntilOnClient("the crosshair on the villager", () -> mc().objectMouseOver != null && mc().objectMouseOver.entityHit != null
			&& mc().objectMouseOver.entityHit.getEntityId() == id, 20);
		waitUntil("a full charge", () -> AttackCooldown.charge(player(), 0.0F) >= 1.0F, 60);
		tap(mc().gameSettings.keyBindAttack);
		waitUntil("the villager hit", () -> villager.getHealth() < villager.getMaxHealth(), 10);
		serverTicks(5);
		assertEqual(0, onServer(() -> boltsNear(0.5D, 2.0D, 3.0D)), "a bolt from a melee hit");
	}

	/**
	 * An armour stand counts as a hit (the trident breaks it, 26.3), so a Channeling trident calls a bolt there: the
	 * user's way of turning a mooshroom without the trident's damage (spec §58.3).
	 */
	public void testChannelingAtAnArmourStand() {
		if (!ModItems.WOODEN_ARMORSTAND.isEnabled()) {
			skipTest("armour stands are turned off (enableArmourStand)");
		}
		weather(true, true);
		spawn(EntityArmourStand::new, 0.5D, 4.0D, TARGET_Z);
		throwChanneling();
		waitUntil("a bolt at the armour stand", () -> boltsNear(0.5D, TARGET_Z, 2.0D) > 0, 20);
	}

	/** A thrown Channeling trident hitting a lightning rod under the open sky in a thunderstorm calls a bolt there. */
	public void testChannelingAtALightningRod() {
		assertNoError(command("setblock 0 4 6 stone"));
		assertNoError(command("setblock 0 5 6 minecraft:lightning_rod"));
		try {
			weather(true, true);
			ItemStack channeling = trident(enchantment("minecraft:channeling"));
			onServer(() -> {
				EntityThrownTrident trident = new EntityThrownTrident(world(), player(), channeling);
				trident.setPosition(0.5D, 5.5D, 4.5D);
				trident.motionX = 0.0D;
				trident.motionY = 0.0D;
				trident.motionZ = 1.0D;
				world().spawnEntityInWorld(trident);
			});
			waitUntil("a bolt at the rod", () -> boltsNear(0.5D, 6.5D, 2.0D) > 0, 20);
		} finally {
			clear(0, 0, 4, 6, 6, 6);
		}
	}

	// ---------------------------------------------------------------- what a bolt does

	public void testCreeperCharged() {
		EntityCreeper creeper = stillMob(EntityCreeper::new, 0.5D, TARGET_Z);
		bolt(0.5D, 4.0D, TARGET_Z);
		waitUntil("the creeper charged", creeper::getPowered, 10);
	}

	/** A pig becomes a zombie pigman with a golden sword, which never despawns. */
	public void testPigBecomesAZombiePigman() {
		stillMob(EntityPig::new, 0.5D, TARGET_Z);
		bolt(0.5D, 4.0D, TARGET_Z);
		waitUntil("a zombie pigman", () -> !near(EntityPigZombie.class, 0.5D, TARGET_Z, 3.0D).isEmpty(), 10);
		EntityPigZombie pigman = onServer(() -> near(EntityPigZombie.class, 0.5D, TARGET_Z, 3.0D).get(0));
		assertEqual(Items.golden_sword, onServer(() -> pigman.getHeldItem() == null ? null : pigman.getHeldItem().getItem()));
		assertTrue(onServer(() -> pigman.persistenceRequired), "the zombie pigman can despawn");
	}

	/** On Peaceful a pig is struck as anything else: hurt, still a pig. */
	public void testPigOnPeaceful() {
		difficulty(EnumDifficulty.PEACEFUL);
		EntityPig pig = stillMob(EntityPig::new, 0.5D, TARGET_Z);
		bolt(0.5D, 4.0D, TARGET_Z);
		waitUntil("the pig hurt", () -> pig.getHealth() < pig.getMaxHealth(), 10);
		assertFalse(onServer(() -> pig.isDead), "the pig is gone on Peaceful");
		assertEqual(0, onServer(() -> near(EntityPigZombie.class, 0.5D, TARGET_Z, 3.0D).size()));
	}

	/** A villager becomes a witch, which never despawns. */
	public void testVillagerBecomesAWitch() {
		stillMob(EntityVillager::new, 0.5D, TARGET_Z);
		bolt(0.5D, 4.0D, TARGET_Z);
		waitUntil("a witch", () -> !near(EntityWitch.class, 0.5D, TARGET_Z, 3.0D).isEmpty(), 10);
		assertTrue(onServer(() -> near(EntityWitch.class, 0.5D, TARGET_Z, 3.0D).get(0).persistenceRequired), "the witch can despawn");
	}

	/** On Peaceful a villager is struck as anything else: hurt, still a villager. */
	public void testVillagerOnPeaceful() {
		difficulty(EnumDifficulty.PEACEFUL);
		EntityVillager villager = stillMob(EntityVillager::new, 0.5D, TARGET_Z);
		bolt(0.5D, 4.0D, TARGET_Z);
		waitUntil("the villager hurt", () -> villager.getHealth() < villager.getMaxHealth(), 10);
		assertFalse(onServer(() -> villager.isDead), "the villager is gone on Peaceful");
		assertEqual(0, onServer(() -> near(EntityWitch.class, 0.5D, TARGET_Z, 3.0D).size()));
	}

	/** A red mooshroom turns brown, once a bolt, unhurt and not burning, the same calf with the same name. */
	public void testMooshroomTurnsBrown() {
		EntityMooshroom red = stillMob(EntityMooshroom::new, 0.5D, TARGET_Z);
		onServer(() -> {
			red.setGrowingAge(-24000);
			red.setCustomNameTag("Daisy");
		});
		bolt(0.5D, 4.0D, TARGET_Z);
		serverTicks(15);
		List<EntityMooshroom> mooshrooms = onServer(() -> near(EntityMooshroom.class, 0.5D, TARGET_Z, 3.0D));
		assertEqual(1, mooshrooms.size(), "mooshrooms after the bolt");
		EntityMooshroom after = mooshrooms.get(0);
		assertTrue(after instanceof EntityBrownMooshroom, "still red");
		assertTrue(onServer(() -> after.getGrowingAge() < 0), "the calf grew up");
		assertEqual("Daisy", onServer(after::getCustomNameTag));
		assertAlmostEqual(after.getMaxHealth(), onServer(after::getHealth), 1.0E-4);
		assertFalse(onServer(after::isBurning), "set on fire");
	}

	public void testBrownMooshroomTurnsRed() {
		stillMob(EntityBrownMooshroom::new, 0.5D, TARGET_Z);
		bolt(0.5D, 4.0D, TARGET_Z);
		serverTicks(15);
		List<EntityMooshroom> mooshrooms = onServer(() -> near(EntityMooshroom.class, 0.5D, TARGET_Z, 3.0D));
		assertEqual(1, mooshrooms.size(), "mooshrooms after the bolt");
		assertFalse(mooshrooms.get(0) instanceof EntityBrownMooshroom, "still brown");
	}

	/** Item frames, paintings and armour stands are left alone (1.7.10's lightning breaks frames and paintings). */
	public void testFramesPaintingsAndArmourStandsUntouched() {
		onServer(() -> {
			for (int y = 4; y <= 6; y++) {
				for (int x = -1; x <= 1; x++) {
					world().setBlock(x, y, 9, Blocks.stone, 0, 3);
				}
			}
		});
		try {
			List<Entity> spared = onServer(() -> {
				List<Entity> placed = new ArrayList<>();
				int north = Direction.facingToDirection[2];
				EntityItemFrame frame = new EntityItemFrame(world(), 0, 5, 9, north);
				EntityPainting painting = new EntityPainting(world(), 1, 4, 9, north);
				EntityArmourStand stand = new EntityArmourStand(world());
				stand.setLocationAndAngles(-0.5D, 4.0D, 7.5D, 0.0F, 0.0F);
				for (Entity entity : new Entity[] {frame, painting, stand}) {
					world().spawnEntityInWorld(entity);
					placed.add(entity);
				}
				return placed;
			});
			bolt(0.5D, 4.0D, 8.0D);
			serverTicks(15);
			for (Entity entity : spared) {
				assertFalse(onServer(() -> entity.isDead), entity.getClass().getSimpleName() + " broken by lightning");
			}
		} finally {
			clear(-1, 1, 4, 6, 9, 9);
		}
	}

	/** Lightning on oxidized copper: the struck block is back to plain copper. */
	public void testCopperCleaned() {
		assertNoError(command("setblock 0 4 6 minecraft:oxidized_copper"));
		try {
			bolt(0.5D, 5.0D, 6.5D);
			waitUntil("the copper cleaned", () -> "minecraft:copper_block".equals(modernId(0, 4, 6)), 10);
		} finally {
			clear(0, 0, 4, 4, 6, 6);
		}
	}

	/** A struck lightning rod gives a signal of 15 for 8 ticks. */
	public void testLightningRodPowered() {
		assertNoError(command("setblock 0 4 6 minecraft:lightning_rod"));
		try {
			bolt(0.5D, 5.0D, 6.5D);
			waitUntil("the rod powered", () -> rodSignal(0, 4, 6) == 15, 5);
			serverTicks(12);
			assertEqual(0, onServer(() -> rodSignal(0, 4, 6)), "the rod still powered");
		} finally {
			clear(0, 0, 4, 4, 6, 6);
		}
	}

	/** The signal is strong into the block the rod stands on: a lamp beside that block (not beside the rod) lights. */
	public void testLightningRodPowersItsBlock() {
		assertNoError(command("setblock 0 4 6 stone"));
		assertNoError(command("setblock 1 4 6 redstone_lamp"));
		assertNoError(command("setblock 0 5 6 minecraft:lightning_rod"));
		try {
			// pointing up (/setblock gives EFR's blocks their first state only, spec §49)
			onServer(() -> world().setBlockMetadataWithNotify(0, 5, 6, 1, 3));
			bolt(0.5D, 6.0D, 6.5D);
			waitUntil("the lamp lit", () -> world().getBlock(1, 4, 6) == Blocks.lit_redstone_lamp, 5);
		} finally {
			clear(0, 1, 4, 7, 5, 7);
		}
	}

	/** A struck weathered rod is cleaned, as copper is, and its signal still ends. */
	public void testStruckRodCleaned() {
		assertNoError(command("setblock 0 4 6 minecraft:oxidized_lightning_rod"));
		try {
			bolt(0.5D, 5.0D, 6.5D);
			waitUntil("the rod cleaned", () -> "minecraft:lightning_rod".equals(modernId(0, 4, 6)), 5);
			assertEqual(15, onServer(() -> rodSignal(0, 4, 6)), "the cleaned rod's signal");
			serverTicks(20);
			assertEqual(0, onServer(() -> rodSignal(0, 4, 6)), "the cleaned rod still powered");
		} finally {
			clear(0, 0, 4, 4, 6, 6);
		}
	}

	/** A visual-only bolt (a trap's) strikes no one and lights no fire, on Normal where a real one would. */
	public void testVisualBoltHarmless() {
		difficulty(EnumDifficulty.NORMAL);
		EntityPig pig = stillMob(EntityPig::new, 0.5D, TARGET_Z);
		onServer(() -> world().addWeatherEffect(Lightning.visualBolt(world(), 0.5D, 4.0D, TARGET_Z)));
		serverTicks(10);
		assertFalse(onServer(() -> pig.isDead), "the pig turned into a zombie pigman");
		assertEqual(pig.getMaxHealth(), onServer(pig::getHealth), "the pig hurt");
		assertEqual(0, onServer(() -> {
			int fires = 0;
			for (int x = -1; x <= 1; x++) {
				for (int y = 3; y <= 5; y++) {
					for (int z = 6; z <= 8; z++) {
						fires += world().getBlock(x, y, z) == Blocks.fire ? 1 : 0;
					}
				}
			}
			return fires;
		}), "fire from a visual bolt");
	}

	// ---------------------------------------------------------------- heads

	/** A charged creeper's explosion: the zombie it kills drops a zombie head. */
	public void testZombieHead() {
		assertEqual(1, headsFromAChargedCreeper(world -> {
			EntityZombie zombie = new EntityZombie(world);
			zombie.setLocationAndAngles(1.5D, 4.0D, TARGET_Z, 0.0F, 0.0F);
			return Collections.singletonList(zombie);
		}, 2));
	}

	/** And a wither skeleton its skull (EFR's handler left these out). */
	public void testWitherSkeletonSkull() {
		assertEqual(1, headsFromAChargedCreeper(world -> {
			EntitySkeleton skeleton = new EntitySkeleton(world);
			skeleton.setSkeletonType(1);
			skeleton.setLocationAndAngles(1.5D, 4.0D, TARGET_Z, 0.0F, 0.0F);
			return Collections.singletonList(skeleton);
		}, 1));
	}

	/** Only one head a charged creeper, however many it kills. */
	public void testOneHeadACreeper() {
		assertEqual(1, headsFromAChargedCreeper(world -> {
			List<EntityLiving> victims = new ArrayList<>();
			EntityZombie zombie = new EntityZombie(world);
			zombie.setLocationAndAngles(1.5D, 4.0D, TARGET_Z, 0.0F, 0.0F);
			victims.add(zombie);
			EntitySkeleton skeleton = new EntitySkeleton(world);
			skeleton.setLocationAndAngles(-0.5D, 4.0D, TARGET_Z, 0.0F, 0.0F);
			victims.add(skeleton);
			EntityCreeper creeper = new EntityCreeper(world);
			creeper.setLocationAndAngles(0.5D, 4.0D, TARGET_Z + 1.0D, 0.0F, 0.0F);
			victims.add(creeper);
			return victims;
		}, -1));
	}

	// ---------------------------------------------------------------- natural lightning

	/** It goes to the closest lightning rod within 128 blocks that is the highest block of its column. */
	public void testNaturalLightningFindsARod() {
		assertNoError(command("setblock 20 4 20 minecraft:lightning_rod"));
		try {
			int[] target = onServer(() -> Lightning.naturalTarget(world(), 0, 0));
			assertEqual("20 5 20", target[0] + " " + target[1] + " " + target[2]);
		} finally {
			clear(20, 20, 4, 4, 20, 20);
		}
	}

	public void testNaturalLightningIgnoresAFarRod() {
		assertNoError(command("setblock 140 4 0 minecraft:lightning_rod"));
		try {
			int[] target = onServer(() -> Lightning.naturalTarget(world(), 0, 0));
			assertEqual("0 4 0", target[0] + " " + target[1] + " " + target[2]);
		} finally {
			clear(140, 140, 4, 4, 0, 0);
		}
	}

	/** Without a rod, a living mob under the sky within 3 blocks of the column. */
	public void testNaturalLightningFindsAMob() {
		stillMob(EntityVillager::new, 12.5D, 2.5D);
		int[] target = onServer(() -> Lightning.naturalTarget(world(), 10, 0));
		assertEqual("12 4 2", target[0] + " " + target[1] + " " + target[2]);
	}

	// ---------------------------------------------------------------- the skeleton horse trap

	/** A player within 10 blocks springs it: four skeleton horses, each with a skeleton riding. */
	public void testSkeletonTrapSprings() {
		EntityHorse trap = spawn(EntityHorse::new, 0.5D, 4.0D, 8.5D);
		onServer(() -> {
			trap.setHorseType(4);
			edit(trap, tag -> tag.setBoolean("SkeletonTrap", true));
		});
		waitUntil("four ridden skeleton horses", () -> riddenSkeletonHorses() == 4, 40);
		assertFalse(onServer(() -> nbt(trap).getBoolean("SkeletonTrap")), "still a trap");
		assertTrue(onServer(trap::isTame), "the trap horse isn't tame");
	}

	/** An unsprung trap goes after 18 000 ticks. */
	public void testUnsprungTrapGoes() {
		EntityHorse trap = spawn(EntityHorse::new, 0.5D, 4.0D, 30.5D);
		onServer(() -> {
			trap.setHorseType(4);
			edit(trap, tag -> {
				tag.setBoolean("SkeletonTrap", true);
				tag.setInteger("SkeletonTrapTime", 17995);
			});
		});
		serverTicks(15);
		assertTrue(onServer(() -> trap.isDead), "the trap is still there past 18 000 ticks");
	}

	// ---------------------------------------------------------------- helpers

	private static Enchantment enchantment(String id) {
		Enchantment enchantment = EnchantmentIds.byId(id);
		assertIsNotNone(enchantment, "EFR has no " + id);
		return enchantment;
	}

	private static Item tridentItem() {
		ItemIds.Entry entry = ItemIds.byId(TRIDENT);
		assertTrue(entry != null && entry.item() != null, "EFR has no trident (" + TRIDENT + ")");
		return entry.item();
	}

	private static ItemStack trident(Enchantment enchantment) {
		ItemStack trident = new ItemStack(tridentItem());
		trident.addEnchantment(enchantment, 1);
		return trident;
	}

	private void holdTrident(ItemStack trident) {
		setHeld(trident);
		waitUntilOnClient("the trident in hand", () -> mc().thePlayer.getHeldItem() != null && mc().thePlayer.getHeldItem().getItem() == tridentItem(), 40);
	}

	/** Throws a Channeling trident straight ahead from the start. */
	private void throwChanneling() {
		holdTrident(trident(enchantment("minecraft:channeling")));
		look(0.0F, 0.0F);
		hold(mc().gameSettings.keyBindUseItem, 15);
		ticks(2);
	}

	/** A mob that doesn't walk, at x z on the ground. */
	private <E extends EntityLiving> E stillMob(Function<World, E> make, double x, double z) {
		E mob = spawn(make, x, 4.0D, z);
		onServer(() -> mob.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.0D));
		return mob;
	}

	private void bolt(double x, double y, double z) {
		onServer(() -> world().addWeatherEffect(new EntityLightningBolt(world(), x, y, z)));
	}

	/** Bolts within this horizontal distance of x z; on the server. */
	private static int boltsNear(double x, double z, double distance) {
		int count = 0;
		for (Object effect : world().weatherEffects) {
			if (effect instanceof EntityLightningBolt bolt && !bolt.isDead
				&& (bolt.posX - x) * (bolt.posX - x) + (bolt.posZ - z) * (bolt.posZ - z) <= distance * distance) {
				count++;
			}
		}
		return count;
	}

	@SuppressWarnings("unchecked")
	private static <E extends Entity> List<E> near(Class<E> type, double x, double z, double distance) {
		List<E> found = new ArrayList<>();
		for (Object loaded : world().loadedEntityList) {
			if (type.isInstance(loaded) && !((Entity) loaded).isDead) {
				Entity entity = (Entity) loaded;
				if ((entity.posX - x) * (entity.posX - x) + (entity.posZ - z) * (entity.posZ - z) <= distance * distance) {
					found.add((E) entity);
				}
			}
		}
		return found;
	}

	/**
	 * Mobs (made in the world, standing still) next to a charged creeper that blows up: how many heads of this kind
	 * (skull damage; -1 for any) lie on the ground after.
	 */
	private int headsFromAChargedCreeper(Function<World, List<? extends EntityLiving>> victims, int kind) {
		onServer(() -> {
			for (EntityLiving victim : victims.apply(world())) {
				world().spawnEntityInWorld(victim);
				victim.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.0D);
			}
		});
		EntityCreeper creeper = stillMob(EntityCreeper::new, 0.5D, TARGET_Z);
		onServer(() -> {
			creeper.getDataWatcher().updateObject(17, (byte) 1);
			creeper.func_146079_cb();
		});
		waitUntil("the explosion", () -> creeper.isDead, 60);
		serverTicks(5);
		return onServer(() -> {
			int heads = 0;
			for (EntityItem item : near(EntityItem.class, 0.5D, TARGET_Z, 8.0D)) {
				ItemStack stack = item.getEntityItem();
				if (stack.getItem() == Items.skull && (kind < 0 || stack.getItemDamage() == kind)) {
					heads += stack.stackSize;
				}
			}
			return heads;
		});
	}

	/** The block's modern id; on the server. */
	private static String modernId(int x, int y, int z) {
		BlockIds.Found found = BlockIds.find(world().getBlock(x, y, z), world().getBlockMetadata(x, y, z));
		return found == null ? "unknown" : found.type().id;
	}

	/** The strongest weak signal the block gives on any side; on the server. */
	private static int rodSignal(int x, int y, int z) {
		Block block = world().getBlock(x, y, z);
		int strongest = 0;
		for (int side = 0; side < 6; side++) {
			strongest = Math.max(strongest, block.isProvidingWeakPower(world(), x, y, z, side));
		}
		return strongest;
	}

	/** Skeleton horses within 12 blocks of the start with a skeleton riding; on the server. */
	private static int riddenSkeletonHorses() {
		int count = 0;
		for (EntityHorse horse : near(EntityHorse.class, 0.5D, 8.5D, 12.0D)) {
			if (horse.getHorseType() == 4 && horse.riddenByEntity instanceof EntitySkeleton) {
				count++;
			}
		}
		return count;
	}

	private static NBTTagCompound nbt(Entity entity) {
		NBTTagCompound tag = new NBTTagCompound();
		entity.writeToNBT(tag);
		return tag;
	}

	private static void edit(Entity entity, Consumer<NBTTagCompound> change) {
		NBTTagCompound tag = nbt(entity);
		change.accept(tag);
		entity.readFromNBT(tag);
	}

	/** Rain and thunder at full strength at once on both sides, or none. */
	private void weather(boolean rain, boolean thunder) {
		float rainStrength = rain ? 1.0F : 0.0F;
		float thunderStrength = thunder ? 1.0F : 0.0F;
		onServer(() -> {
			world().getWorldInfo().setRaining(rain);
			world().getWorldInfo().setRainTime(rain ? 100000 : 0);
			world().getWorldInfo().setThundering(thunder);
			world().getWorldInfo().setThunderTime(thunder ? 100000 : 0);
			world().rainingStrength = rainStrength;
			world().prevRainingStrength = rainStrength;
			world().thunderingStrength = thunderStrength;
			world().prevThunderingStrength = thunderStrength;
		});
		onClient(() -> {
			mc().theWorld.rainingStrength = rainStrength;
			mc().theWorld.prevRainingStrength = rainStrength;
			mc().theWorld.thunderingStrength = thunderStrength;
			mc().theWorld.prevThunderingStrength = thunderStrength;
		});
		ticks(2);
	}

	private void clear(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		onServer(() -> {
			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					for (int z = minZ; z <= maxZ; z++) {
						world().setBlock(x, y, z, Blocks.air, 0, 3);
					}
				}
			}
		});
	}
}
