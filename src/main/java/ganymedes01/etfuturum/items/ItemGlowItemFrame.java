package ganymedes01.etfuturum.items;

import ganymedes01.etfuturum.entities.ModernFrames;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** The glow item frame's item (SPEC-modern-movement.md §67.4): placed as an item frame is (`ModernFrames.place`). */
public class ItemGlowItemFrame extends BaseItem {

	public ItemGlowItemFrame() {
		super("glow_item_frame");
		setCreativeTab(CreativeTabs.tabDecorations);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		return ModernFrames.place(stack, player, world, x, y, z, side, true);
	}
}
