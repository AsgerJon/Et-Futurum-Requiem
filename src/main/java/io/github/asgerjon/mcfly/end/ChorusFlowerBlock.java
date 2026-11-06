package io.github.asgerjon.mcfly.end;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.RandomTicking;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 26.3's chorus flower (`ChorusFlowerBlock`; SPEC-modern-movement.md §84): meta its age, 0 to 5 (5 dead). Its random
 * tick grows it as 26.3's does (up, or branching to the sides at the next age, or dying), with 26.3's sounds; it stands
 * as 26.3's `canSurvive` says, else it breaks a tick later; it drops itself only when something broke it (26.3's loot
 * asks for an entity: a player, an explosion's source, a projectile), not when it falls for want of footing; a
 * projectile hitting it breaks it. 0.4 to break, wood sounds, popped by pistons, quicker with an axe.
 */
public class ChorusFlowerBlock extends McFlyBlock implements ModelledBlock, RandomTicking {

	private static final ForgeDirection[] HORIZONTAL = {ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST};

	@SideOnly(Side.CLIENT)
	private IIcon deadIcon;

	public ChorusFlowerBlock() {
		super(Material.plants);
		setNames("chorus_flower");
		setHardness(0.4F);
		this.blockResistance = 0.4F * 5.0F;
		setStepSound(soundTypeWood);
		setHarvestLevel("axe", 0);
		setTickRandomly(true);
	}

	private static Block plant() {
		return McFlyBlocks.CHORUS_PLANT.get();
	}

	// ---------------------------------------------------------------- growing (26.3's randomTick)

	@Override
	public void randomTick(World world, int x, int y, int z, Random random) {
		if (!world.isAirBlock(x, y + 1, z) || y + 1 >= world.getHeight()) {
			return;
		}
		int age = world.getBlockMetadata(x, y, z);
		if (age >= 5) {
			return;
		}
		boolean growUpwards = false;
		boolean pillarOnSupport = false;
		Block below = world.getBlock(x, y - 1, z);
		if (ChorusPlantBlock.supportsPlant(below)) {
			growUpwards = true;
		} else if (below == plant()) {
			int height = 1;
			for (int i = 0; i < 4; i++) {
				Block test = world.getBlock(x, y - (height + 1), z);
				if (test != plant()) {
					if (ChorusPlantBlock.supportsPlant(test)) {
						pillarOnSupport = true;
					}
					break;
				}
				height++;
			}
			if (height < 2 || height <= random.nextInt(pillarOnSupport ? 5 : 4)) {
				growUpwards = true;
			}
		} else if (world.isAirBlock(x, y - 1, z)) {
			growUpwards = true;
		}
		if (growUpwards && allNeighboursEmpty(world, x, y + 1, z, null) && world.isAirBlock(x, y + 2, z)) {
			world.setBlock(x, y, z, plant(), 0, 2);
			placeGrownFlower(world, x, y + 1, z, age);
		} else if (age < 4) {
			int tries = random.nextInt(4);
			if (pillarOnSupport) {
				tries++;
			}
			boolean branched = false;
			for (int i = 0; i < tries; i++) {
				ForgeDirection side = HORIZONTAL[random.nextInt(4)];
				int tx = x + side.offsetX;
				int tz = z + side.offsetZ;
				if (world.isAirBlock(tx, y, tz) && world.isAirBlock(tx, y - 1, tz) && allNeighboursEmpty(world, tx, y, tz, side.getOpposite())) {
					placeGrownFlower(world, tx, y, tz, age + 1);
					branched = true;
				}
			}
			if (branched) {
				world.setBlock(x, y, z, plant(), 0, 2);
			} else {
				placeDeadFlower(world, x, y, z);
			}
		} else {
			placeDeadFlower(world, x, y, z);
		}
	}

