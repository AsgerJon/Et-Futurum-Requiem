package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import ganymedes01.etfuturum.command.modern.arguments.EffectIds;
import ganymedes01.etfuturum.command.modern.arguments.EnchantmentIds;
import ganymedes01.etfuturum.command.modern.arguments.EntityTypeIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import ganymedes01.etfuturum.unittas.TasTestCase;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Not tests of behaviour: lists for planning. Each writes the ids EFR knows of one kind, as its commands take them
 * (1.7.10's under their modern ids, EFR's own under theirs), to a file in the results. They are checked against what
 * 26.3's world generation places, spawns and fills chests with, before any of it is planned (the user, 2026-11-02:
 * "make sure that we all the blocks, items and mobs ready *before* enhancing the world generation").
 */
public class InventoryReport extends TasTestCase {

	public void testWriteBlockIds() throws IOException {
		write("modern-block-ids.txt", BlockIds::ids);
	}

	public void testWriteItemIds() throws IOException {
		write("modern-item-ids.txt", ItemIds::ids);
	}

	public void testWriteEntityIds() throws IOException {
		write("modern-entity-ids.txt", EntityTypeIds::summonableIds);
	}

	public void testWriteEffectIds() throws IOException {
		write("modern-effect-ids.txt", EffectIds::ids);
	}

	public void testWriteEnchantmentIds() throws IOException {
		write("modern-enchantment-ids.txt", EnchantmentIds::ids);
	}

	private void write(String file, Callable<Collection<String>> ids) throws IOException {
		List<String> lines = onServer(() -> new ArrayList<>(ids.call()));
		Files.write(new File(resultsFolder(), file).toPath(), lines, StandardCharsets.UTF_8);
		assertGreater(lines.size(), 10);
	}
}
