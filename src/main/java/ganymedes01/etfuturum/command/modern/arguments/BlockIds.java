package ganymedes01.etfuturum.command.modern.arguments;

import com.github.bsideup.jabel.Desugar;
import ganymedes01.etfuturum.blocks.BaseSlab;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockPressurePlateWeighted;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Block ids for commands (spec §49.3): each modern block with its properties, and the 1.7.10 block and meta for each
 * state 1.7.10 can show. Vanilla's from the table's 1.12 rows; EFR's by the names they show, with vanilla's rows for
 * EFR's blocks of vanilla shapes (stairs, slabs, doors, ...), EFR's others' own metas where known (EfrBlockStates);
 * tags from 26.3's. Built on first use, when every mod has
 * registered its blocks.
 */
public final class BlockIds {

	private static final String MINECRAFT = "minecraft:";
	private static final String EFR_PREFIX = Reference.MOD_ID + ":";

	/** A 1.7.10 block and meta, and the modern properties it stands for. */
	@Desugar
	public record State(Block block, int meta, Map<String, String> properties) {
	}

	/** What 1.7.10 has at a place, as modern sees it. */
	@Desugar
	public record Found(Type type, State state) {
	}

	/** A modern block. */
	public static final class Type {
		public final String id;
		/** Every property modern gives the block, with its values in order. */
		public final Map<String, List<String>> properties;
		private final List<State> states = new ArrayList<>();
		/** The properties its states differ in: the ones 1.7.10 keeps in the meta. */
		private final Set<String> stored = new HashSet<>();
		/** 26.3's own defaults where known (EFR's blocks, §64.3), before the inferred ones. */
		private Map<String, String> ownDefaults = Collections.emptyMap();

		private Type(String id, Map<String, List<String>> properties) {
			this.id = id;
			this.properties = properties;
		}

		private void add(State state) {
			for (State other : states) {
				for (Map.Entry<String, String> property : state.properties.entrySet()) {
					if (!property.getValue().equals(other.properties.get(property.getKey()))) {
						stored.add(property.getKey());
					}
				}
			}
			states.add(state);
		}

		/** Whether 1.7.10 keeps the property in the meta (else it works it out itself, or has no such thing). */
		public boolean isStored(String property) {
			return stored.contains(property);
		}

		/**
		 * The state for the given properties: the one that has the most of them, then the most of the others at their
		 * defaults, then the lowest meta.
		 */
		public State state(Map<String, String> given) {
			State best = null;
			int bestScore = -1;
			for (State state : states) {
				int score = 0;
				for (String property : stored) {
					String value = state.properties.get(property);
					if (given.containsKey(property)) {
						score += given.get(property).equals(value) ? 1000 : 0;
					} else if (defaultValue(property).equals(value)) {
						score++;
					}
				}
				if (score > bestScore) {
					bestScore = score;
					best = state;
				}
			}
			return best;
		}

		public State defaultState() {
			return state(Collections.emptyMap());
		}

		public boolean hasBlockEntity() {
			State state = defaultState();
			return state.block.hasTileEntity(state.meta);
		}

		/** The default of one of its properties [inferred; 26.3's data has no defaults], spec §49.3. */
		String defaultValue(String property) {
			if (ownDefaults.containsKey(property)) {
				return ownDefaults.get(property);
			}
			List<String> values = properties.containsKey(property) ? properties.get(property) : Collections.emptyList();
			switch (property) {
				case "facing":
					return id.equals("minecraft:hopper") ? "down" : "north";
				case "half":
					return values.contains("lower") ? "lower" : "bottom";
				case "shape":
					return values.contains("straight") ? "straight" : "north_south";
				case "axis":
					return "y";
				case "type":
					return values.contains("bottom") ? "bottom" : "single";
				case "hinge":
					return "left";
				case "part":
					return "foot";
				case "face":
					return "wall";
				case "distance":
					return "7";
				case "lit":
					return id.endsWith("redstone_torch") || id.endsWith("redstone_wall_torch") ? "true" : "false";
				default:
					break;
			}
			if (values.isEmpty()) {
				return "";
			}
			if (values.contains("false")) {
				return "false";
			}
			if (values.contains("none")) {
				return "none";
			}
			// numbers come in order, so the first is the lowest
			return values.get(0);
		}

		private int mostCommonBlockLowestMeta(Block block) {
			int lowest = Integer.MAX_VALUE;
			for (State state : states) {
				if (state.block == block) {
					lowest = Math.min(lowest, state.meta);
				}
			}
			return lowest;
		}

		private Block mostCommonBlock() {
			Map<Block, Integer> counts = new IdentityHashMap<>();
			Block most = null;
			for (State state : states) {
				int count = counts.merge(state.block, 1, Integer::sum);
				if (most == null || count > counts.get(most)) {
					most = state.block;
				}
			}
			return most;
		}
	}

