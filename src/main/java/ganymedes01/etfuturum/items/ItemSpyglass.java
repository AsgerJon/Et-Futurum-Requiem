package ganymedes01.etfuturum.items;

import io.github.asgerjon.mcfly.registry.McFlyItems;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * The spyglass (SPEC-modern-movement.md §64.5; 26.3 {@code SpyglassItem}): used at once, for up to 1200 ticks, a sound
 * as it goes up and one as it comes down. Looking through it (the zoom, the scope, the slower mouse) is the client's
 * ({@code client.Spyglass}); the arm raised to the eye is the model's.
 */
public class ItemSpyglass extends BaseItem {

	public static final int USE_DURATION = 1200;
	public static final String USE_SOUND = Reference.MCAssetVer + ":item.spyglass.use";
	public static final String STOP_SOUND = Reference.MCAssetVer + ":item.spyglass.stop_using";

	public ItemSpyglass() {
		super("spyglass");
		setMaxStackSize(1);
	}

	/** 26.3 {@code Player.isScoping}: using a spyglass. */
	public static boolean isScoping(EntityPlayer player) {
		if (player == null || !McFlyItems.SPYGLASS.isEnabled() || !player.isUsingItem()) {
			return false;
		}
		ItemStack using = player.getItemInUse();
		return using != null && using.getItem() == McFlyItems.SPYGLASS.get();
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return USE_DURATION;
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		return EnumAction.none;
	}

	/** Both sides run it: the player's own sound plays on its client, the others hear the server's. */
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		player.playSound(USE_SOUND, 1.0F, 1.0F);
		player.setItemInUse(stack, getMaxItemUseDuration(stack));
		return stack;
	}

	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
		player.playSound(STOP_SOUND, 1.0F, 1.0F);
	}

	@Override
	public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
		player.playSound(STOP_SOUND, 1.0F, 1.0F);
		return stack;
	}
}
