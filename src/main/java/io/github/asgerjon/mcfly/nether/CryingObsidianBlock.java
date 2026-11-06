package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.particle.McFlyParticlesClient;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/**
 * Crying obsidian (SPEC-modern-movement.md §88.3), 26.3's `CryingObsidianBlock`: 50 and 1200, a diamond pickaxe, light
 * 10, not moved by pistons (26.3's `PistonBaseBlock.isPushable` refuses it, as obsidian), and its tears: in 1 of 5
 * display ticks a random side but the top, a tear there unless the block beside it covers that face.
 */
public class CryingObsidianBlock extends McFlyBlock {

	public static final String TEAR = "minecraft:dripping_obsidian_tear";

	public CryingObsidianBlock() {
		super(Material.rock);
		setNames("crying_obsidian");
		setHardness(50.0F);
		this.blockResistance = 1200.0F * 5.0F;
		setStepSound(soundTypePiston);
		setLightLevel(10.0F / 15.0F);
		setHarvestLevel("pickaxe", 3);
	}

	@Override
	public int getMobilityFlag() {
		return 2;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.obsidianColor;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(World world, int x, int y, int z, Random random) {
		if (random.nextInt(5) != 0) {
			return;
		}
		ForgeDirection dir = ForgeDirection.getOrientation(random.nextInt(6));
		if (dir == ForgeDirection.UP) {
			return;
		}
		int nx = x + dir.offsetX, ny = y + dir.offsetY, nz = z + dir.offsetZ;
		if (world.getBlock(nx, ny, nz).isSideSolid(world, nx, ny, nz, dir.getOpposite())) {
			return;
		}
		double px = dir.offsetX == 0 ? random.nextDouble() : 0.5D + dir.offsetX * 0.6D;
		double py = dir.offsetY == 0 ? random.nextDouble() : 0.5D + dir.offsetY * 0.6D;
		double pz = dir.offsetZ == 0 ? random.nextDouble() : 0.5D + dir.offsetZ * 0.6D;
		McFlyParticlesClient.add(TEAR, x + px, y + py, z + pz, 0.0D, 0.0D, 0.0D);
	}
}
