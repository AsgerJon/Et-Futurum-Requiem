package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEventListener;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * McFly's sculk shrieker and Darkness (SPEC-modern-movement.md §90.9, step H4), as 26.3's (`SculkShriekerBlock` and its
 * block entity, `WardenSpawnTracker`, `Warden.applyDarknessAround`, `MobEffects.DARKNESS`): its stored form; a player's
 * step on it, a sensor's clicking near it; with `can_summon` the player's warning level up, the reply at the shriek's
 * end and Darkness to survival players within 40; the warning saved with the player; growth's shrieker; Darkness's blend
 * on the client. On a stone floor at y 7 (x 6 to 30, z 6 to 14).
 */
@TasSuite("sculk")
public class SculkShriekerTests extends TasTestCase {

	private static final int X = 10, Y = 8, Z = 10;

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().playerNetServerHandler.setPlayerLocation(20.5D, 4.0D, -14.5D, 0.0F, 0.0F);
			player().clearActivePotions();
			resetWarning();
		});
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().clearActivePotions();
			resetWarning();
		});
	}

	private static void resetWarning() {
		if (player() instanceof io.github.asgerjon.mcfly.sculk.WardenWarned) {
			((io.github.asgerjon.mcfly.sculk.WardenWarned) player()).mcfly$wardenSpawnTracker().reset();
		}
	}

	private static void clear() {
		for (int x = X - 4; x <= X + 20; x++) {
			for (int z = Z - 4; z <= Z + 4; z++) {
				for (int y = Y - 1; y <= Y + 4; y++) {
					world().setBlock(x, y, z, y == Y - 1 ? Blocks.stone : Blocks.air, 0, 2);
				}
			}
		}
		for (Object entity : world().getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(X - 5, Y - 2, Z - 5, X + 21, Y + 6, Z + 5))) {
			if (!(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
				((Entity) entity).setDead();
			}
		}
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject("etfuturum:" + name);
		return block instanceof Block && block != Blocks.air ? (Block) block : null;
	}

	private void needShrieker() {
		assertTrue(block("sculk_shrieker") != null, "no sculk_shrieker block");
	}

	private static Potion darkness() {
		return ganymedes01.etfuturum.potion.EffectRegistry.get("minecraft:darkness");
	}

	private static NBTTagCompound saved(int x, int y, int z) {
		TileEntity tile = world().getTileEntity(x, y, z);
		NBTTagCompound tag = new NBTTagCompound();
		if (tile != null) {
			tile.writeToNBT(tag);
		}
		return tag;
	}

	/** The player's warden warning as saved (`warden_spawn_tracker`). */
	private static NBTTagCompound warning() {
		NBTTagCompound tag = new NBTTagCompound();
		player().writeToNBT(tag);
		return tag.getCompoundTag("warden_spawn_tracker");
	}

	private static final class Recorder implements GameEventListener {
		final List<String> heard = new ArrayList<>();
		long shriekAt = -1;

		@Override
		public double[] position(World world) {
			return new double[]{X + 0.5D, Y + 0.5D, Z + 0.5D};
		}

		@Override
		public int radius() {
			return 16;
		}

		@Override
		public boolean handle(World world, GameEvent event, GameEvent.Context context, double x, double y, double z) {
			heard.add(event.name.replace("minecraft:", ""));
			if (event.name.equals("minecraft:shriek") && shriekAt < 0) {
				shriekAt = world.getTotalWorldTime();
			}
			return true;
		}
	}

	/** The player onto the shrieker at (X, Y, Z), in survival, standing on its top (half a block up). */
	private void stepOnto() {
		stepOnto(X, Z);
	}

	private void stepOnto(int x, int z) {
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(x + 0.5D, Y + 0.5D, z + 0.5D, 0.0F, 0.0F));
		waitUntil("shrieking", () -> onServer(() -> (world().getBlockMetadata(x, Y, z) & 1) == 1), 60);
	}

	// ---------------------------------------------------------------- stored form

	/** 26.3's shrieker: McFly's, 3 to break, the hoe, 5 experience, only to Silk Touch itself; its block entity with `warning_level` and `listener`. */
	public void testStoredForm() {
		needShrieker();
		String got = onServer(() -> {
			Block shrieker = block("sculk_shrieker");
			world().setBlock(X, Y, Z, shrieker, 0, 3);
			NBTTagCompound tag = saved(X, Y, Z);
			return shrieker.getClass().getName().startsWith("io.github.asgerjon.mcfly.") + " " + shrieker.getBlockHardness(world(), X, Y, Z) + " "
				+ shrieker.getHarvestTool(0) + " " + shrieker.getExpDrop(world(), 0, 0) + " drops " + shrieker.getDrops(world(), X, Y, Z, 0, 0).size() + " silk "
				+ shrieker.canSilkHarvest(world(), player(), X, Y, Z, 0) + "; " + tag.getString("id") + " " + tag.hasKey("warning_level") + " " + tag.hasKey("listener");
		});
		assertEqual("true 3.0 hoe 5 drops 0 silk true; etfuturum.sculk_shrieker true true", got, "the stored form");
	}

	// ---------------------------------------------------------------- shrieking

	/** The player stepping onto it: it shrieks for 90 ticks (a tick due 90 on), posting `shriek`; without `can_summon` no warning. */
	public void testStepShrieks() {
		needShrieker();
		// a place of its own: a tick another test's shriek left due at a shrieker's place would run on this one
		int x = X + 6, z = Z + 2;
		Recorder recorder = new Recorder();
		onServer(() -> {
			world().setBlock(x, Y, z, block("sculk_shrieker"), 0, 3);
			GameEvents.register(world(), recorder);
		});
		try {
			stepOnto(x, z);
			String got = onServer(() -> {
				long due = -1;
				for (net.minecraft.world.NextTickListEntry entry : ((io.github.asgerjon.mcfly.mixins.early.redstone.AccessorWorldServerTicks) world()).mcfly$pendingTicks()) {
					if (entry.xCoord == x && entry.yCoord == Y && entry.zCoord == z) {
						due = entry.scheduledTime - recorder.shriekAt;
					}
				}
				return "shrieking, its end due " + due + " after the shriek, heard shriek " + recorder.heard.contains("shriek")
					+ ", warning " + warning().getInteger("warning_level");
			});
			assertEqual("shrieking, its end due 90 after the shriek, heard shriek true, warning 0", got, "a step on it");
		} finally {
			onServer(() -> GameEvents.unregister(world(), recorder));
		}
	}

	/** A sensor 4 off hearing the player's step clicks; the shrieker hears the clicking (`#shrieker_can_listen`, by the player) and shrieks. */
	public void testSensorSetsItOff() {
		needShrieker();
		onServer(() -> {
			world().setBlock(X, Y, Z, block("sculk_shrieker"), 0, 3);
			world().setBlock(X + 4, Y, Z, block("sculk_sensor"), 0, 3);
		});
		serverTicks(2);
		onServer(() -> GameEvents.post(world(), GameEvent.get("step"), X + 6.5D, Y, Z + 0.5D, player()));
		waitUntil("shrieking", () -> onServer(() -> (world().getBlockMetadata(X, Y, Z) & 1) == 1), 60);
	}

	/**
	 * With `can_summon` (meta 2): the player's warning level goes to 1 with 200 cooldown (26.3's `tryWarn`); at the shriek's
	 * end the reply and Darkness (260 ticks) to the survival player within 40.
	 */
	public void testWarningAndDarkness() {
		needShrieker();
		assertTrue(darkness() != null, "no darkness effect");
		onServer(() -> world().setBlock(X, Y, Z, block("sculk_shrieker"), 2, 3));
		stepOnto();
		String warned = onServer(() -> {
			NBTTagCompound tracker = warning();
			int cooldown = tracker.getInteger("cooldown_ticks");
			return "level " + tracker.getInteger("warning_level") + ", cooldown " + (cooldown > 180 && cooldown <= 200 ? "about 200" : String.valueOf(cooldown));
		});
		assertEqual("level 1, cooldown about 200", warned, "the warning");
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z + 3.5D, 0.0F, 0.0F));
		waitUntil("the shriek's end", () -> onServer(() -> (world().getBlockMetadata(X, Y, Z) & 1) == 0), 120);
		String dark = onServer(() -> {
			PotionEffect effect = player().getActivePotionEffect(darkness());
			return effect == null ? "no darkness" : "darkness " + (effect.getDuration() > 240 && effect.getDuration() <= 260 ? "about 260" : String.valueOf(effect.getDuration()))
				+ ", level " + effect.getAmplifier();
		});
		assertEqual("darkness about 260, level 0", dark, "Darkness");
	}

	/** In creative, a shriek's response gives no Darkness (26.3's `addEffectToPlayersAround`: survival and adventure). */
	public void testNoDarknessInCreative() {
		needShrieker();
		onServer(() -> world().setBlock(X, Y, Z, block("sculk_shrieker"), 2, 3));
		stepOnto();
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z + 3.5D, 0.0F, 0.0F);
		});
		waitUntil("the shriek's end", () -> onServer(() -> (world().getBlockMetadata(X, Y, Z) & 1) == 0), 120);
		assertEqual(false, onServer(() -> darkness() != null && player().isPotionActive(darkness())), "Darkness in creative");
	}

	// ---------------------------------------------------------------- growth

	/** 26.3's growth on the 1 in 11: a shrieker, `can_summon` off. */
	public void testGrowthShrieker() {
		needShrieker();
		long seed = 0;
		for (long s = 0; s < 100000; s++) {
			Random roll = new Random(s);
			if (roll.nextInt(10) == 0 && roll.nextInt(10) < 20 && roll.nextInt(11) == 0) {
				seed = s;
				break;
			}
		}
		long chosen = seed;
		String got = onServer(() -> {
			io.github.asgerjon.mcfly.sculk.SculkBlock sculk = (io.github.asgerjon.mcfly.sculk.SculkBlock) block("sculk");
			world().setBlock(X, Y - 1, Z, block("sculk_catalyst"), 0, 3);
			world().setBlock(X + 8, Y - 1, Z, sculk, 0, 3);
			io.github.asgerjon.mcfly.sculk.SculkSpreader spreader = io.github.asgerjon.mcfly.sculk.SculkSpreader.createLevelSpreader();
			io.github.asgerjon.mcfly.sculk.SculkSpreader.ChargeCursor cursor = new io.github.asgerjon.mcfly.sculk.SculkSpreader.ChargeCursor(X + 8, Y - 1, Z, 20);
			sculk.attemptUseCharge(cursor, world(), X, Y - 1, Z, new Random(chosen), spreader, true);
			return Block.blockRegistry.getNameForObject(world().getBlock(X + 8, Y, Z)) + " " + world().getBlockMetadata(X + 8, Y, Z);
		});
		assertEqual("etfuturum:sculk_shrieker 0", got, "growth's shrieker");
	}

	// ---------------------------------------------------------------- Darkness on the client

	/** 26.3's blend: Darkness fades in over 22 ticks on the client (a factor between 0 and 1 a few ticks in, 1 after 22). */
	public void testDarknessBlends() {
		needShrieker();
		Potion darkness = darkness();
		assertTrue(darkness != null, "no darkness effect");
		onServer(() -> player().addPotionEffect(new PotionEffect(darkness.id, 260, 0)));
		ticks(6);
		float early = onClient(() -> io.github.asgerjon.mcfly.potion.EffectBlend.factor(mc().thePlayer, darkness, 1.0F));
		ticks(30);
		float later = onClient(() -> io.github.asgerjon.mcfly.potion.EffectBlend.factor(mc().thePlayer, darkness, 1.0F));
		assertEqual("fading in, then whole", (early > 0.0F && early < 1.0F ? "fading in" : "early " + early) + ", " + (later == 1.0F ? "then whole" : "later " + later),
			"the blend");
	}
}
