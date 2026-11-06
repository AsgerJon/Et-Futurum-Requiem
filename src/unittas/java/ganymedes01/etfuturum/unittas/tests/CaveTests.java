package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Glow lichen and cave vines as our own (SPEC-modern-movement.md §90.3, step A), as 26.3's: ours, EFR's classes gone;
 * EFR's stored form read back (a lichen's faces in its `State`, a head's `MaxLength` and `TipSheared`, the berries in meta
 * bit 0); 26.3's properties; a sheared lichen dropping one a face, nothing otherwise; a lichen losing its faces with their
 * support, no drops; bonemeal spreading a lichen; vines growing by random ticks, a cut vine falling with its berries,
 * berries picked out at the block; glow berries' food and the compost chances. Asked of the world, not of a class, so
 * that the same tests judge EFR's blocks. At z 280, y 8 and up (air below for vines).
 */
@TasSuite("caves")
public class CaveTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 280;
	private static final String[] EFR_CLASSES = {"blocks.BlockGlowLichen", "tileentities.TileEntityGlowLichen", "blocks.itemblocks.ItemBlockGlowLichen",
		"api.event.GlowLichenHighlightEvent", "client.renderer.boundingbox.GlowLichenBoundingBoxRenderer", "client.renderer.block.BlockGlowLichenRenderer",
		"blocks.BaseCaveVines", "blocks.BlockCaveVines", "blocks.BlockCaveVinesPlant", "tileentities.TileEntityCaveVines", "items.ItemGlowBerries"};
	/** Faces as EFR's `State` and 26.3's `Direction` order hold them. */
	private static final int DOWN = 1, UP = 1 << 1, NORTH = 1 << 2;

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

	/** The test's space (x 38 to 42, y 6 to 13) emptied, its dropped items gone (on the server thread). */
	private void clear() {
		for (int x = X - 2; x <= X + 2; x++) {
			for (int y = Y - 2; y <= Y + 5; y++) {
				for (int z = Z - 2; z <= Z + 2; z++) {
					world().setBlock(x, y, z, Blocks.air, 0, 2);
				}
			}
		}
		for (Object entity : items()) {
			((EntityItem) entity).setDead();
		}
	}

	private List<EntityItem> items() {
		List<EntityItem> found = new ArrayList<>();
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 3, Y - 3, Z - 3, X + 4, Y + 7, Z + 4))) {
			if (!((EntityItem) entity).isDead) {
				found.add((EntityItem) entity);
			}
		}
		return found;
	}

	/** How many of this item lie dropped round the test's space. */
	private int dropped(Item item) {
		int count = 0;
		for (EntityItem entity : items()) {
			if (entity.getEntityItem().getItem() == item) {
				count += entity.getEntityItem().stackSize;
			}
		}
		return count;
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	private static Item berries() {
		Object item = Item.itemRegistry.getObject(McFly.DOMAIN + ":glow_berries_item");
		assertTrue(item instanceof Item, "no " + McFly.DOMAIN + ":glow_berries_item");
		return (Item) item;
	}

	/** A tile entity there as EFR wrote it into a chunk. */
	private void loadTileEntity(int x, int y, int z, String id, NBTTagCompound data) {
		data.setString("id", id);
		data.setInteger("x", x);
		data.setInteger("y", y);
		data.setInteger("z", z);
		TileEntity tile = TileEntity.createAndLoadEntity(data);
		assertTrue(tile != null, "no tile entity " + id);
		world().setTileEntity(x, y, z, tile);
	}

	/** A lichen at (X, Y, Z) with EFR's faces, stone behind each. */
	private void lichen(int faces) {
		int[][] offsets = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
		for (int face = 0; face < 6; face++) {
			if ((faces & 1 << face) != 0) {
				world().setBlock(X + offsets[face][0], Y + offsets[face][1], Z + offsets[face][2], Blocks.stone, 0, 2);
			}
		}
		world().setBlock(X, Y, Z, block("glow_lichen"), 0, 2);
		NBTTagCompound data = new NBTTagCompound();
		data.setInteger("State", faces);
		loadTileEntity(X, Y, Z, McFly.DOMAIN + ".glow_lichen", data);
	}

	/** The player breaks (X, Y, Z) by hand with this held, as survival does. */
	private void harvest(ItemStack held) {
		player().inventory.setInventorySlotContents(player().inventory.currentItem, held);
		player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
	}

	/** A random tick at (x, y, z): 26.3's own where the block keeps it apart, else 1.7.10's. */
	private void randomTick(int x, int y, int z, Random random) {
		Block block = world().getBlock(x, y, z);
		if (block instanceof io.github.asgerjon.mcfly.base.RandomTicking ticking) {
			ticking.randomTick(world(), x, y, z, random);
		} else {
			block.updateTick(world(), x, y, z, random);
		}
	}

	public void testCaveBlocksAreOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : new String[]{"glow_lichen", "cave_vine", "cave_vine_plant"}) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		if (!berries().getClass().getName().startsWith("io.github.asgerjon.mcfly.")) {
			wrong.add("glow_berries_item " + berries().getClass().getName());
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.add(name + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "the lichen's and the vines' classes");
	}

	/** EFR's lichen with two faces (up and north), sheared: 26.3's one a face. By hand: nothing. */
	public void testShearedLichenDropsOneAFace() {
		String got = onServer(() -> {
			lichen(UP | NORTH);
			harvest(new ItemStack(Items.shears));
			int sheared = dropped(Item.getItemFromBlock(block("glow_lichen")));
			clear();
			lichen(UP | NORTH);
			harvest(null);
			return "sheared " + sheared + ", by hand " + dropped(Item.getItemFromBlock(block("glow_lichen")));
		});
		assertEqual("sheared 2, by hand 0", got, "a two-faced lichen's drops");
	}

	/** EFR's lichen (down and north faces): its north support gone, the down face stays; the floor gone too, it's air, no drops. */
	public void testLichenLosesFacesWithTheirSupport() {
		String got = onServer(() -> {
			lichen(DOWN | NORTH);
			world().setBlock(X, Y, Z - 1, Blocks.air, 0, 3);
			String after = world().getBlock(X, Y, Z) == block("glow_lichen") ? "stays" : "gone";
			world().setBlock(X, Y - 1, Z, Blocks.air, 0, 3);
			return "north gone: " + after + ", floor gone: " + (world().isAirBlock(X, Y, Z) ? "air" : "still there") + ", dropped "
				+ dropped(Item.getItemFromBlock(block("glow_lichen")));
		});
		assertEqual("north gone: stays, floor gone: air, dropped 0", got, "a lichen losing its supports");
	}

	/** 26.3's: 0.2, light 7, its sound (GLOW_LICHEN: grass's break, the vine's step), fire 15 and 100, the axe its tool, no collision, replaceable. */
	public void testLichenProperties() {
		String got = onServer(() -> {
			lichen(DOWN);
			Block lichen = block("glow_lichen");
			return lichen.getBlockHardness(world(), X, Y, Z) + " " + lichen.getLightValue(world(), X, Y, Z) + " " + lichen.stepSound.getBreakSound() + " " + lichen.stepSound.getStepResourcePath() + " "
				+ lichen.getFireSpreadSpeed(world(), X, Y, Z, net.minecraftforge.common.util.ForgeDirection.UP) + " "
				+ lichen.getFlammability(world(), X, Y, Z, net.minecraftforge.common.util.ForgeDirection.UP) + " " + lichen.getHarvestTool(0) + " "
				+ (lichen.getCollisionBoundingBoxFromPool(world(), X, Y, Z) == null) + " " + lichen.isReplaceable(world(), X, Y, Z);
		});
		assertEqual("0.2 7 dig.grass " + McFly.MC_ASSETS + ":block.vine.step 15 100 axe true true", got,
			"hardness, light, sound, fire, tool, no collision, replaceable");
	}

	/** Bonemeal on a lichen on a floor of stone: one more face, spread to a neighbour's floor (26.3's same plane). */
	public void testBonemealSpreadsLichen() {
		String got = onServer(() -> {
			for (int x = X - 1; x <= X + 1; x++) {
				for (int z = Z - 1; z <= Z + 1; z++) {
					world().setBlock(x, Y - 1, z, Blocks.stone, 0, 2);
				}
			}
			lichen(DOWN);
			net.minecraft.item.ItemDye.applyBonemeal(new ItemStack(Items.dye, 1, 15), world(), X, Y, Z, player());
			int lichens = 0;
			for (int x = X - 1; x <= X + 1; x++) {
				for (int z = Z - 1; z <= Z + 1; z++) {
					lichens += world().getBlock(x, Y, z) == block("glow_lichen") ? 1 : 0;
				}
			}
			return "lichens " + lichens;
		});
		assertEqual("lichens 2", got, "after one bonemeal");
	}

	/**
	 * EFR's vine of two (body, head) with `MaxLength` 3: one more to grow (26.3's age 24), then no more however many random
	 * ticks; a sheared head (`TipSheared`) grows none.
	 */
	public void testVinesReadEfrLength() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (boolean sheared : new boolean[]{false, true}) {
				clear();
				world().setBlock(X, Y + 3, Z, Blocks.stone, 0, 2);
				world().setBlock(X, Y + 2, Z, block("cave_vine_plant"), 0, 2);
				world().setBlock(X, Y + 1, Z, block("cave_vine"), 0, 2);
				NBTTagCompound data = new NBTTagCompound();
				data.setInteger("MaxLength", 3);
				data.setBoolean("TipSheared", sheared);
				loadTileEntity(X, Y + 1, Z, McFly.DOMAIN + ".cave_vines", data);
				Random random = new Random(3);
				for (int i = 0; i < 400; i++) {
					int y = Y + 1;
					while (world().getBlock(X, y - 1, Z) == block("cave_vine") || world().getBlock(X, y - 1, Z) == block("cave_vine_plant")) {
						y--;
					}
					randomTick(X, y, Z, random);
				}
				int length = 0;
				for (int y = Y + 2; world().getBlock(X, y, Z) == block("cave_vine") || world().getBlock(X, y, Z) == block("cave_vine_plant"); y--) {
					length++;
				}
				out.append(sheared ? ", sheared " : "unsheared ").append(length);
			}
			return out.toString();
		});
		assertEqual("unsheared 3, sheared 2", got, "the vines' lengths after 400 random ticks at their heads");
	}

	/** 26.3's: instabreak, light 14 with berries (none without), its own sound, fire 15 and 60, no collision, climbable. */
	public void testVinesProperties() {
		String got = onServer(() -> {
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
			world().setBlock(X, Y, Z, block("cave_vine"), 1, 2);
			Block vine = block("cave_vine");
			String lit = vine.getLightValue(world(), X, Y, Z) + "";
			world().setBlockMetadataWithNotify(X, Y, Z, 0, 2);
			return vine.getBlockHardness(world(), X, Y, Z) + " " + lit + " " + vine.getLightValue(world(), X, Y, Z) + " " + vine.stepSound.getBreakSound() + " "
				+ vine.getFireSpreadSpeed(world(), X, Y, Z, net.minecraftforge.common.util.ForgeDirection.UP) + " "
				+ vine.getFlammability(world(), X, Y, Z, net.minecraftforge.common.util.ForgeDirection.UP) + " "
				+ (vine.getCollisionBoundingBoxFromPool(world(), X, Y, Z) == null) + " " + vine.isLadder(world(), X, Y, Z, player());
		});
		assertEqual("0.0 14 0 " + McFly.MC_ASSETS + ":block.cave_vines.break 15 60 true true", got,
			"hardness, light with and without berries, sound, fire, no collision, climbable");
	}

	/** A vine cut above a head with berries: the head, unheld, is destroyed with its drops (26.3's), the berries fall. */
	public void testCutVineDropsItsBerries() {
		onServer(() -> {
			world().setBlock(X, Y + 2, Z, Blocks.stone, 0, 2);
			world().setBlock(X, Y + 1, Z, block("cave_vine_plant"), 0, 2);
			world().setBlock(X, Y, Z, block("cave_vine"), 1, 2);
			world().setBlock(X, Y + 1, Z, Blocks.air, 0, 3);
		});
		serverTicks(5);
		String got = onServer(() -> (world().isAirBlock(X, Y, Z) ? "gone" : "still there") + ", berries " + dropped(berries()));
		assertEqual("gone, berries 1", got, "the head below a cut vine");
	}

	/** Berries picked by hand: one, popped out at the block (26.3's `popResource`), the vine without them after. */
	public void testPickingBerries() {
		String got = onServer(() -> {
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
			world().setBlock(X, Y, Z, block("cave_vine"), 1, 2);
			block("cave_vine").onBlockActivated(world(), X, Y, Z, player(), 0, 0.5F, 0.5F, 0.5F);
			List<EntityItem> drops = items();
			String where = drops.size() == 1 && Math.abs(drops.get(0).posX - (X + 0.5)) <= 0.26 && Math.abs(drops.get(0).posZ - (Z + 0.5)) <= 0.26
				? "at the block" : "elsewhere " + (drops.isEmpty() ? "" : drops.get(0).posX + " " + drops.get(0).posZ);
			return "berries " + dropped(berries()) + " " + where + ", meta " + world().getBlockMetadata(X, Y, Z);
		});
		assertEqual("berries 1 at the block, meta 0", got, "picking a vine's berries");
	}

	/**
	 * A lichen's outline is its faces' union, as 26.3 draws a shape: one face 12 edges; down and north (an L) 22, not the
	 * 24 of two boxes; all six (a shell round a hollow) 48, the outer cube's 12 cut in three by the faces, the hollow's 12.
	 */
	public void testOutlineIsTheUnion() throws Exception {
		Class<?> outline = Class.forName("io.github.asgerjon.mcfly.model.client.ShapeOutline");
		Class<?> multiface = Class.forName("io.github.asgerjon.mcfly.base.McFlyMultiface");
		java.lang.reflect.Method edges = outline.getMethod("edges", List.class), boxes = multiface.getMethod("faceBoxes", int.class);
		String got = ((List<?>) edges.invoke(null, boxes.invoke(null, DOWN))).size() + " " + ((List<?>) edges.invoke(null, boxes.invoke(null, DOWN | NORTH))).size()
			+ " " + ((List<?>) edges.invoke(null, boxes.invoke(null, 63))).size();
		assertEqual("12 22 48", got, "edges of one face, of down and north, of all six");
	}

	/** Glow berries: 26.3's food 2 with 0.1; compost 0.3 (EFR's none), glow lichen 0.5. */
	public void testGlowBerriesAndCompost() {
		ItemFood food = (ItemFood) berries();
		ItemStack stack = new ItemStack(food);
		Integer berries = ganymedes01.etfuturum.api.CompostingRegistry.getComposts().get(stack);
		Integer lichen = ganymedes01.etfuturum.api.CompostingRegistry.getComposts().get(new ItemStack(block("glow_lichen")));
		assertEqual("2 0.1 compost 30 50", food.func_150905_g(stack) + " " + food.func_150906_h(stack) + " compost " + berries + " " + lichen,
			"food, saturation modifier, compost chances");
	}
}
