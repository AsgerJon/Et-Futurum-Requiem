package io.github.asgerjon.mcfly.sculk;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 26.3's sculk sensor and calibrated sculk sensor (`SculkSensorBlock`, `CalibratedSculkSensorBlock`;
 * SPEC-modern-movement.md §90.9, step H2).
 *
 * - **Stored form:** the plain one's meta is its phase (0 inactive, 1 active, 2 cooldown). The calibrated one's meta & 3
 *   is its facing (1.7.10's horizontal order: 0 south, 1 west, 2 north, 3 east) and meta >> 2 its phase. The power lives in
 *   the block entity, which 1.7.10's meta has no room for.
 * - **Hearing:** its block entity hears game events out to 8 (16) and turns it active with the power by distance, for
 *   30 ticks (10); then cooldown for 10, then inactive.
 * - **Activating:** posts `sculk_sensor_tendrils_clicking`, clicks, and sets amethyst next to it resonating.
 * - **Signals and drops:** weak power to every side (not the calibrated one's back), strong only into the block below,
 *   the comparator the last frequency while active. 1.5 to break, a hoe its tool, light 1, 5 experience but to Silk
 *   Touch.
 * - **Drawn:** from 26.3's models, full bright while active.
 */
public class SculkSensorBlock extends McFlyBlock implements ModelledBlock {

	public static final int INACTIVE = 0, ACTIVE = 1, COOLDOWN = 2;
	private static final int COOLDOWN_TICKS = 10;

	/** 26.3's `RESONANCE_PITCH_BEND`: the note block's pitch of each frequency's tone. */
	private static final float[] RESONANCE_PITCH_BEND = new float[16];

	static {
		int[] tones = {0, 0, 2, 4, 6, 7, 9, 10, 12, 14, 15, 18, 19, 21, 22, 24};
		for (int i = 0; i < 16; i++) {
			RESONANCE_PITCH_BEND[i] = (float) Math.pow(2.0D, (tones[i] - 12) / 12.0D);
		}
	}

	private final boolean calibrated;

	@SideOnly(Side.CLIENT)
	private Map<String, IIcon> icons;

	public SculkSensorBlock(boolean calibrated) {
		super(Material.ground);
		this.calibrated = calibrated;
		setNames(calibrated ? "calibrated_sculk_sensor" : "sculk_sensor");
		strength(1.5F, 1.5F).tool("hoe", 0).light(1).sound(McFlySoundType.SCULK_SENSOR).colour(MapColor.cyanColor);
		setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
	}

	public boolean calibrated() {
		return calibrated;
	}

	// ---------------------------------------------------------------- the meta

	public static int phase(Block block, int meta) {
		return block instanceof SculkSensorBlock && ((SculkSensorBlock) block).calibrated ? meta >> 2 & 3 : meta & 3;
	}

	private int withPhase(int meta, int phase) {
		return calibrated ? meta & 3 | phase << 2 : phase;
	}

	/** The calibrated one's facing as a Forge direction (0 south, 1 west, 2 north, 3 east in the meta). */
	public static ForgeDirection facing(int meta) {
		switch (meta & 3) {
			case 0:
				return ForgeDirection.SOUTH;
			case 1:
				return ForgeDirection.WEST;
			case 2:
				return ForgeDirection.NORTH;
			default:
				return ForgeDirection.EAST;
		}
	}

	public static boolean canActivate(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block instanceof SculkSensorBlock && phase(block, world.getBlockMetadata(x, y, z)) == INACTIVE;
	}

	/** 26.3's `getActiveTicks`. */
	public int activeTicks() {
		return calibrated ? 10 : 30;
	}

	private static SculkSensorBlockEntity sensor(IBlockAccess world, int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile instanceof SculkSensorBlockEntity ? (SculkSensorBlockEntity) tile : null;
	}

	// ---------------------------------------------------------------- activating

	/** 26.3's `activate`: active with the power, a tick in 30 (10), the neighbours told; amethyst resonates; the clicking. */
	void activate(World world, int x, int y, int z, Entity source, int power, int frequency) {
		SculkSensorBlockEntity sensor = sensor(world, x, y, z);
		if (sensor != null) {
			sensor.setPower(power);
		}
		world.setBlockMetadataWithNotify(x, y, z, withPhase(world.getBlockMetadata(x, y, z), ACTIVE), 3);
		world.scheduleBlockUpdate(x, y, z, this, activeTicks());
		updateNeighbours(world, x, y, z);
		resonate(world, x, y, z, source, frequency);
		GameEvents.postAtBlock(world, "sculk_sensor_tendrils_clicking", x, y, z, source, null, 0);
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":block.sculk_sensor.clicking", 1.0F, world.rand.nextFloat() * 0.2F + 0.8F);
	}

	/** 26.3's `tryResonateVibration`: each amethyst block next to it (`#vibration_resonators`). */
	private static void resonate(World world, int x, int y, int z, Entity source, int frequency) {
		if (!McFlyBlocks.AMETHYST_BLOCK.isEnabled()) {
			return;
		}
		Block amethyst = McFlyBlocks.AMETHYST_BLOCK.get();
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			int ax = x + direction.offsetX, ay = y + direction.offsetY, az = z + direction.offsetZ;
			if (world.getBlock(ax, ay, az) == amethyst) {
				GameEvents.postAtBlock(world, GameEvent.resonance(frequency).name, ax, ay, az, source, amethyst, world.getBlockMetadata(ax, ay, az));
				world.playSoundEffect(ax + 0.5D, ay + 0.5D, az + 0.5D, McFly.MC_ASSETS + ":block.amethyst_block.resonate", 1.0F,
					RESONANCE_PITCH_BEND[MathHelper.clamp_int(frequency, 0, 15)]);
			}
		}
	}

	/** 26.3's `deactivate`: cooldown, power 0, a tick in 10, the neighbours told. */
	private void deactivate(World world, int x, int y, int z) {
		SculkSensorBlockEntity sensor = sensor(world, x, y, z);
		if (sensor != null) {
			sensor.setPower(0);
		}
		world.setBlockMetadataWithNotify(x, y, z, withPhase(world.getBlockMetadata(x, y, z), COOLDOWN), 3);
		world.scheduleBlockUpdate(x, y, z, this, COOLDOWN_TICKS);
		updateNeighbours(world, x, y, z);
	}

	/** 26.3's `updateNeighbours`: its neighbours and the block below's. */
	private void updateNeighbours(World world, int x, int y, int z) {
		world.notifyBlocksOfNeighborChange(x, y, z, this);
		world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
	}

	/** 26.3's `tick`: active turns to cooldown; cooldown to inactive, with the clicking's stop. */
	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		if (world.isRemote) {
			return;
		}
		int phase = phase(this, world.getBlockMetadata(x, y, z));
		if (phase == ACTIVE) {
			deactivate(world, x, y, z);
		} else if (phase == COOLDOWN) {
			world.setBlockMetadataWithNotify(x, y, z, withPhase(world.getBlockMetadata(x, y, z), INACTIVE), 3);
			world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":block.sculk_sensor.clicking_stop", 1.0F,
				world.rand.nextFloat() * 0.2F + 0.8F);
		}
	}

	/** 26.3's `stepOn`: an entity on the ground on it, while it's inactive, forces a step in. */
	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		if (world.isRemote || !entity.onGround || !canActivate(world, x, y, z)) {
			return;
		}
		SculkSensorBlockEntity sensor = sensor(world, x, y, z);
		if (sensor != null) {
			sensor.steppedOn(entity);
		}
	}

	/** 26.3's `affectNeighborsAfterRemoval`: broken while active, its neighbours hear of it. */
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		boolean active = phase(this, meta) == ACTIVE;
		super.breakBlock(world, x, y, z, block, meta);
		if (active && !world.isRemote) {
			updateNeighbours(world, x, y, z);
		}
	}

	/** The calibrated one faces the way the player faces (26.3's `getHorizontalDirection`). */
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		if (calibrated) {
			int facing = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
			world.setBlockMetadataWithNotify(x, y, z, facing, 2);
		}
	}

	@Override
	public boolean hasTileEntity(int meta) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		return calibrated ? new SculkSensorBlockEntity.Calibrated() : new SculkSensorBlockEntity();
	}

	// ---------------------------------------------------------------- signals

	@Override
	public boolean canProvidePower() {
		return true;
	}

	/**
	 * The power to every side; the calibrated one gives none out of its back (26.3's `getSignal`: none toward its
	 * facing, the asker's side toward it from behind).
	 */
	@Override
	public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
		if (calibrated && side == facing(world.getBlockMetadata(x, y, z)).ordinal()) {
			return 0;
		}
		SculkSensorBlockEntity sensor = sensor(world, x, y, z);
		return sensor == null ? 0 : sensor.power();
	}

	/** 26.3's `getDirectSignal`: strong only into the block below (asked from below, side 1). */
	@Override
	public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
		return side == ForgeDirection.UP.ordinal() ? isProvidingWeakPower(world, x, y, z, side) : 0;
	}

	/** Dust joins it from any side, as any signal source in 26.3. */
	@Override
	public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
		return side != -1;
	}

	@Override
	public boolean isNormalCube(IBlockAccess world, int x, int y, int z) {
		return false;
	}

	@Override
	public boolean hasComparatorInputOverride() {
		return true;
	}

	/** 26.3's `getAnalogOutputSignal`: the last frequency while active, else 0. */
	@Override
	public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
		SculkSensorBlockEntity sensor = sensor(world, x, y, z);
		return sensor != null && phase(this, world.getBlockMetadata(x, y, z)) == ACTIVE ? sensor.lastFrequency() : 0;
	}

	// ---------------------------------------------------------------- drops

	/** 5 experience, unless by Silk Touch (Forge gives none to a block that can be silk-harvested with it). */
	@Override
	public int getExpDrop(IBlockAccess world, int meta, int fortune) {
		return 5;
	}

	@Override
	public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int meta) {
		return true;
	}

	@Override
	public int damageDropped(int meta) {
		return 0;
	}

	@Override
	protected ItemStack createStackedBlock(int meta) {
		return new ItemStack(this);
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

	/** 26.3's blockstate: inactive or active (cooldown shows active); the calibrated one north as the model is, turned by y. */
	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		return Collections.singletonList(part(world.getBlockMetadata(x, y, z)));
	}

	private ModelPart part(int meta) {
		String model = (calibrated ? "calibrated_sculk_sensor_" : "sculk_sensor_") + (phase(this, meta) == INACTIVE ? "inactive" : "active");
		if (!calibrated) {
			return new ModelPart(model);
		}
		switch (facing(meta)) {
			case EAST:
				return new ModelPart(model, 0, 90, false);
			case SOUTH:
				return new ModelPart(model, 0, 180, false);
			case WEST:
				return new ModelPart(model, 0, 270, false);
			default:
				return new ModelPart(model);
		}
	}

	/** 26.3's item model: inactive, north. */
	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(new ModelPart(calibrated ? "calibrated_sculk_sensor_inactive" : "sculk_sensor_inactive"));
	}

	@Override
	public boolean emissive(IBlockAccess world, int x, int y, int z) {
		return phase(this, world.getBlockMetadata(x, y, z)) == ACTIVE;
	}

	private static final String[] TEXTURES = {"sculk_sensor_bottom", "sculk_sensor_side", "sculk_sensor_top", "sculk_sensor_tendril_inactive",
		"sculk_sensor_tendril_active", "calibrated_sculk_sensor_amethyst", "calibrated_sculk_sensor_input_side", "calibrated_sculk_sensor_top"};

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		String name = texture.substring(texture.lastIndexOf('/') + 1);
		IIcon icon = icons.get(name);
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
		// 26.3's particle texture
		blockIcon = icons.get("sculk_sensor_bottom");
	}
}
