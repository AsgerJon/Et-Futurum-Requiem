package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The lush caves' plants as our own (SPEC-modern-movement.md §90.8, step G), as 26.3's: the spore blossom, the big
 * dripleaf and its stem, the small dripleaf, in McFly's new ids (EFR had none): their names by meta, properties,
 * support, the big dripleaf's tilt, bonemeal, drops, the spores and compost. By the player (x 8 to 12, z -10 to -6):
 * the tilt needs a pig that moves.
 */
@TasSuite("caves")
public class LushPlantTests extends TasTestCase {

	private static final int X = 10, Y = 8, Z = -8;
	private static final String[] BLOCKS = {"spore_blossom", "big_dripleaf", "big_dripleaf_stem", "small_dripleaf"};

	@Override
	protected void setUp() {
		onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(0.5D, 4.0D, 0.5D, 0.0F, 0.0F);
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

	/** x 8 to 12, y 6 to 16, z -10 to -6 emptied, a clay floor at y 7; its items and mobs gone. */
	private void clear() {
		for (int x = X - 2; x <= X + 2; x++) {
			for (int y = Y - 2; y <= Y + 8; y++) {
				for (int z = Z - 2; z <= Z + 2; z++) {
					world().setBlock(x, y, z, y == Y - 1 ? Blocks.clay : Blocks.air, 0, 2);
				}
			}
		}
		for (Object entity : world().getEntitiesWithinAABB(net.minecraft.entity.Entity.class, box())) {
			if (!(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
				((net.minecraft.entity.Entity) entity).setDead();
			}
		}
	}

	private static AxisAlignedBB box() {
		return AxisAlignedBB.getBoundingBox(X - 3, Y - 3, Z - 3, X + 4, Y + 10, Z + 4);
	}

	private String dropped() {
		Map<String, Integer> counts = new java.util.TreeMap<>();
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, box())) {
			EntityItem item = (EntityItem) entity;
			if (!item.isDead) {
				ItemStack stack = item.getEntityItem();
				counts.merge(Item.itemRegistry.getNameForObject(stack.getItem()).replace(McFly.DOMAIN + ":", "") + ":" + stack.getItemDamage(), stack.stackSize, Integer::sum);
				item.setDead();
			}
		}
		return counts.toString();
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	/** The 26.3 block it reads as, with its facing, tilt and half where it has them. */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		StringBuilder name = new StringBuilder(found.type().id.replace("minecraft:", ""));
		for (String property : new String[]{"facing", "tilt", "half"}) {
			if (state.containsKey(property)) {
				name.append(",").append(state.get(property));
			}
		}
		return name.toString();
	}

	private String at(int x, int y, int z) {
		Block there = world().getBlock(x, y, z);
		return there == Blocks.air ? "air" : name(there, world().getBlockMetadata(x, y, z));
	}

	/** The player uses the item on a face, facing the way given (yaw: 0 south, 90 west, 180 north, 270 east). */
	private void place(Block block, int x, int y, int z, int side, float yaw) {
		ItemStack stack = new ItemStack(block, 4);
		player().inventory.setInventorySlotContents(player().inventory.currentItem, stack);
		player().rotationYaw = yaw;
		stack.getItem().onItemUse(stack, player(), world(), x, y, z, side, 0.5F, side == 0 ? 0.0F : 1.0F, 0.5F);
	}

	// ---------------------------------------------------------------- ours, names, properties

	/**
	 * 26.3's models with face UV rotations read (McFly's model reader refused them, and drawing the small dripleaf's item
	 * crashed the client, 02:48): the hanging propagules (step E), the spore blossom, the small dripleaf's top.
	 */
	public void testModelsWithFaceRotationsRead() {
		List<String> failed = new ArrayList<>();
		for (String model : new String[]{"mangrove_propagule_hanging_0", "mangrove_propagule_hanging_1", "mangrove_propagule_hanging_2", "mangrove_propagule_hanging_3",
			"mangrove_propagule_hanging_4", "spore_blossom", "small_dripleaf_top"}) {
			try {
				if (io.github.asgerjon.mcfly.model.BlockModel.get(model).elements().isEmpty()) {
					failed.add(model + " empty");
				}
			} catch (RuntimeException e) {
				failed.add(model + " " + e.getMessage());
			}
		}
		assertEqual("[]", failed.toString(), "models with face rotations");
	}

