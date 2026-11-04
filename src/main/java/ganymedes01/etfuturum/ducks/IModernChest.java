package ganymedes01.etfuturum.ducks;

/** A chest's type, as 26.3's `ChestType` (spec §65): `ModernChests.UNKNOWN`, `SINGLE`, `LEFT` or `RIGHT`. */
public interface IModernChest {

	int etfu$chestType();

	void etfu$setChestType(int type);
}
