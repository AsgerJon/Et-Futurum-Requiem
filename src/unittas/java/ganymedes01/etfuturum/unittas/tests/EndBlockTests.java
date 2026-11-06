package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.combat.ICombatPlayer;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The End's blocks as our own (SPEC-modern-movement.md §84): purpur, end stone bricks, end rods, chorus, the gateway
 * block, as 26.3 has them, kept in EFR's stored form. Each test on its own spot (x 300 on, z 300 on), cleared after.
 */
@TasSuite("endblocks")
public class EndBlockTests extends TasTestCase {

	private static final int GROUND = 4;
	private static final String[] OUR_BLOCKS = {"purpur_block", "purpur_pillar", "purpur_stairs", "purpur_slab", "double_purpur_slab", "end_bricks",
		"end_brick_stairs", "end_brick_slab", "double_end_brick_slab", "end_brick_wall", "end_rod", "chorus_plant", "chorus_flower", "end_gateway"};
	private static final String[] EFR_CLASSES = {"ganymedes01.etfuturum.blocks.BlockPurpur", "ganymedes01.etfuturum.blocks.BlockPurpurPillar",
		"ganymedes01.etfuturum.blocks.BlockPurpurSlab", "ganymedes01.etfuturum.blocks.BlockEndBricks", "ganymedes01.etfuturum.blocks.BlockEndRod",
		"ganymedes01.etfuturum.blocks.BlockChorusPlant", "ganymedes01.etfuturum.blocks.BlockChorusFlower", "ganymedes01.etfuturum.blocks.BlockEndGateway",
		"ganymedes01.etfuturum.tileentities.TileEntityGateway", "ganymedes01.etfuturum.items.ItemChorusFruit",
		"ganymedes01.etfuturum.client.renderer.block.BlockChorusPlantRenderer", "ganymedes01.etfuturum.client.renderer.block.BlockChorusFlowerRenderer",
		"ganymedes01.etfuturum.client.renderer.block.BlockEndRodRenderer", "ganymedes01.etfuturum.client.renderer.tileentity.TileEntityGatewayRenderer"};

	private final List<int[]> used = new ArrayList<>();

	private int[] spot(int n) {
		int[] at = {300 + n % 6 * 12, 300 + n / 6 * 12};
		used.add(at);
		return at;
	}

