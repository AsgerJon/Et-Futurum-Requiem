package ganymedes01.etfuturum.client.gui.options;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * 26.3's creative tab Operator Utilities (spec §54.12), as far as 1.7.10 and EFR have its items: the command block, the
 * command block minecart, the barrier and the light blocks, 15 down to 0. The items keep their own tabs (none for the
 * first two), so they stay out of the search tab. Shown only when ControlOptions.operatorTabShown()
 * (modernoptions.client.MixinGuiContainerCreative); made on the client only.
 */
public class CreativeTabOperator extends CreativeTabs {

	public static CreativeTabOperator tab;

	public CreativeTabOperator() {
		super("op");
	}

	@Override
	public Item getTabIconItem() {
		return Item.getItemFromBlock(Blocks.command_block);
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void displayAllReleventItems(List list) {
		list.add(new ItemStack(Blocks.command_block));
		list.add(new ItemStack(Items.command_block_minecart));
		if (ModBlocks.BARRIER.isEnabled()) {
			list.add(ModBlocks.BARRIER.newItemStack());
		}
		if (ModBlocks.LIGHT.isEnabled()) {
			for (int level = 15; level >= 0; level--) {
				list.add(new ItemStack(ModBlocks.LIGHT.get(), 1, level));
			}
		}
	}
}
