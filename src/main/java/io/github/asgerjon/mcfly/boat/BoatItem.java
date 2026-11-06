package io.github.asgerjon.mcfly.boat;

import io.github.asgerjon.mcfly.base.McFlyItem;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

/**
 * A boat's, raft's or chest boat's item (SPEC-modern-movement.md 91.12; 26.3's `BoatItem`): placed where the look meets
 * a block or any fluid, turned as the player, unless the player stands inside something pickable on the way or the boat
 * wouldn't fit there; named as the item; `entity_place`; used up (not in creative). Stacks of 1. Vanilla's boat is
 * placed the same way as oak's while `replaceOldBoats` is on (`MixinItemBoat`).
 */
public class BoatItem extends McFlyItem {

	/** 26.3's `BoatDispenseItemBehavior`, for every boat item. */
	public static final BehaviorDefaultDispenseItem DISPENSING = new Dispensing();

	private final BoatTypes.Type type;
	private final boolean chest;

	public BoatItem(BoatTypes.Type type, boolean chest) {
		this.type = type;
		this.chest = chest;
		String kind = (chest ? "_chest" : "") + (type.raft ? "_raft" : "_boat");
		String prefix = type.domain.equals("biomesoplenty") ? "bop_" : type.domain.equals("witchery") ? "witchery_" : "";
		setUnlocalizedName(io.github.asgerjon.mcfly.McFly.name(prefix + type.wood + kind));
		setTextureName(type.domain + ":" + type.wood + kind);
		setMaxStackSize(1);
	}

	public BoatTypes.Type type() {
		return type;
	}

	public boolean isChest() {
		return chest;
	}

	/** A new boat of this wood, chest or not, not yet in the world. */
	public static McFlyBoat create(World world, BoatTypes.Type type, boolean chest) {
		McFlyBoat boat = chest ? new McFlyChestBoat(world) : new McFlyBoat(world);
		boat.setType(type);
		return boat;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		use(stack, world, player, type, chest);
		return stack;
	}

