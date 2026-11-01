package ganymedes01.etfuturum.command.modern.commands;

import com.github.bsideup.jabel.Desugar;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
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
import ganymedes01.etfuturum.command.modern.arguments.BlockPredicateArgument;
import ganymedes01.etfuturum.command.modern.arguments.BlockTest;
import ganymedes01.etfuturum.command.modern.arguments.Vec3Argument;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/** Modern's /clone (spec §49.1) within one world; 1.7.10 has none. */
public final class CloneCommand extends ModernCommand {

	private static final SimpleCommandExceptionType OVERLAP = new SimpleCommandExceptionType(ComponentMessage.translatable("commands.clone.overlap"));
	private static final SimpleCommandExceptionType FAILED = new SimpleCommandExceptionType(ComponentMessage.translatable("commands.clone.failed"));
	private static final SimpleCommandExceptionType NOT_LOADED = new SimpleCommandExceptionType(ComponentMessage.translatable("argument.pos.unloaded"));
	private static final BlockTest ANY = (world, x, y, z) -> true;
	private static final BlockTest NOT_AIR = (world, x, y, z) -> !world.isAirBlock(x, y, z);

	private enum Mode {
		FORCE(true), MOVE(true), NORMAL(false);

		final boolean canOverlap;

		Mode(boolean canOverlap) {
			this.canOverlap = canOverlap;
		}
	}

	/** One block to put in: where, what, and its block entity's data (null for none). */
	@Desugar
	private record Copy(BlockPos pos, Block block, int meta, NBTTagCompound data) {
	}

	@FunctionalInterface
	private interface Filter {
		BlockTest get(CommandContext<ModernCommandSource> context);
	}

	public CloneCommand() {
		super("clone", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("clone")
			.requires(requirement())
			.then(RequiredArgumentBuilder.<ModernCommandSource, Vec3Argument.Coordinates>argument("begin", BlockPosArgument.blockPos())
				.then(RequiredArgumentBuilder.<ModernCommandSource, Vec3Argument.Coordinates>argument("end", BlockPosArgument.blockPos())
					.then(withFilters(RequiredArgumentBuilder.<ModernCommandSource, Vec3Argument.Coordinates>argument("destination", BlockPosArgument.blockPos()), false)
						.then(withFilters(LiteralArgumentBuilder.<ModernCommandSource>literal("strict"), true))))));
	}

	/** The node runs as a normal clone; after it replace, masked and filtered, each with force, move or normal. */
	private <T extends ArgumentBuilder<ModernCommandSource, T>> T withFilters(T builder, boolean strict) {
		return builder.executes(c -> clone(c, ANY, Mode.NORMAL, strict))
			.then(withModes(LiteralArgumentBuilder.<ModernCommandSource>literal("replace"), c -> ANY, strict))
			.then(withModes(LiteralArgumentBuilder.<ModernCommandSource>literal("masked"), c -> NOT_AIR, strict))
			.then(LiteralArgumentBuilder.<ModernCommandSource>literal("filtered")
				.then(withModes(RequiredArgumentBuilder.<ModernCommandSource, BlockTest>argument("filter", BlockPredicateArgument.blockPredicate()),
					c -> BlockPredicateArgument.getBlockPredicate(c, "filter"), strict)));
	}

	private <T extends ArgumentBuilder<ModernCommandSource, T>> T withModes(T builder, Filter filter, boolean strict) {
		builder.executes(c -> clone(c, filter.get(c), Mode.NORMAL, strict));
		for (Mode mode : Mode.values()) {
			builder.then(LiteralArgumentBuilder.<ModernCommandSource>literal(mode.name().toLowerCase(Locale.ROOT))
				.executes(c -> clone(c, filter.get(c), mode, strict)));
		}
		return builder;
	}

