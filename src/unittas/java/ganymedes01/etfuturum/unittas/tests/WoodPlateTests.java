package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The wood sets' pressure plates as our own (SPEC-modern-movement.md §91.7, step E), as 26.3's: EFR's ids and metas,
 * strength, no fire, sounds, colours; pressed by anything (wood's set) or by mobs alone (stone's), up 20 ticks after,
 * its clicks by the set type; placed on a wall's top (26.3's `canSupportCenter`); drops, fuel, ore names and names. At
 * z 660, y 8.
 */
@TasSuite("woods")
public class WoodPlateTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 660;
	private static final String MODERN = McFly.MC_ASSETS + ":";
	private static final String[] WOODS = {"crimson", "warped", "mangrove", "cherry", "bamboo"};
	private static final String[] OLD = {"spruce", "birch", "jungle", "acacia", "dark_oak"};

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

	/** x 36 to 44, y 7 to 11, z 656 to 664 emptied, a stone floor under it, its items and pigs gone. */
	private void clear() {
		for (int x = X - 4; x <= X + 4; x++) {
			for (int z = Z - 4; z <= Z + 4; z++) {
				for (int y = Y; y <= Y + 3; y++) {
					world().setBlock(x, y, z, Blocks.air, 0, 2);
				}
				world().setBlock(x, Y - 1, z, Blocks.stone, 0, 2);
			}
		}
		AxisAlignedBB area = AxisAlignedBB.getBoundingBox(X - 5, Y - 2, Z - 5, X + 6, Y + 5, Z + 6);
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, area)) {
			((EntityItem) entity).setDead();
		}
		for (Object entity : world().getEntitiesWithinAABB(EntityPig.class, area)) {
			((EntityPig) entity).setDead();
		}
	}

	private String dropped() {
		Map<String, Integer> counts = new java.util.TreeMap<>();
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 5, Y - 2, Z - 5, X + 6, Y + 5, Z + 6))) {
			EntityItem item = (EntityItem) entity;
			if (!item.isDead) {
				counts.merge(Recipes26_3.nameOf(item.getEntityItem()), item.getEntityItem().stackSize, Integer::sum);
				item.setDead();
			}
		}
		return counts.toString();
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(name.contains(":") ? name : McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + name);
		return (Block) block;
	}

	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		String name = found.type().id.replace("minecraft:", "");
		return "true".equals(found.state().properties().get("powered")) ? name + ",powered" : name;
	}

	private String breakSound(int x, int y, int z) {
		Block block = world().getBlock(x, y, z);
		String own = block.stepSound.getBreakSound();
		Object variants = MultiBlockSoundRegistry.multiBlockSounds.get(block);
		if (variants == null) {
			return own;
		}
		String got = ((MultiBlockSoundContainer) variants).getSound(world(), x, y, z, own, MultiBlockSoundRegistry.BlockSoundType.BREAK);
		return got != null ? got : own;
	}

	private int meta() {
		return world().getBlockMetadata(X, Y, Z);
	}

	// ---------------------------------------------------------------- ours, stored form, properties

	public void testPlatesAreOurs() {
		List<String> wrong = new ArrayList<>();
		List<String> names = new ArrayList<>();
		for (String wood : OLD) {
			names.add("pressure_plate_" + wood);
		}
		for (String wood : WOODS) {
			names.add(wood + "_pressure_plate");
		}
		for (String name : names) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		try {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum.blocks.BlockWoodPressurePlate") != null) {
				wrong.add("blocks.BlockWoodPressurePlate still there");
			}
		} catch (java.io.IOException e) {
			wrong.add(e.toString());
		}
		assertEqual("[]", wrong.toString(), "the plates' classes");
	}

	public void testStoredForm() {
		String got = onServer(() -> name(block("pressure_plate_birch"), 0) + " " + name(block("pressure_plate_birch"), 1) + " "
			+ name(block("crimson_pressure_plate"), 1));
		assertEqual("birch_pressure_plate birch_pressure_plate,powered crimson_pressure_plate,powered", got, "26.3's names");
	}

	/** 26.3's: 0.5, the axe's, no fire; the wood's sound; the planks' colour. */
	public void testProperties() {
		String[] newSounds = {"block.nether_wood.break", "block.nether_wood.break", "dig.wood", "block.cherry_wood.break", "block.bamboo_wood.break"};
		MapColor[] newColours = {MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.clothColor, MapColor.yellowColor};
		MapColor[] oldColours = {MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor, MapColor.brownColor};
		List<Object[]> want = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			want.add(new Object[]{WOODS[i] + "_pressure_plate", newSounds[i], newColours[i]});
			want.add(new Object[]{"pressure_plate_" + OLD[i], "dig.wood", oldColours[i]});
		}
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (Object[] entry : want) {
				Block block = block((String) entry[0]);
				world().setBlock(X, Y, Z, block, 0, 2);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null) + " "
					+ block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP) + " " + block.getFlammability(world(), X, Y, Z, ForgeDirection.UP) + " "
					+ block.getHarvestTool(0) + " " + breakSound(X, Y, Z).replace(MODERN, "");
				if (!("0.5 0.5 0 0 axe " + entry[1]).equals(got) || block.getMapColor(0) != entry[2]) {
					found.add(entry[0] + " " + got + " colour " + block.getMapColor(0).colorIndex);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, resistance, fire, tool, sound, colour (want: as listed)");
	}

	// ---------------------------------------------------------------- pressing, clicks, placing

	/**
	 * 26.3's sensing: a wooden plate by anything (an item), vanilla's stone one by mobs alone (an item doesn't, a pig
	 * does); up again 20 ticks after it's gone.
	 */
	public void testSensing() {
		standBy();
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, block("cherry_pressure_plate"), 0, 3);
			world().setBlock(X + 2, Y, Z, Blocks.stone_pressure_plate, 0, 3);
			EntityItem item = new EntityItem(world(), X + 0.5D, Y + 0.3D, Z + 0.5D, new ItemStack(Items.stick));
			item.delayBeforeCanPickup = 1000;
			// no hop off the plate (an item starts with a random one); spawned clear of the floor, so nothing pushes it aside
			item.motionX = item.motionY = item.motionZ = 0.0D;
			world().spawnEntityInWorld(item);
			EntityItem other = new EntityItem(world(), X + 2.5D, Y + 0.3D, Z + 0.5D, new ItemStack(Items.stick));
			other.delayBeforeCanPickup = 1000;
			// no hop off the plate (an item starts with a random one); spawned clear of the floor, so nothing pushes it aside
			other.motionX = other.motionY = other.motionZ = 0.0D;
			world().spawnEntityInWorld(other);
			return "";
		});
		serverTicks(10);
		String pressed = onServer(() -> meta() + " " + world().getBlockMetadata(X + 2, Y, Z));
		onServer(() -> {
			clearItems();
			EntityPig pig = new EntityPig(world());
			pig.setLocationAndAngles(X + 2.5D, Y + 0.1D, Z + 0.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(pig);
		});
		serverTicks(25);
		String afterItems = onServer(() -> meta() + " " + world().getBlockMetadata(X + 2, Y, Z));
		assertEqual("1 0, 0 1", pressed + ", " + afterItems, "the wooden and stone plates: with items on both; the items gone, a pig on the stone one");
	}

	/** The player by the plates, and the chunks round it loaded, so what's put on them ticks. */
	private void standBy() {
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z + 3.5D, 180.0F, 0.0F));
		serverTicks(20);
	}

	private void clearItems() {
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 5, Y - 2, Z - 5, X + 6, Y + 5, Z + 6))) {
			((EntityItem) entity).setDead();
		}
	}

	/** The set type's clicks: cherry's on and off; the oak set's for vanilla's wooden plate. */
	public void testClicks() {
		for (String[] plate : new String[][]{{"cherry_pressure_plate", "block.cherry_wood_pressure_plate."},
			{"minecraft:wooden_pressure_plate", "block.wooden_pressure_plate."}}) {
			standBy();
			onServer(() -> world().setBlock(X, Y, Z, block(plate[0]), 0, 3));
			serverTicks(2);
			clearSounds();
			onServer(() -> {
				EntityItem item = new EntityItem(world(), X + 0.5D, Y + 0.3D, Z + 0.5D, new ItemStack(Items.stick));
				item.delayBeforeCanPickup = 1000;
				// no hop off the plate (an item starts with a random one); spawned clear of the floor, so nothing pushes it aside
				item.motionX = item.motionY = item.motionZ = 0.0D;
				world().spawnEntityInWorld(item);
			});
			waitForSound(MODERN + plate[1] + "click_on", 20);
			onServer(this::clearItems);
			waitForSound(MODERN + plate[1] + "click_off", 45);
		}
	}

	/** 26.3's `canSupportCenter`: a plate stands on a wall's top (and a fence's, as 1.7.10's did). */
	public void testOnAWall() {
		String got = onServer(() -> {
			Block plate = block("pressure_plate_spruce");
			world().setBlock(X, Y, Z, Blocks.cobblestone_wall, 0, 2);
			boolean wall = plate.canPlaceBlockAt(world(), X, Y + 1, Z);
			world().setBlock(X, Y, Z, Blocks.fence, 0, 2);
			boolean fence = plate.canPlaceBlockAt(world(), X, Y + 1, Z);
			return wall + " " + fence;
		});
		assertEqual("true true", got, "placeable on a wall, on a fence");
	}

	// ---------------------------------------------------------------- drops, fuel, ore names, names

	public void testDrops() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, block("bamboo_pressure_plate"), 0, 2);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
			return dropped();
		});
		assertEqual("{bamboo_pressure_plate=1}", got, "drops by hand");
	}

	/** 26.3's fuel: 300, crimson's and warped's none. */
	public void testFuel() {
		StringBuilder got = new StringBuilder();
		for (String name : new String[]{"warped_pressure_plate", "cherry_pressure_plate", "pressure_plate_spruce"}) {
			got.append(got.length() == 0 ? "" : " ").append(TileEntityFurnace.getItemBurnTime(new ItemStack(block(name))));
		}
		assertEqual("0 300 300", got.toString(), "burn times");
	}

	public void testOreNames() {
		List<String> missing = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			for (String name : new String[]{WOODS[i] + "_pressure_plate", "pressure_plate_" + OLD[i]}) {
				boolean has = false;
				for (int id : OreDictionary.getOreIDs(new ItemStack(block(name)))) {
					has |= "pressurePlateWood".equals(OreDictionary.getOreName(id));
				}
				if (!has) {
					missing.add(name);
				}
			}
		}
		assertEqual("[]", missing.toString(), "plates without pressurePlateWood");
	}

	public void testNames() {
		String got = onClient(() -> {
			StringBuilder names = new StringBuilder();
			for (String name : new String[]{"pressure_plate_birch", "bamboo_pressure_plate", "minecraft:wooden_pressure_plate"}) {
				names.append(names.length() == 0 ? "" : ", ").append(new ItemStack(block(name)).getDisplayName());
			}
			return names.toString();
		});
		assertEqual("Birch Pressure Plate, Bamboo Pressure Plate, Oak Pressure Plate", got, "names");
	}
}
