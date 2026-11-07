package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * The smoker and blast furnace (SPEC-modern-movement.md 92.3, step A), as 26.3's `AbstractFurnaceBlockEntity` on EFR's
 * ids and NBT: 100 ticks an item, fuel lasting half as long, lit while it burns, 26.3's smoking and blasting lists,
 * experience by recipes used, hoppers by face, the comparator. At z 800, y 8, on stone. Read through block names, tile
 * entity NBT and class names, so it runs on EFR's (and fails there).
 */
@TasSuite("workstations")
public class SmokerTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 800;

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z - 4.5D, 0.0F, 0.0F);
		});
		serverTicks(2);
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	/** x 35 to 45, y 7 to 11, z 795 to 805: stone at y 7, air above; no items or orbs. */
	private static void clear() {
		for (int x = X - 5; x <= X + 5; x++) {
			for (int z = Z - 5; z <= Z + 5; z++) {
				for (int y = Y - 1; y <= Y + 3; y++) {
					world().setBlock(x, y, z, y == Y - 1 ? Blocks.stone : Blocks.air, 0, 2);
				}
			}
		}
		for (Object entity : world().getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(X - 6, Y - 3, Z - 6, X + 7, Y + 6, Z + 7))) {
			if (entity instanceof EntityItem || entity instanceof EntityXPOrb) {
				((Entity) entity).setDead();
			}
		}
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject("etfuturum:" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no block etfuturum:" + name);
		return (Block) block;
	}

	/** A cooker at the middle, facing north (meta 2), holding these. */
	private static IInventory place(String name, ItemStack input, ItemStack fuel) {
		world().setBlock(X, Y, Z, block(name), 2, 3);
		IInventory cooker = (IInventory) world().getTileEntity(X, Y, Z);
		cooker.setInventorySlotContents(0, input);
		cooker.setInventorySlotContents(1, fuel);
		return cooker;
	}

	private static NBTTagCompound saved() {
		NBTTagCompound tag = new NBTTagCompound();
		world().getTileEntity(X, Y, Z).writeToNBT(tag);
		return tag;
	}

	private static String result(IInventory cooker) {
		ItemStack stack = cooker.getStackInSlot(2);
		return stack == null ? "nothing" : stack.stackSize + " " + Recipes26_3.nameOf(stack);
	}

	// ---------------------------------------------------------------- ours

	public void testOurs() {
		List<String> wrong = new ArrayList<>();
		for (String name : new String[]{"smoker", "lit_smoker", "blast_furnace", "lit_blast_furnace"}) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		String tile = onServer(() -> {
			world().setBlock(X, Y, Z, block("smoker"), 2, 3);
			TileEntity entity = world().getTileEntity(X, Y, Z);
			return entity == null ? "none" : entity.getClass().getName();
		});
		if (!tile.startsWith("io.github.asgerjon.mcfly.")) {
			wrong.add("tile " + tile);
		}
		for (String name : new String[]{"blocks.BlockSmoker", "blocks.BlockBlastFurnace", "tileentities.TileEntitySmoker", "tileentities.TileEntityBlastFurnace",
			"inventory.ContainerSmoker", "inventory.ContainerBlastFurnace", "client.gui.inventory.GuiSmoker", "client.gui.inventory.GuiBlastFurnace",
			"recipes.SmokerRecipes", "recipes.BlastFurnaceRecipes"}) {
			try {
				if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
					wrong.add(name + " still there");
				}
			} catch (java.io.IOException e) {
				wrong.add(name + " " + e);
			}
		}
		assertEqual("[]", wrong.toString(), "the smoker's and blast furnace's classes");
	}

	/** EFR's `Items`, `BurnTime`, `CookTime`, `CustomName` read back; 26.3's names written. */
	public void testStoredForm() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, block("smoker"), 2, 3);
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("id", "etfuturum.smoker");
			tag.setInteger("x", X);
			tag.setInteger("y", Y);
			tag.setInteger("z", Z);
			NBTTagList items = new NBTTagList();
			NBTTagCompound beef = new NBTTagCompound();
			new ItemStack(Items.beef, 5).writeToNBT(beef);
			beef.setByte("Slot", (byte) 0);
			items.appendTag(beef);
			tag.setTag("Items", items);
			tag.setInteger("BurnTime", 300);
			tag.setShort("CookTime", (short) 40);
			tag.setString("CustomName", "Smoky");
			world().getTileEntity(X, Y, Z).readFromNBT(tag);
			NBTTagCompound saved = saved();
			NBTTagCompound first = saved.getTagList("Items", 10).getCompoundTagAt(0);
			ItemStack stack = ItemStack.loadItemStackFromNBT(first);
			return (stack == null ? "none" : Recipes26_3.nameOf(stack) + " " + stack.stackSize) + ", lit " + saved.getInteger("lit_time_remaining") + ", cook "
				+ saved.getInteger("cooking_time_spent") + ", name " + saved.getString("CustomName");
		});
		assertEqual("beef 5, lit 300, cook 40, name Smoky", got, "EFR's tag read, 26.3's written");
	}

	// ---------------------------------------------------------------- cooking

	/** Beef to steak in 100 ticks; a coal's 1600 halved to 800. */
	public void testSmokerCooks() {
		IInventory smoker = onServer(() -> place("smoker", new ItemStack(Items.beef, 3), new ItemStack(Items.coal)));
		serverTicks(94);
		String early = onServer(() -> result(smoker));
		serverTicks(12);
		String done = onServer(() -> result(smoker) + ", lit total " + saved().getInteger("lit_total_time"));
		assertEqual("nothing; 1 cooked_beef, lit total 800", early + "; " + done, "after 94 ticks; after 106");
	}

	/** Iron ore to an ingot in 100 ticks; beef has no blasting recipe, so the fuel waits and it stays unlit. */
	public void testBlastFurnace() {
		IInventory furnace = onServer(() -> place("blast_furnace", new ItemStack(Blocks.iron_ore), new ItemStack(Items.coal)));
		serverTicks(106);
		String ore = onServer(() -> result(furnace));
		IInventory refused = onServer(() -> place("blast_furnace", new ItemStack(Items.beef), new ItemStack(Items.coal)));
		serverTicks(20);
		String beef = onServer(() -> {
			ItemStack fuel = refused.getStackInSlot(1);
			return result(refused) + ", fuel " + (fuel == null ? 0 : fuel.stackSize) + ", " + Block.blockRegistry.getNameForObject(world().getBlock(X, Y, Z));
		});
		assertEqual("1 iron_ingot; beef: nothing, fuel 1, etfuturum:blast_furnace", ore + "; beef: " + beef, "iron ore; beef");
	}

	/** Lit (its lit block, light 13) while a stick burns (100 halved to 50), unlit after. */
	public void testLit() {
		onServer(() -> place("smoker", new ItemStack(Items.beef), new ItemStack(Items.stick)));
		serverTicks(10);
		String burning = onServer(() -> Block.blockRegistry.getNameForObject(world().getBlock(X, Y, Z)) + " " + world().getBlock(X, Y, Z).getLightValue());
		serverTicks(60);
		String after = onServer(() -> Block.blockRegistry.getNameForObject(world().getBlock(X, Y, Z)));
		assertEqual("etfuturum:lit_smoker 13; etfuturum:smoker", burning + "; " + after, "while a stick burns; after");
	}

	/** 26.3's smoking and blasting lists, and the two blocks' crafting (`workstations_cooking_recipes_26_3.txt`). */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("workstations_cooking_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(35, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}

	/** Two ingots: `RecipesUsed` 2 of 26.3's recipe; broken, it pops their experience (0.7 each: 1 or 2). */
	public void testExperience() {
		onServer(() -> place("blast_furnace", new ItemStack(Blocks.iron_ore, 2), new ItemStack(Items.coal)));
		serverTicks(206);
		String used = onServer(() -> {
			NBTTagCompound recipes = saved().getCompoundTag("RecipesUsed");
			return String.valueOf(recipes.getInteger("minecraft:iron_ingot_from_blasting_iron_ore"));
		});
		int experience = onServer(() -> {
			world().func_147480_a(X, Y, Z, true);
			int total = 0;
			for (Object orb : world().getEntitiesWithinAABB(EntityXPOrb.class, AxisAlignedBB.getBoundingBox(X - 2, Y - 1, Z - 2, X + 3, Y + 3, Z + 3))) {
				total += ((EntityXPOrb) orb).getXpValue();
			}
			return total;
		});
		assertEqual("used 2, experience 1 to 2", "used " + used + ", experience " + (experience >= 1 && experience <= 2 ? "1 to 2" : String.valueOf(experience)),
			"recipes used; popped when broken");
	}

	/** Hoppers: from above into the input, from the side into the fuel; one below takes the steak. */
	public void testHoppers() {
		onServer(() -> {
			world().setBlock(X, Y, Z, block("smoker"), 2, 3);
			world().setBlock(X, Y + 1, Z, Blocks.hopper, 0, 3);
			((TileEntityHopper) world().getTileEntity(X, Y + 1, Z)).setInventorySlotContents(0, new ItemStack(Items.beef));
			// facing west (4), into the smoker's east side
			world().setBlock(X + 1, Y, Z, Blocks.hopper, 4, 3);
			((TileEntityHopper) world().getTileEntity(X + 1, Y, Z)).setInventorySlotContents(0, new ItemStack(Items.coal));
			world().setBlock(X, Y - 1, Z, Blocks.hopper, 0, 3);
		});
		serverTicks(140);
		String got = onServer(() -> {
			ItemStack below = ((TileEntityHopper) world().getTileEntity(X, Y - 1, Z)).getStackInSlot(0);
			IInventory smoker = (IInventory) world().getTileEntity(X, Y, Z);
			ItemStack fuel = smoker.getStackInSlot(1);
			return "below " + (below == null ? "nothing" : below.stackSize + " " + Recipes26_3.nameOf(below)) + ", fuel left " + (fuel == null ? 0 : fuel.stackSize);
		});
		assertEqual("below 1 cooked_beef, fuel left 0", got, "beef from above, coal from the side, steak out below");
	}

	/** One beef in the input: 1 (26.3's and 1.7.10's fullness rule). */
	public void testComparator() {
		int got = onServer(() -> {
			place("smoker", new ItemStack(Items.beef), null);
			return world().getBlock(X, Y, Z).getComparatorInputOverride(world(), X, Y, Z, 0);
		});
		assertEqual(1, got, "the comparator by one beef");
	}

	/**
	 * Broken by the player with a pickaxe, a named smoker drops itself named so (26.3's loot copies `custom_name`; 92.4
	 * found A without it).
	 */
	public void testDropKeepsItsName() {
		String got = onServer(() -> {
			place("smoker", null, null);
			TileEntity tile = world().getTileEntity(X, Y, Z);
			NBTTagCompound tag = new NBTTagCompound();
			tile.writeToNBT(tag);
			tag.setString("CustomName", "Smoky");
			tile.readFromNBT(tag);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.wooden_pickaxe));
			player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			List<String> found = new ArrayList<>();
			for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 3, Y - 2, Z - 3, X + 4, Y + 4, Z + 4))) {
				ItemStack stack = ((EntityItem) entity).getEntityItem();
				found.add(stack.stackSize + " " + Recipes26_3.nameOf(stack) + (stack.hasDisplayName() ? " named " + stack.getDisplayName() : ""));
			}
			return found.toString();
		});
		assertEqual("[1 smoker named Smoky]", got, "what breaking a named smoker drops");
	}

	/** 26.3's names (`block.minecraft.smoker`, `blast_furnace`) and strength (3.5), a pickaxe's. */
	public void testNamesAndStrength() {
		String names = onClient(() -> new ItemStack(block("smoker")).getDisplayName() + ", " + new ItemStack(block("blast_furnace")).getDisplayName()
			+ ", " + StatCollector.translateToLocal("container.smoker") + ", " + StatCollector.translateToLocal("container.blast_furnace"));
		String strength = onServer(() -> {
			Block smoker = block("smoker");
			return smoker.getBlockHardness(world(), X, Y, Z) + " " + smoker.getHarvestTool(2) + " " + block("blast_furnace").getBlockHardness(world(), X, Y, Z);
		});
		assertEqual("Smoker, Blast Furnace, Smoker, Blast Furnace; 3.5 pickaxe 3.5", names + "; " + strength, "names; strength and tool");
	}
}
