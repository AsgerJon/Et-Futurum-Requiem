package ganymedes01.etfuturum.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import java.util.List;

/**
 * The Netherite Upgrade smithing template (1.20+): the third input of a netherite upgrade at the smithing table,
 * consumed with the diamond gear and the ingot. Found in Nether fortress chests here, bastions in modern, and copied
 * with seven diamonds and netherrack. The tooltip is modern's: what it applies to and what it takes.
 */
public class ItemNetheriteUpgradeTemplate extends BaseItem {

	public ItemNetheriteUpgradeTemplate() {
		super("netherite_upgrade_smithing_template");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
		tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("item.etfuturum.smithing_template.name"));
		tooltip.add("");
		tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("item.etfuturum.smithing_template.applies_to"));
		tooltip.add(" " + EnumChatFormatting.BLUE + StatCollector.translateToLocal("item.etfuturum.smithing_template.netherite_upgrade.applies_to"));
		tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("item.etfuturum.smithing_template.ingredients"));
		tooltip.add(" " + EnumChatFormatting.BLUE + StatCollector.translateToLocal("item.etfuturum.smithing_template.netherite_upgrade.ingredients"));
	}
}
