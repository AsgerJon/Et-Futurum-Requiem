package ganymedes01.etfuturum.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tab completion of a block position with the coordinates of the block the player looks at, as modern suggests
 * them (spec §39). Modern completes the whole argument; the 1.7.10 client replaces only the last word, so each
 * suggestion is cut to its part from the last word on.
 */
public final class CoordinateSuggestions {

	/** The suggestion when no block is looked at, or the sender is not a player. */
	private static final String[] RELATIVE = {"~", "~", "~"};

	private CoordinateSuggestions() {
	}

	/** Completions for a position whose x is {@code args[start]}, while the last argument is one of its three; else null. */
	public static List<String> blockPosition(ICommandSender sender, String[] args, int start) {
		int typedFields = args.length - start;
		if (typedFields < 1 || typedFields > 3) {
			return null;
		}
		return complete(String.join(" ", Arrays.copyOfRange(args, start, args.length)), lookedAtBlock(sender));
	}

	/** Modern's suggestions for what is typed of a position, given the target's x, y and z, cut to the last word on. */
	static List<String> complete(String typed, String[] target) {
		List<String> suggestions = new ArrayList<>();
		if (typed.isEmpty()) {
			String full = target[0] + " " + target[1] + " " + target[2];
			if (isPosition(full)) {
				suggestions.add(target[0]);
				suggestions.add(target[0] + " " + target[1]);
				suggestions.add(full);
			}
		} else {
			String[] fields = typed.split(" ");
			if (fields.length == 1) {
				String full = fields[0] + " " + target[1] + " " + target[2];
				if (isPosition(full)) {
					suggestions.add(fields[0] + " " + target[1]);
					suggestions.add(full);
				}
			} else if (fields.length == 2) {
				String full = fields[0] + " " + fields[1] + " " + target[2];
				if (isPosition(full)) {
					suggestions.add(full);
				}
			}
		}
		int lastWord = typed.lastIndexOf(' ') + 1;
		List<String> completions = new ArrayList<>();
		for (String suggestion : suggestions) {
			if (suggestion.startsWith(typed)) {
				completions.add(suggestion.substring(lastWord));
			}
		}
		return completions;
	}

	/** The block in the player's crosshair reach, traced from the eyes on the server, as text; else ~ ~ ~. */
	private static String[] lookedAtBlock(ICommandSender sender) {
		if (!(sender instanceof EntityPlayerMP player)) {
			return RELATIVE;
		}
		// The client's crosshair reach in 1.7.10 (PlayerControllerMP.getBlockReachDistance)
		double reach = player.theItemInWorldManager.isCreative() ? 5.0D : 4.5D;
		Vec3 eyes = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
		Vec3 look = player.getLook(1.0F);
		Vec3 end = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
		MovingObjectPosition hit = player.worldObj.rayTraceBlocks(eyes, end, false);
		if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
			return RELATIVE;
		}
		return new String[]{Integer.toString(hit.blockX), Integer.toString(hit.blockY), Integer.toString(hit.blockZ)};
	}

	/** Three coordinates separated by single spaces. */
	private static boolean isPosition(String value) {
		String[] fields = value.split(" ", -1);
		return fields.length == 3 && isCoordinate(fields[0]) && isCoordinate(fields[1]) && isCoordinate(fields[2]);
	}

	/** 1.7.10's coordinate syntax (CommandBase.func_110665_a): ~, ~ followed by a number, or a number. */
	private static boolean isCoordinate(String field) {
		boolean relative = field.startsWith("~");
		String number = relative ? field.substring(1) : field;
		if (number.isEmpty()) {
			return relative;
		}
		try {
			return Double.isFinite(Double.parseDouble(number));
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
