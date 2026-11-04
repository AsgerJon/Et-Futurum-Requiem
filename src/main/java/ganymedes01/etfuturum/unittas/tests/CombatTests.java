package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import cpw.mods.fml.relauncher.ReflectionHelper;
import ganymedes01.etfuturum.ModEnchantments;
import ganymedes01.etfuturum.client.gui.GuiAttackIndicatorButton;
import ganymedes01.etfuturum.client.gui.options.IOptionsLayout;
import ganymedes01.etfuturum.client.particle.SweepAttackFX;
import ganymedes01.etfuturum.combat.AttackCooldown;
import ganymedes01.etfuturum.combat.ICombatPlayer;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.lib.Reference;
import io.github.asgerjon.mcfly.mixins.early.fov.client.AccessorGuiOptionsRowList;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.Offhand;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.client.gui.GuiOptionsRowList;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * The combat update's stage 1 (spec §46.1 to §46.5, the checks of §46.8): the charge and what it scales, crits,
 * sweeps and Sweeping Edge, sprint knockback, the charge times, tooltips, swords not blocking, the hand's dip. Hits go
 * through the real left click; the targets are villagers (no armour, 20 health), so a hit's damage reads straight off
 * their health. The player stands at 0.5 4 0.5 facing south (+z). §46.8's two-player check needs a dedicated server.
 */
public class CombatTests extends TasTestCase {

	private static final String ATTACK_SOUND = Reference.MCAssetVer + ":entity.player.attack.";

	@Override
	protected void setUp() {
		if (!McFlyConfig.modernCombat) {
			skipTest("the combat update is turned off (modernCombat)");
		}
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		setHeld(new ItemStack(Items.diamond_sword));
		waitUntilOnClient("the client to have the sword", () -> mc().thePlayer.getHeldItem() != null
			&& mc().thePlayer.getHeldItem().getItem() == Items.diamond_sword, 40);
		// the server applies a held item's attributes in the player's own update, which comes with the client's next
		// movement packet, not every server tick: wait for that too, or a test can still read the bare hand's 5 ticks
		// (testChargeTimes failed so 4 times in 47 runs)
		waitUntil("the server to have applied the sword", () -> AttackCooldown.delay(player()) > 6.0F, 40);
	}

	/** A villager standing at x z (feet on the ground); returns its entity id. */
	private int villagerAt(double x, double z) {
		EntityVillager villager = spawn(EntityVillager::new, x, 4.0, z);
		return onServer(villager::getEntityId);
	}

	private float healthOf(int entityId) {
		return onServer(() -> ((EntityVillager) world().getEntityByID(entityId)).getHealth());
	}

	/** Turns to the yaw and pitch and waits for the crosshair to be on the entity. */
	private void aimAt(int entityId, float yaw, float pitch) {
		look(yaw, pitch);
		waitUntilOnClient("the crosshair on entity " + entityId, () -> mc().objectMouseOver != null && mc().objectMouseOver.entityHit != null
			&& mc().objectMouseOver.entityHit.getEntityId() == entityId, 20);
	}

	/** Waits for the server's charge to be full (the server works out the hit). */
	private void fullCharge() {
		waitUntil("a full charge", () -> AttackCooldown.charge(player(), 0.0F) >= 1.0F, 60);
	}

	/** Left click, and time for the server to work out the hit. */
	private void attack() {
		tap(mc().gameSettings.keyBindAttack);
		ticks(3);
	}

	// ---------------------------------------------------------------- the charge

	/** Waiting for the indicator: a diamond sword at full charge does its 7 (§46.8 1). */
	public void testFullChargeHit() {
		int villager = villagerAt(0.5, 2.0);
		aimAt(villager, 0.0F, 0.0F);
		fullCharge();
		attack();
		assertAlmostEqual(13.0, healthOf(villager), 1.0E-4);
	}

