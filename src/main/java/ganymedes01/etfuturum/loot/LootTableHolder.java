package ganymedes01.etfuturum.loot;

/**
 * A container that holds 26.3's loot table to be rolled when it's first opened (`RandomizableContainer`;
 * SPEC-modern-movement.md §76.12): the table's name and seed, saved as 26.3's `LootTable` and `LootTableSeed`.
 * 1.7.10's chest has it by a mixin.
 */
public interface LootTableHolder {

	void etfu$setLootTable(String table, long seed);

	/** The table still to roll ("minecraft:chests/end_city_treasure"), or null. */
	String etfu$lootTable();

	long etfu$lootTableSeed();

	void etfu$clearLootTable();
}
