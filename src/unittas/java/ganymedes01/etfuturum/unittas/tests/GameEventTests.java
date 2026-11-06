package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEventListener;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.mcfly.gameevent.VibrationSystem;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * McFly's game events and vibrations (SPEC-modern-movement.md §90.9, step H1), as 26.3's (`GameEvent`,
 * `GameEventDispatcher`, `VibrationSystem`, `VibrationSelector`): the 61 events with their radii and frequencies and
 * 26.3's tags; dispatch within both radii, nearest first; a vibration listener's choice of a tick's candidates, its
 * travel time, wool's occlusion, sneaking and damping; and the game posting them where 26.3 does. At z 460, y 8 and up;
 * the game's own at x 14, z 4, by the player (entities there move).
 */
@TasSuite("sculk")
public class GameEventTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 460;
	private static final int NEAR_X = 14, NEAR_Z = 4;

	@Override
	protected void setUp() {
		onServer(() -> {
			clear(X, Z, 20);
			clear(NEAR_X, NEAR_Z, 4);
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().setSneaking(false);
		});
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear(X, Z, 20);
			clear(NEAR_X, NEAR_Z, 4);
			player().setSneaking(false);
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		});
	}

	/** Air round (x, y 8 to 12, z) out to r, a stone floor at y 7; its mobs and items gone. */
	private void clear(int cx, int cz, int r) {
		for (int x = cx - r; x <= cx + r; x++) {
			for (int y = Y - 1; y <= Y + 4; y++) {
				for (int z = cz - 2; z <= cz + 2; z++) {
					world().setBlock(x, y, z, y == Y - 1 ? Blocks.stone : Blocks.air, 0, 2);
				}
			}
		}
		for (Object entity : world().getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(cx - r - 1, Y - 2, cz - 3, cx + r + 2, Y + 6, cz + 4))) {
			if (!(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
				((Entity) entity).setDead();
			}
		}
	}

	/** A plain listener that writes down what it hears. */
	private static final class Recorder implements GameEventListener {
		final String id;
		final double x, y, z;
		final int radius;
		final List<String> heard;

		Recorder(String id, double x, double y, double z, int radius, List<String> heard) {
			this.id = id;
			this.x = x;
			this.y = y;
			this.z = z;
			this.radius = radius;
			this.heard = heard;
		}

		@Override
		public double[] position(World world) {
			return new double[]{x, y, z};
		}

		@Override
		public int radius() {
			return radius;
		}

		@Override
		public boolean handle(World world, GameEvent event, GameEvent.Context context, double ex, double ey, double ez) {
			heard.add(id.isEmpty() ? event.name.replace("minecraft:", "") : id);
			return true;
		}
	}

	/** A vibration user that writes down what's delivered. */
	private static final class Ears implements VibrationSystem.User {
		final double x, y, z;
		final List<String> got = new ArrayList<>();

		Ears(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public int radius() {
			return 8;
		}

		@Override
		public double[] position(World world) {
			return new double[]{x, y, z};
		}

		@Override
		public boolean canReceive(World world, int x, int y, int z, GameEvent event, GameEvent.Context context) {
			return true;
		}

		@Override
		public void onReceive(World world, int x, int y, int z, GameEvent event, Entity source, float distance) {
			got.add(event.name.replace("minecraft:", "") + "@" + (int) distance);
		}
	}

	// ---------------------------------------------------------------- the events

	/** 26.3's events: 61 (46 and amethyst's 15 `resonate_*`); their radii (16, `shriek` 32, the jukebox's 10) and frequencies (1 to 15, resonate_n n); its tags. */
	public void testEventsAsIn26_3() {
		StringBuilder frequencies = new StringBuilder();
		for (String name : new String[]{"step", "projectile_land", "item_interact_finish", "entity_action", "equip", "shear", "entity_damage", "eat",
			"container_close", "block_open", "block_change", "block_destroy", "block_place", "teleport", "explode", "sculk_sensor_tendrils_clicking", "resonate_7"}) {
			GameEvent event = GameEvent.get(name);
			frequencies.append(event == null ? "?" : String.valueOf(event.frequency)).append(" ");
		}
		GameEvent step = GameEvent.get("minecraft:step"), shriek = GameEvent.get("shriek"), jukebox = GameEvent.get("jukebox_play");
		String got = GameEvent.all().size() + " events; radii " + (step == null ? "?" : step.radius) + " " + (shriek == null ? "?" : shriek.radius) + " "
			+ (jukebox == null ? "?" : jukebox.radius) + "; frequencies " + frequencies.toString().trim() + "; step sneak-ignored "
			+ (step != null && step.is("minecraft:ignore_vibrations_sneaking")) + ", clicking for shriekers " + (GameEvent.get("sculk_sensor_tendrils_clicking") != null
			&& GameEvent.get("sculk_sensor_tendrils_clicking").is("minecraft:shrieker_can_listen")) + ", step a vibration " + (step != null && step.is("minecraft:vibrations"));
		assertEqual("61 events; radii 16 32 10; frequencies 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 0 7; step sneak-ignored true, clicking for shriekers true, "
			+ "step a vibration true", got, "26.3's game events");
	}

	/** 26.3's `getRedstoneStrengthForDistance`: max(1, 15 - floor(15 / radius x distance)). */
	public void testRedstoneStrengthForDistance() {
		String got = VibrationSystem.redstoneStrengthForDistance(0.0F, 8) + " " + VibrationSystem.redstoneStrengthForDistance(1.0F, 8) + " "
			+ VibrationSystem.redstoneStrengthForDistance(4.0F, 8) + " " + VibrationSystem.redstoneStrengthForDistance(8.0F, 8) + " "
			+ VibrationSystem.redstoneStrengthForDistance(5.0F, 16);
		assertEqual("15 14 8 1 11", got, "power by distance");
	}

	/** An event reaches the listeners within its radius and theirs, nearest first: 5 and 15 out of 16 hear; 17 out doesn't, nor one of radius 4 at 5. */
	public void testDispatchByRadius() {
		String got = onServer(() -> {
			List<String> heard = new ArrayList<>();
			List<Recorder> recorders = new ArrayList<>();
			recorders.add(new Recorder("17", X + 17.5D, Y + 0.5D, Z + 0.5D, 16, heard));
			recorders.add(new Recorder("15", X + 15.5D, Y + 0.5D, Z + 0.5D, 16, heard));
			recorders.add(new Recorder("5", X - 4.5D, Y + 0.5D, Z + 0.5D, 16, heard));
			recorders.add(new Recorder("5 of 4", X + 0.5D, Y + 0.5D, Z + 5.5D, 4, heard));
			for (Recorder recorder : recorders) {
				GameEvents.register(world(), recorder);
			}
			try {
				GameEvents.post(world(), GameEvent.get("block_place"), X + 0.5D, Y + 0.5D, Z + 0.5D, null);
			} finally {
				for (Recorder recorder : recorders) {
					GameEvents.unregister(world(), recorder);
				}
			}
			return heard.toString();
		});
		assertEqual("[5, 15]", got, "who heard");
	}

	// ---------------------------------------------------------------- vibrations

	/** The listener of a vibration user at the point, registered; the post; its world clock moved on tick by tick, its ticks till delivery. */
	private String vibrate(double ux, String[] events, double[][] from) {
		return onServer(() -> {
			Ears ears = new Ears(ux, Y + 0.5D, Z + 0.5D);
			VibrationSystem.Data data = new VibrationSystem.Data();
			VibrationSystem.Listener listener = new VibrationSystem.Listener(data, ears);
			GameEvents.register(world(), listener);
			try {
				for (int i = 0; i < events.length; i++) {
					GameEvents.post(world(), GameEvent.get(events[i]), from[i][0], from[i][1], from[i][2], null);
				}
				long time = world().getTotalWorldTime();
				int ticks = 0;
				while (ears.got.isEmpty() && ticks < 30) {
					ticks++;
					world().getWorldInfo().incrementTotalWorldTime(time + ticks);
					VibrationSystem.tick(world(), data, ears);
				}
				return ears.got + " after " + ticks;
			} finally {
				GameEvents.unregister(world(), listener);
			}
		});
	}

	/**
	 * 26.3's selector: of a tick's candidates the nearest (a step 3 off over a block placed 6 off), on a tie the higher
	 * frequency (a block placed over a step, both 4 off); chosen the next tick, delivered floor(distance) on: 3 ticks.
	 */
	public void testVibrationChoiceAndTravel() {
		double y = Y + 0.5D, z = Z + 0.5D;
		String nearest = vibrate(X + 0.5D, new String[]{"block_place", "step"}, new double[][]{{X + 6.5D, y, z}, {X + 3.5D, y, z}});
		String tie = vibrate(X + 0.5D, new String[]{"step", "block_place"}, new double[][]{{X + 4.5D, y, z}, {X - 3.5D, y, z}});
		assertEqual("[step@3] after 3", nearest, "the nearest of a tick");
		assertEqual("[block_place@4] after 4", tie, "the higher frequency of a tie");
	}

	/** Wool between occludes (26.3's `#occludes_vibration_signals`); stone doesn't. */
	public void testWoolOccludes() {
		double y = Y + 0.5D, z = Z + 0.5D;
		onServer(() -> world().setBlock(X + 2, Y, Z, Blocks.wool, 0, 2));
		String wool = vibrate(X + 0.5D, new String[]{"block_place"}, new double[][]{{X + 4.5D, y, z}});
		onServer(() -> world().setBlock(X + 2, Y, Z, Blocks.stone, 0, 2));
		String stone = vibrate(X + 0.5D, new String[]{"block_place"}, new double[][]{{X + 4.5D, y, z}});
		assertEqual("[] after 30", wool, "through wool");
		assertEqual("[block_place@4] after 4", stone, "through stone");
	}

	/** A sneaking player's step is ignored (not a block placed); a step on a wool carpet is damped (26.3's `#dampens_vibrations`). */
	public void testSneakingAndDamping() {
		String got = onServer(() -> {
			Ears ears = new Ears(X + 0.5D, Y + 0.5D, Z + 0.5D);
			VibrationSystem.Listener listener = new VibrationSystem.Listener(new VibrationSystem.Data(), ears);
			StringBuilder result = new StringBuilder();
			player().setSneaking(true);
			result.append("sneaking step ").append(listener.handle(world(), GameEvent.get("step"), new GameEvent.Context(player(), null, 0), X + 3.5D, Y + 0.5D, Z + 0.5D));
			result.append(", sneaking place ").append(listener.handle(world(), GameEvent.get("block_place"), new GameEvent.Context(player(), null, 0), X + 3.5D, Y + 0.5D,
				Z + 0.5D));
			player().setSneaking(false);
			listener = new VibrationSystem.Listener(new VibrationSystem.Data(), ears);
			result.append(", step on carpet ").append(listener.handle(world(), GameEvent.get("step"), new GameEvent.Context(player(), Blocks.carpet, 0), X + 3.5D,
				Y + 0.5D, Z + 0.5D));
			return result.toString();
		});
		assertEqual("sneaking step false, sneaking place true, step on carpet false", got, "who is heard");
	}

	// ---------------------------------------------------------------- the game's own

	/** A listener by the player: what each action posts (26.3's), once each. */
	public void testTheGamePostsThem() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(0.5D, 4.0D, 0.5D, 0.0F, 0.0F);
			List<String> heard = new ArrayList<>();
			Recorder recorder = new Recorder("", NEAR_X + 0.5D, Y + 0.5D, NEAR_Z + 0.5D, 16, heard);
			GameEvents.register(world(), recorder);
			StringBuilder result = new StringBuilder();
			try {
				ItemStack stone = new ItemStack(Blocks.stone, 4);
				player().inventory.setInventorySlotContents(player().inventory.currentItem, stone);
				// a player's use, through `ItemStack.tryPlaceItemIntoWorld` (where Forge fires its place event)
				stone.tryPlaceItemIntoWorld(player(), world(), NEAR_X, Y - 1, NEAR_Z, 1, 0.5F, 1.0F, 0.5F);
				result.append("place ").append(heard);
				heard.clear();
				player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
				player().theItemInWorldManager.tryHarvestBlock(NEAR_X, Y, NEAR_Z);
				result.append("; break ").append(heard);
				heard.clear();
				world().setBlock(NEAR_X + 1, Y, NEAR_Z, Blocks.wooden_door, 0, 2);
				world().setBlock(NEAR_X + 1, Y + 1, NEAR_Z, Blocks.wooden_door, 8, 2);
				Blocks.wooden_door.onBlockActivated(world(), NEAR_X + 1, Y, NEAR_Z, player(), 1, 0.5F, 0.5F, 0.5F);
				Blocks.wooden_door.onBlockActivated(world(), NEAR_X + 1, Y, NEAR_Z, player(), 1, 0.5F, 0.5F, 0.5F);
				result.append("; door ").append(heard);
				heard.clear();
				world().setBlock(NEAR_X - 1, Y, NEAR_Z, Blocks.noteblock, 0, 2);
				Blocks.noteblock.onBlockActivated(world(), NEAR_X - 1, Y, NEAR_Z, player(), 1, 0.5F, 0.5F, 0.5F);
				result.append("; note block ").append(heard);
				heard.clear();
				EntityPig pig = new EntityPig(world());
				pig.setLocationAndAngles(NEAR_X + 0.5D, Y, NEAR_Z + 2.5D, 0.0F, 0.0F);
				world().spawnEntityInWorld(pig);
				pig.attackEntityFrom(DamageSource.causePlayerDamage(player()), 2.0F);
				result.append("; pig hurt ").append(heard);
				heard.clear();
				pig.attackEntityFrom(DamageSource.causePlayerDamage(player()), 100.0F);
				result.append(", killed ").append(heard.contains("entity_die") ? "[entity_die]" : heard.toString());
				heard.clear();
				EntitySheep sheep = new EntitySheep(world());
				sheep.setLocationAndAngles(NEAR_X - 1.5D, Y, NEAR_Z + 2.5D, 0.0F, 0.0F);
				world().spawnEntityInWorld(sheep);
				ItemStack shears = new ItemStack(Items.shears);
				player().inventory.setInventorySlotContents(player().inventory.currentItem, shears);
				shears.getItem().itemInteractionForEntity(shears, player(), sheep);
				result.append("; sheared ").append(heard);
				heard.clear();
				// code setting a block posts nothing (26.3's `setBlock` doesn't; the actions above do)
				world().setBlock(NEAR_X + 2, Y, NEAR_Z + 1, Blocks.stone, 0, 3);
				result.append("; code's setBlock ").append(heard);
			} finally {
				GameEvents.unregister(world(), recorder);
			}
			return result.toString();
		});
		assertEqual("place [block_place]; break [block_destroy]; door [block_open, block_close]; note block [note_block_play]; pig hurt [entity_damage], "
			+ "killed [entity_die]; sheared [shear]; code's setBlock []", got, "the game's game events");
	}

	// ---------------------------------------------------------------- the entity's own (H1's rest, batch 1)

	/** A pig by the player at (x, y, z), new (its first update not run yet). */
	private static EntityPig pig(double x, double y, double z) {
		EntityPig pig = new EntityPig(world());
		pig.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
		world().spawnEntityInWorld(pig);
		return pig;
	}

	/** What a listener by the player (radius 16) hears while this runs, on the server. */
	private static List<String> heardDuring(Runnable action) {
		List<String> heard = new ArrayList<>();
		Recorder recorder = new Recorder("", NEAR_X + 0.5D, Y + 0.5D, NEAR_Z + 0.5D, 16, heard);
		GameEvents.register(world(), recorder);
		try {
			action.run();
		} finally {
			GameEvents.unregister(world(), recorder);
		}
		return heard;
	}

	/**
	 * 26.3's steps (`Entity.applyMovementEmissionAndPlaySound`): a step each time 1.7.10 takes one (its walked
	 * distance, x 0.6, past the next whole block): a pig walking 3.6 on the floor steps twice; one moving as far in
	 * water clear of the floor swims twice instead (not on the ground, in water: `swim`).
	 */
	public void testStepsAndSwims() {
		String got = onServer(() -> {
			EntityPig walker = pig(NEAR_X - 3.5D, Y, NEAR_Z - 0.5D);
			List<String> walked = heardDuring(() -> {
				for (int i = 0; i < 12; i++) {
					walker.moveEntity(0.3D, -0.08D, 0.0D);
				}
			});
			walker.setDead();
			for (int x = NEAR_X - 4; x <= NEAR_X + 4; x++) {
				world().setBlock(x, Y, NEAR_Z + 1, Blocks.water, 0, 2);
				world().setBlock(x, Y + 1, NEAR_Z + 1, Blocks.water, 0, 2);
			}
			EntityPig swimmer = pig(NEAR_X - 3.5D, Y + 0.5D, NEAR_Z + 1.5D);
			swimmer.handleWaterMovement();
			List<String> swum = heardDuring(() -> {
				for (int i = 0; i < 12; i++) {
					swimmer.moveEntity(0.3D, 0.0D, 0.0D);
				}
			});
			swimmer.setDead();
			for (int x = NEAR_X - 4; x <= NEAR_X + 4; x++) {
				world().setBlock(x, Y, NEAR_Z + 1, Blocks.air, 0, 2);
				world().setBlock(x, Y + 1, NEAR_Z + 1, Blocks.air, 0, 2);
			}
			return "walking " + walked + ", in water " + (swimmer.isInWater() ? "" : "(not in water) ") + swum;
		});
		assertEqual("walking [step, step], in water [swim, swim]", got, "steps and swims");
	}

	/**
	 * 26.3's `hit_ground` (`Entity.checkFallDamage`: landing with any fall distance) and `splash`
	 * (`Entity.doWaterSplashEffect`: into water, not on the first tick): a pig landing after a fall of 2, one put into
	 * water after its first update.
	 */
	public void testFallAndSplash() {
		String got = onServer(() -> {
			EntityPig faller = pig(NEAR_X - 2.5D, Y + 2.0D, NEAR_Z - 0.5D);
			faller.fallDistance = 2.0F;
			List<String> fell = heardDuring(() -> faller.moveEntity(0.0D, -2.5D, 0.0D));
			faller.setDead();
			EntityPig diver = pig(NEAR_X + 2.5D, Y, NEAR_Z + 1.5D);
			diver.onEntityUpdate();
			world().setBlock(NEAR_X + 2, Y, NEAR_Z + 1, Blocks.water, 0, 2);
			world().setBlock(NEAR_X + 2, Y + 1, NEAR_Z + 1, Blocks.water, 0, 2);
			List<String> splashed = heardDuring(diver::handleWaterMovement);
			diver.setDead();
			world().setBlock(NEAR_X + 2, Y, NEAR_Z + 1, Blocks.air, 0, 2);
			world().setBlock(NEAR_X + 2, Y + 1, NEAR_Z + 1, Blocks.air, 0, 2);
			return "fell " + fell + (faller.onGround ? "" : " (in the air)") + ", into water " + splashed;
		});
		assertEqual("fell [hit_ground], into water [splash]", got, "landing and splashing");
	}

	/** 26.3's `entity_mount` and `entity_dismount` (`Entity.startRiding`, `removeVehicle`): the player on a pig and off, a pig on a pig and off. */
	public void testMountAndDismount() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			EntityPig pig = pig(NEAR_X + 0.5D, Y, NEAR_Z + 1.5D);
			EntityPig rider = pig(NEAR_X - 1.5D, Y, NEAR_Z + 1.5D);
			List<String> mounted = heardDuring(() -> player().mountEntity(pig));
			List<String> dismounted = heardDuring(() -> player().mountEntity(null));
			List<String> pigOn = heardDuring(() -> rider.mountEntity(pig));
			List<String> pigOff = heardDuring(() -> rider.mountEntity(null));
			pig.setDead();
			rider.setDead();
			return "player " + mounted + " " + dismounted + ", pig " + pigOn + " " + pigOff;
		});
		assertEqual("player [entity_mount] [entity_dismount], pig [entity_mount] [entity_dismount]", got, "riding");
	}

	/**
	 * 26.3's `item_interact_start` and `_finish` (`LivingEntity.startUsingItem`, `stopUsingItem`: every item's use
	 * effects make them) and `eat` or `drink` (`Consumable.onConsume`, by its animation): an apple started and stopped,
	 * an apple, a water bottle and milk eaten or drunk.
	 */
	public void testUsingItems() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			ItemStack apple = new ItemStack(Items.apple, 4);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, apple);
			List<String> started = heardDuring(() -> player().setItemInUse(apple, 32));
			List<String> stopped = heardDuring(() -> player().stopUsingItem());
			List<String> ate = heardDuring(() -> apple.onFoodEaten(world(), player()));
			ItemStack water = new ItemStack(Items.potionitem, 1, 0);
			List<String> drank = heardDuring(() -> water.onFoodEaten(world(), player()));
			ItemStack milk = new ItemStack(Items.milk_bucket);
			List<String> milked = heardDuring(() -> milk.onFoodEaten(world(), player()));
			return "start " + started + ", stop " + stopped + ", apple " + ate + ", water " + drank + ", milk " + milked;
		});
		assertEqual("start [item_interact_start], stop [item_interact_finish], apple [eat], water [drink], milk [drink]", got, "using items");
	}

	/**
	 * 26.3's `equip` and `unequip` (`LivingEntity.onEquipItem`: a slot's item changed, not on the first tick; `equip`
	 * when the new item is equippable, else `unequip`, a sword into a mob's hand too; a player's armour slots only): a
	 * zombie pigman given a helmet, a sword, then its helmet taken; the player given a helmet, a sword, then its helmet
	 * taken.
	 */
	public void testEquipping() {
		String mob = onServer(() -> {
			EntityPigZombie pigman = new EntityPigZombie(world());
			pigman.setLocationAndAngles(NEAR_X - 2.5D, Y, NEAR_Z + 1.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(pigman);
			StringBuilder result = new StringBuilder();
			result.append("first ").append(equips(heardDuring(pigman::onUpdate)));
			pigman.setCurrentItemOrArmor(4, new ItemStack(Items.iron_helmet));
			result.append(", helmet ").append(equips(heardDuring(pigman::onUpdate)));
			pigman.setCurrentItemOrArmor(0, new ItemStack(Items.golden_sword));
			result.append(", sword ").append(equips(heardDuring(pigman::onUpdate)));
			pigman.setCurrentItemOrArmor(4, null);
			result.append(", helmet off ").append(equips(heardDuring(pigman::onUpdate)));
			pigman.setDead();
			return result.toString();
		});
		assertEqual("first [], helmet [equip], sword [unequip], helmet off [unequip]", mob, "a mob's equipment");
		List<String> heard = new ArrayList<>();
		Recorder recorder = new Recorder("", NEAR_X + 0.5D, Y + 0.5D, NEAR_Z + 0.5D, 16, heard);
		onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			GameEvents.register(world(), recorder);
		});
		StringBuilder player = new StringBuilder();
		try {
			ticks(3);
			onServer(() -> player().inventory.armorInventory[3] = new ItemStack(Items.iron_helmet));
			ticks(3);
			player.append("helmet ").append(onServer(() -> equips(heard)));
			onServer(heard::clear);
			onServer(() -> player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.golden_sword)));
			ticks(3);
			player.append(", sword ").append(onServer(() -> equips(heard)));
			onServer(heard::clear);
			onServer(() -> player().inventory.armorInventory[3] = null);
			ticks(3);
			player.append(", helmet off ").append(onServer(() -> equips(heard)));
		} finally {
			onServer(() -> {
				GameEvents.unregister(world(), recorder);
				player().inventory.armorInventory[3] = null;
			});
		}
		assertEqual("helmet [equip], sword [], helmet off [unequip]", player.toString(), "the player's equipment");
	}

	private static String equips(List<String> heard) {
		return heard.stream().filter(name -> name.endsWith("equip")).collect(Collectors.toList()).toString();
	}

	// ---------------------------------------------------------------- blocks (H1's rest, batch 2)

	/** A block's use by the player (top face), as the server takes it. */
	private static void use(Block block, int x, int y, int z) {
		block.onBlockActivated(world(), x, y, z, player(), 1, 0.5F, 0.5F, 0.5F);
	}

	/**
	 * 26.3's `block_activate` and `_deactivate` (`LeverBlock.pull`, `ButtonBlock.press` and its release,
	 * `BasePressurePlateBlock.checkPressed`), `block_attach` and `_detach` (`TripWireHookBlock.emitState`, for both
	 * hooks): a lever pulled twice; a stone button pressed and let go; a stone plate stepped on by a pig and left; two
	 * tripwire hooks strung, the string trodden and left, then cut with shears (`shear`, 26.3's
	 * `TripWireBlock.playerWillDestroy`: disarmed, so its going doesn't set the hooks off first).
	 */
	public void testRedstoneParts() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			StringBuilder result = new StringBuilder();
			world().setBlock(NEAR_X - 3, Y, NEAR_Z, Blocks.lever, 5, 2);
			result.append("lever ").append(heardDuring(() -> use(Blocks.lever, NEAR_X - 3, Y, NEAR_Z)));
			result.append(" ").append(heardDuring(() -> use(Blocks.lever, NEAR_X - 3, Y, NEAR_Z)));
			world().setBlock(NEAR_X + 3, Y, NEAR_Z, Blocks.stone, 0, 2);
			world().setBlock(NEAR_X + 3, Y, NEAR_Z + 1, Blocks.stone_button, 3, 2);
			result.append("; button ").append(heardDuring(() -> use(Blocks.stone_button, NEAR_X + 3, Y, NEAR_Z + 1)));
			result.append(" ").append(heardDuring(() -> Blocks.stone_button.updateTick(world(), NEAR_X + 3, Y, NEAR_Z + 1, world().rand)));
			world().setBlock(NEAR_X + 1, Y, NEAR_Z - 1, Blocks.stone_pressure_plate, 0, 2);
			EntityPig pig = pig(NEAR_X + 1.5D, Y, NEAR_Z - 0.5D);
			result.append("; plate ").append(heardDuring(() -> Blocks.stone_pressure_plate.onEntityCollidedWithBlock(world(), NEAR_X + 1, Y, NEAR_Z - 1, pig)));
			pig.setDead();
			pig.setPosition(NEAR_X + 1.5D, Y + 30.0D, NEAR_Z - 0.5D);
			result.append(" ").append(heardDuring(() -> Blocks.stone_pressure_plate.updateTick(world(), NEAR_X + 1, Y, NEAR_Z - 1, world().rand)));
			int z = NEAR_Z + 2;
			world().setBlock(NEAR_X - 4, Y, z, Blocks.stone, 0, 2);
			world().setBlock(NEAR_X - 3, Y, z, Blocks.tripwire_hook, 3, 2);
			world().setBlock(NEAR_X - 2, Y, z, Blocks.tripwire, 0, 2);
			world().setBlock(NEAR_X - 1, Y, z, Blocks.tripwire, 0, 2);
			world().setBlock(NEAR_X, Y, z, Blocks.tripwire_hook, 1, 2);
			world().setBlock(NEAR_X + 1, Y, z, Blocks.stone, 0, 2);
			Runnable hookUpdate = () -> Blocks.tripwire_hook.func_150136_a(world(), NEAR_X - 3, Y, z, false, world().getBlockMetadata(NEAR_X - 3, Y, z), false, -1, 0);
			result.append("; hooks ").append(heardDuring(hookUpdate));
			world().setBlockMetadataWithNotify(NEAR_X - 2, Y, z, world().getBlockMetadata(NEAR_X - 2, Y, z) | 1, 2);
			result.append(", trodden ").append(heardDuring(hookUpdate));
			world().setBlockMetadataWithNotify(NEAR_X - 2, Y, z, world().getBlockMetadata(NEAR_X - 2, Y, z) & ~1, 2);
			result.append(", left ").append(heardDuring(hookUpdate));
			// cut with shears (disarmed first: 26.3's `TripWireBlock.playerWillDestroy`, its `shear`), then gone
			player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.shears));
			result.append(", cut ").append(heardDuring(() -> {
				Blocks.tripwire.onBlockHarvested(world(), NEAR_X - 2, Y, z, world().getBlockMetadata(NEAR_X - 2, Y, z), player());
				world().setBlock(NEAR_X - 2, Y, z, Blocks.air, 0, 2);
			}));
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			return result.toString();
		});
		assertEqual("lever [block_activate] [block_deactivate]; button [block_activate] [block_deactivate]; plate [block_activate] [block_deactivate]; "
			+ "hooks [block_attach, block_attach], trodden [block_activate, block_activate], left [block_deactivate, block_deactivate], "
			+ "cut [shear, block_detach, block_detach]", got, "redstone parts");
	}

	/**
	 * 26.3's `block_open` and `_close` for trapdoors and gates (`TrapDoorBlock.playSound`, `FenceGateBlock`), by hand
	 * and by redstone: a wooden trapdoor used twice and opened by redstone; an oak gate used twice, then opened and
	 * closed by a redstone block beside it; a copper trapdoor (McFly's) used twice.
	 */
	public void testTrapdoorsAndGates() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			StringBuilder result = new StringBuilder();
			world().setBlock(NEAR_X - 3, Y, NEAR_Z + 1, Blocks.stone, 0, 2);
			world().setBlock(NEAR_X - 3, Y, NEAR_Z, Blocks.trapdoor, 0, 2);
			result.append("trapdoor ").append(heardDuring(() -> use(Blocks.trapdoor, NEAR_X - 3, Y, NEAR_Z)));
			result.append(" ").append(heardDuring(() -> use(Blocks.trapdoor, NEAR_X - 3, Y, NEAR_Z)));
			result.append(", by redstone ").append(heardDuring(() -> ((net.minecraft.block.BlockTrapDoor) Blocks.trapdoor).func_150120_a(world(), NEAR_X - 3, Y, NEAR_Z, true)));
			world().setBlock(NEAR_X + 3, Y, NEAR_Z, Blocks.fence_gate, 0, 2);
			result.append("; gate ").append(heardDuring(() -> use(Blocks.fence_gate, NEAR_X + 3, Y, NEAR_Z)));
			result.append(" ").append(heardDuring(() -> use(Blocks.fence_gate, NEAR_X + 3, Y, NEAR_Z)));
			result.append(", by redstone ").append(heardDuring(() -> world().setBlock(NEAR_X + 4, Y, NEAR_Z, Blocks.redstone_block, 0, 3)));
			result.append(" ").append(heardDuring(() -> world().setBlock(NEAR_X + 4, Y, NEAR_Z, Blocks.air, 0, 3)));
			Block copper = (Block) Block.blockRegistry.getObject("etfuturum:copper_trapdoor");
			if (copper != null && copper != Blocks.air) {
				world().setBlock(NEAR_X, Y, NEAR_Z + 2, Blocks.stone, 0, 2);
				world().setBlock(NEAR_X, Y, NEAR_Z + 1, copper, 0, 2);
				result.append("; copper ").append(heardDuring(() -> use(copper, NEAR_X, Y, NEAR_Z + 1)));
				result.append(" ").append(heardDuring(() -> use(copper, NEAR_X, Y, NEAR_Z + 1)));
			} else {
				result.append("; copper (off)");
			}
			return result.toString();
		});
		assertEqual("trapdoor [block_open] [block_close], by redstone [block_open]; gate [block_open] [block_close], by redstone [block_open] "
			+ "[block_close]; copper [block_open] [block_close]", got, "trapdoors and gates");
	}

	/**
	 * 26.3's `container_open` and `_close` (`ContainerOpenersCounter`: the first to open, the last to close; at each
	 * chest of a double one; `ShulkerBoxBlockEntity.startOpen`): a chest, a double chest, an ender chest and a shulker
	 * box opened by the player and closed.
	 */
	public void testContainers() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			StringBuilder result = new StringBuilder();
			world().setBlock(NEAR_X - 3, Y, NEAR_Z + 1, Blocks.chest, 2, 2);
			TileEntityChest chest = (TileEntityChest) world().getTileEntity(NEAR_X - 3, Y, NEAR_Z + 1);
			result.append("chest ").append(heardDuring(() -> player().displayGUIChest(chest)));
			result.append(" ").append(heardDuring(() -> player().closeScreen()));
			world().setBlock(NEAR_X + 2, Y, NEAR_Z + 1, Blocks.chest, 2, 2);
			world().setBlock(NEAR_X + 3, Y, NEAR_Z + 1, Blocks.chest, 2, 2);
			IInventory both = Blocks.chest.func_149951_m(world(), NEAR_X + 2, Y, NEAR_Z + 1);
			result.append("; double ").append(heardDuring(() -> player().displayGUIChest(both)));
			result.append(" ").append(heardDuring(() -> player().closeScreen()));
			world().setBlock(NEAR_X, Y, NEAR_Z + 2, Blocks.ender_chest, 2, 2);
			InventoryEnderChest ender = player().getInventoryEnderChest();
			ender.func_146031_a((TileEntityEnderChest) world().getTileEntity(NEAR_X, Y, NEAR_Z + 2));
			result.append("; ender chest ").append(heardDuring(() -> player().displayGUIChest(ender)));
			result.append(" ").append(heardDuring(() -> player().closeScreen()));
			Block shulker = (Block) Block.blockRegistry.getObject("etfuturum:shulker_box");
			if (shulker != null && shulker != Blocks.air) {
				world().setBlock(NEAR_X - 1, Y, NEAR_Z + 2, shulker, 0, 3);
				result.append("; shulker box ").append(heardDuring(() -> use(shulker, NEAR_X - 1, Y, NEAR_Z + 2)));
				result.append(" ").append(heardDuring(() -> player().closeScreen()));
			} else {
				result.append("; shulker box (off)");
			}
			return result.toString();
		});
		assertEqual("chest [container_open] [container_close]; double [container_open, container_open] [container_close, container_close]; "
			+ "ender chest [container_open] [container_close]; shulker box [container_open] [container_close]", got, "containers");
	}

	/**
	 * 26.3's `fluid_place` and `fluid_pickup` (`BucketItem`, `BottleItem.use`): the player, looking down, empties a
	 * water bucket at its feet, fills the bucket again, then a bottle from a new source there.
	 */
	public void testBucketsAndBottles() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 90.0F);
			StringBuilder result = new StringBuilder();
			ItemStack water = new ItemStack(Items.water_bucket);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, water);
			result.append("emptied ").append(heardDuring(() -> water.getItem().onItemRightClick(water, world(), player())));
			result.append(world().getBlock(NEAR_X, Y, NEAR_Z - 2).getMaterial() == net.minecraft.block.material.Material.water ? "" : " (no water)");
			ItemStack bucket = new ItemStack(Items.bucket);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, bucket);
			result.append(", filled ").append(heardDuring(() -> bucket.getItem().onItemRightClick(bucket, world(), player())));
			world().setBlock(NEAR_X, Y, NEAR_Z - 2, Blocks.water, 0, 2);
			ItemStack bottle = new ItemStack(Items.glass_bottle, 2);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, bottle);
			result.append(", bottle ").append(heardDuring(() -> bottle.getItem().onItemRightClick(bottle, world(), player())));
			world().setBlock(NEAR_X, Y, NEAR_Z - 2, Blocks.air, 0, 2);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			player().rotationPitch = 0.0F;
			return result.toString();
		});
		assertEqual("emptied [fluid_place], filled [fluid_pickup], bottle [fluid_pickup]", got, "buckets and bottles");
	}

	/**
	 * 26.3's `prime_fuse` (`TntBlock.prime`, `Creeper.tick`), `explode` (`ServerExplosion.explode`) and
	 * `lightning_strike` (`LightningBolt.tick`): TNT lit by the player, a creeper set off, a small blast that breaks
	 * nothing, a bolt's first tick.
	 */
	public void testFusesBlastsAndLightning() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			StringBuilder result = new StringBuilder();
			result.append("tnt ").append(heardDuring(() -> ((net.minecraft.block.BlockTNT) Blocks.tnt).func_150114_a(world(), NEAR_X - 3, Y, NEAR_Z + 1, 1, player())));
			for (Object tnt : world().getEntitiesWithinAABB(EntityTNTPrimed.class, AxisAlignedBB.getBoundingBox(NEAR_X - 5, Y - 1, NEAR_Z - 1, NEAR_X - 1, Y + 3, NEAR_Z + 3))) {
				((Entity) tnt).setDead();
			}
			EntityCreeper creeper = new EntityCreeper(world());
			creeper.setLocationAndAngles(NEAR_X + 3.5D, Y, NEAR_Z + 1.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(creeper);
			creeper.func_146079_cb();
			result.append(", creeper ").append(heardDuring(creeper::onUpdate).stream().filter("prime_fuse"::equals).collect(Collectors.toList()));
			creeper.setDead();
			result.append(", blast ").append(heardDuring(() -> world().newExplosion(null, NEAR_X - 2.5D, Y + 0.5D, NEAR_Z + 1.5D, 1.0F, false, false)).stream()
				.filter("explode"::equals).collect(Collectors.toList()));
			EntityLightningBolt bolt = new EntityLightningBolt(world(), NEAR_X - 3.5D, Y, NEAR_Z + 2.5D);
			result.append(", bolt ").append(heardDuring(bolt::onUpdate).stream().filter("lightning_strike"::equals).collect(Collectors.toList()));
			return result.toString();
		});
		assertEqual("tnt [prime_fuse], creeper [prime_fuse], blast [explode], bolt [lightning_strike]", got, "fuses, blasts and lightning");
	}

	// ---------------------------------------------------------------- projectiles, teleports, placing, mobs (batch 3)

	/** Only these events of what was heard. */
	private static List<String> only(List<String> heard, String... names) {
		List<String> wanted = java.util.Arrays.asList(names);
		return heard.stream().filter(wanted::contains).collect(Collectors.toList());
	}

	/** Its updates until it's gone or stuck (an arrow's shake), 30 at most. */
	private static void fly(Entity projectile) {
		for (int i = 0; i < 30 && !projectile.isDead; i++) {
			projectile.onUpdate();
			if (projectile instanceof EntityArrow && ((EntityArrow) projectile).arrowShake > 0) {
				break;
			}
		}
	}

	/**
	 * 26.3's `projectile_shoot` (`Projectile.tick`'s first, by its owner; `HasBeenShot` saved) and `projectile_land`
	 * (`Projectile.onHit`, on a block or an entity): an arrow shot at a wall and one at a pig, a snowball and a small
	 * fireball at the wall; the first arrow saved and loaded shoots no more. A fishing rod cast and reeled in
	 * (`FishingRodItem.use`: `item_interact_start`, `_finish`), its hook's first tick.
	 */
	public void testProjectiles() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			for (int y = Y; y <= Y + 2; y++) {
				world().setBlock(NEAR_X + 3, y, NEAR_Z + 1, Blocks.stone, 0, 2);
			}
			StringBuilder result = new StringBuilder();
			EntityArrow arrow = new EntityArrow(world(), NEAR_X - 2.5D, Y + 1.5D, NEAR_Z + 1.5D);
			arrow.shootingEntity = player();
			arrow.motionX = 1.0D;
			world().spawnEntityInWorld(arrow);
			result.append("arrow at a wall ").append(only(heardDuring(() -> fly(arrow)), "projectile_shoot", "projectile_land"));
			EntityPig pig = pig(NEAR_X + 1.5D, Y, NEAR_Z + 0.5D);
			EntityArrow atPig = new EntityArrow(world(), NEAR_X - 3.5D, Y + 1.2D, NEAR_Z + 0.5D);
			atPig.shootingEntity = player();
			atPig.motionX = 1.0D;
			world().spawnEntityInWorld(atPig);
			result.append(", at a pig ").append(only(heardDuring(() -> fly(atPig)), "projectile_shoot", "projectile_land"));
			pig.setDead();
			EntitySnowball snowball = new EntitySnowball(world(), NEAR_X - 2.5D, Y + 1.5D, NEAR_Z + 1.5D);
			snowball.motionX = 1.0D;
			world().spawnEntityInWorld(snowball);
			result.append(", snowball ").append(only(heardDuring(() -> fly(snowball)), "projectile_shoot", "projectile_land"));
			EntitySmallFireball fireball = new EntitySmallFireball(world(), NEAR_X - 2.5D, Y + 1.5D, NEAR_Z + 1.5D, 1.0D, 0.0D, 0.0D);
			world().spawnEntityInWorld(fireball);
			result.append(", fireball ").append(only(heardDuring(() -> fly(fireball)), "projectile_shoot", "projectile_land"));
			NBTTagCompound saved = new NBTTagCompound();
			arrow.writeToNBT(saved);
			arrow.setDead();
			EntityArrow loaded = new EntityArrow(world());
			loaded.readFromNBT(saved);
			result.append(", the arrow loaded ").append(only(heardDuring(loaded::onUpdate), "projectile_shoot", "projectile_land"));
			ItemStack rod = new ItemStack(Items.fishing_rod);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, rod);
			result.append("; rod cast ").append(only(heardDuring(() -> rod.getItem().onItemRightClick(rod, world(), player())), "item_interact_start",
				"item_interact_finish"));
			Entity hook = player().fishEntity;
			result.append(", its hook ").append(hook == null ? "(none)" : only(heardDuring(hook::onUpdate), "projectile_shoot").toString());
			result.append(", reeled in ").append(only(heardDuring(() -> rod.getItem().onItemRightClick(rod, world(), player())), "item_interact_start",
				"item_interact_finish"));
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			return result.toString();
		});
		assertEqual("arrow at a wall [projectile_shoot, projectile_land], at a pig [projectile_shoot, projectile_land], snowball [projectile_shoot, "
			+ "projectile_land], fireball [projectile_shoot, projectile_land], the arrow loaded []; rod cast [item_interact_start], its hook "
			+ "[projectile_shoot], reeled in [item_interact_finish]", got, "projectiles");
	}

	/**
	 * 26.3's `teleport` (`Enderman.teleport`, `TeleportRandomlyConsumeEffect`: at where it was): an enderman moved
	 * along the floor; the player eating a chorus fruit (McFly's): the teleport, then the eating (26.3's
	 * `Consumable.onConsume`: its effects, then `eat`).
	 */
	public void testTeleports() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			StringBuilder result = new StringBuilder();
			EntityEnderman enderman = new EntityEnderman(world());
			enderman.setLocationAndAngles(NEAR_X - 2.5D, Y, NEAR_Z + 1.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(enderman);
			List<String> moved = heardDuring(() -> {
				try {
					java.lang.reflect.Method teleport = EntityEnderman.class.getDeclaredMethod("teleportTo", double.class, double.class, double.class);
					teleport.setAccessible(true);
					teleport.invoke(enderman, NEAR_X + 2.5D, (double) Y, NEAR_Z + 1.5D);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			});
			result.append("enderman ").append(only(moved, "teleport")).append(enderman.posX > NEAR_X ? "" : " (didn't move)");
			enderman.setDead();
			Item chorus = (Item) Item.itemRegistry.getObject("etfuturum:chorus_fruit");
			if (chorus != null) {
				ItemStack fruit = new ItemStack(chorus, 2);
				result.append("; chorus fruit ").append(only(heardDuring(() -> fruit.onFoodEaten(world(), player())), "teleport", "eat"));
			} else {
				result.append("; chorus fruit (off)");
			}
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			return result.toString();
		});
		assertEqual("enderman [teleport]; chorus fruit [teleport, eat]", got, "teleports");
	}

	/**
	 * 26.3's `entity_place` (`SpawnEggItem`, `MinecartItem`, `BoatItem`, `HangingEntityItem`, `Chicken.aiStep`'s egg,
	 * `EndCrystalItem`), by the player: a pig from an egg, a minecart on a rail, a boat on the floor, a painting and an
	 * item frame on a wall, a chicken's egg, an end crystal on obsidian.
	 */
	public void testPlacingEntities() {
		String got = onServer(() -> {
			// looking at the floor 1.6 ahead (south): a boat goes where the look meets the floor (26.3's `BoatItem`), and
			// none where it would overlap the player, so not a block ahead (where 1.7.10's went to the block's middle)
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 45.0F);
			StringBuilder result = new StringBuilder();
			ItemStack egg = new ItemStack(Items.spawn_egg, 4, 90);
			result.append("spawn egg ").append(only(heardDuring(() -> egg.getItem().onItemUse(egg, player(), world(), NEAR_X - 3, Y - 1, NEAR_Z + 1, 1, 0.5F, 1.0F,
				0.5F)), "entity_place"));
			world().setBlock(NEAR_X + 2, Y, NEAR_Z + 1, Blocks.rail, 0, 2);
			ItemStack cart = new ItemStack(Items.minecart);
			result.append(", minecart ").append(only(heardDuring(() -> cart.getItem().onItemUse(cart, player(), world(), NEAR_X + 2, Y, NEAR_Z + 1, 1, 0.5F, 0.5F,
				0.5F)), "entity_place"));
			ItemStack boat = new ItemStack(Items.boat);
			result.append(", boat ").append(only(heardDuring(() -> boat.getItem().onItemRightClick(boat, world(), player())), "entity_place"));
			world().setBlock(NEAR_X - 1, Y + 1, NEAR_Z + 2, Blocks.stone, 0, 2);
			world().setBlock(NEAR_X + 1, Y + 1, NEAR_Z + 2, Blocks.stone, 0, 2);
			ItemStack painting = new ItemStack(Items.painting);
			result.append(", painting ").append(only(heardDuring(() -> painting.getItem().onItemUse(painting, player(), world(), NEAR_X - 1, Y + 1, NEAR_Z + 2, 2,
				0.5F, 0.5F, 0.0F)), "entity_place"));
			ItemStack frame = new ItemStack(Items.item_frame);
			result.append(", item frame ").append(only(heardDuring(() -> frame.getItem().onItemUse(frame, player(), world(), NEAR_X + 1, Y + 1, NEAR_Z + 2, 2,
				0.5F, 0.5F, 0.0F)), "entity_place"));
			EntityChicken chicken = new EntityChicken(world());
			chicken.setLocationAndAngles(NEAR_X - 2.5D, Y, NEAR_Z - 0.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(chicken);
			chicken.timeUntilNextEgg = 1;
			result.append(", chicken ").append(only(heardDuring(chicken::onLivingUpdate), "entity_place"));
			Item crystalItem = (Item) Item.itemRegistry.getObject("etfuturum:end_crystal");
			if (crystalItem != null) {
				world().setBlock(NEAR_X + 3, Y - 1, NEAR_Z - 1, Blocks.obsidian, 0, 2);
				ItemStack crystal = new ItemStack(crystalItem);
				result.append(", end crystal ").append(only(heardDuring(() -> crystal.getItem().onItemUse(crystal, player(), world(), NEAR_X + 3, Y - 1, NEAR_Z - 1, 1,
					0.5F, 1.0F, 0.5F)), "entity_place"));
				world().setBlock(NEAR_X + 3, Y - 1, NEAR_Z - 1, Blocks.stone, 0, 2);
			} else {
				result.append(", end crystal (none)");
			}
			player().rotationPitch = 0.0F;
			return result.toString();
		});
		assertEqual("spawn egg [entity_place], minecart [entity_place], boat [entity_place], painting [entity_place], item frame [entity_place], "
			+ "chicken [entity_place], end crystal [entity_place]", got, "placing entities");
	}

	/**
	 * 26.3's `entity_interact` (`Mob.interact`: a mob's own use of the player's, or an item used on it,
	 * `Player.interactOn`; a leash untied, not tied): a cow fed wheat, a sheep sheared (`shear` first), a pig touched
	 * with nothing, a cow leashed and let go.
	 */
	public void testInteractingWithMobs() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			StringBuilder result = new StringBuilder();
			EntityCow cow = new EntityCow(world());
			cow.setLocationAndAngles(NEAR_X - 2.5D, Y, NEAR_Z + 1.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(cow);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.wheat, 4));
			result.append("cow fed ").append(only(heardDuring(() -> player().interactWith(cow)), "entity_interact", "shear"));
			EntitySheep sheep = new EntitySheep(world());
			sheep.setLocationAndAngles(NEAR_X + 2.5D, Y, NEAR_Z + 1.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(sheep);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.shears));
			result.append(", sheep sheared ").append(only(heardDuring(() -> player().interactWith(sheep)), "entity_interact", "shear"));
			EntityPig pig = pig(NEAR_X + 0.5D, Y, NEAR_Z + 1.5D);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			result.append(", pig touched ").append(only(heardDuring(() -> player().interactWith(pig)), "entity_interact", "shear"));
			EntityCow other = new EntityCow(world());
			other.setLocationAndAngles(NEAR_X - 0.5D, Y, NEAR_Z + 1.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(other);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.lead, 2));
			result.append(", leashed ").append(only(heardDuring(() -> player().interactWith(other)), "entity_interact", "shear"));
			result.append(", let go ").append(only(heardDuring(() -> player().interactWith(other)), "entity_interact", "shear"));
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			return result.toString();
		});
		assertEqual("cow fed [entity_interact], sheep sheared [shear, entity_interact], pig touched [], leashed [], let go [entity_interact]", got,
			"interacting with mobs");
	}

	// ---------------------------------------------------------------- blocks changed (batch 4)

	private static final String[] CHANGES = {"block_change", "block_place", "block_destroy", "item_interact_finish", "eat", "fluid_place", "fluid_pickup",
		"block_attach"};

	/** The player holding this, as the server has it. */
	private static ItemStack holding(ItemStack stack) {
		player().inventory.setInventorySlotContents(player().inventory.currentItem, stack);
		return stack;
	}

	/**
	 * 26.3's `block_change` and its neighbours where a block changes in place, by the player unless said:
	 * - a hoe's tilling (a tool's `BlockTransformer`: `block_change`, not Forge's place event's `block_place`);
	 * - bone meal on wheat (`BoneMealItem.useOn`: `item_interact_finish`, at the player, nothing placed);
	 * - flint and steel's fire (still `block_place`);
	 * - farmland trodden (`FarmlandBlock.turnToBaseBlock`);
	 * - cake eaten to its last slice (`CakeBlock.eat`: `eat`, the last `block_destroy` too);
	 * - a flower planted in a pot (`FlowerPotBlock.useItemOn`);
	 * - an item frame given an apple, turned, and knocked empty (`ItemFrame.interact`, `hurtServer`);
	 * - a cauldron filled from a bucket, emptied into one, filled again, and a bottle taken (`CauldronInteractions`,
	 *   `LayeredCauldronBlock.lowerFillLevel`: its `block_change` first, the pickup's source none);
	 * - a leashed cow tied to a fence (`LeadItem.bindPlayerMobs`: `block_attach`).
	 */
	public void testBlockChanges() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			StringBuilder result = new StringBuilder();
			world().setBlock(NEAR_X - 3, Y - 1, NEAR_Z - 1, Blocks.dirt, 0, 2);
			ItemStack hoe = holding(new ItemStack(Items.iron_hoe));
			result.append("hoe ").append(only(heardDuring(() -> hoe.tryPlaceItemIntoWorld(player(), world(), NEAR_X - 3, Y - 1, NEAR_Z - 1, 1, 0.5F, 1.0F, 0.5F)), CHANGES));
			world().setBlock(NEAR_X - 1, Y - 1, NEAR_Z - 1, Blocks.farmland, 7, 2);
			world().setBlock(NEAR_X - 1, Y, NEAR_Z - 1, Blocks.wheat, 0, 2);
			ItemStack boneMeal = holding(new ItemStack(Items.dye, 4, 15));
			result.append("; bone meal ").append(only(heardDuring(() -> boneMeal.tryPlaceItemIntoWorld(player(), world(), NEAR_X - 1, Y, NEAR_Z - 1, 1, 0.5F, 0.5F,
				0.5F)), CHANGES));
			ItemStack flint = holding(new ItemStack(Items.flint_and_steel));
			result.append("; flint and steel ").append(only(heardDuring(() -> flint.tryPlaceItemIntoWorld(player(), world(), NEAR_X - 3, Y - 1, NEAR_Z + 1, 1, 0.5F,
				1.0F, 0.5F)), CHANGES));
			world().setBlock(NEAR_X - 3, Y, NEAR_Z + 1, Blocks.air, 0, 2);
			holding(null);
			world().setBlock(NEAR_X + 1, Y - 1, NEAR_Z - 1, Blocks.farmland, 7, 2);
			result.append("; trodden ").append(only(heardDuring(() -> Blocks.farmland.onFallenUpon(world(), NEAR_X + 1, Y - 1, NEAR_Z - 1, player(), 10.0F)), CHANGES));
			player().getFoodStats().addStats(-10, 0.0F);
			world().setBlock(NEAR_X + 1, Y, NEAR_Z + 1, Blocks.cake, 4, 2);
			result.append("; cake ").append(only(heardDuring(() -> use(Blocks.cake, NEAR_X + 1, Y, NEAR_Z + 1)), CHANGES));
			result.append(" ").append(only(heardDuring(() -> use(Blocks.cake, NEAR_X + 1, Y, NEAR_Z + 1)), CHANGES));
			world().setBlock(NEAR_X - 1, Y, NEAR_Z + 1, Blocks.flower_pot, 0, 2);
			holding(new ItemStack(Blocks.yellow_flower));
			result.append("; pot ").append(only(heardDuring(() -> use(Blocks.flower_pot, NEAR_X - 1, Y, NEAR_Z + 1)), CHANGES));
			world().setBlock(NEAR_X + 3, Y + 1, NEAR_Z + 2, Blocks.stone, 0, 2);
			EntityItemFrame frame = new EntityItemFrame(world(), NEAR_X + 3, Y + 1, NEAR_Z + 2, 2);
			world().spawnEntityInWorld(frame);
			holding(new ItemStack(Items.apple));
			result.append("; frame ").append(only(heardDuring(() -> frame.interactFirst(player())), CHANGES));
			result.append(" ").append(only(heardDuring(() -> frame.interactFirst(player())), CHANGES));
			result.append(" ").append(only(heardDuring(() -> frame.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F)), CHANGES));
			frame.setDead();
			world().setBlock(NEAR_X + 3, Y, NEAR_Z - 1, Blocks.cauldron, 0, 2);
			holding(new ItemStack(Items.water_bucket));
			result.append("; cauldron ").append(only(heardDuring(() -> use(Blocks.cauldron, NEAR_X + 3, Y, NEAR_Z - 1)), CHANGES));
			holding(new ItemStack(Items.bucket));
			result.append(" ").append(only(heardDuring(() -> use(Blocks.cauldron, NEAR_X + 3, Y, NEAR_Z - 1)), CHANGES));
			holding(new ItemStack(Items.water_bucket));
			result.append(" ").append(only(heardDuring(() -> use(Blocks.cauldron, NEAR_X + 3, Y, NEAR_Z - 1)), CHANGES));
			holding(new ItemStack(Items.glass_bottle));
			result.append(" ").append(only(heardDuring(() -> use(Blocks.cauldron, NEAR_X + 3, Y, NEAR_Z - 1)), CHANGES));
			world().setBlock(NEAR_X, Y, NEAR_Z + 2, Blocks.fence, 0, 2);
			EntityCow cow = new EntityCow(world());
			cow.setLocationAndAngles(NEAR_X + 0.5D, Y, NEAR_Z + 0.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(cow);
			cow.setLeashedToEntity(player(), true);
			ItemStack lead = holding(new ItemStack(Items.lead));
			result.append("; lead ").append(only(heardDuring(() -> lead.getItem().onItemUse(lead, player(), world(), NEAR_X, Y, NEAR_Z + 2, 1, 0.5F, 1.0F, 0.5F)),
				CHANGES));
			holding(null);
			return result.toString();
		});
		assertEqual("hoe [block_change]; bone meal [item_interact_finish]; flint and steel [block_place]; trodden [block_change]; cake [eat] [eat, "
			+ "block_destroy]; pot [block_change]; frame [block_change] [block_change] [block_change]; cauldron [fluid_place] [fluid_pickup] [fluid_place] "
			+ "[block_change, fluid_pickup]; lead [block_attach]", got, "blocks changed");
	}

	// ---------------------------------------------------------------- flaps, tilts, spawners, jukeboxes (batch 5)

	/**
	 * 26.3's `flap` (`Entity.processFlappingMovement`, moving with air under it): a bat not hanging, on a tick of 3
	 * (`Bat.isFlapping`), not on the next; a chicken falling with its wings out flaps each time its flown distance (x 0.6)
	 * passes the next flap, half a block on (`Chicken.onFlap`). The big dripleaf's tilt, McFly's: `block_change` for each
	 * tilt but the unstable one (26.3's `Tilt.causesVibration`), its reset too. A spawner's mob: `entity_place` at it, by
	 * it (`BaseSpawner.serverTick`). A record put in a jukebox and taken out: `block_change` for the record and for the
	 * jukebox going in (`JukeboxPlayable.tryInsertIntoJukebox`), one coming out.
	 */
	public void testFlapsTiltsSpawnersJukeboxes() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(NEAR_X + 0.5D, Y, NEAR_Z - 1.5D, 0.0F, 0.0F);
			StringBuilder result = new StringBuilder();
			EntityBat bat = new EntityBat(world());
			bat.setLocationAndAngles(NEAR_X - 2.5D, Y + 2.0D, NEAR_Z + 0.5D, 0.0F, 0.0F);
			bat.setIsBatHanging(false);
			world().spawnEntityInWorld(bat);
			bat.ticksExisted = 3;
			result.append("bat ").append(only(heardDuring(() -> bat.moveEntity(0.1D, 0.0D, 0.0D)), "flap"));
			bat.ticksExisted = 4;
			result.append(" ").append(only(heardDuring(() -> bat.moveEntity(0.1D, 0.0D, 0.0D)), "flap"));
			bat.setDead();
			EntityChicken chicken = new EntityChicken(world());
			chicken.setLocationAndAngles(NEAR_X + 2.5D, Y + 4.5D, NEAR_Z + 0.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(chicken);
			chicken.destPos = 1.0F;
			result.append("; chicken ").append(only(heardDuring(() -> {
				for (int i = 0; i < 8; i++) {
					chicken.moveEntity(0.0D, -0.5D, 0.0D);
				}
			}), "flap"));
			chicken.setDead();
			Block dripleaf = (Block) Block.blockRegistry.getObject("etfuturum:big_dripleaf");
			if (dripleaf != null && dripleaf != Blocks.air) {
				world().setBlock(NEAR_X - 3, Y, NEAR_Z - 1, dripleaf, 1 << 2, 2);
				result.append("; dripleaf");
				for (int i = 0; i < 3; i++) {
					result.append(" ").append(only(heardDuring(() -> dripleaf.updateTick(world(), NEAR_X - 3, Y, NEAR_Z - 1, world().rand)), "block_change"));
				}
				world().setBlock(NEAR_X - 3, Y, NEAR_Z - 1, Blocks.air, 0, 2);
			} else {
				result.append("; dripleaf (off)");
			}
			world().setBlock(NEAR_X + 3, Y, NEAR_Z - 1, Blocks.mob_spawner, 0, 2);
			TileEntityMobSpawner spawner = (TileEntityMobSpawner) world().getTileEntity(NEAR_X + 3, Y, NEAR_Z - 1);
			// a zombie pigman: 1.7.10's mobs from a spawner need the dark (`getBlockPathWeight`) but these
			spawner.func_145881_a().setEntityName("PigZombie");
			List<String> placed = new ArrayList<>();
			int spawned = 0;
			for (int tries = 0; tries < 5 && spawned == 0; tries++) {
				spawner.func_145881_a().spawnDelay = 0;
				placed.addAll(only(heardDuring(() -> spawner.func_145881_a().updateSpawner()), "entity_place"));
				spawned = world().getEntitiesWithinAABB(EntityPigZombie.class, AxisAlignedBB.getBoundingBox(NEAR_X - 6, Y - 3, NEAR_Z - 7, NEAR_X + 12, Y + 5, NEAR_Z + 5)).size();
			}
			for (Object pigman : world().getEntitiesWithinAABB(EntityPigZombie.class, AxisAlignedBB.getBoundingBox(NEAR_X - 6, Y - 3, NEAR_Z - 7, NEAR_X + 12, Y + 5, NEAR_Z + 5))) {
				((Entity) pigman).setDead();
			}
			world().setBlock(NEAR_X + 3, Y, NEAR_Z - 1, Blocks.air, 0, 2);
			result.append("; spawner ").append(spawned == 0 ? "(none spawned)" : placed.size() == spawned ? "an entity_place each" : placed.size() + " for " + spawned);
			world().setBlock(NEAR_X, Y, NEAR_Z + 2, Blocks.jukebox, 0, 2);
			ItemStack record = holding(new ItemStack(Items.record_13));
			result.append("; jukebox ").append(only(heardDuring(() -> record.getItem().onItemUse(record, player(), world(), NEAR_X, Y, NEAR_Z + 2, 1, 0.5F, 1.0F, 0.5F)),
				CHANGES));
			holding(null);
			result.append(" ").append(only(heardDuring(() -> use(Blocks.jukebox, NEAR_X, Y, NEAR_Z + 2)), CHANGES));
			return result.toString();
		});
		assertEqual("bat [flap] []; chicken [flap, flap, flap]; dripleaf [block_change] [block_change] [block_change]; spawner an entity_place each; "
			+ "jukebox [block_change, block_change] [block_change]", got, "flaps, tilts, spawners and jukeboxes");
	}
}
