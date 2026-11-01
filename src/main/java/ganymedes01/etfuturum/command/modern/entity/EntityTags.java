package ganymedes01.etfuturum.command.modern.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.LinkedHashSet;
import java.util.Set;

/** Entity tags (spec §51.2): a list of words in the entity's Forge data, saved with it. */
public final class EntityTags {

	private static final String KEY = "Tags";
	/** Modern's limit per entity. */
	private static final int MAX = 1024;

	private EntityTags() {
	}

	public static Set<String> get(Entity entity) {
		Set<String> tags = new LinkedHashSet<>();
		NBTTagList list = entity.getEntityData().getTagList(KEY, 8);
		for (int i = 0; i < list.tagCount(); i++) {
			tags.add(list.getStringTagAt(i));
		}
		return tags;
	}

	/** False when it has the tag already, or has too many. */
	public static boolean add(Entity entity, String tag) {
		Set<String> tags = get(entity);
		if (tags.size() >= MAX || !tags.add(tag)) {
			return false;
		}
		save(entity, tags);
		return true;
	}

	/** False when it doesn't have the tag. */
	public static boolean remove(Entity entity, String tag) {
		Set<String> tags = get(entity);
		if (!tags.remove(tag)) {
			return false;
		}
		save(entity, tags);
		return true;
	}

	private static void save(Entity entity, Set<String> tags) {
		NBTTagList list = new NBTTagList();
		for (String tag : tags) {
			list.appendTag(new NBTTagString(tag));
		}
		entity.getEntityData().setTag(KEY, list);
	}
}
