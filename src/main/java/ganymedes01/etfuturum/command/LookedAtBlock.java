package ganymedes01.etfuturum.command;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * /setblock without coordinates targets the block the player looks at within reach, a convenience of this mod: the
 * coordinates are filled in before the vanilla command runs. The look is traced on the server from the eyes along
 * the player's facing, as far as the block reach goes.
 */
public final class LookedAtBlock {

	private LookedAtBlock() {
	}

	/** The arguments with the looked-at block's coordinates in front when the first argument is not a coordinate. */
	public static String[] fillInCoordinates(ICommandSender sender, String[] args) {
		if (!ConfigMixins.setblockAtLookedBlock || args.length == 0 || isCoordinate(args[0]) || !(sender instanceof EntityPlayerMP player)) {
			return args;
		}
		double reach = player.theItemInWorldManager.getBlockReachDistance();
		Vec3 eyes = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
		Vec3 look = player.getLook(1.0F);
		Vec3 end = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
		MovingObjectPosition hit = player.worldObj.rayTraceBlocks(eyes, end, false);
		if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
			throw new CommandException("commands.etfuturum.setblock.nothingInReach");
		}
		String[] withCoordinates = new String[args.length + 3];
		withCoordinates[0] = Integer.toString(hit.blockX);
		withCoordinates[1] = Integer.toString(hit.blockY);
		withCoordinates[2] = Integer.toString(hit.blockZ);
		System.arraycopy(args, 0, withCoordinates, 3, args.length);
		return withCoordinates;
	}

	/** A coordinate argument: relative (~), or a number. */
	public static boolean isCoordinate(String argument) {
		if (argument.startsWith("~")) {
			return true;
		}
		try {
			Double.parseDouble(argument);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