	/**
	 * Spam-clicking does little (§46.8 1): two clicks in a row at an iron golem (100 health, no armour, not knocked back,
	 * so it stays under the crosshair), from a charge just restarted (as a swing restarts it), do a few points in all (the
	 * second, weaker still, falls in the golem's hurt cooldown and adds nothing, as in modern), with the weak sound.
	 * Sprinting adds no knockback while charging (§46.8 5). The golem hits back, and throws its target up, so it
	 * stands in a pit before the player's roof (golemArena).
	 */
	public void testSpamClickIsWeak() {
		golemArena(true);
		EntityIronGolem golem = spawn(EntityIronGolem::new, 0.5, 3.0, 3.8);
		int id = onServer(golem::getEntityId);
		try {
			aimAt(id, 0.0F, 0.0F);
			onServer(() -> player().setSprinting(true));
			clearSounds();
			// the charge restarted on both sides just before the clicks, whatever the setting up took
			onServer(() -> ((ICombatPlayer) player()).etfu$resetAttackStrength());
			onClient(() -> ((ICombatPlayer) mc().thePlayer).etfu$resetAttackStrength());
			tap(mc().gameSettings.keyBindAttack);
			attack();
			float taken = onServer(() -> golem.getMaxHealth() - golem.getHealth());
			assertGreater(taken, 0.0);
			assertTrue(taken < 3.5F, "two quick clicks did " + taken + " (a full charge does 7)");
			waitForSound(ATTACK_SOUND + "weak", 20);
			assertNotIn(ATTACK_SOUND + "knockback", soundsHeard(), "sprinting while charging knocked back");
		} finally {
			onServer(() -> {
				player().setSprinting(false);
				golem.setDead();
			});
			golemArena(false);
		}
	}

	/**
	 * A golem's pit and the player's roof, built or taken down. The roof, 5 by 5 on y 6 over the start point with two
	 * blocks of room, keeps the golem (2.9 tall) from coming under it, from any side. The pit, a block deep just past
	 * the roof (x -1 to 1, z 3 to 5), walls the golem in and keeps its eyes below the roof: the server lets a mob it
	 * can't see be hit only within 3 (processUseEntity), and the golem 3.3 away would be unseen past the roof's edge.
	 * From a block lower the golem's reach (2.9, its feet to the target's) falls short, while the player's reach of 3
	 * gets to its front, 2.6 away.
	 */
	private void golemArena(boolean build) {
		onServer(() -> {
			for (int x = -2; x <= 2; x++) {
				for (int z = -2; z <= 2; z++) {
					world().setBlock(x, 6, z, build ? Blocks.stone : Blocks.air);
				}
			}
			for (int x = -1; x <= 1; x++) {
				for (int z = 3; z <= 5; z++) {
					world().setBlock(x, 3, z, build ? Blocks.air : Blocks.grass);
				}
			}
		});
	}

	/** The indicator's time to fill (§46.8 2): 20 / attack speed ticks, a sword 12.5 (1.6 a second), a diamond axe 20 (1). */
	public void testChargeTimes() {
		ticks(2);
		assertAlmostEqual(12.5, onServer(() -> (double) AttackCooldown.delay(player())), 1.0E-4);
		assertAlmostEqual(12.5, onClient(() -> (double) AttackCooldown.delay(mc().thePlayer)), 1.0E-4);
		setHeld(new ItemStack(Items.diamond_axe));
		waitUntilOnClient("the client to have the axe", () -> mc().thePlayer.getHeldItem() != null
			&& mc().thePlayer.getHeldItem().getItem() == Items.diamond_axe, 40);
		// as in setUp: the server applies the axe in the player's own update (with the next movement packet), and the
		// client has it from the server's attribute packet after that (a fixed 2 ticks failed once in a full run)
		waitUntil("the server to have applied the axe", () -> AttackCooldown.delay(player()) > 15.0F, 40);
		waitUntilOnClient("the client to have the axe's attributes", () -> AttackCooldown.delay(mc().thePlayer) > 15.0F, 40);
		assertAlmostEqual(20.0, onServer(() -> (double) AttackCooldown.delay(player())), 1.0E-4);
		assertAlmostEqual(20.0, onClient(() -> (double) AttackCooldown.delay(mc().thePlayer)), 1.0E-4);
	}

