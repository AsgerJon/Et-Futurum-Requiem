package ganymedes01.etfuturum.client.gui.options;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.client.options.ControlOptions;
import ganymedes01.etfuturum.client.options.ToggleKeys;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.opengl.GL11;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

/**
 * The buttons of the modern options screens (spec §54.3, §54.10). Each option sets its value when clicked, in its
 * own mousePressed, as 1.7.10's option sliders and EFR's option buttons do; the list then tells the screen, which only
 * acts on buttons that open another screen.
 */
public final class OptionWidgets {

	private OptionWidgets() {
	}

	/** 1.7.10's option as a slider or a cycling button, under its own name. */
	public static GuiButton of(GameSettings.Options option) {
		return of(option, null);
	}

	/** 1.7.10's option, under 26.3's name (a lang key) where 26.3 renamed it. */
	public static GuiButton of(GameSettings.Options option, String nameKey) {
		return option.getEnumFloat() ? new GuiOptionSlider(option.returnEnumOrdinal(), 0, 0, option) : new Cycling(option, nameKey);
	}

	/** A button that opens another screen ("Controls...", "Key Binds..."). */
	public static GuiButton link(int id, String key) {
		return new GuiButton(id, 0, 0, 150, 20, I18n.format(key));
	}

	/** 1.7.10's on/off and list options: a click moves to the next value. */
	public static final class Cycling extends GuiButton {
		private final GameSettings.Options option;
		private final String nameKey;

		private Cycling(GameSettings.Options option, String nameKey) {
			super(option.returnEnumOrdinal(), 0, 0, 150, 20, "");
			this.option = option;
			this.nameKey = nameKey;
			displayString = label();
		}

		/** 1.7.10's label ("Invert Mouse: OFF"), with 26.3's name in front of the value where it has one. */
		private String label() {
			String label = Minecraft.getMinecraft().gameSettings.getKeyBinding(option);
			if (nameKey == null) {
				return label;
			}
			int colon = label.indexOf(':');
			return I18n.format(nameKey) + (colon < 0 ? "" : label.substring(colon));
		}

		@Override
		public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
			if (!super.mousePressed(mc, mouseX, mouseY)) {
				return false;
			}
			mc.gameSettings.setOptionValue(option, 1);
			displayString = label();
			return true;
		}
	}

	/** 26.3's Hold/Toggle for key 0 to 3 of ToggleKeys (spec §54.11): "Sneak: Hold", a click to Toggle and back. */
	public static final class HoldOrToggle extends GuiButton {
		private static final String[] KEYS = {"key.sneak", "key.sprint", "key.attack", "key.use"};
		private final int key;

		public HoldOrToggle(int id, int key) {
			super(id, 0, 0, 150, 20, "");
			this.key = key;
			displayString = label();
		}

		private String label() {
			return I18n.format(KEYS[key]) + ": " + I18n.format(ToggleKeys.get(key) ? "options.key.toggle" : "options.key.hold");
		}

		@Override
		public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
			if (!super.mousePressed(mc, mouseX, mouseY)) {
				return false;
			}
			ToggleKeys.set(key, !ToggleKeys.get(key));
			displayString = label();
			return true;
		}
	}

	/** An on/off option of EFR's (spec §54.12): "Auto-Jump: OFF", a click to ON and back. */
	public static final class OnOff extends GuiButton {
		private final String nameKey;
		private final BooleanSupplier get;
		private final Consumer<Boolean> set;

		public OnOff(int id, String nameKey, BooleanSupplier get, Consumer<Boolean> set) {
			super(id, 0, 0, 150, 20, "");
			this.nameKey = nameKey;
			this.get = get;
			this.set = set;
			displayString = label();
		}

		private String label() {
			return I18n.format(nameKey) + ": " + I18n.format(get.getAsBoolean() ? "options.on" : "options.off");
		}

		@Override
		public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
			if (!super.mousePressed(mc, mouseX, mouseY)) {
				return false;
			}
			set.accept(!get.getAsBoolean());
			displayString = label();
			return true;
		}
	}

	/**
	 * A slider over whole steps min..max of an option of EFR's (spec §54.12), drawn and dragged as 1.7.10's option
	 * sliders: "Sprint Window: 7".
	 */
	public static final class Slider extends GuiButton {
		private final String nameKey;
		private final int min;
		private final int max;
		private final IntSupplier get;
		private final IntConsumer set;
		private final IntFunction<String> value;
		private boolean dragging;

		public Slider(int id, String nameKey, int min, int max, IntSupplier get, IntConsumer set, IntFunction<String> value) {
			super(id, 0, 0, 150, 20, "");
			this.nameKey = nameKey;
			this.min = min;
			this.max = max;
			this.get = get;
			this.set = set;
			this.value = value;
			displayString = label();
		}

		private String label() {
			return I18n.format(nameKey) + ": " + value.apply(get.getAsInt());
		}

		private float position() {
			return (float) (get.getAsInt() - min) / (max - min);
		}

		private void moveTo(int mouseX) {
			float position = Math.max(0.0F, Math.min(1.0F, (float) (mouseX - (xPosition + 4)) / (width - 8)));
			set.accept(min + Math.round(position * (max - min)));
			displayString = label();
		}

		@Override
		public int getHoverState(boolean mouseOver) {
			return 0;
		}

		@Override
		protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
			if (!visible) {
				return;
			}
			if (dragging) {
				moveTo(mouseX);
			}
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			int knob = xPosition + (int) (position() * (width - 8));
			drawTexturedModalRect(knob, yPosition, 0, 66, 4, 20);
			drawTexturedModalRect(knob + 4, yPosition, 196, 66, 4, 20);
		}

		@Override
		public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
			if (!super.mousePressed(mc, mouseX, mouseY)) {
				return false;
			}
			moveTo(mouseX);
			dragging = true;
			return true;
		}

		@Override
		public void mouseReleased(int mouseX, int mouseY) {
			dragging = false;
		}
	}

	/** 26.3's Sprint Window: 0 to 10 ticks, 0 shown as OFF. */
	public static Slider sprintWindow(int id) {
		return new Slider(id, "options.sprintWindow", 0, 10, () -> ControlOptions.sprintWindow, ticks -> ControlOptions.sprintWindow = ticks,
			ticks -> ticks == 0 ? I18n.format("options.off") : String.valueOf(ticks));
	}

	/** 26.3's Scroll Sensitivity: steps -200..100 for 10^(step/100), 0.01 to 10, shown with two decimals. */
	public static Slider scrollSensitivity(int id) {
		return new Slider(id, "options.mouseWheelSensitivity", -200, 100,
			() -> (int) Math.round(Math.log10(ControlOptions.scrollSensitivity) * 100.0D),
			step -> ControlOptions.scrollSensitivity = Math.pow(10.0D, step / 100.0D),
			step -> String.format(Locale.ROOT, "%.2f", Math.pow(10.0D, step / 100.0D)));
	}

	/** EFR's Command Suggestions (spec §40.5) as a Chat Settings button: on, the suggestion list opens as you type. */
	public static final class CommandSuggestions extends GuiButton {

		public CommandSuggestions(int id) {
			super(id, 0, 0, 150, 20, "");
			displayString = label();
		}

		private static String label() {
			return I18n.format("options.autoSuggestCommands") + ": " + I18n.format(McFlyConfig.commandSuggestions ? "options.on" : "options.off");
		}

		@Override
		public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
			if (!super.mousePressed(mc, mouseX, mouseY)) {
				return false;
			}
			McFlyConfig.saveCommandSuggestions(!McFlyConfig.commandSuggestions);
			displayString = label();
			return true;
		}
	}
}