	/** Set last, so a thread that sees it sees the rest too. */
	private static volatile Map<String, Type> byId;
	private static Map<Block, Found[]> byBlock;
	private static Map<String, List<Type>> tags;

	private BlockIds() {
	}

	public static Type byId(String id) {
		build();
		return byId.get(id);
	}

	/** Every id, sorted. */
	public static Collection<String> ids() {
		build();
		return Collections.unmodifiableCollection(byId.keySet());
	}

	/** Every tag id (`minecraft:logs`), sorted. */
	public static Collection<String> tagIds() {
		build();
		return Collections.unmodifiableCollection(tags.keySet());
	}

	/** A tag's blocks, or null for a tag that doesn't exist. */
	public static List<Type> tag(String id) {
		build();
		return tags.get(id);
	}

	/** The modern block and state of a 1.7.10 block and meta, or null for one that has none. */
	public static Found find(Block block, int meta) {
		build();
		Found[] found = byBlock.get(block);
		if (found == null) {
			return null;
		}
		return found[meta & 15] != null ? found[meta & 15] : found[16];
	}

	private static synchronized void build() {
		if (byId != null) {
			return;
		}
		Map<String, Map<String, List<String>>> properties = new HashMap<>();
		for (String line : LegacyIdTable.lines("block_properties")) {
			String[] words = line.split(" ");
			Map<String, List<String>> values = new LinkedHashMap<>();
			for (int i = 1; i < words.length; i++) {
				int equals = words[i].indexOf('=');
				values.put(words[i].substring(0, equals), Arrays.asList(words[i].substring(equals + 1).split("\\|")));
			}
			properties.put(words[0], values);
		}
		Map<String, Type> types = new TreeMap<>();
		Map<Block, Found[]> reverse = new IdentityHashMap<>();
		for (String line : LegacyIdTable.lines("blocks")) {
			String[] row = line.split(" ");
			if (!Block.blockRegistry.containsKey(MINECRAFT + row[0])) {
				continue;
			}
			Block block = (Block) Block.blockRegistry.getObject(MINECRAFT + row[0]);
			String state = row[2];
			int bracket = state.indexOf('[');
			String name = bracket < 0 ? state : state.substring(0, bracket);
			Map<String, String> values = new HashMap<>();
			if (bracket >= 0) {
				for (String pair : state.substring(bracket + 1, state.length() - 1).split(",")) {
					int equals = pair.indexOf('=');
					values.put(pair.substring(0, equals), pair.substring(equals + 1));
				}
			}
			Type type = types.computeIfAbsent(MINECRAFT + name, id -> new Type(id, propertiesOf(properties, name)));
			add(reverse, type, new State(block, Integer.parseInt(row[1]), values));
		}
		addEfr(types, reverse, properties);
		Map<String, List<Type>> tagMap = new TreeMap<>();
		Map<String, List<String>> tagLines = new HashMap<>();
		for (String line : LegacyIdTable.lines("block_tags")) {
			String[] words = line.split(" ");
			tagLines.put(words[0], Arrays.asList(words).subList(1, words.length));
		}
		for (String tag : tagLines.keySet()) {
			List<Type> members = new ArrayList<>();
			for (String member : resolveTag(tagLines, tag, new HashSet<>())) {
				Type type = types.get(MINECRAFT + member);
				if (type != null && !members.contains(type)) {
					members.add(type);
				}
			}
			if (!members.isEmpty()) {
				tagMap.put(MINECRAFT + tag, members);
			}
		}
		tags = tagMap;
		byBlock = reverse;
		byId = types;
	}

	private static Map<String, List<String>> propertiesOf(Map<String, Map<String, List<String>>> properties, String name) {
		Map<String, List<String>> values = properties.get(name);
		return values == null ? Collections.emptyMap() : values;
	}

	/** The state, and the first modern state for its block and meta (index 16: the block's first state, for a meta no row has). */
	private static void add(Map<Block, Found[]> reverse, Type type, State state) {
		type.add(state);
		Found[] found = reverse.computeIfAbsent(state.block, block -> new Found[17]);
		if (found[state.meta & 15] == null) {
			found[state.meta & 15] = new Found(type, state);
		}
		if (found[16] == null) {
			found[16] = new Found(type, state);
		}
	}

	private static List<String> resolveTag(Map<String, List<String>> tagLines, String tag, Set<String> visiting) {
		List<String> members = new ArrayList<>();
		if (!visiting.add(tag) || !tagLines.containsKey(tag)) {
			return members;
		}
		for (String value : tagLines.get(tag)) {
			if (value.startsWith("#")) {
				members.addAll(resolveTag(tagLines, value.substring(1), visiting));
			} else {
				members.add(value);
			}
		}
		return members;
	}

