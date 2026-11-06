package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.IIcon;

/**
 * The item of a block 26.3 shows as a flat sprite (SPEC-modern-movement.md §88.5): the chain and the lanterns, whose
 * items are `item/generated` of their item textures, not the block's model.
 */
public class NetherLightItem extends ItemBlock {

	/** A block with a flat item: its item texture's name. */
	public interface Flat {
		String itemTexture();
	}

	@SideOnly(Side.CLIENT)
	private IIcon sprite;

	public NetherLightItem(Block block) {
		super(block);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getSpriteNumber() {
		return 1;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register) {
		sprite = register.registerIcon(((Flat) field_150939_a).itemTexture());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int damage) {
		return sprite;
	}
}
