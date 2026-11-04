package ganymedes01.etfuturum.items.equipment;

import io.github.asgerjon.mcfly.banner.Banners;
import io.github.asgerjon.mcfly.base.McFlyItem;
import ganymedes01.etfuturum.combat.ICombatPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

/**
 * The shield (spec §46.9): raised with right click, in either hand, it blocks hits from the front once up for 5
 * ticks (combat.ShieldBlocking). 336 durability, repaired with planks.
 */
public class ItemShield extends McFlyItem {

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

	/** Named for its banner's colour ("Red Shield"), as 26.3's ShieldItem.getName (spec §66.9). */
	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		int base = Banners.shieldBase(stack);
		if (base < 0) {
			return super.getItemStackDisplayName(stack);
		}
		return net.minecraft.util.StatCollector.translateToLocal("item.etfuturum.shield." + ganymedes01.etfuturum.recipes.ModRecipes.dye_names[base] + ".name");
	}

	/** Its banner's layers, as the banner lists them. */
	@Override
	@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void addInformation(ItemStack stack, EntityPlayer player, java.util.List tooltip, boolean advanced) {
		super.addInformation(stack, player, tooltip, advanced);
		Banners.addLayerLines(stack, tooltip);
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
