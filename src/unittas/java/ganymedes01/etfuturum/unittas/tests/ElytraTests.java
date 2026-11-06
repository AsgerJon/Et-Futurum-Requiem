package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.elytra.GlidingPlayer;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEventListener;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The elytra as our own (SPEC-modern-movement.md §85): its classes McFly's under EFR's ids, its stored form kept, and
 * gliding as 26.3's (§17 to §22, ours already: these guard it through the rewrite). Glides start from x 400, z 400 on
 * the flat world, high up.
 */
@TasSuite("elytra")
public class ElytraTests extends TasTestCase {

	private static final int GROUND = 4;
	/** The gliding flag's bit in the shared flags (EFR's `elytraDataWatcherFlag`, 7 unless set). */
	private static final int FLAG = 7;
	private static final String[] EFR_CLASSES = {"ganymedes01.etfuturum.items.equipment.ItemArmorElytra",
		"ganymedes01.etfuturum.client.renderer.entity.elytra.LayerBetterElytra", "ganymedes01.etfuturum.client.renderer.entity.elytra.ModelElytra",
		"ganymedes01.etfuturum.client.sound.ElytraSound", "ganymedes01.etfuturum.entities.EntityBoostingFireworkRocket",
		"ganymedes01.etfuturum.network.StartElytraFlyingMessage", "ganymedes01.etfuturum.network.StartElytraFlyingHandler",
		"ganymedes01.etfuturum.api.elytra.IElytraPlayer", "ganymedes01.etfuturum.elytra.IElytraPlayer", "ganymedes01.etfuturum.elytra.FallFlying",
		"ganymedes01.etfuturum.elytra.ElytraAnimationState", "ganymedes01.etfuturum.mixins.early.backlytra.MixinEntityPlayer",
		"ganymedes01.etfuturum.mixins.early.backlytra.MixinEntityLivingBase", "ganymedes01.etfuturum.mixins.early.backlytra.MixinNetHandlerPlayServer",
		"ganymedes01.etfuturum.mixins.early.backlytra.MixinEntityTrackerEntry", "ganymedes01.etfuturum.mixins.early.backlytra.client.MixinEntityPlayerSP",
		"ganymedes01.etfuturum.mixins.early.backlytra.client.MixinRenderPlayer", "ganymedes01.etfuturum.mixins.early.backlytra.client.MixinModelBiped",
		"ganymedes01.etfuturum.mixins.early.backlytra.thaumcraft.MixinEventHandlerEntity", "ganymedes01.etfuturum.compat.CompatBaublesExpanded"};

