package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.registry.McFlyItems;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.entities.EntityShulker;
import ganymedes01.etfuturum.entities.EntityShulkerBullet;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.ArrayList;

/**
 * Shulkers as 26.3's (SPEC-modern-movement.md §76.13, stage 8 and its rebuild): armour 20 once closed and none open, arrows doing nothing to
 * a closed one, its peek easing a twentieth a tick, staying where it's attached and leaving when its block goes, a
 * bullet's 4 damage and 10 seconds of levitation, a bullet hit splitting an open one off, and the shell dropped half
 * the time.
 */
public class ShulkerTests extends TasTestCase {

	@Override
	protected void setUp() {
		if (!ConfigEntities.enableShulker) {
			skipTest("shulkers are off");
		}
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			for (Object entity : new ArrayList<Object>(player().worldObj.loadedEntityList)) {
				if (entity instanceof EntityShulker || entity instanceof EntityShulkerBullet || entity instanceof EntityVillager || entity instanceof EntityArrow) {
					((Entity) entity).setDead();
				}
			}
		});
	}

	/** A shulker on the ground 6 east of the player (on the flat world's grass). */
	private EntityShulker shulker(int dx, int dz) {
		return onServer(() -> {
			World world = player().worldObj;
			int x = (int) Math.floor(player().posX) + dx;
			int y = (int) Math.floor(player().posY);
			int z = (int) Math.floor(player().posZ) + dz;
			EntityShulker made = new EntityShulker(world);
			made.setPosition(x + 0.5, y, z + 0.5);
			world.spawnEntityInWorld(made);
			return made;
		});
	}

	/**
	 * Its armour as 26.3's (the reference's `shulker.txt`, kept as `shulker_26_3.txt`): +20 is a modifier its closing
	 * puts on and its opening takes off, so a new one has none until it has closed once.
	 */
	public void testArmour() throws java.io.IOException {
		String expected = null;
		try (java.io.InputStream in = ShulkerTests.class.getResourceAsStream("/assets/etfuturum/unittas/shulker_26_3.txt")) {
			assertTrue(in != null, "no shulker_26_3.txt");
			java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
			for (String line; (line = reader.readLine()) != null; ) {
				if (line.startsWith("shulker armour ")) {
					expected = line.substring("shulker armour ".length());
				}
			}
		}
		EntityShulker shulker = shulker(6, 0);
		String found = onServer(() -> {
			int fresh = shulker.getTotalArmorValue();
			shulker.setRawPeekAmount(100);
			int opened = shulker.getTotalArmorValue();
			shulker.setRawPeekAmount(0);
			return "fresh " + fresh + ", opened " + opened + ", closed again " + shulker.getTotalArmorValue();
		});
		assertEqual(expected, found, "its armour new, opened, then closed (26.3's)");
	}

	public void testArrowsOnClosed() {
		EntityShulker shulker = shulker(6, 2);
		String found = onServer(() -> {
			EntityArrow arrow = new EntityArrow(player().worldObj, player(), 1.0F);
			boolean closed = shulker.attackEntityFrom(DamageSource.causeArrowDamage(arrow, player()), 4.0F);
			float afterClosed = shulker.getHealth();
			shulker.setRawPeekAmount(100);
			boolean open = shulker.attackEntityFrom(DamageSource.causeArrowDamage(arrow, player()), 4.0F);
			return closed + " " + (int) afterClosed + " " + open;
		});
		assertEqual("false 30 true", found, "an arrow on it closed (its health after), then open");
	}

	public void testPeekEases() {
		EntityShulker shulker = shulker(6, 4);
		onServer(() -> shulker.setRawPeekAmount(100));
		ticks(10);
		float peek = onServer(() -> shulker.getPeek());
		assertTrue(peek > 0.3F && peek < 0.7F, "its peek 10 ticks after opening (a twentieth a tick): " + peek);
	}

	public void testLeavesWhenItsBlockGoes() {
		String found = onServer(() -> {
			World world = player().worldObj;
			int x = (int) Math.floor(player().posX) + 10;
			int y = (int) Math.floor(player().posY) + 4;
			int z = (int) Math.floor(player().posZ);
			world.setBlock(x, y - 1, z, Blocks.stone);
			EntityShulker made = new EntityShulker(world);
			made.setPosition(x + 0.5, y, z + 0.5);
			world.spawnEntityInWorld(made);
			return x + " " + y + " " + z;
		});
		ticks(10);
		String stayed = onServer(() -> {
			for (Object entity : player().worldObj.loadedEntityList) {
				if (entity instanceof EntityShulker shulker && shulker.getDistance(Double.parseDouble(found.split(" ")[0]) + 0.5, Double.parseDouble(found.split(" ")[1]), Double.parseDouble(found.split(" ")[2]) + 0.5) < 0.1) {
					return shulker.attachFaceName();
				}
			}
			return "gone";
		});
		onServer(() -> {
			String[] at = found.split(" ");
			player().worldObj.setBlockToAir(Integer.parseInt(at[0]), Integer.parseInt(at[1]) - 1, Integer.parseInt(at[2]));
		});
		ticks(10);
		String after = onServer(() -> {
			String[] at = found.split(" ");
			for (Object entity : player().worldObj.loadedEntityList) {
				if (entity instanceof EntityShulker shulker && shulker.getDistance(Integer.parseInt(at[0]) + 0.5, Integer.parseInt(at[1]), Integer.parseInt(at[2]) + 0.5) < 16.0) {
					return shulker.getDistance(Integer.parseInt(at[0]) + 0.5, Integer.parseInt(at[1]), Integer.parseInt(at[2]) + 0.5) < 0.1 ? "still there" : "moved";
				}
			}
			return "gone";
		});
		assertEqual("down moved", stayed + " " + after, "on its block, then with the block gone");
	}

	public void testBulletHit() {
		String found = onServer(() -> {
			World world = player().worldObj;
			EntityVillager villager = new EntityVillager(world);
			villager.setLocationAndAngles(player().posX + 8, player().posY, player().posZ + 8, 0.0F, 0.0F);
			world.spawnEntityInWorld(villager);
			EntityShulkerBullet bullet = new EntityShulkerBullet(world, null, villager, 1);
			bullet.hit(villager);
			PotionEffect levitation = null;
			for (Object effect : villager.getActivePotionEffects()) {
				if (((PotionEffect) effect).getPotionID() != Potion.regeneration.id) {
					levitation = (PotionEffect) effect;
				}
			}
			return (int) villager.getHealth() + " " + (levitation == null ? "none" : levitation.getDuration());
		});
		assertEqual("16 200", found, "a villager hit by a bullet (its health, its levitation's ticks)");
	}

	/**
	 * An open shulker alone hit by a bullet teleports and splits (no other near: it never fails). Its 5 tries within 8
	 * need air with a face to hold and room to open (2 high on a floor), so it's in a block of stone layers every third
	 * level, two of air between (in the open, 26.3's mostly fail too); up to three bullets while it's above half health.
	 */
	public void testBulletSplitsOpenShulker() {
		String found = onServer(() -> {
			World world = player().worldObj;
			int cx = (int) Math.floor(player().posX) + 30;
			int cy = (int) Math.floor(player().posY) + 12;
			int cz = (int) Math.floor(player().posZ);
			for (int dx = -9; dx <= 9; dx++) {
				for (int dy = -9; dy <= 9; dy++) {
					for (int dz = -9; dz <= 9; dz++) {
						world.setBlock(cx + dx, cy + dy, cz + dz, Math.floorMod(dy + 1, 3) == 0 ? Blocks.stone : Blocks.air, 0, 2);
					}
				}
			}
			EntityShulker shulker = new EntityShulker(world);
			shulker.setPosition(cx + 0.5, cy, cz + 0.5);
			world.spawnEntityInWorld(shulker);
			int count = 1;
			for (int shot = 0; shot < 3 && count == 1; shot++) {
				shulker.setRawPeekAmount(100);
				shulker.hurtResistantTime = 0;
				new EntityShulkerBullet(world, null, shulker, 1).hit(shulker);
				count = 0;
				for (Object entity : world.loadedEntityList) {
					if (entity instanceof EntityShulker other && !other.isDead && other.getDistance(cx + 0.5, cy, cz + 0.5) < 20.0) {
						count++;
					}
				}
			}
			for (int dx = -9; dx <= 9; dx++) {
				for (int dy = -9; dy <= 9; dy++) {
					for (int dz = -9; dz <= 9; dz++) {
						world.setBlock(cx + dx, cy + dy, cz + dz, Blocks.air, 0, 2);
					}
				}
			}
			return count + "";
		});
		assertEqual("2", found, "shulkers after bullets hit an open one alone");
	}

	// ---------------------------------------------------------------- the rebuild from scratch (spec §76.13)

	/** The player's block, floored. */
	private static int[] here() {
		return new int[] {(int) Math.floor(player().posX), (int) Math.floor(player().posY), (int) Math.floor(player().posZ)};
	}

	/** A shulker made in the block at x y z (on the server's thread). */
	private static EntityShulker shulkerIn(int x, int y, int z) {
		return shulkerIn(x, y, z, false);
	}

	/**
	 * The same, without AI if asked (26.3's `NoAI`, given as saved data: no goals, so no peeking of its own, and no
	 * teleports; it still finds a face each tick).
	 */
	private static EntityShulker shulkerIn(int x, int y, int z, boolean noAi) {
		EntityShulker made = new EntityShulker(player().worldObj);
		made.setPosition(x + 0.5, y, z + 0.5);
		if (noAi) {
			net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
			made.writeToNBT(tag);
			tag.setBoolean("NoAI", true);
			made.readFromNBT(tag);
		}
		player().worldObj.spawnEntityInWorld(made);
		return made;
	}

	/** Saved as 26.3's: `AttachFace`, `Peek`, `Color`; nothing of EFR's 1.9 attachment place. */
	public void testSavedAs26_3() {
		String found = onServer(() -> {
			int[] at = here();
			EntityShulker shulker = shulkerIn(at[0] + 8, at[1], at[2] - 6);
			shulker.setRawPeekAmount(30);
			shulker.setColor((byte) 14);
			net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
			shulker.writeToNBT(tag);
			boolean efrs = tag.hasKey("APX") || tag.hasKey("APY") || tag.hasKey("APZ");
			return tag.getByte("AttachFace") + " " + tag.getByte("Peek") + " " + tag.getByte("Color") + " " + (efrs ? "with APX" : "no APX");
		});
		assertEqual("0 30 14 no APX", found, "a closed red shulker on the floor peeking 30, saved");
	}

	/** Put anywhere, it stands in its block's middle (26.3's setPos: floor x + 0.5, floor(y + 0.5), floor z + 0.5). */
	public void testSnapsToItsBlock() {
		String found = onServer(() -> {
			int[] at = here();
			int x = at[0] + 8;
			int z = at[2] - 3;
			EntityShulker shulker = shulkerIn(x, at[1], z);
			shulker.setPosition(x + 0.3, at[1] + 0.7, z + 0.9);
			return String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", shulker.posX - x, shulker.posY - at[1], shulker.posZ - z);
		});
		assertEqual("0.50 1.00 0.50", found, "put at 0.3 0.7 0.9 into its block");
	}

	/**
	 * Its box: the block's cube, stretched out from its face as it opens (fully open, one more block). Without AI, so
	 * its own peeking doesn't change the opening.
	 */
	public void testBoxGrowsWithPeek() {
		int[] at = onServer(ShulkerTests::here);
		int fx = at[0] + 8;
		int fz = at[2];
		int wx = at[0] + 8;
		int wy = at[1] + 3;
		int wz = at[2] + 4;
		EntityShulker[] made = onServer(() -> {
			player().worldObj.setBlock(wx, wy, wz - 1, Blocks.stone);
			return new EntityShulker[] {shulkerIn(fx, at[1], fz, true), shulkerIn(wx, wy, wz, true)};
		});
		ticks(3);
		String closed = onServer(() -> String.format(java.util.Locale.ROOT, "%s y %.2f to %.2f, %s z %.2f to %.2f", made[0].attachFaceName(), made[0].boundingBox.minY - at[1],
			made[0].boundingBox.maxY - at[1], made[1].attachFaceName(), made[1].boundingBox.minZ - wz, made[1].boundingBox.maxZ - wz));
		onServer(() -> {
			made[0].setRawPeekAmount(100);
			made[1].setRawPeekAmount(100);
		});
		ticks(25);
		String open = onServer(() -> String.format(java.util.Locale.ROOT, "y %.2f to %.2f, z %.2f to %.2f", made[0].boundingBox.minY - at[1], made[0].boundingBox.maxY - at[1],
			made[1].boundingBox.minZ - wz, made[1].boundingBox.maxZ - wz));
		onServer(() -> player().worldObj.setBlockToAir(wx, wy, wz - 1));
		assertEqual("down y 0.00 to 1.00, north z 0.00 to 1.00 / open y 0.00 to 2.00, z 0.00 to 2.00", closed + " / open " + open,
			"on the floor and on a wall to its north, closed and fully open");
	}

	/** It never moves by itself: no gravity under a ceiling, and motion given to it does nothing. */
	public void testNoMotionNoGravity() {
		int[] at = onServer(ShulkerTests::here);
		int x = at[0] + 8;
		int y = at[1] + 3;
		int z = at[2] + 8;
		EntityShulker shulker = onServer(() -> {
			player().worldObj.setBlock(x, y + 1, z, Blocks.stone);
			return shulkerIn(x, y, z);
		});
		ticks(3);
		onServer(() -> {
			shulker.motionX = 0.3;
			shulker.motionY = -0.5;
			shulker.addVelocity(0.2, 0.0, 0.2);
		});
		ticks(10);
		String found = onServer(() -> String.format(java.util.Locale.ROOT, "%s %.2f %.2f %.2f", shulker.attachFaceName(), shulker.posX - x, shulker.posY - y, shulker.posZ - z));
		onServer(() -> player().worldObj.setBlockToAir(x, y + 1, z));
		assertEqual("up 0.50 0.00 0.50", found, "under a ceiling, pushed and given motion");
	}

	/** Its floor gone, it holds on to the next face it can (26.3's order: down, up, north, south, west, east). */
	public void testFindsTheNextFace() {
		int[] at = onServer(ShulkerTests::here);
		int x = at[0] - 8;
		int z = at[2] + 3;
		EntityShulker shulker = onServer(() -> {
			player().worldObj.setBlock(x, at[1], z - 1, Blocks.stone);
			return shulkerIn(x, at[1], z);
		});
		ticks(3);
		onServer(() -> player().worldObj.setBlockToAir(x, at[1] - 1, z));
		ticks(5);
		String found = onServer(() -> shulker.attachFaceName() + (shulker.getDistance(x + 0.5, at[1], z + 0.5) < 0.1 ? " still there" : " moved"));
		onServer(() -> {
			player().worldObj.setBlockToAir(x, at[1], z - 1);
			player().worldObj.setBlock(x, at[1] - 1, z, Blocks.grass);
		});
		assertEqual("north still there", found, "the floor under it gone, a block to its north");
	}

	/** Where it couldn't open (a block on top of it), it leaves: no face holds it with room to open, so it teleports. */
	public void testLeavesWhereItCantOpen() {
		int[] at = onServer(ShulkerTests::here);
		int x = at[0] - 6;
		int z = at[2] - 6;
		EntityShulker shulker = onServer(() -> {
			player().worldObj.setBlock(x, at[1] + 1, z, Blocks.stone);
			return shulkerIn(x, at[1], z);
		});
		ticks(60);
		boolean moved = onServer(() -> shulker.getDistance(x + 0.5, at[1], z + 0.5) > 0.1);
		onServer(() -> player().worldObj.setBlockToAir(x, at[1] + 1, z));
		assertTrue(moved, "a shulker with a block on top of it, 60 ticks on: still in its place");
	}

	/** The client's portal particles (1.7.10's `EntityPortalFX`), counted on the client's thread. */
	private static int portalParticles() throws IllegalAccessException {
		java.lang.reflect.Field layers = cpw.mods.fml.relauncher.ReflectionHelper.findField(net.minecraft.client.particle.EffectRenderer.class, "fxLayers", "field_78876_b");
		int count = 0;
		for (java.util.List<?> layer : (java.util.List<?>[]) layers.get(mc().effectRenderer)) {
			for (Object fx : layer) {
				if (fx instanceof net.minecraft.client.particle.EntityPortalFX) {
					count++;
				}
			}
		}
		return count;
	}

	/** Its teleport leaves a trail the client draws: portal particles along the way (26.3's level event 2016). */
	public void testTeleportTrail() {
		int[] at = onServer(ShulkerTests::here);
		int x = at[0] + 4;
		int z = at[2] + 4;
		int before = onClient(ShulkerTests::portalParticles);
		EntityShulker shulker = onServer(() -> {
			player().worldObj.setBlock(x, at[1] + 1, z, Blocks.stone);
			return shulkerIn(x, at[1], z);
		});
		waitUntil("the shulker to teleport", () -> shulker.getDistance(x + 0.5, at[1], z + 0.5) > 0.1, 100);
		onServer(() -> player().worldObj.setBlockToAir(x, at[1] + 1, z));
		waitUntilOnClient("portal particles from the teleport", () -> portalParticles() > before, 20);
	}

	/** No dyeing: 26.3's Java shulker isn't dyed by a player (EFR's was, as Bedrock's). */
	public void testNoDyeing() {
		EntityShulker shulker = shulker(-6, 2);
		setHeld(new ItemStack(net.minecraft.init.Items.dye, 4, 1));
		onServer(() -> shulker.interactFirst(player()));
		byte color = onServer(shulker::getColor);
		setHeld(null);
		assertEqual(16, (int) color, "its colour after a right click with red dye (16: none)");
	}

	/** A golem, not a monster: it stays in Peaceful; its bullet doesn't. */
	public void testPeaceful() {
		EntityShulker shulker = shulker(-6, 4);
		try {
			difficulty(net.minecraft.world.EnumDifficulty.PEACEFUL);
			EntityShulkerBullet bullet = onServer(() -> {
				EntityShulkerBullet made = new EntityShulkerBullet(player().worldObj, shulker, player(), 1);
				player().worldObj.spawnEntityInWorld(made);
				return made;
			});
			ticks(5);
			assertEqual("shulker alive, bullet gone", onServer(() -> (shulker.isDead ? "shulker gone" : "shulker alive") + ", " + (bullet.isDead ? "bullet gone" : "bullet still there")),
				"5 ticks in Peaceful");
		} finally {
			difficulty(net.minecraft.world.EnumDifficulty.EASY);
		}
	}

	/** A bullet's first leg towards a target 10 east, level with it: along x only, no gravity while it has a target. */
	public void testBulletFirstLeg() {
		int[] at = onServer(ShulkerTests::here);
		EntityShulkerBullet bullet = onServer(() -> {
			World world = player().worldObj;
			EntityShulker shooter = shulkerIn(at[0] + 3, at[1], at[2] - 10);
			EntityVillager villager = new EntityVillager(world);
			villager.setLocationAndAngles(at[0] + 13.5, at[1], at[2] - 9.5, 0.0F, 0.0F);
			villager.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed).setBaseValue(0.0);
			world.spawnEntityInWorld(villager);
			EntityShulkerBullet made = new EntityShulkerBullet(world, shooter, villager, 1);
			world.spawnEntityInWorld(made);
			return made;
		});
		double startZ = onServer(() -> bullet.posZ);
		ticks(8);
		String found = onServer(() -> String.format(java.util.Locale.ROOT, "y %.3f z %.3f, x moving %s, z moved %.3f", bullet.motionY, bullet.motionZ, bullet.motionX > 0.05,
			bullet.posZ - startZ));
		assertEqual("y 0.000 z 0.000, x moving true, z moved 0.000", found, "8 ticks into its first leg");
	}

	/** Hit itself, a bullet is gone. */
	public void testBulletHitGoes() {
		String found = onServer(() -> {
			EntityShulker shooter = shulkerIn(here()[0] - 3, here()[1], here()[2] + 10);
			EntityShulkerBullet bullet = new EntityShulkerBullet(player().worldObj, shooter, player(), 1);
			player().worldObj.spawnEntityInWorld(bullet);
			boolean hurt = bullet.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F);
			return hurt + " " + bullet.isDead;
		});
		assertEqual("true true", found, "a bullet hit by the player (hurt, gone)");
	}

	public void testShellHalfTheTime() {
		if (!McFlyItems.SHULKER_SHELL.isEnabled()) {
			skipTest("shulker shells are off");
		}
		EntityShulker shulker = shulker(-6, -6);
		int shells = onServer(() -> {
			int dropped = 0;
			for (int i = 0; i < 2000; i++) {
				dropped += shulker.rollShells(0);
			}
			return dropped;
		});
		assertTrue(shells > 900 && shells < 1100, "shells from 2000 deaths (26.3: half): " + shells);
	}
}
