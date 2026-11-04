package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.client.gui.options.IOptionsLayout;
import ganymedes01.etfuturum.client.options.ControlOptions;
import ganymedes01.etfuturum.client.options.ToggleKeys;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiOptionsRowList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.EnumDifficulty;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Tests that read and click options screens as the mouse would, and save and load options.txt (SPEC-modern-movement.md
 * §54.10). Screens are read and clicked on the client only: an open options screen pauses the integrated server.
 */
public abstract class ScreenTestCase extends TasTestCase {

	/**
	 * options.txt read again, keeping the settings the unitTAS run holds while it goes (pause on lost focus off, Easy,
	 * the Hold/Toggle keys in Hold, the controls of §54.12 at 26.3's defaults), which the file has as the player set
	 * them. Call on the client.
	 */
	protected static void reloadOptions() {
		boolean[] toggles = {ToggleKeys.crouch, ToggleKeys.sprint, ToggleKeys.attack, ToggleKeys.use};
		Object[] controls = {ControlOptions.autoJump, ControlOptions.sprintWindow, ControlOptions.operatorItemsTab,
			ControlOptions.invertMouseX, ControlOptions.scrollSensitivity, ControlOptions.discreteScrolling};
		reloadOptionsWithItsToggles();
		ToggleKeys.crouch = toggles[0];
		ToggleKeys.sprint = toggles[1];
		ToggleKeys.attack = toggles[2];
		ToggleKeys.use = toggles[3];
		ControlOptions.autoJump = (Boolean) controls[0];
		ControlOptions.sprintWindow = (Integer) controls[1];
		ControlOptions.operatorItemsTab = (Boolean) controls[2];
		ControlOptions.invertMouseX = (Boolean) controls[3];
		ControlOptions.scrollSensitivity = (Double) controls[4];
		ControlOptions.discreteScrolling = (Boolean) controls[5];
	}

	/**
	 * options.txt read again, the Hold/Toggle keys and the controls of §54.12 included (for the tests of their saving).
	 * Call on the client.
	 */
	protected static void reloadOptionsWithItsToggles() {
		boolean pauseOnLostFocus = mc().gameSettings.pauseOnLostFocus;
		EnumDifficulty difficulty = mc().gameSettings.difficulty;
		mc().gameSettings.loadOptions();
		mc().gameSettings.pauseOnLostFocus = pauseOnLostFocus;
		mc().gameSettings.difficulty = difficulty;
	}

	// ---------------------------------------------------------------- reading and clicking screens

	protected void openOptions() {
		onClient(() -> mc().displayGuiScreen(new GuiIngameMenu()));
		ticks(2);
		click("Options...");
	}

	/** The open screen's entries as names: a button's label up to its value, a header in brackets. */
	protected List<String> layout() {
		ticks(1);
		return onClient(() -> names(mc().currentScreen));
	}

	/** The value the named button shows (its label after the name). */
	protected String valueOf(String name) {
		return onClient(() -> {
			String label = EnumChatFormatting.getTextWithoutFormattingCodes(find(mc().currentScreen, name).displayString);
			int colon = label.indexOf(": ");
			return colon < 0 ? "" : label.substring(colon + 2);
		});
	}

	/** Clicks the open screen's button of that name at its middle and lets go, as the mouse would. */
	protected void click(String name) {
		// a frame first: a list places its rows' buttons when it draws them
		ticks(2);
		onClient(() -> {
			GuiScreen screen = mc().currentScreen;
			press(screen, find(screen, name));
		});
		ticks(2);
	}

	protected static void press(GuiScreen screen, GuiButton button) {
		assertTrue(button.enabled && button.visible, "'" + name(button) + "' can be clicked");
		assertTrue(button.yPosition >= 0 && button.yPosition + button.height <= screen.height, "'" + name(button) + "' is in view");
		int x = button.xPosition + button.width / 2;
		int y = button.yPosition + button.height / 2;
		call(screen, new String[] {"mouseClicked", "func_73864_a"}, x, y, 0);
		// let go at once, or a slider keeps following the real mouse
		call(screen, new String[] {"mouseMovedOrUp", "func_146286_b"}, x, y, 0);
	}

