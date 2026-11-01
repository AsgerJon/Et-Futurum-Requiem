package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.EntityArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntitySelector;
import ganymedes01.etfuturum.command.modern.arguments.RotationArgument;
import ganymedes01.etfuturum.command.modern.arguments.Vec3Argument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Modern's /teleport and its alias /tp (spec §51.1), within a world as 1.7.10's. */
public final class TeleportCommand extends ModernCommand {

	private static final SimpleCommandExceptionType INVALID_POSITION = Responses.failure("commands.teleport.invalidPosition");
	private static final SimpleCommandExceptionType OTHER_WORLD = new SimpleCommandExceptionType(ComponentMessage.translatable("commands.tp.notSameDimension"));

	/** Where a teleported entity ends up looking, besides keeping its own rotation. */
	@FunctionalInterface
	private interface Facing {
		float[] rotation(Entity entity, double x, double y, double z, ModernCommandSource source) throws CommandSyntaxException;
	}

	public TeleportCommand() {
		super("teleport", 2);
	}

	@Override
	public List<String> aliases() {
		return Collections.singletonList("tp");
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		LiteralCommandNode<ModernCommandSource> teleport = dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("teleport")
			.requires(requirement())
			.then(RequiredArgumentBuilder.<ModernCommandSource, Vec3Argument.Coordinates>argument("location", Vec3Argument.vec3())
				.executes(c -> toPosition(c.getSource(), Collections.singletonList(Commands.requireEntity(c.getSource())),
					Vec3Argument.getVec3(c, "location"), null)))
			.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("destination", EntityArgument.of(true, false))
				.executes(c -> toEntity(c.getSource(), Collections.singletonList(Commands.requireEntity(c.getSource())),
					EntityArgument.getEntities(c, "destination").get(0))))
			.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("targets", EntityArgument.entities())
				.then(RequiredArgumentBuilder.<ModernCommandSource, Vec3Argument.Coordinates>argument("location", Vec3Argument.vec3())
					.executes(c -> toPosition(c.getSource(), EntityArgument.getEntities(c, "targets"), Vec3Argument.getVec3(c, "location"), null))
					.then(RequiredArgumentBuilder.<ModernCommandSource, RotationArgument.Rotation>argument("rotation", RotationArgument.rotation())
						.executes(c -> {
							RotationArgument.Rotation rotation = RotationArgument.getRotation(c, "rotation");
							return toPosition(c.getSource(), EntityArgument.getEntities(c, "targets"), Vec3Argument.getVec3(c, "location"),
								(entity, x, y, z, source) -> rotation.apply(entity.rotationYaw, entity.rotationPitch));
						}))
					.then(LiteralArgumentBuilder.<ModernCommandSource>literal("facing")
						.then(LiteralArgumentBuilder.<ModernCommandSource>literal("entity")
							.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("facingEntity", EntityArgument.of(true, false))
								.executes(c -> facingEntity(c, true))
								.then(LiteralArgumentBuilder.<ModernCommandSource>literal("eyes").executes(c -> facingEntity(c, true)))
								.then(LiteralArgumentBuilder.<ModernCommandSource>literal("feet").executes(c -> facingEntity(c, false)))))
						.then(RequiredArgumentBuilder.<ModernCommandSource, Vec3Argument.Coordinates>argument("facingLocation", Vec3Argument.vec3())
							.executes(c -> {
								Vec3 point = Vec3Argument.getVec3(c, "facingLocation");
								return toPosition(c.getSource(), EntityArgument.getEntities(c, "targets"), Vec3Argument.getVec3(c, "location"),
									(entity, x, y, z, source) -> lookAt(entity, x, y, z, point));
							}))))
				.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("destination", EntityArgument.of(true, false))
					.executes(c -> toEntity(c.getSource(), EntityArgument.getEntities(c, "targets"), EntityArgument.getEntities(c, "destination").get(0))))));
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("tp").requires(requirement()).redirect(teleport));
	}

	private int facingEntity(com.mojang.brigadier.context.CommandContext<ModernCommandSource> context, boolean eyes) throws CommandSyntaxException {
		Entity facing = EntityArgument.getEntities(context, "facingEntity").get(0);
		Vec3 point = Vec3.createVectorHelper(facing.posX, facing.boundingBox.minY + (eyes ? facing.getEyeHeight() : 0.0D), facing.posZ);
		return toPosition(context.getSource(), EntityArgument.getEntities(context, "targets"), Vec3Argument.getVec3(context, "location"),
			(entity, x, y, z, source) -> lookAt(entity, x, y, z, point));
	}

	/** The rotation from the entity's eyes, once it stands at x y z, to the point. */
	private static float[] lookAt(Entity entity, double x, double y, double z, Vec3 point) {
		double dx = point.xCoord - x;
		double dy = point.yCoord - (y + entity.getEyeHeight());
		double dz = point.zCoord - z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
		float pitch = (float) -(Math.atan2(dy, flat) * 180.0D / Math.PI);
		return new float[]{MathHelper.wrapAngleTo180_float(yaw), MathHelper.wrapAngleTo180_float(pitch)};
	}

	private int toPosition(ModernCommandSource source, List<Entity> targets, Vec3 position, Facing facing) throws CommandSyntaxException {
		for (Entity entity : targets) {
			float[] rotation = facing == null ? new float[]{entity.rotationYaw, entity.rotationPitch}
				: facing.rotation(entity, position.xCoord, position.yCoord, position.zCoord, source);
			move(source, entity, position.xCoord, position.yCoord, position.zCoord, rotation[0], rotation[1], source.world().provider.dimensionId);
		}
		String x = String.format(Locale.ROOT, "%f", position.xCoord);
		String y = String.format(Locale.ROOT, "%f", position.yCoord);
		String z = String.format(Locale.ROOT, "%f", position.zCoord);
		return Responses.send(source, this, targets, null,
			"commands.teleport.success.location.single", e -> new Object[]{e.func_145748_c_(), x, y, z},
			"commands.teleport.success.location.multiple", n -> new Object[]{n, x, y, z});
	}

	private int toEntity(ModernCommandSource source, List<Entity> targets, Entity destination) throws CommandSyntaxException {
		for (Entity entity : targets) {
			move(source, entity, destination.posX, destination.boundingBox.minY, destination.posZ, destination.rotationYaw, destination.rotationPitch,
				destination.worldObj.provider.dimensionId);
		}
		return Responses.send(source, this, targets, null,
			"commands.teleport.success.entity.single", e -> new Object[]{e.func_145748_c_(), destination.func_145748_c_()},
			"commands.teleport.success.entity.multiple", n -> new Object[]{n, destination.func_145748_c_()});
	}

	/** A player moves by its connection (1.7.10's way); others are set in place. Falling stops, as in modern. */
	private static void move(ModernCommandSource source, Entity entity, double x, double y, double z, float yaw, float pitch, int dimension) throws CommandSyntaxException {
		if (Math.abs(x) >= 3.0E7D || Math.abs(z) >= 3.0E7D || Math.abs(y) >= 2.0E7D) {
			throw INVALID_POSITION.create();
		}
		if (entity.worldObj.provider.dimensionId != dimension) {
			throw OTHER_WORLD.create();
		}
		if (entity.ridingEntity != null) {
			entity.mountEntity(null);
		}
		if (entity instanceof EntityPlayerMP player) {
			player.playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
		} else {
			entity.setLocationAndAngles(x, y, z, yaw, pitch);
			entity.setRotationYawHead(yaw);
		}
		entity.motionY = 0.0D;
		entity.onGround = true;
		entity.fallDistance = 0.0F;
		if (entity instanceof EntityLiving living) {
			living.getNavigator().clearPathEntity();
		}
	}
}
