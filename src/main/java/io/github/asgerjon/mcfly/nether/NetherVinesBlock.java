package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.RandomTicking;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Weeping and twisting vines (SPEC-modern-movement.md §88.7), 26.3's `GrowingPlantHeadBlock` and `BodyBlock` in EFR's
 * one block a kind: the head is the vine with none of its kind further on (down for weeping, up for twisting), the
 * rest the body. The meta is the head's age, 0 to 15 for 26.3's 0 to 25: placed 0 to 14 at random; each random tick a
 * head under 15 grows one on with 26.3's 0.1, the new head a year older 15 times in 25 (26.3's column lengths); bonemeal
 * grows 26.3's chain (each next one at 0.826 of the last's chance); shears on the head stop it. Held by its kind or a
 * solid face behind it; climbable; dropped by shears or Silk Touch, else at 0.33, 0.55, 0.77, 1 by Fortune. Drawn with
 * 26.3's `cross` models, head and plant.
 */
public class NetherVinesBlock extends Block implements ModelledBlock, NetherLightItem.Flat, IGrowable, IShearable, RandomTicking {

	public static final int MAX_AGE = 15;
	private final String name;
	private final ForgeDirection growth;
	@SideOnly(Side.CLIENT)
	private IIcon body;

	public NetherVinesBlock(String name, ForgeDirection growth) {
		super(Material.plants);
		this.name = name;
		this.growth = growth;
		setCreativeTab(McFlyTab.TAB);
		setBlockName(McFly.name(name));
		setBlockTextureName(name);
		setHardness(0.0F);
		setStepSound(McFlySoundType.WEEPING_VINES);
		setTickRandomly(true);
		setBlockBounds(0.25F, 0.0F, 0.25F, 0.75F, 1.0F, 0.75F);
	}

	public boolean isHead(IBlockAccess world, int x, int y, int z) {
		return world.getBlock(x + growth.offsetX, y + growth.offsetY, z + growth.offsetZ) != this;
	}

	private boolean held(World world, int x, int y, int z) {
		ForgeDirection back = growth.getOpposite();
		int bx = x + back.offsetX, by = y + back.offsetY, bz = z + back.offsetZ;
		Block behind = world.getBlock(bx, by, bz);
		return behind == this || behind.isSideSolid(world, bx, by, bz, growth);
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return super.canPlaceBlockAt(world, x, y, z) && held(world, x, y, z);
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		world.setBlockMetadataWithNotify(x, y, z, world.rand.nextInt(MAX_AGE), 2);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		if (!held(world, x, y, z)) {
			dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
			world.setBlockToAir(x, y, z);
		}
	}

	/** One more on the head: the new head a year older 15 times in 25. */
	private boolean growOn(World world, int x, int y, int z, Random random) {
		int nx = x + growth.offsetX, ny = y + growth.offsetY, nz = z + growth.offsetZ;
		if (ny < 0 || ny >= world.getHeight() || !world.isAirBlock(nx, ny, nz)) {
			return false;
		}
		int age = world.getBlockMetadata(x, y, z);
		world.setBlock(nx, ny, nz, this, Math.min(MAX_AGE, age + (random.nextInt(25) < 15 ? 1 : 0)), 3);
		return true;
	}

	@Override
	public void randomTick(World world, int x, int y, int z, Random random) {
		if (isHead(world, x, y, z) && world.getBlockMetadata(x, y, z) < MAX_AGE && random.nextDouble() < 0.1D) {
			growOn(world, x, y, z, random);
		}
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		randomTick(world, x, y, z, random);
	}

	/** The head of the column this vine is in. */
	private int[] head(World world, int x, int y, int z) {
		while (world.getBlock(x + growth.offsetX, y + growth.offsetY, z + growth.offsetZ) == this) {
			x += growth.offsetX;
			y += growth.offsetY;
			z += growth.offsetZ;
		}
		return new int[]{x, y, z};
	}

	// ---------------------------------------------------------------- bonemeal (IGrowable)

	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
		int[] h = head(world, x, y, z);
		return world.isAirBlock(h[0] + growth.offsetX, h[1] + growth.offsetY, h[2] + growth.offsetZ);
	}

	@Override
	public boolean func_149852_a(World world, Random random, int x, int y, int z) {
		return true;
	}

	@Override
	public void func_149853_b(World world, Random random, int x, int y, int z) {
		int[] h = head(world, x, y, z);
		double chance = 1.0D;
		while (random.nextDouble() < chance) {
			chance *= 0.826D;
			if (!growOn(world, h[0], h[1], h[2], random)) {
				return;
			}
			h[0] += growth.offsetX;
			h[1] += growth.offsetY;
			h[2] += growth.offsetZ;
		}
	}

	// ---------------------------------------------------------------- shears, drops

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		ItemStack held = player.getHeldItem();
		if (held == null || !(held.getItem() instanceof ItemShears) || !isHead(world, x, y, z) || world.getBlockMetadata(x, y, z) == MAX_AGE) {
			return false;
		}
		if (!world.isRemote) {
			world.setBlockMetadataWithNotify(x, y, z, MAX_AGE, 3);
			world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":block.growing_plant.crop", 1.0F, 1.0F);
			held.damageItem(1, player);
		}
		return true;
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return random.nextFloat() < new float[]{0.33F, 0.55F, 0.77F, 1.0F}[Math.max(0, Math.min(3, fortune))] ? Item.getItemFromBlock(this) : null;
	}

	@Override
	public int damageDropped(int meta) {
		return 0;
	}

	@Override
	protected boolean canSilkHarvest() {
		return true;
	}

	@Override
	protected ItemStack createStackedBlock(int meta) {
		return new ItemStack(this);
	}

	@Override
	public boolean isShearable(ItemStack item, IBlockAccess world, int x, int y, int z) {
		return true;
	}

	@Override
	public ArrayList<ItemStack> onSheared(ItemStack item, IBlockAccess world, int x, int y, int z, int fortune) {
		ArrayList<ItemStack> drops = new ArrayList<>();
		drops.add(new ItemStack(this));
		return drops;
	}

	@Override
	public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
		return true;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return null;
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
		return McFlyRenderIds.MODEL_FLAT_ITEM;
	}

	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		return Collections.singletonList(new ModelPart(isHead(world, x, y, z) ? name : name + "_plant"));
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(new ModelPart(name));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		super.registerBlockIcons(register);
		body = register.registerIcon(name + "_plant");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		return texture.endsWith("_plant") ? body : blockIcon;
	}

	@Override
	public String itemTexture() {
		return name;
	}
}
