package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Soul fire (SPEC-modern-movement.md §88.6), 26.3's `SoulFireBlock`: only on soul sand or soul soil
 * (`#soul_fire_base_blocks`), gone when that goes; light 10; 2 damage a hit (fire's 1) and set alight; never spreading
 * or ageing, rain or not. Any fire on a soul base becomes this one (26.3's `BaseFireBlock.getState`;
 * `mixins.early.soulfire.MixinBlockFire`). On 1.7.10's `BlockFire` for its drawing (the fire render type, which on a
 * solid base is 26.3's floor flames alone) and its portal lighting; `soul_fire_0`, `soul_fire_1`.
 */
public class SoulFireBlock extends BlockFire {

	@SideOnly(Side.CLIENT)
	private IIcon[] icons;

	public SoulFireBlock() {
		setBlockName(McFly.name("soul_fire"));
		setBlockTextureName("soul_fire");
		setLightLevel(10.0F / 15.0F);
		setHardness(0.0F);
		setStepSound(soundTypeWood);
		disableStats();
	}

	/** Soul sand or McFly's soul soil: 26.3's soul fire base blocks. */
	public static boolean isSoulBase(Block block) {
		return block == Blocks.soul_sand || McFlyBlocks.SOUL_SOIL.isEnabled() && block == McFlyBlocks.SOUL_SOIL.get();
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return isSoulBase(world.getBlock(x, y - 1, z));
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		if (!canPlaceBlockAt(world, x, y, z)) {
			world.setBlockToAir(x, y, z);
		}
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		if (!canPlaceBlockAt(world, x, y, z)) {
			world.setBlockToAir(x, y, z);
		}
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		if (!world.isRemote && !entity.isImmuneToFire()) {
			entity.setFire(8);
			entity.attackEntityFrom(DamageSource.inFire, 2.0F);
		}
	}

	@Override
	public boolean isBurning(IBlockAccess world, int x, int y, int z) {
		return false;
	}

	@Override
	public boolean canCatchFire(IBlockAccess world, int x, int y, int z, net.minecraftforge.common.util.ForgeDirection face) {
		return false;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.lightBlueColor;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		icons = new IIcon[]{register.registerIcon("soul_fire_0"), register.registerIcon("soul_fire_1")};
		blockIcon = icons[0];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getFireIcon(int layer) {
		return icons[layer & 1];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return icons[0];
	}
}
