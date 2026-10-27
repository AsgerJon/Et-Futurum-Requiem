package ganymedes01.etfuturum.mixins.early.arrowmomentum;

import ganymedes01.etfuturum.movement.IKnownMovement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C03PacketPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The server learns a player's movement from the position packets, as modern's server keeps the last known client
 * movement: each packet's step from the position the server holds to the one the packet brings. A packet without a
 * position means the player stands still; riding, the vehicle's own step is what the server knows. Nothing is
 * recorded while a teleport is still waiting for the client's confirmation, since those positions are stale.
 */
@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayServer {
	@Shadow
	public EntityPlayerMP playerEntity;
	@Shadow
	private boolean hasMoved;

	@Inject(method = "processPlayer", at = @At("HEAD"))
	private void etfu$rememberClientMovement(C03PacketPlayer packet, CallbackInfo ci) {
		if (!hasMoved || !(playerEntity instanceof IKnownMovement known)) {
			return;
		}
		Entity vehicle = playerEntity.ridingEntity;
		double x, y, z;
		if (vehicle != null) {
			x = vehicle.posX - vehicle.prevPosX;
			y = vehicle.posY - vehicle.prevPosY;
			z = vehicle.posZ - vehicle.prevPosZ;
		} else if (packet.func_149466_j()) {
			x = packet.func_149464_c() - playerEntity.posX;
			y = packet.func_149467_d() - playerEntity.posY;
			z = packet.func_149472_e() - playerEntity.posZ;
		} else {
			x = y = z = 0;
		}
		// A step the server would reject as moving too fast is no movement worth knowing.
		if (x * x + y * y + z * z > 100) {
			x = y = z = 0;
		}
		known.etfu$setKnownMovement(x, y, z);
	}
}
