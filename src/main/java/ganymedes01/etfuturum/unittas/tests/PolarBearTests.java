package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.McFly;
import ganymedes01.etfuturum.command.modern.arguments.EntityTypeIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The polar bear (SPEC-modern-movement.md §60), the first mob this fork adds. Written before the bear exists, so it is
 * made by its modern id, `minecraft:polar_bear`, and its standing is read by name: until the bear is in, every test
 * fails at "EFR has no polar bear".
 */
public class PolarBearTests extends TasTestCase {

	private static final String BEAR = "minecraft:polar_bear";
	private static final String[] SOUNDS = {"ambient", "ambient_baby", "death", "hurt", "step", "warning"};

	@Override
	protected void tearDown() {
		difficulty(EnumDifficulty.EASY);
	}

	// ---------------------------------------------------------------- the mob

	public void testSummon() {
		assertNoError(command("summon " + BEAR + " ~ ~ ~5"));
		List<EntityAnimal> bears = onServer(() -> bearsNear(0.5D, 5.5D, 3.0D));
		assertEqual(1, bears.size());
		assertEqual(BEAR, onServer(() -> EntityTypeIds.idOf(bears.get(0))));
	}

	/** 30 health, speed 0.25, follow range 20, attack 6; 1.4 x 1.4 with the eyes at 0.85 of that, a cub 0.7 with its eyes at 0.34375. */
	public void testAttributesAndSize() {
		double[] values = onServer(() -> {
			EntityAnimal adult = newBear();
			EntityAnimal cub = newBear();
			cub.setGrowingAge(-24000);
			return new double[]{adult.getEntityAttribute(SharedMonsterAttributes.maxHealth).getBaseValue(),
				adult.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getBaseValue(),
				adult.getEntityAttribute(SharedMonsterAttributes.followRange).getBaseValue(),
				adult.getEntityAttribute(SharedMonsterAttributes.attackDamage) == null ? -1.0D : adult.getEntityAttribute(SharedMonsterAttributes.attackDamage).getBaseValue(),
				adult.width, adult.height, adult.getEyeHeight(), cub.width, cub.height, cub.getEyeHeight()};
		});
		double[] wanted = {30.0D, 0.25D, 20.0D, 6.0D, 1.4D, 1.4D, 1.19D, 0.7D, 0.7D, 0.34375D};
		String[] names = {"health", "speed", "follow range", "attack", "width", "height", "eyes", "cub width", "cub height", "cub eyes"};
		for (int i = 0; i < wanted.length; i++) {
			assertTrue(Math.abs(wanted[i] - values[i]) < 1.0E-3, names[i] + " is " + values[i] + ", not " + wanted[i]);
		}
	}

	/**
	 * A cub on the client stands where the server has it (26.3 keeps a mob's box about its position when its size
	 * changes; 1.7.10 shrinks it from a corner, so a client that first met the cub at adult size drifted it 0.35 blocks).
	 */
	public void testCubWhereItIsOnTheClient() {
		EntityAnimal cub = spawnBear(0.5D, 6.5D, true);
		// no AI: a cub that starts wandering within the ticks below moves off the spot (it failed so once in a full run)
		onServer(() -> {
			cub.tasks.taskEntries.clear();
			cub.targetTasks.taskEntries.clear();
		});
		ticks(10);
		double[] at = onClient(() -> {
			Entity seen = mc().theWorld.getEntityByID(cub.getEntityId());
			return seen == null ? null : new double[]{seen.posX, seen.posZ, (seen.boundingBox.minX + seen.boundingBox.maxX) / 2.0D,
				(seen.boundingBox.minZ + seen.boundingBox.maxZ) / 2.0D};
		});
		assertIsNotNone(at, "the client has no cub");
		assertAlmostEqual(0.5D, at[0], 0.01D);
		assertAlmostEqual(6.5D, at[1], 0.01D);
		assertAlmostEqual(0.5D, at[2], 0.01D);
		assertAlmostEqual(6.5D, at[3], 0.01D);
	}