	@Override
	protected void setUp() {
		if (block("purpur_block") == null) {
			skipTest("the End's blocks are off");
		}
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			for (int[] at : used) {
				for (int x = at[0] - 5; x <= at[0] + 5; x++) {
					for (int z = at[1] - 5; z <= at[1] + 5; z++) {
						for (int y = GROUND + 9; y >= GROUND; y--) {
							world().setBlockToAir(x, y, z);
						}
						world().setBlock(x, GROUND - 1, z, Blocks.grass);
					}
				}
				for (Object entity : world().getEntitiesWithinAABB(net.minecraft.entity.Entity.class,
					AxisAlignedBB.getBoundingBox(at[0] - 6, GROUND - 1, at[1] - 6, at[0] + 6, GROUND + 10, at[1] + 6))) {
					if (!(entity instanceof EntityPlayer)) {
						((net.minecraft.entity.Entity) entity).setDead();
					}
				}
			}
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.clearInventory(null, -1);
		});
		used.clear();
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject("etfuturum:" + name);
		return block instanceof Block && block != Blocks.air ? (Block) block : null;
	}

	private static Block need(String name) {
		Block block = block(name);
		assertTrue(block != null, "no etfuturum:" + name);
		return block;
	}

	private static Item item(String name) {
		Object item = Item.itemRegistry.getObject("etfuturum:" + name);
		assertTrue(item != null, "no item etfuturum:" + name);
		return (Item) item;
	}

	private void set(int x, int y, int z, Block block, int meta) {
		onServer(() -> world().setBlock(x, y, z, block, meta, 3));
	}

	/** Placed as a player places it: the item used on the block at (x, y, z), on that side. */
	private void use(ItemStack stack, int x, int y, int z, int side) {
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			stack.copy().tryPlaceItemIntoWorld(player(), world(), x, y, z, side, 0.5F, 0.5F, 0.5F);
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	private static List<String> dropsNear(int x, int y, int z) {
		List<String> found = new ArrayList<>();
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(x - 2, y - 2, z - 2, x + 3, y + 3, z + 3))) {
			ItemStack stack = ((EntityItem) entity).getEntityItem();
			found.add(Item.itemRegistry.getNameForObject(stack.getItem()) + " x" + stack.stackSize);
		}
		java.util.Collections.sort(found);
		return found;
	}

	private static String box(AxisAlignedBB box, int x, int y, int z) {
		if (box == null) {
			return "none";
		}
		return String.format("%.3f %.3f %.3f to %.3f %.3f %.3f", box.minX - x, box.minY - y, box.minZ - z, box.maxX - x, box.maxY - y, box.maxZ - z);
	}

	/** The block's collision at (x, y, z), its boxes joined (1.7.10's own list, which sets the block's bounds first). */
	private static String collision(Block block, int x, int y, int z) {
		List<AxisAlignedBB> boxes = new ArrayList<>();
		block.addCollisionBoxesToList(world(), x, y, z, AxisAlignedBB.getBoundingBox(x - 1, y - 1, z - 1, x + 2, y + 2, z + 2), boxes, null);
		if (boxes.isEmpty()) {
			return "none";
		}
		AxisAlignedBB all = boxes.get(0).copy();
		for (AxisAlignedBB box : boxes) {
			all = all.func_111270_a(box);
		}
		return box(all, x, y, z);
	}

	/** The block's outline at (x, y, z), its bounds set for the place first (as 1.7.10's renderer and ray trace do). */
	private static String outline(Block block, int x, int y, int z) {
		block.setBlockBoundsBasedOnState(world(), x, y, z);
		return box(block.getSelectedBoundingBoxFromPool(world(), x, y, z), x, y, z);
	}

	/** The block's collision boxes at (x, y, z) hold the point (in the block's own 0 to 1 space). */
	private static boolean solidAt(Block block, int x, int y, int z, double px, double py, double pz) {
		List<AxisAlignedBB> boxes = new ArrayList<>();
		block.addCollisionBoxesToList(world(), x, y, z, AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1), boxes, null);
		for (AxisAlignedBB box : boxes) {
			if (box.minX - x < px && px < box.maxX - x && box.minY - y < py && py < box.maxY - y && box.minZ - z < pz && pz < box.maxZ - z) {
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------- ours, in EFR's stored form

	/** Every one of them ours under the old ids, in McFly's tab, the tile entity too; none of EFR's classes left. */
	@SuppressWarnings("unchecked")
	public void testBlocksAreOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		java.lang.reflect.Field names = cpw.mods.fml.relauncher.ReflectionHelper.findField(TileEntity.class, "nameToClassMap", "field_145855_i");
		Object tile = ((java.util.Map<String, Class<?>>) names.get(null)).get("etfuturum.end_gateway");
		List<Object> things = new ArrayList<>();
		for (String name : OUR_BLOCKS) {
			things.add(block(name));
		}
		things.add(Item.itemRegistry.getObject("etfuturum:chorus_fruit"));
		things.add(Item.itemRegistry.getObject("etfuturum:chorus_fruit_popped"));
		things.add(tile);
		for (Object thing : things) {
			String type = thing == null ? "nothing" : thing instanceof Class ? ((Class<?>) thing).getName() : thing.getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(type);
			}
		}
		Object tab = Class.forName("io.github.asgerjon.mcfly.registry.McFlyTab").getField("TAB").get(null);
		for (String name : new String[]{"purpur_block", "purpur_pillar", "end_bricks", "end_brick_wall", "end_rod", "chorus_flower"}) {
			if (onClient(() -> need(name).getCreativeTabToDisplayOn()) != tab) {
				wrong.add(name + " not in McFly's tab");
			}
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes(name) != null) {
				wrong.add(name.substring(name.lastIndexOf('.') + 1) + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "the End's blocks' classes");
	}

	/**
	 * EFR's metas read as before: the pillar's axis (4 east-west), the rod's facing (2 north), the slab's top half (8),
	 * the flower's age (5 dead: it doesn't grow).
	 */
	public void testStoredFormKept() throws Exception {
		int[] at = spot(0);
		int x = at[0];
		int z = at[1];
		set(x, GROUND, z, need("end_rod"), 2);
		set(x + 2, GROUND, z, need("purpur_slab"), 8);
		set(x + 4, GROUND - 1, z, Blocks.end_stone, 0);
		set(x + 4, GROUND, z, need("chorus_flower"), 5);
		set(x + 6, GROUND, z, need("purpur_pillar"), 4);
		String found = onServer(() -> outline(need("end_rod"), x, GROUND, z) + " / " + collision(need("purpur_slab"), x + 2, GROUND, z));
		assertEqual("0.375 0.375 0.000 to 0.625 0.625 1.000 / 0.000 0.500 0.000 to 1.000 1.000 1.000", found, "the north rod, the top slab");
		randomTick(x + 4, GROUND, z, new FixedRandom(0));
		assertEqual("etfuturum:chorus_flower 5, air above", onServer(() -> name(x + 4, GROUND, z) + " " + world().getBlockMetadata(x + 4, GROUND, z) + ", "
			+ (world().isAirBlock(x + 4, GROUND + 1, z) ? "air" : "not air") + " above"), "a dead flower");
		boolean sideIsTop = onClient(() -> need("purpur_pillar").getIcon(5, 4) == need("purpur_pillar").getIcon(1, 0));
		assertTrue(sideIsTop, "an east-west pillar (meta 4) shows its top on its east side");
	}

	// ---------------------------------------------------------------- as 26.3

	/** 26.3's hardness and resistance (1.7.10's explosion resistance is 26.3's number), light, and no drops by hand. */
	public void testStrengthAs26_3() {
		String[][] expected = {{"purpur_block", "1.5 6.0"}, {"purpur_pillar", "1.5 6.0"}, {"purpur_stairs", "1.5 6.0"}, {"purpur_slab", "2.0 6.0"},
			{"end_bricks", "3.0 9.0"}, {"end_brick_stairs", "3.0 9.0"}, {"end_brick_slab", "3.0 9.0"}, {"end_brick_wall", "3.0 9.0"},
			{"end_rod", "0.0 0.0"}, {"chorus_plant", "0.4 0.4"}, {"chorus_flower", "0.4 0.4"}, {"end_gateway", "-1.0 3600000.0"}};
		List<String> wrong = new ArrayList<>();
		for (String[] one : expected) {
			String got = onServer(() -> {
				Block block = need(one[0]);
				return block.getBlockHardness(world(), 0, 0, 0) + " " + block.getExplosionResistance(null);
			});
			if (!got.equals(one[1])) {
				wrong.add(one[0] + " " + got + " (26.3: " + one[1] + ")");
			}
		}
		assertEqual("[]", wrong.toString(), "hardness and resistance");
		assertEqual("14 15", need("end_rod").getLightValue() + " " + need("end_gateway").getLightValue(), "the rod's and the gateway's light");
	}

	/** 26.3's requiresCorrectToolForDrops: purpur and end stone bricks drop nothing by hand, themselves with a pickaxe. */
	public void testDropsNeedAPickaxe() {
		int[] at = spot(1);
		int x = at[0];
		int z = at[1];
		// four blocks 8 apart, so each one's drops are told apart
		set(x - 4, GROUND, z - 4, need("purpur_block"), 0);
		set(x + 4, GROUND, z - 4, need("end_bricks"), 0);
		set(x - 4, GROUND, z + 4, need("purpur_block"), 0);
		set(x + 4, GROUND, z + 4, need("end_bricks"), 0);
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			player().theItemInWorldManager.tryHarvestBlock(x - 4, GROUND, z - 4);
			player().theItemInWorldManager.tryHarvestBlock(x + 4, GROUND, z - 4);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.wooden_pickaxe));
			player().theItemInWorldManager.tryHarvestBlock(x - 4, GROUND, z + 4);
			player().theItemInWorldManager.tryHarvestBlock(x + 4, GROUND, z + 4);
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
		serverTicks(2);
		String drops = onServer(() -> "hand " + dropsNear(x - 4, GROUND, z - 4) + dropsNear(x + 4, GROUND, z - 4) + ", pickaxe "
			+ dropsNear(x - 4, GROUND, z + 4) + dropsNear(x + 4, GROUND, z + 4));
		assertEqual("hand [][], pickaxe [etfuturum:purpur_block x1][etfuturum:end_bricks x1]", drops, "the drops");
	}

	/** The pillar along the clicked face's axis: top 0, an east face 4 (east-west), a north face 8 (north-south). */
	public void testPillarAxis() {
		int[] at = spot(2);
		int x = at[0];
		int z = at[1];
		set(x, GROUND, z, Blocks.stone, 0);
		ItemStack pillar = new ItemStack(need("purpur_pillar"));
		use(pillar, x, GROUND, z, 1);
		use(pillar, x, GROUND, z, 5);
		use(pillar, x, GROUND, z, 2);
		String metas = onServer(() -> world().getBlockMetadata(x, GROUND + 1, z) + " " + world().getBlockMetadata(x + 1, GROUND, z) + " "
			+ world().getBlockMetadata(x, GROUND, z - 1));
		assertEqual("0 4 8", metas, "the pillar's metas: placed on top, on the east face, on the north face");
	}

	/**
	 * 26.3's end rod: placed facing the clicked face, but the other way on an end rod facing that face; its shape 4 by 4
	 * along its axis, its collision the same; wood sounds.
	 */
	public void testEndRodPlacementAndShape() {
		int[] at = spot(3);
		int x = at[0];
		int z = at[1];
		set(x, GROUND, z, Blocks.stone, 0);
		ItemStack rod = new ItemStack(need("end_rod"));
		use(rod, x, GROUND, z, 1);
		use(rod, x, GROUND + 1, z, 1);
		use(rod, x, GROUND, z, 5);
		String found = onServer(() -> world().getBlockMetadata(x, GROUND + 1, z) + " " + world().getBlockMetadata(x, GROUND + 2, z) + " "
			+ world().getBlockMetadata(x + 1, GROUND, z));
		assertEqual("1 0 5", found, "up on the stone, down on that rod's top, east on the stone's east face");
		String shapes = onServer(() -> collision(need("end_rod"), x, GROUND + 1, z) + " / " + outline(need("end_rod"), x + 1, GROUND, z));
		assertEqual("0.375 0.000 0.375 to 0.625 1.000 0.625 / 0.000 0.375 0.375 to 1.000 0.625 0.625", shapes, "the up rod's collision, the east rod's outline");
		assertEqual("step.wood", need("end_rod").stepSound.getStepResourcePath(), "its sounds");
	}

	/** Its particle: `minecraft:end_rod` from McFly's particles, made by an end rod on the client in time. */
	public void testEndRodParticles() throws Exception {
		int[] at = spot(4);
		int x = at[0];
		int z = at[1];
		Class<?> particles = Class.forName("io.github.asgerjon.mcfly.particle.McFlyParticles");
		assertEqual("minecraft:end_rod", particles.getField("END_ROD").get(null), "the particle's name");
		set(x, GROUND, z, need("end_rod"), 1);
		stand(x + 0.5, GROUND, z + 2.5);
		Block rod = need("end_rod");
		int made = onClient(() -> {
			Random random = new Random(1);
			int before = particleCount("EndRodParticle");
			for (int i = 0; i < 100; i++) {
				rod.randomDisplayTick(mc().theWorld, x, GROUND, z, random);
			}
			return particleCount("EndRodParticle") - before;
		});
		assertTrue(made > 5 && made < 40, "end rod particles from 100 display ticks (26.3: 1 in 5): " + made);
	}

	private void stand(double x, double y, double z) {
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(x, y, z, 0.0F, 0.0F));
		waitUntilOnClient("arriving", () -> Math.abs(mc().thePlayer.posX - x) < 0.01D && Math.abs(mc().thePlayer.posZ - z) < 0.01D, 60);
		ticks(2);
	}

	@SuppressWarnings("rawtypes")
	private static int particleCount(String name) {
		try {
			java.lang.reflect.Field field;
			try {
				field = net.minecraft.client.particle.EffectRenderer.class.getDeclaredField("fxLayers");
			} catch (NoSuchFieldException e) {
				field = net.minecraft.client.particle.EffectRenderer.class.getDeclaredField("field_78876_b");
			}
			field.setAccessible(true);
			int count = 0;
			for (List layer : (List[]) field.get(mc().effectRenderer)) {
				for (Object particle : layer) {
					if (particle.getClass().getSimpleName().contains(name)) {
						count++;
					}
				}
			}
			return count;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 26.3's chorus plant: arms toward plants and flowers (down also toward end stone), its 10-pixel core; standing on end
	 * stone it stays, the stone gone it breaks a tick later with 0 or 1 chorus fruit; it can't go on dirt.
	 */
	public void testChorusPlant() {
		int[] at = spot(5);
		int x = at[0];
		int z = at[1];
		Block plant = need("chorus_plant");
		set(x, GROUND - 1, z, Blocks.end_stone, 0);
		set(x, GROUND, z, plant, 0);
		set(x, GROUND + 1, z, plant, 0);
		set(x, GROUND + 2, z, need("chorus_flower"), 0);
		String arms = onServer(() -> solidAt(plant, x, GROUND + 1, z, 0.5, 0.5, 0.5) + " " + solidAt(plant, x, GROUND + 1, z, 0.5, 0.05, 0.5) + " "
			+ solidAt(plant, x, GROUND + 1, z, 0.5, 0.95, 0.5) + " " + solidAt(plant, x, GROUND + 1, z, 0.5, 0.5, 0.05) + " "
			+ solidAt(plant, x, GROUND + 1, z, 0.1, 0.5, 0.1) + " " + solidAt(plant, x, GROUND, z, 0.5, 0.05, 0.5));
		assertEqual("true true true false false true", arms, "the middle plant: its core, its arms down and up, none north, no corner; the bottom's arm to the end stone");
		set(x - 3, GROUND - 1, z, Blocks.end_stone, 0);
		assertEqual("false true", onServer(() -> plant.canPlaceBlockAt(world(), x + 3, GROUND, z) + " " + plant.canPlaceBlockAt(world(), x - 3, GROUND, z)),
			"placeable on the grass, on the end stone");
		// in one server step: right after, the plant still stands (it breaks on its scheduled tick, a tick later)
		String right = onServer(() -> {
			world().setBlock(x, GROUND - 1, z, Blocks.dirt);
			return Block.blockRegistry.getNameForObject(world().getBlock(x, GROUND, z));
		});
		serverTicks(3);
		String after = onServer(() -> Block.blockRegistry.getNameForObject(world().getBlock(x, GROUND, z)));
		assertEqual("etfuturum:chorus_plant then minecraft:air", right + " then " + after, "the bottom plant with its end stone gone");
		List<String> drops = onServer(() -> dropsNear(x, GROUND, z));
		for (String drop : drops) {
			assertTrue(drop.equals("etfuturum:chorus_fruit x1"), "a plant dropped " + drops);
		}
	}

	/** Chorus flowers as 26.3's random tick grows them; the random answers 0 to every question here. */
	public void testChorusFlowerGrowth() throws Exception {
		int[] at = spot(6);
		int x = at[0];
		int z = at[1];
		Block plant = need("chorus_plant");
		Block flower = need("chorus_flower");
		// on end stone with room: up, the flower one higher at its age, a plant where it was
		set(x, GROUND - 1, z, Blocks.end_stone, 0);
		set(x, GROUND, z, flower, 0);
		randomTick(x, GROUND, z, new FixedRandom(0));
		assertEqual("etfuturum:chorus_plant, etfuturum:chorus_flower 0", onServer(() -> name(x, GROUND, z) + ", " + name(x, GROUND + 1, z) + " "
			+ world().getBlockMetadata(x, GROUND + 1, z)), "grown up from end stone");
		// on a column of 4 plants on end stone: 4 <= nextInt(5) is false, so it branches: 1 + 1 try, north (nextInt(4) 0)
		int bx = x + 4;
		set(bx, GROUND - 1, z, Blocks.end_stone, 0);
		for (int y = GROUND; y < GROUND + 4; y++) {
			set(bx, y, z, plant, 0);
		}
		set(bx, GROUND + 4, z, flower, 1);
		randomTick(bx, GROUND + 4, z, new FixedRandom(0));
		assertEqual("etfuturum:chorus_plant, north etfuturum:chorus_flower 2", onServer(() -> name(bx, GROUND + 4, z) + ", north "
			+ name(bx, GROUND + 4, z - 1) + " " + world().getBlockMetadata(bx, GROUND + 4, z - 1)), "a branch north at the next age");
		// age 4 that can't grow up (a block 2 above): it dies (5)
		int dx = x - 4;
		set(dx, GROUND - 1, z, Blocks.end_stone, 0);
		set(dx, GROUND, z, flower, 4);
		set(dx, GROUND + 2, z, Blocks.stone, 0);
		randomTick(dx, GROUND, z, new FixedRandom(0));
		assertEqual("5", onServer(() -> String.valueOf(world().getBlockMetadata(dx, GROUND, z))), "an age 4 flower with no room up: dead");
	}

	/**
	 * The world's random tick reaches the flower's own (`MixinWorldServerRandomTick`): with random ticks sped up, a flower
	 * on end stone grows (1.7.10's `updateTick` alone, the scheduled tick's, would only check its footing).
	 */
	public void testFlowerGrowsInTheWorld() {
		int[] at = spot(10);
		int x = at[0];
		int z = at[1];
		set(x, GROUND - 1, z, Blocks.end_stone, 0);
		set(x, GROUND, z, need("chorus_flower"), 0);
		stand(x + 2.5, GROUND, z + 2.5);
		String before = onServer(() -> world().getGameRules().getGameRuleStringValue("randomTickSpeed"));
		try {
			onServer(() -> world().getGameRules().setOrCreateGameRule("randomTickSpeed", "4096"));
			waitUntil("the flower growing", () -> world().getBlock(x, GROUND, z) == need("chorus_plant"), 100);
		} finally {
			onServer(() -> world().getGameRules().setOrCreateGameRule("randomTickSpeed", before.isEmpty() ? "3" : before));
		}
	}

	/**
	 * 26.3's chorus flower survival and drops: unsupported it breaks a tick later dropping nothing (its loot needs an
	 * entity); broken by a player it drops itself; an arrow breaks it when mobs may change blocks.
	 */
	public void testChorusFlowerBreaking() {
		int[] at = spot(7);
		int x = at[0];
		int z = at[1];
		Block flower = need("chorus_flower");
		set(x, GROUND - 1, z, Blocks.end_stone, 0);
		set(x, GROUND, z, flower, 0);
		onServer(() -> world().setBlock(x, GROUND - 1, z, Blocks.dirt));
		serverTicks(3);
		assertEqual("minecraft:air []", onServer(() -> name(x, GROUND, z) + " " + dropsNear(x, GROUND, z)), "unsupported: gone, no drop");
		set(x + 3, GROUND - 1, z, Blocks.end_stone, 0);
		set(x + 3, GROUND, z, flower, 0);
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().theItemInWorldManager.tryHarvestBlock(x + 3, GROUND, z);
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
		serverTicks(2);
		assertEqual("[etfuturum:chorus_flower x1]", onServer(() -> dropsNear(x + 3, GROUND, z).toString()), "broken by the player");
		// arrows: with mob griefing off (the test world's) one stays in it; on, one breaks it (26.3's mayBreak)
		for (int[] one : new int[][]{{x - 3, 0}, {x - 5, 1}}) {
			set(one[0], GROUND - 1, z, Blocks.end_stone, 0);
			set(one[0], GROUND, z, flower, 0);
		}
		// near, so the arrows' chunks tick
		stand(x - 3.5, GROUND, z + 3.5);
		for (int[] one : new int[][]{{x - 3, 0}, {x - 5, 1}}) {
			onServer(() -> {
				world().getGameRules().setOrCreateGameRule("mobGriefing", one[1] == 1 ? "true" : "false");
				EntityArrow arrow = new EntityArrow(world(), one[0] + 0.5, GROUND + 0.5, z - 2.5);
				arrow.motionZ = 1.5;
				world().spawnEntityInWorld(arrow);
			});
			serverTicks(10);
		}
		onServer(() -> world().getGameRules().setOrCreateGameRule("mobGriefing", "false"));
		assertEqual("etfuturum:chorus_flower minecraft:air", onServer(() -> name(x - 3, GROUND, z) + " " + name(x - 5, GROUND, z)),
			"an arrow with mob griefing off, then on");
	}

	/**
	 * Chorus fruit as 26.3's: eaten even when full, it teleports the player within 8 each way over a block, no fall
	 * damage after, `item.chorus_fruit.teleport`; then 20 ticks before it can be eaten again.
	 */
	public void testChorusFruit() {
		int[] at = spot(8);
		int x = at[0];
		int z = at[1];
		Item fruit = item("chorus_fruit");
		stand(x + 0.5, GROUND, z + 0.5);
		boolean eating = onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().getFoodStats().addStats(20, 20.0F);
			ItemStack stack = new ItemStack(fruit, 4);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, stack);
			stack.useItemRightClick(world(), player());
			boolean using = player().isUsingItem();
			player().clearItemInUse();
			return using;
		});
		assertTrue(eating, "not eaten when full (26.3: always edible)");
		clearSounds();
		double[] moved = onServer(() -> {
			player().fallDistance = 5.0F;
			ItemStack stack = player().getHeldItem();
			stack.getItem().onEaten(stack, world(), player());
			Block under = world().getBlock((int) Math.floor(player().posX), (int) Math.floor(player().posY) - 1, (int) Math.floor(player().posZ));
			return new double[]{player().posX - (x + 0.5), player().posZ - (z + 0.5), player().fallDistance, under.getMaterial().blocksMovement() ? 1 : 0};
		});
		assertTrue(Math.abs(moved[0]) <= 8.0 && Math.abs(moved[1]) <= 8.0 && (moved[0] != 0.0 || moved[1] != 0.0), "teleported by " + moved[0] + ", " + moved[1]);
		// 26.3 keeps the height it drew above the block (within the block over it), so the eater may drop a little after
		assertEqual("0.0 1.0", moved[2] + " " + moved[3], "no fall distance after, over a block that blocks movement");
		waitForSound(McFly.MC_ASSETS + ":item.chorus_fruit.teleport", 20);
		String cooling = onServer(() -> {
			boolean right = ((ICombatPlayer) player()).etfu$getCooldowns().isCoolingDown(fruit);
			ItemStack stack = player().getHeldItem();
			stack.useItemRightClick(world(), player());
			boolean eatingAgain = player().isUsingItem();
			player().clearItemInUse();
			return right + " " + eatingAgain;
		});
		assertEqual("true false", cooling, "cooling down after, and not eaten again in it");
		// the cooldown counts the player's own updates, which come with its movement packets: a few server ticks spare
		serverTicks(30);
		assertEqual(false, onServer(() -> ((ICombatPlayer) player()).etfu$getCooldowns().isCoolingDown(fruit)), "still cooling after 30 ticks (26.3: 20)");
	}

	/** 26.3's recipes and the smelting. */
	public void testRecipes() {
		ItemStack popped = new ItemStack(item("chorus_fruit_popped"));
		ItemStack purpur = new ItemStack(need("purpur_block"));
		ItemStack purpurSlab = new ItemStack(need("purpur_slab"));
		ItemStack endStone = new ItemStack(Blocks.end_stone);
		ItemStack bricks = new ItemStack(need("end_bricks"));
		ItemStack blaze = new ItemStack(Items.blaze_rod);
		List<String> found = new ArrayList<>();
		found.add(craft(popped, popped, null, popped, popped, null, null, null, null));
		found.add(craft(purpurSlab, null, null, purpurSlab, null, null, null, null, null));
		found.add(craft(purpur, purpur, purpur, null, null, null, null, null, null));
		found.add(craft(purpur, null, null, purpur, purpur, null, purpur, purpur, purpur));
		found.add(craft(endStone, endStone, null, endStone, endStone, null, null, null, null));
		found.add(craft(bricks, bricks, bricks, null, null, null, null, null, null));
		found.add(craft(bricks, null, null, bricks, bricks, null, bricks, bricks, bricks));
		found.add(craft(bricks, bricks, bricks, bricks, bricks, bricks, null, null, null));
		found.add(craft(blaze, null, null, popped, null, null, null, null, null));
		assertEqual("[purpur_block x4, purpur_pillar x1, purpur_slab x6, purpur_stairs x4, end_bricks x4, end_brick_slab x6, end_brick_stairs x4, "
			+ "end_brick_wall x6, end_rod x4]", found.toString(), "the recipes");
		String smelted = onServer(() -> {
			ItemStack fruit = new ItemStack(item("chorus_fruit"));
			ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(fruit);
			return (result == null ? "nothing" : Item.itemRegistry.getNameForObject(result.getItem())) + " " + FurnaceRecipes.smelting().func_151398_b(result);
		});
		assertEqual("etfuturum:chorus_fruit_popped 0.1", smelted, "chorus fruit smelted");
	}

	/** What the 3 by 3 grid makes, as "name xN" (the name without the domain), or "nothing". */
	private String craft(ItemStack... grid) {
		return onServer(() -> {
			InventoryCrafting crafting = new InventoryCrafting(new Container() {
				@Override
				public boolean canInteractWith(EntityPlayer player) {
					return true;
				}
			}, 3, 3);
			for (int slot = 0; slot < 9; slot++) {
				crafting.setInventorySlotContents(slot, grid[slot] == null ? null : grid[slot].copy());
			}
			ItemStack result = CraftingManager.getInstance().findMatchingRecipe(crafting, world());
			if (result == null) {
				return "nothing";
			}
			String name = Item.itemRegistry.getNameForObject(result.getItem());
			return name.substring(name.indexOf(':') + 1) + " x" + result.stackSize;
		});
	}

	/**
	 * 26.3's wall sides and post (§84.1): alone, a straight run open above, the same under stone, under a torch, at a
	 * corner, beside stone, bars, leaves and a pane, beside fence gates, under a wall's post; its collision 1.5 high.
	 */
	public void testWallSidesAndPost() throws Exception {
		int[] at = spot(9);
		int x = at[0];
		int z = at[1];
		Block wall = need("end_brick_wall");
		List<String> found = new ArrayList<>();
		// alone
		set(x, GROUND, z, wall, 0);
		found.add(wallShape(x, GROUND, z));
		// a run east-west, open above; then stone above; then a torch above
		int rx = x + 4;
		for (int dx = -1; dx <= 1; dx++) {
			set(rx + dx, GROUND, z, wall, 0);
		}
		found.add(wallShape(rx, GROUND, z));
		set(rx, GROUND + 1, z, Blocks.stone, 0);
		found.add(wallShape(rx, GROUND, z));
		set(rx, GROUND + 1, z, Blocks.torch, 5);
		found.add(wallShape(rx, GROUND, z));
		// under a lone wall (its post up)
		set(rx, GROUND + 1, z, wall, 0);
		found.add(wallShape(rx, GROUND, z));
		// a corner: north and east
		int cz = z + 4;
		set(x, GROUND, cz, wall, 0);
		set(x, GROUND, cz - 1, wall, 0);
		set(x + 1, GROUND, cz, wall, 0);
		found.add(wallShape(x, GROUND, cz));
		// stone east, iron bars west, leaves north, a glass pane south
		int sx = x + 4;
		int sz = z + 4;
		set(sx, GROUND, sz, wall, 0);
		set(sx + 1, GROUND, sz, Blocks.stone, 0);
		set(sx - 1, GROUND, sz, Blocks.iron_bars, 0);
		set(sx, GROUND, sz - 1, Blocks.leaves, 0);
		set(sx, GROUND, sz + 1, Blocks.glass_pane, 0);
		found.add(wallShape(sx, GROUND, sz));
		// a gate facing north to the east (across: it connects), one facing east to the west (along: it doesn't)
		int gx = x - 4;
		set(gx, GROUND, z, wall, 0);
		set(gx + 1, GROUND, z, Blocks.fence_gate, 2);
		set(gx - 1, GROUND, z, Blocks.fence_gate, 3);
		found.add(wallShape(gx, GROUND, z));
		assertEqual("[post n=none e=none s=none w=none, - n=none e=low s=none w=low, - n=none e=tall s=none w=tall, post n=none e=low s=none w=low, "
				+ "post n=none e=low s=none w=low, post n=low e=low s=none w=none, post n=none e=low s=low w=low, post n=none e=low s=none w=none]",
			found.toString(), "the walls' shapes: alone, a run, under stone, under a torch, under a wall, a corner, beside things, beside gates");
		assertEqual("0.250 0.000 0.250 to 1.000 1.500 0.750", onServer(() -> collision(wall, rx - 1, GROUND, z)),
			"the run's west end's collision: 26.3's 24 high, the post and its east side");
	}

	/** The wall's shape as 26.3's states say it, from McFly's wall (absent on EFR's). */
	private String wallShape(int x, int y, int z) throws Exception {
		Block wall = need("end_brick_wall");
		Method shape = wall.getClass().getMethod("shapeAt", net.minecraft.world.IBlockAccess.class, int.class, int.class, int.class);
		return onServer(() -> {
			try {
				return String.valueOf(shape.invoke(null, world(), x, y, z));
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
	}

	// ---------------------------------------------------------------- helpers

	private static String name(int x, int y, int z) {
		return Block.blockRegistry.getNameForObject(world().getBlock(x, y, z));
	}

	/** The block's random tick (26.3's randomTick, McFly's split from 1.7.10's updateTick), with this random. */
	private void randomTick(int x, int y, int z, Random random) throws Exception {
		Block block = onServer(() -> world().getBlock(x, y, z));
		Method tick = block.getClass().getMethod("randomTick", World.class, int.class, int.class, int.class, Random.class);
		onServer(() -> {
			try {
				tick.invoke(block, world(), x, y, z, random);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/** A random whose every answer is the smallest (its value, kept under the bound). */
	private static final class FixedRandom extends Random {
		private final int value;

		FixedRandom(int value) {
			this.value = value;
		}

		@Override
		public int nextInt(int bound) {
			return Math.min(value, bound - 1);
		}

		@Override
		public float nextFloat() {
			return 0.0F;
		}

		@Override
		public double nextDouble() {
			return 0.0D;
		}
	}
}
