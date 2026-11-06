package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.wood.Trapdoors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

/**
 * A trapdoor of ours (SPEC-modern-movement.md §91.6, step D), 26.3's `TrapDoorBlock` by its set type ({@link Trapdoors}:
 * the powered state in a block entity): opened by hand (not iron), by a change of power, by a wind burst; the set type's
 * sounds by the new state; 26.3's placing; no support needed; climbable over a ladder of its facing. Drawn from 26.3's
 * models (`template_trapdoor_*`, the orientable ones turning their texture with the facing: all but oak's, dark oak's
 * and iron's).
 */
public class McFlyTrapdoor extends BlockTrapDoor implements ModelledBlock, McFlySetType.Typed {

	private static final ThreadLocal<Boolean> PLACED_ON_TOP_OR_BOTTOM = ThreadLocal.withInitial(() -> false);

	private final McFlySetType type;
	private final MapColor colour;
	private final String texture;
	private final boolean orientable;

	public McFlyTrapdoor(String name, Material material, McFlySetType type, MapColor colour, String texture, boolean orientable) {
		super(material);
		this.type = type;
		this.colour = colour;
		this.texture = texture;
		this.orientable = orientable;
		setBlockName(McFly.name(name));
		setBlockTextureName(texture);
		setStepSound(type.sound);
		setCreativeTab(McFlyTab.TAB);
		disableStats();
	}

	/** A wooden trapdoor: 3 and 3, the axe's. */
	public static McFlyTrapdoor wooden(String name, McFlySetType type, MapColor colour, String texture, boolean orientable) {
		McFlyTrapdoor trapdoor = new McFlyTrapdoor(name, Material.wood, type, colour, texture, orientable).strength(3.0F, 3.0F);
		trapdoor.setHarvestLevel("axe", 0);
		return trapdoor;
	}

	/** 26.3's strength: 1.7.10's explosion resistance kept as 26.3's number (1.7.10 divides the field by 5). */
	public McFlyTrapdoor strength(float hardness, float resistance) {
		setHardness(hardness);
		this.blockResistance = resistance * 5.0F;
		return this;
	}

	@Override
	public McFlySetType setType() {
		return type;
	}

	// ---------------------------------------------------------------- the powered state's keeper

	@Override
	public boolean hasTileEntity(int meta) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		return new Trapdoors.Tile();
	}

	// ---------------------------------------------------------------- 26.3's TrapDoorBlock

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (!type.byHand) {
			return false;
		}
		Trapdoors.setOpen(this, world, x, y, z, (world.getBlockMetadata(x, y, z) & Trapdoors.OPEN) == 0, player);
		return true;
	}

	/** 1.7.10's open-or-close (other mods): 26.3's `toggle` to that state. */
	@Override
	public void func_150120_a(World world, int x, int y, int z, boolean open) {
		Trapdoors.setOpen(this, world, x, y, z, open, null);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		Trapdoors.redstone(this, world, x, y, z);
	}

	/** 26.3's `getStateForPlacement`: on a side, facing it, the half by the click; on a top or bottom, that face's half. */
	@Override
	public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
		if (side >= 2) {
			PLACED_ON_TOP_OR_BOTTOM.set(false);
			// 1.7.10's sides 2 north, 3 south, 4 west, 5 east; the facing the clicked face's
			return side - 2 | (hitY > 0.5F ? Trapdoors.TOP : 0);
		}
		PLACED_ON_TOP_OR_BOTTOM.set(true);
		return side == 1 ? 0 : Trapdoors.TOP;
	}

	/** On a top or bottom, facing away from the player; placed powered, open and powered. */
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		int meta = world.getBlockMetadata(x, y, z);
		if (PLACED_ON_TOP_OR_BOTTOM.get()) {
			// the player's facing 0 south, 1 west, 2 north, 3 east; the trapdoor faces the opposite: north, east, south, west
			int facing = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
			int[] opposite = {0, 3, 1, 2};
			meta = meta & Trapdoors.TOP | opposite[facing];
			PLACED_ON_TOP_OR_BOTTOM.set(false);
		}
		boolean powered = world.isBlockIndirectlyGettingPowered(x, y, z);
		world.setBlockMetadataWithNotify(x, y, z, powered ? meta | Trapdoors.OPEN : meta, 2);
		Trapdoors.Tile tile = Trapdoors.tile(world, x, y, z);
		if (tile != null && powered) {
			tile.powered = true;
			tile.markDirty();
		}
	}

	/** 26.3's trapdoors hang anywhere. */
	@Override
	public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
		return world.getBlock(x, y, z).isReplaceable(world, x, y, z);
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return world.getBlock(x, y, z).isReplaceable(world, x, y, z);
	}

	@Override
	public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
		return Trapdoors.usableAsLadder(world, x, y, z);
	}

	@Override
	public MapColor getMapColor(int meta) {
		return colour;
	}

	// ---------------------------------------------------------------- the look

	@Override
	public int getRenderType() {
		return McFlyRenderIds.MODEL;
	}

	/**
	 * 26.3's blockstate: bottom, top or open; the orientable ones turned by the facing (north 0, east 90, south 180, west
	 * 270; open on top flipped over, x 180 and y half a turn on), the others only open.
	 */
	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		return Collections.singletonList(part(world.getBlockMetadata(x, y, z)));
	}

	private ModelPart part(int meta) {
		int[] turn = {0, 180, 270, 90};
		int y = turn[meta & 3];
		boolean open = (meta & Trapdoors.OPEN) != 0, top = (meta & Trapdoors.TOP) != 0;
		String base = orientable ? "template_orientable_trapdoor_" : "template_trapdoor_";
		if (open) {
			return top ? new ModelPart(base + "open", 180, (y + 180) % 360, false) : new ModelPart(base + "open", 0, y, false);
		}
		return new ModelPart(base + (top ? "top" : "bottom"), 0, orientable ? y : 0, false);
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(new ModelPart((orientable ? "template_orientable_trapdoor_" : "template_trapdoor_") + "bottom"));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		return blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		blockIcon = register.registerIcon(texture);
	}
}
