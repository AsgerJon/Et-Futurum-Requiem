package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

/**
 * The chain (SPEC-modern-movement.md §88.5), 26.3's `iron_chain` (`ChainBlock`) in EFR's stored form: meta 0 along y, 1
 * along x, 2 along z, the clicked face's axis; 5 and 6, a pickaxe for drops, its sound; 3 pixels across; drawn from
 * 26.3's `iron_chain` model, its item a flat sprite (EFR's `chain` textures).
 */
public class ChainBlock extends McFlyBlock implements ModelledBlock, NetherLightItem.Flat {

	private static final float LOW = 6.5F / 16.0F, HIGH = 9.5F / 16.0F;

	public ChainBlock() {
		super(Material.iron);
		setNames("chain");
		strength(5.0F, 6.0F).tool("pickaxe", 0).sound(McFlySoundType.CHAIN);
	}

	public static int axis(int meta) {
		return meta == 1 || meta == 2 ? meta : 0;
	}

	@Override
	public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
		return side < 2 ? 0 : side < 4 ? 2 : 1;
	}

	@Override
	public int damageDropped(int meta) {
		return 0;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		switch (axis(world.getBlockMetadata(x, y, z))) {
			case 1:
				setBlockBounds(0.0F, LOW, LOW, 1.0F, HIGH, HIGH);
				break;
			case 2:
				setBlockBounds(LOW, LOW, 0.0F, HIGH, HIGH, 1.0F);
				break;
			default:
				setBlockBounds(LOW, 0.0F, LOW, HIGH, 1.0F, HIGH);
		}
	}

	@Override
	public void setBlockBoundsForItemRender() {
		setBlockBounds(LOW, 0.0F, LOW, HIGH, 1.0F, HIGH);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return super.getCollisionBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public int getRenderType() {
		return McFlyRenderIds.MODEL;
	}

	/** 26.3's blockstate: along y as the model is, x turned 90 and 90, z turned 90. */
	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		switch (axis(world.getBlockMetadata(x, y, z))) {
			case 1:
				return Collections.singletonList(new ModelPart("iron_chain", 90, 90, false));
			case 2:
				return Collections.singletonList(new ModelPart("iron_chain", 90, 0, false));
			default:
				return Collections.singletonList(new ModelPart("iron_chain"));
		}
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(new ModelPart("iron_chain"));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		return blockIcon;
	}

	@Override
	public String itemTexture() {
		return "chain";
	}
}
