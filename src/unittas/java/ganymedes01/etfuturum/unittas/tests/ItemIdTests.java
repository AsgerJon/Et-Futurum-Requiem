package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.BlockSlab;
import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Modern ids for 1.7.10's variants (spec §48): vanilla's by item and damage, EFR's by the name the player sees. */
@TasSuite("commands")
public class ItemIdTests extends TasTestCase {

	private ItemStack give(String id) {
		assertNoError(command("/give @s " + id));
		List<ItemStack> stacks = inventory();
		assertTrue(!stacks.isEmpty(), "/give @s " + id + " gave nothing");
		return stacks.get(0);
	}

	private void assertGives(String modernId, String legacy) {
		onServer(() -> player().inventory.clearInventory(null, -1));
		assertEqual(legacy, id(give(modernId)), modernId);
	}

	private void assertGivesNamed(String modernId, String displayName) {
		onServer(() -> player().inventory.clearInventory(null, -1));
		assertEqual(displayName, give(modernId).getDisplayName(), modernId);
	}

	public void testVanillaVariants() {
		assertGives("red_wool", "minecraft:wool:14");
		assertGives("dark_oak_planks", "minecraft:planks:5");
		assertGives("stone_bricks", "minecraft:stonebrick:0");
		assertGives("bone_meal", "minecraft:dye:15");
		assertGives("red_dye", "minecraft:dye:1");
		assertGives("cooked_cod", "minecraft:cooked_fished:0");
		assertGives("short_grass", "minecraft:tallgrass:1");
		assertGives("fern", "minecraft:tallgrass:2");
		assertGives("smooth_stone_slab", "minecraft:stone_slab:0");
		assertGives("podzol", "minecraft:dirt:2");
		assertGives("red_bed", "minecraft:bed:0");
		assertGives("enchanted_golden_apple", "minecraft:golden_apple:1");
		assertGives("melon_slice", "minecraft:melon:0");
		assertGives("melon", "minecraft:melon_block:0");
	}

	public void testSpawnEggs() {
		assertGives("zombie_spawn_egg", "minecraft:spawn_egg:54");
		assertGives("zombified_piglin_spawn_egg", "minecraft:spawn_egg:57");
	}

	public void testEfrRabbitEgg() {
		onServer(() -> player().inventory.clearInventory(null, -1));
		ItemStack egg = give("rabbit_spawn_egg");
		assertEqual("minecraft:spawn_egg", Item.itemRegistry.getNameForObject(egg.getItem()));
		assertEqual("etfuturum.rabbit", EntityList.getStringFromID(egg.getItemDamage()));
	}

	public void testEfrBlocksByModernName() {
		assertGivesNamed("granite", "Granite");
		assertGivesNamed("coarse_dirt", "Coarse Dirt");
		assertGivesNamed("wet_sponge", "Wet Sponge");
		assertGivesNamed("magma_block", "Magma Block");
		assertGivesNamed("bone_block", "Bone Block");
		assertGivesNamed("stone_slab", "Stone Slab");
		assertGivesNamed("white_bed", "White Bed");
		assertGivesNamed("red_shulker_box", "Red Shulker Box");
		assertGivesNamed("white_banner", "White Banner");
	}

	/** The user's report (2026-11-01): EFR's double slab shares its slab's name and got the id, so this gave double slabs. */
	public void testEfrSlabIsTheSlab() {
		if (!io.github.asgerjon.mcfly.registry.McFlyBlocks.SMOOTH_QUARTZ_SLAB.isEnabled()) {
			skipTest("no smooth quartz slab");
		}
		onServer(() -> player().inventory.clearInventory(null, -1));
		assertEqual(Item.getItemFromBlock(io.github.asgerjon.mcfly.registry.McFlyBlocks.SMOOTH_QUARTZ_SLAB.get()), give("smooth_quartz_slab").getItem());
	}

	/** Modern has no double slab item: a double slab keeps only its registry name, never a slab's modern id. */
	public void testNoModernIdGivesADoubleSlab() {
		List<String> doubles = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (String id : ItemIds.ids()) {
				Item item = ItemIds.byId(id).item();
				if (item instanceof ItemBlock itemBlock && itemBlock.field_150939_a instanceof BlockSlab slab && slab.isOpaqueCube()
					&& !id.equals(String.valueOf(Item.itemRegistry.getNameForObject(item)).toLowerCase(Locale.ROOT))) {
					found.add(id);
				}
			}
			return found;
		});
		assertEqual(new ArrayList<String>(), doubles);
	}

	public void testEfrOwnName() {
		ItemStack stairs = give("etfuturum:netherite_stairs");
		assertTrue(id(stairs).startsWith("etfuturum:"), id(stairs));
	}

	public void testDamageDoesntChangeTheVariant() {
		assertEqual("minecraft:wool:14", id(give("red_wool[damage=3]")));
	}

	public void testOldNamesAreNotModern() {
		assertError(command("/give @s wool"), "Unknown item 'minecraft:wool'");
	}

	public void testSuggestsColours() {
		List<String> shown = type("/give @s red_").suggestions();
		assertTrue(shown.stream().anyMatch(id -> id.contains("red_wool")), "no red_wool in " + shown);
		assertTrue(shown.stream().anyMatch(id -> id.contains("red_bed")), "no red_bed in " + shown);
	}
}
