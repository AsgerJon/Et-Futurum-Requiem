package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.tileentities.TileEntityBanner;
import ganymedes01.etfuturum.unittas.TasTestCase;
import ganymedes01.etfuturum.world.structure.modern.ModernBlockStates;
import ganymedes01.etfuturum.world.structure.modern.ModernBox;
import ganymedes01.etfuturum.world.structure.modern.ModernRotation;
import ganymedes01.etfuturum.world.structure.modern.ModernState;
import ganymedes01.etfuturum.world.structure.modern.ModernTemplate;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.world.WorldServer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

/**
 * 26.3's structure templates placed as 26.3 places them (SPEC-modern-movement.md §76.12, stage 7a): the reference
 * placed each of the 20 end city templates at each turn (pivot 0, structure blocks left out) and read back every block
 * that isn't air (`end_templates_26_3.txt.gz`). Each placed here must make the same blocks at the same places (26.3's
 * states as 1.7.10's blocks), with the same box, banners, brewing stand and markers. The table itself: a few states
 * checked by hand against 1.7.10's own metadata.
 */
public class StructureTemplateTests extends TasTestCase {

	private static final String DUMP = "/assets/etfuturum/unittas/end_templates_26_3.txt.gz";

	@Override
	protected void setUp() {
		if (!ModBlocks.PURPUR_BLOCK.isEnabled() || !ModBlocks.END_ROD.isEnabled() || !ModBlocks.BANNER.isEnabled()) {
			skipTest("purpur, end rods or banners are off");
		}
	}

	@Override
	protected int timeoutSeconds() {
		return 300;
	}

	/** One placing in the dump. */
	private static final class Placing {
		String name;
		ModernRotation rotation;
		int x;
		int y;
		int z;
		String box;
		/** "dx dy dz" to the rest of the line ("state [extra]"). */
		final Map<String, String> blocks = new LinkedHashMap<>();
		final List<String> markers = new ArrayList<>();
	}