	public void testPlantsAreOurs() {
		List<String> wrong = new ArrayList<>();
		for (String name : BLOCKS) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		assertEqual("[]", wrong.toString(), "the lush plants' classes");
	}

	/** McFly's metas by 26.3's names: the dripleaf's facing in meta & 3 (1.7.10's south, west, north, east), its tilt meta >> 2; the small one's upper half 4. */
	public void testNames() {
		String got = onServer(() -> name(block("spore_blossom"), 0) + " " + name(block("big_dripleaf"), 0) + " " + name(block("big_dripleaf"), 1 | 4)
			+ " " + name(block("big_dripleaf"), 2 | 8) + " " + name(block("big_dripleaf"), 3 | 12) + " " + name(block("big_dripleaf_stem"), 1) + " "
			+ name(block("small_dripleaf"), 0) + " " + name(block("small_dripleaf"), 3 | 4));
		assertEqual("spore_blossom big_dripleaf,south,none big_dripleaf,west,unstable big_dripleaf,north,partial big_dripleaf,east,full "
			+ "big_dripleaf_stem,west small_dripleaf,south,lower small_dripleaf,east,upper", got, "26.3's names");
	}

	/** 26.3's: the spore blossom and small dripleaf instabreak, the dripleaf and stem 0.1; fire 60 and 100; their sounds; the plants' green. */
	public void testProperties() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (String name : BLOCKS) {
				Block block = block(name);
				world().setBlock(X, Y, Z, block, 0, 2);
				result.append(result.length() == 0 ? "" : "; ").append(name).append(" ").append(block.getBlockHardness(world(), X, Y, Z)).append(" ")
					.append(block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP)).append(" ").append(block.getFlammability(world(), X, Y, Z, ForgeDirection.UP))
					.append(" ").append(block.stepSound.getBreakSound().replace(McFly.MC_ASSETS + ":", "")).append(block.getMapColor(0) == MapColor.foliageColor ? " green" : " not green");
			}
			return result.toString();
		});
		assertEqual("spore_blossom 0.0 60 100 block.spore_blossom.break green; big_dripleaf 0.1 60 100 block.big_dripleaf.break green; "
			+ "big_dripleaf_stem 0.1 60 100 block.big_dripleaf.break green; small_dripleaf 0.0 60 100 block.small_dripleaf.break green", got, "properties");
	}

	// ---------------------------------------------------------------- support

	/**
	 * The spore blossom under stone, not on the floor; the big dripleaf on clay facing the player's way, not on stone, and
	 * one placed on another makes that a stem facing as it did; the small dripleaf two tall on clay, not on dirt in the
	 * dry, on dirt under a water source.
	 */
	public void testPlacingAndSupport() {
		String got = onServer(() -> {
			world().setBlock(X, Y + 4, Z, Blocks.stone, 0, 2);
			place(block("spore_blossom"), X, Y + 4, Z, 0, 0.0F);
			place(block("spore_blossom"), X + 1, Y - 1, Z, 1, 0.0F);
			String spore = at(X, Y + 3, Z) + ", on the floor " + at(X + 1, Y, Z);
			place(block("big_dripleaf"), X, Y - 1, Z, 1, 180.0F);
			place(block("big_dripleaf"), X, Y, Z, 1, 0.0F);
			String big = at(X, Y, Z) + " under " + at(X, Y + 1, Z);
			world().setBlock(X - 1, Y - 1, Z, Blocks.stone, 0, 2);
			place(block("big_dripleaf"), X - 1, Y - 1, Z, 1, 0.0F);
			String onStone = at(X - 1, Y, Z);
			place(block("small_dripleaf"), X + 1, Y - 1, Z + 1, 1, 90.0F);
			String small = at(X + 1, Y, Z + 1) + " under " + at(X + 1, Y + 1, Z + 1);
			world().setBlock(X - 1, Y - 1, Z + 1, Blocks.dirt, 0, 2);
			place(block("small_dripleaf"), X - 1, Y - 1, Z + 1, 1, 90.0F);
			String dry = at(X - 1, Y, Z + 1);
			world().setBlock(X - 1, Y - 1, Z - 1, Blocks.dirt, 0, 2);
			for (int[] side : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				world().setBlock(X - 1 + side[0], Y, Z - 1 + side[1], Blocks.stone, 0, 2);
			}
			world().setBlock(X - 1, Y, Z - 1, Blocks.water, 0, 2);
			place(block("small_dripleaf"), X - 1, Y - 1, Z - 1, 1, 90.0F);
			String wet = at(X - 1, Y, Z - 1);
			return "spore " + spore + "; big " + big + ", on stone " + onStone + "; small " + small + ", dry dirt " + dry + ", under water " + wet;
		});
		assertEqual("spore spore_blossom, on the floor air; big big_dripleaf_stem,south under big_dripleaf,south,none, on stone air; "
			+ "small small_dripleaf,east,lower under small_dripleaf,east,upper, dry dirt air, under water small_dripleaf,east,lower", got, "placing");
	}

	// ---------------------------------------------------------------- the tilt

	/**
	 * A pig standing on a big dripleaf: unstable at once, partial 10 ticks on, full 10 more (the pig drops through), back
	 * 100 more; a redstone signal resets an unstable one at once.
	 */
	public void testTilt() {
		EntityPig pig = onServer(() -> {
			world().setBlock(X, Y, Z, block("big_dripleaf"), 0, 3);
			EntityPig spawned = new EntityPig(world());
			spawned.setLocationAndAngles(X + 0.5D, Y + 15.0D / 16.0D + 0.01D, Z + 0.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(spawned);
			return spawned;
		});
		// timed by the world's clock (a sample here is more than one server tick)
		List<String> changes = new ArrayList<>();
		String last = "";
		long start = -1;
		for (int sample = 0; sample < 160 && (changes.isEmpty() || !changes.get(changes.size() - 1).startsWith("none@")); sample++) {
			serverTicks(1);
			String[] now = onServer(() -> new String[]{name(world().getBlock(X, Y, Z), world().getBlockMetadata(X, Y, Z)), String.valueOf(world().getTotalWorldTime())});
			if (!now[0].equals(last)) {
				long time = Long.parseLong(now[1]);
				if (start < 0 && now[0].endsWith("unstable")) {
					start = time;
				}
				if (start >= 0) {
					changes.add(now[0].substring(now[0].lastIndexOf(',') + 1) + "@" + (time - start));
				}
				last = now[0];
			}
		}
		onServer(pig::setDead);
		String reset = onServer(() -> {
			world().setBlock(X, Y, Z, block("big_dripleaf"), 4, 3);
			world().setBlock(X + 1, Y, Z, Blocks.redstone_block, 0, 3);
			return name(world().getBlock(X, Y, Z), world().getBlockMetadata(X, Y, Z));
		});
		assertEqual("[unstable@0, partial@10, full@20, none@120]", changes.toString(), "the tilt's steps (ticks after it went unstable)");
		assertEqual("big_dripleaf,south,none", reset, "redstone by an unstable dripleaf");
	}

	// ---------------------------------------------------------------- bonemeal, drops

	/** Bonemeal: a big dripleaf grows one (the old leaf a stem); on its stem, the leaf at the top grows; a small dripleaf becomes a big one 2 to 5 tall. */
	public void testBonemeal() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, block("big_dripleaf"), 2, 3);
			ItemDye.applyBonemeal(new ItemStack(Items.dye, 1, 15), world(), X, Y, Z, player());
			String once = at(X, Y, Z) + " under " + at(X, Y + 1, Z);
			ItemDye.applyBonemeal(new ItemStack(Items.dye, 1, 15), world(), X, Y, Z, player());
			String stem = at(X, Y + 1, Z) + " under " + at(X, Y + 2, Z);
			place(block("small_dripleaf"), X + 1, Y - 1, Z, 1, 0.0F);
			ItemDye.applyBonemeal(new ItemStack(Items.dye, 1, 15), world(), X + 1, Y, Z, player());
			int height = 0;
			while (world().getBlock(X + 1, Y + height, Z) == block("big_dripleaf_stem")) {
				height++;
			}
			boolean leaf = world().getBlock(X + 1, Y + height, Z) == block("big_dripleaf");
			return once + "; from the stem: " + stem + "; small to big: " + (leaf && height + 1 >= 2 && height + 1 <= 5 ? "2 to 5 tall" : height + 1 + " " + leaf);
		});
		assertEqual("big_dripleaf_stem,north under big_dripleaf,north,none; from the stem: big_dripleaf_stem,north under big_dripleaf,north,none; small to big: 2 to 5 tall",
			got, "bonemeal");
	}

	/** 26.3's loot: a small dripleaf only to shears; a stem a big dripleaf, and the leaf it held another; the spore blossom itself. */
	public void testDrops() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			Object[][] cases = {{"small_dripleaf", null}, {"small_dripleaf", new ItemStack(Items.shears)}, {"big_dripleaf_stem", null}, {"spore_blossom", null}};
			for (Object[] entry : cases) {
				clear();
				world().setBlock(X, Y + 4, Z, Blocks.stone, 0, 2);
				Block block = block((String) entry[0]);
				if (block == block("small_dripleaf")) {
					place(block, X, Y - 1, Z, 1, 0.0F);
				} else if (block == block("spore_blossom")) {
					place(block, X, Y + 4, Z, 0, 0.0F);
				} else {
					world().setBlock(X, Y, Z, block, 0, 2);
					world().setBlock(X, Y + 1, Z, block("big_dripleaf"), 0, 2);
				}
				int y = block == block("spore_blossom") ? Y + 3 : Y;
				player().inventory.setInventorySlotContents(player().inventory.currentItem, (ItemStack) entry[1]);
				player().theItemInWorldManager.tryHarvestBlock(X, y, Z);
				result.append(result.length() == 0 ? "" : "; ").append(entry[0]).append(entry[1] == null ? " by hand " : " sheared ").append(dropped());
			}
			return result.toString();
		});
		assertEqual("small_dripleaf by hand {}; small_dripleaf sheared {small_dripleaf:0=1}; big_dripleaf_stem by hand {big_dripleaf:0=2}; "
			+ "spore_blossom by hand {spore_blossom:0=1}", got, "drops");
	}

	// ---------------------------------------------------------------- spores, compost

	/** Each display tick a falling spore 0.7 into the blossom's block, at a random spot (26.3's `animateTick`; seeded, the particle setting All). */
	public void testFallingSpores() {
		onServer(() -> {
			world().setBlock(X, Y + 4, Z, Blocks.stone, 0, 2);
			world().setBlock(X, Y + 3, Z, block("spore_blossom"), 0, 2);
		});
		waitUntilOnClient("the client's spore blossom", () -> mc().theWorld.getBlock(X, Y + 3, Z) == block("spore_blossom"), 60);
		String got = onClient(() -> {
			int setting = mc().gameSettings.particleSetting;
			List<EntityFX> before = particlesAt();
			try {
				mc().gameSettings.particleSetting = 0;
				Random random = new Random(7);
				for (int i = 0; i < 100; i++) {
					block("spore_blossom").randomDisplayTick(mc().theWorld, X, Y + 3, Z, random);
				}
			} finally {
				mc().gameSettings.particleSetting = setting;
			}
			List<EntityFX> made = particlesAt();
			made.removeAll(before);
			return made.size() + " falling spores";
		});
		assertEqual("100 falling spores", got, "falling spores");
	}

	/** The client's particles at 0.7 into the blossom's block (the falling spores' start). */
	@SuppressWarnings("unchecked")
	private List<EntityFX> particlesAt() {
		List<?>[] layers = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(net.minecraft.client.particle.EffectRenderer.class, mc().effectRenderer,
			"fxLayers", "field_78876_b");
		List<EntityFX> found = new ArrayList<>();
		for (List<?> layer : layers) {
			for (Object entry : layer) {
				EntityFX particle = (EntityFX) entry;
				if (particle.posX >= X && particle.posX < X + 1 && particle.posZ >= Z && particle.posZ < Z + 1 && Math.abs(particle.posY - (Y + 3.7D)) < 0.01D) {
					found.add(particle);
				}
			}
		}
		return found;
	}

	/** 26.3's compost: the spore blossom and the big dripleaf 0.65 (MEDIUM), the small dripleaf 0.3 (LOW). */
	public void testCompost() {
		Map<ItemStack, Integer> composts = ganymedes01.etfuturum.api.CompostingRegistry.getComposts();
		String got = composts.get(new ItemStack(block("spore_blossom"))) + " " + composts.get(new ItemStack(block("big_dripleaf"))) + " "
			+ composts.get(new ItemStack(block("small_dripleaf")));
		assertEqual("65 65 30", got, "compost chances");
	}
}
