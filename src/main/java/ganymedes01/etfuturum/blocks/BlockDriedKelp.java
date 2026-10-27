package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

/**
 * Nine dried kelp (SPEC-modern-movement.md §29): a plain block that burns for 20 items (see ModFuels).
 */
public class BlockDriedKelp extends BaseBlock {

	@SideOnly(Side.CLIENT)
	private IIcon topIcon;

	@SideOnly(Side.CLIENT)
	private IIcon bottomIcon;

	public BlockDriedKelp() {
		super(Material.grass);
		setNames("dried_kelp_block");
		setHardness(0.5F);
		setResistance(2.5F);
		setStepSound(soundTypeGrass);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		blockIcon = register.registerIcon("dried_kelp_side");
		topIcon = register.registerIcon("dried_kelp_top");
		bottomIcon = register.registerIcon("dried_kelp_bottom");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return side == 0 ? bottomIcon : side == 1 ? topIcon : blockIcon;
	}
}
