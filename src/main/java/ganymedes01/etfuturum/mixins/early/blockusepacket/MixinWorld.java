package ganymedes01.etfuturum.mixins.early.blockusepacket;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A block can't be placed inside the player placing it (SPEC-modern-movement.md §35). 1.7.10 left that to the
 * client: its own placement check counted the player's body and refused to send the click, while the server's check
 * excluded the placing player and trusted the client. With every click sent to the server (modernBlockUsePackets),
 * the server has to count the placing player itself, as it does since 1.8 and as modern's unobstructed check does.
 * Only the placing player's exclusion is dropped; other entities passed here (none in vanilla) keep theirs.
 */
@Mixin(World.class)
public class MixinWorld {
	@ModifyVariable(method = "canPlaceEntityOnSide", at = @At("HEAD"), argsOnly = true)
	private Entity etfu$countThePlacingPlayer(Entity excluded) {
		return excluded instanceof EntityPlayer ? null : excluded;
	}
}