	/** It eats nothing, so nothing breeds it. */
	public void testNoBreedingItem() {
		List<String> breeding = onServer(() -> {
			EntityAnimal bear = newBear();
			List<String> found = new ArrayList<>();
			for (ItemStack food : new ItemStack[]{new ItemStack(Items.fish, 1, 0), new ItemStack(Items.fish, 1, 1), new ItemStack(Items.cooked_fished),
				new ItemStack(Items.wheat), new ItemStack(Items.porkchop), new ItemStack(Items.apple)}) {
				if (bear.isBreedingItem(food)) {
					found.add(food.getDisplayName());
				}
			}
			return found;
		});
		assertEqual(Collections.emptyList(), breeding);
	}

	/** A spawned group: the first an adult, the rest cubs. */
	public void testSpawnGroupHasCubs() {
		boolean[] babies = onServer(() -> {
			EntityAnimal first = newBear();
			IEntityLivingData group = first.onSpawnWithEgg(null);
			EntityAnimal second = newBear();
			second.onSpawnWithEgg(group);
			EntityAnimal third = newBear();
			third.onSpawnWithEgg(group);
			return new boolean[]{first.isChild(), second.isChild(), third.isChild()};
		});
		assertFalse(babies[0], "the first of the group is a cub");
		assertTrue(babies[1] && babies[2], "the rest are adults");
	}

	/** Its spawn egg gives an adult; used on a bear, a cub. */
	public void testSpawnEgg() {
		ItemIds.Entry egg = ItemIds.byId(BEAR + "_spawn_egg");
		assertTrue(egg != null && egg.item() == Items.spawn_egg, "no polar bear spawn egg");
		Entity spawned = onServer(() -> ItemMonsterPlacer.spawnCreature(world(), egg.meta(), 0.5D, 4.0D, 6.5D));
		assertTrue(spawned != null && BEAR.equals(onServer(() -> EntityTypeIds.idOf(spawned))), "the egg spawned " + spawned);
		assertFalse(onServer(() -> ((EntityAnimal) spawned).isChild()), "the egg gave a cub");
		onServer(() -> {
			player().setCurrentItemOrArmor(0, new ItemStack(Items.spawn_egg, 1, egg.meta()));
			((EntityAnimal) spawned).interact(player());
		});
		waitUntil("a cub from the egg", () -> {
			for (EntityAnimal bear : bearsNear(0.5D, 6.5D, 4.0D)) {
				if (bear.isChild()) {
					return true;
				}
			}
			return false;
		}, 10);
	}

	// ---------------------------------------------------------------- neutral, and mothers

	/** No cub about: it leaves a survival player alone. */
	public void testNeutralWithoutACub() {
		survival();
		EntityAnimal bear = spawnBear(0.5D, 6.5D, false);
		ticks(200);
		assertIsNone(onServer(bear::getAttackTarget));
	}

	/** With a cub within 8 blocks, an adult goes for a survival player within 10. */
	public void testDefendsItsCub() {
		survival();
		EntityAnimal mother = spawnBear(0.5D, 6.5D, false);
		spawnBear(2.5D, 7.5D, true);
		waitUntil("the mother after the player", () -> mother.getAttackTarget() == player(), 300);
	}

	public void testNotInCreative() {
		EntityAnimal mother = spawnBear(0.5D, 6.5D, false);
		spawnBear(2.5D, 7.5D, true);
		ticks(200);
		assertIsNone(onServer(mother::getAttackTarget));
	}

	public void testNotOnPeaceful() {
		survival();
		difficulty(EnumDifficulty.PEACEFUL);
		EntityAnimal mother = spawnBear(0.5D, 6.5D, false);
		spawnBear(2.5D, 7.5D, true);
		ticks(200);
		assertIsNone(onServer(mother::getAttackTarget));
	}