	/**
	 * Switching to a different item restarts the charge (§46.8 8); to more of the same (another diamond sword) it
	 * doesn't, as modern only restarts for a different item.
	 */
	public void testSwitchingRestartsTheCharge() {
		onServer(() -> {
			player().inventory.setInventorySlotContents(1, new ItemStack(Items.iron_sword));
			player().inventory.setInventorySlotContents(2, new ItemStack(Items.diamond_sword));
			player().inventoryContainer.detectAndSendChanges();
		});
		fullCharge();
		onClient(() -> {
			mc().thePlayer.inventory.currentItem = 2;
		});
		ticks(3);
		assertAlmostEqual(1.0, onServer(() -> (double) AttackCooldown.charge(player(), 0.0F)), 1.0E-4);
		onClient(() -> {
			mc().thePlayer.inventory.currentItem = 1;
		});
		ticks(3);
		assertTrue(onServer(() -> AttackCooldown.charge(player(), 0.0F) < 0.4F), "switching to an iron sword didn't restart the server's charge");
		assertTrue(onClient(() -> AttackCooldown.charge(mc().thePlayer, 0.0F) < 0.4F), "switching to an iron sword didn't restart the client's charge");
	}

	/** The held item dips after a swing and rises again as it charges (§46.8 8): its height is the swap charge cubed. */
	public void testHandDipsAfterASwing() {
		Field progress = ReflectionHelper.findField(ItemRenderer.class, "equippedProgress", "field_78454_c");
		waitUntilOnClient("the hand up", () -> progress.getFloat(mc().entityRenderer.itemRenderer) > 0.99F, 40);
		// a swing at the air
		look(0.0F, -90.0F);
		tap(mc().gameSettings.keyBindAttack);
		ticks(2);
		assertTrue(onClient(() -> progress.getFloat(mc().entityRenderer.itemRenderer) < 0.5F), "the hand didn't dip after the swing");
		waitUntilOnClient("the hand up again", () -> progress.getFloat(mc().entityRenderer.itemRenderer) > 0.99F, 40);
	}

	// ---------------------------------------------------------------- crits, sprinting

	/** Jumping and hitting at full charge while falling, not sprinting: a crit, 7 × 1.5, with the crit sound (§46.8 4). */
	public void testJumpHitIsACrit() {
		int villager = villagerAt(0.5, 2.0);
		// looking down, so the crosshair stays on the villager at the top of the jump
		aimAt(villager, 0.0F, 45.0F);
		fullCharge();
		jumpAndFall();
		clearSounds();
		attack();
		assertAlmostEqual(9.5, healthOf(villager), 1.0E-4);
		waitForSound(ATTACK_SOUND + "crit", 20);
	}

	/** A sprint-jump hit is no crit (§46.8 4); at full charge it knocks back, with the knockback sound (§46.8 5). */
	public void testSprintJumpHitIsNoCrit() {
		int villager = villagerAt(0.5, 2.0);
		aimAt(villager, 0.0F, 45.0F);
		fullCharge();
		jumpAndFall();
		onServer(() -> player().setSprinting(true));
		clearSounds();
		try {
			attack();
			assertAlmostEqual(13.0, healthOf(villager), 1.0E-4);
			waitForSound(ATTACK_SOUND + "knockback", 20);
			assertNotIn(ATTACK_SOUND + "crit", soundsHeard(), "a sprinting hit was a crit");
		} finally {
			onServer(() -> player().setSprinting(false));
		}
	}

	/** A sprint hit at full charge knocks back, with its sound, and ends the sprint (§46.8 5). */
	public void testSprintHitKnocksBack() {
		int villager = villagerAt(0.5, 2.0);
		aimAt(villager, 0.0F, 0.0F);
		fullCharge();
		onServer(() -> player().setSprinting(true));
		clearSounds();
		try {
			attack();
			assertAlmostEqual(13.0, healthOf(villager), 1.0E-4);
			waitForSound(ATTACK_SOUND + "knockback", 20);
			assertFalse(onServer(() -> player().isSprinting()), "the knockback hit didn't end the sprint");
		} finally {
			onServer(() -> player().setSprinting(false));
		}
	}

