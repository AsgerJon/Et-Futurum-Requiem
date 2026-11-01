package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.BlockInput;
import ganymedes01.etfuturum.command.modern.arguments.BlockPos;
import ganymedes01.etfuturum.command.modern.arguments.BlockPosArgument;
import ganymedes01.etfuturum.command.modern.arguments.BlockStateArgument;
import ganymedes01.etfuturum.command.modern.arguments.Vec3Argument;
import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.Locale;

/** Modern's /setblock (spec §49.1); 1.7.10's form stays for its block names (spec §49.3). */
public final class SetBlockCommand extends ModernCommand {

	/** EFR's keys: 1.7.10's own /setblock uses modern's for its own texts. */
	private static final SimpleCommandExceptionType FAILED = new SimpleCommandExceptionType(ComponentMessage.translatable("commands.etfuturum.setblock.failed"));

	private enum Mode {REPLACE, DESTROY, KEEP, STRICT}

	public SetBlockCommand() {
		super("setblock", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		RequiredArgumentBuilder<ModernCommandSource, BlockInput> block = RequiredArgumentBuilder.<ModernCommandSource, BlockInput>argument("block", BlockStateArgument.block())
			.executes(c -> setBlock(c, Mode.REPLACE));
		for (Mode mode : Mode.values()) {
			block.then(LiteralArgumentBuilder.<ModernCommandSource>literal(mode.name().toLowerCase(Locale.ROOT)).executes(c -> setBlock(c, mode)));
		}
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("setblock")
			.requires(requirement())
			.then(RequiredArgumentBuilder.<ModernCommandSource, Vec3Argument.Coordinates>argument("pos", BlockPosArgument.blockPos())
				.then(block)));
	}

	/** 1.7.10's /setblock needs one of its block names (or a block number) as its fourth word. */
	@Override
	public boolean legacyCouldRun(String[] args) {
		if (args.length < 4) {
			return false;
		}
		return Block.blockRegistry.containsKey(args[3]) || args[3].matches("\\d+");
	}

	private int setBlock(CommandContext<ModernCommandSource> context, Mode mode) throws CommandSyntaxException {
		ModernCommandSource source = context.getSource();
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
		BlockInput block = BlockStateArgument.getBlock(context, "block");
		World world = source.world();
		if (mode == Mode.KEEP && !world.isAirBlock(pos.x(), pos.y(), pos.z())) {
			throw FAILED.create();
		}
		boolean placeNeeded = true;
		if (mode == Mode.DESTROY) {
			world.func_147480_a(pos.x(), pos.y(), pos.z(), true);
			placeNeeded = !block.isAir() || !world.isAirBlock(pos.x(), pos.y(), pos.z());
		}
		if (placeNeeded && !block.place(world, pos.x(), pos.y(), pos.z())) {
			throw FAILED.create();
		}
		if (mode != Mode.STRICT) {
			world.notifyBlockChange(pos.x(), pos.y(), pos.z(), world.getBlock(pos.x(), pos.y(), pos.z()));
		}
		source.sendSuccess(legacyView(), true, "commands.etfuturum.setblock.success", pos.x(), pos.y(), pos.z());
		return 1;
	}
}
