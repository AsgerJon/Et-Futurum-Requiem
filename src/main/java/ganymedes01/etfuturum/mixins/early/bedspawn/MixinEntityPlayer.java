package ganymedes01.etfuturum.mixins.early.bedspawn;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Getting into a bed in modern's order (SPEC-modern-movement.md §53.3), as startSleepInBed: the reach first, then the
 * spawn point, then whether sleeping is possible now. 1.7.10 asked about the time first, measured the reach from the
 * head's corner afterwards and only set the spawn point on waking up.
 */
@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {

	@Inject(method = "sleepInBedAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isDaytime()Z"), cancellable = true)
	private void etfu$reachAndSpawnBeforeTheTime(int x, int y, int z, CallbackInfoReturnable<EntityPlayer.EnumStatus> cir) {
		EntityPlayer player = (EntityPlayer) (Object) this;
		if (!etfu$inReach(player, x, y, z)) {
			cir.setReturnValue(EntityPlayer.EnumStatus.TOO_FAR_AWAY);
			return;
		}
		ChunkCoordinates bed = new ChunkCoordinates(x, y, z);
		if (!bed.equals(player.getBedLocation(player.dimension))) {
			player.addChatComponentMessage(new ChatComponentTranslation("block.minecraft.set_spawn"));
		}
		player.setSpawnChunk(bed, false);
	}

	/** 1.7.10's own reach check, after the time, never fails: the modern one has run before it. */
	@ModifyConstant(method = "sleepInBedAt", constant = {@Constant(doubleValue = 3.0D), @Constant(doubleValue = 2.0D)})
	private double etfu$reachCheckedBefore(double limit) {
		return Double.MAX_VALUE;
	}

	/** Modern's reach: either half's bottom centre within 3 across and 2 up or down of the player's feet. */
	@Unique
	private static boolean etfu$inReach(EntityPlayer player, int x, int y, int z) {
		if (etfu$near(player, x, y, z)) {
			return true;
		}
		Block block = player.worldObj.getBlock(x, y, z);
		if (!block.isBed(player.worldObj, x, y, z, player)) {
			return false;
		}
		int direction = block.getBedDirection(player.worldObj, x, y, z) & 3;
		return etfu$near(player, x - Direction.offsetX[direction], y, z - Direction.offsetZ[direction]);
	}

	@Unique
	private static boolean etfu$near(EntityPlayer player, int x, int y, int z) {
		return Math.abs(player.posX - (x + 0.5D)) <= 3.0D && Math.abs(player.posY - y) <= 2.0D && Math.abs(player.posZ - (z + 0.5D)) <= 3.0D;
	}
}