	/** Hurt by a player, it goes for the player, and so does every adult about. */
	public void testHurtBringsTheAdults() {
		survival();
		EntityAnimal hit = spawnBear(0.5D, 6.5D, false);
		EntityAnimal other = spawnBear(5.5D, 10.5D, false);
		onServer(() -> hit.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F));
		waitUntil("the hurt bear after the player", () -> hit.getAttackTarget() == player(), 20);
		waitUntil("the other adult after the player", () -> other.getAttackTarget() == player(), 20);
	}

	/** A hurt cub doesn't fight: it runs, and the adults come. */
	public void testHurtCubCallsTheAdults() {
		survival();
		EntityAnimal cub = spawnBear(0.5D, 6.5D, true);
		EntityAnimal adult = spawnBear(5.5D, 10.5D, false);
		onServer(() -> cub.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F));
		waitUntil("the adult after the player", () -> adult.getAttackTarget() == player(), 20);
		assertIsNone(onServer(cub::getAttackTarget));
	}

	/** Its anger is saved: `anger_end_time` (world time) and `angry_at` (UUID), and loaded back. */
	public void testAngerSaved() {
		survival();
		EntityAnimal bear = spawnBear(0.5D, 6.5D, false);
		onServer(() -> bear.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F));
		waitUntil("the bear after the player", () -> bear.getAttackTarget() == player(), 20);
		ticks(2);
		NBTTagCompound saved = onServer(() -> {
			NBTTagCompound tag = new NBTTagCompound();
			bear.writeToNBT(tag);
			return tag;
		});
		long now = onServer(() -> world().getTotalWorldTime());
		assertTrue(saved.hasKey("anger_end_time"), "no anger_end_time in " + saved);
		long end = saved.getLong("anger_end_time");
		assertTrue(end > now + 15 * 20 && end <= now + 39 * 20, "anger ends at " + end + ", now " + now);
		assertTrue(saved.hasKey("angry_at"), "no angry_at in " + saved);
		NBTTagCompound loaded = onServer(() -> {
			EntityAnimal copy = newBear();
			copy.readFromNBT(saved);
			NBTTagCompound tag = new NBTTagCompound();
			copy.writeToNBT(tag);
			return tag;
		});
		assertEqual(end, loaded.getLong("anger_end_time"));
		assertEqual(saved.getTag("angry_at").toString(), loaded.getTag("angry_at").toString());
	}

	// ---------------------------------------------------------------- the attack

	/**
	 * Its target close but out of reach (held there: speed 0), it stands up with its warning roar in the last 10 ticks
	 * before each try; standing is synced to the client.
	 */
	public void testWarningStand() {
		survival();
		EntityAnimal bear = spawnBear(0.5D, 3.5D, false);
		onServer(() -> {
			bear.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.0D);
			bear.setAttackTarget(player());
		});
		clearSounds();
		waitUntilOnClient("the bear standing on the client", () -> {
			Entity seen = mc().theWorld.getEntityByID(bear.getEntityId());
			return seen != null && standing(seen);
		}, 60);
		waitForSound(McFly.MC_ASSETS + ":entity.polar_bear.warning", 20);
	}

	/** In reach, it hits for its 6 (on Normal, no armour). */
	public void testHitsForSix() {
		survival();
		difficulty(EnumDifficulty.NORMAL);
		onServer(() -> player().setHealth(20.0F));
		EntityAnimal bear = spawnBear(0.5D, 2.0D, false);
		onServer(() -> bear.setAttackTarget(player()));
		waitUntil("the bear's hit", () -> player().getHealth() < 20.0F, 60);
		assertAlmostEqual(14.0D, onServer(() -> (double) player().getHealth()), 1.0E-3);
	}

	/** An adult hunts foxes; a cub leaves them be. */
	public void testAdultHuntsFoxes() {
		EntityLiving fox = spawnFox(0.5D, 9.5D);
		EntityAnimal bear = spawnBear(0.5D, 4.5D, false);
		waitUntil("the bear after the fox", () -> bear.getAttackTarget() == fox, 300);
	}

	public void testCubLeavesFoxesBe() {
		spawnFox(0.5D, 9.5D);
		EntityAnimal cub = spawnBear(0.5D, 4.5D, true);
		ticks(200);
		assertIsNone(onServer(cub::getAttackTarget));
	}

	// ---------------------------------------------------------------- water

	/** In water it keeps 0.98 of its sideways speed a tick and sinks by 0.005 (26.3); a pig keeps 1.7.10's 0.8 and 0.02. */
	public void testSwimsWithLittleDrag() {
		double[] after = onServer(() -> {
			for (int x = 20; x <= 24; x++) {
				for (int y = 20; y <= 23; y++) {
					for (int z = 20; z <= 24; z++) {
						world().setBlock(x, y, z, Blocks.water, 0, 2);
					}
				}
			}
			try {
				EntityAnimal bear = newBear();
				EntityPig pig = new EntityPig(world());
				double[] found = new double[4];
				int i = 0;
				for (EntityLiving swimmer : new EntityLiving[]{bear, pig}) {
					swimmer.setPosition(22.5D, 21.0D, 22.5D);
					swimmer.motionX = 0.1D;
					swimmer.motionY = 0.0D;
					swimmer.motionZ = 0.0D;
					swimmer.handleWaterMovement();
					swimmer.moveEntityWithHeading(0.0F, 0.0F);
					found[i++] = swimmer.motionX;
					found[i++] = swimmer.motionY;
				}
				return found;
			} finally {
				for (int x = 20; x <= 24; x++) {
					for (int y = 20; y <= 23; y++) {
						for (int z = 20; z <= 24; z++) {
							world().setBlock(x, y, z, Blocks.air, 0, 2);
						}
					}
				}
			}
		});
		assertAlmostEqual(0.098D, after[0], 1.0E-4);
		assertAlmostEqual(-0.005D, after[1], 1.0E-4);
		assertAlmostEqual(0.08D, after[2], 1.0E-4);
		assertAlmostEqual(-0.02D, after[3], 1.0E-4);
	}

	// ---------------------------------------------------------------- loot

	/** Ten adults killed: only raw cod and salmon, at most 2 each, and some at all. */
	public void testLootIsFish() {
		List<ItemStack> all = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			List<ItemStack> drops = killBear(false, false);
			assertTrue(count(drops) <= 2, "more than 2 from one bear: " + drops);
			all.addAll(drops);
		}
		assertTrue(!all.isEmpty(), "ten bears dropped nothing");
		for (ItemStack drop : all) {
			assertTrue(drop.getItem() == Items.fish && drop.getItemDamage() <= 1, "dropped " + drop);
		}
	}

	/** On fire, it drops its fish cooked. */
	public void testLootCookedWhenBurning() {
		List<ItemStack> all = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			all.addAll(killBear(false, true));
		}
		assertTrue(!all.isEmpty(), "ten burning bears dropped nothing");
		for (ItemStack drop : all) {
			assertTrue(drop.getItem() == Items.cooked_fished, "dropped " + drop);
		}
	}

	/** Killed with a Fire Aspect weapon in hand (the bear not burning), it drops its fish cooked too. */
	public void testLootCookedByFireAspect() {
		ItemStack sword = new ItemStack(Items.iron_sword);
		sword.addEnchantment(Enchantment.fireAspect, 1);
		setHeld(sword);
		List<ItemStack> all = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			all.addAll(killBear(false, false));
		}
		assertTrue(!all.isEmpty(), "ten bears dropped nothing");
		for (ItemStack drop : all) {
			assertTrue(drop.getItem() == Items.cooked_fished, "dropped " + drop);
		}
	}

	public void testCubDropsNothing() {
		List<ItemStack> all = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			all.addAll(killBear(true, false));
		}
		assertEqual(Collections.emptyList(), all);
	}

	// ---------------------------------------------------------------- spawning

	/** Weight 1 in groups of 1 to 2 in Ice Plains, Ice Plains Spikes, Ice Mountains and Frozen Ocean; not in Plains. */
	public void testSpawnLists() {
		List<String> wrong = onServer(() -> {
			Class<?> bear = newBear().getClass();
			List<String> found = new ArrayList<>();
			BiomeGenBase[] with = {BiomeGenBase.icePlains, BiomeGenBase.getBiome(BiomeGenBase.icePlains.biomeID + 128), BiomeGenBase.iceMountains,
				BiomeGenBase.frozenOcean};
			for (BiomeGenBase biome : with) {
				BiomeGenBase.SpawnListEntry entry = entryOf(biome, bear);
				if (entry == null) {
					found.add("none in " + biome.biomeName);
				} else if (entry.itemWeight != 1 || entry.minGroupCount != 1 || entry.maxGroupCount != 2) {
					found.add(biome.biomeName + ": weight " + entry.itemWeight + ", " + entry.minGroupCount + " to " + entry.maxGroupCount);
				}
			}
			if (entryOf(BiomeGenBase.plains, bear) != null) {
				found.add("one in Plains");
			}
			return found;
		});
		assertEqual(Collections.emptyList(), wrong);
	}

	/** In a frozen ocean it spawns on ice (in light), not on grass; elsewhere on grass as any animal. */
	public void testFrozenOceanSpawnsOnIce() {
		byte[] before = onServer(() -> world().getChunkFromBlockCoords(12, 12).getBiomeArray().clone());
		try {
			boolean[] can = onServer(() -> {
				EntityAnimal bear = newBear();
				bear.setPosition(12.5D, 4.0D, 12.5D);
				boolean plainsOnGrass = bear.getCanSpawnHere();
				byte[] frozen = before.clone();
				java.util.Arrays.fill(frozen, (byte) BiomeGenBase.frozenOcean.biomeID);
				world().getChunkFromBlockCoords(12, 12).setBiomeArray(frozen);
				boolean oceanOnGrass = bear.getCanSpawnHere();
				world().setBlock(12, 3, 12, Blocks.ice, 0, 2);
				boolean oceanOnIce = bear.getCanSpawnHere();
				world().setBlock(12, 3, 12, Blocks.grass, 0, 2);
				return new boolean[]{plainsOnGrass, oceanOnGrass, oceanOnIce};
			});
			assertTrue(can[0], "no spawn on grass in the plains");
			assertFalse(can[1], "a spawn on grass in a frozen ocean");
			assertTrue(can[2], "no spawn on ice in a frozen ocean");
		} finally {
			onServer(() -> world().getChunkFromBlockCoords(12, 12).setBiomeArray(before));
		}
	}

	// ---------------------------------------------------------------- sounds

	public void testSoundsAndSubtitles() {
		List<String> missing = onClient(() -> {
			List<String> found = new ArrayList<>();
			for (String sound : SOUNDS) {
				String path = "entity.polar_bear." + sound;
				if (mc().getSoundHandler().getSound(new ResourceLocation(McFly.MC_ASSETS, path)) == null) {
					found.add("the sound " + path);
				}
				if (!"step".equals(sound) && !StatCollector.canTranslate("subtitle." + path)) {
					found.add("the subtitle of " + path);
				}
			}
			return found;
		});
		assertEqual(Collections.emptyList(), missing);
	}

	// ---------------------------------------------------------------- helpers

	/** A new polar bear, not in the world; on the server. */
	static EntityAnimal newBear() {
		EntityTypeIds.Spawn spawn = EntityTypeIds.spawnFor(BEAR, world().rand);
		Entity made = spawn == null || spawn.name == null ? null : EntityList.createEntityByName(spawn.name, world());
		assertTrue(made instanceof EntityAnimal, "EFR has no polar bear (" + BEAR + ")");
		return (EntityAnimal) made;
	}

	/** A bear in the world, feet on the ground at x z, an adult or a cub. */
	private EntityAnimal spawnBear(double x, double z, boolean cub) {
		return onServer(() -> {
			EntityAnimal bear = newBear();
			if (cub) {
				bear.setGrowingAge(-24000);
			}
			bear.setLocationAndAngles(x, 4.0D, z, 180.0F, 0.0F);
			world().spawnEntityInWorld(bear);
			return bear;
		});
	}

	private EntityLiving spawnFox(double x, double z) {
		EntityLiving made = onServer(() -> {
			EntityTypeIds.Spawn spawn = EntityTypeIds.spawnFor("minecraft:fox", world().rand);
			Entity fox = spawn == null || spawn.name == null ? null : EntityList.createEntityByName(spawn.name, world());
			if (!(fox instanceof EntityLiving)) {
				return null;
			}
			fox.setLocationAndAngles(x, 4.0D, z, 0.0F, 0.0F);
			world().spawnEntityInWorld(fox);
			return (EntityLiving) fox;
		});
		if (made == null) {
			skipTest("EFR's foxes are turned off (enableFoxes)");
		}
		return made;
	}

	/** A bear killed by the player, on fire or not; what it dropped. */
	private List<ItemStack> killBear(boolean cub, boolean burning) {
		EntityAnimal bear = spawnBear(0.5D, 5.5D, cub);
		onServer(() -> {
			if (burning) {
				bear.setFire(10);
			}
			bear.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1000.0F);
		});
		waitUntil("the bear dead", () -> bear.isDead, 40);
		ticks(2);
		return onServer(() -> {
			List<ItemStack> found = new ArrayList<>();
			for (Object loaded : new ArrayList<Object>(world().loadedEntityList)) {
				if (loaded instanceof EntityItem item && !item.isDead
					&& (item.posX - 0.5D) * (item.posX - 0.5D) + (item.posZ - 5.5D) * (item.posZ - 5.5D) <= 9.0D) {
					found.add(item.getEntityItem().copy());
					item.setDead();
				}
			}
			return found;
		});
	}

	private static int count(List<ItemStack> stacks) {
		int count = 0;
		for (ItemStack stack : stacks) {
			count += stack.stackSize;
		}
		return count;
	}

	/** Polar bears alive within this distance of x z (sideways); on the server. */
	private static List<EntityAnimal> bearsNear(double x, double z, double distance) {
		List<EntityAnimal> found = new ArrayList<>();
		for (Object loaded : world().loadedEntityList) {
			if (loaded instanceof EntityAnimal animal && !animal.isDead && BEAR.equals(EntityTypeIds.idOf(animal))
				&& (animal.posX - x) * (animal.posX - x) + (animal.posZ - z) * (animal.posZ - z) <= distance * distance) {
				found.add(animal);
			}
		}
		return found;
	}

	@SuppressWarnings("unchecked")
	private static BiomeGenBase.SpawnListEntry entryOf(BiomeGenBase biome, Class<?> type) {
		if (biome == null) {
			return null;
		}
		for (BiomeGenBase.SpawnListEntry entry : (List<BiomeGenBase.SpawnListEntry>) biome.getSpawnableList(EnumCreatureType.creature)) {
			if (entry.entityClass == type) {
				return entry;
			}
		}
		return null;
	}

	/** The bear's synced standing, read by its name (the bear's class doesn't exist before its code). */
	private static boolean standing(Entity bear) {
		try {
			return (Boolean) bear.getClass().getMethod("isStanding").invoke(bear);
		} catch (ReflectiveOperationException e) {
			fail("the bear has no isStanding: " + e);
			return false;
		}
	}

	private void survival() {
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		ticks(2);
	}
}
