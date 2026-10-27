package ganymedes01.etfuturum.offhand;

import net.minecraft.item.ItemStack;

/**
 * Per-player off-hand state on the entity (SPEC-modern-movement.md §36.2, §36.3): the copy of the off-hand stack
 * synced to everyone through the DataWatcher, and the hand an ongoing item use belongs to.
 */
public interface IOffhandPlayer {

	ItemStack etfu$getWatchedOffhand();

	/** The stack being used, on either side; EntityPlayer.getItemInUse() exists only on the client. */
	ItemStack etfu$getItemInUse();

	Hand etfu$getUsingHand();

	void etfu$setUsingHand(Hand hand);
}
