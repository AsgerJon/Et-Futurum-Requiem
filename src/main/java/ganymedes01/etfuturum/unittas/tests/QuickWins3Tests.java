package ganymedes01.etfuturum.unittas.tests;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import ganymedes01.etfuturum.entities.EntityArmourStand;
import ganymedes01.etfuturum.entities.EntityThrownTrident;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecartTNT;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.Explosion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quick wins batch 3 (SPEC-modern-movement.md §61): the TNT minecart's fuse, the trident stopping where it hits a mob,
 * item names in rarity colour, armour stands against mobs and wither skulls. The inventory figure is checked by the
 * 26.3 comparison's inventory views.
 */
public class QuickWins3Tests extends TasTestCase {

	@Override
	protected void tearDown() {
		onServer(() -> {
			world().getGameRules().setOrCreateGameRule("mobGriefing", "false");
		});
	}

	// ---------------------------------------------------------------- the TNT minecart

	/**
	 * Broken by fire it is lit, not blown up: still there with a fuse of 0 to 38, and then it goes off by itself. Far out,
	 * on a spot of its own: its crater mustn't be under another test's mob (or under a cart, which a fall sets off).
	 */
	public void testTntCartBurntIsLit() {
		EntityMinecartTNT cart = onServer(() -> {
			EntityMinecartTNT made = new EntityMinecartTNT(world(), 60.5D, 4.0D, 60.5D);
			world().spawnEntityInWorld(made);
			return made;
		});
		ticks(2);
		int fuse = onServer(() -> {
			cart.attackEntityFrom(DamageSource.inFire, 5.0F);
			NBTTagCompound tag = new NBTTagCompound();
			cart.writeToNBT(tag);
			return cart.isDead ? -100 : tag.getInteger("TNTFuse");
		});
		assertTrue(fuse != -100, "the cart blew up at once");
		assertTrue(fuse >= 0 && fuse <= 38, "fuse " + fuse);
		waitUntil("the cart going off", () -> cart.isDead, 45);
	}

	/** A burning arrow sets it off at once (it isn't broken by the arrow's small hit). On a spot of its own. */
	public void testBurningArrowSetsTntCartOff() {
		EntityMinecartTNT cart = onServer(() -> {
			EntityMinecartTNT made = new EntityMinecartTNT(world(), 60.5D, 4.0D, 80.5D);
			world().spawnEntityInWorld(made);
			return made;
		});
		ticks(2);
		boolean gone = onServer(() -> {
			EntityArrow arrow = new EntityArrow(world(), 60.5D, 5.0D, 78.0D);
			arrow.motionZ = 1.5D;
			arrow.setFire(10);
			cart.attackEntityFrom(DamageSource.causeArrowDamage(arrow, null), 2.0F);
			return cart.isDead;
		});
		assertTrue(gone, "the cart didn't go off at the burning arrow");
	}

	// ---------------------------------------------------------------- the trident

	/** Hitting a still villager, it is left where its flight met the villager's box widened by 0.3 (26.3), not short of it. */
	public void testTridentStopsWhereItHitsAMob() {
		double[] at = onServer(() -> {
			EntityVillager villager = new EntityVillager(world());
			villager.setPosition(0.5D, 4.0D, 6.5D);
			world().spawnEntityInWorld(villager);
			EntityThrownTrident thrown = new EntityThrownTrident(world());
			thrown.setItem(new ItemStack(trident()));
			thrown.setPosition(0.5D, 5.0D, 4.0D);
			thrown.motionZ = 2.5D;
			thrown.onUpdate();
			return new double[]{thrown.posZ, villager.boundingBox.minZ - 0.3D, villager.getHealth(), villager.getMaxHealth()};
		});
		assertTrue(at[2] < at[3], "the villager wasn't hit");
		assertAlmostEqual(at[1], at[0], 0.01D);
	}

	// ---------------------------------------------------------------- names and rarity

	/** On the screen: a trident's name above the hotbar in aqua (rare), stone's in white. */
	public void testHeldNameInRarityColour() {
		setHeld(new ItemStack(trident()));
		ticks(3);
		int[] trident = namePixels(screenshot("quick3.name_trident"));
		assertTrue(trident[0] > 10, "no aqua in the trident's name: " + trident[0] + " aqua, " + trident[1] + " white");
		setHeld(new ItemStack(Blocks.stone));
		ticks(3);
		int[] stone = namePixels(screenshot("quick3.name_stone"));
		assertEqual(0, stone[0], "aqua in stone's name");
		assertTrue(stone[1] > 10, "no white in stone's name");
	}

