package io.github.asgerjon.mcfly.wood.client;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 26.3's `TextFieldHelper` (SPEC-modern-movement.md 91.8; `batch-woods3`) on 1.7.10's keys: a cursor and selection in a
 * line someone else holds. Ctrl+A, C, V, X; Backspace and Delete (with ctrl, a word: 26.3's
 * `StringSplitter.getWordPosition`); left and right (shift selects, ctrl by words); Home and End. Typed characters
 * are 26.3's allowed chat characters; a change is kept only if the check passes (the sign's 90 pixels).
 */
public final class SignTextField {

	private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9A-FK-OR]");

	private final Supplier<String> get;
	private final Consumer<String> set;
	private final Predicate<String> fits;
	private int cursor;
	private int selection;

	public SignTextField(Supplier<String> get, Consumer<String> set, Predicate<String> fits) {
		this.get = get;
		this.set = set;
		this.fits = fits;
		setCursorToEnd();
	}

	public int cursor() {
		return cursor;
	}

	public int selection() {
		return selection;
	}

	/** 26.3's `StringUtil.isAllowedChatCharacter`. */
	public static boolean allowed(char c) {
		return c != 167 && c >= ' ' && c != 127;
	}

	public void charTyped(char c) {
		if (allowed(c)) {
			insert(get.get(), String.valueOf(c));
		}
	}

	/** Whether the key was the field's. */
	public boolean keyPressed(int key) {
		boolean ctrl = GuiScreen.isCtrlKeyDown(), shift = GuiScreen.isShiftKeyDown();
		if (ctrl && key == Keyboard.KEY_A) {
			selection = 0;
			cursor = get.get().length();
			return true;
		}
		if (ctrl && key == Keyboard.KEY_C) {
			GuiScreen.setClipboardString(selected(get.get()));
			return true;
		}
		if (ctrl && key == Keyboard.KEY_V) {
			insert(get.get(), FORMATTING.matcher(GuiScreen.getClipboardString().replaceAll("\\r", "")).replaceAll(""));
			selection = cursor;
			return true;
		}
		if (ctrl && key == Keyboard.KEY_X) {
			String text = get.get();
			GuiScreen.setClipboardString(selected(text));
			set.accept(deleteSelection(text));
			return true;
		}
		switch (key) {
			case Keyboard.KEY_BACK:
				remove(-1, ctrl);
				return true;
			case Keyboard.KEY_DELETE:
				// 26.3 takes it and goes on (its keyPressed returns false for Delete)
				remove(1, ctrl);
				return false;
			case Keyboard.KEY_LEFT:
				move(-1, shift, ctrl);
				return true;
			case Keyboard.KEY_RIGHT:
				move(1, shift, ctrl);
				return true;
			case Keyboard.KEY_HOME:
				cursor = 0;
				keepSelection(shift);
				return true;
			case Keyboard.KEY_END:
				cursor = get.get().length();
				keepSelection(shift);
				return true;
			default:
				return false;
		}
	}

	public void setCursorToEnd() {
		cursor = get.get().length();
		selection = cursor;
	}

	private void keepSelection(boolean selecting) {
		if (!selecting) {
			selection = cursor;
		}
	}

	private void insert(String text, String added) {
		if (selection != cursor) {
			text = deleteSelection(text);
		}
		cursor = Math.max(0, Math.min(cursor, text.length()));
		String changed = new StringBuilder(text).insert(cursor, added).toString();
		if (fits.test(changed)) {
			set.accept(changed);
			selection = cursor = Math.min(changed.length(), cursor + added.length());
		}
	}

	private void move(int count, boolean selecting, boolean words) {
		String text = get.get();
		cursor = words ? wordPosition(text, count, cursor) : Math.max(0, Math.min(text.length(), cursor + count));
		keepSelection(selecting);
	}

	private void remove(int count, boolean words) {
		String text = get.get();
		if (text.isEmpty()) {
			return;
		}
		String changed;
		if (selection != cursor) {
			changed = deleteSelection(text);
		} else {
			int other = words ? wordPosition(text, count, cursor) : Math.max(0, Math.min(text.length(), cursor + count));
			int start = Math.min(other, cursor), end = Math.max(other, cursor);
			changed = new StringBuilder(text).delete(start, end).toString();
			if (count < 0) {
				selection = cursor = start;
			}
		}
		set.accept(changed);
	}

	private String selected(String text) {
		return text.substring(Math.min(cursor, selection), Math.max(cursor, selection));
	}

	private String deleteSelection(String text) {
		if (selection == cursor) {
			return text;
		}
		int start = Math.min(cursor, selection), end = Math.max(cursor, selection);
		selection = cursor = start;
		return text.substring(0, start) + text.substring(end);
	}

	/** 26.3's `StringSplitter.getWordPosition` (spaces stripped). */
	static int wordPosition(String text, int dir, int from) {
		int result = from;
		boolean reverse = dir < 0;
		for (int i = 0; i < Math.abs(dir); i++) {
			if (reverse) {
				while (result > 0 && (text.charAt(result - 1) == ' ' || text.charAt(result - 1) == '\n')) {
					result--;
				}
				while (result > 0 && text.charAt(result - 1) != ' ' && text.charAt(result - 1) != '\n') {
					result--;
				}
			} else {
				int space = text.indexOf(' ', result), newline = text.indexOf('\n', result);
				if (space == -1 && newline == -1) {
					result = text.length();
				} else {
					result = space == -1 ? newline : newline == -1 ? space : Math.min(space, newline);
					while (result < text.length() && (text.charAt(result) == ' ' || text.charAt(result) == '\n')) {
						result++;
					}
				}
			}
		}
		return result;
	}
}
