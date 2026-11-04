package io.github.asgerjon.mcfly.mixins.early.spectator;

import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A spectator's clicks in a menu are refused (26.3's `handleContainerClick`: the menu sent back as it is), so it sees
 * a container without changing it.
 */
@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayServer {

	@Shadow
	public EntityPlayerMP playerEntity;

	@Inject(method = "processClickWindow", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/inventory/Container;slotClick(IIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;"), cancellable = true)
	private void mcfly$refuseSpectatorClicks(C0EPacketClickWindow packet, CallbackInfo ci) {
		if (Spectator.isSpectator(playerEntity)) {
			ci.cancel();
			List<ItemStack> contents = new ArrayList<>();
			for (Object slot : playerEntity.openContainer.inventorySlots) {
				contents.add(((Slot) slot).getStack());
			}
			playerEntity.sendContainerAndContentsToPlayer(playerEntity.openContainer, contents);
		}
	}
}
