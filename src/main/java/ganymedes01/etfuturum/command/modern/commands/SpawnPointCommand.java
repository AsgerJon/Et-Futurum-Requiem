package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.BlockPos;
import ganymedes01.etfuturum.command.modern.arguments.BlockPosArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntityArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntitySelector;
import ganymedes01.etfuturum.command.modern.arguments.RotationArgument;
import ganymedes01.etfuturum.command.modern.arguments.Vec3Argument;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;

import java.util.Collections;
import java.util.List;

/** Modern's /spawnpoint [targets] [pos] [rotation] (spec §51.1); 1.7.10 keeps no angle. */
public final class SpawnPointCommand extends ModernCommand {

	public SpawnPointCommand() {
		super("spawnpoint", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("spawnpoint")
			.requires(requirement())
			.executes(c -> set(c, Collections.singletonList(Commands.requirePlayer(c.getSource())), null, 0.0F))
			.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("targets", EntityArgument.players())
				.executes(c -> set(c, EntityArgument.getPlayers(c, "targets"), null, 0.0F))
				.then(RequiredArgumentBuilder.<ModernCommandSource, Vec3Argument.Coordinates>argument("pos", BlockPosArgument.blockPos())
					.executes(c -> set(c, EntityArgument.getPlayers(c, "targets"), BlockPosArgument.getLoadedBlockPos(c, "pos"), 0.0F))
					.then(RequiredArgumentBuilder.<ModernCommandSource, RotationArgument.Rotation>argument("rotation", RotationArgument.rotation())
						.executes(c -> set(c, EntityArgument.getPlayers(c, "targets"), BlockPosArgument.getLoadedBlockPos(c, "pos"),
							RotationArgument.getRotation(c, "rotation").apply(c.getSource().rotation()[1], 0.0F)[0]))))));
	}

	/** Modern's id for a dimension. */
	static String dimensionId(int dimension) {
		return switch (dimension) {
			case 0 -> "minecraft:overworld";
			case -1 -> "minecraft:the_nether";
			case 1 -> "minecraft:the_end";
			default -> "minecraft:dim" + dimension;
		};
	}

	private int set(CommandContext<ModernCommandSource> context, List<EntityPlayerMP> players, BlockPos given, float angle) throws CommandSyntaxException {
		ModernCommandSource source = context.getSource();
		BlockPos pos = given;
		if (pos == null) {
			net.minecraft.util.Vec3 at = source.position();
			pos = new BlockPos(MathHelper.floor_double(at.xCoord), MathHelper.floor_double(at.yCoord), MathHelper.floor_double(at.zCoord));
		}
		int dimension = source.world().provider.dimensionId;
		for (EntityPlayerMP player : players) {
			player.setSpawnChunk(new ChunkCoordinates(pos.x(), pos.y(), pos.z()), true, dimension);
		}
		BlockPos at = pos;
		String shownAngle = String.valueOf(MathHelper.wrapAngleTo180_float(angle));
		return Responses.send(source, this, players, null,
			"commands.spawnpoint.success.single", p -> new Object[]{at.x(), at.y(), at.z(), shownAngle, dimensionId(dimension), p.func_145748_c_()},
			"commands.spawnpoint.success.multiple", n -> new Object[]{at.x(), at.y(), at.z(), shownAngle, dimensionId(dimension), n});
	}
}
