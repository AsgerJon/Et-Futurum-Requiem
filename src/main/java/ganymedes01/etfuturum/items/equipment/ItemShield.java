package ganymedes01.etfuturum.items.equipment;

import ganymedes01.etfuturum.combat.ICombatPlayer;
import ganymedes01.etfuturum.items.BaseItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

/**
 * The shield (spec §46.9): raised with right click, in either hand, it blocks hits from the front once up for 5
 * ticks (combat.ShieldBlocking). 336 durability, repaired with planks.
 */
public class ItemShield extends BaseItem {

	/** Modern raises it for as long as the button is held. */
	public static final int USE_DURATION = 72000;
	public static final int BLOCK_DELAY = 5;

	public ItemShield() {
		super("shield");
		setMaxStackSize(1);
		setMaxDamage(336);
		setFull3D();
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		return EnumAction.block;
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return USE_DURATION;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (!((ICombatPlayer) player).etfu$getCooldowns().isCoolingDown(this)) {
			player.setItemInUse(stack, getMaxItemUseDuration(stack));
		}
		return stack;
	}

	@Override
	public boolean getIsRepairable(ItemStack shield, ItemStack material) {
		for (int id : OreDictionary.getOreIDs(material)) {
			if (OreDictionary.getOreName(id).equals("plankWood")) {
				return true;
			}
		}
		return false;
	}
}
