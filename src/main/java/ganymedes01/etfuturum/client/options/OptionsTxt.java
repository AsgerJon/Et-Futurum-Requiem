package ganymedes01.etfuturum.client.options;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * EFR's client options in options.txt (SPEC-modern-movement.md §54.10), under 26.3's names and in its value forms:
 * fovEffectScale, attackIndicator (0 off, 1 crosshair, 2 hotbar), autoSuggestions, mainHand ("left" or "right") and
 * the modelPart_ lines; the Hold/Toggle keys, toggleCrouch, toggleSprint, toggleAttack, toggleUse (§54.11); and
 * autoJump, sprintWindow, operatorItemsTab, invertXMouse, mouseWheelSensitivity, discrete_mouse_scroll (§54.12). A
 * name options.txt lacks leaves the value as EFR's config had it (the toggles: Hold; the controls: 26.3's defaults).
 */
public final class OptionsTxt {

	/** 26.3's model part ids, in EFR's SkinPart order (McFlyConfig.skinPartsShown). */
	private static final String[] MODEL_PARTS = {"cape", "jacket", "left_sleeve", "right_sleeve", "left_pants_leg", "right_pants_leg", "hat"};
	private static final String[] ATTACK_INDICATOR = {"off", "crosshair", "hotbar"};
	/** 26.3's names of the Hold/Toggle settings, in ToggleKeys' order. */
	private static final String[] TOGGLES = {"toggleCrouch", "toggleSprint", "toggleAttack", "toggleUse"};

	private OptionsTxt() {
	}

	/** Appends EFR's lines to options.txt as GameSettings.saveOptions writes it. */
	public static void write(PrintWriter out) {
		out.println("fovEffectScale:" + McFlyConfig.fovEffectScale);
		int attackIndicator = 1;
		for (int i = 0; i < ATTACK_INDICATOR.length; i++) {
			if (ATTACK_INDICATOR[i].equals(McFlyConfig.attackIndicator)) {
				attackIndicator = i;
			}
		}
		out.println("attackIndicator:" + attackIndicator);
		out.println("autoSuggestions:" + McFlyConfig.commandSuggestions);
		out.println("mainHand:" + (McFlyConfig.mainHandLeft ? "\"left\"" : "\"right\""));
		boolean[] shown = McFlyConfig.skinPartsShown();
		for (int i = 0; i < MODEL_PARTS.length; i++) {
			out.println("modelPart_" + MODEL_PARTS[i] + ":" + shown[i]);
		}
		for (int i = 0; i < TOGGLES.length; i++) {
			out.println(TOGGLES[i] + ":" + ToggleKeys.get(i));
		}
		out.println("autoJump:" + ControlOptions.autoJump);
		out.println("sprintWindow:" + ControlOptions.sprintWindow);
		out.println("operatorItemsTab:" + ControlOptions.operatorItemsTab);
		out.println("invertXMouse:" + ControlOptions.invertMouseX);
		out.println("mouseWheelSensitivity:" + ControlOptions.scrollSensitivity);
		out.println("discrete_mouse_scroll:" + ControlOptions.discreteScrolling);
	}

	/** Takes EFR's values from options.txt; a line that doesn't parse leaves its value as it was. */
	public static void read(File optionsFile) {
		if (!optionsFile.isFile()) {
			return;
		}
		boolean[] shown = McFlyConfig.skinPartsShown();
		try {
			for (String line : Files.readAllLines(optionsFile.toPath(), StandardCharsets.UTF_8)) {
				int colon = line.indexOf(':');
				if (colon > 0) {
					read(line.substring(0, colon), line.substring(colon + 1).trim(), shown);
				}
			}
		} catch (IOException e) {
			McFly.LOG.error("Can't read EFR's options from " + optionsFile + ": " + e);
			return;
		}
		McFlyConfig.skinCape = shown[0];
		McFlyConfig.skinJacket = shown[1];
		McFlyConfig.skinLeftSleeve = shown[2];
		McFlyConfig.skinRightSleeve = shown[3];
		McFlyConfig.skinLeftPantsLeg = shown[4];
		McFlyConfig.skinRightPantsLeg = shown[5];
		McFlyConfig.skinHat = shown[6];
	}

	private static boolean isBoolean(String text) {
		return text.equals("true") || text.equals("false");
	}

	private static void read(String name, String value, boolean[] shown) {
		// 26.3 writes strings as JSON, in quotes
		String text = value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"") ? value.substring(1, value.length() - 1) : value;
		switch (name) {
			case "fovEffectScale" -> {
				try {
					McFlyConfig.fovEffectScale = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(text)));
				} catch (NumberFormatException e) {
					// left as it was
				}
			}
			case "attackIndicator" -> {
				for (int i = 0; i < ATTACK_INDICATOR.length; i++) {
					if (text.equals(String.valueOf(i)) || text.equals(ATTACK_INDICATOR[i])) {
						McFlyConfig.attackIndicator = ATTACK_INDICATOR[i];
					}
				}
			}
			case "autoSuggestions" -> {
				if (text.equals("true") || text.equals("false")) {
					McFlyConfig.commandSuggestions = Boolean.parseBoolean(text);
				}
			}
			case "mainHand" -> {
				if (text.equals("left") || text.equals("right")) {
					McFlyConfig.mainHandLeft = text.equals("left");
				}
			}
			case "autoJump" -> {
				if (isBoolean(text)) {
					ControlOptions.autoJump = Boolean.parseBoolean(text);
				}
			}
			case "sprintWindow" -> {
				try {
					ControlOptions.sprintWindow = Math.max(0, Math.min(10, Integer.parseInt(text)));
				} catch (NumberFormatException e) {
					// left as it was
				}
			}
			case "operatorItemsTab" -> {
				if (isBoolean(text)) {
					ControlOptions.operatorItemsTab = Boolean.parseBoolean(text);
				}
			}
			case "invertXMouse" -> {
				if (isBoolean(text)) {
					ControlOptions.invertMouseX = Boolean.parseBoolean(text);
				}
			}
			case "mouseWheelSensitivity" -> {
				try {
					ControlOptions.scrollSensitivity = Math.max(0.01D, Math.min(10.0D, Double.parseDouble(text)));
				} catch (NumberFormatException e) {
					// left as it was
				}
			}
			case "discrete_mouse_scroll" -> {
				if (isBoolean(text)) {
					ControlOptions.discreteScrolling = Boolean.parseBoolean(text);
				}
			}
			default -> {
				for (int i = 0; i < TOGGLES.length; i++) {
					if (name.equals(TOGGLES[i]) && (text.equals("true") || text.equals("false"))) {
						ToggleKeys.set(i, Boolean.parseBoolean(text));
					}
				}
				for (int i = 0; i < MODEL_PARTS.length; i++) {
					if (name.equals("modelPart_" + MODEL_PARTS[i]) && (text.equals("true") || text.equals("false"))) {
						shown[i] = Boolean.parseBoolean(text);
					}
				}
			}
		}
	}
}