	/** 26.3's `use`; true when a boat was (or, on the client, would be) placed. */
	public static boolean use(ItemStack stack, World world, EntityPlayer player, BoatTypes.Type type, boolean chest) {
		// getPlayerPOVHitResult with any fluid: from the eyes, the block reach (5) along the look
		Vec3 from = Vec3.createVectorHelper(player.posX, player.posY + (world.isRemote ? player.getEyeHeight() - player.getDefaultEyeHeight() : player.getEyeHeight()),
			player.posZ);
		Vec3 look = look(player);
		double range = 5.0D;
		Vec3 to = from.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range);
		MovingObjectPosition hit = world.rayTraceBlocks(from, to, true);
		if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
			return false;
		}
		// not while standing inside something pickable along the look
		AxisAlignedBB around = player.boundingBox.addCoord(look.xCoord * range, look.yCoord * range, look.zCoord * range).expand(1.0D, 1.0D, 1.0D);
		@SuppressWarnings("unchecked")
		List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(player, around);
		for (Entity entity : entities) {
			if (entity.canBeCollidedWith()) {
				float border = entity.getCollisionBorderSize();
				if (entity.boundingBox.expand(border, border, border).isVecInside(from)) {
					return false;
				}
			}
		}
		// a fluid is hit at its height (1.7.10's ray meets a liquid's whole block)
		double hitY = hit.hitVec.yCoord;
		if (hit.sideHit == 1 && world.getBlock(hit.blockX, hit.blockY, hit.blockZ).getMaterial().isLiquid()) {
			hitY = hit.blockY + McFlyBoat.liquidHeight(world, hit.blockX, hit.blockY, hit.blockZ);
		}
		McFlyBoat boat = create(world, type, chest);
		boat.setInitialPos(hit.hitVec.xCoord, hitY, hit.hitVec.zCoord);
		if (stack.hasDisplayName()) {
			boat.setCustomName(stack.getDisplayName());
		}
		boat.rotationYaw = player.rotationYaw;
		boat.prevRotationYaw = player.rotationYaw;
		if (!world.getCollidingBoundingBoxes(boat, boat.boundingBox).isEmpty()) {
			return false;
		}
		if (!world.isRemote) {
			world.spawnEntityInWorld(boat);
			GameEvents.post(world, GameEvent.get("entity_place"), hit.hitVec.xCoord, hitY, hit.hitVec.zCoord, player, null, 0);
			if (!player.capabilities.isCreativeMode) {
				stack.stackSize--;
			}
		}
		int id = Item.getIdFromItem(stack.getItem());
		StatBase used = id >= 0 && id < StatList.objectUseStats.length ? StatList.objectUseStats[id] : null;
		if (used != null) {
			player.addStat(used, 1);
		}
		return true;
	}

	/** 26.3's view vector, from the player's pitch and yaw. */
	private static Vec3 look(EntityPlayer player) {
		float pitch = player.rotationPitch * (float) Math.PI / 180.0F;
		float yaw = -player.rotationYaw * (float) Math.PI / 180.0F;
		float cosYaw = MathHelper.cos(yaw);
		float sinYaw = MathHelper.sin(yaw);
		float cosPitch = MathHelper.cos(pitch);
		float sinPitch = MathHelper.sin(pitch);
		return Vec3.createVectorHelper(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
	}

	/**
	 * 26.3's `BoatDispenseItemBehavior`: into water just in front (raised 1), or onto air over water; anything else and
	 * the item is dispensed as any item is. Just outside the dispenser (0.5625 and half the boat's width), turned as it
	 * faces; the dispenser's click (level event 1000).
	 */
	private static final class Dispensing extends BehaviorDefaultDispenseItem {

		private final BehaviorDefaultDispenseItem plain = new BehaviorDefaultDispenseItem();

		@Override
		protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
			BoatTypes.Type type;
			boolean chest;
			if (stack.getItem() instanceof BoatItem item) {
				type = item.type;
				chest = item.chest;
			} else {
				// vanilla's boat, oak's while `replaceOldBoats` is on
				type = BoatTypes.OAK;
				chest = false;
			}
			EnumFacing facing = BlockDispenser.func_149937_b(source.getBlockMetadata());
			World world = source.getWorld();
			double outside = 0.5625D + 1.375D / 2.0D;
			double x = source.getX() + facing.getFrontOffsetX() * outside;
			double y = source.getY() + facing.getFrontOffsetY() * 1.125F;
			double z = source.getZ() + facing.getFrontOffsetZ() * outside;
			int frontX = source.getXInt() + facing.getFrontOffsetX();
			int frontY = source.getYInt() + facing.getFrontOffsetY();
			int frontZ = source.getZInt() + facing.getFrontOffsetZ();
			double raise;
			if (world.getBlock(frontX, frontY, frontZ).getMaterial() == Material.water) {
				raise = 1.0D;
			} else {
				if (!world.isAirBlock(frontX, frontY, frontZ) || world.getBlock(frontX, frontY - 1, frontZ).getMaterial() != Material.water) {
					return plain.dispense(source, stack);
				}
				raise = 0.0D;
			}
			McFlyBoat boat = create(world, type, chest);
			boat.setInitialPos(x, y + raise, z);
			if (stack.hasDisplayName()) {
				boat.setCustomName(stack.getDisplayName());
			}
			boat.rotationYaw = yRot(facing);
			boat.prevRotationYaw = boat.rotationYaw;
			world.spawnEntityInWorld(boat);
			stack.splitStack(1);
			return stack;
		}

		@Override
		protected void playDispenseSound(IBlockSource source) {
			source.getWorld().playAuxSFX(1000, source.getXInt(), source.getYInt(), source.getZInt(), 0);
		}

		/** 26.3's `Direction.toYRot`: south 0, west 90, north 180, east 270 (up and down 0). */
		private static float yRot(EnumFacing facing) {
			switch (facing) {
				case WEST:
					return 90.0F;
				case NORTH:
					return 180.0F;
				case EAST:
					return 270.0F;
				default:
					return 0.0F;
			}
		}
	}
}
