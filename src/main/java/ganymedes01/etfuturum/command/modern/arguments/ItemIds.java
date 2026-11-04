package ganymedes01.etfuturum.command.modern.arguments;

import io.github.asgerjon.mcfly.McFly;
import com.github.bsideup.jabel.Desugar;
import ganymedes01.etfuturum.blocks.ISubBlocksBlock;
import ganymedes01.etfuturum.recipes.ModRecipes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Item ids for commands (spec §45.7, §48): modern's ids, each naming one 1.7.10 item and damage (`red_wool` is wool
 * with damage 14). Vanilla's come from the flattening table, spawn eggs from the mob in the egg's damage, EFR's from
 * the names EFR gives its items (mostly modern's), other mods' from their registry names in lower case. 1.7.10's own
 * names that modern renamed (`wool`, `reeds`) aren't ids; the 1.7.10 form of a command still takes them. Built on
 * first use, when every mod has registered its items.
 */
public final class ItemIds {

	private static final String EFR_PREFIX = McFly.DOMAIN + ":";
	private static final String MINECRAFT = "minecraft:";

	/** An id's item, the damage that makes it the id's variant, and the data that does (null for none). */
	@Desugar
	public record Entry(Item item, int meta, NBTTagCompound tag) {

		public ItemStack createStack(int count) {
			ItemStack stack = new ItemStack(item, count, meta);
			if (tag != null) {
				stack.setTagCompound((NBTTagCompound) tag.copy());
			}
			return stack;
		}
	}

	/** Set once built, so a thread that sees it sees it whole. */
	private static volatile Map<String, Entry> byId;

	private ItemIds() {
	}

	public static Entry byId(String id) {
		build();
		return byId.get(id);
	}

	/** Every id, sorted. */
	public static Collection<String> ids() {
		build();
		return Collections.unmodifiableCollection(byId.keySet());
	}

	private static synchronized void build() {
		if (byId != null) {
			return;
		}
		Map<String, Entry> ids = new TreeMap<>();
		for (String line : LegacyIdTable.lines("items")) {
			String[] row = line.split(" ");
			Item item = (Item) Item.itemRegistry.getObject(row[1].indexOf(':') < 0 ? MINECRAFT + row[1] : row[1]);
			if (item != null) {
				ids.putIfAbsent(MINECRAFT + row[0], new Entry(item, Integer.parseInt(row[2]), null));
			}
		}
		Set<String> modern = LegacyIdTable.words("modern_items");
		addSpawnEggs(ids, modern);
		Map<String, String> efrNames = LegacyIdTable.pairs("efr");
		for (Object key : Item.itemRegistry.getKeys()) {
			String name = (String) key;
			Item item = (Item) Item.itemRegistry.getObject(name);
			if (item == null) {
				continue;
			}
			if (name.startsWith(EFR_PREFIX)) {
				addEfr(ids, item, name, modern, efrNames);
			} else if (name.indexOf(':') > 0 && !name.startsWith(MINECRAFT)) {
				ids.putIfAbsent(name.toLowerCase(Locale.ROOT), new Entry(item, 0, null));
			}
		}
		byId = ids;
	}

	/** 1.7.10 has one egg item with the mob in its damage; modern has an egg item per mob. */
	private static void addSpawnEggs(Map<String, Entry> ids, Set<String> modern) {
		for (Object key : new TreeSet<Object>(EntityList.entityEggs.keySet())) {
			int mob = (Integer) key;
			String type = EntityTypeIds.idOfName(EntityList.getStringFromID(mob));
			if (type != null && type.startsWith(MINECRAFT)) {
				String egg = type.substring(MINECRAFT.length()) + "_spawn_egg";
				if (modern.contains(egg)) {
					ids.putIfAbsent(MINECRAFT + egg, new Entry(Items.spawn_egg, mob, null));
				}
			}
		}
	}

	/**
	 * EFR's item by the names it shows (`tile.etfuturum.polished_granite`): modern's id where modern has that name,
	 * `etfuturum:` and the name where it doesn't, the registry name when EFR's name isn't one at all. A block that lists
	 * its variants by name is read from its list first, so a name goes to its own damage rather than to an unused damage
	 * that shows the same name. Shulker boxes keep their colour in data. A double slab shows its slab's name and modern
	 * has no such item, so it keeps only its registry name.
	 */
	private static void addEfr(Map<String, Entry> ids, Item item, String registryName, Set<String> modern, Map<String, String> efrNames) {
		Block block = Block.getBlockFromItem(item);
		if (block instanceof BlockSlab && block.isOpaqueCube()) {
			ids.putIfAbsent(registryName.toLowerCase(Locale.ROOT), new Entry(item, 0, null));
			return;
		}
		String[] types = block instanceof ISubBlocksBlock sub ? sub.getTypes() : null;
		int metas = item.getHasSubtypes() ? 16 : 1;
		boolean named = false;
		for (int pass = 0; pass < 2; pass++) {
			for (int meta = 0; meta < metas; meta++) {
				if (pass == 0 && (types == null || meta >= types.length || types[meta].isEmpty())) {
					continue;
				}
				named |= addEfrName(ids, new ItemStack(item, 1, meta), modern, efrNames);
			}
		}
		if (item instanceof io.github.asgerjon.mcfly.shulkerbox.ShulkerBoxItem) {
			for (int color = 1; color <= ModRecipes.dye_names.length; color++) {
				ItemStack stack = new ItemStack(item);
				stack.setTagCompound(new NBTTagCompound());
				stack.getTagCompound().setByte("Color", (byte) color);
				addEfrName(ids, stack, modern, efrNames);
			}
		}
		if (!named) {
			ids.putIfAbsent(registryName.toLowerCase(Locale.ROOT), new Entry(item, 0, null));
		}
	}

	/** Adds the stack under its EFR name unless the id is taken; false when the stack has no usable name. */
	private static boolean addEfrName(Map<String, Entry> ids, ItemStack stack, Set<String> modern, Map<String, String> efrNames) {
		String name;
		try {
			name = stack.getItem().getUnlocalizedName(stack);
		} catch (RuntimeException e) {
			// a damage the item has no variant for
			return false;
		}
		if (name == null) {
			return false;
		}
		for (String prefix : new String[]{"tile.", "item."}) {
			if (name.startsWith(prefix)) {
				name = name.substring(prefix.length());
			}
		}
		if (!name.startsWith(McFly.DOMAIN + ".")) {
			return false;
		}
		name = name.substring(McFly.DOMAIN.length() + 1);
		if (!isPlainName(name)) {
			return false;
		}
		String renamed = efrNames.get(name);
		if (renamed != null) {
			name = renamed;
		}
		String id = (modern.contains(name) ? MINECRAFT : EFR_PREFIX) + name;
		NBTTagCompound tag = stack.getTagCompound();
		ids.putIfAbsent(id, new Entry(stack.getItem(), stack.getItemDamage(), tag == null ? null : (NBTTagCompound) tag.copy()));
		return true;
	}

	private static boolean isPlainName(String name) {
		if (name.isEmpty()) {
			return false;
		}
		for (char c : name.toCharArray()) {
			if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_')) {
				return false;
			}
		}
		return true;
	}
}
