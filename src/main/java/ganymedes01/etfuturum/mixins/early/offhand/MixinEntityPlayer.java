package ganymedes01.etfuturum.mixins.early.offhand;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.movement.ModernMovement;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.IOffhandPlayer;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The player's off-hand state (SPEC-modern-movement.md §36.2, §36.3): the DataWatcher copy of the off-hand stack
 * for other clients (1.7.10's equipment packet has no slot for it; the copy, not the live stack, goes in so a change
 * of count is noticed), and the hand an ongoing item use belongs to: the use goes on while that hand still holds the
 * item, and the result goes back into that hand.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase implements IOffhandPlayer {

	private static final int ITEM_STACK_DATA_TYPE = 5;

	@Shadow
	private ItemStack itemInUse;

	@Shadow
	private int itemInUseCount;

	@Shadow
	protected abstract void updateItemUse(ItemStack stack, int count);

	@Shadow
	public abstract void clearItemInUse();

	@Unique
	private Hand etfu$usingHand = Hand.MAIN;

	public MixinEntityPlayer(World world) {
		super(world);
	}

	/**
	 * The off-hand's synced objects. The using hand and the main arm live in the pose byte (§36.3), which the pose
	 * system registers; without it (sneaking, swimming and the elytra all off) the byte is registered here.
	 */
	@Inject(method = "entityInit", at = @At("TAIL"))
	private void etfu$registerOffhandWatcher(CallbackInfo ci) {
		if (Offhand.isEnabled()) {
			this.dataWatcher.addObjectByDataType(ConfigFunctions.offhandDataWatcherId, ITEM_STACK_DATA_TYPE);
			if (!ModernMovement.isPoseSystemEnabled()) {
				this.dataWatcher.addObject(ConfigFunctions.poseDataWatcherId, (byte) 0);
			}
		}
	}

	@Inject(method = "onUpdate", at = @At("TAIL"))
	private void etfu$syncOffhand(CallbackInfo ci) {
		if (!Offhand.isEnabled() || this.worldObj.isRemote) {
			return;
		}
		ItemStack current = Offhand.getOffhand((EntityPlayer) (Object) this);
		ItemStack watched = this.dataWatcher.getWatchableObjectItemStack(ConfigFunctions.offhandDataWatcherId);
		if (!ItemStack.areItemStacksEqual(current, watched)) {
			this.dataWatcher.updateObject(ConfigFunctions.offhandDataWatcherId, ItemStack.copyItemStack(current));
		}
	}

	@Override
	public ItemStack etfu$getWatchedOffhand() {
		return Offhand.isEnabled() ? this.dataWatcher.getWatchableObjectItemStack(ConfigFunctions.offhandDataWatcherId) : null;
	}

	/**
	 * The server and the local player know the using hand themselves; the other players on a client read the synced
	 * bit (isClientWorld is true for the two that decide).
	 */
	@Override
	public ItemStack etfu$getItemInUse() {
		return this.itemInUse;
	}

	@Override
	public Hand etfu$getUsingHand() {
		if (this.isClientWorld()) {
			return etfu$usingHand;
		}
		return Offhand.readPoseBit((EntityPlayer) (Object) this, Offhand.USING_OFFHAND_BIT) ? Hand.OFF : Hand.MAIN;
	}

	@Override
	public void etfu$setUsingHand(Hand hand) {
		etfu$usingHand = hand;
		Offhand.setPoseBit((EntityPlayer) (Object) this, Offhand.USING_OFFHAND_BIT, hand == Hand.OFF);
	}

	/** The item-in-use check compares against the hand that is using it, not always the main hand. */
	@ModifyExpressionValue(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;", ordinal = 0))
	private ItemStack etfu$itemOfTheUsingHand(ItemStack current) {
		return etfu$usingHand == Hand.OFF && Offhand.isEnabled() ? Offhand.getOffhandOf((EntityPlayer) (Object) this) : current;
	}

	/** A new use belongs to the main hand unless the off-hand code marks it afterwards. */
	@Inject(method = "setItemInUse", at = @At("HEAD"))
	private void etfu$newUseIsMainHand(ItemStack stack, int duration, CallbackInfo ci) {
		if (stack != this.itemInUse) {
			etfu$setUsingHand(Hand.MAIN);
		}
	}

	@Inject(method = "clearItemInUse", at = @At("TAIL"))
	private void etfu$useEnded(CallbackInfo ci) {
		etfu$setUsingHand(Hand.MAIN);
	}

	/** Finishing a use from the off-hand puts the result (bowl, bottle) back into the off-hand. */
	@Inject(method = "onItemUseFinish", at = @At("HEAD"), cancellable = true)
	private void etfu$finishOffhandUse(CallbackInfo ci) {
		if (etfu$usingHand != Hand.OFF || !Offhand.isEnabled()) {
			return;
		}
		ci.cancel();
		if (this.itemInUse == null) {
			return;
		}
		EntityPlayer self = (EntityPlayer) (Object) this;
		updateItemUse(this.itemInUse, 16);
		int size = this.itemInUse.stackSize;
		ItemStack result = this.itemInUse.onFoodEaten(this.worldObj, self);
		result = ForgeEventFactory.onItemUseFinish(self, this.itemInUse, this.itemInUseCount, result);
		if (result != this.itemInUse || result != null && result.stackSize != size) {
			Offhand.setOffhand(self, result != null && result.stackSize == 0 ? null : result);
		}
		clearItemInUse();
	}
}
