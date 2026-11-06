package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemLead;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The player's leashed mobs tied to a fence post `block_attach` at it, by the player, once (SPEC-modern-movement.md §90.9, step H1; 26.3's `LeadItem.bindPlayerMobs`). */
@Mixin(ItemLead.class)
public abstract class MixinItemLead {

	@Inject(method = "func_150909_a", at = @At("RETURN"))
	private static void mcfly$tied(EntityPlayer player, World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) {
			GameEventHooks.atBlock(world, "block_attach", x, y, z, player);
		}
	}
}