	protected static GuiButton find(GuiScreen screen, String name) {
		for (Object entry : entries(screen)) {
			if (entry instanceof GuiButton button && name.equals(name(button))) {
				return button;
			}
		}
		throw new AssertionError("no button '" + name + "' on " + (screen == null ? "no screen" : screen.getClass().getSimpleName())
			+ "; it has " + names(screen));
	}

	protected static List<String> names(GuiScreen screen) {
		List<String> names = new ArrayList<>();
		if (screen == null) {
			return names;
		}
		for (Object entry : entries(screen)) {
			names.add(entry instanceof GuiButton button ? name(button) : "[" + entry + "]");
		}
		return names;
	}

	/** A label up to its value: "FOV: Normal" is FOV. */
	protected static String name(GuiButton button) {
		String label = EnumChatFormatting.getTextWithoutFormattingCodes(button.displayString);
		int colon = label.indexOf(": ");
		return colon < 0 ? label : label.substring(0, colon);
	}

	/**
	 * A screen's buttons and headers, top to bottom: from the screen itself when it lists them (EFR's modern screens),
	 * else 1.7.10's row list (Video Settings), then the screen's own buttons by position.
	 */
	protected static List<Object> entries(GuiScreen screen) {
		if (screen instanceof IOptionsLayout layout) {
			return layout.etfu$entries();
		}
		List<Object> entries = new ArrayList<>();
		for (Field field : fields(screen.getClass())) {
			// Video Settings declares its row list as a GuiListExtended
			if (get(field, screen) instanceof GuiOptionsRowList list) {
				for (Object row : (List<?>) get(field(GuiOptionsRowList.class, "field_148184_k"), list)) {
					for (String side : new String[] {"field_148323_b", "field_148324_c"}) {
						Object button = get(field(GuiOptionsRowList.Row.class, side), row);
						if (button != null) {
							entries.add(button);
						}
					}
				}
			}
		}
		entries.addAll(buttons(screen));
		return entries;
	}

	/** The screen's buttonList, top to bottom and left to right. */
	@SuppressWarnings("unchecked")
	protected static List<GuiButton> buttons(GuiScreen screen) {
		Field field;
		try {
			field = GuiScreen.class.getDeclaredField("buttonList");
		} catch (NoSuchFieldException e) {
			field = field(GuiScreen.class, "field_146292_n");
		}
		List<GuiButton> buttons = new ArrayList<>((List<GuiButton>) get(field, screen));
		buttons.sort(Comparator.<GuiButton>comparingInt(b -> b.yPosition).thenComparingInt(b -> b.xPosition));
		return buttons;
	}

	protected static List<Field> fields(Class<?> type) {
		List<Field> fields = new ArrayList<>();
		for (Class<?> c = type; c != null && c != GuiScreen.class; c = c.getSuperclass()) {
			fields.addAll(Arrays.asList(c.getDeclaredFields()));
		}
		return fields;
	}

	protected static Field field(Class<?> owner, String name) {
		try {
			return owner.getDeclaredField(name);
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException(owner.getSimpleName() + " has no field " + name, e);
		}
	}

	protected static Object get(Field field, Object owner) {
		try {
			field.setAccessible(true);
			return field.get(owner);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	/** GuiScreen's mouse handlers (dev names first, then the obfuscated ones). */
	protected static void call(GuiScreen screen, String[] names, int x, int y, int button) {
		for (String name : names) {
			try {
				Method method = GuiScreen.class.getDeclaredMethod(name, int.class, int.class, int.class);
				method.setAccessible(true);
				method.invoke(screen, x, y, button);
				return;
			} catch (NoSuchMethodException e) {
				// the other name
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Can't click on " + screen, e);
			}
		}
		throw new IllegalStateException("GuiScreen has none of " + Arrays.toString(names));
	}

	// ---------------------------------------------------------------- options.txt

	/** Not a client step: tearDown asks for it from inside one (the game folder is fixed, so any thread may read it). */
	protected static File optionsFile() {
		return new File(mc().mcDataDir, "options.txt");
	}

	protected List<String> optionsLines() {
		try {
			return Files.readAllLines(optionsFile().toPath(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Can't read options.txt", e);
		}
	}

	protected void writeOptionsLines(List<String> lines) {
		try {
			Files.write(optionsFile().toPath(), lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Can't write options.txt", e);
		}
	}

	protected static void needs(boolean on, String option) {
		if (!on) {
			skipTest(option + " is off in mixins.cfg");
		}
	}
}
