package ganymedes01.etfuturum.blocks;

import io.github.asgerjon.mcfly.sound.McFlySoundType;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.particle.McFlyParticles;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.ducks.IFreezingEntity;
import ganymedes01.etfuturum.entities.Freezing;
import io.github.asgerjon.mcfly.mixins.early.powdersnow.AccessorEntityLivingBase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.MaterialLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Powder snow (SPEC-modern-movement.md §62.1, §62.2, §62.11; 26.3 {@code PowderSnowBlock}): no collision except for
 * entities that fell more than 2.5 blocks (a 0.9 high box), falling blocks and walkers above it; entities inside are
 * stuck (0.9, 1.5, 0.9), freeze and have their fire put out, and a burning one melts it. Its material doesn't block
 * movement, so fluids wash it away as 26.3's {@code washed_away_by_fluids} does.
 */
public class BlockPowderSnow extends Block {

	/** Like 1.7.10's snow layer material, but kept in place by pistons and not replaced by placing. */
	public static final MaterialLogic MATERIAL = new MaterialLogic(MapColor.snowColor);

	public BlockPowderSnow() {
		super(MATERIAL);
		setHardness(0.25F);
		// 1.7.10 stores resistance * 3 and explodes with a fifth of it: 26.3's 0.25
		setResistance(0.25F * 5.0F / 3.0F);
		setLightOpacity(1);
		setStepSound(McFlySoundType.POWDER_SNOW);
		setBlockName(McFly.name("powder_snow"));
		setBlockTextureName("powder_snow");
	}

	/** Any powder snow block in the box (the enderman's landing, a dismount spot). */
	public static boolean anyIn(World world, AxisAlignedBB box) {
		if (!McFlyBlocks.POWDER_SNOW.isEnabled()) {
			return false;
		}
		int x0 = MathHelper.floor_double(box.minX), x1 = MathHelper.floor_double(box.maxX);
		int y0 = MathHelper.floor_double(box.minY), y1 = MathHelper.floor_double(box.maxY);
		int z0 = MathHelper.floor_double(box.minZ), z1 = MathHelper.floor_double(box.maxZ);
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					if (world.getBlock(x, y, z) == McFlyBlocks.POWDER_SNOW.get()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// ---------------------------------------------------------------- shape

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
		return McFlyRenderIds.POWDER_SNOW;
	}

	/** 26.3 {@code skipRendering}: no face against another powder snow block. */
	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
		return world.getBlock(x, y, z) != this && super.shouldSideBeRendered(world, x, y, z, side);
	}

	/** No collision without an entity: placement checks, so it can be placed where an entity stands (26.3). */
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return null;
	}

	/** 26.3 {@code getCollisionShape}, per entity. */
	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
		if (entity == null) {
			return;
		}
		AxisAlignedBB box = null;
		if (entity.fallDistance > 2.5F) {
			box = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 0.9F, z + 1);
		} else if (entity instanceof EntityFallingBlock
				|| Freezing.canWalkOnPowderSnow(entity) && entity.boundingBox.minY > y + 1 - 1.0E-5F && !entity.isSneaking()) {
			box = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
		}
		if (box != null && mask.intersectsWith(box)) {
			list.add(box);
		}
	}

	/** The path finder doesn't walk into it (26.3's path type {@code POWDER_SNOW}, malus -1). */
	@Override
	public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
		return false;
	}

	// ---------------------------------------------------------------- entities in it

	/** 26.3 {@code entityInside}: stuck, freezing, a burning entity melts it, then its fire goes out. */
	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		if (!(entity instanceof IFreezingEntity)) {
			return;
		}
		IFreezingEntity state = (IFreezingEntity) entity;
		boolean feetIn = world.getBlock(MathHelper.floor_double(entity.posX), MathHelper.floor_double(entity.boundingBox.minY),
				MathHelper.floor_double(entity.posZ)) == this;
		if (!(entity instanceof EntityLivingBase) || feetIn) {
			state.etfu$makeStuck(0.9F, 1.5D, 0.9F);
			// 26.3 resets the fall distance in it; the server's player handles its fall after this check of the move,
			// so for it that is done after the fall (powdersnow.MixinEntityPlayerMP), or the landing would be lost
			if (!(entity instanceof EntityPlayerMP)) {
				entity.fallDistance = 0.0F;
			}
			if (!world.isRemote && state.etfu$movedSideways() && world.rand.nextBoolean()) {
				float sideways = 1.0F / 12.0F;
				// one snowflake by name (spec §79 B7): count 0, so it moves by the spread given (speed 1)
				McFlyParticles.send((net.minecraft.world.WorldServer) world, McFlyParticles.SNOWFLAKE, entity.posX, y + 1, entity.posZ, 0,
					(world.rand.nextFloat() * 2.0F - 1.0F) * sideways, 0.05F, (world.rand.nextFloat() * 2.0F - 1.0F) * sideways, 1.0D);
			}
		}
		// 26.3 applies FREEZE once a tick however many powder snow blocks the entity is in
		boolean first = !state.etfu$isInPowderSnow();
		state.etfu$setInPowderSnow(true);
		if (first && !world.isRemote && Freezing.canFreeze(entity)) {
			state.etfu$setTicksFrozen(Math.min(Freezing.TICKS_TO_FREEZE, state.etfu$getTicksFrozen() + 1));
		}
		if (!world.isRemote && entity.isBurning() && (world.getGameRules().getGameRuleBooleanValue("mobGriefing") || entity instanceof EntityPlayer)
				&& mayInteract(world, x, y, z, entity)) {
			world.func_147480_a(x, y, z, false);
		}
		state.etfu$extinguishAfterCollisions();
	}

	/** 26.3 {@code mayInteract}: a player under spawn protection doesn't melt it; mobs always may. */
	private static boolean mayInteract(World world, int x, int y, int z, Entity entity) {
		if (!(entity instanceof EntityPlayerMP)) {
			return true;
		}
		MinecraftServer server = MinecraftServer.getServer();
		return server == null || !server.isBlockProtected(world, x, y, z, (EntityPlayer) entity);
	}

	/** 26.3 {@code fallOn}: no fall damage; a living entity that fell 4 or more plays its fall sound (big from 7). */
	@Override
	public void onFallenUpon(World world, int x, int y, int z, Entity entity, float fallDistance) {
		// both sides, as 26.3's: a 1.7.10 player's own sounds from the server reach everyone but it, its client plays them
		if (fallDistance >= 4.0F && entity instanceof AccessorEntityLivingBase) {
			entity.playSound(((AccessorEntityLivingBase) entity).etfu$fallSound(fallDistance < 7.0F ? 1 : 5), 1.0F, 1.0F);
		}
		entity.fallDistance = 0.0F;
	}

	// ---------------------------------------------------------------- drops and the bucket

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return null;
	}

	@Override
	public int quantityDropped(Random random) {
		return 0;
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
		return new ArrayList<>();
	}

	@Override
	public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) {
		return false;
	}

	/** Its item is the bucket (26.3 has no powder snow item). */
	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
		return McFlyItems.POWDER_SNOW_BUCKET.isEnabled() ? McFlyItems.POWDER_SNOW_BUCKET.newItemStack() : null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		return McFlyItems.POWDER_SNOW_BUCKET.isEnabled() ? McFlyItems.POWDER_SNOW_BUCKET.get() : null;
	}
}
