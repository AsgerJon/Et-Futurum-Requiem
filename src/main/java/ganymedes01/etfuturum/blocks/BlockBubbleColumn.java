package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.client.particle.CustomParticles;
import ganymedes01.etfuturum.core.utils.IInitAction;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.lib.RenderIDs;
import ganymedes01.etfuturum.movement.BubbleColumns;
import ganymedes01.etfuturum.movement.IBubbleColumnEntity;
import ganymedes01.etfuturum.recipes.ModRecipes;
import ganymedes01.etfuturum.world.EtFuturumWorldListener;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Map;
import java.util.Random;

public class BlockBubbleColumn extends BaseBlock implements IInitAction {

	private static final int[][] HORIZONTAL_OFFSETS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private final Map<Block, Integer> supportBlockMeta = new Object2IntOpenHashMap<>();
	private int lastColumnSoundTick = -1;

	/**
	 * @param upBlocks   blocks that produce upward columns (meta 0), e.g. soul sand
	 * @param downBlocks blocks that produce downward/whirlpool columns (meta 8), e.g. magma
	 */
	public BlockBubbleColumn(Block[] upBlocks, Block[] downBlocks) {
		super(Material.water);
		for (Block b : upBlocks) supportBlockMeta.put(b, 0);
		for (Block b : downBlocks) supportBlockMeta.put(b, 8);
		setLightOpacity(Blocks.water.getLightOpacity());
		setBlockName("bubble_column");
		setBlockBounds(0, 0, 0, 0, 0, 0);
	}

	public boolean isUp(int meta) {
		return meta == 0;
	}

