package io.github.asgerjon.mcfly.caves;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyMultiface;
import io.github.asgerjon.mcfly.base.McFlyMultifaceTile;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.nether.NetherLightItem;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemShears;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/**
 * Glow lichen as 26.3's (SPEC-modern-movement.md §90.3), EFR's id and tile entity: faces on McFly's multiface base; 0.2
 * hard, light 7, its own sound, fire 15 and 100, popped by pistons, the axe its tool and shears 2.0; sheared, one a
 * face; bonemeal spreads one face (26.3's same block, same plane, round the edge). Map colour: 26.3's glow lichen as
 * 1.7.10's nearest (silver).
 */
public class GlowLichenBlock extends McFlyMultiface implements IGrowable, NetherLightItem.Flat {

	/** EFR's tile entity, under its id `etfuturum.glow_lichen`. */
	public static final class Tile extends McFlyMultifaceTile {
	}

	private static final ModelPart[] PARTS = {new ModelPart("glow_lichen", 90, 0, true), new ModelPart("glow_lichen", 270, 0, true),
		new ModelPart("glow_lichen"), new ModelPart("glow_lichen", 0, 180, true), new ModelPart("glow_lichen", 0, 270, true),
		new ModelPart("glow_lichen", 0, 90, true)};

	public GlowLichenBlock() {
		super(Material.vine);
		setBlockName(McFly.name("glow_lichen"));
		setBlockTextureName("glow_lichen");
		setHardness(0.2F);
		setLightLevel(7.0F / 15.0F);
		setStepSound(McFlySoundType.GLOW_LICHEN);
		setHarvestLevel("axe", 0);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new Tile();
	}

	@Override
	protected ModelPart[] faceParts() {
		return PARTS;
	}

	@Override
	public String itemTexture() {
		return "glow_lichen";
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.silverColor;
	}

	@Override
	public int getMobilityFlag() {
		return 1;
	}

	/** Shears at 26.3's 2.0 (`#shears_minor_breaking_speed`); 1.7.10's shears are 1.0 on it. */
	@Override
	public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z) {
		float strength = ForgeHooks.blockStrength(this, player, world, x, y, z);
		return player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ItemShears ? strength * 2.0F : strength;
	}

	@Override
	public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return true;
	}

	@Override
	public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return 100;
	}

	@Override
	public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return 15;
	}

	// ---------------------------------------------------------------- bonemeal

	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
		return canSpread(world, x, y, z);
	}

	@Override
	public boolean func_149852_a(World world, Random random, int x, int y, int z) {
		return true;
	}

	@Override
	public void func_149853_b(World world, Random random, int x, int y, int z) {
		spreadFromRandomFace(world, x, y, z, random);
	}
}
