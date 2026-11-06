package io.github.asgerjon.mcfly.end;

import ganymedes01.etfuturum.combat.ICombatPlayer;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.network.ItemCooldownMessage;
import io.github.asgerjon.mcfly.network.LevelEventMessage;
import io.github.asgerjon.mcfly.network.McFlyNetwork;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * 26.3's chorus fruit (`Items.CHORUS_FRUIT`, `Foods.CHORUS_FRUIT`, `TeleportRandomlyConsumeEffect`; SPEC-modern-movement.md
 * §84): 4 food and 0.3 saturation, eaten even when full; eaten, it tries 16 times for a place within 8 each way and lands
 * the eater there as 26.3's `randomTeleport` does, then a cooldown of 20 ticks.
 */
public class ChorusFruitItem extends ItemFood {

	private static final int COOLDOWN = 20;
	private static final float DIAMETER = 16.0F;

	public ChorusFruitItem() {
		super(4, 0.3F, false);
		setAlwaysEdible();
		setCreativeTab(McFlyTab.TAB);
		setUnlocalizedName(McFly.name("chorus_fruit"));
		setTextureName("chorus_fruit");
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (((ICombatPlayer) player).etfu$getCooldowns().isCoolingDown(this)) {
			return stack;
		}
		return super.onItemRightClick(stack, world, player);
	}

	@Override
	protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
		if (world.isRemote) {
			return;
		}
		teleportRandomly((WorldServer) world, player);
		((ICombatPlayer) player).etfu$getCooldowns().add(this, COOLDOWN);
		if (player instanceof EntityPlayerMP) {
			McFlyNetwork.CHANNEL.sendTo(new ItemCooldownMessage(this, COOLDOWN), (EntityPlayerMP) player);
		}
	}

	/** 26.3's `TeleportRandomlyConsumeEffect.apply` (diameter 16, directional particles on). */
	private static boolean teleportRandomly(WorldServer world, EntityPlayer player) {
		Random random = player.getRNG();
		for (int attempt = 0; attempt < 16; attempt++) {
			double x = player.posX + (random.nextDouble() - 0.5) * DIAMETER;
			double y = MathHelper.clamp_double(player.posY + (random.nextDouble() - 0.5) * DIAMETER, 0.0, world.getActualHeight() - 1);
			double z = player.posZ + (random.nextDouble() - 0.5) * DIAMETER;
			if (player.ridingEntity != null) {
				player.mountEntity(null);
			}
			double oldX = player.posX;
			double oldY = player.posY;
			double oldZ = player.posZ;
			if (randomTeleport(world, player, x, y, z)) {
				// 26.3's `teleport` at where the eater was (§90.9, step H1)
				io.github.asgerjon.mcfly.gameevent.GameEvents.post(world, io.github.asgerjon.mcfly.gameevent.GameEvent.get("teleport"), oldX, oldY, oldZ, player, null, 0);
				world.playSoundEffect(player.posX, player.posY, player.posZ, McFly.MC_ASSETS + ":item.chorus_fruit.teleport", 1.0F, 1.0F);
				int bx = MathHelper.floor_double(oldX);
				int by = MathHelper.floor_double(oldY);
				int bz = MathHelper.floor_double(oldZ);
				int dx = clamp(MathHelper.floor_double(player.posX) - bx);
				int dy = clamp(MathHelper.floor_double(player.posY) - by);
				int dz = clamp(MathHelper.floor_double(player.posZ) - bz);
				// 26.3's entity event 46 (the eater's 128 portal particles from where it was) and level event 2017
				LevelEventMessage.sendAround(world, LevelEventMessage.ENTITY_TELEPORT, (int) Math.round(oldX * 32), (int) Math.round(oldY * 32),
					(int) Math.round(oldZ * 32), player.getEntityId());
				LevelEventMessage.sendAround(world, 2017, bx, by, bz, (dx + 127) << 16 | (dy + 127) << 8 | (dz + 127));
				player.fallDistance = 0.0F;
				return true;
			}
		}
		return false;
	}

	/** 26.3's `BlockUtil.clampedPackDifferenceInPosition` range, -127 to 127 each way. */
	private static int clamp(int difference) {
		return Math.max(-127, Math.min(127, difference));
	}

	/**
	 * 26.3's `LivingEntity.randomTeleport`: down from the place to the first block that blocks movement (26.3's
	 * `entities_can_teleport_to`), there if the eater fits with no liquid about it; the height kept as it is above the
	 * block, not snapped onto it.
	 */
	private static boolean randomTeleport(WorldServer world, EntityPlayer player, double x, double y, double z) {
		int bx = MathHelper.floor_double(x);
		int by = MathHelper.floor_double(y);
		int bz = MathHelper.floor_double(z);
		if (!world.blockExists(bx, by, bz)) {
			return false;
		}
		double feet = y;
		while (by > 0) {
			by--;
			if (world.getBlock(bx, by, bz).getMaterial().blocksMovement()) {
				return checkAndTeleport(world, player, x, feet, z);
			}
			feet--;
		}
		return false;
	}

	private static boolean checkAndTeleport(WorldServer world, EntityPlayer player, double x, double y, double z) {
		double halfWidth = player.width / 2.0;
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x - halfWidth, y, z - halfWidth, x + halfWidth, y + player.height, z + halfWidth);
		if (!world.func_147461_a(box).isEmpty() || world.isAnyLiquid(box)) {
			return false;
		}
		player.setPositionAndUpdate(x, y, z);
		return true;
	}
}