	@Override
	public void randomDisplayTick(World worldIn, int x, int y, int z, Random random) {
		super.randomDisplayTick(worldIn, x, y, z, random);
		int particleSetting = Minecraft.getMinecraft().gameSettings.particleSetting;
		boolean up = isUp(worldIn.getBlockMetadata(x, y, z));
		if (up) {
			// Centered bubble: always spawned regardless of setting
			CustomParticles.spawnBubbleColumnUp(worldIn, x + 0.5, y, z + 0.5, 0, 0.04, 0);
			// Random bubble: always (All), 60% chance (Decreased), skip (Minimal)
			if (particleSetting == 0 || (particleSetting == 1 && random.nextFloat() < 0.6f)) {
				CustomParticles.spawnBubbleColumnUp(worldIn,
					x + random.nextFloat(), y + random.nextFloat(), z + random.nextFloat(), 0, 0.04, 0);
			}
		} else {
			// All: 50%, Decreased: 40%, Minimal: 25%
			boolean spawn = particleSetting == 0 ? random.nextInt(2) == 0
						  : particleSetting == 1 ? random.nextFloat() < 0.4f
						  : random.nextInt(4) == 0;
			if (spawn) {
				CustomParticles.spawnWaterCurrentDown(worldIn, x + 0.5, y + 0.8, z + 0.5);
			}
		}
		if (random.nextInt(200) == 0) {
			worldIn.playSound(x + random.nextFloat(), y + random.nextFloat(), z + random.nextFloat(),
					getBubblingNoise(worldIn, x, y, z, up ? 0 : 1, random), 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
		}
	}

	/**
	 * A cell acts on every entity whose hitbox overlaps it, once per tick (SPEC-modern-movement.md §28): the open top
	 * cell throws it out, any other cell carries it, and living entities take the push after their movement step. The
	 * server sends the surface splash; the client plays the inside sound for its own player when a cell first overlaps
	 * the middle of the hitbox.
	 */
	@Override
	public void onEntityCollidedWithBlock(World worldIn, int x, int y, int z, Entity entityIn) {
		// dont have particles trigger this; gets a little too crazy
		if (worldIn.isRemote && entityIn instanceof EntityFX) return;
		if (!BubbleColumns.isAffected(entityIn)) return;

		boolean drag = !isUp(worldIn.getBlockMetadata(x, y, z));
		boolean above = BubbleColumns.isOpenAbove(worldIn, x, y, z);
		if (entityIn instanceof IBubbleColumnEntity) {
			((IBubbleColumnEntity) entityIn).etfu$queueBubbleColumn(drag, above);
		} else if (above) {
			BubbleColumns.pushAbove(entityIn, drag);
		} else {
			BubbleColumns.pushInside(entityIn, drag);
		}

		if (above && worldIn instanceof WorldServer) {
			BubbleColumns.sendSurfaceParticles((WorldServer) worldIn, x, y, z);
		}

		// The local player only (isClientWorld is true for it alone on a client); named by its common type, since a
		// client-only type here would make the server's verifier load it when the block registry touches this class.
		if (worldIn.isRemote && entityIn instanceof EntityPlayer player && player.isClientWorld() && BubbleColumns.overlapsCore(player, y)) {
			int tick = player.ticksExisted;
			if (tick > 1 && lastColumnSoundTick != tick && lastColumnSoundTick != tick - 1) {
				player.playSound(Reference.MCAssetVer + ":" + "block.bubble_column." + (drag ? "whirlpool" : "upwards") + "_inside", 1, 1);
			}
			lastColumnSoundTick = tick;
		}
	}

	protected String getBubblingNoise(World world, int x, int y, int z, int meta, Random random) {
		return Reference.MCAssetVer + ":" + "block.bubble_column." + (isUp(meta) ? "upwards" : "whirlpool") + "_ambient";
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World worldIn, int x, int y, int z) {
		return null;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World worldIn, int x, int y, int z) {
		return null;
	}

	@Override
	public void onBlockAdded(World worldIn, int x, int y, int z) {
		manageColumn(worldIn, x, y, z);
	}

	@Override
	public void onNeighborBlockChange(World worldIn, int x, int y, int z, Block neighbor) {
		manageColumn(worldIn, x, y, z);
	}

	protected void manageColumn(World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		Block below = world.getBlock(x, y - 1, z);
		if (below == this) {
			// Inherit metadata from column below
			int belowMeta = world.getBlockMetadata(x, y - 1, z);
			if (belowMeta != meta) {
				world.setBlockMetadataWithNotify(x, y, z, belowMeta, 3);
				meta = belowMeta;
			}
		} else if (supportBlockMeta.containsKey(below)) {
			// Validate metadata matches the support block
			int expectedMeta = supportBlockMeta.get(below);
			if (expectedMeta != meta) {
				world.setBlockMetadataWithNotify(x, y, z, expectedMeta, 3);
				meta = expectedMeta;
			}
		} else {
			// No valid support below
			world.setBlock(x, y, z, Blocks.water);
			return;
		}
		if (isFullVanillaWater(world.getBlock(x, y + 1, z), world.getBlockMetadata(x, y + 1, z))) {
			world.setBlock(x, y + 1, z, this, meta, 3);
		}

		// Spread water to adjacent air blocks
		for (int[] offset : HORIZONTAL_OFFSETS) {
			Block adjacent = world.getBlock(x + offset[0], y, z + offset[1]);
			if (adjacent == Blocks.air) {
				world.setBlock(x + offset[0], y, z + offset[1], Blocks.flowing_water, 1, 3);
			}
		}
	}

	public static boolean isFullVanillaWater(Block block, int meta) {
		return meta == 0 && (block == Blocks.water || block == Blocks.flowing_water);
	}

	@Override
	public boolean canRenderInPass(int pass) {
		return pass == 1;
	}

	@Override
	public int getRenderBlockPass() {
		return 1;
	}

	@Override
	public int getRenderType() {
		return RenderIDs.BUBBLE_COLUMN;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public void postInitAction() {
		if (ModRecipes.validateItems(this)) {
			supportBlockMeta.forEach((block, meta) -> {
				if (ModRecipes.validateItems(block)) {
					EtFuturumWorldListener.bubbleColumnMap.put(block, meta);
				}
			});
		}
	}

	@Override
	public Item getItem(World worldIn, int x, int y, int z) {
		return Item.getItemFromBlock(Blocks.water);
	}
}
