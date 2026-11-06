package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A record put in a jukebox posts two `block_change`s at it (SPEC-modern-movement.md §90.9, step H1; 26.3's
 * `JukeboxPlayable.tryInsertIntoJukebox`): the jukebox's own for its item (by no one), then the player's.
 */
@Mixin(ItemRecord.class)
public abstract class MixinItemRecord {

	@Inject(method = "onItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockJukebox;func_149926_b(Lnet/minecraft/world/World;IIILnet/minecraft/item/ItemStack;)V", shift = At.Shift.AFTER))
	private void mcfly$inserted(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
		CallbackInfoReturnable<Boolean> cir) {
		GameEventHooks.atBlock(world, "block_change", x, y, z, null);
		GameEventHooks.atBlock(world, "block_change", x, y, z, player);
	}
}
