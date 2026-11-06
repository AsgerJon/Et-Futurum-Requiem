package io.github.asgerjon.mcfly.sculk;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 26.3's sculk shrieker (`SculkShriekerBlock`; SPEC-modern-movement.md §90.9, step H4), a new id: meta 1 shrieking, 2
 * `can_summon`. 3 and 3, the hoe, its sound type, the bottom half's collision; only to Silk Touch it drops itself, else
 * 5 experience. A player stepping on it, or a sensor's clicking it hears, sets it shrieking for 90 ticks; its end (or
 * its breaking while shrieking) may answer with the warden's reply and Darkness.
 */
public class SculkShriekerBlock extends McFlyBlock implements ModelledBlock {

	public static final int SHRIEKING = 1, CAN_SUMMON = 2;
	public static final double TOP_Y = 0.5D;

	@SideOnly(Side.CLIENT)
	private Map<String, IIcon> icons;

	public SculkShriekerBlock() {
		super(Material.ground);
		setNames("sculk_shrieker");
		strength(3.0F, 3.0F).tool("hoe", 0).sound(McFlySoundType.SCULK_SHRIEKER).colour(MapColor.blackColor);
		setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
	}

	public static boolean shrieking(int meta) {
		return (meta & SHRIEKING) != 0;
	}

	public static boolean canSummon(int meta) {
		return (meta & CAN_SUMMON) != 0;
	}

	private static SculkShriekerBlockEntity shrieker(IBlockAccess world, int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile instanceof SculkShriekerBlockEntity ? (SculkShriekerBlockEntity) tile : null;
	}

	/** 26.3's `stepOn`: a player (or what's theirs) on the ground on it tries a shriek. */
	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		if (world.isRemote || !entity.onGround) {
			return;
		}
		SculkShriekerBlockEntity shrieker = shrieker(world, x, y, z);
		if (shrieker != null) {
			shrieker.tryShriek(SculkShriekerBlockEntity.tryGetPlayer(entity));
		}
	}

	/** 26.3's `tick`: the shriek's end, then the response. */
	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		int meta = world.getBlockMetadata(x, y, z);
		if (!world.isRemote && shrieking(meta)) {
			world.setBlockMetadataWithNotify(x, y, z, meta & ~SHRIEKING, 3);
			SculkShriekerBlockEntity shrieker = shrieker(world, x, y, z);
			if (shrieker != null) {
				shrieker.tryRespond();
			}
		}
	}

	/** 26.3's `preRemoveSideEffects`: broken while shrieking, it responds. */
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if (!world.isRemote && shrieking(meta)) {
			SculkShriekerBlockEntity shrieker = shrieker(world, x, y, z);
			if (shrieker != null) {
				shrieker.tryRespond();
			}
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	@Override
	public boolean hasTileEntity(int meta) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		return new SculkShriekerBlockEntity();
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return null;
	}

	@Override
	public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int meta) {
		return true;
	}

	@Override
	public int getExpDrop(IBlockAccess world, int meta, int fortune) {
		return 5;
	}

	@Override
	public int damageDropped(int meta) {
		return 0;
	}

	@Override
	protected net.minecraft.item.ItemStack createStackedBlock(int meta) {
		return new net.minecraft.item.ItemStack(this);
	}

	// ---------------------------------------------------------------- the look

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
		return Collections.singletonList(new ModelPart(canSummon(world.getBlockMetadata(x, y, z)) ? "sculk_shrieker_can_summon" : "sculk_shrieker"));
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(new ModelPart("sculk_shrieker"));
	}

	private static final String[] TEXTURES = {"sculk_shrieker_bottom", "sculk_shrieker_side", "sculk_shrieker_top", "sculk_shrieker_inner_top",
		"sculk_shrieker_can_summon_inner_top"};

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		IIcon icon = icons.get(texture.substring(texture.lastIndexOf('/') + 1));
		return icon != null ? icon : blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		icons = new HashMap<>();
		for (String texture : TEXTURES) {
			icons.put(texture, register.registerIcon(texture));
		}
		blockIcon = icons.get("sculk_shrieker_bottom");
	}
}