	private static List<Placing> readDump() throws Exception {
		List<Placing> placings = new ArrayList<>();
		try (InputStream in = StructureTemplateTests.class.getResourceAsStream(DUMP)) {
			assertTrue(in != null, "no " + DUMP);
			BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8));
			Placing placing = null;
			for (String line; (line = reader.readLine()) != null; ) {
				String[] parts = line.split(" ");
				if (line.startsWith("t ")) {
					placing = new Placing();
					placing.name = parts[1];
					placing.rotation = ModernRotation.valueOf(parts[2]);
					placing.x = Integer.parseInt(parts[3]);
					placing.y = Integer.parseInt(parts[4]);
					placing.z = Integer.parseInt(parts[5]);
					placing.box = parts[6] + " " + parts[7] + " " + parts[8] + " " + parts[9] + " " + parts[10] + " " + parts[11];
					placings.add(placing);
				} else if (line.startsWith("b ")) {
					String[] fields = line.split(" ", 5);
					placing.blocks.put(fields[1] + " " + fields[2] + " " + fields[3], fields[4]);
				} else if (line.startsWith("m ")) {
					placing.markers.add(line.substring(2));
				}
			}
		}
		return placings;
	}

	/** A dumped state ("minecraft:x[a=b,c=d]") read back. */
	private static ModernState parse(String state) {
		int open = state.indexOf('[');
		Map<String, String> properties = new TreeMap<>();
		if (open >= 0) {
			for (String pair : state.substring(open + 1, state.length() - 1).split(",")) {
				String[] kv = pair.split("=");
				properties.put(kv[0], kv[1]);
			}
		}
		return new ModernState(open >= 0 ? state.substring(0, open) : state, properties);
	}

	private static String describe(Block block, int meta) {
		return Block.blockRegistry.getNameForObject(block) + ":" + meta;
	}

	/** What's at the place, with what its block entity holds of the dump's extras. */
	private static String found(WorldServer world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		StringBuilder out = new StringBuilder(describe(block, world.getBlockMetadata(x, y, z)));
		TileEntity entity = world.getTileEntity(x, y, z);
		if (entity instanceof TileEntityBanner banner) {
			NBTTagCompound tag = new NBTTagCompound();
			banner.writeToNBT(tag);
			out.append(" base ").append(tag.getInteger("Base")).append(tag.getBoolean("IsStanding") ? " standing" : " wall");
			NBTTagList patterns = tag.getTagList("Patterns", 10);
			for (int i = 0; i < patterns.tagCount(); i++) {
				out.append(' ').append(patterns.getCompoundTagAt(i).getString("Pattern")).append(':').append(patterns.getCompoundTagAt(i).getInteger("Color"));
			}
		} else if (entity instanceof TileEntityBrewingStand stand) {
			for (int i = 0; i < 4; i++) {
				ItemStack stack = stand.getStackInSlot(i);
				if (stack != null) {
					out.append(' ').append(i).append(':').append(stack.getItemDamage());
				}
			}
		} else if (entity instanceof TileEntitySkull skull) {
			out.append(" type ").append(skull.func_145904_a());
		}
		return out.toString();
	}

	/** What the dump's line should be here: its state through the table, its extras as 1.7.10 holds them. */
	private static String wanted(String line) {
		String[] parts = line.split(" ");
		ModernState state = parse(parts[0]);
		ModernBlockStates.Placed placed = ModernBlockStates.convert(state);
		if (placed == null) {
			return "no block for " + parts[0];
		}
		StringBuilder out = new StringBuilder(describe(placed.block, placed.meta));
		if (parts.length > 1 && parts[1].equals("patterns")) {
			String colour = state.id.substring("minecraft:".length(), state.id.indexOf(state.id.endsWith("_wall_banner") ? "_wall_banner" : "_banner"));
			out.append(" base ").append(15 - ModernBlockStates.woolMeta(colour)).append(state.id.endsWith("_wall_banner") ? " wall" : " standing");
			for (int i = 2; i < parts.length; i++) {
				String[] layer = parts[i].split(":");
				out.append(' ').append(ganymedes01.etfuturum.blocks.ModernBanners.byName(layer[0]).getPatternID()).append(':').append(15 - ModernBlockStates.woolMeta(layer[1]));
			}
		} else if (parts.length > 1 && parts[1].equals("items")) {
			for (int i = 2; i < parts.length; i++) {
				String[] item = parts[i].split(":");
				String potion = item[1].substring(item[1].indexOf('/') + 1);
				out.append(' ').append(item[0]).append(':').append(ganymedes01.etfuturum.potion.NamedPotions.byId(potion).legacyDamage());
			}
		} else if (state.id.endsWith("_head") || state.id.endsWith("_skull")) {
			out.append(" type ").append(state.id.contains("dragon") ? 5 : 0);
		}
		return out.toString();
	}

	/** Every template at every turn: the same box, blocks, block entities and markers as 26.3's. */
	public void testTemplatesAs26_3() throws Exception {
		List<Placing> placings = readDump();
		assertEqual(80, placings.size(), "placings in the dump");
		List<String> wrong = new ArrayList<>();
		int[] counts = new int[2];
		for (Placing placing : placings) {
			String result = onServer(() -> {
				WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
				ModernTemplate template = ModernTemplate.get("end_city/" + placing.name);
				ModernTemplate.Settings settings = new ModernTemplate.Settings().rotation(placing.rotation);
				ModernBox box = template.boundingBox(settings, placing.x, placing.y, placing.z);
				List<String> bad = new ArrayList<>();
				if (!box.toString().equals(placing.box)) {
					bad.add("box " + box + ", 26.3's " + placing.box);
				}
				List<ModernTemplate.Marker> markers = template.place(world, placing.x, placing.y, placing.z, settings, new Random(1L));
				List<String> markerLines = new ArrayList<>();
				for (ModernTemplate.Marker marker : markers) {
					markerLines.add((marker.x - placing.x) + " " + (marker.y - placing.y) + " " + (marker.z - placing.z) + " " + marker.metadata);
				}
				if (!markerLines.equals(placing.markers)) {
					bad.add("markers " + markerLines + ", 26.3's " + placing.markers);
				}
				int blocks = 0;
				for (int x = box.minX; x <= box.maxX; x++) {
					for (int y = box.minY; y <= box.maxY; y++) {
						for (int z = box.minZ; z <= box.maxZ; z++) {
							String key = (x - placing.x) + " " + (y - placing.y) + " " + (z - placing.z);
							String line = placing.blocks.get(key);
							String got = world.getBlock(x, y, z) == Blocks.air ? "air" : found(world, x, y, z);
							String want = line == null ? "air" : wanted(line);
							blocks++;
							if (!got.equals(want) && bad.size() < 4) {
								bad.add("(" + key + ") " + got + ", 26.3's " + (line == null ? "air" : line) + " = " + want);
							}
						}
					}
				}
				return blocks + "|" + String.join("; ", bad);
			});
			int split = result.indexOf('|');
			counts[0]++;
			counts[1] += Integer.parseInt(result.substring(0, split));
			if (split < result.length() - 1 && wrong.size() < 12) {
				wrong.add(placing.name + " " + placing.rotation + ": " + result.substring(split + 1));
			}
		}
		assertEqual("[]", wrong.toString(), "the templates (" + counts[0] + " placings, " + counts[1] + " places)");
	}

	/** The table against 1.7.10's own metadata for the end city's states. */
	public void testStatesAs1_7_10() {
		String[][] cases = {
			{"minecraft:purpur_stairs[facing=east,half=bottom,shape=straight]", "0"},
			{"minecraft:purpur_stairs[facing=north,half=top,shape=straight]", "7"},
			{"minecraft:purpur_pillar[axis=x]", "4"},
			{"minecraft:purpur_pillar[axis=z]", "8"},
			{"minecraft:purpur_slab[type=top]", "8"},
			{"minecraft:end_rod[facing=up]", "1"},
			{"minecraft:end_rod[facing=west]", "4"},
			{"minecraft:ladder[facing=south]", "3"},
			{"minecraft:chest[facing=east,type=single]", "5"},
			{"minecraft:magenta_stained_glass", "2"},
			{"minecraft:magenta_wall_banner[facing=west]", "4"},
			{"minecraft:dragon_wall_head[facing=north,powered=false]", "2"},
		};
		List<String> wrong = new ArrayList<>();
		for (String[] c : cases) {
			ModernBlockStates.Placed placed = ModernBlockStates.convert(parse(c[0]));
			if (placed == null || placed.meta != Integer.parseInt(c[1])) {
				wrong.add(c[0] + " -> " + (placed == null ? "none" : placed.meta) + ", want " + c[1]);
			}
		}
		assertEqual("[]", wrong.toString(), "26.3's states as 1.7.10's metadata");
	}
}
