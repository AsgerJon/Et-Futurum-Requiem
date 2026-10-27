package ganymedes01.etfuturum.skins;

/** The parts of a skin a player can show or hide (modern's PlayerModelPart, 1.8+), each a bit of the synced byte. */
public enum SkinPart {
	CAPE("cape"),
	JACKET("jacket"),
	LEFT_SLEEVE("leftSleeve"),
	RIGHT_SLEEVE("rightSleeve"),
	LEFT_PANTS_LEG("leftPantsLeg"),
	RIGHT_PANTS_LEG("rightPantsLeg"),
	HAT("hat");

	public static final SkinPart[] VALUES = values();
	/** The config key's suffix and the language key's. */
	public final String key;

	SkinPart(String key) {
		this.key = key;
	}

	public int mask() {
		return 1 << ordinal();
	}

	public String translationKey() {
		return "options.etfuturum.skinPart." + key;
	}
}
