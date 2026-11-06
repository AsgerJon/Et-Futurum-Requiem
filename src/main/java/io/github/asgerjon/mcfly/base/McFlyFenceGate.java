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
import net.minecraft.block.Block;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collections;
import java.util.List;

/**
 * A wooden gate of ours (SPEC-modern-movement.md §91.5, step C), 26.3's `FenceGateBlock` in 1.7.10's metas (`& 3` the
 * facing: 0 south, 1 west, 2 north, 3 east; `4` open; `8` powered, 26.3's `POWERED`):
 * - **use:** opens swinging away from the player (turned when it faces the player), or closes; the wood type's
 *   `fence_gate.open` or `close` by the new state; `block_open` or `block_close`;
 * - **redstone:** only a change in power opens or closes it (the powered bit), so a player can still work a powered gate;
 * - **a wind burst** toggles it unless powered;
 * - **in a wall** (a wall either side across it, worked out): lowered 3/16, its model and outline;
 * - no support needed (26.3's), 2 and 3, the axe's; drawn from 26.3's models.
 */
public class McFlyFenceGate extends BlockFenceGate implements ModelledBlock {

	public static final int OPEN = 4, POWERED = 8;

	private final McFlyFence.Wood wood;
	@SideOnly(Side.CLIENT)
	private IIcon[] customIcons;

	public McFlyFenceGate(McFlyFence.Wood wood) {
		this.wood = wood;
		setBlockName(McFly.name(wood.name + "_fence_gate"));
		setHardness(2.0F);
		this.blockResistance = 3.0F * 5.0F;
		setStepSound(wood.sound);
		setHarvestLevel("axe", 0);
		setCreativeTab(McFlyTab.TAB);
	}

	private static boolean facesAlongZ(int meta) {
		return (meta & 1) == 0;
	}

	/** 26.3's `IN_WALL`: a wall on either side across its facing. */
	public static boolean inWall(IBlockAccess world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		return facesAlongZ(meta) ? world.getBlock(x - 1, y, z) instanceof BlockWall || world.getBlock(x + 1, y, z) instanceof BlockWall
			: world.getBlock(x, y, z - 1) instanceof BlockWall || world.getBlock(x, y, z + 1) instanceof BlockWall;
	}

	// ---------------------------------------------------------------- 26.3's FenceGateBlock

	private void playSound(World world, int x, int y, int z, boolean opened) {
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":" + wood.gateSound + (opened ? ".open" : ".close"), 1.0F,
			world.rand.nextFloat() * 0.1F + 0.9F);
	}

	private static void post(World world, int x, int y, int z, boolean opened, Entity source) {
		GameEvents.postAtBlock(world, opened ? "block_open" : "block_close", x, y, z, source, null, 0);
	}

	/** 26.3's `useWithoutItem`. */
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		int meta = world.getBlockMetadata(x, y, z);
		if ((meta & OPEN) != 0) {
			meta &= ~OPEN;
		} else {
			int facing = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
			if ((meta & 3) == (facing + 2) % 4) {
				meta = meta & ~3 | facing;
			}
			meta |= OPEN;
		}
		world.setBlockMetadataWithNotify(x, y, z, meta, 2);
		if (!world.isRemote) {
			boolean opened = (meta & OPEN) != 0;
			playSound(world, x, y, z, opened);
			post(world, x, y, z, opened, player);
		}
		return true;
	}

	/** 26.3's `neighborChanged`: a change in power opens or closes it, and sets the bit. */
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		if (world.isRemote) {
			return;
		}
		int meta = world.getBlockMetadata(x, y, z);
		boolean power = world.isBlockIndirectlyGettingPowered(x, y, z);
		if (((meta & POWERED) != 0) != power) {
			world.setBlockMetadataWithNotify(x, y, z, meta & 3 | (power ? OPEN | POWERED : 0), 2);
			if (((meta & OPEN) != 0) != power) {
				playSound(world, x, y, z, power);
				post(world, x, y, z, power, null);
			}
		}
	}

	/** 26.3's `onExplosionHit` for a wind burst: toggled, unless powered. */
	public void burst(World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		if ((meta & POWERED) != 0) {
			return;
		}
		boolean opened = (meta & OPEN) == 0;
		world.setBlockMetadataWithNotify(x, y, z, meta ^ OPEN, 3);
		playSound(world, x, y, z, opened);
		post(world, x, y, z, opened, null);
	}

	/** 26.3's `getStateForPlacement`: the player's facing; open and powered when placed powered. */
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		int facing = (MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3) % 4;
		world.setBlockMetadataWithNotify(x, y, z, facing | (world.isBlockIndirectlyGettingPowered(x, y, z) ? OPEN | POWERED : 0), 2);
	}

	/** 26.3's gates need nothing under them. */
	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return world.getBlock(x, y, z).isReplaceable(world, x, y, z);
	}

	/** 26.3's shape: 4 thick, 16 high, 13 in a wall. */
	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		float top = inWall(world, x, y, z) ? 0.8125F : 1.0F;
		if (facesAlongZ(world.getBlockMetadata(x, y, z))) {
			setBlockBounds(0.0F, 0.0F, 0.375F, 1.0F, top, 0.625F);
		} else {
			setBlockBounds(0.375F, 0.0F, 0.0F, 0.625F, top, 1.0F);
		}
	}

	/** 26.3's collision: none open, else 1.5 high and 4 thick (1.7.10's). */
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		if ((meta & OPEN) != 0) {
			return null;
		}
		return facesAlongZ(meta) ? AxisAlignedBB.getBoundingBox(x, y, z + 0.375D, x + 1, y + 1.5D, z + 0.625D)
			: AxisAlignedBB.getBoundingBox(x + 0.375D, y, z, x + 0.625D, y + 1.5D, z + 1);
	}

	// ---------------------------------------------------------------- what it is

	@Override
	public MapColor getMapColor(int meta) {
		return wood.colour;
	}

	@Override
	public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return wood.burns;
	}

	@Override
	public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return wood.burns ? 20 : 0;
	}

	@Override
	public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return wood.burns ? 5 : 0;
	}

	// ---------------------------------------------------------------- the look

	@Override
	public int getRenderType() {
		return FMLCommonHandler.instance().getEffectiveSide().isClient() ? McFlyRenderIds.MODEL : 21;
	}

	/** 26.3's blockstate: the model by open and in a wall, turned by the facing (south 0, west 90, north 180, east 270), uvlock. */
	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		String model = (wood.custom != null ? wood.custom + "_fence_gate" : "template_fence_gate") + (inWall(world, x, y, z) ? "_wall" : "")
			+ ((meta & OPEN) != 0 ? "_open" : "");
		return Collections.singletonList(new ModelPart(model, 0, 90 * (meta & 3), true));
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(new ModelPart(wood.custom != null ? wood.custom + "_fence_gate" : "template_fence_gate"));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		if (wood.custom != null && customIcons != null) {
			return texture.endsWith("_particle") ? customIcons[1] : customIcons[0];
		}
		return wood.planksIcon();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return wood.custom != null && customIcons != null ? customIcons[1] : wood.planksIcon();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		if (wood.custom != null) {
			customIcons = new IIcon[]{register.registerIcon(wood.custom + "_fence_gate"), register.registerIcon(wood.custom + "_fence_gate_particle")};
		}
	}
}
