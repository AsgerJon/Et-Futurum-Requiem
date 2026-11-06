package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.client.gui.IGuiScaleWheel;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonLanguage;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Modern options, stage 1 (SPEC-modern-movement.md §54.10): the screens as 26.3's, holding today's options, and EFR's
 * client options kept in options.txt under 26.3's names. Screens are read and clicked on the client only: an open
 * options screen pauses the integrated server. Each test puts the player's options.txt and the values it touched
 * back.
 */
@TasSuite("options")
public class OptionsTests extends ScreenTestCase {

	/** EFR's client options under their 26.3 names in options.txt. */
	private static final List<String> SAVED_NAMES = Arrays.asList("fovEffectScale", "attackIndicator", "autoSuggestions", "mainHand",
		"modelPart_cape", "modelPart_jacket", "modelPart_left_sleeve", "modelPart_right_sleeve", "modelPart_left_pants_leg",
		"modelPart_right_pants_leg", "modelPart_hat");

	private byte[] optionsTxt;
	private float fovEffectScale;
	private String attackIndicator;
	private boolean commandSuggestions;
	private boolean mainHandLeft;
	private boolean[] skinParts;

	/**
	 * A copy of the player's options.txt on disk while a test runs: if a run dies before tearDown puts the file back,
	 * the next run's setUp puts it back from here first.
	 */
	private static File backupFile() {
		return new File(mc().mcDataDir, "options.txt.unittas-backup");
	}

