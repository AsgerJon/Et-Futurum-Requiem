package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.client.options.ToggleKeys;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldSettings;

import java.util.Arrays;
import java.util.List;

/**
 * Modern options, stage 2a (SPEC-modern-movement.md §54.11): 26.3's Hold/Toggle for Sneak, Sprint, Attack/Destroy
 * and Use Item/Place Block. In Toggle a press flips the key and its release does nothing; a screen lets go of the
 * keys, and closing it puts toggled keyboard keys back down (never Use, and Attack only on a key); a respawn lets go
 * of them, though the death screen's mark brings sneak back once the respawn closes it. Each test starts with all
 * four in Hold.
 */
@TasSuite("options")
public class ToggleKeyTests extends ScreenTestCase {

	private boolean[] toggles;

	@Override
	protected void setUp() {
		needs(McFlyConfig.modernOptions, "modernOptions");
		toggles = onClient(() -> new boolean[] {ToggleKeys.crouch, ToggleKeys.sprint, ToggleKeys.attack, ToggleKeys.use});
		setToggles(false, false, false, false);
	}

	@Override
	protected void tearDown() {
		onClient(() -> {
			if (mc().thePlayer.getHealth() <= 0.0F) {
				// a death test that failed before its respawn
				mc().thePlayer.respawnPlayer();
			}
			mc().displayGuiScreen(null);
			ToggleKeys.crouch = toggles[0];
			ToggleKeys.sprint = toggles[1];
			ToggleKeys.attack = toggles[2];
			ToggleKeys.use = toggles[3];
			KeyBinding.unPressAllKeys();
		});
		ticks(2);
	}

	// ---------------------------------------------------------------- the options

	public void testControlsHasTheToggles() {
		openOptions();
		click("Controls...");
		List<String> keys = Arrays.asList("Sneak", "Sprint", "Attack/Destroy", "Use Item/Place Block");
		// the four right after the two links; the whole screen is OptionsTests' and ControlsTests' (stage 2b)
		assertEqual(keys, layout().subList(2, 6));
		for (String key : keys) {
			assertEqual("Hold", valueOf(key), key);
		}
		click("Sneak");
		assertEqual("Toggle", valueOf("Sneak"));
		assertTrue(onClient(() -> ToggleKeys.crouch), "Sneak in Toggle");
	}

	public void testTogglesSavedInOptionsTxt() {
		onClient(() -> {
			ToggleKeys.crouch = true;
			ToggleKeys.attack = true;
			mc().gameSettings.saveOptions();
		});
		List<String> lines = optionsLines();
		assertIn("toggleCrouch:true", lines);
		assertIn("toggleSprint:false", lines);
		assertIn("toggleAttack:true", lines);
		assertIn("toggleUse:false", lines);
		onClient(() -> {
			ToggleKeys.crouch = false;
			ToggleKeys.sprint = true;
			ToggleKeys.attack = false;
			ToggleKeys.use = true;
			reloadOptionsWithItsToggles();
		});
		assertTrue(onClient(() -> ToggleKeys.crouch && !ToggleKeys.sprint && ToggleKeys.attack && !ToggleKeys.use), "loaded back as saved");
	}

	// ---------------------------------------------------------------- the keys

	public void testToggledSneakStaysDown() {
		setToggles(true, false, false, false);
		tap(sneak());
		ticks(3);
		assertTrue(down(sneak()), "the sneak key stays down after it is let go");
		assertTrue(onClient(() -> mc().thePlayer.isSneaking()), "sneaking");
		tap(sneak());
		ticks(3);
		assertFalse(down(sneak()), "the next tap lets it go");
		assertFalse(onClient(() -> mc().thePlayer.isSneaking()), "not sneaking");
	}

	/** Hold, the default, is the plain key. */
	public void testHoldSneakUnchanged() {
		tap(sneak());
		ticks(3);
		assertFalse(down(sneak()), "in Hold the key is up once let go");
		assertFalse(onClient(() -> mc().thePlayer.isSneaking()), "not sneaking");
	}

	public void testToggledSprint() {
		setToggles(false, true, false, false);
		tap(sprint());
		assertTrue(down(sprint()), "the sprint key stays down");
		KeyBinding forward = onClient(() -> mc().gameSettings.keyBindForward);
		keyDown(forward);
		try {
			ticks(10);
			assertTrue(onClient(() -> mc().thePlayer.isSprinting()), "walking forward sprints");
		} finally {
			keyUp(forward);
		}
	}

	public void testToggledSneakAcrossTheInventory() {
		setToggles(true, false, false, false);
		tap(sneak());
		assertTrue(down(sneak()), "toggled down");
		onClient(() -> mc().displayGuiScreen(new GuiInventory(mc().thePlayer)));
		ticks(3);
		assertFalse(down(sneak()), "let go while the inventory is open");
		onClient(() -> mc().displayGuiScreen(null));
		ticks(3);
		assertTrue(down(sneak()), "down again once it closes");
		assertTrue(onClient(() -> mc().thePlayer.isSneaking()), "sneaking again");
	}

