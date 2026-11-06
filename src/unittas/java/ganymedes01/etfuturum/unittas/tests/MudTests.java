package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mud as our own (SPEC-modern-movement.md §90, step C), as 26.3's: mud, packed mud and mud bricks (EFR's one id), the
 * bricks' slab, stairs and wall. Ours, EFR's classes gone; EFR's stored form by 26.3's names; 26.3's strength, tools,
 * sounds and colours; mud's lower collision and its plants; packed mud dropping by hand, the bricks not; a water bottle
 * turning dirt or coarse dirt into mud from any face but below, the bottle given back (EFR's from below too), and from a
 * dispenser (EFR's none); 26.3's recipes. At z 320, y 8 and up.
 */
@TasSuite("caves")
public class MudTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 320;
	private static final String[] BLOCKS = {"mud", "packed_mud", "mud_brick_slab", "double_mud_brick_slab", "mud_brick_stairs", "mud_brick_wall"};

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
		});
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		});
	}

	private void clear() {
		for (int x = X - 2; x <= X + 2; x++) {
			for (int y = Y - 2; y <= Y + 3; y++) {
				for (int z = Z - 2; z <= Z + 2; z++) {
					world().setBlock(x, y, z, Blocks.air, 0, 2);
				}
			}
		}
		for (EntityItem item : items()) {
			item.setDead();
		}
	}

	private List<EntityItem> items() {
		List<EntityItem> found = new ArrayList<>();
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 3, Y - 3, Z - 3, X + 4, Y + 5, Z + 4))) {
			if (!((EntityItem) entity).isDead) {
				found.add((EntityItem) entity);
			}
		}
		return found;
	}

	private String dropped() {
		Map<String, Integer> counts = new java.util.TreeMap<>();
		for (EntityItem item : items()) {
			ItemStack stack = item.getEntityItem();
			counts.merge(Item.itemRegistry.getNameForObject(stack.getItem()).replace(McFly.DOMAIN + ":", "") + ":" + stack.getItemDamage(), stack.stackSize, Integer::sum);
		}
		return counts.toString();
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		return found == null ? "nothing" : found.type().id.replace("minecraft:", "");
	}

	private String breakSound(int x, int y, int z) {
		Block block = world().getBlock(x, y, z);
		String own = block.stepSound.getBreakSound();
		Object variants = MultiBlockSoundRegistry.multiBlockSounds.get(block);
		if (variants == null) {
			return own;
		}
		String got = ((ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer) variants).getSound(world(), x, y, z, own,
			MultiBlockSoundRegistry.BlockSoundType.BREAK);
		return got != null ? got : own;
	}

	private void harvest(ItemStack held) {
		player().inventory.setInventorySlotContents(player().inventory.currentItem, held);
		player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
	}

	/** The player uses what it holds on (X, Y, Z)'s face, as a right click there does on the server. */
	private void use(ItemStack held, int side) {
		player().inventory.setInventorySlotContents(player().inventory.currentItem, held);
		player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), held, X, Y, Z, side, 0.5F, 0.5F, 0.5F);
	}

	public void testMudIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : BLOCKS) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : new String[]{"blocks.BlockMud", "blocks.BlockPackedMud"}) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.add(name + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "mud's classes");
	}

	public void testStoredForm() {
		String got = onServer(() -> name(block("mud"), 0) + " " + name(block("packed_mud"), 0) + " " + name(block("packed_mud"), 1) + " "
			+ name(block("mud_brick_slab"), 0) + " " + name(block("mud_brick_stairs"), 0) + " " + name(block("mud_brick_wall"), 0));
		assertEqual("mud packed_mud mud_bricks mud_brick_slab mud_brick_stairs mud_brick_wall", got, "26.3's names");
	}

	/** 26.3's: mud 0.5 and 0.5, a shovel; packed mud 1 and 3, the bricks and their set 1.5 and 3, a pickaxe; each its own sound; colours. */
	public void testStrengthToolsSounds() {
		Object[][] want = {{"mud", 0, "0.5 0.5 shovel block.mud.break", MapColor.grayColor}, {"packed_mud", 0, "1.0 3.0 pickaxe block.packed_mud.break", MapColor.dirtColor},
			{"packed_mud", 1, "1.5 3.0 pickaxe block.mud_bricks.break", MapColor.dirtColor}, {"mud_brick_slab", 0, "1.5 3.0 pickaxe block.mud_bricks.break", MapColor.dirtColor},
			{"double_mud_brick_slab", 0, "1.5 3.0 pickaxe block.mud_bricks.break", MapColor.dirtColor},
			{"mud_brick_stairs", 0, "1.5 3.0 pickaxe block.mud_bricks.break", MapColor.dirtColor}, {"mud_brick_wall", 0, "1.5 3.0 pickaxe block.mud_bricks.break", MapColor.dirtColor}};
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (Object[] entry : want) {
				Block block = block((String) entry[0]);
				int meta = (Integer) entry[1];
				world().setBlock(X, Y, Z, block, meta, 2);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null, world(), X, Y, Z, X, Y, Z) + " "
					+ block.getHarvestTool(meta) + " " + breakSound(X, Y, Z).replace(McFly.MC_ASSETS + ":", "");
				if (!entry[2].equals(got) || block.getMapColor(meta) != entry[3]) {
					found.add(entry[0] + ":" + meta + " " + got + " colour " + block.getMapColor(meta).colorIndex);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "strength, tool, sound, colour (want: as listed)");
	}

	/** Mud's collision is 14 pixels high (26.3's `column(16, 0, 14)`); plants grow on it as on dirt. */
	public void testMudShapeAndPlants() {
		String got = onServer(() -> {
			Block mud = block("mud");
			world().setBlock(X, Y, Z, mud, 0, 2);
			AxisAlignedBB box = mud.getCollisionBoundingBoxFromPool(world(), X, Y, Z);
			boolean sapling = mud.canSustainPlant(world(), X, Y, Z, ForgeDirection.UP, (net.minecraftforge.common.IPlantable) Blocks.sapling);
			return (box == null ? "none" : String.valueOf(box.maxY - Y)) + " sapling " + sapling;
		});
		assertEqual("0.875 sapling true", got, "mud's collision top and a sapling on it");
	}

	/** Packed mud drops itself by hand (26.3's: no tool rule); mud bricks need a pickaxe. */
	public void testPackedMudByHand() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (int meta = 0; meta < 2; meta++) {
				world().setBlock(X, Y, Z, block("packed_mud"), meta, 2);
				harvest(null);
				out.append(dropped()).append(' ');
				clear();
			}
			world().setBlock(X, Y, Z, block("packed_mud"), 1, 2);
			harvest(new ItemStack(Items.wooden_pickaxe));
			return out.append(dropped()).toString();
		});
		assertEqual("{packed_mud:0=1} {} {packed_mud:1=1}", got, "packed mud by hand, mud bricks by hand, by a pickaxe");
	}

	/** A water bottle on dirt's top: mud, the bottle given back; on its bottom: nothing (26.3's); on podzol: nothing. */
	public void testWaterBottleMakesMud() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			world().setBlock(X, Y, Z, Blocks.dirt, 0, 2);
			use(new ItemStack(Items.potionitem, 1, 0), 1);
			ItemStack held = player().getHeldItem();
			out.append(name(world().getBlock(X, Y, Z), world().getBlockMetadata(X, Y, Z))).append(" held ")
				.append(held == null ? "nothing" : Item.itemRegistry.getNameForObject(held.getItem())).append("; ");
			world().setBlock(X, Y, Z, Blocks.dirt, 0, 2);
			use(new ItemStack(Items.potionitem, 1, 0), 0);
			out.append("from below ").append(name(world().getBlock(X, Y, Z), 0)).append("; ");
			world().setBlock(X, Y, Z, Blocks.dirt, 1, 2);
			use(new ItemStack(Items.potionitem, 1, 0), 1);
			out.append("coarse ").append(name(world().getBlock(X, Y, Z), 0)).append("; ");
			world().setBlock(X, Y, Z, Blocks.dirt, 2, 2);
			use(new ItemStack(Items.potionitem, 1, 0), 1);
			return out.append("podzol ").append(world().getBlock(X, Y, Z) == Blocks.dirt ? "stays" : "changed").toString();
		});
		assertEqual("mud held minecraft:glass_bottle; from below dirt; coarse mud; podzol stays", got, "water bottles on dirt");
	}

	/** A dispenser with a water bottle facing dirt: mud, the bottle left in the dispenser (26.3's). */
	public void testDispenserMakesMud() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, Blocks.dirt, 0, 2);
			// facing north (2): the dirt at Z is north of it
			world().setBlock(X, Y, Z + 1, Blocks.dispenser, 2, 2);
			// 1.7.10's dispenser turns away from solid blocks when placed: faced at the dirt again
			world().setBlockMetadataWithNotify(X, Y, Z + 1, 2, 2);
			TileEntityDispenser dispenser = (TileEntityDispenser) world().getTileEntity(X, Y, Z + 1);
			dispenser.setInventorySlotContents(0, new ItemStack(Items.potionitem, 1, 0));
			// 1.7.10's dispenser fires on the scheduled tick after it's powered: that tick
			Blocks.dispenser.updateTick(world(), X, Y, Z + 1, world().rand);
			ItemStack left = dispenser.getStackInSlot(0);
			String result = name(world().getBlock(X, Y, Z), 0) + " left " + (left == null ? "nothing" : Item.itemRegistry.getNameForObject(left.getItem()));
			world().setBlock(X, Y, Z + 1, Blocks.air, 0, 2);
			return result;
		});
		assertEqual("mud left minecraft:glass_bottle", got, "a dispensed water bottle on dirt");
	}

	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("mud_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(5, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}
}
