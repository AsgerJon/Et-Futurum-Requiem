package io.github.asgerjon.mcfly.dye;

import net.minecraft.block.material.MapColor;

/**
 * 26.3's 16 dye colours (SPEC-modern-movement.md §80; 26.3's `DyeColor`): by id (white 0 to black 15, as wool's meta,
 * a banner's base and its layers number them), with 26.3's name, texture colour (what a banner's layers and a shield's
 * are tinted by), firework colour and text colour (RGB), and 1.7.10's nearest map colour. A 1.7.10 dye's damage runs
 * the other way (black 0).
 */
public enum DyeColor {
	WHITE(0, "white", 0xF9FFFE, 0xF0F0F0, 0xFFFFFF, MapColor.snowColor),
	ORANGE(1, "orange", 0xF9801D, 0xEB8844, 0xFF681F, MapColor.adobeColor),
	MAGENTA(2, "magenta", 0xC74EBD, 0xC354CD, 0xFF00FF, MapColor.magentaColor),
	LIGHT_BLUE(3, "light_blue", 0x3AB3DA, 0x6689D3, 0x9AC0CD, MapColor.lightBlueColor),
	YELLOW(4, "yellow", 0xFED83D, 0xDECF2A, 0xFFFF00, MapColor.yellowColor),
	LIME(5, "lime", 0x80C71F, 0x41CD34, 0xBFFF00, MapColor.limeColor),
	PINK(6, "pink", 0xF38BAA, 0xD88198, 0xFF69B4, MapColor.pinkColor),
	GRAY(7, "gray", 0x474F52, 0x434343, 0x808080, MapColor.grayColor),
	LIGHT_GRAY(8, "light_gray", 0x9D9D97, 0xABABAB, 0xD3D3D3, MapColor.silverColor),
	CYAN(9, "cyan", 0x169C9C, 0x287697, 0x00FFFF, MapColor.cyanColor),
	PURPLE(10, "purple", 0x8932B8, 0x7B2FBE, 0xA020F0, MapColor.purpleColor),
	BLUE(11, "blue", 0x3C44AA, 0x253192, 0x0000FF, MapColor.blueColor),
	BROWN(12, "brown", 0x835432, 0x51301A, 0x8B4513, MapColor.brownColor),
	GREEN(13, "green", 0x5E7C16, 0x3B511A, 0x00FF00, MapColor.greenColor),
	RED(14, "red", 0xB02E26, 0xB3312C, 0xFF0000, MapColor.redColor),
	BLACK(15, "black", 0x1D1D21, 0x1E1B1B, 0x000000, MapColor.blackColor);

	private static final DyeColor[] BY_ID = values();

	private final int id;
	private final String name;
	private final int textureColour;
	private final int fireworkColour;
	private final int textColour;
	private final MapColor mapColour;

	DyeColor(int id, String name, int textureColour, int fireworkColour, int textColour, MapColor mapColour) {
		this.id = id;
		this.name = name;
		this.textureColour = textureColour;
		this.fireworkColour = fireworkColour;
		this.textColour = textColour;
		this.mapColour = mapColour;
	}

	/** By id (white 0 to black 15); out of range, white (26.3's `byId`). */
	public static DyeColor byId(int id) {
		return id >= 0 && id < BY_ID.length ? BY_ID[id] : WHITE;
	}

	/** By 26.3's name ("light_blue"), or null. */
	public static DyeColor byName(String name) {
		for (DyeColor colour : BY_ID) {
			if (colour.name.equals(name)) {
				return colour;
			}
		}
		return null;
	}

	/** The colour of a 1.7.10 dye's damage (black 0 to white 15). */
	public static DyeColor fromDyeDamage(int damage) {
		return byId(15 - (damage & 15));
	}

	public int id() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int textureColour() {
		return textureColour;
	}

	public int fireworkColour() {
		return fireworkColour;
	}

	public int textColour() {
		return textColour;
	}

	public MapColor mapColour() {
		return mapColour;
	}

	/** The damage of 1.7.10's dye of this colour (black 0 to white 15). */
	public int dyeDamage() {
		return 15 - id;
	}

	/** Forge's ore dictionary name for dyes of this colour (`dyeLightBlue`). */
	public String oreName() {
		StringBuilder ore = new StringBuilder("dye");
		for (String word : name.split("_")) {
			ore.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return ore.toString();
	}
}
