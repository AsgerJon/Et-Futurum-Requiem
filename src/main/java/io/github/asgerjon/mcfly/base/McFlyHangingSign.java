package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.nether.NetherLightItem;
import io.github.asgerjon.mcfly.wood.HangingSigns;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.block.material.MapColor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * A wood's hanging sign (SPEC-modern-movement.md 91.9, step G; 26.3's `CeilingHangingSignBlock` and
 * `WallHangingSignBlock`), on a ceiling or between walls, as 1.7.10's `BlockSign` (so 91.8's sign code is its: the use,
 * sides, editor, wax). The ceiling sign's meta is its rotation, `attached` in its tile entity; the wall sign's meta its
 * facing (2 north, 3 south, 4 west, 5 east). Held by the block above's middle; the wall sign checks its walls only when
 * placed (26.3's). Made in pairs.
 */
public class McFlyHangingSign extends BlockSign implements NetherLightItem.Flat {

	public final boolean ceiling;
	public final String blockTexture;
	public final String itemTexture;
	public final String guiTexture;
	private final MapColor colour;
	private final Supplier<Block> planks;
	private final int planksMeta;
	private McFlyHangingSign other;

	public McFlyHangingSign(String name, boolean ceiling, String wood, Block.SoundType sound, MapColor colour, Supplier<Block> planks, int planksMeta) {
		super(HangingSigns.Tile.class, ceiling);
		this.ceiling = ceiling;
		this.blockTexture = "minecraft:" + wood + "_hanging_sign";
		this.itemTexture = "minecraft:" + wood + "_hanging_sign";
		this.guiTexture = "minecraft:textures/gui/hanging_signs/" + wood + ".png";
		this.colour = colour;
		this.planks = planks;
		this.planksMeta = planksMeta;
		setHardness(1.0F);
		this.blockResistance = 1.0F * 5.0F;
		setStepSound(sound);
		setBlockName(McFly.name(name));
		disableStats();
	}

	public static void pair(McFlyHangingSign ceiling, McFlyHangingSign wall) {
		ceiling.other = wall;
		wall.other = ceiling;
	}

	public McFlyHangingSign ceilingSign() {
		return ceiling ? this : other;
	}

	public McFlyHangingSign wallSign() {
		return ceiling ? other : this;
	}

	public Item signItem() {
		return Item.getItemFromBlock(ceilingSign());
	}

	@Override
	public String itemTexture() {
		return itemTexture;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return planks.get().getIcon(side, planksMeta);
	}

	@Override
	public MapColor getMapColor(int meta) {
		return colour;
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return signItem();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		return signItem();
	}

	@Override
	public String getItemIconName() {
		return itemTexture;
	}

	// ---------------------------------------------------------------- using it: chaining

	/**
	 * 26.3's `shouldTryToChainAnotherHangingSign`: a hanging sign item on a ceiling sign's underside, or on a wall sign's
	 * side without text (the clicked face off its facing's axis), places a sign instead of using this one.
	 */
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		ItemStack held = player.getCurrentEquippedItem();
		if (held != null && Block.getBlockFromItem(held.getItem()) instanceof McFlyHangingSign) {
			if (ceiling ? side == 0 : !HangingSigns.sameAxis(side, world.getBlockMetadata(x, y, z))) {
				return false;
			}
		}
		return super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
	}

	// ---------------------------------------------------------------- held, shapes, collision

	/** The ceiling sign falls with what holds it; the wall sign stays (26.3's `canSurvive` is the default). */
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		if (ceiling && !HangingSigns.heldFromAbove(world, x, y, z)) {
			dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
			world.setBlockToAir(x, y, z);
		}
	}

	/**
	 * 26.3's shapes: the ceiling sign at 0 and 8 its board along x (1 to 15, 2 deep, 10 high), at 4 and 12 along z, else a
	 * 10-by-10 column the block's height; the wall sign its plank (16 by 4, 2 thick, at the top) and board, as one box.
	 */
	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		float p = 1.0F / 16.0F;
		if (ceiling) {
			switch (meta) {
				case 0:
				case 8:
					setBlockBounds(p, 0.0F, 7 * p, 15 * p, 10 * p, 9 * p);
					break;
				case 4:
				case 12:
					setBlockBounds(7 * p, 0.0F, p, 9 * p, 10 * p, 15 * p);
					break;
				default:
					setBlockBounds(3 * p, 0.0F, 3 * p, 13 * p, 1.0F, 13 * p);
			}
		} else if (meta == 4 || meta == 5) {
			setBlockBounds(6 * p, 0.0F, 0.0F, 10 * p, 1.0F, 1.0F);
		} else {
			setBlockBounds(0.0F, 0.0F, 6 * p, 1.0F, 1.0F, 10 * p);
		}
	}

	/** No collision but the wall sign's plank (26.3's `WallHangingSignBlock.getCollisionShape`). */
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		if (ceiling) {
			return null;
		}
		int meta = world.getBlockMetadata(x, y, z);
		double p = 1.0D / 16.0D;
		return meta == 4 || meta == 5 ? AxisAlignedBB.getBoundingBox(x + 6 * p, y + 14 * p, z, x + 10 * p, y + 1, z + 1)
			: AxisAlignedBB.getBoundingBox(x, y + 14 * p, z + 6 * p, x + 1, y + 1, z + 10 * p);
	}

	// ---------------------------------------------------------------- drawn: 26.3's models

	/** Over `wood.MixinBlockSign`'s (`ModelledBlock` is on every `BlockSign` at runtime only, so no `@Override`). */
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		if (!ceiling) {
			return Collections.singletonList(new ModelPart("template_wall_hanging_sign", 0, meta == 2 ? 180 : meta == 4 ? 90 : meta == 5 ? 270 : 0, false));
		}
		boolean attached = world.getTileEntity(x, y, z) instanceof HangingSigns.Tile tile && tile.attached;
		String model = (attached ? "template_attached_hanging_sign_rot_" : "template_hanging_sign_rot_") + (meta & 3);
		return Collections.singletonList(new ModelPart(model, 0, 90 * ((meta & 15) >> 2), false));
	}
}
