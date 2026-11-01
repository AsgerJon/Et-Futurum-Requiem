package ganymedes01.etfuturum.command.modern.nbt;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;

import java.util.List;
import java.util.Map;

/** The plain values {@link SnbtReader} makes: as 1.7.10 NBT, and as text for messages (spec §45.6, §45.7). */
public final class Snbt {

	private static final int COMPOUND = 10;

	private Snbt() {
	}

	/** 1.7.10 NBT for a value. A list of mixed types is stored as modern stores it, its elements wrapped. */
	public static NBTBase toNbt(Object value) {
		if (value instanceof Byte b) {
			return new NBTTagByte(b);
		}
		if (value instanceof Short s) {
			return new NBTTagShort(s);
		}
		if (value instanceof Integer i) {
			return new NBTTagInt(i);
		}
		if (value instanceof Long l) {
			return new NBTTagLong(l);
		}
		if (value instanceof Float f) {
			return new NBTTagFloat(f);
		}
		if (value instanceof Double d) {
			return new NBTTagDouble(d);
		}
		if (value instanceof String s) {
			return new NBTTagString(s);
		}
		if (value instanceof byte[] bytes) {
			return new NBTTagByteArray(bytes.clone());
		}
		if (value instanceof int[] ints) {
			return new NBTTagIntArray(ints.clone());
		}
		if (value instanceof long[]) {
			throw new IllegalArgumentException("Long arrays don't exist in 1.7.10's NBT");
		}
		if (value instanceof Map<?, ?> map) {
			NBTTagCompound compound = new NBTTagCompound();
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				compound.setTag((String) entry.getKey(), toNbt(entry.getValue()));
			}
			return compound;
		}
		List<?> list = (List<?>) value;
		NBTTagList tags = new NBTTagList();
		int type = -1;
		boolean mixed = false;
		for (Object element : list) {
			int elementType = toNbt(element).getId();
			mixed |= type != -1 && type != elementType;
			type = elementType;
		}
		for (Object element : list) {
			NBTBase tag = toNbt(element);
			if (mixed && (tag.getId() != COMPOUND || isWrapper((NBTTagCompound) tag))) {
				NBTTagCompound wrapper = new NBTTagCompound();
				wrapper.setTag("", tag);
				tag = wrapper;
			}
			tags.appendTag(tag);
		}
		return tags;
	}

	private static boolean isWrapper(NBTTagCompound compound) {
		return compound.func_150296_c().size() == 1 && compound.hasKey("");
	}

	/** Modern's printed form, for messages: `1b`, `2s`, `3`, `4L`, `5.0f`, `6.0d`, `"text"`, `{a:1}`, `[1,2]`. */
	public static String toText(Object value) {
		if (value instanceof Byte) {
			return value + "b";
		}
		if (value instanceof Short) {
			return value + "s";
		}
		if (value instanceof Long) {
			return value + "L";
		}
		if (value instanceof Float) {
			return value + "f";
		}
		if (value instanceof Double) {
			return value + "d";
		}
		if (value instanceof String s) {
			return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
		}
		if (value instanceof byte[] bytes) {
			StringBuilder text = new StringBuilder("[B;");
			for (int i = 0; i < bytes.length; i++) {
				text.append(i > 0 ? "," : "").append(bytes[i]).append('B');
			}
			return text.append(']').toString();
		}
		if (value instanceof int[] ints) {
			StringBuilder text = new StringBuilder("[I;");
			for (int i = 0; i < ints.length; i++) {
				text.append(i > 0 ? "," : "").append(ints[i]);
			}
			return text.append(']').toString();
		}
		if (value instanceof long[] longs) {
			StringBuilder text = new StringBuilder("[L;");
			for (int i = 0; i < longs.length; i++) {
				text.append(i > 0 ? "," : "").append(longs[i]).append('L');
			}
			return text.append(']').toString();
		}
		if (value instanceof Map<?, ?> map) {
			StringBuilder text = new StringBuilder("{");
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				text.append(text.length() > 1 ? "," : "").append(entry.getKey()).append(':').append(toText(entry.getValue()));
			}
			return text.append('}').toString();
		}
		if (value instanceof List<?> list) {
			StringBuilder text = new StringBuilder("[");
			for (Object element : list) {
				text.append(text.length() > 1 ? "," : "").append(toText(element));
			}
			return text.append(']').toString();
		}
		return String.valueOf(value);
	}
}
