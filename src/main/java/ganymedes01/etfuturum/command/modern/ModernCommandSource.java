package ganymedes01.etfuturum.command.modern;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Who a command on the server comes from, as Brigadier's source: a player, the console or a command block (spec §41). */
public final class ModernCommandSource implements SuggestionSource {

	private final ICommandSender sender;

	public ModernCommandSource(ICommandSender sender) {
		this.sender = sender;
	}

	public ICommandSender sender() {
		return sender;
	}

	/** 1.7.10's check: operators by their level, single player with cheats, command blocks and the console. */
	public boolean hasPermission(int level, String command) {
		return sender.canCommandSenderUseCommand(level, command);
	}

	@Override
	public boolean allowsSelectors() {
		return hasPermission(2, "");
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<String> onlinePlayerNames() {
		List<String> names = new ArrayList<>();
		for (EntityPlayerMP player : (List<EntityPlayerMP>) server().getConfigurationManager().playerEntityList) {
			names.add(player.getCommandSenderName());
		}
		return names;
	}

	public MinecraftServer server() {
		return MinecraftServer.getServer();
	}

	/** The entity running the command, or null for the console and command blocks. */
	public Entity entity() {
		return sender instanceof Entity entity ? entity : null;
	}

	/** An entity's own position, a command block's centre, the console's the world spawn, as modern places them. */
	public Vec3 position() {
		if (sender instanceof Entity entity) {
			return positionOf(entity);
		}
		if (sender instanceof CommandBlockLogic) {
			ChunkCoordinates block = sender.getPlayerCoordinates();
			return Vec3.createVectorHelper(block.posX + 0.5, block.posY + 0.5, block.posZ + 0.5);
		}
		ChunkCoordinates spawn = server().worldServers[0].getSpawnPoint();
		return Vec3.createVectorHelper(spawn.posX, spawn.posY, spawn.posZ);
	}

	/**
	 * Modern's position of an entity, the bottom centre of its hitbox (spec §43.4). 1.7.10's posY is the middle for
	 * items, minecarts, boats and others.
	 */
	public static Vec3 positionOf(Entity entity) {
		return Vec3.createVectorHelper(entity.posX, entity.boundingBox.minY, entity.posZ);
	}

	/** The sender's rotation, {pitch, yaw}, for local coordinates; none for the console and command blocks. */
	public float[] rotation() {
		return sender instanceof Entity entity ? new float[]{entity.rotationPitch, entity.rotationYaw} : new float[]{0.0F, 0.0F};
	}

	/** The world the command runs in: the sender's; the console's is the overworld. */
	public WorldServer world() {
		return sender.getEntityWorld() instanceof WorldServer world ? world : server().worldServers[0];
	}

	/** To the sender; with {@code broadcast} to operators and the log as well, by 1.7.10's feedback rules. */
	public void sendSuccess(ICommand command, boolean broadcast, String key, Object... args) {
		if (broadcast) {
			CommandBase.func_152373_a(sender, command, key, args);
		} else {
			sender.addChatMessage(new ChatComponentTranslation(key, args));
		}
	}

	public void sendFailure(IChatComponent message) {
		IChatComponent red = new ChatComponentText("").appendSibling(message);
		red.getChatStyle().setColor(EnumChatFormatting.RED);
		sender.addChatMessage(red);
	}
}
