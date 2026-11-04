package ganymedes01.etfuturum.items;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.world.LodestoneRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * The lodestone compass (SPEC-modern-movement.md §57): 1.7.10's own compass with 26.3's lodestone tracker as NBT, as
 * 26.3 keeps it on its compass. Having the tracker, whatever is in it, makes a lodestone compass.
 */
public final class LodestoneCompass {

	/** 1 when tracked (the lodestone's loss is seen); its presence makes a lodestone compass. */
	public static final String TRACKED = "LodestoneTracked";
	/** The target, {X, Y, Z}, and its dimension number; both missing when there is none. */
	public static final String POS = "LodestonePos", DIMENSION = "LodestoneDimension";
	public static final String LOCK_SOUND = McFly.MC_ASSETS + ":item.lodestone_compass.lock";

	private LodestoneCompass() {
	}

	public static boolean isLodestoneCompass(ItemStack stack) {
		return stack != null && stack.hasTagCompound() && stack.getTagCompound().hasKey(TRACKED);
	}

	/** Gives the stack the tracker; target null for none. */
	public static void setTracker(ItemStack stack, boolean tracked, int[] target, int dimension) {
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		NBTTagCompound tag = stack.getTagCompound();
		tag.setByte(TRACKED, (byte) (tracked ? 1 : 0));
		if (target == null) {
			tag.removeTag(POS);
			tag.removeTag(DIMENSION);
		} else {
			NBTTagCompound pos = new NBTTagCompound();
			pos.setInteger("X", target[0]);
			pos.setInteger("Y", target[1]);
			pos.setInteger("Z", target[2]);
			tag.setTag(POS, pos);
			tag.setInteger(DIMENSION, dimension);
		}
	}

	public static boolean isLodestone(World world, int x, int y, int z) {
		return McFlyBlocks.LODESTONE.isEnabled() && world.getBlock(x, y, z) == McFlyBlocks.LODESTONE.get();
	}

	/**
	 * A compass used on the lodestone at x, y, z, as 26.3's CompassItem.useOn: one compass in survival is changed where
	 * it is; otherwise one is copied with the target, taken from the stack (not in creative) and put in the inventory,
	 * or dropped when there's no room. The client only answers "used" and waits for the server's stacks.
	 */
	public static boolean lock(ItemStack stack, EntityPlayer player, World world, int x, int y, int z) {
		if (world.isRemote) {
			return true;
		}
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, LOCK_SOUND, 1.0F, 1.0F);
		int[] target = {x, y, z};
		int dimension = world.provider.dimensionId;
		if (!player.capabilities.isCreativeMode && stack.stackSize == 1) {
			setTracker(stack, true, target, dimension);
		} else {
			ItemStack lodestoneCompass = stack.copy();
			lodestoneCompass.stackSize = 1;
			setTracker(lodestoneCompass, true, target, dimension);
			if (!player.capabilities.isCreativeMode) {
				stack.stackSize--;
			}
			if (!player.inventory.addItemStackToInventory(lodestoneCompass)) {
				player.dropPlayerItemWithRandomChoice(lodestoneCompass, false);
			}
			// after a block use 1.7.10 resends only the held slot and marks every other change as sent, so the
			// copy's slot never reached the player's client: send it now
			player.openContainer.detectAndSendChanges();
		}
		return true;
	}

	/**
	 * 26.3's LodestoneTracker.tick, on the server each tick for a compass a player or other living entity has: a tracked
	 * target in this dimension goes when its lodestone is gone. Only the target's two tags go, so every compass that
	 * lost its lodestone is the same and they stack (§57.4). No height bounds of EFR's own: 1.7.10 has nothing outside
	 * 0..255, so such a target goes all the same, and a taller world's lodestone there works (§57.2).
	 */
	public static void tick(ItemStack stack, World world) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null || !tag.hasKey(TRACKED) || tag.getByte(TRACKED) == 0 || !tag.hasKey(POS)
			|| tag.getInteger(DIMENSION) != world.provider.dimensionId) {
			return;
		}
		NBTTagCompound pos = tag.getCompoundTag(POS);
		if (!hasLodestone(world, pos.getInteger("X"), pos.getInteger("Y"), pos.getInteger("Z"))) {
			tag.removeTag(POS);
			tag.removeTag(DIMENSION);
		}
	}

	/** A loaded chunk is asked directly; an unloaded one through the record, without loading it, as 26.3 does. */
	private static boolean hasLodestone(World world, int x, int y, int z) {
		if (world.getChunkProvider().chunkExists(x >> 4, z >> 4)) {
			return isLodestone(world, x, y, z);
		}
		return LodestoneRecord.get(world).contains(x, y, z);
	}
}
