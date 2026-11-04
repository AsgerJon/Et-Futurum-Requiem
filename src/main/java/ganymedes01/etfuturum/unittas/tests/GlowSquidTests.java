package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.entities.EntityGlowSquid;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumSkyBlock;

import java.lang.reflect.Field;
import java.util.List;

/**
 * The glow squid and the glow ink sac (SPEC-modern-movement.md §67.1; 26.3's `GlowSquid`, `GlowSquidRenderer`,
 * `GlowParticle`, `SquidInkParticle`, the loot table `entities/glow_squid`). A pool of water on the flat world (x 40..46,
 * z 40..46, y 4..7) for most tests; sealed stone boxes at y 20 and 31 for the spawn rule (the flat world's sky is open
 * above y 3, and the rule wants no light at all).
 */
public class GlowSquidTests extends TasTestCase {

	private static final int Y = 4;
	private static final String MODERN = McFly.MC_ASSETS + ":";

	@Override
	protected int timeoutSeconds() {
		return 90;
	}

	@Override
	protected void setUp() {
		if (!McFlyConfig.enableGlowSquids) {
			skipTest("enableGlowSquids is off");
		}
		assertTrue(EntityList.classToStringMapping.containsKey(EntityGlowSquid.class), "the glow squid isn't registered");
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			fill(38, Y, 38, 48, Y + 4, 48, Blocks.air);
			fill(38, 18, 38, 44, 35, 44, Blocks.air);
		});
	}

	/** `/summon glow_squid` makes one; 26.3's id and EFR's egg. */
	public void testSummonedById() {
		pool();
		assertNoError(command("summon glow_squid 43 5 43"));
		assertTrue(onServer(() -> !entitiesNear(EntityGlowSquid.class, 80.0D).isEmpty()), "no glow squid after /summon");
	}

	/** Hurt, it goes dark for 100 ticks (synced to the client), counting down a tick. */
	public void testDarkWhenHurt() {
		EntityGlowSquid squid = squidInPool();
		assertEqual(0, onServer(squid::getDarkTicks), "dark before it was hurt");
		// read in the same server step: a tick between would count it down
		assertEqual(100, onServer(() -> {
			squid.attackEntityFrom(DamageSource.generic, 1.0F);
			return squid.getDarkTicks();
		}), "the dark ticks once hurt");
		waitUntilOnClient("the client sees it dark", () -> client(squid) != null && client(squid).getDarkTicks() > 80, 20);
		serverTicks(20);
		int left = onServer(squid::getDarkTicks);
		assertTrue(left <= 81 && left >= 75, "the dark ticks 20 ticks later: " + left);
		waitUntil("light again", () -> squid.getDarkTicks() == 0, 120);
	}

	/**
	 * The light it's drawn with (26.3's `getBlockLightLevel`): 15 x (1 - dark / 10) clamped to 0..15, or the place's
	 * block light when more: full when not dark, the place's when dark, back over the last 10 ticks.
	 */
	public void testGlowLight() {
		assertEqual(15, EntityGlowSquid.glowLight(0, 0));
		assertEqual(7, EntityGlowSquid.glowLight(5, 0));
		assertEqual(9, EntityGlowSquid.glowLight(5, 9));
		assertEqual(1, EntityGlowSquid.glowLight(9, 0));
		assertEqual(0, EntityGlowSquid.glowLight(10, 0));
		assertEqual(4, EntityGlowSquid.glowLight(100, 4));
		EntityGlowSquid squid = squidInPool();
		waitUntilOnClient("the squid on the client", () -> client(squid) != null, 20);
		assertEqual(15, onClient(() -> client(squid).getBrightnessForRender(1.0F) >> 4 & 15), "the block light it's drawn with, not dark");
		onServer(() -> squid.attackEntityFrom(DamageSource.generic, 1.0F));
		waitUntilOnClient("dark on the client", () -> client(squid).getDarkTicks() > 50, 20);
		int place = onServer(() -> world().getSavedLightValue(EnumSkyBlock.Block, 43, Y + 1, 43));
		assertEqual(place, onClient(() -> client(squid).getBrightnessForRender(1.0F) >> 4 & 15), "the block light it's drawn with, dark");
	}

	/** Glow particles around it on the client, every tick. */
	public void testGlowParticles() throws Exception {
		EntityGlowSquid squid = squidInPool();
		waitUntilOnClient("the squid on the client", () -> client(squid) != null, 20);
		ticks(10);
		assertGreater(onClient(() -> particles("GlowFX")), 3);
	}

	/** Hurt by a mob (the player), it squirts glow ink (the particles and `entity.glow_squid.squirt`) and says it's hurt. */
	public void testInkAndHurtSound() throws Exception {
		EntityGlowSquid squid = squidInPool();
		waitUntilOnClient("the squid on the client", () -> client(squid) != null, 20);
		clearSounds();
		// 26.3 squirts only when a mob hurt it (Squid.hurtServer: getLastHurtByMob)
		onServer(() -> squid.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F));
		waitForSound(MODERN + "entity.glow_squid.hurt", 10);
		waitForSound(MODERN + "entity.glow_squid.squirt", 10);
		assertGreater(onClient(() -> particles("GlowSquidInkFX")), 0);
	}

	/** Its ambient and death sounds. */
	public void testAmbientAndDeathSounds() {
		EntityGlowSquid squid = squidInPool();
		clearSounds();
		onServer(squid::playLivingSound);
		waitForSound(MODERN + "entity.glow_squid.ambient", 10);
		onServer(() -> squid.attackEntityFrom(DamageSource.outOfWorld, 100.0F));
		waitForSound(MODERN + "entity.glow_squid.death", 10);
	}

	/** It drops 1 to 3 glow ink sacs, and Looting adds up to its level (26.3: round(level x random)); no ink sacs. */
	public void testLoot() {
		if (!McFlyItems.GLOW_INK_SAC.isEnabled()) {
			skipTest("the glow ink sac is off");
		}
		pool();
		int[] plain = kills(30, 0);
		int[] looting = kills(30, 3);
		String seen = "plain " + plain[0] + ".." + plain[1] + ", Looting III " + looting[0] + ".." + looting[1];
		assertTrue(plain[0] >= 1 && plain[1] <= 3 && plain[1] == 3, seen + " (wanted 1..3)");
		assertTrue(looting[0] >= 1 && looting[1] <= 6 && looting[1] > 3, seen + " (wanted 1..6 with Looting III)");
		assertEqual(0, plain[2] + looting[2], "ink sacs dropped");
	}

	/** It may spawn only at y 30 or below (26.3: sea level - 33), in water, with no light of either kind. */
	public void testSpawnRule() {
		darkBox(38, 20, 38);
		darkBox(38, 31, 38);
		assertEqual(0, onServer(() -> rawLight(40, 21, 40)), "the box at y 20 isn't dark");
		assertTrue(canSpawn(40.5D, 21.0D, 40.5D), "no spawn in dark water at y 21");
		assertFalse(canSpawn(40.5D, 32.0D, 40.5D), "a spawn in dark water at y 32");
		onServer(() -> world().setBlock(41, 21, 41, Blocks.glowstone));
		assertFalse(canSpawn(39.5D, 21.0D, 39.5D), "a spawn in lit water");
		onServer(() -> {
			world().setBlock(41, 21, 41, Blocks.water);
			// the roof (y 23) stays: dark air, so only the water is missing
			fill(39, 21, 39, 41, 22, 41, Blocks.air);
		});
		assertEqual(0, onServer(() -> rawLight(40, 21, 40)), "the emptied box isn't dark");
		assertFalse(canSpawn(40.5D, 21.0D, 40.5D), "a spawn in air");
	}

	/**
	 * What the spawner picks at a spot (`WorldServer.spawnRandomCreature`, through `GlowSquidSpawning`; written after its
	 * code): in dark water at y 30 or below only glow squids, in lit water at the top only squids; and the biome's own
	 * list keeps both (a first try removed from it: Forge passes the biome's list on as it is).
	 */
	public void testSpawnPicksBySpot() {
		if (!McFlyConfig.enableGlowSquidSpawns) {
			skipTest("enableGlowSquidSpawns is off");
		}
		darkBox(38, 20, 38);
		pool();
		String dark = onServer(() -> picks(40, 21, 40));
		String lit = onServer(() -> picks(43, Y + 3, 43));
		String biome = onServer(() -> {
			List<?> own = world().getBiomeGenForCoords(43, 43).getSpawnableList(net.minecraft.entity.EnumCreatureType.waterCreature);
			boolean glow = false, plain = false;
			for (Object entry : own) {
				Class<?> kind = ((net.minecraft.world.biome.BiomeGenBase.SpawnListEntry) entry).entityClass;
				glow |= kind == EntityGlowSquid.class;
				plain |= kind == net.minecraft.entity.passive.EntitySquid.class;
			}
			return (glow ? "glow squids" : "no glow squids") + ", " + (plain ? "squids" : "no squids");
		});
		assertEqual("glow squids", dark, "picked in dark deep water");
		assertEqual("squids", lit, "picked in lit water at the top");
		assertEqual("glow squids, squids", biome, "the biome's own water list afterwards");
	}

	/** What the spawner picked in 200 tries at the spot: "glow squids", "squids", both, or "nothing". */
	private static String picks(int x, int y, int z) {
		boolean glow = false, plain = false;
		for (int i = 0; i < 200; i++) {
			net.minecraft.world.biome.BiomeGenBase.SpawnListEntry entry = world().spawnRandomCreature(net.minecraft.entity.EnumCreatureType.waterCreature, x, y, z);
			if (entry != null) {
				glow |= entry.entityClass == EntityGlowSquid.class;
				plain |= entry.entityClass == net.minecraft.entity.passive.EntitySquid.class;
			}
		}
		return glow && plain ? "both" : glow ? "glow squids" : plain ? "squids" : "nothing";
	}

	/** Its dark ticks are saved and read (`DarkTicksRemaining`). */
	public void testSavedDarkTicks() {
		EntityGlowSquid squid = squidInPool();
		onServer(() -> squid.attackEntityFrom(DamageSource.generic, 1.0F));
		serverTicks(5);
		int[] read = onServer(() -> {
			NBTTagCompound tag = new NBTTagCompound();
			squid.writeToNBT(tag);
			EntityGlowSquid copy = new EntityGlowSquid(world());
			copy.readFromNBT(tag);
			return new int[]{tag.getInteger("DarkTicksRemaining"), copy.getDarkTicks(), squid.getDarkTicks()};
		});
		assertEqual(read[2], read[0], "the saved dark ticks");
		assertEqual(read[2], read[1], "the dark ticks read back");
	}

	/** The glow ink sac is an item of its own, named. */
	public void testGlowInkSac() {
		if (!McFlyItems.GLOW_INK_SAC.isEnabled()) {
			skipTest("the glow ink sac is off");
		}
		assertEqual("Glow Ink Sac", McFlyItems.GLOW_INK_SAC.newItemStack().getDisplayName());
	}

	// ---------------------------------------------------------------- helpers

	private void pool() {
		onServer(() -> {
			fill(39, Y, 39, 47, Y + 3, 47, Blocks.stone);
			fill(40, Y, 40, 46, Y + 3, 46, Blocks.water);
		});
	}

	private EntityGlowSquid squidInPool() {
		pool();
		// beside the pool: a server sends a sound only to players within 16 blocks
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(43.5D, Y, 36.5D, 0.0F, 0.0F));
		waitUntilOnClient("beside the pool", () -> Math.abs(mc().thePlayer.posZ - 36.5D) < 0.01D, 40);
		EntityGlowSquid squid = spawn(EntityGlowSquid::new, 43.5D, Y + 1.0D, 43.5D);
		onServer(() -> squid.tasks.taskEntries.clear());
		return squid;
	}

	/** The squid as the client has it, or null. */
	private static EntityGlowSquid client(EntityGlowSquid squid) {
		Entity entity = mc().theWorld.getEntityByID(squid.getEntityId());
		return entity instanceof EntityGlowSquid glowing ? glowing : null;
	}

	/** Kills this many glow squids (by the player, holding a sword with this Looting); the fewest and most sacs, and ink sacs. */
	private int[] kills(int times, int looting) {
		ItemStack sword = new ItemStack(Items.diamond_sword);
		if (looting > 0) {
			sword.addEnchantment(Enchantment.looting, looting);
		}
		setHeld(sword);
		int fewest = Integer.MAX_VALUE, most = 0, ink = 0;
		for (int i = 0; i < times; i++) {
			EntityGlowSquid squid = spawn(EntityGlowSquid::new, 43.5D, Y + 1.0D, 43.5D);
			int[] dropped = onServer(() -> {
				squid.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(player()), 1000.0F);
				int sacs = 0, inks = 0;
				for (EntityItem item : entitiesNear(EntityItem.class, 80.0D)) {
					ItemStack stack = item.getEntityItem();
					if (stack.getItem() == McFlyItems.GLOW_INK_SAC.get()) {
						sacs += stack.stackSize;
					} else if (stack.getItem() == Items.dye) {
						inks += stack.stackSize;
					}
					item.setDead();
				}
				return new int[]{sacs, inks};
			});
			fewest = Math.min(fewest, dropped[0]);
			most = Math.max(most, dropped[0]);
			ink += dropped[1];
			serverTicks(1);
		}
		return new int[]{fewest, most, ink};
	}

	/** A 5 x 5 x 5 stone box from (x, y, z) with water inside (3 x 3 x 3), sealed, so no light gets in. */
	private void darkBox(int x, int y, int z) {
		onServer(() -> {
			fill(x, y - 1, z, x + 4, y + 3, z + 4, Blocks.stone);
			fill(x + 1, y, z + 1, x + 3, y + 2, z + 3, Blocks.water);
		});
	}

	private boolean canSpawn(double x, double y, double z) {
		return onServer(() -> {
			EntityGlowSquid squid = new EntityGlowSquid(world());
			squid.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
			return squid.getCanSpawnHere();
		});
	}

	private static int rawLight(int x, int y, int z) {
		return Math.max(world().getSavedLightValue(EnumSkyBlock.Sky, x, y, z), world().getSavedLightValue(EnumSkyBlock.Block, x, y, z));
	}

	/** The particles alive now whose class name has this in it. */
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

	private static void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				for (int z = z1; z <= z2; z++) {
					world().setBlock(x, y, z, block, 0, 2);
				}
			}
		}
	}
}
