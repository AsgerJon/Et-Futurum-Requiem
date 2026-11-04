package io.github.asgerjon.mcfly.mixins.early.spectator;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Iterator;
import java.util.List;

/** Spectators don't count for sleeping through the night (26.3's sleep status leaves them out). */
@Mixin(WorldServer.class)
public class MixinWorldServer {

	@WrapOperation(method = "areAllPlayersAsleep", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
	private Iterator<EntityPlayer> mcfly$asleepWithoutSpectators(List<EntityPlayer> players, Operation<Iterator<EntityPlayer>> original) {
		return original.call(Spectator.withoutSpectators(players));
	}

	@WrapOperation(method = "updateAllPlayersSleepingFlag", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
	private boolean mcfly$noneWithoutSpectators(List<EntityPlayer> players, Operation<Boolean> original,
		@Share("awake") LocalRef<List<EntityPlayer>> others) {
		List<EntityPlayer> notSpectating = Spectator.withoutSpectators(players);
		others.set(notSpectating);
		return original.call(notSpectating);
	}

	@WrapOperation(method = "updateAllPlayersSleepingFlag", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
	private Iterator<EntityPlayer> mcfly$sleepingWithoutSpectators(List<EntityPlayer> players, Operation<Iterator<EntityPlayer>> original,
		@Share("awake") LocalRef<List<EntityPlayer>> others) {
		return original.call(others.get());
	}
}
