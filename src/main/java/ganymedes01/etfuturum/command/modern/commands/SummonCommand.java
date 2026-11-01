package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.CompoundTagArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntityTypeArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntityTypeIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemData;
import ganymedes01.etfuturum.command.modern.arguments.Vec3Argument;
import ganymedes01.etfuturum.command.modern.entity.LegacyEntityData;
import ganymedes01.etfuturum.command.modern.item.DecodeException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.IMob;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Modern's /summon (spec §47): a modern entity type, at a position, with modern's data made into 1.7.10's. */
public final class SummonCommand extends ModernCommand {

	/** Modern's texts under EFR's keys where 1.7.10's own /summon uses the same key for its own message. */
	private static final SimpleCommandExceptionType FAILED = simple("commands.etfuturum.summon.failed");
	private static final SimpleCommandExceptionType FAILED_PEACEFUL = simple("commands.summon.failed.peaceful");
	private static final SimpleCommandExceptionType DUPLICATE_UUID = simple("commands.summon.failed.uuid");
	private static final SimpleCommandExceptionType INVALID_POSITION = simple("commands.summon.invalidPosition");
	private static final DynamicCommandExceptionType NOT_SUMMONABLE = new DynamicCommandExceptionType(
		type -> ComponentMessage.translatable("entity.not_summonable", type));
	/** EFR's own: modern leaves unreadable data out with a log line; here the command says what was wrong. */
	private static final Dynamic2CommandExceptionType BAD_DATA = new Dynamic2CommandExceptionType(
		(type, reason) -> ComponentMessage.translatable("commands.etfuturum.summon.badData", type, reason));

	public SummonCommand() {
		super("summon", 2);
	}

	private static SimpleCommandExceptionType simple(String key) {
		return new SimpleCommandExceptionType(ComponentMessage.translatable(key));
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("summon")
			.requires(requirement())
			.then(RequiredArgumentBuilder.<ModernCommandSource, String>argument("entity", EntityTypeArgument.summonable())
				.executes(c -> summon(c.getSource(), EntityTypeArgument.getEntityType(c, "entity"), c.getSource().position(), null))
				.then(RequiredArgumentBuilder.<ModernCommandSource, Vec3Argument.Coordinates>argument("pos", Vec3Argument.vec3())
					.executes(c -> summon(c.getSource(), EntityTypeArgument.getEntityType(c, "entity"), Vec3Argument.getVec3(c, "pos"), null))
					.then(RequiredArgumentBuilder.<ModernCommandSource, Map<String, Object>>argument("nbt", CompoundTagArgument.compoundTag())
						.executes(c -> summon(c.getSource(), EntityTypeArgument.getEntityType(c, "entity"), Vec3Argument.getVec3(c, "pos"),
							CompoundTagArgument.getCompoundTag(c, "nbt")))))));
	}

	/**
	 * 1.7.10's /summon needs one of its entity names first (`Skeleton`, `LightningBolt`, `modid.Name`); with anything
	 * else (`pig`) it could only say "Unable to summon object", so the modern error shows.
	 */
	@Override
	public boolean legacyCouldRun(String[] args) {
		return args.length > 0 && (EntityList.stringToClassMapping.containsKey(args[0]) || "LightningBolt".equals(args[0]));
	}

	/** Without data the mob gets its usual spawn treatment (a skeleton its bow); with data it doesn't, as in modern. */
	private int summon(ModernCommandSource source, String type, Vec3 pos, Map<String, Object> data) throws CommandSyntaxException {
		if (!EntityTypeIds.isSummonable(type)) {
			throw NOT_SUMMONABLE.create(type);
		}
		if (Math.abs(pos.xCoord) >= 3.0E7D || Math.abs(pos.zCoord) >= 3.0E7D || Math.abs(pos.yCoord) >= 2.0E7D) {
			throw INVALID_POSITION.create();
		}
		WorldServer world = source.world();
		Entity entity = create(world, type, pos, data);
		if (entity instanceof EntityLightningBolt lightning) {
			world.addWeatherEffect(lightning);
		} else {
			if (world.difficultySetting == EnumDifficulty.PEACEFUL && entity instanceof IMob) {
				throw FAILED_PEACEFUL.create();
			}
			if (data == null && entity instanceof EntityLiving living) {
				EntityTypeIds.giveSpawnTreatment(type, living);
			}
			if (data != null && data.containsKey("UUID") && isTaken(source, entity.getUniqueID())) {
				throw DUPLICATE_UUID.create();
			}
			if (!world.spawnEntityInWorld(entity)) {
				throw FAILED.create();
			}
			if (data != null) {
				addPassengers(world, entity, data, pos);
			}
		}
		source.sendSuccess(legacyView(), true, "commands.etfuturum.summon.success", entity.func_145748_c_());
		return 1;
	}

	/** The entity from its 1.7.10 name, the data translated, the variant's data on top, at the position. */
	private static Entity create(World world, String type, Vec3 pos, Map<String, Object> data) throws CommandSyntaxException {
		EntityTypeIds.Spawn spawn = EntityTypeIds.spawnFor(type, world.rand);
		if (spawn == null) {
			throw FAILED.create();
		}
		if (spawn.name == null) {
			return new EntityLightningBolt(world, pos.xCoord, pos.yCoord, pos.zCoord);
		}
		NBTTagCompound tag;
		try {
			tag = data == null ? new NBTTagCompound() : LegacyEntityData.convert(data, type);
		} catch (DecodeException e) {
			throw BAD_DATA.create(type, e.getMessage());
		}
		for (Object key : spawn.variant.func_150296_c()) {
			tag.setTag((String) key, spawn.variant.getTag((String) key).copy());
		}
		tag.setString("id", spawn.name);
		Entity entity = EntityList.createEntityFromNBT(tag, world);
		if (entity == null) {
			throw FAILED.create();
		}
		entity.setLocationAndAngles(pos.xCoord, pos.yCoord, pos.zCoord, entity.rotationYaw, entity.rotationPitch);
		spawn.after.accept(entity);
		return entity;
	}

	/** The first passenger rides it (1.7.10 carries one), and so on down its own passengers. */
	@SuppressWarnings("unchecked")
	private static void addPassengers(World world, Entity vehicle, Map<String, Object> data, Vec3 pos) throws CommandSyntaxException {
		if (!(data.get("Passengers") instanceof List<?> passengers) || passengers.isEmpty()) {
			return;
		}
		if (!(passengers.get(0) instanceof Map<?, ?> passengerData) || !(passengerData.get("id") instanceof String id)) {
			throw BAD_DATA.create("Passengers", "a passenger needs an id");
		}
		String type = ItemData.withNamespace(id);
		if (!EntityTypeIds.isSummonable(type)) {
			throw NOT_SUMMONABLE.create(type);
		}
		Map<String, Object> passengerMap = (Map<String, Object>) passengerData;
		Entity passenger = create(world, type, pos, passengerMap);
		if (world.spawnEntityInWorld(passenger)) {
			passenger.mountEntity(vehicle);
			addPassengers(world, passenger, passengerMap, pos);
		}
	}

	private static boolean isTaken(ModernCommandSource source, UUID uuid) {
		for (WorldServer world : source.server().worldServers) {
			for (Object loaded : world.loadedEntityList) {
				if (uuid.equals(((Entity) loaded).getUniqueID())) {
					return true;
				}
			}
		}
		return false;
	}
}
