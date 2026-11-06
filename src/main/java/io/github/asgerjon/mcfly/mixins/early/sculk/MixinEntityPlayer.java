package io.github.asgerjon.mcfly.mixins.early.sculk;

import io.github.asgerjon.mcfly.sculk.WardenSpawnTracker;
import io.github.asgerjon.mcfly.sculk.WardenWarned;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Each player's warden warning (SPEC-modern-movement.md §90.9, step H4), saved as 26.3's `warden_spawn_tracker`; ticked by `MixinEntityPlayerMP`. */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer implements WardenWarned {

	@Unique
	private final WardenSpawnTracker mcfly$tracker = new WardenSpawnTracker();

	@Override
	public WardenSpawnTracker mcfly$wardenSpawnTracker() {
		return mcfly$tracker;
	}

	@Inject(method = "writeEntityToNBT", at = @At("TAIL"))
	private void mcfly$saveWarning(NBTTagCompound tag, CallbackInfo ci) {
		NBTTagCompound tracker = new NBTTagCompound();
		mcfly$tracker.writeToNBT(tracker);
		tag.setTag("warden_spawn_tracker", tracker);
	}

	@Inject(method = "readEntityFromNBT", at = @At("TAIL"))
	private void mcfly$loadWarning(NBTTagCompound tag, CallbackInfo ci) {
		mcfly$tracker.readFromNBT(tag.getCompoundTag("warden_spawn_tracker"));
	}
}