	@Override
	protected void setUp() {
		try {
			if (backupFile().isFile()) {
				Files.copy(backupFile().toPath(), optionsFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			optionsTxt = Files.readAllBytes(optionsFile().toPath());
			Files.write(backupFile().toPath(), optionsTxt);
		} catch (IOException e) {
			throw new IllegalStateException("Can't back up options.txt", e);
		}
		onClient(() -> {
			fovEffectScale = McFlyConfig.fovEffectScale;
			attackIndicator = McFlyConfig.attackIndicator;
			commandSuggestions = McFlyConfig.commandSuggestions;
			mainHandLeft = McFlyConfig.mainHandLeft;
			skinParts = McFlyConfig.skinPartsShown();
		});
	}

	@Override
	protected void tearDown() {
		// the screen first: closing some screens saves the options
		onClient(() -> mc().displayGuiScreen(null));
		ticks(1);
		onClient(() -> {
			McFlyConfig.fovEffectScale = fovEffectScale;
			McFlyConfig.attackIndicator = attackIndicator;
			McFlyConfig.mainHandLeft = mainHandLeft;
			setSkinParts(skinParts);
			if (McFlyConfig.commandSuggestions != commandSuggestions) {
				// a click on the screen saved it (to functions.cfg, or to options.txt, which is put back below)
				McFlyConfig.saveCommandSuggestions(commandSuggestions);
			}
		});
		// options.txt last, since saving any option writes it
		try {
			Files.write(optionsFile().toPath(), optionsTxt);
			Files.delete(backupFile().toPath());
		} catch (IOException e) {
			throw new IllegalStateException("Can't put options.txt back (the copy is in options.txt.unittas-backup)", e);
		}
		onClient(OptionsTests::reloadOptions);
		ticks(1);
	}

	// ---------------------------------------------------------------- the screens

	public void testOptionsScreen() {
		openOptions();
		assertEqual(optionsList(), layout());
	}

	public void testControlsScreen() {
		openOptions();
		click("Controls...");
		// with stage 2a's Hold/Toggle keys (§54.11) after the two links, then stage 2b's options (§54.12)
		assertEqual(Arrays.asList("Mouse Settings...", "Key Binds...", "Sneak", "Sprint", "Attack/Destroy", "Use Item/Place Block",
			"Auto-Jump", "Sprint Window", "Operator Items Tab", "Done"), layout());
	}

	public void testMouseSettingsScreen() {
		openOptions();
		click("Controls...");
		click("Mouse Settings...");
		// stage 2b's options (§54.12) in 26.3's order; 1.7.10's Touchscreen Mode at the end
		assertEqual(Arrays.asList("Sensitivity", "Scroll Sensitivity", "Discrete Scrolling", "Invert Mouse X", "Invert Mouse Y",
			"Touchscreen Mode", "Done"), layout());
	}

	/** 1.7.10's key list with its Reset Keys and Done, and none of the options 1.7.10 shows above it. */
	public void testKeyBindsScreen() {
		openOptions();
		click("Controls...");
		click("Key Binds...");
		assertTrue(onClient(() -> keyList(mc().currentScreen) != null), "the key list is there");
		assertEqual(Arrays.asList("Reset Keys", "Done"), layout());
	}

	public void testAccessibilityScreen() {
		openOptions();
		click("Accessibility Settings...");
		assertEqual(accessibilityList(), layout());
	}

	public void testChatSettingsScreen() {
		openOptions();
		click("Chat Settings...");
		assertEqual(chatList(), layout());
	}

	public void testVideoSettingsScreen() {
		openOptions();
		click("Video Settings...");
		assertEqual(videoList(), layout());
	}

	/**
	 * 26.3's row of icon buttons between the menu buttons and Options... | Quit Game: Language and Accessibility, 20 x 20,
	 * 4 apart, centred, 24 under Forge's Realms | Mods row; Options... and Quit Game 24 under it.
	 */
	public void testTitleScreenRow() {
		onClient(() -> mc().displayGuiScreen(new GuiMainMenu()));
		ticks(3);
		onClient(() -> {
			GuiScreen screen = mc().currentScreen;
			GuiButton language = null;
			for (GuiButton button : buttons(screen)) {
				if (button instanceof GuiButtonLanguage) {
					language = button;
				}
			}
			assertIsNotNone(language, "the language button");
			GuiButton mods = find(screen, "Mods");
			GuiButton options = find(screen, "Options...");
			GuiButton quit = find(screen, "Quit Game");
			int middle = screen.width / 2;
			assertEqual(middle - 22, language.xPosition, "the language button's x");
			assertEqual(mods.yPosition + 24, language.yPosition, "the icon row 24 under the Realms | Mods row");
			GuiButton accessibility = iconAt(screen, middle + 2, language.yPosition);
			assertIsNotNone(accessibility, "an accessibility button right of the language button");
			assertEqual(language.yPosition + 24, options.yPosition, "Options... 24 under the icon row");
			assertEqual(options.yPosition, quit.yPosition, "Quit Game beside Options...");
			press(screen, accessibility);
		});
		ticks(2);
		assertEqual(accessibilityList(), layout());
		click("Done");
		assertTrue(onClient(() -> mc().currentScreen instanceof GuiMainMenu), "Done goes back to the title screen");
	}

	// ---------------------------------------------------------------- one setting, kept

	/** Chat Text Opacity is on Accessibility and on Chat Settings, one setting (§54.1). */
	public void testOneSettingOnTwoScreens() {
		// full, so the click on the slider's middle (half) changes it
		onClient(() -> {
			mc().gameSettings.chatOpacity = 1.0F;
		});
		openOptions();
		click("Accessibility Settings...");
		click("Chat Text Opacity");
		String shown = valueOf("Chat Text Opacity");
		assertNotEqual(1.0F, onClient(() -> mc().gameSettings.chatOpacity));
		click("Done");
		click("Chat Settings...");
		assertEqual(shown, valueOf("Chat Text Opacity"));
	}

	public void testValueSurvivesTheScreen() {
		needs(McFlyConfig.modernChat, "modernChat");
		openOptions();
		click("Chat Settings...");
		click("Command Suggestions");
		String shown = valueOf("Command Suggestions");
		onClient(() -> mc().displayGuiScreen(null));
		ticks(1);
		openOptions();
		click("Chat Settings...");
		assertEqual(shown, valueOf("Command Suggestions"));
	}

	/** The four EFR options, changed and saved: options.txt holds each under its 26.3 name; loading puts them back. */
	public void testOptionsSavedInOptionsTxt() {
		onClient(() -> {
			McFlyConfig.fovEffectScale = 0.5F;
			McFlyConfig.attackIndicator = "hotbar";
			McFlyConfig.commandSuggestions = false;
			McFlyConfig.mainHandLeft = true;
			setSkinParts(new boolean[] {true, false, true, true, true, true, true});
			mc().gameSettings.saveOptions();
		});
		List<String> lines = optionsLines();
		assertIn("fovEffectScale:0.5", lines);
		assertIn("attackIndicator:2", lines);
		assertIn("autoSuggestions:false", lines);
		assertIn("mainHand:\"left\"", lines);
		assertIn("modelPart_jacket:false", lines);
		assertIn("modelPart_hat:true", lines);
		onClient(() -> {
			McFlyConfig.fovEffectScale = 0.9F;
			McFlyConfig.attackIndicator = "off";
			McFlyConfig.commandSuggestions = true;
			McFlyConfig.mainHandLeft = false;
			setSkinParts(new boolean[] {true, true, true, true, true, true, true});
			reloadOptions();
		});
		assertAlmostEqual(0.5, onClient(() -> (double) McFlyConfig.fovEffectScale), 1.0E-6);
		assertEqual("hotbar", onClient(() -> McFlyConfig.attackIndicator));
		assertFalse(onClient(() -> McFlyConfig.commandSuggestions), "Command Suggestions loaded");
		assertTrue(onClient(() -> McFlyConfig.mainHandLeft), "the main hand loaded");
		assertFalse(onClient(() -> McFlyConfig.skinPartsShown()[1]), "the jacket loaded");
	}

	/** An options.txt without EFR's names (from before the move): EFR's config values stay, and the next save writes them. */
	public void testOptionsCarryOverFromTheConfig() {
		List<String> kept = new ArrayList<>();
		for (String line : optionsLines()) {
			int colon = line.indexOf(':');
			if (colon < 0 || !SAVED_NAMES.contains(line.substring(0, colon))) {
				kept.add(line);
			}
		}
		writeOptionsLines(kept);
		onClient(() -> {
			McFlyConfig.fovEffectScale = 0.25F;
			McFlyConfig.attackIndicator = "off";
			McFlyConfig.mainHandLeft = true;
			reloadOptions();
		});
		assertAlmostEqual(0.25, onClient(() -> (double) McFlyConfig.fovEffectScale), 1.0E-6);
		assertEqual("off", onClient(() -> McFlyConfig.attackIndicator));
		assertTrue(onClient(() -> McFlyConfig.mainHandLeft), "the main hand kept");
		onClient(() -> mc().gameSettings.saveOptions());
		List<String> lines = optionsLines();
		assertIn("fovEffectScale:0.25", lines);
		assertIn("attackIndicator:0", lines);
		assertIn("mainHand:\"left\"", lines);
	}

	/** GUI Scale by mouse wheel (session item 21) still works on the Video Settings screen. */
	public void testGuiScaleWheel() {
		needs(McFlyConfig.guiScaleWheel, "guiScaleWheel");
		openOptions();
		click("Video Settings...");
		int before = onClient(() -> mc().gameSettings.guiScale);
		click("GUI Scale");
		int clicked = onClient(() -> mc().gameSettings.guiScale);
		assertEqual((before + 1) & 3, clicked, "the click");
		onClient(() -> {
			IGuiScaleWheel screen = (IGuiScaleWheel) mc().currentScreen;
			assertTrue(screen.etfu$wheelChangesGuiScale(), "the wheel armed by the click");
			screen.etfu$guiScaleWheel(120);
		});
		ticks(3);
		assertEqual((clicked + 1) & 3, onClient(() -> mc().gameSettings.guiScale), "the wheel");
	}

	// ---------------------------------------------------------------- what the screens should hold (§54.10)

	/** A 20 x 20 button whose top left corner is there, other than the language button. */
	private static GuiButton iconAt(GuiScreen screen, int x, int y) {
		for (GuiButton button : buttons(screen)) {
			if (!(button instanceof GuiButtonLanguage) && button.xPosition == x && button.yPosition == y && button.width == 20 && button.height == 20) {
				return button;
			}
		}
		return null;
	}

	private static GuiKeyBindingList keyList(GuiScreen screen) {
		if (!(screen instanceof GuiControls)) {
			return null;
		}
		for (Field field : fields(screen.getClass())) {
			if (GuiKeyBindingList.class.isAssignableFrom(field.getType())) {
				return (GuiKeyBindingList) get(field, screen);
			}
		}
		return null;
	}

	private static List<String> optionsList() {
		List<String> list = new ArrayList<>(Arrays.asList("FOV", "Difficulty"));
		if (McFlyConfig.skinCustomization) {
			list.add("Skin Customization...");
		}
		list.addAll(Arrays.asList("Music & Sounds...", "Video Settings...", "Controls...", "Language...", "Chat Settings...",
			"Resource Packs...", "Accessibility Settings...", "Snooper Settings...", "Done"));
		return list;
	}

	private static List<String> accessibilityList() {
		List<String> list = new ArrayList<>(Arrays.asList("Controls...", "Chat Text Opacity", "View Bobbing"));
		if (McFlyConfig.fovEffects) {
			list.add("FOV Effects");
		}
		list.add("Done");
		return list;
	}

	private static List<String> chatList() {
		List<String> list = new ArrayList<>(Arrays.asList("Chat", "Colors", "Web Links", "Prompt on Links", "Chat Text Opacity",
			"Chat Text Size", "Width", "Focused Height", "Unfocused Height"));
		if (McFlyConfig.modernChat) {
			list.add("Command Suggestions");
		}
		list.addAll(Arrays.asList("Show Cape", "Done"));
		return list;
	}

	private static List<String> videoList() {
		List<String> list = new ArrayList<>(Arrays.asList("[Display]", "Max Framerate", "VSync", "GUI Scale", "Fullscreen", "Brightness",
			"[Quality & Performance]", "Graphics", "Render Distance", "Smooth Lighting", "Clouds", "Particles", "Mipmap Levels",
			"Anisotropic Filtering", "3D Anaglyph"));
		if (OpenGlHelper.field_153197_d) {
			list.add("Advanced OpenGL");
		}
		if (McFlyConfig.modernCombat) {
			list.addAll(Arrays.asList("[Preferences]", "Attack Indicator"));
		}
		list.add("Done");
		return list;
	}

	// ---------------------------------------------------------------- EFR's values

	private static void setSkinParts(boolean[] shown) {
		McFlyConfig.skinCape = shown[0];
		McFlyConfig.skinJacket = shown[1];
		McFlyConfig.skinLeftSleeve = shown[2];
		McFlyConfig.skinRightSleeve = shown[3];
		McFlyConfig.skinLeftPantsLeg = shown[4];
		McFlyConfig.skinRightPantsLeg = shown[5];
		McFlyConfig.skinHat = shown[6];
	}

}