	private int clone(CommandContext<ModernCommandSource> context, BlockTest filter, Mode mode, boolean strict) throws CommandSyntaxException {
		ModernCommandSource source = context.getSource();
		BlockPos begin = BlockPosArgument.getLoadedBlockPos(context, "begin");
		BlockPos end = BlockPosArgument.getLoadedBlockPos(context, "end");
		BlockPos destination = BlockPosArgument.getLoadedBlockPos(context, "destination");
		int minX = Math.min(begin.x(), end.x());
		int minY = Math.min(begin.y(), end.y());
		int minZ = Math.min(begin.z(), end.z());
		int sizeX = Math.abs(begin.x() - end.x());
		int sizeY = Math.abs(begin.y() - end.y());
		int sizeZ = Math.abs(begin.z() - end.z());
		int offsetX = destination.x() - minX;
		int offsetY = destination.y() - minY;
		int offsetZ = destination.z() - minZ;
		boolean overlaps = destination.x() <= minX + sizeX && destination.x() + sizeX >= minX
			&& destination.y() <= minY + sizeY && destination.y() + sizeY >= minY
			&& destination.z() <= minZ + sizeZ && destination.z() + sizeZ >= minZ;
		if (!mode.canOverlap && overlaps) {
			throw OVERLAP.create();
		}
		long volume = (long) (sizeX + 1) * (sizeY + 1) * (sizeZ + 1);
		if (volume > FillCommand.LIMIT) {
			throw FillCommand.TOO_BIG.create(FillCommand.LIMIT, volume);
		}
		World world = source.world();
		if (!world.checkChunksExist(minX, minY, minZ, minX + sizeX, minY + sizeY, minZ + sizeZ)
			|| !world.checkChunksExist(destination.x(), destination.y(), destination.z(), destination.x() + sizeX, destination.y() + sizeY, destination.z() + sizeZ)
			|| destination.y() + sizeY >= world.getHeight()) {
			throw NOT_LOADED.create();
		}
		// solid blocks first, then those with block entities, then the rest (torches, plants), so each has its support
		List<Copy> solid = new ArrayList<>();
		List<Copy> withEntity = new ArrayList<>();
		List<Copy> other = new ArrayList<>();
		Deque<BlockPos> toClear = new ArrayDeque<>();
		for (int z = minZ; z <= minZ + sizeZ; z++) {
			for (int y = minY; y <= minY + sizeY; y++) {
				for (int x = minX; x <= minX + sizeX; x++) {
					if (!filter.test(world, x, y, z)) {
						continue;
					}
					Block block = world.getBlock(x, y, z);
					int meta = world.getBlockMetadata(x, y, z);
					BlockPos to = new BlockPos(x + offsetX, y + offsetY, z + offsetZ);
					TileEntity tile = world.getTileEntity(x, y, z);
					if (tile != null) {
						NBTTagCompound data = new NBTTagCompound();
						tile.writeToNBT(data);
						withEntity.add(new Copy(to, block, meta, data));
						toClear.addLast(new BlockPos(x, y, z));
					} else if (!block.isOpaqueCube() && !block.renderAsNormalBlock()) {
						other.add(new Copy(to, block, meta, null));
						toClear.addFirst(new BlockPos(x, y, z));
					} else {
						solid.add(new Copy(to, block, meta, null));
						toClear.addLast(new BlockPos(x, y, z));
					}
				}
			}
		}
		if (mode == Mode.MOVE) {
			// the attached blocks go first, and containers are emptied, so nothing drops
			for (BlockPos pos : toClear) {
				BlockInput.emptyContainer(world, pos.x(), pos.y(), pos.z());
				world.setBlock(pos.x(), pos.y(), pos.z(), Blocks.air, 0, 2);
			}
		}
		List<Copy> all = new ArrayList<>(solid);
		all.addAll(withEntity);
		all.addAll(other);
		int count = 0;
		for (Copy copy : all) {
			BlockPos pos = copy.pos();
			BlockInput.emptyContainer(world, pos.x(), pos.y(), pos.z());
			if (BlockInput.setBlock(world, pos.x(), pos.y(), pos.z(), copy.block(), copy.meta())) {
				count++;
			}
		}
		for (Copy copy : withEntity) {
			BlockPos pos = copy.pos();
			TileEntity tile = world.getTileEntity(pos.x(), pos.y(), pos.z());
			if (tile != null) {
				NBTTagCompound data = (NBTTagCompound) copy.data().copy();
				data.setInteger("x", pos.x());
				data.setInteger("y", pos.y());
				data.setInteger("z", pos.z());
				tile.readFromNBT(data);
				tile.markDirty();
				world.markBlockForUpdate(pos.x(), pos.y(), pos.z());
			}
		}
		if (!strict) {
			for (int i = all.size() - 1; i >= 0; i--) {
				BlockPos pos = all.get(i).pos();
				world.notifyBlockChange(pos.x(), pos.y(), pos.z(), all.get(i).block());
			}
			if (mode == Mode.MOVE) {
				for (BlockPos pos : toClear) {
					world.notifyBlockChange(pos.x(), pos.y(), pos.z(), Blocks.air);
				}
			}
		}
		if (count == 0) {
			throw FAILED.create();
		}
		source.sendSuccess(legacyView(), true, "commands.clone.success", count);
		return count;
	}
}
