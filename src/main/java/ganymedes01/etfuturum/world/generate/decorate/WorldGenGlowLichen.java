package ganymedes01.etfuturum.world.generate.decorate;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

import static net.minecraft.world.EnumSkyBlock.Sky;

public class WorldGenGlowLichen extends WorldGenerator {

    private final Block glowLichen;

    public WorldGenGlowLichen(Block glowLichen) {
        this.glowLichen = glowLichen;
    }
    @Override
    public boolean generate(World world, Random rand, int x, int y, int z)
    {
        if (world.isAirBlock(x, y, z) && world.getSavedLightValue(Sky, x, y, z) <= 3) {
            for (int side = 0; side < 6; side++) {
                ForgeDirection dir = ForgeDirection.getOrientation(side);
                int bx = x + dir.offsetX;
                int by = y + dir.offsetY;
                int bz = z + dir.offsetZ;

                Block target = world.getBlock(bx, by, bz);
                if (target != null && target.isSideSolid(world, bx, by, bz, dir.getOpposite()))
                {
                    int meta = 1 << side;
                    world.setBlock(x, y, z, glowLichen);

                    // McFly's lichen (spec §90.3): its faces and bonemeal spread
                    if (world.getBlock(x, y, z) instanceof io.github.asgerjon.mcfly.caves.GlowLichenBlock lichen)
                    {
                        lichen.setFaces(world, x, y, z, meta);
                        for (int i = 0; i < rand.nextInt(7); i++)
                        {
                            lichen.func_149853_b(world, rand, x, y, z);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
