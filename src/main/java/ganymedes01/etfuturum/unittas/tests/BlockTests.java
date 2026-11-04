package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.command.modern.arguments.BlockInput;
import ganymedes01.etfuturum.unittas.TasCommand;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;

import java.util.List;

/** The block argument, /setblock, /fill and /clone (spec §49). The tests build in 8..40 east and south of the spawn. */
public class BlockTests extends TasTestCase {

	/** Every test starts on clear ground: air above the grass in the building area, and nothing lying about. */
	@Override
	protected void setUp() {
		onServer(() -> {
			for (int x = 8; x <= 40; x++) {
				for (int z = 8; z <= 40; z++) {
					for (int y = 4; y <= 12; y++) {
						if (!world().isAirBlock(x, y, z)) {
							// an earlier test's chest would spill its diamonds
							BlockInput.emptyContainer(world(), x, y, z);
							world().setBlock(x, y, z, Blocks.air, 0, 2);
						}
					}
				}
			}
			for (EntityItem item : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(0, 0, 0, 48, 16, 48))) {
				item.setDead();
			}
		});
	}

	/** `minecraft:wool:14`: the 1.7.10 block and meta there. */
	private String blockAt(int x, int y, int z) {
		return onServer(() -> Block.blockRegistry.getNameForObject(world().getBlock(x, y, z)) + ":" + world().getBlockMetadata(x, y, z));
	}

	// ---------------------------------------------------------------- /setblock

	public void testSetBlock() {
		assertSaid(command("/setblock 10 4 10 stone"), "Changed the block at 10, 4, 10");
		assertEqual("minecraft:stone:0", blockAt(10, 4, 10));
		command("/setblock 11 4 10 red_wool");
		assertEqual("minecraft:wool:14", blockAt(11, 4, 10));
	}

	public void testProperties() {
		command("/setblock 10 4 10 oak_stairs[facing=east,half=top]");
		assertEqual("minecraft:oak_stairs:4", blockAt(10, 4, 10));
		command("/setblock 11 4 10 oak_stairs");
		assertEqual("minecraft:oak_stairs:3", blockAt(11, 4, 10), "plain stairs face north");
		command("/setblock 12 4 10 spruce_log[axis=x]");
		assertEqual("minecraft:log:5", blockAt(12, 4, 10));
		command("/setblock 13 4 10 furnace[facing=south,lit=true]");
		assertEqual("minecraft:lit_furnace:3", blockAt(13, 4, 10));
	}

	public void testEfrBlock() {
		assertNoError(command("/setblock 10 4 10 granite"));
		String name = onServer(() -> new ItemStack(world().getBlock(10, 4, 10), 1, world().getBlockMetadata(10, 4, 10)).getDisplayName());
		assertEqual("Granite", name);
	}

	public void testKeepAndUnchanged() {
		command("/setblock 10 4 10 stone");
		assertError(command("/setblock 10 4 10 dirt keep"), "Could not set the block");
		assertError(command("/setblock 10 4 10 stone"), "Could not set the block");
		assertEqual("minecraft:stone:0", blockAt(10, 4, 10));
	}

	public void testDestroyDrops() {
		command("/setblock 10 4 10 oak_planks");
		command("/setblock 10 4 10 stone destroy");
		ticks(2);
		boolean dropped = onServer(() -> {
			for (EntityItem item : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(8, 3, 8, 13, 7, 13))) {
				if (item.getEntityItem().getItem() == Item.getItemFromBlock(Blocks.planks)) {
					return true;
				}
			}
			return false;
		});
		assertTrue(dropped, "the planks didn't drop");
		assertEqual("minecraft:stone:0", blockAt(10, 4, 10));
	}

	public void testErrors() {
		assertError(command("/setblock 10 4 10 nope"), "Unknown block type 'minecraft:nope'");
		assertError(command("/setblock 10 4 10 stone[foo=bar]"), "Block minecraft:stone does not have property 'foo'");
		assertError(command("/setblock 10 4 10 oak_stairs[facing=up]"), "Block minecraft:oak_stairs does not accept 'up' for facing property");
		assertError(command("/setblock 10 4 10 oak_stairs[facing=east"), "Expected closing ] for block state properties");
		assertError(command("/setblock 10 300 10 stone"), "That position is out of this world!");
	}

	public void testChestContents() {
		assertNoError(command("/setblock 10 4 10 chest{Items:[{Slot:0b,id:\"diamond\",count:3}]}"));
		ItemStack first = onServer(() -> ItemStack.copyItemStack(((TileEntityChest) world().getTileEntity(10, 4, 10)).getStackInSlot(0)));
		assertIsNotNone(first, "the chest is empty");
		assertEqual(Items.diamond, first.getItem());
		assertEqual(3, first.stackSize);
	}

	/** EFR's slabs have a slab's states; the double one is a block of its own, as 1.7.10's are (the user, 2026-11-01). */
	public void testEfrSlabTypes() {
		if (!ModBlocks.SMOOTH_QUARTZ_SLAB.isEnabled() || !ModBlocks.CUT_COPPER_SLAB.isEnabled()) {
			skipTest("no smooth quartz or cut copper slab");
		}
		String slab = Block.blockRegistry.getNameForObject(ModBlocks.SMOOTH_QUARTZ_SLAB.get());
		String doubleSlab = Block.blockRegistry.getNameForObject(ModBlocks.DOUBLE_SMOOTH_QUARTZ_SLAB.get());
		command("/setblock 10 4 10 smooth_quartz_slab");
		assertEqual(slab + ":0", blockAt(10, 4, 10));
		command("/setblock 11 4 10 smooth_quartz_slab[type=top]");
		assertEqual(slab + ":8", blockAt(11, 4, 10));
		assertNoError(command("/setblock 12 4 10 smooth_quartz_slab[type=double]"));
		assertEqual(doubleSlab + ":0", blockAt(12, 4, 10));
		// a variant keeps its meta in the double block (waxed, so it doesn't age during the test)
		command("/setblock 13 4 10 waxed_exposed_cut_copper_slab");
		int variant = onServer(() -> world().getBlockMetadata(13, 4, 10));
		assertTrue(variant > 0, "waxed exposed is not cut copper's first variant");
		command("/setblock 14 4 10 waxed_exposed_cut_copper_slab[type=double]");
		assertEqual(Block.blockRegistry.getNameForObject(ModBlocks.DOUBLE_CUT_COPPER_SLAB.get()) + ":" + variant, blockAt(14, 4, 10));
		// and a double slab is that slab to a block filter
		assertNoError(command("/fill 12 4 10 12 4 10 stone replace smooth_quartz_slab"));
		assertEqual("minecraft:stone:0", blockAt(12, 4, 10));
	}

	public void testLegacyForm() {
		command("/setblock 10 4 10 wool 14");
		assertEqual("minecraft:wool:14", blockAt(10, 4, 10));
	}

	public void testSuggestions() {
		assertIn("[", type("/setblock ~ ~ ~ oak_stairs").suggestions());
		List<String> names = type("/setblock ~ ~ ~ oak_stairs[").suggestions();
		assertIn("facing=", names);
		assertIn("half=", names);
		List<String> values = type("/setblock ~ ~ ~ oak_stairs[facing=").suggestions();
		for (String value : new String[]{"east", "north", "south", "west"}) {
			assertIn(value, values);
		}
	}

	// ---------------------------------------------------------------- /fill

	public void testFillCube() {
		assertSaid(command("/fill 20 4 20 22 6 22 stone"), "Successfully filled 27 block(s)");
		assertEqual("minecraft:stone:0", blockAt(21, 5, 21));
		assertError(command("/fill 20 4 20 22 6 22 stone"), "No blocks were filled");
	}

	public void testFillHollowAndOutline() {
		command("/fill 20 4 20 24 8 24 stone");
		command("/fill 20 4 20 24 8 24 glass outline");
		assertEqual("minecraft:glass:0", blockAt(20, 4, 20));
		assertEqual("minecraft:stone:0", blockAt(22, 6, 22), "outline leaves the inside");
		command("/fill 20 4 20 24 8 24 glass hollow");
		assertEqual("minecraft:air:0", blockAt(22, 6, 22), "hollow empties the inside");
	}

	public void testFillReplace() {
		command("/fill 20 4 20 22 4 22 red_wool");
		command("/setblock 21 4 21 stone");
		assertSaid(command("/fill 20 4 20 22 4 22 blue_wool replace red_wool"), "Successfully filled 8 block(s)");
		assertEqual("minecraft:stone:0", blockAt(21, 4, 21));
		assertSaid(command("/fill 20 4 20 22 4 22 yellow_wool replace #minecraft:wool"), "Successfully filled 8 block(s)");
		assertEqual("minecraft:wool:4", blockAt(20, 4, 20));
	}

	public void testFillTooBig() {
		assertError(command("/fill 0 4 0 40 30 40 stone"), "Too many blocks in the specified area (maximum 32768, but specified 45387)");
	}

	// ---------------------------------------------------------------- /clone

	/** Stone with a torch on it, red wool, and a chest with three diamonds, at 20..22 4..5 20. */
	private void build() {
		command("/setblock 20 4 20 stone");
		command("/setblock 20 5 20 torch");
		command("/setblock 21 4 20 red_wool");
		command("/setblock 22 4 20 chest{Items:[{Slot:0b,id:\"diamond\",count:3}]}");
	}

	public void testClone() {
		build();
		TasCommand clone = command("/clone 20 4 20 22 5 20 20 4 30");
		assertSaid(clone, "Successfully cloned");
		assertEqual("minecraft:stone:0", blockAt(20, 4, 30));
		assertEqual(blockAt(20, 5, 20), blockAt(20, 5, 30), "the torch");
		assertEqual("minecraft:wool:14", blockAt(21, 4, 30));
		ItemStack first = onServer(() -> ItemStack.copyItemStack(((TileEntityChest) world().getTileEntity(22, 4, 30)).getStackInSlot(0)));
		assertIsNotNone(first, "the copied chest is empty");
		assertEqual(3, first.stackSize);
		assertEqual("minecraft:stone:0", blockAt(20, 4, 20), "the source stays");
	}

	public void testCloneMasked() {
		build();
		command("/fill 20 4 30 22 5 30 dirt");
		command("/clone 20 4 20 22 5 20 20 4 30 masked");
		assertEqual("minecraft:dirt:0", blockAt(21, 5, 30), "air isn't copied");
		assertEqual("minecraft:stone:0", blockAt(20, 4, 30));
	}

	public void testCloneMove() {
		build();
		command("/clone 20 4 20 22 5 20 20 4 30 replace move");
		assertEqual("minecraft:stone:0", blockAt(20, 4, 30));
		assertEqual("minecraft:air:0", blockAt(20, 4, 20), "move empties the source");
		assertEqual("minecraft:air:0", blockAt(22, 4, 20));
		boolean spilled = onServer(() -> !world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(18, 3, 18, 25, 8, 23)).isEmpty());
		assertFalse(spilled, "the moved chest spilled its diamonds");
	}

	public void testCloneOverlap() {
		build();
		assertError(command("/clone 20 4 20 22 4 20 21 4 20"), "The source and destination areas cannot overlap");
		assertNoError(command("/clone 20 4 20 22 4 20 21 4 20 replace force"));
		assertEqual("minecraft:stone:0", blockAt(21, 4, 20));
	}
}
