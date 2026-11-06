package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.RandomTicking;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Copper as our own (SPEC-modern-movement.md §87), as 26.3's: ours, EFR's classes gone; the stored form EFR wrote, read
 * as before (26.3's names through the modern command layer); strength and sounds; ageing (`ChangeOverTimeBlock`: a
 * random tick's 0.05688889, then the neighbours' sum); honeycomb's wax, a dispenser's, the axe's wax off and scrape; the
 * bulb, the grate, doors by hand; the raw ores' drops; 26.3's recipes (`copper_recipes_26_3.txt`, from its jar).
 * Lightning's part is `LightningTests`'. A row at z 40 on the flat world.
 */
@TasSuite("copper")
public class CopperTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 40;
	private static final String[] STAGES = {"", "exposed_", "weathered_", "oxidized_"};
	private static final String[] EFR_CLASSES = {"blocks.BlockCopper", "blocks.BlockChiseledCopper", "blocks.BlockCopperGrate",
		"blocks.BlockCopperBulb", "blocks.BlockCutCopperSlab", "blocks.BlockCutCopperStairs", "blocks.BlockCopperDoor",
		"blocks.BlockCopperTrapdoor", "blocks.IDegradable", "blocks.BlockLightningRod", "blocks.ores.BlockCopperOre",
		"blocks.ores.BlockDeepslateCopperOre", "blocks.rawore.BlockRawOre"};

	@Override
	protected void setUp() {
		clear();
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
	}

	@Override
	protected void tearDown() {
		clear();
		onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
	}

	private void clear() {
		onServer(() -> {
			for (int x = X - 3; x <= X + 12; x++) {
				for (int y = Y; y <= Y + 3; y++) {
					for (int z = Z - 3; z <= Z + 3; z++) {
						world().setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
			for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
				if (entity instanceof EntityItem) {
					((EntityItem) entity).setDead();
				}
			}
		});
	}

	// ---------------------------------------------------------------- helpers

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	/** 26.3's name's state here: the block and meta the command layer gives it. */
	private static BlockIds.State state(String modern) {
		BlockIds.Type type = BlockIds.byId("minecraft:" + modern);
		assertTrue(type != null, "no block for minecraft:" + modern);
		return type.defaultState();
	}

	private static void put(int x, int y, int z, String modern) {
		BlockIds.State state = state(modern);
		world().setBlock(x, y, z, state.block(), state.meta(), 3);
	}

	/** 26.3's name of what's at a place (empty for nothing it knows). */
	private static String at(int x, int y, int z) {
		BlockIds.Found found = BlockIds.find(world().getBlock(x, y, z), world().getBlockMetadata(x, y, z));
		return found == null ? "?" : found.type().id.replace("minecraft:", "");
	}

	private static ItemStack stack(String modern, int count) {
		ItemIds.Entry entry = ItemIds.byId("minecraft:" + modern);
		assertTrue(entry != null, "no item for minecraft:" + modern);
		return entry.createStack(count);
	}

	/** A random whose floats are these, then 0.999 (no further chance passes). */
	private static Random rolls(float... floats) {
		return new Random() {
			private int next;

			@Override
			public float nextFloat() {
				return next < floats.length ? floats[next++] : 0.999F;
			}
		};
	}

	/** A random tick here, as the world gives one (McFly's random tick where the block has one). */
	private static void randomTick(int x, int y, int z, Random random) {
		Block block = world().getBlock(x, y, z);
		if (block instanceof RandomTicking) {
			((RandomTicking) block).randomTick(world(), x, y, z, random);
		} else {
			block.updateTick(world(), x, y, z, random);
		}
	}

	// ---------------------------------------------------------------- ours

	/** Every copper block and item McFly's, the rod in McFly's package; none of EFR's copper classes left. */
	public void testCopperIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : new String[]{"copper_block", "chiseled_copper", "copper_grate", "copper_bulb", "powered_copper_bulb", "cut_copper_slab",
			"double_cut_copper_slab", "cut_copper_stairs", "waxed_oxidized_cut_copper_stairs", "copper_door", "waxed_oxidized_copper_door",
			"copper_trapdoor", "waxed_oxidized_copper_trapdoor", "lightning_rod", "waxed_oxidized_lightning_rod", "copper_ore",
			"deepslate_copper_ore", "raw_ore_block"}) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : new String[]{"copper_ingot", "raw_ore"}) {
			Object item = net.minecraft.item.Item.itemRegistry.getObject(McFly.DOMAIN + ":" + name);
			if (item == null || !item.getClass().getName().startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + (item == null ? "missing" : item.getClass().getName()));
			}
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.add(name + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "copper's classes");
	}

	// ---------------------------------------------------------------- the stored form

	/**
	 * Each id and meta EFR wrote is read as before, by 26.3's names: the copper block's 16 (stage, cut, waxed), chiseled
	 * copper's 8 and the 12 to 15 EFR's waxing in the world wrote, the grate's 8, the bulb's lit and powered, the slab's
	 * halves and its double.
	 */
	public void testStoredForm() {
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (int meta = 0; meta < 16; meta++) {
				String stage = STAGES[meta % 4];
				String wax = meta >= 8 ? "waxed_" : "";
				boolean cut = meta % 8 >= 4;
				String want = wax + stage + (cut ? "cut_copper" : stage.isEmpty() ? "copper_block" : "copper");
				check(found, "copper_block", meta, want, null);
				if (meta < 8) {
					check(found, "chiseled_copper", meta, (meta >= 4 ? "waxed_" : "") + STAGES[meta % 4] + "chiseled_copper", null);
				}
				if (meta < 8) {
					check(found, "copper_grate", meta, (meta >= 4 ? "waxed_" : "") + STAGES[meta % 4] + "copper_grate", null);
					String slab = (meta >= 4 ? "waxed_" : "") + STAGES[meta % 4] + "cut_copper_slab";
					check(found, "cut_copper_slab", meta, slab, "type=bottom");
					check(found, "cut_copper_slab", meta + 8, slab, "type=top");
					check(found, "double_cut_copper_slab", meta, slab, "type=double");
				}
				String bulb = wax + stage + "copper_bulb";
				check(found, "copper_bulb", meta, bulb, "lit=" + (meta % 8 >= 4) + ",powered=false");
				check(found, "powered_copper_bulb", meta, bulb, "lit=" + (meta % 8 >= 4) + ",powered=true");
			}
			// chiseled copper's 12 to 15 (EFR's waxing in the world wrote them; no item has them, so no name): waxed, its
			// stage meta % 4: it drops the waxed item and an axe takes its wax off
			Block chiseled = block("chiseled_copper");
			for (int meta = 12; meta < 16; meta++) {
				world().setBlock(X, Y, Z, chiseled, meta, 2);
				ItemStack axe = new ItemStack(Items.iron_axe);
				player().inventory.setInventorySlotContents(player().inventory.currentItem, axe);
				player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), axe, X, Y, Z, 1, 0.5F, 1.0F, 0.5F);
				String after = at(X, Y, Z);
				if (chiseled.damageDropped(meta) != 4 + meta % 4 || !(STAGES[meta % 4] + "chiseled_copper").equals(after)) {
					found.add("chiseled_copper:" + meta + " drops " + chiseled.damageDropped(meta) + ", an axe leaves " + after);
				}
			}
			world().setBlock(X, Y, Z, Blocks.air, 0, 2);
			return found;
		});
		assertEqual("[]", wrong.toString(), "the stored form, as 26.3's names");
	}

	private static void check(List<String> wrong, String id, int meta, String modern, String properties) {
		BlockIds.Found found = BlockIds.find(block(id), meta);
		String got = found == null ? "nothing" : found.type().id.replace("minecraft:", "");
		Map<String, String> props = found == null ? null : found.state().properties();
		boolean propsRight = true;
		if (properties != null && props != null) {
			for (String pair : properties.split(",")) {
				String[] kv = pair.split("=");
				propsRight &= kv[1].equals(props.get(kv[0]));
			}
		}
		if (!modern.equals(got) || !propsRight) {
			wrong.add(id + ":" + meta + " " + got + (props == null ? "" : props.toString()) + " not " + modern + (properties == null ? "" : "[" + properties + "]"));
		}
	}

	// ---------------------------------------------------------------- strength and sounds

	/**
	 * 26.3's strength (hardness, then explosion resistance as 26.3 gives it: 1.7.10's field is 5 times it) and the
	 * pickaxe's level for drops: copper 3 and 6, stone's; its ores 3 and 3, the deepslate one 4.5 and 3; raw blocks 5 and
	 * 6, gold's iron's level. And 26.3's sounds: copper's, the grate's, the bulb's, deepslate's.
	 */
	public void testStrengthAndSounds() {
		String[][] blocks = {{"copper_block", "3.0 6.0 1", "copper"}, {"oxidized_cut_copper", "3.0 6.0 1", "copper"},
			{"waxed_chiseled_copper", "3.0 6.0 1", "copper"}, {"copper_grate", "3.0 6.0 1", "copper_grate"},
			{"copper_bulb", "3.0 6.0 1", "copper_bulb"}, {"cut_copper_slab", "3.0 6.0 1", "copper"}, {"cut_copper_stairs", "3.0 6.0 1", "copper"},
			{"copper_trapdoor", "3.0 6.0 1", "copper"}, {"lightning_rod", "3.0 6.0 1", "copper"},
			{"copper_ore", "3.0 3.0 1", "stone"}, {"deepslate_copper_ore", "4.5 3.0 1", "deepslate"},
			{"raw_copper_block", "5.0 6.0 1", "stone"}, {"raw_iron_block", "5.0 6.0 1", "stone"}, {"raw_gold_block", "5.0 6.0 2", "stone"}};
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (String[] entry : blocks) {
				BlockIds.State state = state(entry[0]);
				world().setBlock(X, Y, Z, state.block(), state.meta(), 3);
				Block block = state.block();
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null) + " " + block.getHarvestLevel(state.meta());
				String sound = block.stepSound.getBreakSound();
				String wantSound = "stone".equals(entry[2]) ? "dig.stone" : McFly.MC_ASSETS + ":block." + entry[2] + ".break";
				if (!entry[1].equals(got) || !wantSound.equals(sound) || !"pickaxe".equals(block.getHarvestTool(state.meta()))) {
					found.add(entry[0] + " " + got + " " + block.getHarvestTool(state.meta()) + " " + sound);
				}
			}
			world().setBlock(X, Y, Z, Blocks.air, 0, 3);
			return found;
		});
		assertEqual("[]", wrong.toString(), "strength, the pickaxe's level and sounds (want: as listed)");
	}

	// ---------------------------------------------------------------- ageing

	/**
	 * 26.3's ageing: a random tick's 0.05688889 first, then within 4 (Manhattan) of the same kind (not waxed): any younger
	 * and nothing; else ((older + 1) / (older + same + 1))^2, times 0.75 unaffected. Lone unaffected copper: 0.75, so a
	 * roll of 0.74 ages it and 0.76 doesn't; beside one of the same age, 0.1875; beside an older one, 0.75; beside a
	 * younger one, nothing; a lone exposed one, 1.0; a first roll of 0.06, nothing at all.
	 */
	public void testAgeing() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			out.append("lone ").append(age(new String[0], "copper_block", 0.05F, 0.74F)).append('/').append(age(new String[0], "copper_block", 0.05F, 0.76F));
			out.append(", first roll ").append(age(new String[0], "copper_block", 0.06F, 0.0F));
			out.append(", same ").append(age(new String[]{"copper_block"}, "copper_block", 0.05F, 0.18F)).append('/')
				.append(age(new String[]{"copper_block"}, "copper_block", 0.05F, 0.19F));
			out.append(", older ").append(age(new String[]{"exposed_copper"}, "copper_block", 0.05F, 0.74F));
			out.append(", younger ").append(age(new String[]{"copper_block"}, "exposed_copper", 0.05F, 0.0F));
			out.append(", exposed ").append(age(new String[0], "exposed_copper", 0.05F, 0.99F));
			out.append(", waxed neighbour ").append(age(new String[]{"waxed_copper_block"}, "exposed_copper", 0.05F, 0.99F));
			out.append(", waxed ").append(age(new String[0], "waxed_copper_block", 0.0F, 0.0F));
			out.append(", oxidized ").append(age(new String[0], "oxidized_copper", 0.0F, 0.0F));
			return out.toString();
		});
		assertEqual("lone exposed_copper/copper_block, first roll copper_block, same exposed_copper/copper_block, older exposed_copper, younger exposed_copper,"
			+ " exposed weathered_copper, waxed neighbour weathered_copper, waxed waxed_copper_block, oxidized oxidized_copper", got, "what a random tick left");
	}

	/** One block given a random tick with these rolls, the neighbours 2 east of it: 26.3's name after. */
	private static String age(String[] neighbours, String block, float... rolls) {
		for (int dx = 0; dx <= 4; dx++) {
			world().setBlock(X + dx, Y, Z, Blocks.air, 0, 2);
		}
		put(X, Y, Z, block);
		for (int i = 0; i < neighbours.length; i++) {
			put(X + 2 + i, Y, Z, neighbours[i]);
		}
		randomTick(X, Y, Z, rolls(rolls));
		return at(X, Y, Z);
	}

	/** A door ages from its lower half, and both halves change; its upper half's tick does nothing. */
	public void testDoorAgesFromItsLowerHalf() {
		String got = onServer(() -> {
			world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 3);
			BlockIds.State door = state("copper_door");
			world().setBlock(X, Y, Z, door.block(), 0, 2);
			world().setBlock(X, Y + 1, Z, door.block(), 8, 2);
			randomTick(X, Y + 1, Z, rolls(0.0F, 0.0F));
			String upper = at(X, Y, Z) + " " + at(X, Y + 1, Z);
			randomTick(X, Y, Z, rolls(0.0F, 0.0F));
			return "upper's tick " + upper + ", lower's tick " + at(X, Y, Z) + " " + at(X, Y + 1, Z);
		});
		assertEqual("upper's tick copper_door copper_door, lower's tick exposed_copper_door exposed_copper_door", got, "a copper door's ageing");
	}

	// ---------------------------------------------------------------- wax and the axe

	/**
	 * Honeycomb on unwaxed copper waxes it, keeping its shape (a lit bulb lit, a rod's facing, a door's both halves); one
	 * used in survival, none in creative; nothing on waxed copper. Sneaking: 26.3's block action comes first, so a door
	 * or trapdoor clicked with honeycomb opens unless the player sneaks (the last case).
	 */
	public void testHoneycombWaxes() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			player().setSneaking(true);
			world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 3);
			for (String name : new String[]{"exposed_copper", "cut_copper_slab", "weathered_cut_copper_stairs", "copper_grate", "chiseled_copper", "copper_trapdoor"}) {
				put(X, Y, Z, name);
				out.append(wax(X, Y, Z)).append(' ').append(at(X, Y, Z)).append(", ");
			}
			put(X, Y, Z, "copper_bulb");
			world().setBlockMetadataWithNotify(X, Y, Z, 4, 2);
			out.append(wax(X, Y, Z)).append(' ').append(at(X, Y, Z)).append(" lit ").append(world().getBlockMetadata(X, Y, Z) % 8 >= 4).append(", ");
			put(X, Y, Z, "lightning_rod");
			world().setBlockMetadataWithNotify(X, Y, Z, 3, 2);
			out.append(wax(X, Y, Z)).append(' ').append(at(X, Y, Z)).append(" facing ").append(world().getBlockMetadata(X, Y, Z) & 7).append(", ");
			BlockIds.State door = state("copper_door");
			world().setBlock(X, Y, Z, door.block(), 0, 2);
			world().setBlock(X, Y + 1, Z, door.block(), 8, 2);
			out.append(wax(X, Y + 1, Z)).append(' ').append(at(X, Y, Z)).append(' ').append(at(X, Y + 1, Z)).append(", ");
			world().setBlock(X, Y + 1, Z, Blocks.air, 0, 2);
			put(X, Y, Z, "waxed_copper_block");
			out.append("waxed again ").append(wax(X, Y, Z)).append(", ");
			player().setGameType(WorldSettings.GameType.CREATIVE);
			put(X, Y, Z, "copper_block");
			out.append("creative ").append(wax(X, Y, Z)).append(' ').append(at(X, Y, Z)).append(", ");
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().setSneaking(false);
			world().setBlock(X, Y, Z, door.block(), 0, 2);
			world().setBlock(X, Y + 1, Z, door.block(), 8, 2);
			out.append("door standing ").append(wax(X, Y, Z)).append(' ').append(at(X, Y, Z)).append(" open ").append((world().getBlockMetadata(X, Y, Z) & 4) != 0);
			return out.toString();
		});
		assertEqual("1 waxed_exposed_copper, 1 waxed_cut_copper_slab, 1 waxed_weathered_cut_copper_stairs, 1 waxed_copper_grate, 1 waxed_chiseled_copper,"
			+ " 1 waxed_copper_trapdoor, 1 waxed_copper_bulb lit true, 1 waxed_lightning_rod facing 3, 1 waxed_copper_door waxed_copper_door,"
			+ " waxed again 0, creative 0 waxed_copper_block, door standing 0 copper_door open true", got, "honeycomb used (how many) and what it left");
	}

	/** A right click with honeycomb at a place, as the server takes one; how many it used. */
	private static int wax(int x, int y, int z) {
		ItemStack honeycomb = stack("honeycomb", 10);
		player().inventory.setInventorySlotContents(player().inventory.currentItem, honeycomb);
		player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), honeycomb, x, y, z, 1, 0.5F, 1.0F, 0.5F);
		ItemStack left = player().inventory.getCurrentItem();
		return 10 - (left == null ? 0 : left.stackSize);
	}

	/** A dispenser with honeycomb waxes the copper it faces (26.3's honeycomb dispense behaviour). */
	public void testDispenserWaxes() {
		onServer(() -> {
			// meta 5: facing east
			world().setBlock(X, Y, Z, Blocks.dispenser, 5, 3);
			// a dispenser turns itself away from solid blocks when placed (1.7.10's onBlockAdded): east after that
			world().setBlockMetadataWithNotify(X, Y, Z, 5, 2);
			((net.minecraft.tileentity.TileEntityDispenser) world().getTileEntity(X, Y, Z)).setInventorySlotContents(0, stack("honeycomb", 4));
			put(X + 1, Y, Z, "weathered_copper");
		});
		onServer(() -> world().setBlock(X - 1, Y, Z, Blocks.redstone_block, 0, 3));
		serverTicks(8);
		String after = onServer(() -> at(X + 1, Y, Z) + " " + ((net.minecraft.tileentity.TileEntityDispenser) world().getTileEntity(X, Y, Z)).getStackInSlot(0).stackSize);
		assertEqual("waxed_weathered_copper 3", after, "the copper in front, then the honeycomb left");
	}

	/**
	 * An axe on waxed copper takes the wax off; on weathered copper, a stage back; on unaffected unwaxed copper, nothing;
	 * each use costs the axe 1.
	 */
	public void testAxeScrapesAndTakesWaxOff() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			ItemStack axe = new ItemStack(Items.iron_axe);
			for (String name : new String[]{"waxed_exposed_copper", "exposed_copper", "copper_block", "waxed_oxidized_cut_copper_stairs", "oxidized_copper_grate"}) {
				put(X, Y, Z, name);
				int damage = axe.getItemDamage();
				player().inventory.setInventorySlotContents(player().inventory.currentItem, axe);
				player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), axe, X, Y, Z, 1, 0.5F, 1.0F, 0.5F);
				out.append(name).append(" -> ").append(at(X, Y, Z)).append(" (").append(axe.getItemDamage() - damage).append("), ");
			}
			return out.toString();
		});
		assertEqual("waxed_exposed_copper -> exposed_copper (1), exposed_copper -> copper_block (1), copper_block -> copper_block (0),"
			+ " waxed_oxidized_cut_copper_stairs -> oxidized_cut_copper_stairs (1), oxidized_copper_grate -> weathered_copper_grate (1), ", got, "the axe on copper (its damage)");
	}

	// ---------------------------------------------------------------- the bulb, the grate, doors

	/** The bulb flips its light on a rising edge of power only; its light by stage while lit; a comparator reads 15 lit. */
	public void testBulb() {
		onServer(() -> put(X, Y, Z, "copper_bulb"));
		String[] steps = new String[4];
		for (int i = 0; i < 4; i++) {
			int step = i;
			onServer(() -> {
				if (step % 2 == 0) {
					world().setBlock(X - 1, Y, Z, Blocks.redstone_block, 0, 3);
				} else {
					world().setBlock(X - 1, Y, Z, Blocks.air, 0, 3);
				}
			});
			serverTicks(2);
			steps[i] = onServer(() -> {
				Block block = world().getBlock(X, Y, Z);
				return (world().getBlockMetadata(X, Y, Z) % 8 >= 4) + "/" + block.getComparatorInputOverride(world(), X, Y, Z, 0);
			});
		}
		String light = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (int stage = 0; stage < 4; stage++) {
				BlockIds.State state = state(STAGES[stage] + "copper_bulb");
				world().setBlock(X + 3, Y, Z, state.block(), state.meta() | 4, 2);
				out.append(world().getBlock(X + 3, Y, Z).getLightValue(world(), X + 3, Y, Z)).append(' ');
			}
			return out.toString().trim();
		});
		assertEqual("lit/comparator: powered true/15, unpowered true/15, powered false/0, unpowered false/0; light 15 12 8 4",
			"lit/comparator: powered " + steps[0] + ", unpowered " + steps[1] + ", powered " + steps[2] + ", unpowered " + steps[3] + "; light " + light, "the bulb");
	}

	/** The grate lets light through and isn't a full cube (26.3's transparent block). */
	public void testGrateLetsLightThrough() {
		String got = onServer(() -> {
			Block grate = state("copper_grate").block();
			return grate.getLightOpacity() + " " + grate.isOpaqueCube() + " " + grate.renderAsNormalBlock();
		});
		assertEqual("0 false false", got, "the grate's light opacity, opaque, drawn as a full block");
	}

	/** Copper doors and trapdoors open by hand (`BlockSetType.COPPER`). */
	public void testDoorsOpenByHand() {
		String got = onServer(() -> {
			world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 3);
			BlockIds.State door = state("copper_door");
			world().setBlock(X, Y, Z, door.block(), 0, 2);
			world().setBlock(X, Y + 1, Z, door.block(), 8, 2);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			door.block().onBlockActivated(world(), X, Y, Z, player(), 1, 0.5F, 0.5F, 0.5F);
			boolean doorOpen = (world().getBlockMetadata(X, Y, Z) & 4) != 0;
			put(X + 3, Y, Z, "copper_trapdoor");
			state("copper_trapdoor").block().onBlockActivated(world(), X + 3, Y, Z, player(), 1, 0.5F, 0.5F, 0.5F);
			boolean trapdoorOpen = (world().getBlockMetadata(X + 3, Y, Z) & 4) != 0;
			return doorOpen + " " + trapdoorOpen;
		});
		assertEqual("true true", got, "the door, then the trapdoor, open by hand");
	}

	// ---------------------------------------------------------------- ores

	/**
	 * Mined with a stone pickaxe, no Silk Touch: copper ore drops 2 to 5 raw copper, iron ore 1 raw iron, gold ore (an
	 * iron pickaxe) 1 raw gold; with Silk Touch the ore itself. 20 of each.
	 */
	public void testRawOreDrops() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			out.append("copper ").append(drops(state("copper_ore"), new ItemStack(Items.stone_pickaxe))).append(", ");
			out.append("deepslate copper ").append(drops(state("deepslate_copper_ore"), new ItemStack(Items.stone_pickaxe))).append(", ");
			out.append("iron ").append(drops(new BlockIds.State(Blocks.iron_ore, 0, null), new ItemStack(Items.stone_pickaxe))).append(", ");
			out.append("gold ").append(drops(new BlockIds.State(Blocks.gold_ore, 0, null), new ItemStack(Items.iron_pickaxe))).append(", ");
			ItemStack silk = new ItemStack(Items.iron_pickaxe);
			silk.addEnchantment(net.minecraft.enchantment.Enchantment.silkTouch, 1);
			out.append("silk ").append(drops(state("copper_ore"), silk));
			return out.toString();
		});
		assertEqual("copper raw_copper 2-5, deepslate copper raw_copper 2-5, iron raw_iron 1-1, gold raw_gold 1-1, silk copper_ore 1-1", got, "what ores drop (min-max of 20)");
	}

	/** The block mined 20 times by the player with the tool: what it dropped, and the least and most of it. */
	private static String drops(BlockIds.State ore, ItemStack tool) {
		String what = null;
		int least = Integer.MAX_VALUE;
		int most = 0;
		for (int i = 0; i < 20; i++) {
			world().setBlock(X, Y, Z, ore.block(), ore.meta(), 3);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, tool.copy());
			player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
			int count = 0;
			for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
				if (entity instanceof EntityItem item && !item.isDead && item.getDistance(X + 0.5, Y + 0.5, Z + 0.5) < 3.0) {
					ItemStack got = item.getEntityItem();
					String name = Recipes26_3.nameOf(got);
					what = what == null ? name : what.equals(name) ? what : what + "|" + name;
					count += got.stackSize;
					item.setDead();
				}
			}
			least = Math.min(least, count);
			most = Math.max(most, count);
		}
		return what + " " + least + "-" + most;
	}

	// ---------------------------------------------------------------- recipes

	/** 26.3's 105 copper, rod and raw ore recipes (its jar's, `copper_recipes_26_3.txt`), each giving 26.3's result here. */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("copper_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(105, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}
}
