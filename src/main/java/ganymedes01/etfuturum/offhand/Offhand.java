package ganymedes01.etfuturum.offhand;

import ganymedes01.etfuturum.compat.ModsList;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.entity.EntityLivingBase;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.core.utils.Logger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * The off-hand (SPEC-modern-movement.md §36). One extra stack per player, reached as inventory index 40 and
 * container slot 45, swapped with the main hand by a key, used when the main hand has nothing to do.
 */
public final class Offhand {

	/** The off-hand's index in the player inventory, after the 36 main and 4 armour slots. */
	public static final int INVENTORY_INDEX = 40;
	/** Its slot in the player's inventory container, after the crafting, armour, main and hotbar slots. */
	public static final int CONTAINER_SLOT = 45;
	/** Its slot number in the saved inventory list, modern's number for the off-hand. */
	public static final int NBT_SLOT = 150;
	/** Where the slot sits in the inventory screen and the creative survival tab. */
	/**
	 * Inventory screen: modern's slot (77, 62) sits flush against the player box, whose right border is at x = 75 in
	 * its texture; 1.7.10's box is three pixels wider (border at x = 78), so the slot moves with it to x = 80.
	 */
	public static final int SCREEN_X = 80, SCREEN_Y = 62, CREATIVE_X = 35, CREATIVE_Y = 20;
	/** The client's swing request type for the off-hand (vanilla sends 1 for a main-hand swing). */
	public static final int OFFHAND_SWING_REQUEST = 2;
	/**
	 * The animation type the server sends for an off-hand swing (vanilla's main-hand swing is 0). Modern uses 3, but
	 * in 1.7.10 that is the server's "started eating" notice (EntityPlayerMP.setItemInUse sends it to the trackers and
	 * the player; the vanilla client ignores it), so the first free number is used instead.
	 */
	public static final int OFFHAND_SWING_ANIMATION = 6;
	/** Bits 4 and 5 of the pose byte (§36.3): the main arm is the left one; an ongoing use belongs to the off-hand. */
	public static final int LEFT_HANDED_BIT = 16, USING_OFFHAND_BIT = 32;

	private static boolean enabled;

	private Offhand() {
	}

	/** Called in preInit, once the configs and the mod list are known. */
	public static void init() {
		boolean otherOffhand = ModsList.BACKHAND.isLoaded() || ModsList.BATTLEGEAR_2.isLoaded();
		enabled = ConfigMixins.enableOffhand && !otherOffhand;
		if (ConfigMixins.enableOffhand && otherOffhand) {
			Logger.info("Et Futurum Requiem's off-hand is off because Backhand or Battlegear 2 is loaded; a player can only have one.");
		}
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static ItemStack getOffhand(EntityPlayer player) {
		if (!enabled || !(player.inventory instanceof IOffhandInventory)) {
			return null;
		}
		return ((IOffhandInventory) player.inventory).etfu$getOffhand();
	}

	public static void setOffhand(EntityPlayer player, ItemStack stack) {
		if (enabled && player.inventory instanceof IOffhandInventory) {
			((IOffhandInventory) player.inventory).etfu$setOffhand(stack);
		}
	}

	/**
	 * The off-hand stack of any player: the inventory's own on the server and for the player this client controls,
	 * the synced copy for the other players on a client (their inventories are empty here).
	 */
	public static ItemStack getOffhandOf(EntityPlayer player) {
		if (!enabled) {
			return null;
		}
		if (player.worldObj.isRemote && !player.isClientWorld()) {
			return player instanceof IOffhandPlayer ? ((IOffhandPlayer) player).etfu$getWatchedOffhand() : null;
		}
		return getOffhand(player);
	}

	/** The off-hand stack to draw for a player; see getOffhandOf. */
	public static ItemStack getOffhandForRender(EntityPlayer player, EntityPlayer clientPlayer) {
		return getOffhandOf(player);
	}

	/** The hand an ongoing item use belongs to (§36.2), the main hand when there is none. */
	public static Hand getUsingHand(EntityPlayer player) {
		return enabled && player instanceof IOffhandPlayer ? ((IOffhandPlayer) player).etfu$getUsingHand() : Hand.MAIN;
	}

	/**
	 * Swings a hand (§36.4): the entity's one swing animation, marked as this hand's if it (re)starts. The local
	 * player's request and the server's animation packet carry the hand in their type.
	 */
	public static void swing(EntityLivingBase entity, Hand hand) {
		if (enabled && entity instanceof IOffhandSwinger) {
			((IOffhandSwinger) entity).etfu$setNextSwingHand(hand);
		}
		entity.swingItem();
	}

	/** The hand of the current swing, or of the last one. */
	public static Hand getSwingingHand(EntityLivingBase entity) {
		return enabled && entity instanceof IOffhandSwinger ? ((IOffhandSwinger) entity).etfu$getSwingingHand() : Hand.MAIN;
	}

	/** Whether the swinging hand is on the left arm, which is where the swing animation then plays (§36.4). */
	public static boolean isLeftArmSwinging(EntityLivingBase entity) {
		if (!enabled || !(entity instanceof EntityPlayer)) {
			return false;
		}
		return getSwingingHand(entity).arm(isLeftHanded((EntityPlayer) entity)) == Arm.LEFT;
	}

	/** A bit of the synced pose byte (§36.3), false when the player has none. */
	public static boolean readPoseBit(EntityPlayer player, int bit) {
		if (!enabled || !(player instanceof IOffhandPlayer)) {
			return false;
		}
		return (player.getDataWatcher().getWatchableObjectByte(ConfigFunctions.poseDataWatcherId) & bit) != 0;
	}

	/** Sets a bit of the synced pose byte, on the server; the pose system's own bits are left alone. */
	public static void setPoseBit(EntityPlayer player, int bit, boolean on) {
		if (!enabled || !(player instanceof IOffhandPlayer) || player.worldObj.isRemote) {
			return;
		}
		byte current = player.getDataWatcher().getWatchableObjectByte(ConfigFunctions.poseDataWatcherId);
		byte value = (byte) (on ? current | bit : current & ~bit);
		if (value != current) {
			player.getDataWatcher().updateObject(ConfigFunctions.poseDataWatcherId, value);
		}
	}

	/**
	 * The swap key's work, on the server (§36.1): main-hand and off-hand items change places and any ongoing item use
	 * ends without finishing. The open container sends both slots on its next tick.
	 */
	public static void swapHands(EntityPlayer player) {
		if (!enabled) {
			return;
		}
		ItemStack main = player.inventory.getCurrentItem();
		player.inventory.mainInventory[player.inventory.currentItem] = getOffhand(player);
		setOffhand(player, main);
		player.clearItemInUse();
	}

	/** The inventory indices to search for ammunition: the off-hand first (§36.2), then the rest as before. */
	public static int[] slotsOffhandFirst(net.minecraft.inventory.IInventory inventory) {
		int size = inventory.getSizeInventory();
		if (!enabled || !(inventory instanceof IOffhandInventory)) {
			int[] slots = new int[size];
			for (int i = 0; i < size; i++) slots[i] = i;
			return slots;
		}
		int[] slots = new int[size + 1];
		slots[0] = INVENTORY_INDEX;
		for (int i = 0; i < size; i++) slots[i + 1] = i;
		return slots;
	}

	/** Whether this player's main hand is the left one (§37), synced as bit 4 of the pose byte. Set from stage 4 on. */
	public static boolean isLeftHanded(EntityPlayer player) {
		return readPoseBit(player, LEFT_HANDED_BIT);
	}
}
