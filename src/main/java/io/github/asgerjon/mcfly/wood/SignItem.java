package io.github.asgerjon.mcfly.wood;

import io.github.asgerjon.mcfly.base.McFlySign;
import io.github.asgerjon.mcfly.nether.NetherLightItem;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * A sign's item (SPEC-modern-movement.md 91.8): 26.3's stacks of 16, placed by `SignPlacing`, named as its sign. The
 * vanilla woods' are items of their own (EFR's `item_sign_<wood>`); the new woods' the standing sign's item block
 * (`OfBlock`).
 */
public class SignItem extends Item {

	private final McFlySign sign;

	public SignItem(McFlySign sign) {
		this.sign = sign;
		setMaxStackSize(16);
		setCreativeTab(McFlyTab.TAB);
		setTextureName(sign.itemTexture);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		return SignPlacing.place(stack, player, world, x, y, z, side, sign.standingSign(), sign.wallSign());
	}

	@Override
	public String getUnlocalizedName() {
		return sign.getUnlocalizedName();
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return sign.getUnlocalizedName();
	}

	/** The new woods' sign item (the standing sign's item block) and every hanging sign's (91.9), shown flat. */
	public static class OfBlock extends NetherLightItem {

		public OfBlock(Block block) {
			super(block);
			setMaxStackSize(16);
		}

		@Override
		public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
			if (field_150939_a instanceof io.github.asgerjon.mcfly.base.McFlyHangingSign hanging) {
				// 26.3's `HangingSignItem` (91.9)
				return SignPlacing.placeHanging(stack, player, world, x, y, z, side, hanging.ceilingSign(), hanging.wallSign());
			}
			McFlySign sign = (McFlySign) field_150939_a;
			return SignPlacing.place(stack, player, world, x, y, z, side, sign.standingSign(), sign.wallSign());
		}
	}
}
