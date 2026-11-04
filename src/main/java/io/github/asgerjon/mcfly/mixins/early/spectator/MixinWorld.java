package io.github.asgerjon.mcfly.mixins.early.spectator;

import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The world's searches don't find spectators (26.3's `EntitySelector.NO_SPECTATORS`): not as the closest player, not in
 * the plain entity searches (the ones with a selector of their own are left to it).
 */
@Mixin(World.class)
public class MixinWorld {

	@Redirect(method = "getClosestPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;getDistanceSq(DDD)D"))
	private double mcfly$spectatorsFarAway(EntityPlayer player, double x, double y, double z) {
		return Spectator.isSpectator(player) ? Double.MAX_VALUE : player.getDistanceSq(x, y, z);
	}

	@Inject(method = "getClosestPlayer", at = @At("TAIL"), cancellable = true)
	private void mcfly$neverASpectator(double x, double y, double z, double range, CallbackInfoReturnable<EntityPlayer> cir) {
		if (cir.getReturnValue() != null && Spectator.isSpectator(cir.getReturnValue())) {
			cir.setReturnValue(null);
		}
	}

	@ModifyArg(method = "getEntitiesWithinAABBExcludingEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getEntitiesWithinAABBExcludingEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/command/IEntitySelector;)Ljava/util/List;"),
		index = 2)
	private IEntitySelector mcfly$plainSearchSkipsSpectators(IEntitySelector selector) {
		return Spectator.NOT_SPECTATING;
	}

	@ModifyArg(method = "getEntitiesWithinAABB",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;selectEntitiesWithinAABB(Ljava/lang/Class;Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/command/IEntitySelector;)Ljava/util/List;"),
		index = 2)
	private IEntitySelector mcfly$classSearchSkipsSpectators(IEntitySelector selector) {
		return Spectator.NOT_SPECTATING;
	}
}
