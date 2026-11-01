package ganymedes01.etfuturum.command.modern;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import cpw.mods.fml.common.network.ByteBufUtils;
import ganymedes01.etfuturum.command.modern.arguments.EffectArgument;
import ganymedes01.etfuturum.command.modern.arguments.EnchantmentArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntityArgument;
import ganymedes01.etfuturum.command.modern.arguments.BlockPosArgument;
import ganymedes01.etfuturum.command.modern.arguments.BlockPredicateArgument;
import ganymedes01.etfuturum.command.modern.arguments.BlockStateArgument;
import ganymedes01.etfuturum.command.modern.arguments.CompoundTagArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntityTypeArgument;
import ganymedes01.etfuturum.command.modern.arguments.ItemArgument;
import ganymedes01.etfuturum.command.modern.arguments.Vec3Argument;
import ganymedes01.etfuturum.command.modern.arguments.ItemPredicateArgument;
import ganymedes01.etfuturum.command.modern.arguments.ComponentArgument;
import ganymedes01.etfuturum.command.modern.arguments.MessageArgument;
import ganymedes01.etfuturum.command.modern.arguments.GameModeArgument;
import ganymedes01.etfuturum.command.modern.arguments.RotationArgument;
import ganymedes01.etfuturum.command.modern.arguments.TimeArgument;
import ganymedes01.etfuturum.lib.Reference;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * The argument types the command tree can carry to the client (spec §40.1): each by a name and its settings. The
 * settings go length-prefixed, so a client that doesn't know a type can skip it; it then reads the argument as a
 * greedy string, which accepts anything and leaves the checking to the server.
 */
public final class ArgumentTypes {

