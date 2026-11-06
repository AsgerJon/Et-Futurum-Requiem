package io.github.asgerjon.mcfly.boat;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.network.McFlyNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

import java.util.List;

/**
 * Where the steering player's client moved its boat, and its paddles (SPEC-modern-movement.md 91.12; 26.3's
 * `ServerboundMoveVehiclePacket` and `ServerboundPaddleBoatPacket`, in place of EFR's channel's 4). The server checks it
 * as 26.3's `handleMoveVehicle`: more than 100 past its own motion is refused (not in single player), and after moving
 * the boat itself, more than 0.25 off or into something new puts it back; either way the client is told
 * ({@link BoatMovedBackMessage}).
 */
public class BoatMoveMessage implements IMessage {

	private int entityId;
	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;
	private boolean onGround;
	private boolean left;
	private boolean right;

	public BoatMoveMessage() {
	}

	BoatMoveMessage(McFlyBoat boat) {
		entityId = boat.getEntityId();
		x = boat.posX;
		y = boat.posY;
		z = boat.posZ;
		yaw = boat.rotationYaw;
		pitch = boat.rotationPitch;
		onGround = boat.onGround;
		left = boat.getPaddleState(0);
		right = boat.getPaddleState(1);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		entityId = buf.readInt();
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
		yaw = buf.readFloat();
		pitch = buf.readFloat();
		int flags = buf.readByte();
		onGround = (flags & 1) != 0;
		left = (flags & 2) != 0;
		right = (flags & 4) != 0;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(entityId);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(yaw);
		buf.writeFloat(pitch);
		buf.writeByte((onGround ? 1 : 0) | (left ? 2 : 0) | (right ? 4 : 0));
	}

	public static class Handler implements IMessageHandler<BoatMoveMessage, IMessage> {

		@Override
		public IMessage onMessage(BoatMoveMessage message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().playerEntity;
			if (!valid(message.x, message.y, message.z, message.yaw, message.pitch)) {
				ctx.getServerHandler().kickPlayerFromServer("Invalid move vehicle packet received");
				return null;
			}
			Entity entity = player.worldObj.getEntityByID(message.entityId);
			if (!(entity instanceof McFlyBoat boat) || boat.isDead || boat.controllingPassenger() != player) {
				return null;
			}
			boat.setPaddleState(message.left, message.right);
			double oldX = boat.posX;
			double oldY = boat.posY;
			double oldZ = boat.posZ;
			double targetX = MathHelper.clamp_double(message.x, -3.0E7D, 3.0E7D);
			double targetY = MathHelper.clamp_double(message.y, -2.0E7D, 2.0E7D);
			double targetZ = MathHelper.clamp_double(message.z, -3.0E7D, 3.0E7D);
			float targetYaw = MathHelper.wrapAngleTo180_float(message.yaw);
			float targetPitch = MathHelper.wrapAngleTo180_float(message.pitch);
			double dx = targetX - boat.firstGoodX;
			double dy = targetY - boat.firstGoodY;
			double dz = targetZ - boat.firstGoodZ;
			double expected = boat.motionX * boat.motionX + boat.motionY * boat.motionY + boat.motionZ * boat.motionZ;
			double moved = dx * dx + dy * dy + dz * dz;
			MinecraftServer server = MinecraftServer.getServer();
			boolean owner = server.isSinglePlayer() && player.getCommandSenderName().equals(server.getServerOwner());
			if (moved - expected > 100.0D && !owner) {
				McFly.LOG.warn("{} (vehicle of {}) moved too quickly! {},{},{}", boat.getCommandSenderName(), player.getCommandSenderName(), dx, dy, dz);
				McFlyNetwork.CHANNEL.sendTo(new BoatMovedBackMessage(boat), player);
				return null;
			}
			AxisAlignedBB oldBox = boat.boundingBox.copy();
			boat.moveEntity(targetX - boat.posX, targetY - boat.posY, targetZ - boat.posZ);
			dx = targetX - boat.posX;
			dy = targetY - boat.posY;
			// 26.3's own (`yDist > -0.5 || yDist < 0.5`): the height is never checked
			dy = 0.0D;
			dz = targetZ - boat.posZ;
			moved = dx * dx + dy * dy + dz * dz;
			boolean wrong = moved > 0.0625D;
			if (wrong) {
				McFly.LOG.warn("{} (vehicle of {}) moved wrongly! {}", boat.getCommandSenderName(), player.getCommandSenderName(), Math.sqrt(moved));
			}
			if (wrong && boat.worldObj.getCollidingBoundingBoxes(boat, oldBox).isEmpty() || collidesWithAnythingNew(boat, oldBox, targetX, targetY, targetZ)) {
				boat.setPositionAndRotation(oldX, oldY, oldZ, targetYaw, targetPitch);
				McFlyNetwork.CHANNEL.sendTo(new BoatMovedBackMessage(boat), player);
				return null;
			}
			boat.setPositionAndRotation(targetX, targetY, targetZ, targetYaw, targetPitch);
			boat.onGround = message.onGround;
			return null;
		}

		private static boolean valid(double... values) {
			for (double value : values) {
				if (Double.isNaN(value) || Double.isInfinite(value)) {
					return false;
				}
			}
			return true;
		}

		/** 26.3's `isEntityCollidingWithAnythingNew`: a collision there that its old box didn't touch. */
		@SuppressWarnings("unchecked")
		private static boolean collidesWithAnythingNew(McFlyBoat boat, AxisAlignedBB oldBox, double x, double y, double z) {
			double e = 1.0E-5D;
			AxisAlignedBB there = boat.boundingBox.getOffsetBoundingBox(x - boat.posX, y - boat.posY, z - boat.posZ).contract(e, e, e);
			AxisAlignedBB old = oldBox.contract(e, e, e);
			for (AxisAlignedBB box : (List<AxisAlignedBB>) boat.worldObj.func_147461_a(there)) {
				if (!box.intersectsWith(old)) {
					return true;
				}
			}
			return false;
		}
	}
}
