package io.github.asgerjon.mcfly.wood;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/**
 * One side of a sign (SPEC-modern-movement.md 91.8; 26.3's `SignText`): four plain lines (1.7.10's text has no
 * components), the dye's colour by name and the glow. The front's lines are 1.7.10's own `signText` array, so vanilla's
 * and other mods' code reading it sees the same text.
 */
public final class SignSide {

	public String[] lines;
	public String color = Signs.BLACK;
	public boolean glowing;

	public SignSide(String[] lines) {
		this.lines = lines;
	}

	public static SignSide empty() {
		return new SignSide(new String[]{"", "", "", ""});
	}

	/** 26.3's `hasMessage`: some line has text. */
	public boolean hasMessage() {
		for (String line : lines) {
			if (line != null && !line.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public String line(int i) {
		return lines[i] == null ? "" : lines[i];
	}

	public void setColor(String color) {
		this.color = color == null || color.isEmpty() ? Signs.BLACK : color;
	}

	/** 26.3's `back_text` form: `messages` (4 strings), `color`, `has_glowing_text`. */
	public NBTTagCompound write() {
		NBTTagCompound tag = new NBTTagCompound();
		NBTTagList messages = new NBTTagList();
		for (int i = 0; i < Signs.LINES; i++) {
			messages.appendTag(new NBTTagString(line(i)));
		}
		tag.setTag("messages", messages);
		tag.setString("color", color);
		tag.setBoolean("has_glowing_text", glowing);
		return tag;
	}

	/** Reads 26.3's form; what's missing reads as empty and black. */
	public void read(NBTTagCompound tag) {
		NBTTagList messages = tag.getTagList("messages", 8);
		for (int i = 0; i < Signs.LINES; i++) {
			lines[i] = i < messages.tagCount() ? messages.getStringTagAt(i) : "";
		}
		setColor(tag.getString("color"));
		glowing = tag.getBoolean("has_glowing_text");
	}

	public void clear() {
		for (int i = 0; i < Signs.LINES; i++) {
			lines[i] = "";
		}
		color = Signs.BLACK;
		glowing = false;
	}
}