	private static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);
	private static final Map<Class<?>, Serializer<?>> BY_CLASS = new HashMap<>();
	private static final Map<String, Serializer<?>> BY_NAME = new HashMap<>();

	private static final int HAS_MIN = 1;
	private static final int HAS_MAX = 2;

	static {
		register("brigadier:bool", BoolArgumentType.class, new Serializer<BoolArgumentType>() {
			@Override
			public void write(BoolArgumentType type, ByteBuf buf) {
			}

			@Override
			public BoolArgumentType read(ByteBuf buf) {
				return BoolArgumentType.bool();
			}
		});
		register("brigadier:integer", IntegerArgumentType.class, new Serializer<IntegerArgumentType>() {
			@Override
			public void write(IntegerArgumentType type, ByteBuf buf) {
				boolean min = type.getMinimum() != Integer.MIN_VALUE, max = type.getMaximum() != Integer.MAX_VALUE;
				buf.writeByte((min ? HAS_MIN : 0) | (max ? HAS_MAX : 0));
				if (min) buf.writeInt(type.getMinimum());
				if (max) buf.writeInt(type.getMaximum());
			}

			@Override
			public IntegerArgumentType read(ByteBuf buf) {
				int flags = buf.readByte();
				int min = (flags & HAS_MIN) != 0 ? buf.readInt() : Integer.MIN_VALUE;
				int max = (flags & HAS_MAX) != 0 ? buf.readInt() : Integer.MAX_VALUE;
				return IntegerArgumentType.integer(min, max);
			}
		});
		register("brigadier:long", LongArgumentType.class, new Serializer<LongArgumentType>() {
			@Override
			public void write(LongArgumentType type, ByteBuf buf) {
				boolean min = type.getMinimum() != Long.MIN_VALUE, max = type.getMaximum() != Long.MAX_VALUE;
				buf.writeByte((min ? HAS_MIN : 0) | (max ? HAS_MAX : 0));
				if (min) buf.writeLong(type.getMinimum());
				if (max) buf.writeLong(type.getMaximum());
			}

			@Override
			public LongArgumentType read(ByteBuf buf) {
				int flags = buf.readByte();
				long min = (flags & HAS_MIN) != 0 ? buf.readLong() : Long.MIN_VALUE;
				long max = (flags & HAS_MAX) != 0 ? buf.readLong() : Long.MAX_VALUE;
				return LongArgumentType.longArg(min, max);
			}
		});
		register("brigadier:float", FloatArgumentType.class, new Serializer<FloatArgumentType>() {
			@Override
			public void write(FloatArgumentType type, ByteBuf buf) {
				boolean min = type.getMinimum() != -Float.MAX_VALUE, max = type.getMaximum() != Float.MAX_VALUE;
				buf.writeByte((min ? HAS_MIN : 0) | (max ? HAS_MAX : 0));
				if (min) buf.writeFloat(type.getMinimum());
				if (max) buf.writeFloat(type.getMaximum());
			}

			@Override
			public FloatArgumentType read(ByteBuf buf) {
				int flags = buf.readByte();
				float min = (flags & HAS_MIN) != 0 ? buf.readFloat() : -Float.MAX_VALUE;
				float max = (flags & HAS_MAX) != 0 ? buf.readFloat() : Float.MAX_VALUE;
				return FloatArgumentType.floatArg(min, max);
			}
		});
		register("brigadier:double", DoubleArgumentType.class, new Serializer<DoubleArgumentType>() {
			@Override
			public void write(DoubleArgumentType type, ByteBuf buf) {
				boolean min = type.getMinimum() != -Double.MAX_VALUE, max = type.getMaximum() != Double.MAX_VALUE;
				buf.writeByte((min ? HAS_MIN : 0) | (max ? HAS_MAX : 0));
				if (min) buf.writeDouble(type.getMinimum());
				if (max) buf.writeDouble(type.getMaximum());
			}

			@Override
			public DoubleArgumentType read(ByteBuf buf) {
				int flags = buf.readByte();
				double min = (flags & HAS_MIN) != 0 ? buf.readDouble() : -Double.MAX_VALUE;
				double max = (flags & HAS_MAX) != 0 ? buf.readDouble() : Double.MAX_VALUE;
				return DoubleArgumentType.doubleArg(min, max);
			}
		});
		register("brigadier:string", StringArgumentType.class, new Serializer<StringArgumentType>() {
			@Override
			public void write(StringArgumentType type, ByteBuf buf) {
				buf.writeByte(type.getType().ordinal());
			}

			@Override
			public StringArgumentType read(ByteBuf buf) {
				return switch (StringArgumentType.StringType.values()[buf.readByte()]) {
					case SINGLE_WORD -> StringArgumentType.word();
					case QUOTABLE_PHRASE -> StringArgumentType.string();
					case GREEDY_PHRASE -> StringArgumentType.greedyString();
				};
			}
		});
		register("minecraft:entity", EntityArgument.class, new Serializer<EntityArgument>() {
			@Override
			public void write(EntityArgument type, ByteBuf buf) {
				buf.writeByte((type.single() ? 1 : 0) | (type.playersOnly() ? 2 : 0));
			}

			@Override
			public EntityArgument read(ByteBuf buf) {
				int flags = buf.readByte();
				return EntityArgument.of((flags & 1) != 0, (flags & 2) != 0);
			}
		});
		register("minecraft:mob_effect", EffectArgument.class, new Serializer<EffectArgument>() {
			@Override
			public void write(EffectArgument type, ByteBuf buf) {
			}

			@Override
			public EffectArgument read(ByteBuf buf) {
				return EffectArgument.effect();
			}
		});
		register("minecraft:enchantment", EnchantmentArgument.class, new Serializer<EnchantmentArgument>() {
			@Override
			public void write(EnchantmentArgument type, ByteBuf buf) {
			}

			@Override
			public EnchantmentArgument read(ByteBuf buf) {
				return EnchantmentArgument.enchantment();
			}
		});
		register("minecraft:item_stack", ItemArgument.class, new Serializer<ItemArgument>() {
			@Override
			public void write(ItemArgument type, ByteBuf buf) {
			}

			@Override
			public ItemArgument read(ByteBuf buf) {
				return ItemArgument.item();
			}
		});
		register("minecraft:vec3", Vec3Argument.class, new Serializer<Vec3Argument>() {
			@Override
			public void write(Vec3Argument type, ByteBuf buf) {
				buf.writeBoolean(type.centreCorrect());
			}

			@Override
			public Vec3Argument read(ByteBuf buf) {
				return Vec3Argument.vec3(buf.readBoolean());
			}
		});
		register("minecraft:entity_type", EntityTypeArgument.class, new Serializer<EntityTypeArgument>() {
			@Override
			public void write(EntityTypeArgument type, ByteBuf buf) {
			}

			@Override
			public EntityTypeArgument read(ByteBuf buf) {
				return EntityTypeArgument.summonable();
			}
		});
		register("minecraft:block_pos", BlockPosArgument.class, new Serializer<BlockPosArgument>() {
			@Override
			public void write(BlockPosArgument type, ByteBuf buf) {
			}

			@Override
			public BlockPosArgument read(ByteBuf buf) {
				return BlockPosArgument.blockPos();
			}
		});
		register("minecraft:block_state", BlockStateArgument.class, new Serializer<BlockStateArgument>() {
			@Override
			public void write(BlockStateArgument type, ByteBuf buf) {
			}

			@Override
			public BlockStateArgument read(ByteBuf buf) {
				return BlockStateArgument.block();
			}
		});
		register("minecraft:block_predicate", BlockPredicateArgument.class, new Serializer<BlockPredicateArgument>() {
			@Override
			public void write(BlockPredicateArgument type, ByteBuf buf) {
			}

			@Override
			public BlockPredicateArgument read(ByteBuf buf) {
				return BlockPredicateArgument.blockPredicate();
			}
		});
		register("minecraft:time", TimeArgument.class, new Serializer<TimeArgument>() {
			@Override
			public void write(TimeArgument type, ByteBuf buf) {
				buf.writeInt(type.minimum());
			}

			@Override
			public TimeArgument read(ByteBuf buf) {
				return TimeArgument.time(buf.readInt());
			}
		});
		register("minecraft:rotation", RotationArgument.class, unit(RotationArgument::rotation));
		register("minecraft:gamemode", GameModeArgument.class, unit(GameModeArgument::gameMode));
		register("minecraft:message", MessageArgument.class, unit(MessageArgument::message));
		register("minecraft:component", ComponentArgument.class, unit(ComponentArgument::textComponent));
		register("minecraft:item_predicate", ItemPredicateArgument.class, unit(ItemPredicateArgument::itemPredicate));
		register("minecraft:nbt_compound_tag", CompoundTagArgument.class, new Serializer<CompoundTagArgument>() {
			@Override
			public void write(CompoundTagArgument type, ByteBuf buf) {
			}

			@Override
			public CompoundTagArgument read(ByteBuf buf) {
				return CompoundTagArgument.compoundTag();
			}
		});
	}

	private ArgumentTypes() {
	}

	/** For an argument type with nothing to send: the client makes a new one. */
	private static <T extends ArgumentType<?>> Serializer<T> unit(java.util.function.Supplier<T> make) {
		return new Serializer<T>() {
			@Override
			public void write(T type, ByteBuf buf) {
			}

			@Override
			public T read(ByteBuf buf) {
				return make.get();
			}
		};
	}

	public static <T extends ArgumentType<?>> void register(String name, Class<T> type, Serializer<T> serializer) {
		BY_CLASS.put(type, serializer);
		BY_NAME.put(name, serializer);
		serializer.name = name;
	}

	/** The type's name and its settings, length-prefixed. A type without a serializer goes as a greedy string. */
	@SuppressWarnings("unchecked")
	public static void write(ArgumentType<?> type, ByteBuf buf) {
		Serializer<ArgumentType<?>> serializer = (Serializer<ArgumentType<?>>) BY_CLASS.get(type.getClass());
		if (serializer == null) {
			LOGGER.warn("No network form for the command argument type {}; the client reads it as a greedy string", type.getClass().getName());
			type = StringArgumentType.greedyString();
			serializer = (Serializer<ArgumentType<?>>) BY_CLASS.get(StringArgumentType.class);
		}
		ByteBuf settings = Unpooled.buffer();
		serializer.write(type, settings);
		ByteBufUtils.writeUTF8String(buf, serializer.name);
		ByteBufUtils.writeVarInt(buf, settings.readableBytes(), 5);
		buf.writeBytes(settings);
	}

	public static ArgumentType<?> read(ByteBuf buf) {
		String name = ByteBufUtils.readUTF8String(buf);
		ByteBuf settings = buf.readBytes(ByteBufUtils.readVarInt(buf, 5));
		Serializer<?> serializer = BY_NAME.get(name);
		return serializer == null ? StringArgumentType.greedyString() : serializer.read(settings);
	}

	public abstract static class Serializer<T extends ArgumentType<?>> {
		private String name;

		public abstract void write(T type, ByteBuf buf);

		public abstract T read(ByteBuf buf);
	}
}
