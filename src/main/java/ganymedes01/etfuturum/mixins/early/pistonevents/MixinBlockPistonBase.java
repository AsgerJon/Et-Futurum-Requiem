package ganymedes01.etfuturum.mixins.early.pistonevents;

import net.minecraft.block.BlockPistonBase;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Piston block events act on the piston as it stands when they run, as modern's do: the facing comes from the block,
 * not from the event. 1.7.10 took it from the event, queued when the piston was still the old one. The difference is
 * what makes modern's "exploding piston" bedrock breaking work: a piston placed facing down into the spot of one that
 * was just blown up inherits its pending retraction and clears the block in front of its own face. Only the piston
 * base that reappears after the retraction keeps the queued facing, as in modern.
 * <p>
 * This mixin is for vanilla's method only. With {@code betterPistons} on, EFR's Back in Slime port
 * ({@code backinslime.MixinBlockPistonBase}) overwrites the whole method and carries the same rule itself; the mixin
 * plugin registers only one of the two, since Mixin refuses to inject into a method another mixin overwrote.
 */
@Mixin(BlockPistonBase.class)
public class MixinBlockPistonBase {

	@Unique
	private int etfu$queuedFacing;

	@ModifyVariable(method = "onBlockEventReceived", at = @At("HEAD"), argsOnly = true, ordinal = 4)
	private int etfu$facingOfThePistonNow(int eventData, World world, int x, int y, int z, int eventId, int eventDataAgain) {
		etfu$queuedFacing = eventData;
		return BlockPistonBase.getPistonOrientation(world.getBlockMetadata(x, y, z));
	}

	@ModifyArg(method = "onBlockEventReceived", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockPistonMoving;getTileEntity(Lnet/minecraft/block/Block;IIZZ)Lnet/minecraft/tileentity/TileEntity;", ordinal = 0), index = 1)
	private int etfu$restoreTheQueuedFacing(int storedMeta) {
		return etfu$queuedFacing;
	}
}