	/** The name as drawn: the rarity's colour, then italic when renamed. */
	public void testHeldNameRenamedIsItalic() {
		ItemStack sword = new ItemStack(Items.iron_sword);
		sword.setStackDisplayName("Bob");
		String drawn = heldName(sword);
		assertEqual(EnumChatFormatting.WHITE.toString() + EnumChatFormatting.ITALIC + "Bob", drawn);
	}

	/** Enchanting goes up a step: common to rare, rare to epic (26.3). */
	public void testEnchantedRarity() {
		ItemStack sword = new ItemStack(Items.iron_sword);
		sword.addEnchantment(Enchantment.sharpness, 1);
		ItemStack enchantedTrident = new ItemStack(trident());
		enchantedTrident.addEnchantment(Enchantment.unbreaking, 1);
		assertEqual(EnumRarity.common, new ItemStack(Items.iron_sword).getRarity());
		assertEqual(EnumRarity.rare, sword.getRarity());
		assertEqual(EnumRarity.rare, new ItemStack(trident()).getRarity());
		assertEqual(EnumRarity.epic, enchantedTrident.getRarity());
	}

	// ---------------------------------------------------------------- armour stands and wither skulls

	/** A wither skull hits as its own damage type, `witherSkull`, a projectile from the skull. */
	public void testWitherSkullDamageType() {
		Attacks attacks = new Attacks();
		MinecraftForge.EVENT_BUS.register(attacks);
		try {
			// far out: the skull's blast mustn't crater the ground other tests stand their mobs on
			EntityVillager villager = spawnVillager(80.5D, 60.5D);
			onServer(() -> fireSkull(80.5D, 60.5D));
			waitUntil("the villager hit by the skull", () -> !attacks.types.isEmpty(), 40);
			assertTrue(attacks.types.contains("witherSkull"), "the skull hit as " + attacks.types);
			assertTrue(attacks.projectile, "the skull's damage isn't a projectile's");
			assertTrue(villager != null, "no villager");
		} finally {
			MinecraftForge.EVENT_BUS.unregister(attacks);
		}
	}

	/**
	 * With mob griefing on, a wither skull breaks an armour stand at once, and the stand drops itself: a stand the skull's
	 * blast breaks drops only what it wore (26.3 and EFR alike), so the stand item shows it was the skull.
	 */
	public void testWitherSkullBreaksAStand() {
		if (!ModItems.WOODEN_ARMORSTAND.isEnabled()) {
			skipTest("armour stands are turned off (enableArmourStand)");
		}
		onServer(() -> world().getGameRules().setOrCreateGameRule("mobGriefing", "true"));
		// far out: with mob griefing on, the skull's blast craters the ground
		EntityArmourStand stand = spawnStand(80.5D, 80.5D);
		onServer(() -> fireSkull(80.5D, 80.5D));
		waitUntil("the stand broken", () -> stand.isDead, 40);
		ticks(2);
		assertTrue(onServer(() -> {
			for (Object loaded : world().loadedEntityList) {
				if (loaded instanceof EntityItem item && !item.isDead && item.getEntityItem().getItem() == ModItems.WOODEN_ARMORSTAND.get()) {
					return true;
				}
			}
			return false;
		}), "no armour stand dropped: the blast broke it, not the skull");
	}

	/** Mob griefing off: a skeleton's arrow and a creeper's blast leave a stand; a player's arrow still breaks it. */
	public void testMobGriefingOffSparesStands() {
		EntityArmourStand stand = spawnStand(0.5D, 8.5D);
		boolean[] alive = onServer(() -> {
			EntitySkeleton skeleton = new EntitySkeleton(world());
			skeleton.setPosition(0.5D, 4.0D, 12.5D);
			EntityArrow arrow = new EntityArrow(world(), 0.5D, 5.0D, 10.0D);
			stand.attackEntityFrom(DamageSource.causeArrowDamage(arrow, skeleton), 2.0F);
			boolean afterArrow = !stand.isDead;
			EntityCreeper creeper = new EntityCreeper(world());
			creeper.setPosition(0.5D, 4.0D, 10.5D);
			stand.attackEntityFrom(DamageSource.setExplosionSource(new Explosion(world(), creeper, 0.5D, 4.0D, 10.5D, 3.0F)), 10.0F);
			boolean afterBlast = !stand.isDead;
			stand.attackEntityFrom(DamageSource.causeArrowDamage(arrow, player()), 2.0F);
			return new boolean[]{afterArrow, afterBlast, !stand.isDead};
		});
		assertTrue(alive[0], "a skeleton's arrow broke it with mob griefing off");
		assertTrue(alive[1], "a creeper's blast broke it with mob griefing off");
		assertFalse(alive[2], "a player's arrow didn't break it");
	}

