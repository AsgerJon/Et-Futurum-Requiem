package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.particle.McFlyParticlesClient;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.BlockTorch;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The soul torch (SPEC-modern-movement.md §88.5), 26.3's `soul_torch` and `soul_wall_torch` in EFR's one block (1.7.10's
 * torch metas: 1 to 4 on a wall, 5 standing): light 10, wood's sounds, and each display tick 26.3's smoke and
 * `soul_fire_flame` at the torch's top (`TorchBlock` and `WallTorchBlock.animateTick`: 0.27 out from the wall, 0.22 up).
 */
public class SoulTorchBlock extends BlockTorch {

	public static final String FLAME = "minecraft:soul_fire_flame";

	public SoulTorchBlock() {
		setCreativeTab(McFlyTab.TAB);
		setBlockName(McFly.name("soul_torch"));
		setBlockTextureName("soul_torch");
		setHardness(0.0F);
		setLightLevel(10.0F / 15.0F);
		setStepSound(soundTypeWood);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(World world, int x, int y, int z, Random random) {
		int meta = world.getBlockMetadata(x, y, z);
		double px = x + 0.5D, py = y + 0.7D, pz = z + 0.5D;
		double up = 0.22D, out = 0.27D;
		switch (meta) {
			case 1:
				px -= out;
				py += up;
				break;
			case 2:
				px += out;
				py += up;
				break;
			case 3:
				pz -= out;
				py += up;
				break;
			case 4:
				pz += out;
				py += up;
				break;
			default:
				break;
		}
		world.spawnParticle("smoke", px, py, pz, 0.0D, 0.0D, 0.0D);
		McFlyParticlesClient.add(FLAME, px, py, pz, 0.0D, 0.0D, 0.0D);
	}
}