	/** 26.3's level event 1033: `block.chorus_flower.grow` there. */
	private void placeGrownFlower(World world, int x, int y, int z, int age) {
		world.setBlock(x, y, z, this, age, 2);
		world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, McFly.MC_ASSETS + ":block.chorus_flower.grow", 1.0F, 1.0F);
	}

	/** 26.3's level event 1034: `block.chorus_flower.death` there. */
	private void placeDeadFlower(World world, int x, int y, int z) {
		world.setBlock(x, y, z, this, 5, 2);
		world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, McFly.MC_ASSETS + ":block.chorus_flower.death", 1.0F, 1.0F);
	}

	private static boolean allNeighboursEmpty(World world, int x, int y, int z, ForgeDirection ignore) {
		for (ForgeDirection side : HORIZONTAL) {
			if (side != ignore && !world.isAirBlock(x + side.offsetX, y, z + side.offsetZ)) {
				return false;
			}
		}
		return true;
	}

	// ---------------------------------------------------------------- footing (26.3's canSurvive, tick)

	@Override
	public boolean canBlockStay(World world, int x, int y, int z) {
		Block below = world.getBlock(x, y - 1, z);
		if (below == plant() || ChorusPlantBlock.supportsPlant(below)) {
			return true;
		}
		if (!world.isAirBlock(x, y - 1, z)) {
			return false;
		}
		boolean oneNeighbour = false;
		for (ForgeDirection side : HORIZONTAL) {
			int nx = x + side.offsetX;
			int nz = z + side.offsetZ;
			if (world.getBlock(nx, y, nz) == plant()) {
				if (oneNeighbour) {
					return false;
				}
				oneNeighbour = true;
			} else if (!world.isAirBlock(nx, y, nz)) {
				return false;
			}
		}
		return oneNeighbour;
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return super.canPlaceBlockAt(world, x, y, z) && canBlockStay(world, x, y, z);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		if (!canBlockStay(world, x, y, z)) {
			world.scheduleBlockUpdate(x, y, z, this, 1);
		}
	}

	/** The scheduled tick only (the random one is `randomTick`): broken without drops when it can't stand. */
	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		if (!canBlockStay(world, x, y, z)) {
			world.func_147480_a(x, y, z, false);
		}
	}

	// ---------------------------------------------------------------- drops: only when something broke it

	@Override
	public void dropBlockAsItemWithChance(World world, int x, int y, int z, int meta, float chance, int fortune) {
		// 26.3's loot needs the entity that broke it; the drops with one are below
	}

	@Override
	public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta) {
		player.addExhaustion(0.025F);
		dropBlockAsItem(world, x, y, z, new ItemStack(this));
	}

	@Override
	public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
		// 26.3: dropped when the explosion has a source, and survives it (1 in its size)
		if (!world.isRemote && explosion.exploder != null && world.rand.nextFloat() < 1.0F / explosion.explosionSize) {
			dropBlockAsItem(world, x, y, z, new ItemStack(this));
		}
		super.onBlockExploded(world, x, y, z, explosion);
	}

	/** 26.3's `onProjectileHit`: a projectile that hits it breaks it (when mobs may change blocks), dropping it. */
	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		if (!world.isRemote && entity instanceof IProjectile && world.getGameRules().getGameRuleBooleanValue("mobGriefing")) {
			dropBlockAsItem(world, x, y, z, new ItemStack(this));
			world.func_147480_a(x, y, z, false);
		}
	}

	// ---------------------------------------------------------------- shape and look

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

	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		return itemParts(world.getBlockMetadata(x, y, z));
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(new ModelPart(meta >= 5 ? "chorus_flower_dead" : "chorus_flower"));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		if (texture.endsWith("chorus_flower_dead")) {
			return deadIcon;
		}
		return texture.endsWith("chorus_plant") ? McFlyBlocks.CHORUS_PLANT.get().getIcon(0, 0) : blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return meta >= 5 ? deadIcon : blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister icons) {
		blockIcon = icons.registerIcon("chorus_flower");
		deadIcon = icons.registerIcon("chorus_flower_dead");
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.purpleColor;
	}
}
