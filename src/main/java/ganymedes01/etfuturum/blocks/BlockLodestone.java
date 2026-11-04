package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.world.LodestoneRecord;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * 26.3's lodestone (SPEC-modern-movement.md §57): a compass used on it points to it. Iron's material: it needs a tool,
 * has the metal map colour, and a note block on it plays harp, as 26.3's.
 */
public class BlockLodestone extends Block {

	private IIcon topIcon;

	public BlockLodestone() {
		super(Material.iron);
		setHardness(3.5F);
		// 1.7.10's explosions read a fifth of three times this: 26.3's 3.5
		setResistance(3.5F * 5.0F / 3.0F);
		setHarvestLevel("pickaxe", 0);
		setBlockName(Utils.getUnlocalisedName("lodestone"));
		setBlockTextureName("lodestone");
		setCreativeTab(EtFuturum.creativeTabBlocks);
		Utils.setBlockSound(this, ModSounds.soundLodestone);
	}

	/** Pistons can't move it. */
	@Override
	public int getMobilityFlag() {
		return 2;
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		if (!world.isRemote) {
			LodestoneRecord.get(world).add(x, y, z);
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if (!world.isRemote) {
			LodestoneRecord.get(world).remove(x, y, z);
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		return side > 1 ? blockIcon : topIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		this.blockIcon = reg.registerIcon(this.getTextureName() + "_side");
		this.topIcon = reg.registerIcon(this.getTextureName() + "_top");
	}

}
