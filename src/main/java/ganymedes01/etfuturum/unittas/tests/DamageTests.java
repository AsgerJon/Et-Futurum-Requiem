package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.combat.AttackCooldown;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.entities.EntityTippedArrow;
import io.github.asgerjon.mcfly.mixins.early.combat.AccessorEntity;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.Explosion;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * The combat update's damage rules (spec §46.10), by exact numbers: modern's armour formula with toughness (players
 * through Forge's step, mobs through 1.7.10's own), each protection against its own damage (by hand, and on a real
 * fall, arrow and explosion), burning and an explosion's push by the levels worn, Sharpness, Strength, Weakness,
 * Haste and Mining Fatigue, modern's knockback, the armour tooltips. Hits by hand are made on the server with cactus
 * damage (it counts armour, isn't scaled by difficulty and pushes nobody) unless said.
 */
public class DamageTests extends TasTestCase {

	@Override
	protected void setUp() {
		if (!McFlyConfig.modernCombat) {
			skipTest("the combat update is turned off (modernCombat)");
		}
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
	}

	/** Puts on a set, helmet to boots (null for none), each as a new stack. */
	private void wear(Item helmet, Item chestplate, Item leggings, Item boots) {
		wear(stack(helmet), stack(chestplate), stack(leggings), stack(boots));
	}

	private void wear(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
		onServer(() -> {
			player().inventory.armorInventory[3] = helmet;
			player().inventory.armorInventory[2] = chestplate;
			player().inventory.armorInventory[1] = leggings;
			player().inventory.armorInventory[0] = boots;
			player().inventoryContainer.detectAndSendChanges();
		});
	}

	private static ItemStack stack(Item item) {
		return item == null ? null : new ItemStack(item);
	}

	private static ItemStack enchanted(Item item, Enchantment enchantment, int level) {
		ItemStack stack = new ItemStack(item);
		stack.addEnchantment(enchantment, level);
		return stack;
	}

	/** What the hit took off the player, from full health, outside the hurt timer. */
	private float hitPlayer(DamageSource source, float amount) {
		return onServer(() -> {
			player().setHealth(player().getMaxHealth());
			player().hurtResistantTime = 0;
			player().attackEntityFrom(source, amount);
			return player().getMaxHealth() - player().getHealth();
		});
	}

	// ---------------------------------------------------------------- armour

	/**
	 * A 10 on a player: no armour 10; full iron (15 points) 15 − 10 ÷ 2 = 10 count, 6 through; full diamond (20,
	 * toughness 8) 20 − 10 ÷ 4 = 17.5 count, 3 through; each diamond piece loses max(1, 10 ÷ 4) = 2.
	 */
	public void testArmourOnPlayers() {
		assertAlmostEqual(10.0, hitPlayer(DamageSource.cactus, 10.0F), 1.0E-4);
		wear(Items.iron_helmet, Items.iron_chestplate, Items.iron_leggings, Items.iron_boots);
		assertAlmostEqual(6.0, hitPlayer(DamageSource.cactus, 10.0F), 1.0E-4);
		wear(Items.diamond_helmet, Items.diamond_chestplate, Items.diamond_leggings, Items.diamond_boots);
		assertAlmostEqual(3.0, hitPlayer(DamageSource.cactus, 10.0F), 1.0E-4);
		for (int slot = 0; slot < 4; slot++) {
			int piece = slot;
			assertEqual(2, onServer(() -> player().inventory.armorInventory[piece].getItemDamage()), "a diamond piece's wear from a 10");
		}
	}

	/**
	 * A 30 on an iron golem (100 health, no armour of its own; mobs go through 1.7.10's own armour step): full diamond
	 * 20 − 30 ÷ 4 = 12.5 count, 15 through; full iron 15 − 30 ÷ 2 is under a fifth of 15, so 3 count, 26.4 through.
	 */
	public void testArmourOnMobs() {
		EntityIronGolem golem = spawn(EntityIronGolem::new, 0.5, 4.0, 4.5);
		try {
			assertAlmostEqual(15.0, hitGolem(golem, Items.diamond_helmet, Items.diamond_chestplate, Items.diamond_leggings, Items.diamond_boots), 1.0E-4);
			assertAlmostEqual(26.4, hitGolem(golem, Items.iron_helmet, Items.iron_chestplate, Items.iron_leggings, Items.iron_boots), 1.0E-4);
		} finally {
			onServer(golem::setDead);
		}
	}

	private float hitGolem(EntityIronGolem golem, Item helmet, Item chestplate, Item leggings, Item boots) {
		return onServer(() -> {
			golem.setCurrentItemOrArmor(4, stack(helmet));
			golem.setCurrentItemOrArmor(3, stack(chestplate));
			golem.setCurrentItemOrArmor(2, stack(leggings));
			golem.setCurrentItemOrArmor(1, stack(boots));
			golem.setHealth(golem.getMaxHealth());
			golem.hurtResistantTime = 0;
			golem.attackEntityFrom(DamageSource.cactus, 30.0F);
			return golem.getMaxHealth() - golem.getHealth();
		});
	}

	/** Full netherite: toughness 12, and no wear from fire (diamond wears). */
	public void testNetherite() {
		if (!ModItems.NETHERITE_HELMET.isEnabled()) {
			skipTest("netherite is turned off");
		}
		wear(ModItems.NETHERITE_HELMET.get(), ModItems.NETHERITE_CHESTPLATE.get(), ModItems.NETHERITE_LEGGINGS.get(), ModItems.NETHERITE_BOOTS.get());
		float points = onServer(() -> (float) player().getTotalArmorValue());
		float counted = Math.min(Math.max(points - 10.0F / (2.0F + 12.0F / 4.0F), points * 0.2F), 20.0F);
		assertAlmostEqual(10.0F * (1.0F - counted / 25.0F), hitPlayer(DamageSource.cactus, 10.0F), 1.0E-4);
		hitPlayer(DamageSource.inFire, 4.0F);
		for (int slot = 0; slot < 4; slot++) {
			int piece = slot;
			assertEqual(2, onServer(() -> player().inventory.armorInventory[piece].getItemDamage()), "netherite wore from fire");
		}
	}

	/**
	 * A totem saves only from a hit that kills after armour (a netherite-axe zombie's hit used one up in full
	 * netherite: it was judged before armour). Full diamond and a 22.5: 20 − 22.5 ÷ 4 = 14.375 points count, 9.5625
	 * through, the totem stays; no armour and a 30: it's used, leaving 1 health (5 %) and Regeneration.
	 */
	public void testTotemOnlyForAFatalHit() {
		if (!ModItems.TOTEM_OF_UNDYING.isEnabled()) {
			skipTest("the totem of undying is turned off");
		}
		setHeld(new ItemStack(ModItems.TOTEM_OF_UNDYING.get()));
		wear(Items.diamond_helmet, Items.diamond_chestplate, Items.diamond_leggings, Items.diamond_boots);
		assertAlmostEqual(9.5625, hitPlayer(DamageSource.cactus, 22.5F), 1.0E-4);
		assertIsNotNone(onServer(() -> player().getHeldItem()), "the totem was used on a hit the armour made harmless");
		wear((Item) null, null, null, null);
		float left = onServer(() -> {
			player().setHealth(player().getMaxHealth());
			player().hurtResistantTime = 0;
			player().attackEntityFrom(DamageSource.cactus, 30.0F);
			return player().getHealth();
		});
		assertAlmostEqual(1.0, left, 1.0E-4);
		assertIsNone(onServer(() -> player().getHeldItem()));
		assertTrue(onServer(() -> player().isPotionActive(Potion.regeneration)), "no Regeneration after the totem");
	}

	/**
	 * A tipped arrow's Harming comes after its hit, a totem the hit used included, as 26.3's: at 2 health with two
	 * totems, a Harming II arrow's 3 uses the first (1 health and 8 golden ones left); Harming's 12 then adds 12 − 3 = 9
	 * inside the hurt cooldown the arrow started, more than that, so the second goes too (MC-206307). Harming first,
	 * as EFR had it, left the second.
	 */
	public void testHarmingArrowAfterATotem() {
		boolean before = McFlyConfig.arrowHarmingAfterTotemFix;
		try {
			McFlyConfig.arrowHarmingAfterTotemFix = false;
			harmingArrowAt2HealthWithTwoTotems();
			assertIsNone(onServer(() -> player().getHeldItem()));
			assertTrue(onServer(() -> player().getHealth() > 0.0F), "dead: the second totem didn't save the player");
		} finally {
			McFlyConfig.arrowHarmingAfterTotemFix = before;
		}
	}

	/**
	 * With arrowHarmingAfterTotemFix (on by default; Carpet Fixes' remedy): no Harming follows the hit that used the
	 * first totem, so the second stays, and the first one's 8 golden hearts are untouched.
	 */
	public void testHarmingArrowAfterATotemFixed() {
		boolean before = McFlyConfig.arrowHarmingAfterTotemFix;
		try {
			McFlyConfig.arrowHarmingAfterTotemFix = true;
			harmingArrowAt2HealthWithTwoTotems();
			assertEqual(1, onServer(() -> player().getHeldItem() == null ? 0 : player().getHeldItem().stackSize), "totems left");
			// untouched by Harming: the golden hearts all there (health is 1 and up, Regeneration II heals at once)
			assertAlmostEqual(8.0, onServer(() -> player().getAbsorptionAmount()), 1.0E-4);
			assertTrue(onServer(() -> player().getHealth() > 0.0F), "dead");
		} finally {
			McFlyConfig.arrowHarmingAfterTotemFix = before;
		}
	}

	/** Two totems in hand, 2 health, and a Harming II arrow of 3 from the front; waits for the hit and its aftermath. */
	private void harmingArrowAt2HealthWithTwoTotems() {
		if (!ModItems.TIPPED_ARROW.isEnabled() || !ModItems.TOTEM_OF_UNDYING.isEnabled()) {
			skipTest("tipped arrows or the totem of undying are turned off");
		}
		setHeld(new ItemStack(ModItems.TOTEM_OF_UNDYING.get(), 2));
		onServer(() -> {
			player().setHealth(2.0F);
			player().hurtResistantTime = 0;
			ItemStack tipped = new ItemStack(ModItems.TIPPED_ARROW.get());
			NBTTagCompound tag = new NBTTagCompound();
			tag.setTag("Potion", new PotionEffect(Potion.harm.id, 1, 1).writeCustomPotionEffectToNBT(new NBTTagCompound()));
			tipped.setTagCompound(tag);
			EntityTippedArrow arrow = new EntityTippedArrow(world());
			arrow.setArrow(tipped);
			arrow.setPosition(0.5, 5.0, 4.5);
			arrow.setThrowableHeading(0.0, 0.0, -1.0, 1.5F, 0.0F);
			world().spawnEntityInWorld(arrow);
		});
		waitUntil("the arrow's hit", () -> player().getHeldItem() == null || player().getHeldItem().stackSize < 2, 40);
		ticks(3);
	}

	// ---------------------------------------------------------------- protection

	/**
	 * A fall of 10 (no armour counts for falls): Protection IV on four pieces is 16 points, 36 % through; with
	 * Feather Falling IV on the boots too, 28 points, counted as 20: 20 % through. No random halving.
	 */
	public void testProtection() {
		ItemStack boots = enchanted(Items.leather_boots, Enchantment.protection, 4);
		wear(enchanted(Items.leather_helmet, Enchantment.protection, 4), enchanted(Items.leather_chestplate, Enchantment.protection, 4),
			enchanted(Items.leather_leggings, Enchantment.protection, 4), boots);
		for (int i = 0; i < 5; i++) {
			assertAlmostEqual(3.6, hitPlayer(DamageSource.fall, 10.0F), 1.0E-4);
		}
		onServer(() -> player().inventory.armorInventory[0].addEnchantment(Enchantment.featherFalling, 4));
		assertAlmostEqual(2.0, hitPlayer(DamageSource.fall, 10.0F), 1.0E-4);
	}

	/**
	 * Each protection counts against its own damage only, at its own points a level: Projectile Protection IV 8 points
	 * against arrows, Fire Protection IV 8 against fire, Blast Protection IV 8 against explosions, Feather Falling IV
	 * 12 against falls (each 10 → 6.8, falls 5.2), and nothing against cactus; Protection IV 4 against anything (8.4)
	 * but the void. Worn on a pumpkin, which has no armour points, so only the enchantment counts. On Normal, where an
	 * explosion isn't scaled.
	 */
	public void testProtectionByDamageType() {
		difficulty(EnumDifficulty.NORMAL);
		// an arrow for the damage source, not in the world
		EntityArrow arrow = onServer(() -> new EntityArrow(world()));
		Object[][] cases = {
			{Enchantment.projectileProtection, DamageSource.causeArrowDamage(arrow, arrow), 6.8},
			{Enchantment.fireProtection, DamageSource.inFire, 6.8},
			{Enchantment.blastProtection, DamageSource.setExplosionSource(null), 6.8},
			{Enchantment.featherFalling, DamageSource.fall, 5.2},
			{Enchantment.protection, DamageSource.cactus, 8.4},
			{Enchantment.protection, DamageSource.fall, 8.4},
		};
		for (Object[] row : cases) {
			Enchantment enchantment = (Enchantment) row[0];
			DamageSource source = (DamageSource) row[1];
			wear(enchanted(Item.getItemFromBlock(Blocks.pumpkin), enchantment, 4), null, null, null);
			String what = enchantment.getTranslatedName(4) + " against " + source.getDamageType();
			float taken = hitPlayer(source, 10.0F);
			assertTrue(Math.abs(taken - (double) row[2]) < 1.0E-4, what + " let " + taken + " of 10 through, not " + row[2]);
			if (enchantment != Enchantment.protection) {
				float cactus = hitPlayer(DamageSource.cactus, 10.0F);
				assertTrue(Math.abs(cactus - 10.0F) < 1.0E-4, enchantment.getTranslatedName(4) + " protected against cactus: " + cactus + " of 10");
			}
		}
		wear(enchanted(Item.getItemFromBlock(Blocks.pumpkin), Enchantment.protection, 4), null, null, null);
		assertAlmostEqual(10.0, hitPlayer(DamageSource.outOfWorld, 10.0F), 1.0E-4);
	}

	/**
	 * A landing after a fall of 10.5 does 8 (⌈10.5 − 3⌉); with Feather Falling IV boots (12 points) 8 × 13 ÷ 25 = 4.16.
	 * Through 1.7.10's own landing (handleFalling, fall, the fall damage), from a set fall distance: a fall from a
	 * teleport loses its first part to the server, which counts only once it has the client's new position.
	 */
	public void testFeatherFallingOnALanding() {
		assertAlmostEqual(8.0, land(), 1.0E-4);
		wear(null, null, null, enchanted(Items.leather_boots, Enchantment.featherFalling, 4));
		assertAlmostEqual(4.16, land(), 1.0E-4);
	}

	/** Lands the player from a fall of 10.5, at full health; what the landing took. */
	private float land() {
		return onServer(() -> {
			player().setHealth(player().getMaxHealth());
			player().hurtResistantTime = 0;
			player().fallDistance = 10.5F;
			player().handleFalling(0.0D, true);
			return player().getMaxHealth() - player().getHealth();
		});
	}

	/** A real arrow does 3 (⌈speed × 2⌉); with Projectile Protection IV (8 points) 3 × 17 ÷ 25 = 2.04. */
	public void testProjectileProtectionOnARealArrow() {
		assertAlmostEqual(3.0, arrowHit(), 1.0E-4);
		wear(enchanted(Item.getItemFromBlock(Blocks.pumpkin), Enchantment.projectileProtection, 4), null, null, null);
		assertAlmostEqual(2.04, arrowHit(), 1.0E-4);
	}

	/** An arrow from the front at chest height, at full health; what it took. */
	private float arrowHit() {
		onServer(() -> {
			player().setHealth(player().getMaxHealth());
			player().hurtResistantTime = 0;
			player().setArrowCountInEntity(0);
			EntityArrow arrow = new EntityArrow(world());
			arrow.setPosition(0.5, 5.0, 4.5);
			arrow.setThrowableHeading(0.0, 0.0, -1.0, 1.5F, 0.0F);
			world().spawnEntityInWorld(arrow);
		});
		waitUntil("the arrow's hit", () -> player().getHealth() < player().getMaxHealth(), 40);
		return onServer(() -> player().getMaxHealth() - player().getHealth());
	}

	/**
	 * A real explosion: Blast Protection IV (one piece) cuts the push the player gets by 60 %, the push their client is
	 * sent included (1.7.10 sent the uncut one).
	 */
	public void testBlastProtectionOnARealExplosion() {
		// both explosions in one server step, so the first push can't move the player before the second
		String[] before = new String[1];
		double[] pushes = onServer(() -> {
			before[0] = blastSurroundings();
			double bare = explosionPush();
			player().inventory.armorInventory[3] = enchanted(Item.getItemFromBlock(Blocks.pumpkin), Enchantment.blastProtection, 4);
			return new double[]{bare, explosionPush()};
		});
		assertTrue(pushes[0] > 0.0, "no push; " + before[0]);
		assertAlmostEqual(0.4, pushes[1] / pushes[0], 1.0E-6);
	}

	/** For a failure: the player's place, the ray share the blast would see, and any block between them. In a server step. */
	private static String blastSurroundings() {
		EntityPlayer player = player();
		StringBuilder blocks = new StringBuilder();
		for (int x = -1; x <= 1; x++) {
			for (int y = 3; y <= 6; y++) {
				for (int z = 0; z <= 4; z++) {
					if (!world().isAirBlock(x, y, z) && y > 3) {
						blocks.append(' ').append(Block.blockRegistry.getNameForObject(world().getBlock(x, y, z))).append('@').append(x).append(',').append(y).append(',').append(z);
					}
				}
			}
		}
		return "player at " + player.posX + " " + player.posY + " " + player.posZ + ", flying " + player.capabilities.isFlying
			+ ", riding " + player.ridingEntity + ", density " + world().getBlockDensity(Vec3.createVectorHelper(0.5, 4.5, 3.0), player.boundingBox)
			+ ", blocks:" + (blocks.length() == 0 ? " none" : blocks);
	}

	/** An explosion of 2 a few blocks ahead; the length of the push sent to the player's client. In a server step. */
	private static double explosionPush() {
		player().setHealth(player().getMaxHealth());
		player().hurtResistantTime = 0;
		Explosion explosion = world().createExplosion(null, 0.5, 4.5, 3.0, 2.0F, false);
		Vec3 push = (Vec3) explosion.func_77277_b().get(player());
		return push == null ? 0.0 : push.lengthVector();
	}

	/** Fire Protection II on two pieces (4 levels): 10 s of fire burns 4 s; on all four (8 levels) not at all. */
	public void testFireProtectionShortensBurning() {
		wear(enchanted(Items.leather_helmet, Enchantment.fireProtection, 2), enchanted(Items.leather_chestplate, Enchantment.fireProtection, 2), null, null);
		assertEqual(80, onServer(() -> {
			player().extinguish();
			player().setFire(10);
			return ((AccessorEntity) player()).etfu$getFire();
		}));
		wear(enchanted(Items.leather_helmet, Enchantment.fireProtection, 2), enchanted(Items.leather_chestplate, Enchantment.fireProtection, 2),
			enchanted(Items.leather_leggings, Enchantment.fireProtection, 2), enchanted(Items.leather_boots, Enchantment.fireProtection, 2));
		assertEqual(0, onServer(() -> {
			player().extinguish();
			player().setFire(10);
			return ((AccessorEntity) player()).etfu$getFire();
		}));
	}

	/** Blast Protection IV on one piece takes 60 % off an explosion's push; on two (8 levels), all of it. */
	public void testBlastProtectionPush() {
		wear(enchanted(Items.leather_helmet, Enchantment.blastProtection, 4), null, null, null);
		assertAlmostEqual(0.4, onServer(() -> EnchantmentProtection.func_92092_a(player(), 1.0D)), 1.0E-6);
		wear(enchanted(Items.leather_helmet, Enchantment.blastProtection, 4), enchanted(Items.leather_chestplate, Enchantment.blastProtection, 4), null, null);
		assertAlmostEqual(0.0, onServer(() -> EnchantmentProtection.func_92092_a(player(), 1.0D)), 1.0E-6);
	}

	// ---------------------------------------------------------------- weapons and effects

	/** Sharpness I adds 1, V adds 3 (0.5 + 0.5 a level); Smite V still 12.5 on the undead. A Sharpness V diamond sword at full charge does 10. */
	public void testSharpness() {
		assertAlmostEqual(1.0, Enchantment.sharpness.func_152376_a(1, EnumCreatureAttribute.UNDEFINED), 1.0E-6);
		assertAlmostEqual(3.0, Enchantment.sharpness.func_152376_a(5, EnumCreatureAttribute.UNDEFINED), 1.0E-6);
		assertAlmostEqual(12.5, Enchantment.smite.func_152376_a(5, EnumCreatureAttribute.UNDEAD), 1.0E-6);
		setHeld(enchanted(Items.diamond_sword, Enchantment.sharpness, 5));
		EntityVillager villager = spawn(EntityVillager::new, 0.5, 4.0, 2.0);
		waitUntil("a full charge", () -> AttackCooldown.charge(player(), 0.0F) >= 1.0F, 60);
		assertAlmostEqual(10.0, onServer(() -> {
			player().attackTargetEntityWithCurrentItem(villager);
			return villager.getMaxHealth() - villager.getHealth();
		}), 1.0E-4);
	}

	/** Attack damage: a diamond sword 7; with Strength I 10, II 13; a fist with Weakness I nothing (1 − 4, at least 0). */
	public void testStrengthAndWeakness() {
		setHeld(new ItemStack(Items.diamond_sword));
		ticks(2);
		assertAlmostEqual(7.0, attackDamage(), 1.0E-4);
		effect(Potion.damageBoost, 0);
		assertAlmostEqual(10.0, attackDamage(), 1.0E-4);
		effect(Potion.damageBoost, 1);
		assertAlmostEqual(13.0, attackDamage(), 1.0E-4);
		onServer(() -> player().removePotionEffect(Potion.damageBoost.id));
		setHeld(null);
		ticks(2);
		effect(Potion.weakness, 0);
		assertAlmostEqual(0.0, attackDamage(), 1.0E-4);
	}

	private double attackDamage() {
		return onServer(() -> player().getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue());
	}

	private void effect(Potion potion, int amplifier) {
		onServer(() -> {
			player().removePotionEffect(potion.id);
			player().addPotionEffect(new PotionEffect(potion.id, 600, amplifier));
		});
	}

	/** A sword's charge time: 12.5 ticks; Haste II 20 ÷ 1.92; Mining Fatigue I 20 ÷ 1.44; on the client too. */
	public void testHasteAndMiningFatigue() {
		setHeld(new ItemStack(Items.diamond_sword));
		ticks(2);
		assertAlmostEqual(12.5, onServer(() -> (double) AttackCooldown.delay(player())), 1.0E-3);
		effect(Potion.digSpeed, 1);
		ticks(3);
		assertAlmostEqual(20.0 / 1.92, onServer(() -> (double) AttackCooldown.delay(player())), 1.0E-3);
		assertAlmostEqual(20.0 / 1.92, onClient(() -> (double) AttackCooldown.delay(mc().thePlayer)), 1.0E-3);
		onServer(() -> player().removePotionEffect(Potion.digSpeed.id));
		effect(Potion.digSlowdown, 0);
		ticks(3);
		assertAlmostEqual(20.0 / 1.44, onServer(() -> (double) AttackCooldown.delay(player())), 1.0E-3);
	}

	// ---------------------------------------------------------------- knockback

	/**
	 * Being hurt: 0.4 sideways and up from standing still; two netherite pieces (0.2) 0.32; in the air no lift. A
	 * Knockback I sword at full charge adds a second push of 0.5 on the halved first: 0.2 + 0.5 = 0.7.
	 */
	public void testKnockback() {
		EntityVillager villager = spawn(EntityVillager::new, 0.5, 4.0, 2.0);
		// no AI: a step aside before the hit turns the push off the player's line (0.697 for 0.7 once, 2026-11-03)
		onServer(() -> villager.tasks.taskEntries.clear());
		double[] pushed = push(villager, true);
		assertAlmostEqual(0.4, pushed[0], 1.0E-6);
		assertAlmostEqual(0.4, pushed[1], 1.0E-6);
		pushed = push(villager, false);
		assertAlmostEqual(0.4, pushed[0], 1.0E-6);
		assertAlmostEqual(0.0, pushed[1], 1.0E-6);
		if (ModItems.NETHERITE_HELMET.isEnabled()) {
			onServer(() -> {
				villager.setCurrentItemOrArmor(4, new ItemStack(ModItems.NETHERITE_HELMET.get()));
				villager.setCurrentItemOrArmor(3, new ItemStack(ModItems.NETHERITE_CHESTPLATE.get()));
			});
			assertAlmostEqual(0.32, push(villager, true)[0], 1.0E-6);
			onServer(() -> {
				villager.setCurrentItemOrArmor(4, null);
				villager.setCurrentItemOrArmor(3, null);
			});
		}
		setHeld(enchanted(Items.diamond_sword, Enchantment.knockback, 1));
		waitUntil("a full charge", () -> AttackCooldown.charge(player(), 0.0F) >= 1.0F, 60);
		assertAlmostEqual(0.7, onServer(() -> {
			still(villager, true);
			player().attackTargetEntityWithCurrentItem(villager);
			return Math.sqrt(villager.motionX * villager.motionX + villager.motionZ * villager.motionZ);
		}), 1.0E-6);
	}

	/** Hurts the villager standing still (on the ground or in the air); its motion right after: sideways, up. */
	private double[] push(EntityVillager villager, boolean onGround) {
		return onServer(() -> {
			still(villager, onGround);
			villager.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F);
			return new double[]{Math.sqrt(villager.motionX * villager.motionX + villager.motionZ * villager.motionZ), villager.motionY};
		});
	}

	private static void still(EntityVillager villager, boolean onGround) {
		villager.setHealth(villager.getMaxHealth());
		villager.hurtResistantTime = 0;
		villager.motionX = villager.motionY = villager.motionZ = 0.0D;
		villager.onGround = onGround;
	}

	// ---------------------------------------------------------------- tooltips

	/** Modern's armour block: the slot, then points, toughness and knockback resistance (× 10), blue. */
	public void testArmourTooltips() {
		List<String> chestplate = tooltip(new ItemStack(Items.diamond_chestplate));
		assertIn("When on Body:", chestplate);
		assertIn("+8 Armor", chestplate);
		assertIn("+2 Armor Toughness", chestplate);
		List<String> helmet = tooltip(new ItemStack(Items.iron_helmet));
		assertIn("When on Head:", helmet);
		assertIn("+2 Armor", helmet);
		assertNotIn("+0 Armor Toughness", helmet);
		if (ModItems.NETHERITE_HELMET.isEnabled()) {
			List<String> netherite = tooltip(new ItemStack(ModItems.NETHERITE_HELMET.get()));
			assertIn("+3 Armor Toughness", netherite);
			assertIn("+1 Knockback Resistance", netherite);
		}
	}

	private List<String> tooltip(ItemStack stack) {
		return onClient(() -> {
			List<String> lines = new ArrayList<>();
			for (Object line : stack.getTooltip(mc().thePlayer, false)) {
				lines.add(EnumChatFormatting.getTextWithoutFormattingCodes((String) line));
			}
			return lines;
		});
	}
}
