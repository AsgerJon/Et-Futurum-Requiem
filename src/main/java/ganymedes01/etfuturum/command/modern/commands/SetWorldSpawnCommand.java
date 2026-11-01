package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.BlockPos;
import ganymedes01.etfuturum.command.modern.arguments.BlockPosArgument;
import ganymedes01.etfuturum.command.modern.arguments.RotationArgument;
import ganymedes01.etfuturum.command.modern.arguments.Vec3Argument;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

/** Modern's /setworldspawn [pos] [rotation] (spec §51.1), for the overworld only. */
public final class SetWorldSpawnCommand extends ModernCommand {

	private static final SimpleCommandExceptionType NOT_OVERWORLD = Responses.failure("commands.setworldspawn.failure.not_overworld");

	public SetWorldSpawnCommand() {
		super("setworldspawn", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("setworldspawn")
			.requires(requirement())
			.executes(c -> set(c.getSource(), null, 0.0F))
			.then(RequiredArgumentBuilder.<ModernCommandSource, Vec3Argument.Coordinates>argument("pos", BlockPosArgument.blockPos())
				.executes(c -> set(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"), 0.0F))
				.then(RequiredArgumentBuilder.<ModernCommandSource, RotationArgument.Rotation>argument("rotation", RotationArgument.rotation())
					.executes(c -> set(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"),
						RotationArgument.getRotation(c, "rotation").apply(c.getSource().rotation()[1], 0.0F)[0])))));
	}

	private int set(ModernCommandSource source, BlockPos given, float angle) throws CommandSyntaxException {
		if (source.world().provider.dimensionId != 0) {
			throw NOT_OVERWORLD.create();
		}
		BlockPos pos = given;
		if (pos == null) {
			Vec3 at = source.position();
			pos = new BlockPos(MathHelper.floor_double(at.xCoord), MathHelper.floor_double(at.yCoord), MathHelper.floor_double(at.zCoord));
		}
		source.world().setSpawnLocation(pos.x(), pos.y(), pos.z());
		source.sendSuccess(legacyView(), true, "commands.etfuturum.setworldspawn.success", pos.x(), pos.y(), pos.z(),
			String.valueOf(MathHelper.wrapAngleTo180_float(angle)));
		return 1;
	}
}