	// ---------------------------------------------------------------- helpers

	private static Item trident() {
		ItemIds.Entry entry = ItemIds.byId("minecraft:trident");
		assertTrue(entry != null && entry.item() != null, "EFR has no trident");
		return entry.item();
	}

	/** The name above the hotbar as EFR draws it (`client.ModernRarity.heldName`, read by name: it doesn't exist before its code). */
	private String heldName(ItemStack stack) {
		return onClient(() -> {
			try {
				return (String) Class.forName("ganymedes01.etfuturum.client.ModernRarity").getMethod("heldName", ItemStack.class).invoke(null, stack);
			} catch (ReflectiveOperationException e) {
				fail("EFR has no ModernRarity.heldName: " + e);
				return null;
			}
		});
	}

	/** Aqua (85, 255, 255) and white text pixels in the band where the held item's name is drawn, in this screenshot. */
	private int[] namePixels(String shot) {
		int[] layout = onClient(() -> {
			ScaledResolution res = new ScaledResolution(mc(), mc().displayWidth, mc().displayHeight);
			int y = res.getScaledHeight() - 59 + (mc().playerController.shouldDrawHUD() ? 0 : 14);
			return new int[]{res.getScaleFactor(), y, res.getScaledWidth()};
		});
		try {
			BufferedImage image = ImageIO.read(new File(resultsFolder(), shot));
			int scale = layout[0];
			int aqua = 0, white = 0;
			for (int y = (layout[1] - 1) * scale; y < (layout[1] + 9) * scale && y < image.getHeight(); y++) {
				for (int x = (layout[2] / 2 - 80) * scale; x < (layout[2] / 2 + 80) * scale; x++) {
					int rgb = image.getRGB(x, y) & 0xFFFFFF;
					if (rgb == 0x55FFFF) {
						aqua++;
					} else if (rgb == 0xFFFFFF) {
						white++;
					}
				}
			}
			return new int[]{aqua, white};
		} catch (java.io.IOException e) {
			fail("can't read " + shot + ": " + e);
			return null;
		}
	}

	private EntityVillager spawnVillager(double x, double z) {
		return onServer(() -> {
			EntityVillager villager = new EntityVillager(world());
			villager.setPosition(x, 4.0D, z);
			world().spawnEntityInWorld(villager);
			return villager;
		});
	}

	private EntityArmourStand spawnStand(double x, double z) {
		return onServer(() -> {
			EntityArmourStand stand = new EntityArmourStand(world());
			stand.setLocationAndAngles(x, 4.0D, z, 0.0F, 0.0F);
			world().spawnEntityInWorld(stand);
			return stand;
		});
	}

	/** A wither skull from a zombie (a stand-in shooter: a wither would be a boss fight), flying south at x z from 4 blocks short. On the server. */
	private static void fireSkull(double x, double z) {
		EntityZombie shooter = new EntityZombie(world());
		shooter.setPosition(x, 4.0D, z - 6.0D);
		EntityWitherSkull skull = new EntityWitherSkull(world(), shooter, 0.0D, 0.0D, 1.0D);
		skull.setPosition(x, 5.0D, z - 4.0D);
		// straight south (the constructor scatters its aim)
		skull.accelerationX = 0.0D;
		skull.accelerationY = 0.0D;
		skull.accelerationZ = 0.1D;
		skull.motionX = 0.0D;
		skull.motionY = 0.0D;
		skull.motionZ = 0.5D;
		world().spawnEntityInWorld(skull);
	}

	/** The damage types that reach living things while registered (server side). Public, for Forge's event bus. */
	public static class Attacks {
		final List<String> types = new CopyOnWriteArrayList<>();
		volatile boolean projectile;

		@SubscribeEvent
		public void onAttack(LivingAttackEvent event) {
			if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityVillager)) {
				return;
			}
			types.add(event.source.getDamageType());
			if ("witherSkull".equals(event.source.getDamageType()) && event.source.isProjectile()) {
				projectile = true;
			}
		}
	}
}
