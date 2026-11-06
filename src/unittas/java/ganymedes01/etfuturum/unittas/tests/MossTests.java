package ganymedes01.etfuturum.unittas.tests;

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
 * Moss and azalea as our own (SPEC-modern-movement.md §90.5, step D), as 26.3's: the moss block (its bonemeal a
 * vegetation patch), the carpet, azalea and flowering azalea (bonemeal grows 26.3's azalea tree on McFly's tree system),
 * their leaves (McFly's leaves: decay past 6 from a log, 26.3's drops), rooted dirt (a hoe makes dirt and hanging roots,
 * bonemeal hanging roots below), hanging roots; EFR's stored form by 26.3's names; strength, sounds, colours, fire;
 * recipes (26.3's, not EFR's vines round dirt) and compost. At z 340, y 8 and up; EFR's are an experiment, off here.
 */
@TasSuite("caves")
public class MossTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 340;
	private static final String[] BLOCKS = {"moss_block", "moss_carpet", "azalea", "azalea_leaves", "rooted_dirt", "hanging_roots"};

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

	/** x 34 to 46, y 6 to 19, z 334 to 346 emptied (room for a tree), its items gone. */
	private void clear() {
		for (int x = X - 6; x <= X + 6; x++) {
			for (int y = Y - 2; y <= Y + 11; y++) {
				for (int z = Z - 6; z <= Z + 6; z++) {
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
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 7, Y - 3, Z - 7, X + 8, Y + 12, Z + 8))) {
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
			item.setDead();
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
		if (found == null) {
			return "nothing";
		}
		String persistent = found.state().properties().get("persistent");
		return found.type().id.replace("minecraft:", "") + ("true".equals(persistent) ? ",persistent" : "");
	}

	private void harvest(int x, int y, int z, ItemStack held) {
		player().inventory.setInventorySlotContents(player().inventory.currentItem, held);
		player().theItemInWorldManager.tryHarvestBlock(x, y, z);
	}

	private void floor(Block block, int radius) {
		for (int x = X - radius; x <= X + radius; x++) {
			for (int z = Z - radius; z <= Z + radius; z++) {
				world().setBlock(x, Y - 1, z, block, 0, 2);
			}
		}
	}

	public void testMossIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : BLOCKS) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : new String[]{"blocks.BlockMoss", "blocks.BlockMossCarpet", "blocks.BlockAzalea", "blocks.BlockAzaleaLeaves",
			"client.renderer.block.BlockAzaleaRenderer"}) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.add(name + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "moss's and azalea's classes");
	}

	/** EFR's ids and metas by 26.3's names: azalea 0 and 1, the leaves' kind in meta & 3, 4 placed (26.3's persistent). */
	public void testStoredForm() {
		String got = onServer(() -> name(block("moss_block"), 0) + " " + name(block("moss_carpet"), 0) + " " + name(block("azalea"), 0) + " "
			+ name(block("azalea"), 1) + " " + name(block("azalea_leaves"), 0) + " " + name(block("azalea_leaves"), 1) + " " + name(block("azalea_leaves"), 5)
			+ " " + name(block("rooted_dirt"), 0) + " " + name(block("hanging_roots"), 0));
		assertEqual("moss_block moss_carpet azalea flowering_azalea azalea_leaves flowering_azalea_leaves flowering_azalea_leaves,persistent rooted_dirt "
			+ "hanging_roots", got, "26.3's names");
	}

	/** 26.3's strength, colours and fire (moss 0.1 green; azalea instabreak, fire 30 and 60; leaves 0.2; rooted dirt 0.5, a shovel). */
	public void testProperties() {
		Object[][] want = {{"moss_block", 0, "0.1 0 0", MapColor.greenColor}, {"moss_carpet", 0, "0.1 0 0", MapColor.greenColor},
			{"azalea", 0, "0.0 30 60", MapColor.foliageColor}, {"azalea", 1, "0.0 30 60", MapColor.foliageColor},
			{"azalea_leaves", 0, "0.2 30 60", MapColor.foliageColor}, {"rooted_dirt", 0, "0.5 0 0", MapColor.dirtColor},
			{"hanging_roots", 0, "0.0 30 60", MapColor.dirtColor}};
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			floor(Blocks.grass, 1);
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
			for (Object[] entry : want) {
				Block block = block((String) entry[0]);
				int meta = (Integer) entry[1];
				world().setBlock(X, Y, Z, block, meta, 2);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP) + " "
					+ block.getFlammability(world(), X, Y, Z, ForgeDirection.UP);
				if (!entry[2].equals(got) || block.getMapColor(meta) != entry[3]) {
					found.add(entry[0] + ":" + meta + " " + got + " colour " + block.getMapColor(meta).colorIndex);
				}
			}
			if (!"shovel".equals(block("rooted_dirt").getHarvestTool(0))) {
				found.add("rooted_dirt's tool " + block("rooted_dirt").getHarvestTool(0));
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, fire, colour (want: as listed)");
	}

	/**
	 * An azalea on moss, bonemealed till it grows (26.3's 0.45 a try): 26.3's azalea tree, oak logs up from the azalea's
	 * place (4 to 6 and a bend), azalea leaves round them (not placed: they decay), rooted dirt under the trunk.
	 */
	public void testAzaleaGrowsATree() {
		String got = onServer(() -> {
			floor(block("moss_block"), 6);
			world().setBlock(X, Y, Z, block("azalea"), 0, 2);
			for (int i = 0; i < 40 && world().getBlock(X, Y, Z) == block("azalea"); i++) {
				ItemDye.applyBonemeal(new ItemStack(Items.dye, 1, 15), world(), X, Y, Z, player());
			}
			int logs = 0, leaves = 0, placed = 0;
			for (int x = X - 6; x <= X + 6; x++) {
				for (int y = Y; y <= Y + 11; y++) {
					for (int z = Z - 6; z <= Z + 6; z++) {
						Block there = world().getBlock(x, y, z);
						logs += there == Blocks.log && world().getBlockMetadata(x, y, z) == 0 ? 1 : 0;
						if (there == block("azalea_leaves")) {
							leaves++;
							placed += (world().getBlockMetadata(x, y, z) & 4) != 0 ? 1 : 0;
						}
					}
				}
			}
			return "origin " + (world().getBlock(X, Y, Z) == Blocks.log ? "log" : name(world().getBlock(X, Y, Z), 0)) + ", below "
				+ name(world().getBlock(X, Y - 1, Z), 0) + ", logs " + (logs >= 5 && logs <= 9 ? "5 to 9" : logs) + ", leaves "
				+ (leaves >= 20 ? "20 or more" : leaves) + ", placed " + placed;
		});
		assertEqual("origin log, below rooted_dirt, logs 5 to 9, leaves 20 or more, placed 0", got, "the grown azalea tree");
	}

	/** A leaf 3 leaves from a log stays through random ticks; its log gone, it decays; a placed one never does. */
	public void testLeavesDecayPastSixFromALog() {
		String got = onServer(() -> {
			Block leaves = block("azalea_leaves");
			world().setBlock(X, Y, Z, Blocks.log, 0, 2);
			for (int i = 1; i <= 3; i++) {
				world().setBlock(X + i, Y, Z, leaves, 0, 2);
			}
			world().setBlock(X, Y, Z + 2, leaves, 4, 2);
			Random random = new Random(2);
			for (int i = 0; i < 20; i++) {
				leaves.updateTick(world(), X + 3, Y, Z, random);
			}
			String near = world().getBlock(X + 3, Y, Z) == leaves ? "stays" : "decayed";
			world().setBlock(X, Y, Z, Blocks.air, 0, 2);
			for (int i = 0; i < 20; i++) {
				for (int x = X + 1; x <= X + 3; x++) {
					if (world().getBlock(x, Y, Z) == leaves) {
						leaves.updateTick(world(), x, Y, Z, random);
					}
				}
				leaves.updateTick(world(), X, Y, Z + 2, random);
			}
			return "near a log " + near + ", log gone " + (world().getBlock(X + 3, Y, Z) == leaves ? "stays" : "decayed") + ", placed "
				+ (world().getBlock(X, Y, Z + 2) == leaves ? "stays" : "decayed");
		});
		assertEqual("near a log stays, log gone decayed, placed stays", got, "azalea leaves' decay");
	}

	/** 26.3's leaves' loot: shears itself; by hand an azalea at 1 in 20 and 1 to 2 sticks at 1 in 50 (over 2000 leaves). */
	public void testLeavesDrops() {
		String got = onServer(() -> {
			Block leaves = block("azalea_leaves");
			world().setBlock(X, Y, Z, leaves, 1, 2);
			harvest(X, Y, Z, new ItemStack(Items.shears));
			String sheared = dropped();
			int azaleas = 0, sticks = 0;
			for (int i = 0; i < 2000; i++) {
				for (ItemStack drop : leaves.getDrops(world(), X, Y, Z, 0, 0)) {
					if (drop.getItem() == Item.getItemFromBlock(block("azalea"))) {
						azaleas += drop.stackSize;
					} else if (drop.getItem() == Items.stick) {
						sticks += drop.stackSize;
					}
				}
			}
			return "sheared " + sheared + ", azaleas " + (azaleas >= 60 && azaleas <= 140 ? "about 100" : azaleas) + ", sticks "
				+ (sticks >= 25 && sticks <= 100 ? "about 60" : sticks);
		});
		assertEqual("sheared {azalea_leaves:1=1}, azaleas about 100, sticks about 60", got, "the leaves' drops");
	}

	/** Bonemeal on moss in a stone floor: 26.3's patch turns stone round it to moss and grows plants on some. */
	public void testMossBonemealSpreads() {
		String got = onServer(() -> {
			floor(Blocks.stone, 4);
			world().setBlock(X, Y - 1, Z, block("moss_block"), 0, 2);
			ItemDye.applyBonemeal(new ItemStack(Items.dye, 1, 15), world(), X, Y - 1, Z, player());
			int moss = 0, plants = 0;
			for (int x = X - 4; x <= X + 4; x++) {
				for (int z = Z - 4; z <= Z + 4; z++) {
					moss += world().getBlock(x, Y - 1, z) == block("moss_block") ? 1 : 0;
					plants += world().isAirBlock(x, Y, z) ? 0 : 1;
				}
			}
			return "moss " + (moss >= 9 ? "9 or more" : moss) + ", plants " + (plants >= 1 ? "some" : "none");
		});
		assertEqual("moss 9 or more, plants some", got, "after one bonemeal");
	}

	/** Rooted dirt: a hoe makes dirt and drops one hanging roots; bonemeal grows hanging roots below. */
	public void testRootedDirt() {
		String got = onServer(() -> {
			world().setBlock(X, Y + 2, Z, block("rooted_dirt"), 0, 2);
			ItemDye.applyBonemeal(new ItemStack(Items.dye, 1, 15), world(), X, Y + 2, Z, player());
			String below = name(world().getBlock(X, Y + 1, Z), 0);
			world().setBlock(X, Y, Z, block("rooted_dirt"), 0, 2);
			ItemStack hoe = new ItemStack(Items.iron_hoe);
			hoe.getItem().onItemUse(hoe, player(), world(), X, Y, Z, 1, 0.5F, 1.0F, 0.5F);
			return "bonemeal " + below + ", hoe " + name(world().getBlock(X, Y, Z), world().getBlockMetadata(X, Y, Z)) + " " + dropped();
		});
		assertEqual("bonemeal hanging_roots, hoe dirt {hanging_roots:0=1}", got, "rooted dirt");
	}

	/** Hanging roots: held under a full face (gone without it); shears only. A moss carpet goes with what it's on. */
	public void testRootsAndCarpetSupport() {
		String got = onServer(() -> {
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
			world().setBlock(X, Y, Z, block("hanging_roots"), 0, 2);
			harvest(X, Y, Z, null);
			String byHand = dropped();
			world().setBlock(X, Y, Z, block("hanging_roots"), 0, 2);
			harvest(X, Y, Z, new ItemStack(Items.shears));
			String sheared = dropped();
			world().setBlock(X, Y, Z, block("hanging_roots"), 0, 2);
			world().setBlock(X, Y + 1, Z, Blocks.air, 0, 3);
			String unheld = world().isAirBlock(X, Y, Z) ? "gone" : "still there";
			world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 2);
			world().setBlock(X, Y, Z, block("moss_carpet"), 0, 2);
			world().setBlock(X, Y - 1, Z, Blocks.air, 0, 3);
			return "roots by hand " + byHand + ", sheared " + sheared + ", unheld " + unheld + "; carpet unheld "
				+ (world().isAirBlock(X, Y, Z) ? "gone" : "still there");
		});
		assertEqual("roots by hand {}, sheared {hanging_roots:0=1}, unheld gone; carpet unheld gone", got, "hanging roots and the carpet");
	}

	/** 26.3's compost: moss 0.65, the carpet 0.3, azalea 0.65, flowering 0.85, the leaves 0.3 and 0.5, hanging roots 0.3. */
	public void testCompost() {
		Map<ItemStack, Integer> composts = ganymedes01.etfuturum.api.CompostingRegistry.getComposts();
		String got = composts.get(new ItemStack(block("moss_block"))) + " " + composts.get(new ItemStack(block("moss_carpet"))) + " "
			+ composts.get(new ItemStack(block("azalea"), 1, 0)) + " " + composts.get(new ItemStack(block("azalea"), 1, 1)) + " "
			+ composts.get(new ItemStack(block("azalea_leaves"), 1, 0)) + " " + composts.get(new ItemStack(block("azalea_leaves"), 1, 1)) + " "
			+ composts.get(new ItemStack(block("hanging_roots")));
		assertEqual("65 30 65 85 30 50 30", got, "compost chances");
	}

	/** 26.3's recipes; EFR's moss of 8 vines round dirt (26.3 has none) gone. */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("moss_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(3, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
		String vines = onServer(() -> {
			net.minecraft.inventory.InventoryCrafting grid = new net.minecraft.inventory.InventoryCrafting(new net.minecraft.inventory.Container() {
				@Override
				public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
					return false;
				}
			}, 3, 3);
			for (int i = 0; i < 9; i++) {
				grid.setInventorySlotContents(i, new ItemStack(i == 4 ? Blocks.dirt : Blocks.vine));
			}
			ItemStack made = net.minecraft.item.crafting.CraftingManager.getInstance().findMatchingRecipe(grid, world());
			return made == null ? "nothing" : Item.itemRegistry.getNameForObject(made.getItem());
		});
		assertEqual("nothing", vines, "8 vines round dirt");
	}

	// ---------------------------------------------------------------- the falling leaves (step D2), by the player

	private static final int SEEN_X = -3, SEEN_Y = 7, SEEN_Z = -3;

	/**
	 * 26.3's falling leaves under azalea leaves (`UntintedParticleLeavesBlock`: a display tick's nextFloat below 0.01,
	 * then x and z in the block, 0.05 below it; tinted 0x70922D), none over a block whose top face is full; each starts
	 * falling at 0.021 a tick, 0.00021 faster each tick (`FallingLeavesParticle`'s tinted provider). Not raining (1.7.10's
	 * dripping water would take its own turn of the random first).
	 */
	public void testFallingLeafParticles() {
		onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(0.5D, 4.0D, 0.5D, 0.0F, 0.0F);
			world().getWorldInfo().setRaining(false);
			world().setBlock(SEEN_X, SEEN_Y, SEEN_Z, block("azalea_leaves"), 4, 2);
			world().setBlock(SEEN_X, SEEN_Y - 1, SEEN_Z, Blocks.air, 0, 2);
		});
		try {
			waitUntilOnClient("the client's air under the leaves, no rain", () -> mc().theWorld.isAirBlock(SEEN_X, SEEN_Y - 1, SEEN_Z)
				&& !mc().theWorld.canLightningStrikeAt(SEEN_X, SEEN_Y + 1, SEEN_Z), 60);
			String open = onClient(() -> fallingLeaves(1L));
			onServer(() -> world().setBlock(SEEN_X, SEEN_Y - 1, SEEN_Z, Blocks.stone, 0, 2));
			waitUntilOnClient("the client's stone under the leaves", () -> mc().theWorld.getBlock(SEEN_X, SEEN_Y - 1, SEEN_Z) == Blocks.stone, 40);
			String overStone = onClient(() -> fallingLeaves(2L));
			assertEqual("all 24 of 24; tinted 70922d, 0.05000 below; falls 0.02121 then 0.02142", open, "falling leaves over air");
			assertEqual("0 (none of 17)", overStone, "falling leaves over stone");
		} finally {
			onServer(() -> {
				world().setBlock(SEEN_X, SEEN_Y, SEEN_Z, Blocks.air, 0, 2);
				world().setBlock(SEEN_X, SEEN_Y - 1, SEEN_Z, Blocks.air, 0, 2);
			});
		}
	}

	/**
	 * 2000 display ticks of the leaves on the client, on a seeded random, the particle setting at All: over air, how many
	 * falling leaves came of the ones 26.3 would make (the same random replayed), their tint and height, and the first
	 * one's fall over two of its ticks; over a full face, how many came (of the ones it would make over air).
	 */
	private String fallingLeaves(long seed) {
		Block leaves = block("azalea_leaves");
		int expected = 0;
		Random replay = new Random(seed);
		for (int i = 0; i < 2000; i++) {
			if (replay.nextFloat() < 0.01F) {
				expected++;
				replay.nextDouble();
				replay.nextDouble();
			}
		}
		int setting = mc().gameSettings.particleSetting;
		List<net.minecraft.client.particle.EntityFX> before = particlesUnderTheLeaves();
		List<net.minecraft.client.particle.EntityFX> made;
		try {
			mc().gameSettings.particleSetting = 0;
			Random random = new Random(seed);
			for (int i = 0; i < 2000; i++) {
				leaves.randomDisplayTick(mc().theWorld, SEEN_X, SEEN_Y, SEEN_Z, random);
			}
		} finally {
			mc().gameSettings.particleSetting = setting;
		}
		made = particlesUnderTheLeaves();
		made.removeAll(before);
		if (!mc().theWorld.isAirBlock(SEEN_X, SEEN_Y - 1, SEEN_Z)) {
			return made.size() + " (none of " + expected + ")";
		}
		if (made.isEmpty()) {
			return "none of " + expected;
		}
		java.util.Set<String> tints = new java.util.TreeSet<>();
		java.util.Set<String> heights = new java.util.TreeSet<>();
		for (net.minecraft.client.particle.EntityFX particle : made) {
			int rgb = Math.round(particle.getRedColorF() * 255.0F) << 16 | Math.round(particle.getGreenColorF() * 255.0F) << 8 | Math.round(particle.getBlueColorF() * 255.0F);
			tints.add(String.format("%06x", rgb));
			heights.add(String.format("%.5f", SEEN_Y - particle.posY));
		}
		net.minecraft.client.particle.EntityFX first = made.get(0);
		double y0 = first.posY;
		first.onUpdate();
		double y1 = first.posY;
		first.onUpdate();
		double y2 = first.posY;
		return (made.size() == expected ? "all " : "") + made.size() + " of " + expected + "; tinted " + String.join(",", tints) + ", "
			+ String.join(",", heights) + " below; falls " + String.format("%.5f then %.5f", y0 - y1, y1 - y2);
	}

	/** The client's particles in the block under the leaves. */
	@SuppressWarnings("unchecked")
	private List<net.minecraft.client.particle.EntityFX> particlesUnderTheLeaves() {
		List<?>[] layers = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(net.minecraft.client.particle.EffectRenderer.class, mc().effectRenderer,
			"fxLayers", "field_78876_b");
		List<net.minecraft.client.particle.EntityFX> found = new ArrayList<>();
		for (List<?> layer : layers) {
			for (Object entry : layer) {
				net.minecraft.client.particle.EntityFX particle = (net.minecraft.client.particle.EntityFX) entry;
				if (particle.posX >= SEEN_X && particle.posX < SEEN_X + 1 && particle.posZ >= SEEN_Z && particle.posZ < SEEN_Z + 1
					&& particle.posY > SEEN_Y - 1 && particle.posY < SEEN_Y) {
					found.add(particle);
				}
			}
		}
		return found;
	}
}