	@Override
	protected void setUp() {
		if (Item.itemRegistry.getObject("etfuturum:elytra") == null) {
			skipTest("the elytra is off");
		}
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.clearInventory(null, -1);
		});
	}

	private static Item elytra() {
		return (Item) Item.itemRegistry.getObject("etfuturum:elytra");
	}

	private static boolean gliding() {
		return (player().getDataWatcher().getWatchableObjectByte(0) & 1 << FLAG) != 0;
	}

	/** In survival with an elytra (this damage) on, at (400, y, 400), looking this way; the client there too. */
	private void ready(int damage, double y, float pitch) {
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().setCurrentItemOrArmor(3, new ItemStack(elytra(), 1, damage));
			player().inventoryContainer.detectAndSendChanges();
			player().playerNetServerHandler.setPlayerLocation(400.5, y, 400.5, 0.0F, pitch);
		});
		waitUntilOnClient("arriving", () -> Math.abs(mc().thePlayer.posX - 400.5) < 0.01 && Math.abs(mc().thePlayer.posZ - 400.5) < 0.01
			&& mc().theWorld.getChunkFromBlockCoords(400, 400).isChunkLoaded, 100);
		ticks(2);
	}

	/** A tap of jump (26.3's takeoff: jump pressed while falling in the air), then a few ticks. */
	private void tapJump() {
		hold(mc().gameSettings.keyBindJump, 2);
		ticks(4);
	}

	// ---------------------------------------------------------------- ours, in EFR's stored form

	/** The item, the rocket and the glide McFly's, under EFR's ids; none of EFR's elytra classes left (compat gone too). */
	public void testElytraIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (Object thing : new Object[]{elytra(), net.minecraft.entity.EntityList.stringToClassMapping.get("etfuturum.boosting_firework_rocket")}) {
			String type = thing == null ? "nothing" : thing instanceof Class ? ((Class<?>) thing).getName() : thing.getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(type);
			}
		}
		for (String name : new String[]{"io.github.asgerjon.mcfly.elytra.FallFlying", "io.github.asgerjon.mcfly.elytra.ElytraItem"}) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes(name) == null) {
				wrong.add(name.substring(name.lastIndexOf('.') + 1) + " missing");
			}
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes(name) != null) {
				wrong.add(name.substring(name.lastIndexOf('.') + 1) + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "the elytra's classes");
	}

	/** The stored form: the wear as damage (432, unusable from 431), `FallFlying` in the player's NBT, the flag's bit. */
	public void testStoredFormKept() {
		assertEqual(432, elytra().getMaxDamage(), "its durability");
		String saved = onServer(() -> {
			NBTTagCompound tag = new NBTTagCompound();
			player().writeToNBT(tag);
			boolean before = tag.hasKey("FallFlying") && !tag.getBoolean("FallFlying");
			tag.setBoolean("FallFlying", true);
			player().readFromNBT(tag);
			boolean flag = gliding();
			tag.setBoolean("FallFlying", false);
			player().readFromNBT(tag);
			return before + " " + flag + " " + gliding();
		});
		assertEqual("true true false", saved, "FallFlying saved false, read back true into the flag's bit, then false");
	}

	// ---------------------------------------------------------------- gliding as 26.3's

	/**
	 * 26.3's `updateFallFlyingMovement` for one step, against our travel step on the server player (open air, so the
	 * move doesn't change the motion): three starts, level, climbing and diving.
	 */
	public void testGlideStepAs26_3() throws Exception {
		Class<?> fallFlying = Class.forName("io.github.asgerjon.mcfly.elytra.FallFlying");
		Method travel = fallFlying.getMethod("travel", EntityPlayer.class);
		double[][] starts = {{0.0, 0.0, -0.5, 0.0, 0.0}, {-30.0, 90.0, 0.8, 0.1, 0.0}, {45.0, -45.0, 0.3, -0.2, 0.3}};
		List<String> wrong = new ArrayList<>();
		for (double[] start : starts) {
			String got = onServer(() -> {
				EntityPlayer player = player();
				// put back after, in the same step: the client never sees these moves
				double[] was = {player.posX, player.posY, player.posZ, player.rotationPitch, player.rotationYaw};
				player.setPosition(400.5, 200.0, 400.5);
				player.rotationPitch = (float) start[0];
				player.rotationYaw = (float) start[1];
				player.motionX = start[2];
				player.motionY = start[3];
				player.motionZ = start[4];
				travel.invoke(null, player);
				String motion = String.format("%.6f %.6f %.6f", player.motionX, player.motionY, player.motionZ);
				player.setPosition(was[0], was[1], was[2]);
				player.rotationPitch = (float) was[3];
				player.rotationYaw = (float) was[4];
				player.motionX = player.motionY = player.motionZ = 0.0;
				return motion;
			});
			double[] want = glide26_3((float) start[0], (float) start[1], start[2], start[3], start[4]);
			String expected = String.format("%.6f %.6f %.6f", want[0], want[1], want[2]);
			if (!expected.equals(got)) {
				wrong.add("pitch " + start[0] + " yaw " + start[1] + ": " + got + " (26.3: " + expected + ")");
			}
		}
		assertEqual("[]", wrong.toString(), "a glide step");
	}

	/** 26.3's `LivingEntity.updateFallFlyingMovement`, transcribed (gravity 0.08, no Slow Falling). */
	private static double[] glide26_3(float pitch, float yaw, double x, double y, double z) {
		// 26.3's calculateViewVector, with its Mth.sin and Mth.cos (a table, indexed through a long)
		float f = pitch * (float) (Math.PI / 180.0);
		float g = -yaw * (float) (Math.PI / 180.0);
		double lookX = sin26(g) * cos26(f);
		double lookY = -sin26(f);
		double lookZ = cos26(g) * cos26(f);
		float lean = pitch * (float) (Math.PI / 180.0);
		double lookHor = Math.sqrt(lookX * lookX + lookZ * lookZ);
		double moveHor = Math.sqrt(x * x + z * z);
		double lift = Math.cos(lean) * Math.cos(lean);
		y += 0.08 * (-1.0 + lift * 0.75);
		if (y < 0.0 && lookHor > 0.0) {
			double convert = y * -0.1 * lift;
			x += lookX * convert / lookHor;
			y += convert;
			z += lookZ * convert / lookHor;
		}
		if (lean < 0.0F && lookHor > 0.0) {
			double convert = moveHor * -sin26(lean) * 0.04;
			x += -lookX * convert / lookHor;
			y += convert * 3.2;
			z += -lookZ * convert / lookHor;
		}
		if (lookHor > 0.0) {
			x += (lookX / lookHor * moveHor - x) * 0.1;
			z += (lookZ / lookHor * moveHor - z) * 0.1;
		}
		return new double[]{x * 0.99F, y * 0.98F, z * 0.99F};
	}

	/** 26.3's `Mth.sin`: the sine table's entry, the index through a long. */
	private static float sin26(double angle) {
		return (float) Math.sin(((long) (angle * 10430.378350470453) & 65535L) / 10430.378350470453);
	}

	/** 26.3's `Mth.cos`: the same table, a quarter turn on. */
	private static float cos26(double angle) {
		return (float) Math.sin(((long) (angle * 10430.378350470453 + 16384.0) & 65535L) / 10430.378350470453);
	}

	/**
	 * Takeoff as 26.3's: a jump tapped while falling with a usable elytra on starts the glide (the flag, a 0.6 by 0.6
	 * box); on the ground, or with a broken one (431), it doesn't.
	 */
	public void testTakeoff() {
		ready(0, GROUND, 0.0F);
		tapJump();
		assertEqual(false, onServer(ElytraTests::gliding), "gliding from the ground");
		ready(431, GROUND + 30, 0.0F);
		ticks(5);
		tapJump();
		assertEqual(false, onServer(ElytraTests::gliding), "gliding on a broken elytra");
		ready(0, GROUND + 30, 0.0F);
		ticks(5);
		tapJump();
		assertEqual("true 0.6 0.6", onServer(() -> gliding() + " " + player().width + " " + player().height), "gliding, its box");
	}

	/**
	 * Gliding wears it 1 every 20 ticks of flight, and landing stops the glide: a shallow glide (26.3 lets a steep dive
	 * build up fall distance, which a landing then hurts with), from 15 up, healed whole first.
	 */
	public void testWearAndLanding() {
		ready(0, GROUND + 15, 20.0F);
		onServer(() -> player().setHealth(player().getMaxHealth()));
		ticks(5);
		tapJump();
		assertEqual(true, onServer(ElytraTests::gliding), "not gliding");
		waitUntil("landing", () -> !onServer(ElytraTests::gliding), 600);
		assertTrue(onServer(() -> player().isEntityAlive() && player().getCurrentArmor(2) != null), "the landing killed the player, or the elytra is gone");
		int worn = onServer(() -> player().getCurrentArmor(2).getItemDamage());
		assertTrue(worn >= 1, "no wear after a glide of more than 20 ticks: " + worn);
	}

	/**
	 * 26.3's `elytra_glide` (SPEC-modern-movement.md §90.9, step H1): every 10th tick of a glide, at the player, as many
	 * as its glide ticks / 10. The listener follows the player (moved each tick, as 26.3 moves an entity's).
	 */
	public void testGlideVibrates() {
		ready(0, GROUND + 40, 0.0F);
		List<String> heard = new ArrayList<>();
		GameEventListener follower = new GameEventListener() {
			@Override
			public double[] position(World world) {
				return new double[]{player().posX, player().boundingBox.minY, player().posZ};
			}

			@Override
			public int radius() {
				return 16;
			}

			@Override
			public boolean handle(World world, GameEvent event, GameEvent.Context context, double x, double y, double z) {
				heard.add(event.name.replace("minecraft:", ""));
				return true;
			}
		};
		onServer(() -> GameEvents.register(world(), follower));
		String got;
		try {
			ticks(5);
			hold(mc().gameSettings.keyBindJump, 2);
			for (int i = 0; i < 36; i++) {
				ticks(1);
				onServer(() -> {
					GameEvents.unregister(world(), follower);
					GameEvents.register(world(), follower);
				});
			}
			got = onServer(() -> gliding() + ", " + heard.stream().filter("elytra_glide"::equals).count() + " heard, glide ticks / 10 = "
				+ ((GlidingPlayer) player()).etfu$getFallFlyingTicks() / 10);
		} finally {
			onServer(() -> GameEvents.unregister(world(), follower));
		}
		String ticksOver10 = got.substring(got.lastIndexOf('=') + 2);
		assertEqual("true, " + ticksOver10 + " heard, glide ticks / 10 = " + ticksOver10, got, "the glide's vibrations");
		assertTrue(!"0".equals(ticksOver10), "a glide under 10 ticks: " + got);
	}

	/** A firework used while gliding boosts it: a rocket attached, the speed up after 5 ticks. */
	public void testRocketBoost() {
		ready(0, GROUND + 60, 0.0F);
		ticks(5);
		tapJump();
		assertEqual(true, onServer(ElytraTests::gliding), "not gliding");
		double before = onServer(() -> Math.sqrt(player().motionX * player().motionX + player().motionZ * player().motionZ));
		onServer(() -> {
			player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.fireworks, 3));
			player().inventoryContainer.detectAndSendChanges();
		});
		ticks(2);
		onClient(() -> mc().playerController.sendUseItem(mc().thePlayer, mc().theWorld, mc().thePlayer.getHeldItem()));
		// 5 ticks: a rocket of one flight lives 10 to 21
		ticks(5);
		String found = onServer(() -> {
			int rockets = 0;
			for (Object entity : world().loadedEntityList) {
				if (entity instanceof net.minecraft.entity.item.EntityFireworkRocket && ((Entity) entity).getDistanceToEntity(player()) < 8) {
					rockets++;
				}
			}
			double after = Math.sqrt(player().motionX * player().motionX + player().motionZ * player().motionZ);
			return (rockets > 0) + " " + (after > before + 0.2);
		});
		assertEqual("true true", found, "a rocket attached, faster after it");
	}

	/**
	 * A rocket boosts whatever it was used by, entity id 0 too: a fresh world's player is often the run's first entity,
	 * id 0, and a rocket that stored the id with 0 for none was never attached, so it pushed nothing, on the client or
	 * the server (found 2026-11-05 by the showcase: 16 rockets spawned, none pushing). 26.3 keeps the id as an
	 * `OptionalInt`.
	 */
	public void testRocketAttachesToEntityZero() {
		String found = onServer(() -> {
			net.minecraft.entity.passive.EntityPig pig = new net.minecraft.entity.passive.EntityPig(world());
			pig.setEntityId(0);
			io.github.asgerjon.mcfly.elytra.BoostingRocket rocket = new io.github.asgerjon.mcfly.elytra.BoostingRocket(world(), new ItemStack(Items.fireworks), pig);
			// as the client gets it: a new one with the server's watched data
			io.github.asgerjon.mcfly.elytra.BoostingRocket copy = new io.github.asgerjon.mcfly.elytra.BoostingRocket(world());
			copy.getDataWatcher().updateWatchedObjectsFromList(rocket.getDataWatcher().getAllWatched());
			return rocket.isAttached() + " " + copy.isAttached();
		});
		assertEqual("true true", found, "a rocket used by entity 0 attached, and its copy as the client gets it");
	}

	/** A shift-click puts an elytra from the inventory into the chest slot (McFly's `shiftClickEquipsElytra`). */
	public void testShiftClickEquips() {
		String found = onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().inventory.setInventorySlotContents(9, new ItemStack(elytra()));
			// slot 9 of the inventory is the container's slot 9 (0 the result, 1 to 4 crafting, 5 to 8 armour)
			player().inventoryContainer.transferStackInSlot(player(), 9);
			ItemStack chest = player().getCurrentArmor(2);
			return chest == null ? "nothing" : Item.itemRegistry.getNameForObject(chest.getItem());
		});
		assertEqual("etfuturum:elytra", found, "the chest slot after the shift-click");
	}
}
