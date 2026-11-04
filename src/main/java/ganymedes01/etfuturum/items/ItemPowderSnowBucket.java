package ganymedes01.etfuturum.items;

import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * The powder snow bucket (SPEC-modern-movement.md §62.6; 26.3 {@code SolidBucketItem}): placed as a block item places
 * its block, then an empty bucket is left (in creative it stays). Picking powder snow up is the empty bucket's, through
 * Forge's {@code FillBucketEvent} ({@code PowderSnowEvents}).
 */
public class ItemPowderSnowBucket extends BaseItem {

	public ItemPowderSnowBucket() {
		super("powder_snow_bucket");
		setMaxStackSize(1);
		setContainerItem(Items.bucket);
	}

	/** 1.7.10's {@code ItemBlock.onItemUse}, with powder snow, the bucket's sound and the empty bucket after. */
	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		if (!McFlyBlocks.POWDER_SNOW.isEnabled()) {
			return false;
		}
		Block snow = McFlyBlocks.POWDER_SNOW.get();
		Block clicked = world.getBlock(x, y, z);
		// 26.3: any cauldron takes it, full (the powder snow cauldron answers its own clicks; EFR's potion cauldron, which
		// 26.3 doesn't have, is left with its potion)
		if ((clicked == Blocks.cauldron || ModBlocks.LAVA_CAULDRON.isEnabled() && clicked == ModBlocks.LAVA_CAULDRON.get())
				&& McFlyBlocks.POWDER_SNOW_CAULDRON.isEnabled()) {
			if (!world.isRemote) {
				world.setBlock(x, y, z, McFlyBlocks.POWDER_SNOW_CAULDRON.get(), 3, 3);
				world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Reference.MCAssetVer + ":item.bucket.empty_powder_snow", 1.0F, 1.0F);
				if (!player.capabilities.isCreativeMode) {
					stack.func_150996_a(Items.bucket);
				}
			}
			return true;
		}
		if (clicked == Blocks.snow_layer && (world.getBlockMetadata(x, y, z) & 7) < 1) {
			side = 1;
		} else if (clicked != Blocks.vine && clicked != Blocks.tallgrass && clicked != Blocks.deadbush && !clicked.isReplaceable(world, x, y, z)) {
			switch (side) {
				case 0:
					y--;
					break;
				case 1:
					y++;
					break;
				case 2:
					z--;
					break;
				case 3:
					z++;
					break;
				case 4:
					x--;
					break;
				case 5:
					x++;
					break;
			}
		}
		if (stack.stackSize == 0 || !player.canPlayerEdit(x, y, z, side, stack)) {
			return false;
		}
		if (y >= 255 || !world.canPlaceEntityOnSide(snow, x, y, z, false, side, player, stack)) {
			return false;
		}
		if (!world.setBlock(x, y, z, snow, 0, 3)) {
			return false;
		}
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Reference.MCAssetVer + ":item.bucket.empty_powder_snow", 1.0F, 1.0F);
		if (!player.capabilities.isCreativeMode) {
			stack.func_150996_a(Items.bucket);
		}
		return true;
	}
}