	/** Jumps, and waits until the player is falling (the server knows from the client's positions). */
	private void jumpAndFall() {
		tap(mc().gameSettings.keyBindJump);
		waitUntilOnClient("falling", () -> !mc().thePlayer.onGround && mc().thePlayer.motionY < 0.0D && mc().thePlayer.fallDistance > 0.1F, 30);
	}

	// ---------------------------------------------------------------- sweeps

	/**
	 * At full charge on the ground a sword sweeps: the mobs beside the target take 1, with the sweep sound (§46.8 3),
	 * and the sweep's particle shows a block ahead at half the player's height, two blocks across (§46.11).
	 */
	public void testSweep() {
		int target = villagerAt(0.5, 2.0);
		int left = villagerAt(1.5, 2.0);
		int right = villagerAt(-0.5, 2.0);
		aimAt(target, 0.0F, 0.0F);
		fullCharge();
		clearSounds();
		tap(mc().gameSettings.keyBindAttack);
		waitUntilOnClient("the sweep's particle", () -> sweepParticle() != null, 10);
		double[] particle = onClient(() -> {
			SweepAttackFX sweep = sweepParticle();
			return new double[]{sweep.posX, sweep.posY, sweep.posZ};
		});
		assertAlmostEqual(0.5, particle[0], 0.01);
		assertAlmostEqual(4.9, particle[1], 0.01);
		assertAlmostEqual(1.5, particle[2], 0.01);
		ticks(3);
		assertAlmostEqual(13.0, healthOf(target), 1.0E-4);
		assertAlmostEqual(19.0, healthOf(left), 1.0E-4);
		assertAlmostEqual(19.0, healthOf(right), 1.0E-4);
		waitForSound(ATTACK_SOUND + "sweep", 20);
	}

	/** A sweep particle among the client's particles, or null. */
	private SweepAttackFX sweepParticle() throws IllegalAccessException {
		Field layers = ReflectionHelper.findField(EffectRenderer.class, "fxLayers", "field_78876_b");
		for (List<?> layer : (List<?>[]) layers.get(mc().effectRenderer)) {
			for (Object particle : layer) {
				if (particle instanceof SweepAttackFX sweep) {
					return sweep;
				}
			}
		}
		return null;
	}

	/** A hit that doesn't sweep (a crit) shows no sweep particle. */
	public void testNoSweepParticleWithoutASweep() {
		int villager = villagerAt(0.5, 2.0);
		aimAt(villager, 0.0F, 45.0F);
		fullCharge();
		jumpAndFall();
		tap(mc().gameSettings.keyBindAttack);
		for (int tick = 0; tick < 6; tick++) {
			assertIsNone(onClient(this::sweepParticle));
			ticks(1);
		}
	}

	/**
	 * Video Settings has modern's Attack Indicator option (§46.11): a click goes Crosshair, Hotbar, OFF and round,
	 * saved to attackIndicator.
	 */
	public void testAttackIndicatorOption() throws Exception {
		String before = McFlyConfig.attackIndicator;
		try {
			McFlyConfig.saveAttackIndicator("crosshair");
			onClient(() -> {
				mc().displayGuiScreen(new GuiVideoSettings(null, mc().gameSettings));
			});
			ticks(3);
			GuiAttackIndicatorButton button = onClient(this::attackIndicatorButton);
			assertIsNotNone(button, "no Attack Indicator button in Video Settings");
			assertEqual("Attack Indicator: Crosshair", onClient(() -> button.displayString));
			for (String expected : new String[]{"hotbar", "off", "crosshair"}) {
				onClient(() -> button.mousePressed(mc(), button.xPosition + 1, button.yPosition + 1));
				assertEqual(expected, McFlyConfig.attackIndicator);
			}
			onClient(() -> button.mousePressed(mc(), button.xPosition + 1, button.yPosition + 1));
			assertEqual("Attack Indicator: Hotbar", onClient(() -> button.displayString));
		} finally {
			McFlyConfig.saveAttackIndicator(before);
		}
	}

