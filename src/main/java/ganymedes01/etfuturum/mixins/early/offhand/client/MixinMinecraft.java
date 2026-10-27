package ganymedes01.etfuturum.mixins.early.offhand.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.Offhand;
import ganymedes01.etfuturum.offhand.OffhandUse;
import ganymedes01.etfuturum.offhand.OffhandUseClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The off-hand's turn (SPEC-modern-movement.md §36.2): 1.7.10's right click runs for the main hand unchanged; when
 * none of its steps used the click (no entity or block used, no item use started, no boost taken, no interact event
 * cancelled), the off-hand gets the same click.
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

	@Shadow
	public EntityClientPlayerMP thePlayer;

	@Shadow
	public MovingObjectPosition objectMouseOver;

	@Unique
	private boolean etfu$mainHandUsedClick;

	@Inject(method = "func_147121_ag", at = @At("HEAD"))
	private void etfu$startRightClick(CallbackInfo ci) {
		etfu$mainHandUsedClick = false;
	}

	@ModifyExpressionValue(method = "func_147121_ag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;interactWithEntitySendPacket(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;)Z"))
	private boolean etfu$entityUsedClick(boolean used) {
		etfu$mainHandUsedClick |= used;
		return used;
	}

	@ModifyExpressionValue(method = "func_147121_ag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;onPlayerRightClick(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;IIIILnet/minecraft/util/Vec3;)Z"))
	private boolean etfu$blockUsedClick(boolean used) {
		etfu$mainHandUsedClick |= used;
		return used;
	}

	@ModifyExpressionValue(method = "func_147121_ag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;sendUseItem(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;)Z"))
	private boolean etfu$airUsedClick(boolean used) {
		etfu$mainHandUsedClick |= used;
		return used;
	}

	@ModifyExpressionValue(method = "func_147121_ag", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/entity/player/PlayerInteractEvent;isCanceled()Z", remap = false))
	private boolean etfu$cancelledClick(boolean cancelled) {
		etfu$mainHandUsedClick |= cancelled;
		return cancelled;
	}

	/**
	 * After the main hand's pass. A click that started a use there (a bow draw, eating: the stack is unchanged, so
	 * 1.7.10's right click reports nothing) or that the firework boost took is consumed, like modern's CONSUME and
	 * SUCCESS results, and never reaches the off-hand.
	 */
	@Inject(method = "func_147121_ag", at = @At("RETURN"))
	private void etfu$offhandTakesTheClick(CallbackInfo ci) {
		if (!Offhand.isEnabled() || etfu$mainHandUsedClick || thePlayer == null || thePlayer.isUsingItem()
				|| OffhandUse.consumesClickWithoutChange(thePlayer)) {
			return;
		}
		if (OffhandUseClient.tryOnClient((Minecraft) (Object) this, objectMouseOver)) {
			Offhand.swing(thePlayer, Hand.OFF);
		}
	}

	/**
	 * Creative pick block tells the server the slot it filled as "the container's last nine slots, plus the selected
	 * index" (§36.1); with the off-hand slot after the hotbar that is one slot too far: the next hotbar slot, or the
	 * off-hand from the last one. Modern names it 36 + selected, so the hotbar ends at slot 45 here.
	 */
	@ModifyExpressionValue(method = "func_147112_ai", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
	private int etfu$hotbarEndsBeforeTheOffhandSlot(int size) {
		return Offhand.isEnabled() ? Offhand.CONTAINER_SLOT : size;
	}
}
