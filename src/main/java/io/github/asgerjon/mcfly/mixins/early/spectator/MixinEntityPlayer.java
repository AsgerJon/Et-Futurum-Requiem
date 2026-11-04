package io.github.asgerjon.mcfly.mixins.early.spectator;

import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A spectator isn't there for the world (spec §83): not pushed, collided with or pushing, on no ladder, in no water,
 * wearing nothing, attacking nothing; the item on its cursor goes back into its inventory when it becomes one.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase {

	@Shadow
	public InventoryPlayer inventory;

	@Shadow
	public abstract EntityItem dropPlayerItemWithRandomChoice(ItemStack stack, boolean randomly);

	public MixinEntityPlayer(World world) {
		super(world);
	}

	private boolean mcfly$spectating() {
		return Spectator.isSpectator((EntityPlayer) (Object) this);
	}

	@Override
	public boolean canBePushed() {
		return !mcfly$spectating() && super.canBePushed();
	}

	@Override
	public boolean canBeCollidedWith() {
		return !mcfly$spectating() && super.canBeCollidedWith();
	}

	@Override
	protected void collideWithNearbyEntities() {
		if (!mcfly$spectating()) {
			super.collideWithNearbyEntities();
		}
	}

	@Override
	public boolean isOnLadder() {
		return !mcfly$spectating() && super.isOnLadder();
	}

	@Override
	public boolean handleWaterMovement() {
		return !mcfly$spectating() && super.handleWaterMovement();
	}

	@Override
	public boolean canAttackWithItem() {
		return !mcfly$spectating() && super.canAttackWithItem();
	}

	@Inject(method = "getEquipmentInSlot", at = @At("HEAD"), cancellable = true)
	private void mcfly$wearsNothing(int slot, CallbackInfoReturnable<ItemStack> cir) {
		if (mcfly$spectating()) {
			cir.setReturnValue(null);
		}
	}

	@Inject(method = "isCurrentToolAdventureModeExempt", at = @At("HEAD"), cancellable = true)
	private void mcfly$noToolUse(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (mcfly$spectating()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "setGameType", at = @At("HEAD"))
	private void mcfly$cursorItemBack(WorldSettings.GameType type, CallbackInfo ci) {
		if (type == Spectator.GAME_TYPE && type != null) {
			ItemStack carried = inventory.getItemStack();
			if (carried != null && !inventory.addItemStackToInventory(carried)) {
				dropPlayerItemWithRandomChoice(carried, true);
			}
		}
	}
}
