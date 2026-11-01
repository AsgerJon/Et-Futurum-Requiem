package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.combat.AttackCooldown;
import ganymedes01.etfuturum.combat.ICombatPlayer;
import ganymedes01.etfuturum.combat.ShieldBlocking;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.core.handlers.ClientEventHandler;
import ganymedes01.etfuturum.entities.EntityTippedArrow;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.Offhand;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The shield (spec §46.9): its rules, played on the server's real damage path, and its placement in each view as
 * screenshots to hold against 26.3's. The player stands at 0.5 4 0.5 looking south (+z): the front is +z.
 */
public class ShieldTests extends TasTestCase {

	private static final String BLOCK_SOUND = Reference.MCAssetVer + ":item.shield.block";
	private static final String BREAK_SOUND = Reference.MCAssetVer + ":item.shield.break";

	/** In survival: a creative player takes no damage to block. */
	@Override
	protected void setUp() {
		if (!ModItems.SHIELD.isEnabled()) {
			skipTest("the shield is turned off (enableShield, modernCombat)");
		}
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		setHeld(new ItemStack(ModItems.SHIELD.get()));
		ticks(2);
	}

	/** Raised on the server as right click raises it, and held up past the 5-tick delay. */
	private void raise() {
		onServer(() -> player().setItemInUse(player().getHeldItem(), 72000));
		serverTicks(6);
	}

	/** The health the hit took (the hurt timer is cleared first, so each hit counts). */
	private float hit(DamageSource source, float amount) {
		return onServer(() -> {
			EntityPlayer player = player();
			player.hurtResistantTime = 0;
			float before = player.getHealth();
			player.attackEntityFrom(source, amount);
			return before - player.getHealth();
		});
	}

	private int shieldDamage() {
		return onServer(() -> {
			ItemStack shield = player().getHeldItem();
			return shield == null ? -1 : shield.getItemDamage();
		});
	}

	private EntityZombie zombieAt(double z) {
		return spawn(EntityZombie::new, 0.5, 4.0, z);
	}

	/**
	 * Back at the start point facing the front: a hit from behind knocks the player forward, past where the next
	 * "front" zombie stands. The knockback is let finish first, so the client doesn't slide on afterwards.
	 */
	private void backToStart() {
		ticks(10);
		onServer(() -> {
			player().motionX = player().motionY = player().motionZ = 0.0D;
			player().playerNetServerHandler.setPlayerLocation(0.5, 4.0, 0.5, 0.0F, 0.0F);
		});
		ticks(3);
	}

	/** An arrow flying along z (direction +1 or -1) from z, at chest height: from the front -1 from z 4.5, from behind +1 from -3.5. */
	private void shoot(Function<World, EntityArrow> make, double z, double direction) {
		onServer(() -> {
			EntityArrow arrow = make.apply(world());
			arrow.setPosition(0.5, 5.0, z);
			arrow.setThrowableHeading(0.0, 0.0, direction, 1.5F, 0.0F);
			world().spawnEntityInWorld(arrow);
		});
	}

	private static EntityArrow poisonArrow(World world) {
		ItemStack tipped = new ItemStack(ModItems.TIPPED_ARROW.get());
		NBTTagCompound tag = new NBTTagCompound();
		tag.setTag("Potion", new PotionEffect(Potion.poison.id, 200, 0).writeCustomPotionEffectToNBT(new NBTTagCompound()));
		tipped.setTagCompound(tag);
		EntityTippedArrow arrow = new EntityTippedArrow(world);
		arrow.setArrow(tipped);
		return arrow;
	}

	private static EntityArrow burningArrow(World world) {
		EntityArrow arrow = new EntityArrow(world);
		arrow.setFire(100);
		return arrow;
	}

