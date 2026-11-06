package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import io.github.asgerjon.mcfly.wood.HangingSigns;
import io.github.asgerjon.mcfly.wood.Shelves;
import io.github.asgerjon.mcfly.wood.Signs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A wood's shelf (SPEC-modern-movement.md 91.10, step H; 26.3's `ShelfBlock`): facing `& 3` (0 north, 1 south, 2 west,
 * 3 east) and `+4` powered in the meta, three items and the chain part in `Shelves.Tile`. Placed facing the player; used
 * on its front by the third hit (a slot swapped with the hand; powered, the chain's slots with the hotbar's end);
 * chained in 3s by power; read by a comparator from behind; 2.0 and 3.0, the shelf's sounds; drawn from 26.3's models.
 */
public class McFlyShelf extends BlockContainer implements ModelledBlock {

	/** A facing's model turn (26.3's blockstate: north 0, east 90, south 180, west 270). */
	private static final int[] TURN = {0, 180, 270, 90};
	/** A facing's back, as 1.7.10's comparator names sides (0 south, 1 west, 2 north, 3 east). */
	private static final int[] BACK_2D = {0, 2, 3, 1};
	private static final String[] CHAIN_MODEL = {"template_shelf_unconnected", "template_shelf_right", "template_shelf_center", "template_shelf_left"};

	private final String texture;
	private final MapColor colour;
	private final Supplier<Block> planks;
	private final int planksMeta;

	public McFlyShelf(String wood, MapColor colour, Supplier<Block> planks, int planksMeta) {
		super(Material.wood);
		this.texture = "minecraft:" + wood + "_shelf";
		this.colour = colour;
		this.planks = planks;
		this.planksMeta = planksMeta;
		setHardness(2.0F);
		this.blockResistance = 3.0F * 5.0F;
		setStepSound(McFlySoundType.SHELF);
		setBlockName(McFly.name(wood + "_shelf"));
		setHarvestLevel("axe", 0);
		setCreativeTab(McFlyTab.TAB);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new Shelves.Tile();
	}

	@Override
	public MapColor getMapColor(int meta) {
		return colour;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	/** Its back is a full face (26.3's shape: the back 3 deep, the whole block). */
	@Override
	public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
		return side.ordinal() == (Shelves.sideOf(world.getBlockMetadata(x, y, z)) ^ 1);
	}

	// ---------------------------------------------------------------- placing, power, breaking

