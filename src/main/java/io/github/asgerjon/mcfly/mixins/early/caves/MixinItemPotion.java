package io.github.asgerjon.mcfly.mixins.early.caves;

import io.github.asgerjon.mcfly.caves.MudBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A water bottle used on dirt makes mud (SPEC-modern-movement.md §90, step C), 26.3's `PotionItem.useOn`: the potion's
 * own use on a block, so that a use there is no drink (1.7.10's potion has none, and a cancelled click still drinks).
 */
@Mixin(ItemPotion.class)
public abstract class MixinItemPotion extends Item {

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		return MudBlocks.useWaterBottle(stack, player, world, x, y, z, side);
	}
}