	/**
	 * EFR's blocks under the ids its items have (`granite` is EFR's stone 1), then EFR's blocks without an item by
	 * their own names; vanilla's ids come first. A block of a vanilla shape takes the vanilla block's states, its
	 * property bits added to the variant's meta. A double slab is its slab's `type=double`, not a block of its own.
	 */
	private static void addEfr(Map<String, Type> types, Map<Block, Found[]> reverse, Map<String, Map<String, List<String>>> properties) {
		Set<String> modern = LegacyIdTable.words("modern_blocks");
		for (String id : ItemIds.ids()) {
			ItemIds.Entry entry = ItemIds.byId(id);
			if (entry.tag() != null || !(entry.item() instanceof ItemBlock itemBlock)
				|| !String.valueOf(Item.itemRegistry.getNameForObject(entry.item())).startsWith(EFR_PREFIX)
				|| itemBlock.field_150939_a instanceof BlockSlab && itemBlock.field_150939_a.isOpaqueCube()) {
				continue;
			}
			String name = id.substring(id.indexOf(':') + 1);
			addEfrBlock(types, reverse, properties, (modern.contains(name) ? MINECRAFT : EFR_PREFIX) + name, name,
				itemBlock.field_150939_a, entry.item().getMetadata(entry.meta()));
		}
		Map<String, String> efrNames = LegacyIdTable.pairs("efr");
		for (Object key : Block.blockRegistry.getKeys()) {
			String registryName = (String) key;
			Block block = (Block) Block.blockRegistry.getObject(registryName);
			if (!registryName.startsWith(EFR_PREFIX) || Item.getItemFromBlock(block) != null || reverse.containsKey(block)) {
				continue;
			}
			String name = block.getUnlocalizedName();
			if (name == null || !name.startsWith("tile." + Reference.MOD_ID + ".")) {
				continue;
			}
			name = name.substring(("tile." + Reference.MOD_ID + ".").length());
			if (!name.matches("[a-z0-9_]+")) {
				continue;
			}
			name = efrNames.getOrDefault(name, name);
			addEfrBlock(types, reverse, properties, (modern.contains(name) ? MINECRAFT : EFR_PREFIX) + name, name, block, 0);
		}
	}

	private static void addEfrBlock(Map<String, Type> types, Map<Block, Found[]> reverse, Map<String, Map<String, List<String>>> properties,
									String id, String name, Block block, int meta) {
		if (types.containsKey(id)) {
			return;
		}
		String shape = vanillaShape(block);
		Type pattern = shape == null ? null : types.get(MINECRAFT + shape);
		Map<String, List<String>> values = propertiesOf(properties, name);
		Type type = new Type(id, values.isEmpty() && pattern != null ? pattern.properties : values);
		types.put(id, type);
		if (pattern == null) {
			// its own metas' properties where known (§64.3), else its one state
			EfrBlockStates.Spec spec = EfrBlockStates.of(block);
			if (spec == null) {
				add(reverse, type, new State(block, meta, Collections.emptyMap()));
				return;
			}
			type.ownDefaults = spec.defaults();
			for (EfrBlockStates.Meta state : spec.metas()) {
				add(reverse, type, new State(block, meta + state.offset(), state.properties()));
			}
			return;
		}
		Block patternBlock = pattern.mostCommonBlock();
		int base = pattern.mostCommonBlockLowestMeta(patternBlock);
		// EFR's double slab is a block of its own, as 1.7.10's are, with the slab's variant as its meta
		Block doubleSlab = block instanceof BaseSlab slab && slab.getDoubleSlab() != block ? slab.getDoubleSlab() : null;
		for (State state : pattern.states) {
			if (state.block == patternBlock) {
				add(reverse, type, new State(block, meta | (state.meta - base), state.properties));
			} else if (doubleSlab != null && "double".equals(state.properties.get("type"))) {
				add(reverse, type, new State(doubleSlab, meta, state.properties));
			}
		}
	}

	/** The vanilla block whose states an EFR block of the same kind has. */
	private static String vanillaShape(Block block) {
		if (block instanceof BlockStairs) {
			return "oak_stairs";
		}
		if (block instanceof BlockSlab && !block.isOpaqueCube()) {
			return "oak_slab";
		}
		if (block instanceof BlockDoor) {
			return "oak_door";
		}
		if (block instanceof BlockTrapDoor) {
			return "oak_trapdoor";
		}
		if (block instanceof BlockFenceGate) {
			return "oak_fence_gate";
		}
		if (block instanceof BlockButton) {
			return "oak_button";
		}
		if (block instanceof BlockPressurePlateWeighted) {
			return "light_weighted_pressure_plate";
		}
		if (block instanceof BlockPressurePlate) {
			return "oak_pressure_plate";
		}
		if (block instanceof BlockRotatedPillar) {
			return "oak_log";
		}
		if (block instanceof BlockLeaves) {
			return "oak_leaves";
		}
		if (block instanceof BlockLadder) {
			return "ladder";
		}
		return null;
	}
}