	/** Facing the player (26.3's `getHorizontalDirection().getOpposite()`), powered if a signal reaches it. */
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		int facing = Shelves.facingAgainst(HangingSigns.sideOfYaw(placer.rotationYaw));
		boolean powered = world.isBlockIndirectlyGettingPowered(x, y, z);
		world.setBlockMetadataWithNotify(x, y, z, facing | (powered ? 4 : 0), 3);
		if (powered && !world.isRemote) {
			Shelves.poweredUp(world, x, y, z);
		}
	}

	/** 26.3's `neighborChanged`: powered by a signal, unpowering unchains it; the sounds and game events; the chain. */
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		if (world.isRemote) {
			return;
		}
		int meta = world.getBlockMetadata(x, y, z);
		boolean signal = world.isBlockIndirectlyGettingPowered(x, y, z);
		if (((meta & 4) != 0) == signal) {
			return;
		}
		int now = (meta & 3) | (signal ? 4 : 0);
		world.setBlockMetadataWithNotify(x, y, z, now, 3);
		if (!signal && world.getTileEntity(x, y, z) instanceof Shelves.Tile tile && tile.sideChain != Shelves.UNCONNECTED) {
			tile.sideChain = Shelves.UNCONNECTED;
			tile.changed(null, null);
		}
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + (signal ? ":block.shelf.activate" : ":block.shelf.deactivate"), 1.0F, 1.0F);
		GameEvents.postAtBlock(world, signal ? "block_activate" : "block_deactivate", x, y, z, null, this, now);
		if (signal) {
			Shelves.poweredUp(world, x, y, z);
		} else {
			Shelves.poweredDown(world, x, y, z, now);
		}
	}

	/** Its items drop; the shelves beside it let go (26.3's `affectNeighborsAfterRemoval`). */
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if (world.getTileEntity(x, y, z) instanceof Shelves.Tile tile) {
			for (int slot = 0; slot < tile.getSizeInventory(); slot++) {
				ItemStack stack = tile.getStackInSlot(slot);
				if (stack != null) {
					EntityItem item = new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, stack.copy());
					world.spawnEntityInWorld(item);
				}
			}
			world.func_147453_f(x, y, z, block);
		}
		super.breakBlock(world, x, y, z, block, meta);
		Shelves.poweredDown(world, x, y, z, meta);
	}

	// ---------------------------------------------------------------- using it

	/**
	 * 26.3's `useItemOn` (and `SelectableSlotContainer.getHitSlot`): on the front face only, the slot by the third
	 * across. Unpowered, the hand's stack and the slot's swap; powered, the chain's slots and the hotbar's end.
	 */
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		int meta = world.getBlockMetadata(x, y, z);
		if (side != Shelves.sideOf(meta) || !(world.getTileEntity(x, y, z) instanceof Shelves.Tile tile)) {
			return false;
		}
		ItemStack held = player.getCurrentEquippedItem();
		if (world.isRemote) {
			// 26.3: consumed (no swing) with something in hand, else passed
			if (held != null) {
				Signs.noSwing = true;
			}
			return held != null;
		}
		float across = switch (side) {
			case 2 -> 1.0F - hitX;
			case 3 -> hitX;
			case 4 -> hitZ;
			default -> 1.0F - hitZ;
		};
		int slot = Math.max(0, Math.min(2, (int) Math.floor(across * 16.0F / (16.0F / 3.0F))));
		if ((meta & 4) == 0) {
			ItemStack removed = tile.swap(slot, held);
			ItemStack toHand = player.capabilities.isCreativeMode && removed == null && held != null ? held.copy() : removed;
			player.inventory.setInventorySlotContents(player.inventory.currentItem, toHand);
			player.inventory.markDirty();
			tile.changed("item_interact_finish", player);
			if (removed != null) {
				play(world, x, y, z, held == null ? "take_item" : "single_swap");
			} else {
				if (held == null) {
					return false;
				}
				play(world, x, y, z, "place_item");
			}
			return true;
		}
		if (swapHotbar(world, x, y, z, player)) {
			play(world, x, y, z, "multi_swap");
		}
		return true;
	}

	/** 26.3's `swapHotbar`: the chain's shelves, left end first, against the hotbar's last 3 slots a shelf. */
	private static boolean swapHotbar(World world, int x, int y, int z, EntityPlayer player) {
		List<int[]> chain = Shelves.chain(world, x, y, z);
		boolean any = false;
		for (int index = 0; index < chain.size(); index++) {
			int[] pos = chain.get(index);
			if (!(world.getTileEntity(pos[0], pos[1], pos[2]) instanceof Shelves.Tile shelf)) {
				continue;
			}
			for (int slot = 0; slot < shelf.getSizeInventory(); slot++) {
				int hotbar = 9 - (chain.size() - index) * shelf.getSizeInventory() + slot;
				if (hotbar >= 0 && hotbar < 9) {
					ItemStack placed = player.inventory.getStackInSlot(hotbar);
					player.inventory.setInventorySlotContents(hotbar, null);
					ItemStack fromShelf = shelf.swap(slot, placed);
					if (placed != null || fromShelf != null) {
						player.inventory.setInventorySlotContents(hotbar, fromShelf);
						any = true;
					}
				}
			}
			player.inventory.markDirty();
			shelf.changed("entity_interact", player);
		}
		return any;
	}

	private static void play(World world, int x, int y, int z, String sound) {
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":block.shelf." + sound, 1.0F, 1.0F);
	}

	// ---------------------------------------------------------------- the comparator

	@Override
	public boolean hasComparatorInputOverride() {
		return true;
	}

	/** From behind only: the three slots as bits 1, 2, 4 (26.3's `getAnalogOutputSignal`). */
	@Override
	public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
		int meta = world.getBlockMetadata(x, y, z);
		if (side != BACK_2D[meta & 3] || !(world.getTileEntity(x, y, z) instanceof Shelves.Tile tile)) {
			return 0;
		}
		int signal = 0;
		for (int slot = 0; slot < 3; slot++) {
			if (tile.getStackInSlot(slot) != null) {
				signal |= 1 << slot;
			}
		}
		return signal;
	}

	// ---------------------------------------------------------------- shapes

	/** 26.3's three boxes facing north (top board, back, bottom board), turned. */
	private static double[][] boxes(int meta) {
		double[][] north = {{0, 12, 11, 16, 16, 13}, {0, 0, 13, 16, 16, 16}, {0, 0, 11, 16, 4, 13}};
		double[][] out = new double[3][];
		for (int i = 0; i < 3; i++) {
			double[] b = north[i];
			switch (meta & 3) {
				case 1:
					out[i] = new double[]{16 - b[3], b[1], 16 - b[5], 16 - b[0], b[4], 16 - b[2]};
					break;
				case 2:
					out[i] = new double[]{b[2], b[1], 16 - b[3], b[5], b[4], 16 - b[0]};
					break;
				case 3:
					out[i] = new double[]{16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]};
					break;
				default:
					out[i] = b;
			}
		}
		return out;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		double[][] parts = boxes(world.getBlockMetadata(x, y, z));
		double[] u = {16, 16, 16, 0, 0, 0};
		for (double[] b : parts) {
			for (int i = 0; i < 3; i++) {
				u[i] = Math.min(u[i], b[i]);
				u[i + 3] = Math.max(u[i + 3], b[i + 3]);
			}
		}
		setBlockBounds((float) u[0] / 16, (float) u[1] / 16, (float) u[2] / 16, (float) u[3] / 16, (float) u[4] / 16, (float) u[5] / 16);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
		for (double[] b : boxes(world.getBlockMetadata(x, y, z))) {
			AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x + b[0] / 16, y + b[1] / 16, z + b[2] / 16, x + b[3] / 16, y + b[4] / 16, z + b[5] / 16);
			if (mask.intersectsWith(box)) {
				list.add(box);
			}
		}
		setBlockBoundsBasedOnState(world, x, y, z);
	}

	// ---------------------------------------------------------------- drawn: 26.3's multipart models

	@Override
	public int getRenderType() {
		return FMLCommonHandler.instance().getEffectiveSide().isClient() ? McFlyRenderIds.MODEL : 0;
	}

	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		int turn = TURN[meta & 3];
		List<ModelPart> parts = new ArrayList<>();
		parts.add(new ModelPart("template_shelf_body", 0, turn, false));
		if ((meta & 4) == 0) {
			parts.add(new ModelPart("template_shelf_unpowered", 0, turn, false));
		} else {
			int chain = world.getTileEntity(x, y, z) instanceof Shelves.Tile tile ? tile.sideChain : Shelves.UNCONNECTED;
			parts.add(new ModelPart(CHAIN_MODEL[chain & 3], 0, turn, false));
		}
		return parts;
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		List<ModelPart> parts = new ArrayList<>();
		parts.add(new ModelPart("template_shelf_inventory"));
		return parts;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		return blockIcon;
	}

	/** Its particles are its planks' (26.3's: the stripped log's; noted in 91.10). */
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return planks.get().getIcon(side, planksMeta);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		blockIcon = register.registerIcon(texture);
	}
}
