package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A note block playing posts `note_block_play` (SPEC-modern-movement.md §90.9, step H1; 26.3's `NoteBlock.playNote`), as its note goes out. */
@Mixin(TileEntityNote.class)
public abstract class MixinTileEntityNote {

	@Inject(method = "triggerNote", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addBlockEvent(IIILnet/minecraft/block/Block;II)V"))
	private void mcfly$played(World world, int x, int y, int z, CallbackInfo ci) {
		GameEvents.postAtBlock(world, "note_block_play", x, y, z, null, Blocks.noteblock, world.getBlockMetadata(x, y, z));
	}
}
