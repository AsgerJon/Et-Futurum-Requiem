package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Shulker boxes as our own (SPEC-modern-movement.md §81), as 26.3 has them, kept in EFR's stored form. Each test on its
 * own spot (x 200 on, z 200 on), cleared after.
 */
public class ShulkerBoxTests extends TasTestCase {

	private static final int GROUND = 4;
	private final List<int[]> used = new ArrayList<>();

	private int[] spot(int dx, int dz) {
		int[] at = {200 + dx * 4, 200 + dz * 4};
		used.add(at);
		return at;
	}

	@Override
	protected void setUp() {
		if (box() == null) {
			skipTest("shulker boxes are off");
		}
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			for (int[] at : used) {
				for (int x = at[0] - 2; x <= at[0] + 2; x++) {
					for (int z = at[1] - 2; z <= at[1] + 2; z++) {
						for (int y = GROUND + 4; y >= GROUND; y--) {
							world().setBlockToAir(x, y, z);
						}
						world().setBlock(x, GROUND - 1, z, Blocks.stone);
					}
				}
				for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, net.minecraft.util.AxisAlignedBB.getBoundingBox(at[0] - 3, GROUND - 1, at[1] - 3, at[0] + 3, GROUND + 6, at[1] + 3))) {
					((EntityItem) entity).setDead();
				}
			}
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.clearInventory(null, -1);
		});
		used.clear();
	}

	private static Block box() {
		Object block = Block.blockRegistry.getObject("etfuturum:shulker_box");
		return block instanceof Block && block != Blocks.air ? (Block) block : null;
	}

	private static ItemStack boxItem(int colour, ItemStack... contents) {
		ItemStack stack = new ItemStack(Item.getItemFromBlock(box()));
		NBTTagCompound tag = new NBTTagCompound();
		NBTTagList items = new NBTTagList();
		for (int i = 0; i < contents.length; i++) {
			NBTTagCompound one = new NBTTagCompound();
			one.setByte("Slot", (byte) i);
			contents[i].writeToNBT(one);
			items.appendTag(one);
		}
		if (contents.length > 0) {
			tag.setTag("Items", items);
		}
		if (colour > 0) {
			tag.setByte("Color", (byte) colour);
		}
		if (!tag.hasNoTags()) {
			stack.setTagCompound(tag);
		}
		return stack;
	}

	/** A box placed as a player places it: on the ground's top (facing up), its contents from the item. */
	private void place(int x, int z, ItemStack item) {
		onServer(() -> {
			world().setBlock(x, GROUND - 1, z, Blocks.stone);
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			item.copy().tryPlaceItemIntoWorld(player(), world(), x, GROUND - 1, z, 1, 0.5F, 1.0F, 0.5F);
		});
	}

	private static List<EntityItem> dropsNear(int x, int z) {
		List<EntityItem> found = new ArrayList<>();
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, net.minecraft.util.AxisAlignedBB.getBoundingBox(x - 3, GROUND - 1, z - 3, x + 4, GROUND + 4, z + 4))) {
			found.add((EntityItem) entity);
		}
		return found;
	}

	/** The box's contents as "item xN" in slot order. */
	private static String contents(ItemStack box) {
		List<String> found = new ArrayList<>();
		if (box.hasTagCompound() && box.getTagCompound().hasKey("Items")) {
			NBTTagList items = box.getTagCompound().getTagList("Items", 10);
			for (int i = 0; i < items.tagCount(); i++) {
				ItemStack stack = ItemStack.loadItemStackFromNBT(items.getCompoundTagAt(i));
				if (stack != null) {
					found.add(Item.itemRegistry.getNameForObject(stack.getItem()) + " x" + stack.stackSize);
				}
			}
		}
		return found.toString();
	}

	// ---------------------------------------------------------------- ours, stored as EFR stored it

	/** A box stored in EFR's form comes back the same through ours; one of Iron Chest's tiers comes back as 27 slots. */
	public void testStoredFormKept() {
		String[] got = onServer(() -> {
			NBTTagCompound stored = new NBTTagCompound();
			stored.setString("id", "etfuturum.shulker_box");
			stored.setInteger("x", 1);
			stored.setInteger("y", 1);
			stored.setInteger("z", 1);
			NBTTagList items = new NBTTagList();
			for (int slot : new int[]{0, 26}) {
				NBTTagCompound one = new NBTTagCompound();
				one.setByte("Slot", (byte) slot);
				new ItemStack(Items.diamond, slot + 1).writeToNBT(one);
				items.appendTag(one);
			}
			stored.setTag("Items", items);
			stored.setByte("Color", (byte) 15);
			stored.setByte("Facing", (byte) 2);
			stored.setString("CustomName", "Loot");
			TileEntity loaded = TileEntity.createAndLoadEntity(stored);
			NBTTagCompound written = new NBTTagCompound();
			if (loaded != null) {
				loaded.writeToNBT(written);
			}
			NBTTagCompound tiered = (NBTTagCompound) stored.copy();
			tiered.setByte("Type", (byte) 1);
			TileEntity big = TileEntity.createAndLoadEntity(tiered);
			return new String[]{loaded == null ? "nothing" : written.equals(stored) ? "same" : written + " from " + stored,
				big instanceof IInventory ? String.valueOf(((IInventory) big).getSizeInventory()) : "nothing"};
		});
		assertEqual("same 27", got[0] + " " + got[1], "EFR's stored form through ours, a tiered box's size");
	}

	/** The classes EFR's shulker boxes were (spec §81): none of them is left. */
	static final String[] EFR_BOX_CLASSES = {"ganymedes01.etfuturum.blocks.BlockShulkerBox", "ganymedes01.etfuturum.tileentities.TileEntityShulkerBox",
		"ganymedes01.etfuturum.blocks.itemblocks.ItemBlockShulkerBox", "ganymedes01.etfuturum.client.renderer.tileentity.TileEntityShulkerBoxRenderer",
		"ganymedes01.etfuturum.client.renderer.item.ItemShulkerBoxRenderer", "ganymedes01.etfuturum.client.gui.inventory.GuiShulkerBox",
		"ganymedes01.etfuturum.recipes.crafting.RecipeDyedShulkerBox", "ganymedes01.etfuturum.dispenser.DispenserBehaviourShulkerBox",
		"ganymedes01.etfuturum.items.ItemShulkerBoxUpgrade"};

	/** The box is ours: block, item and tile entity McFly's, under the old ids; the shell McFly's too; none of EFR's box classes. */
	@SuppressWarnings("unchecked")
	public void testBoxIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		java.lang.reflect.Field names = cpw.mods.fml.relauncher.ReflectionHelper.findField(TileEntity.class, "nameToClassMap", "field_145855_i");
		Object tile = ((java.util.Map<String, Class<?>>) names.get(null)).get("etfuturum.shulker_box");
		for (Object thing : new Object[]{box(), Item.itemRegistry.getObject("etfuturum:shulker_box"), tile, Item.itemRegistry.getObject("etfuturum:shulker_shell")}) {
			String type = thing == null ? "nothing" : thing instanceof Class ? ((Class<?>) thing).getName() : thing.getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(type);
			}
		}
		for (String name : EFR_BOX_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes(name) != null) {
				wrong.add(name.substring(name.lastIndexOf('.') + 1) + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "the shulker box's classes");
	}

	// ---------------------------------------------------------------- as 26.3

	/**
	 * 26.3's opening: a closed box with a block where its lid would go doesn't open; with the way free it opens (the
	 * player's menu its 27 slots), its lid up in 10 ticks; closed, it shuts again in 10.
	 */
	public void testOpeningAndTheLid() throws Exception {
		int[] at = spot(0, 0);
		place(at[0], at[1], boxItem(0));
		// beside it: a player too far from a box has its menu closed again (as any container's)
		onServer(() -> {
			player().setPositionAndUpdate(at[0] + 0.5, GROUND, at[1] + 2.5);
			world().setBlock(at[0], GROUND + 1, at[1], Blocks.stone);
		});
		onServer(() -> box().onBlockActivated(world(), at[0], GROUND, at[1], player(), 1, 0.5F, 1.0F, 0.5F));
		ticks(2);
		boolean blocked = onServer(() -> player().openContainer == player().inventoryContainer);
		onServer(() -> world().setBlockToAir(at[0], GROUND + 1, at[1]));
		onServer(() -> box().onBlockActivated(world(), at[0], GROUND, at[1], player(), 1, 0.5F, 1.0F, 0.5F));
		ticks(2);
		int slots = onServer(() -> player().openContainer == player().inventoryContainer ? 0 : player().openContainer.inventorySlots.size() - 36);
		serverTicks(12);
		float open = onServer(() -> progress(world().getTileEntity(at[0], GROUND, at[1])));
		onServer(() -> player().closeScreen());
		serverTicks(12);
		float shut = onServer(() -> progress(world().getTileEntity(at[0], GROUND, at[1])));
		assertEqual("blocked true, 27 slots, open 1.0, shut 0.0", "blocked " + blocked + ", " + slots + " slots, open " + open + ", shut " + shut,
			"opening and the lid");
	}

	private static float progress(TileEntity tile) throws Exception {
		return (Float) tile.getClass().getMethod("getProgress", float.class).invoke(tile, 1.0F);
	}

	/** A piston breaks a box, which drops itself with its contents (26.3's push reaction DESTROY). */
	public void testPistonBreaksIt() {
		int[] at = spot(1, 0);
		place(at[0], at[1], boxItem(0, new ItemStack(Items.emerald, 5)));
		onServer(() -> {
			world().setBlock(at[0] - 1, GROUND, at[1], Blocks.piston, 5, 3);
			world().setBlock(at[0] - 2, GROUND, at[1], Blocks.redstone_block);
		});
		waitUntil("the box gone", () -> world().getBlock(at[0], GROUND, at[1]) != box(), 40);
		List<String> drops = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (EntityItem item : dropsNear(at[0], at[1])) {
				found.add(item.getEntityItem().getItem() == Item.getItemFromBlock(box()) ? "box " + contents(item.getEntityItem()) : "other");
			}
			return found;
		});
		assertEqual("[box [minecraft:emerald x5]]", drops.toString(), "what the piston left");
	}

	/** Broken in survival it drops itself with its contents; in creative, only when it holds something. */
	public void testDrops() {
		int[] full = spot(2, 0);
		int[] empty = spot(3, 0);
		int[] survival = spot(4, 0);
		place(full[0], full[1], boxItem(0, new ItemStack(Items.apple, 3)));
		place(empty[0], empty[1], boxItem(0));
		place(survival[0], survival[1], boxItem(3, new ItemStack(Items.bread, 2)));
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().theItemInWorldManager.tryHarvestBlock(full[0], GROUND, full[1]);
			player().theItemInWorldManager.tryHarvestBlock(empty[0], GROUND, empty[1]);
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().theItemInWorldManager.tryHarvestBlock(survival[0], GROUND, survival[1]);
		});
		ticks(2);
		String[] got = onServer(() -> {
			String[] found = new String[3];
			int[][] spots = {full, empty, survival};
			for (int i = 0; i < 3; i++) {
				List<String> here = new ArrayList<>();
				for (EntityItem item : dropsNear(spots[i][0], spots[i][1])) {
					if (Math.abs(item.posX - spots[i][0]) < 1.5 && Math.abs(item.posZ - spots[i][1]) < 1.5) {
						ItemStack stack = item.getEntityItem();
						here.add((stack.hasTagCompound() ? stack.getTagCompound().getByte("Color") : 0) + " " + contents(stack));
					}
				}
				found[i] = here.toString();
			}
			return found;
		});
		assertEqual("creative full [0 [minecraft:apple x3]], creative empty [], survival [3 [minecraft:bread x2]]",
			"creative full " + got[0] + ", creative empty " + got[1] + ", survival " + got[2], "the drops");
	}

	/** 26.3's tooltip: the first 5 stacks ("Apple x3"), then "and N more...". */
	public void testTooltip() {
		ItemStack box = boxItem(0, new ItemStack(Items.apple, 3), new ItemStack(Items.bread), new ItemStack(Items.coal, 2), new ItemStack(Items.diamond),
			new ItemStack(Items.emerald, 4), new ItemStack(Items.flint), new ItemStack(Items.gold_ingot, 7));
		List<String> lines = onClient(() -> {
			List<String> tooltip = new ArrayList<>();
			box.getItem().addInformation(box, mc().thePlayer, tooltip, false);
			List<String> plain = new ArrayList<>();
			for (String line : tooltip) {
				plain.add(net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(line));
			}
			return plain;
		});
		assertEqual("[Apple x3, Bread x1, Coal x2, Diamond x1, Emerald x4, and 2 more...]", lines.toString(), "the box's tooltip");
	}

	/** Dyed in the grid (a box and a dye: that colour, contents kept), washed in a water cauldron (plain, one level). */
	public void testDyeingAndWashing() {
		ItemStack box = boxItem(0, new ItemStack(Items.apple, 3));
		ItemStack dyed = onServer(() -> BannerTests.craft(new ItemStack[]{box, new ItemStack(Items.dye, 1, 1), null}, new ItemStack[3], new ItemStack[3]));
		String dyedSays = dyed == null ? "nothing" : (dyed.hasTagCompound() ? dyed.getTagCompound().getByte("Color") : 0) + " " + contents(dyed);
		int[] at = spot(5, 0);
		String washedSays = onServer(() -> {
			world().setBlock(at[0], GROUND, at[1], Blocks.cauldron, 3, 3);
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			ItemStack held = boxItem(15, new ItemStack(Items.apple, 3));
			player().inventory.setInventorySlotContents(player().inventory.currentItem, held);
			held.tryPlaceItemIntoWorld(player(), world(), at[0], GROUND, at[1], 1, 0.5F, 1.0F, 0.5F);
			ItemStack after = player().inventory.getCurrentItem();
			return (after == null ? "nothing" : (after.hasTagCompound() && after.getTagCompound().hasKey("Color") ? after.getTagCompound().getByte("Color") : 0) + " " + contents(after))
				+ ", level " + world().getBlockMetadata(at[0], GROUND, at[1]);
		});
		assertEqual("dyed 15 [minecraft:apple x3], washed 0 [minecraft:apple x3], level 2", "dyed " + dyedSays + ", washed " + washedSays, "dyeing and washing");
	}

	/** Hoppers (its sides) and its own slots take no shulker box, other things yes; a comparator reads it as a container. */
	public void testNoBoxesInsideAndTheComparator() {
		int[] at = spot(6, 0);
		place(at[0], at[1], boxItem(0, new ItemStack(Items.apple, 64), new ItemStack(Items.apple, 64)));
		String got = onServer(() -> {
			TileEntity tile = world().getTileEntity(at[0], GROUND, at[1]);
			ItemStack anotherBox = boxItem(0);
			boolean sideTakesBox = ((ISidedInventory) tile).canInsertItem(5, anotherBox, 1);
			boolean slotTakesBox = ((IInventory) tile).isItemValidForSlot(5, anotherBox);
			boolean takesStone = ((ISidedInventory) tile).canInsertItem(5, new ItemStack(Blocks.stone), 1);
			int signal = box().getComparatorInputOverride(world(), at[0], GROUND, at[1], 0);
			return "box in " + sideTakesBox + " " + slotTakesBox + ", stone in " + takesStone + ", signal " + signal + " (" + Container.calcRedstoneFromInventory((IInventory) tile) + ")";
		});
		assertEqual("box in false false, stone in true, signal 2 (2)", got, "boxes inside, the comparator (two full stacks of 27 slots: 2, as vanilla's formula)");
	}

	/**
	 * A dispenser places the box in front of it (26.3's `ShulkerBoxDispenseBehavior`): opening the dispenser's way when
	 * the block under that spot is empty, else up.
	 */
	public void testDispenserPlacesIt() {
		int[] at = spot(7, 0);
		onServer(() -> {
			for (int y : new int[]{GROUND, GROUND + 2}) {
				world().setBlock(at[0], y, at[1], Blocks.dispenser, 5, 3);
				world().setBlockMetadataWithNotify(at[0], y, at[1], 5, 2);
				((TileEntityDispenser) world().getTileEntity(at[0], y, at[1])).setInventorySlotContents(0, boxItem(4, new ItemStack(Items.apple)));
				world().setBlock(at[0] - 1, y, at[1], Blocks.redstone_block);
			}
		});
		waitUntil("both boxes placed", () -> world().getBlock(at[0] + 1, GROUND, at[1]) == box() && world().getBlock(at[0] + 1, GROUND + 2, at[1]) == box(), 20);
		String got = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (int y : new int[]{GROUND, GROUND + 2}) {
				NBTTagCompound written = new NBTTagCompound();
				world().getTileEntity(at[0] + 1, y, at[1]).writeToNBT(written);
				found.add("colour " + written.getByte("Color") + " facing " + written.getByte("Facing") + " items " + written.getTagList("Items", 10).tagCount());
			}
			return found.toString();
		});
		assertEqual("[colour 4 facing 1 items 1, colour 4 facing 5 items 1]", got, "the dispensed boxes: on the ground, over air");
	}
}