	/**
	 * The indicator under the crosshair leaves the rest of the HUD as it was (the user, 2026-11-04: the dragon's bar
	 * turned black and white now and then while they hit it): Forge draws the boss bar right after the crosshair, from
	 * the texture the crosshair left bound. The bar at half health with the indicator charging looks as it does alone.
	 */
	public void testIndicatorLeavesTheBossBarAlone() throws Exception {
		String before = McFlyConfig.attackIndicator;
		try {
			McFlyConfig.saveAttackIndicator("crosshair");
			cameraView(0);
			look(0.0F, -60.0F);
			onClient(() -> {
				net.minecraft.entity.boss.BossStatus.bossName = "Ender Dragon";
				net.minecraft.entity.boss.BossStatus.healthScale = 0.5F;
				net.minecraft.entity.boss.BossStatus.statusBarTime = 1000;
				net.minecraft.entity.boss.BossStatus.hasColorModifier = false;
			});
			waitUntilOnClient("a full charge on the client", () -> AttackCooldown.charge(mc().thePlayer, 0.0F) >= 1.0F, 60);
			String alone = screenshot("combat.boss_bar_alone");
			onClient(() -> ((ICombatPlayer) mc().thePlayer).etfu$resetAttackStrength());
			String charging = screenshot("combat.boss_bar_indicator_charging");
			assertTrue(onClient(() -> AttackCooldown.charge(mc().thePlayer, 0.0F) < 1.0F), "the indicator still charging when the picture was taken");
			int[] bar = onClient(() -> {
				net.minecraft.client.gui.ScaledResolution scaled = new net.minecraft.client.gui.ScaledResolution(mc(), mc().displayWidth, mc().displayHeight);
				int scale = scaled.getScaleFactor();
				int left = scaled.getScaledWidth() / 2 - 91;
				return new int[] {left * scale, 12 * scale, 182 * scale, 5 * scale};
			});
			java.awt.image.BufferedImage first = javax.imageio.ImageIO.read(new java.io.File(resultsFolder(), alone));
			java.awt.image.BufferedImage second = javax.imageio.ImageIO.read(new java.io.File(resultsFolder(), charging));
			long sum = 0;
			for (int y = bar[1]; y < bar[1] + bar[3]; y++) {
				for (int x = bar[0]; x < bar[0] + bar[2]; x++) {
					int a = first.getRGB(x, y);
					int b = second.getRGB(x, y);
					sum += Math.abs((a >> 16 & 255) - (b >> 16 & 255)) + Math.abs((a >> 8 & 255) - (b >> 8 & 255)) + Math.abs((a & 255) - (b & 255));
				}
			}
			double change = sum / (3.0 * bar[2] * bar[3]);
			assertTrue(change < 3.0, String.format(java.util.Locale.ROOT, "the boss bar with the indicator charging looks as alone: mean change %.1f (%s, %s)", change, alone, charging));
		} finally {
			McFlyConfig.saveAttackIndicator(before);
			onClient(() -> {
				net.minecraft.entity.boss.BossStatus.statusBarTime = 0;
				net.minecraft.entity.boss.BossStatus.bossName = null;
			});
		}
	}