	/** Use Item never comes back after a screen, toggled or not (26.3's use key doesn't restore). */
	public void testToggledUseEndsAtAScreen() {
		setToggles(false, false, false, true);
		tap(use());
		assertTrue(down(use()), "toggled down");
		openAndCloseTheInventory();
		assertFalse(down(use()), "up after the screen");
	}

	/** Attack comes back only when bound to a keyboard key; on the mouse (the default) a screen ends it. */
	public void testToggledAttackOnTheMouseEndsAtAScreen() {
		if (onClient(() -> attack().getKeyCode() >= 0)) {
			skipTest("Attack is bound to a keyboard key here");
		}
		setToggles(false, false, true, false);
		tap(attack());
		assertTrue(down(attack()), "toggled down");
		openAndCloseTheInventory();
		assertFalse(down(attack()), "up after the screen");
	}

	/** One tap in Toggle keeps mining: dirt by hand takes about 20 ticks, far more than the tap. */
	public void testToggledAttackKeepsBreaking() {
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			world().setBlock(0, 4, 1, Blocks.dirt);
		});
		waitUntilOnClient("the dirt on the client", () -> mc().theWorld.getBlock(0, 4, 1) == Blocks.dirt, 20);
		// from the eyes at 5.62, 40 degrees down meets the dirt's top face
		look(0.0F, 40.0F);
		setToggles(false, false, true, false);
		tap(attack());
		try {
			waitUntil("the dirt broken", () -> world().getBlock(0, 4, 1) == Blocks.air, 80);
		} finally {
			// the key stays down and would mine on into the grass by the spawn point
			setToggles(false, false, false, false);
			onClient(KeyBinding::unPressAllKeys);
			onServer(() -> {
				world().setBlockToAir(0, 4, 1);
				world().setBlock(0, 3, 1, Blocks.grass);
			});
		}
	}

	public void testToggledUseKeepsTheBowDrawn() {
		setHeld(new ItemStack(Items.bow));
		waitUntilOnClient("the bow in hand", () -> mc().thePlayer.getHeldItem() != null, 20);
		setToggles(false, false, false, true);
		tap(use());
		ticks(20);
		assertTrue(onClient(() -> mc().thePlayer.isUsingItem()), "still drawing 20 ticks after the tap");
		tap(use());
		ticks(3);
		assertFalse(onClient(() -> mc().thePlayer.isUsingItem()), "the next tap lets the arrow go");
	}

	/**
	 * 26.3: the death screen marks the toggled sneak as it lets it go; the respawn puts the toggle keys up but keeps the
	 * mark, and the death screen closing once the new player is in puts sneak down again.
	 */
	public void testToggledSneakAfterTheDeathScreen() {
		setToggles(true, false, false, false);
		tap(sneak());
		assertTrue(down(sneak()), "toggled down");
		onServer(() -> player().attackEntityFrom(DamageSource.outOfWorld, 1000.0F));
		waitUntilOnClient("the death screen", () -> mc().currentScreen instanceof GuiGameOver, 40);
		assertFalse(down(sneak()), "let go by the death screen");
		// its buttons wake after 20 ticks
		ticks(25);
		click("Respawn");
		waitUntilOnClient("the new player in the world", () -> mc().currentScreen == null && mc().thePlayer.getHealth() > 0.0F, 100);
		ticks(3);
		assertTrue(down(sneak()), "sneak down again after the respawn, as 26.3's death screen leaves it");
	}

	// ---------------------------------------------------------------- helpers

	private void setToggles(boolean crouch, boolean sprint, boolean attack, boolean use) {
		onClient(() -> {
			ToggleKeys.crouch = crouch;
			ToggleKeys.sprint = sprint;
			ToggleKeys.attack = attack;
			ToggleKeys.use = use;
		});
	}

	private void openAndCloseTheInventory() {
		onClient(() -> mc().displayGuiScreen(new GuiInventory(mc().thePlayer)));
		ticks(3);
		onClient(() -> mc().displayGuiScreen(null));
		ticks(3);
	}

	private boolean down(KeyBinding key) {
		return onClient(key::getIsKeyPressed);
	}

	private static KeyBinding sneak() {
		return mc().gameSettings.keyBindSneak;
	}

	private static KeyBinding sprint() {
		return mc().gameSettings.keyBindSprint;
	}

	private static KeyBinding attack() {
		return mc().gameSettings.keyBindAttack;
	}

	private static KeyBinding use() {
		return mc().gameSettings.keyBindUseItem;
	}
}
