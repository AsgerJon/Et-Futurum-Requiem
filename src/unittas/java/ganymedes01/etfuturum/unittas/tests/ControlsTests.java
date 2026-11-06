package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.client.options.ControlOptions;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Modern options, stage 2b (SPEC-modern-movement.md §54.12): the rest of 26.3's Controls (Auto-Jump, Sprint Window,
 * Operator Items Tab) and Mouse Settings (Scroll Sensitivity, Discrete Scrolling, Invert Mouse X). The lanes run south
 * from z 0 at x 60 (sprinting) and x 62 (steps), cleared for every test.
 */
@TasSuite("options")
public class ControlsTests extends ScreenTestCase {

	private static final int SPRINT_LANE = 60, STEP_LANE = 62, LENGTH = 12;

	private Object[] saved;

	@Override
	protected void setUp() {
		needs(McFlyConfig.modernOptions, "modernOptions");
		saved = onClient(() -> new Object[] {ControlOptions.autoJump, ControlOptions.sprintWindow, ControlOptions.operatorItemsTab,
			ControlOptions.invertMouseX, ControlOptions.scrollSensitivity, ControlOptions.discreteScrolling, mc().thePlayer.inventory.currentItem});
		onClient(() -> {
			ControlOptions.autoJump = false;
			ControlOptions.sprintWindow = 7;
			ControlOptions.operatorItemsTab = false;
			ControlOptions.invertMouseX = false;
			ControlOptions.scrollSensitivity = 1.0D;
			ControlOptions.discreteScrolling = false;
		});
		onServer(() -> {
			for (int x = SPRINT_LANE - 1; x <= STEP_LANE + 1; x++) {
				for (int z = -1; z <= LENGTH; z++) {
					world().setBlock(x, 3, z, Blocks.grass, 0, 2);
					for (int y = 4; y <= 8; y++) {
						world().setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
		});
	}

	@Override
	protected void tearDown() {
		onClient(() -> {
			mc().displayGuiScreen(null);
			KeyBinding.unPressAllKeys();
			ControlOptions.autoJump = (Boolean) saved[0];
			ControlOptions.sprintWindow = (Integer) saved[1];
			ControlOptions.operatorItemsTab = (Boolean) saved[2];
			ControlOptions.invertMouseX = (Boolean) saved[3];
			ControlOptions.scrollSensitivity = (Double) saved[4];
			ControlOptions.discreteScrolling = (Boolean) saved[5];
			mc().thePlayer.inventory.currentItem = (Integer) saved[6];
		});
		onServer(() -> {
			if (!player().capabilities.isCreativeMode) {
				player().setGameType(WorldSettings.GameType.CREATIVE);
			}
		});
		ticks(2);
	}

	// ---------------------------------------------------------------- the screens and options.txt

	public void testControlsLayout() {
		openOptions();
		click("Controls...");
		assertEqual(Arrays.asList("Mouse Settings...", "Key Binds...", "Sneak", "Sprint", "Attack/Destroy", "Use Item/Place Block",
			"Auto-Jump", "Sprint Window", "Operator Items Tab", "Done"), layout());
		assertEqual("OFF", valueOf("Auto-Jump"));
		assertEqual("7", valueOf("Sprint Window"));
		assertEqual("OFF", valueOf("Operator Items Tab"));
		click("Auto-Jump");
		assertEqual("ON", valueOf("Auto-Jump"));
		assertTrue(onClient(() -> ControlOptions.autoJump), "Auto-Jump on");
	}

	public void testMouseSettingsLayout() {
		openOptions();
		click("Controls...");
		click("Mouse Settings...");
		assertEqual(Arrays.asList("Sensitivity", "Scroll Sensitivity", "Discrete Scrolling", "Invert Mouse X", "Invert Mouse Y",
			"Touchscreen Mode", "Done"), layout());
		assertEqual("1.00", valueOf("Scroll Sensitivity"));
		assertEqual("OFF", valueOf("Discrete Scrolling"));
		assertEqual("OFF", valueOf("Invert Mouse X"));
	}

	public void testSavedInOptionsTxt() {
		onClient(() -> {
			ControlOptions.autoJump = true;
			ControlOptions.sprintWindow = 3;
			ControlOptions.operatorItemsTab = true;
			ControlOptions.invertMouseX = true;
			ControlOptions.scrollSensitivity = 0.5D;
			ControlOptions.discreteScrolling = true;
			mc().gameSettings.saveOptions();
		});
		List<String> lines = optionsLines();
		for (String line : new String[] {"autoJump:true", "sprintWindow:3", "operatorItemsTab:true", "invertXMouse:true",
			"mouseWheelSensitivity:0.5", "discrete_mouse_scroll:true"}) {
			assertIn(line, lines);
		}
		onClient(() -> {
			ControlOptions.autoJump = false;
			ControlOptions.sprintWindow = 7;
			ControlOptions.operatorItemsTab = false;
			ControlOptions.invertMouseX = false;
			ControlOptions.scrollSensitivity = 1.0D;
			ControlOptions.discreteScrolling = false;
			reloadOptionsWithItsToggles();
		});
		assertTrue(onClient(() -> ControlOptions.autoJump && ControlOptions.sprintWindow == 3 && ControlOptions.operatorItemsTab
			&& ControlOptions.invertMouseX && ControlOptions.scrollSensitivity == 0.5D && ControlOptions.discreteScrolling), "loaded back as saved");
	}

	// ---------------------------------------------------------------- Sprint Window

	/** The second press of forward comes gap + 1 ticks after the first. */
	public void testDoubleTapInTheWindow() {
		assertTrue(doubleTap(7, 2), "window 7, 3 ticks: sprinting");
	}

	public void testShortWindow() {
		assertFalse(doubleTap(2, 3), "window 2, 4 ticks: not sprinting");
	}

	public void testLongWindow() {
		assertTrue(doubleTap(10, 7), "window 10, 8 ticks: sprinting");
	}

	public void testWindowOff() {
		assertFalse(doubleTap(0, 1), "window off, 2 ticks: not sprinting");
	}

	// ---------------------------------------------------------------- Auto-Jump

	public void testAutoJumpUpAStep() {
		steps(1);
		Walk walk = walk(true, false, 30);
		assertTrue(walk.lastFeet >= 4.99D, "on top of the step, feet at " + walk.lastFeet);
	}

	public void testNoAutoJumpWhenOff() {
		steps(1);
		Walk walk = walk(false, false, 30);
		assertTrue(walk.highestFeet < 4.1D, "no jump, feet up to " + walk.highestFeet);
	}

	public void testNoAutoJumpUpASlab() {
		onServer(() -> {
			for (int z = 3; z < LENGTH; z++) {
				world().setBlock(STEP_LANE, 4, z, Blocks.stone_slab, 0, 2);
			}
		});
		Walk walk = walk(true, false, 30);
		assertTrue(walk.lastFeet >= 4.49D, "walked up the slab, feet at " + walk.lastFeet);
		assertTrue(walk.highestFeet < 4.9D, "without a jump, feet up to " + walk.highestFeet);
	}

	public void testNoAutoJumpUnderACeiling() {
		steps(1);
		onServer(() -> {
			for (int z = -1; z < 3; z++) {
				world().setBlock(STEP_LANE, 6, z, Blocks.stone, 0, 2);
			}
		});
		Walk walk = walk(true, false, 30);
		assertTrue(walk.highestFeet < 4.1D, "no jump, feet up to " + walk.highestFeet);
	}

	public void testNoAutoJumpWhileSneaking() {
		steps(1);
		Walk walk = walk(true, true, 50);
		assertTrue(walk.highestFeet < 4.1D, "no jump, feet up to " + walk.highestFeet);
	}

	/** 1.7.10 toggles creative flight on two jump-key presses within 7 ticks; auto-jumps aren't key presses. */
	public void testAutoJumpDoesntFly() {
		steps(3);
		Walk walk = walk(true, false, 40);
		assertTrue(walk.highestFeet >= 6.99D, "up all three steps, feet up to " + walk.highestFeet);
		assertFalse(walk.flew, "flying");
	}

	// ---------------------------------------------------------------- Operator Items Tab

	public void testOperatorTab() {
		onClient(() -> ControlOptions.operatorItemsTab = true);
		assertTrue(onClient(() -> operatorTab() != null), "a creative tab named op");
		assertTrue(onClient(ControlOptions::operatorTabShown), "shown with the option, in creative, with cheats");
		List<ItemStack> items = onClient(() -> {
			List<ItemStack> list = new ArrayList<>();
			operatorTab().displayAllReleventItems(list);
			return list;
		});
		Item commandBlock = Item.getItemFromBlock(Blocks.command_block);
		assertTrue(items.stream().anyMatch(stack -> stack.getItem() == commandBlock), "the command block in " + items);
	}

	public void testNoOperatorTabWhenOff() {
		assertFalse(onClient(ControlOptions::operatorTabShown), "shown with the option off");
	}

	public void testNoOperatorTabInSurvival() {
		onClient(() -> ControlOptions.operatorItemsTab = true);
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		waitUntilOnClient("survival on the client", () -> !mc().playerController.isInCreativeMode(), 20);
		assertFalse(onClient(ControlOptions::operatorTabShown), "shown in survival");
	}

	// ---------------------------------------------------------------- Invert Mouse X

	public void testInvertMouseX() {
		assertEqual(5.0F, onClient(() -> ControlOptions.turnYaw(5.0F)), "off");
		onClient(() -> ControlOptions.invertMouseX = true);
		assertEqual(-5.0F, onClient(() -> ControlOptions.turnYaw(5.0F)), "on");
	}

	// ---------------------------------------------------------------- Scroll Sensitivity, Discrete Scrolling

	/** 1.7.10's units: 120 a notch; turning down (negative) moves to the next slot. */
	public void testHalfSensitivityNeedsTwoNotches() {
		assertEqual(Arrays.asList(4, 5), scrollDown(0.5D, false, -120, -120));
	}

	public void testSmoothScrollGathers() {
		assertEqual(Arrays.asList(4, 4, 4, 5), scrollDown(1.0D, false, -30, -30, -30, -30));
	}

	public void testDiscreteScrollCountsEachEvent() {
		assertEqual(Arrays.asList(5, 6), scrollDown(1.0D, true, -30, -30));
	}

	public void testHighSensitivityStillOneSlot() {
		assertEqual(Arrays.asList(5), scrollDown(3.0D, false, -120));
	}

	public void testScreensGetTheScaledAmount() {
		List<Integer> amounts = onClient(() -> {
			ControlOptions.screenWheel(120);
			ControlOptions.scrollSensitivity = 0.5D;
			return Arrays.asList(ControlOptions.screenWheel(-120), ControlOptions.screenWheel(-120));
		});
		assertEqual(Arrays.asList(0, -120), amounts);
	}

	// ---------------------------------------------------------------- helpers

	/**
	 * Window set, forward for one tick, gap ticks without it, then forward held: whether that sprints. The key goes by
	 * the player's own tick count, since keyDown, keyUp and ticks each cost a tick more or less than they say.
	 */
	private boolean doubleTap(int window, int gap) {
		onClient(() -> ControlOptions.sprintWindow = window);
		standAtTheStart(SPRINT_LANE);
		KeyBinding forward = onClient(() -> mc().gameSettings.keyBindForward);
		// a step runs at the end of a tick: the player sees the key from the next one on
		int start = onClient(() -> {
			KeyBinding.setKeyBindState(forward.getKeyCode(), true);
			KeyBinding.onTick(forward.getKeyCode());
			return mc().thePlayer.ticksExisted;
		});
		try {
			atTick(start + 1, () -> KeyBinding.setKeyBindState(forward.getKeyCode(), false));
			atTick(start + 1 + gap, () -> {
				KeyBinding.setKeyBindState(forward.getKeyCode(), true);
				KeyBinding.onTick(forward.getKeyCode());
			});
			ticks(2);
			return onClient(() -> mc().thePlayer.isSprinting());
		} finally {
			keyUp(forward);
			ticks(2);
		}
	}

	/** Runs the step at the end of the client tick that brings the player's ticksExisted to tick. */
	private void atTick(int tick, Runnable step) {
		for (;;) {
			int now = onClient(() -> {
				int ticked = mc().thePlayer.ticksExisted;
				if (ticked == tick) {
					step.run();
				}
				return ticked;
			});
			if (now == tick) {
				return;
			}
			if (now > tick) {
				throw new AssertionError("the test missed the player's tick " + tick + " (it was at " + now + ")");
			}
		}
	}

	/** count steps, one block higher each, from z 3 on; the last goes on to the end of the lane. */
	private void steps(int count) {
		onServer(() -> {
			for (int z = 3; z < LENGTH; z++) {
				for (int y = 4; y < 4 + Math.min(count, z - 2); y++) {
					world().setBlock(STEP_LANE, y, z, Blocks.stone, 0, 2);
				}
			}
		});
	}

	private static final class Walk {
		double highestFeet;
		double lastFeet;
		boolean flew;
	}

	/** Walks south along the step lane for this many ticks: the highest feet, the last, and whether it flew. */
	private Walk walk(boolean autoJump, boolean sneaking, int ticks) {
		onClient(() -> ControlOptions.autoJump = autoJump);
		standAtTheStart(STEP_LANE);
		KeyBinding forward = onClient(() -> mc().gameSettings.keyBindForward);
		KeyBinding sneak = onClient(() -> mc().gameSettings.keyBindSneak);
		Walk walk = new Walk();
		walk.highestFeet = feet();
		if (sneaking) {
			keyDown(sneak);
		}
		keyDown(forward);
		try {
			// a client step runs at the next tick, so one step a pass is one tick a pass
			for (int i = 0; i < ticks; i++) {
				double[] now = onClient(() -> new double[] {mc().thePlayer.boundingBox.minY, mc().thePlayer.capabilities.isFlying ? 1 : 0});
				walk.highestFeet = Math.max(walk.highestFeet, now[0]);
				walk.flew |= now[1] > 0;
			}
		} finally {
			keyUp(forward);
			keyUp(sneak);
		}
		ticks(2);
		walk.lastFeet = feet();
		return walk;
	}

	private double feet() {
		return onClient(() -> mc().thePlayer.boundingBox.minY);
	}

	/** On the ground at the lane's first block, facing south, not flying. */
	private void standAtTheStart(int lane) {
		onServer(() -> {
			player().capabilities.isFlying = false;
			player().sendPlayerAbilities();
			player().playerNetServerHandler.setPlayerLocation(lane + 0.5D, 4.0D, 0.5D, 0.0F, 0.0F);
		});
		onClient(() -> {
			mc().thePlayer.capabilities.isFlying = false;
			mc().thePlayer.setSprinting(false);
		});
		look(0.0F, 0.0F);
		waitUntilOnClient("standing at the lane's start",
			() -> mc().thePlayer.onGround && Math.abs(mc().thePlayer.posX - (lane + 0.5D)) < 0.01D && Math.abs(mc().thePlayer.posZ - 0.5D) < 0.01D, 40);
		ticks(2);
	}

	/** Slot 4 after a turn up that empties what the hotbar had gathered, then these wheel events: the slot after each. */
	private List<Integer> scrollDown(double sensitivity, boolean discrete, int... wheels) {
		return onClient(() -> {
			InventoryPlayer inventory = mc().thePlayer.inventory;
			ControlOptions.scrollSensitivity = 1.0D;
			ControlOptions.discreteScrolling = false;
			ControlOptions.scrollHotbar(inventory, 120);
			inventory.currentItem = 4;
			ControlOptions.scrollSensitivity = sensitivity;
			ControlOptions.discreteScrolling = discrete;
			List<Integer> slots = new ArrayList<>();
			for (int wheel : wheels) {
				ControlOptions.scrollHotbar(inventory, wheel);
				slots.add(inventory.currentItem);
			}
			return slots;
		});
	}

	/** EFR's Operator Utilities tab, by its label, or null. */
	private static CreativeTabs operatorTab() {
		for (CreativeTabs tab : CreativeTabs.creativeTabArray) {
			if (tab != null && "op".equals(tab.getTabLabel())) {
				return tab;
			}
		}
		return null;
	}
}