	/** The Attack Indicator button in the open Video Settings' rows (EFR's modern screen or 1.7.10's), or null. */
	private GuiAttackIndicatorButton attackIndicatorButton() throws IllegalAccessException {
		if (mc().currentScreen instanceof IOptionsLayout layout) {
			for (Object entry : layout.etfu$entries()) {
				if (entry instanceof GuiAttackIndicatorButton button) {
					return button;
				}
			}
			return null;
		}
		Field list = ReflectionHelper.findField(GuiVideoSettings.class, "optionsRowList");
		Field left = ReflectionHelper.findField(GuiOptionsRowList.Row.class, "field_148323_b");
		Field right = ReflectionHelper.findField(GuiOptionsRowList.Row.class, "field_148324_c");
		GuiOptionsRowList rows = (GuiOptionsRowList) list.get(mc().currentScreen);
		for (GuiOptionsRowList.Row row : ((AccessorGuiOptionsRowList) rows).etfu$rows()) {
			for (Field side : new Field[]{left, right}) {
				if (side.get(row) instanceof GuiAttackIndicatorButton button) {
					return button;
				}
			}
		}
		return null;
	}

	/** Sweeping Edge III: the sweep does 1 + 0.75 × 7 at full charge (§46.8 7a). */
	public void testSweepingEdge() {
		if (ModEnchantments.sweepingEdge == null) {
			skipTest("Sweeping Edge is turned off");
		}
		assertNoError(command("/enchant @s sweeping_edge 3"));
		int target = villagerAt(0.5, 2.0);
		int beside = villagerAt(1.5, 2.0);
		aimAt(target, 0.0F, 0.0F);
		fullCharge();
		attack();
		assertAlmostEqual(13.0, healthOf(target), 1.0E-4);
		assertAlmostEqual(20.0 - 6.25, healthOf(beside), 1.0E-4);
	}

	/** Sweeping Edge comes from the enchanting table for swords and for books (§46.8 7a): level III at 30. */
	public void testSweepingEdgeAtTheTable() {
		if (ModEnchantments.sweepingEdge == null) {
			skipTest("Sweeping Edge is turned off");
		}
		int id = ModEnchantments.sweepingEdge.effectId;
		assertIn(id, onServer(() -> EnchantmentHelper.mapEnchantmentData(30, new ItemStack(Items.diamond_sword)).keySet()));
		assertIn(id, onServer(() -> EnchantmentHelper.mapEnchantmentData(30, new ItemStack(Items.book)).keySet()));
	}

	// ---------------------------------------------------------------- tooltips, right click

	/** "When in Main Hand:" with the totals (§46.8 6): a diamond sword 7 and 1.6, a diamond axe 9 and 1. */
	public void testTooltips() {
		List<String> sword = tooltip(new ItemStack(Items.diamond_sword));
		assertIn("When in Main Hand:", sword);
		assertIn(" 7 Attack Damage", sword);
		assertIn(" 1.6 Attack Speed", sword);
		List<String> axe = tooltip(new ItemStack(Items.diamond_axe));
		assertIn(" 9 Attack Damage", axe);
		assertIn(" 1 Attack Speed", axe);
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

	/** Right click with a sword doesn't block (§46.8 7); with food in the off-hand, it eats. */
	public void testSwordDoesntBlockAndTheOffhandEats() {
		keyDown(mc().gameSettings.keyBindUseItem);
		ticks(6);
		try {
			assertFalse(onServer(() -> player().isUsingItem()), "right click with a sword started a use (blocking)");
		} finally {
			keyUp(mc().gameSettings.keyBindUseItem);
		}
		if (!Offhand.isEnabled()) {
			return;
		}
		onServer(() -> {
			player().getFoodStats().setFoodLevel(10);
			Offhand.setOffhand(player(), new ItemStack(Items.bread));
			player().inventoryContainer.detectAndSendChanges();
		});
		waitUntilOnClient("the client to have the bread and be hungry", () -> Offhand.getOffhand(mc().thePlayer) != null
			&& mc().thePlayer.getFoodStats().getFoodLevel() < 20, 40);
		keyDown(mc().gameSettings.keyBindUseItem);
		try {
			waitUntil("eating from the off-hand", () -> player().isUsingItem() && Offhand.getUsingHand(player()) == Hand.OFF, 20);
			waitUntil("the bread eaten", () -> player().getFoodStats().getFoodLevel() > 10, 60);
		} finally {
			keyUp(mc().gameSettings.keyBindUseItem);
		}
	}
}