	/** The zombies made for hits so far gone, before they walk up and hit the shield themselves. */
	private void removeZombies() {
		onServer(() -> {
			for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
				if (entity instanceof EntityZombie zombie) {
					zombie.setDead();
				}
			}
		});
	}

	// ---------------------------------------------------------------- the rules

	public void testRaiseWithRightClick() {
		keyDown(mc().gameSettings.keyBindUseItem);
		ticks(8);
		try {
			assertTrue(onServer(() -> ShieldBlocking.blockingWith(player()) != null), "right click didn't raise the shield");
		} finally {
			keyUp(mc().gameSettings.keyBindUseItem);
		}
		ticks(3);
		assertFalse(onServer(() -> player().isUsingItem()), "letting go didn't lower it");
	}

	public void testBlockDelay() {
		EntityZombie zombie = zombieAt(2.5);
		float taken = onServer(() -> {
			player().setItemInUse(player().getHeldItem(), 72000);
			player().hurtResistantTime = 0;
			float before = player().getHealth();
			player().attackEntityFrom(DamageSource.causeMobDamage(zombie), 4.0F);
			return before - player().getHealth();
		});
		assertGreater(taken, 0.0);
	}

	public void testFrontAndBehind() {
		raise();
		EntityZombie front = zombieAt(2.5);
		assertEqual(0.0F, hit(DamageSource.causeMobDamage(front), 4.0F), "a hit from the front got through");
		// unitTAS plays on Easy, where a mob's 4 is 4 / 2 + 1 = 3 before the shield sees it (modern scales first too,
		// Player.hurtServer before LivingEntity's blocking): 1 + 3
		assertEqual(4, shieldDamage(), "a 4-damage mob hit (3 on Easy) wears 1 + 3");
		EntityZombie behind = zombieAt(-1.5);
		assertGreater(hit(DamageSource.causeMobDamage(behind), 4.0F), 0.0);
	}

	public void testSmallHitsDontWear() {
		raise();
		hit(DamageSource.causeMobDamage(zombieAt(2.5)), 2.0F);
		assertEqual(0, shieldDamage());
	}

	public void testArrowFromTheFront() {
		raise();
		EntityArrow arrow = spawn(EntityArrow::new, 0.5, 5.0, 3.5);
		assertEqual(0.0F, hit(DamageSource.causeArrowDamage(arrow, null), 4.0F));
	}

	/** A blocked hit plays the block sound for the player to hear (modern's BlocksAttacks.onBlocked). */
	public void testBlockSound() {
		raise();
		clearSounds();
		hit(DamageSource.causeMobDamage(zombieAt(2.5)), 4.0F);
		waitForSound(BLOCK_SOUND, 20);
	}

	/**
	 * A real arrow flying at the raised shield from the front, not a hit made by hand: it does no harm, doesn't stick
	 * in the player, and the block sound plays.
	 */
	public void testArrowShotAtTheShield() {
		raise();
		clearSounds();
		float before = onServer(() -> player().getHealth());
		onServer(() -> {
			player().hurtResistantTime = 0;
		});
		shoot(EntityArrow::new, 4.5, -1.0);
		waitForSound(BLOCK_SOUND, 40);
		assertEqual(before, onServer(() -> player().getHealth()), "the arrow got through the shield");
		assertEqual(0, onServer(() -> player().getArrowCountInEntity()), "the arrow stuck in the player");
	}

	/**
	 * A tipped arrow the raised shield stops gives none of its effect (modern applies them only after a hit that
	 * lands, doPostHurtEffects); one from behind, which lands, does.
	 */
	public void testTippedArrowBlocked() {
		if (!ModItems.TIPPED_ARROW.isEnabled()) {
			skipTest("tipped arrows are turned off");
		}
		raise();
		clearSounds();
		shoot(ShieldTests::poisonArrow, 4.5, -1.0);
		waitForSound(BLOCK_SOUND, 40);
		serverTicks(2);
		assertFalse(onServer(() -> player().isPotionActive(Potion.poison)), "the arrow the shield stopped poisoned the player");
		onServer(() -> {
			player().hurtResistantTime = 0;
		});
		shoot(ShieldTests::poisonArrow, -3.5, 1.0);
		waitUntil("the arrow from behind to poison the player", () -> player().isPotionActive(Potion.poison), 40);
	}

	/**
	 * A burning arrow the shield stops leaves the player as they were, not alight, on the server and on the client
	 * (modern puts the target's fire back when the hit fails); one from behind, which lands, sets them alight.
	 */
	public void testBurningArrowBlocked() {
		raise();
		clearSounds();
		shoot(ShieldTests::burningArrow, 4.5, -1.0);
		waitForSound(BLOCK_SOUND, 40);
		serverTicks(2);
		assertFalse(onServer(() -> player().isBurning()), "the burning arrow the shield stopped set the player alight");
		assertFalse(onClient(() -> mc().thePlayer.isBurning()), "the client shows the player alight");
		onServer(() -> {
			player().hurtResistantTime = 0;
		});
		shoot(ShieldTests::burningArrow, -3.5, 1.0);
		waitUntil("the burning arrow from behind to set the player alight", () -> player().isBurning(), 40);
	}

	/**
	 * Inside the hurt cooldown a blocked hit still wears the shield, but makes no sound: modern's blocking comes before
	 * the cooldown check, the block sound only on the way past it.
	 */
	public void testNoBlockSoundInTheHurtCooldown() {
		raise();
		EntityZombie front = zombieAt(2.5);
		hit(DamageSource.causeMobDamage(front), 4.0F);
		waitForSound(BLOCK_SOUND, 20);
		clearSounds();
		onServer(() -> {
			player().hurtResistantTime = player().maxHurtResistantTime;
			player().attackEntityFrom(DamageSource.causeMobDamage(front), 4.0F);
			// gone before it walks up and hits the shield itself once the cooldown is over (a block with a sound)
			front.setDead();
		});
		ticks(6);
		assertNotIn(BLOCK_SOUND, soundsHeard(), "a block inside the hurt cooldown made the block sound");
		assertEqual(8, shieldDamage(), "both blocks wear the shield (a mob's 4 is 3 on Easy: 1 + 3 each)");
	}

	/**
	 * A shield that wears out breaks with the shield's own sound, which its holder hears too (modern's break_sound;
	 * 1.7.10's random.break left the holder out), and leaves the hand.
	 */
	public void testBreakSound() {
		onServer(() -> {
			ItemStack shield = player().getHeldItem();
			shield.setItemDamage(shield.getMaxDamage() - 1);
		});
		raise();
		clearSounds();
		hit(DamageSource.causeMobDamage(zombieAt(2.5)), 4.0F);
		waitForSound(BREAK_SOUND, 20);
		assertIsNone(onServer(() -> player().getHeldItem()));
		assertFalse(onServer(() -> player().isUsingItem()), "still blocking with the broken shield");
	}

	public void testExplosions() {
		raise();
		EntityCreeper front = spawn(EntityCreeper::new, 0.5, 4.0, 3.5);
		float before = onServer(() -> player().getHealth());
		onServer(() -> {
			world().createExplosion(front, front.posX, front.posY, front.posZ, 2.0F, false);
		});
		assertEqual(before, onServer(() -> player().getHealth()), "a creeper in front got through");
		onServer(() -> {
			player().hurtResistantTime = 0;
		});
		EntityCreeper behind = spawn(EntityCreeper::new, 0.5, 4.0, -2.5);
		onServer(() -> {
			world().createExplosion(behind, behind.posX, behind.posY, behind.posZ, 2.0F, false);
		});
		assertGreater(before - onServer(() -> player().getHealth()), 0.0);
	}

	public void testFireGoesThrough() {
		raise();
		assertGreater(hit(DamageSource.inFire, 2.0F), 0.0);
	}

	public void testAxeDisables() {
		raise();
		EntityZombie zombie = zombieAt(2.5);
		onServer(() -> zombie.setCurrentItemOrArmor(0, new ItemStack(Items.iron_axe)));
		hit(DamageSource.causeMobDamage(zombie), 4.0F);
		assertFalse(onServer(() -> player().isUsingItem()), "the axe didn't lower the shield");
		assertTrue(onServer(() -> ((ICombatPlayer) player()).etfu$getCooldowns().isCoolingDown(ModItems.SHIELD.get())), "no cooldown");
		onServer(() -> {
			player().getHeldItem().getItem().onItemRightClick(player().getHeldItem(), world(), player());
		});
		assertFalse(onServer(() -> player().isUsingItem()), "raised while on cooldown");
		screenshot("shield.cooldown_overlay");
	}

	// ---------------------------------------------------------------- the difficulties

	/**
	 * The difficulty scales a mob's hit before the shield sees it (1.7.10's order and modern's, Player.hurtServer
	 * before LivingEntity's blocking): a 4 is 0 on Peaceful (nothing happens, nothing wears), 3 on Easy, 4 on Normal
	 * and 6 on Hard; the shield wears 1 + that from 3 up. From behind the same hit lands in full.
	 */
	public void testDifficulties() {
		Object[][] cases = {
			{EnumDifficulty.PEACEFUL, 0, 0.0F},
			{EnumDifficulty.EASY, 4, 3.0F},
			{EnumDifficulty.NORMAL, 5, 4.0F},
			{EnumDifficulty.HARD, 7, 6.0F},
		};
		for (Object[] row : cases) {
			EnumDifficulty difficulty = (EnumDifficulty) row[0];
			backToStart();
			difficulty(difficulty);
			setHeld(new ItemStack(ModItems.SHIELD.get()));
			raise();
			assertEqual(0.0F, hit(DamageSource.causeMobDamage(zombieAt(2.5)), 4.0F), "a hit from the front got through on " + difficulty);
			assertEqual(row[1], shieldDamage(), "the shield's wear from a 4 on " + difficulty);
			assertEqual(row[2], hit(DamageSource.causeMobDamage(zombieAt(-1.5)), 4.0F), "what a 4 from behind took on " + difficulty);
			onServer(() -> player().setHealth(player().getMaxHealth()));
			removeZombies();
		}
	}

	/** The 3 the shield starts wearing from is counted after the difficulty: a mob's 2 stays 2 on Normal, but is 3 on Hard. */
	public void testSmallHitsOnHarderDifficulties() {
		difficulty(EnumDifficulty.NORMAL);
		raise();
		hit(DamageSource.causeMobDamage(zombieAt(2.5)), 2.0F);
		assertEqual(0, shieldDamage(), "a 2 on Normal wore the shield");
		removeZombies();
		difficulty(EnumDifficulty.HARD);
		hit(DamageSource.causeMobDamage(zombieAt(2.5)), 2.0F);
		assertEqual(4, shieldDamage(), "a 2 on Hard (3) wears 1 + 3");
	}

	// ---------------------------------------------------------------- the off-hand

	public void testOffhand() {
		if (!Offhand.isEnabled()) {
			skipTest("the off-hand is turned off");
		}
		setHeld(null);
		onServer(() -> {
			Offhand.setOffhand(player(), new ItemStack(ModItems.SHIELD.get()));
			player().inventoryContainer.detectAndSendChanges();
		});
		// the click must find the main hand empty and the shield in the off-hand on the client too, or it raises the
		// main hand's shield from setUp there (a race, not the off-hand's fault)
		waitUntilOnClient("the client to see the shield move to the off-hand",
			() -> mc().thePlayer.getHeldItem() == null && Offhand.getOffhand(mc().thePlayer) != null, 40);
		keyDown(mc().gameSettings.keyBindUseItem);
		ticks(8);
		try {
			assertTrue(onServer(() -> player().isUsingItem() && Offhand.getUsingHand(player()) == Hand.OFF), "the off-hand shield isn't raised");
			assertEqual(0.0F, hit(DamageSource.causeMobDamage(zombieAt(2.5)), 4.0F), "the off-hand shield didn't block");
			screenshot("shield.offhand_first_person_raised");
		} finally {
			keyUp(mc().gameSettings.keyBindUseItem);
		}
	}

	/**
	 * Hit a mob with the sword, then raise the off-hand shield at once while the sword's charge comes back: a right
	 * click at a mob that doesn't take it goes on to the item's own use (modern's startUseItem), so the shield goes
	 * up. The off-hand used to stop at the entity.
	 */
	public void testOffhandShieldAfterASwordHit() {
		if (!Offhand.isEnabled()) {
			skipTest("the off-hand is turned off");
		}
		setHeld(new ItemStack(Items.iron_sword));
		onServer(() -> {
			Offhand.setOffhand(player(), new ItemStack(ModItems.SHIELD.get()));
			player().inventoryContainer.detectAndSendChanges();
		});
		waitUntilOnClient("the client to have the sword and the off-hand shield", () -> mc().thePlayer.getHeldItem() != null
			&& mc().thePlayer.getHeldItem().getItem() == Items.iron_sword && Offhand.getOffhand(mc().thePlayer) != null, 40);
		zombieAt(2.0);
		look(0.0F, 0.0F);
		ticks(3);
		tap(mc().gameSettings.keyBindAttack);
		assertTrue(onClient(() -> AttackCooldown.charge(mc().thePlayer, 0.0F) < AttackCooldown.FULL), "the sword's charge isn't coming back");
		assertTrue(onClient(() -> mc().objectMouseOver != null && mc().objectMouseOver.entityHit instanceof EntityZombie),
			"not looking at the zombie when right click goes down");
		keyDown(mc().gameSettings.keyBindUseItem);
		ticks(8);
		try {
			assertTrue(onServer(() -> player().isUsingItem() && Offhand.getUsingHand(player()) == Hand.OFF), "the off-hand shield isn't raised");
		} finally {
			keyUp(mc().gameSettings.keyBindUseItem);
		}
	}

	public void testRecipe() {
		ItemStack made = onServer(() -> {
			InventoryCrafting grid = new InventoryCrafting(new Container() {
				@Override
				public boolean canInteractWith(EntityPlayer player) {
					return true;
				}
			}, 3, 3);
			String[] pattern = {"WoW", "WWW", " W "};
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 3; column++) {
					char c = pattern[row].charAt(column);
					grid.setInventorySlotContents(row * 3 + column, c == 'W' ? new ItemStack(Blocks.planks) : c == 'o' ? new ItemStack(Items.iron_ingot) : null);
				}
			}
			return CraftingManager.getInstance().findMatchingRecipe(grid, world());
		});
		assertIsNotNone(made, "no recipe matched");
		assertEqual(ModItems.SHIELD.get(), made.getItem());
	}

	// ---------------------------------------------------------------- the placement, for 26.3's to compare

	@Override
	protected int timeoutSeconds() {
		return 180;
	}

	/**
	 * The views unittas-compare.sh holds against 26.3's (unittas-reference, the same views, names and settings): a
	 * pack paints the shield magenta so its outline can be measured; field of view 70, no view bobbing, no clouds, GUI
	 * scale 2, the main hand the right (the config's mainHandLeft only for the run, not saved). The settings and packs
	 * are put back afterwards.
	 */
	public void testScreenshots() throws Exception {
		Object[] before = onClient(() -> new Object[]{mc().gameSettings.fovSetting, mc().gameSettings.viewBobbing, mc().gameSettings.clouds,
			mc().gameSettings.guiScale, new ArrayList<>(mc().getResourcePackRepository().getRepositoryEntries()), ConfigFunctions.mainHandLeft});
		try {
			magentaPack();
			onClient(() -> {
				mc().gameSettings.fovSetting = 70.0F;
				mc().gameSettings.viewBobbing = false;
				mc().gameSettings.clouds = false;
				mc().gameSettings.guiScale = 2;
				ConfigFunctions.mainHandLeft = false;
				ClientEventHandler.sendPlayerSettings();
			});
			calibration();
			look(0.0F, 0.0F);
			ticks(10);
			screenshot("shield.first_person_lowered");
			keyDown(mc().gameSettings.keyBindUseItem);
			ticks(8);
			screenshot("shield.first_person_raised");
			cameraView(1);
			screenshot("shield.third_person_back_raised");
			cameraView(2);
			screenshot("shield.third_person_front_raised");
			keyUp(mc().gameSettings.keyBindUseItem);
			ticks(4);
			screenshot("shield.third_person_front_lowered");
			cameraView(1);
			screenshot("shield.third_person_back_lowered");
			cameraView(0);
			onClient(() -> {
				mc().displayGuiScreen(new GuiInventory(mc().thePlayer) {
					@Override
					public void drawScreen(int mouseX, int mouseY, float partialTicks) {
						// the cursor where the model looks straight ahead (26.3's run does the same)
						super.drawScreen(guiLeft + 51, guiTop + 25, partialTicks);
					}
				});
			});
			ticks(3);
			screenshot("shield.inventory");
			onClient(() -> {
				mc().displayGuiScreen(null);
			});
			setHeld(null);
			if (Offhand.isEnabled()) {
				onServer(() -> {
					Offhand.setOffhand(player(), new ItemStack(ModItems.SHIELD.get()));
					player().inventoryContainer.detectAndSendChanges();
				});
				ticks(10);
				screenshot("shield.offhand_first_person_lowered");
				keyDown(mc().gameSettings.keyBindUseItem);
				ticks(8);
				screenshot("shield.offhand_first_person_raised");
				cameraView(1);
				screenshot("shield.offhand_third_person_back_raised");
				keyUp(mc().gameSettings.keyBindUseItem);
				cameraView(0);
				onServer(() -> Offhand.setOffhand(player(), null));
			}
			// in an item frame on a stone wall ahead, at eye height
			onServer(() -> {
				world().setBlock(0, 5, 3, Blocks.stone);
				EntityItemFrame frame = new EntityItemFrame(world(), 0, 5, 3, 2);
				frame.setDisplayedItem(new ItemStack(ModItems.SHIELD.get()));
				world().spawnEntityInWorld(frame);
			});
			look(0.0F, 0.0F);
			ticks(10);
			screenshot("shield.item_frame");
			onServer(() -> {
				for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
					if (entity instanceof EntityItemFrame frame) {
						frame.setDead();
					}
				}
				world().setBlockToAir(0, 5, 3);
			});
			// on a stone block, seen level: its spin then hardly changes its height on screen
			onServer(() -> {
				world().setBlock(0, 4, 2, Blocks.stone);
				EntityItem dropped = new EntityItem(world(), 0.5, 5.2, 2.5, new ItemStack(ModItems.SHIELD.get()));
				dropped.motionX = dropped.motionY = dropped.motionZ = 0.0D;
				dropped.delayBeforeCanPickup = 32767;
				world().spawnEntityInWorld(dropped);
			});
			look(0.0F, 0.0F);
			ticks(10);
			screenshot("shield.dropped");
			onServer(() -> {
				world().setBlockToAir(0, 4, 2);
			});
		} finally {
			keyUp(mc().gameSettings.keyBindUseItem);
			onClient(() -> {
				mc().gameSettings.fovSetting = (Float) before[0];
				mc().gameSettings.viewBobbing = (Boolean) before[1];
				mc().gameSettings.clouds = (Boolean) before[2];
				mc().gameSettings.guiScale = (Integer) before[3];
				@SuppressWarnings("unchecked")
				List<ResourcePackRepository.Entry> packs = (List<ResourcePackRepository.Entry>) before[4];
				mc().getResourcePackRepository().func_148527_a(packs);
				mc().refreshResources();
				ConfigFunctions.mainHandLeft = (Boolean) before[5];
				ClientEventHandler.sendPlayerSettings();
			});
		}
	}

	/**
	 * The comparison's check of itself: two magenta wool blocks, which every version draws alike, seen from the start
	 * point in first person and from behind. 26.3's run (unittas-reference) places them at the same spots; if these
	 * shots don't line up, the camera or window differs and the shield's numbers mean nothing.
	 */
	private void calibration() throws Exception {
		setHeld(null);
		int[][] blocks = {{1, 1, 3}, {-3, 3, 6}};
		// the block the player stands in, at the start point
		int[] feet = onServer(() -> new int[]{MathHelper.floor_double(player().posX), MathHelper.floor_double(player().boundingBox.minY),
			MathHelper.floor_double(player().posZ)});
		onServer(() -> {
			for (int[] at : blocks) {
				world().setBlock(feet[0] + at[0], feet[1] + at[1], feet[2] + at[2], Blocks.wool, 2, 3);
			}
		});
		look(0.0F, 0.0F);
		ticks(10);
		screenshot("calibration.first_person");
		cameraView(1);
		screenshot("calibration.third_person_back");
		cameraView(0);
		onServer(() -> {
			for (int[] at : blocks) {
				world().setBlockToAir(feet[0] + at[0], feet[1] + at[1], feet[2] + at[2]);
			}
		});
		setHeld(new ItemStack(ModItems.SHIELD.get()));
	}

	/**
	 * A pack in the game's resourcepacks painting EFR's shield texture and magenta wool magenta, put on top of the
	 * selected ones.
	 */
	private void magentaPack() throws Exception {
		File pack = onClient(() -> new File(mc().getResourcePackRepository().getDirResourcepacks(), "unittas_magenta"));
		File textures = new File(pack, "assets/etfuturum/textures/entity/shield");
		File blocks = new File(pack, "assets/minecraft/textures/blocks");
		if (!textures.isDirectory() && !textures.mkdirs() || !blocks.isDirectory() && !blocks.mkdirs()) {
			fail("can't make " + textures + " and " + blocks);
		}
		Files.write(new File(pack, "pack.mcmeta").toPath(),
			"{\"pack\":{\"pack_format\":1,\"description\":\"unitTAS: the shield and magenta wool in magenta\"}}\n".getBytes(StandardCharsets.UTF_8));
		BufferedImage magenta = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 64; x++) {
			for (int y = 0; y < 64; y++) {
				magenta.setRGB(x, y, 0xFFFF00FF);
			}
		}
		ImageIO.write(magenta, "png", new File(textures, "base_nopattern.png"));
		ImageIO.write(magenta, "png", new File(textures, "base.png"));
		ImageIO.write(magenta.getSubimage(0, 0, 16, 16), "png", new File(blocks, "wool_colored_magenta.png"));
		onClient(() -> {
			ResourcePackRepository repository = mc().getResourcePackRepository();
			repository.updateRepositoryEntriesAll();
			List<ResourcePackRepository.Entry> selected = new ArrayList<>(repository.getRepositoryEntries());
			for (ResourcePackRepository.Entry entry : repository.getRepositoryEntriesAll()) {
				if (entry.getResourcePackName().equals("unittas_magenta")) {
					// last is on top (1.7.10 reads the packs from the end), above modern-textures and the like
					selected.add(entry);
				}
			}
			repository.func_148527_a(selected);
			mc().refreshResources();
		});
		ticks(10);
	}
}
