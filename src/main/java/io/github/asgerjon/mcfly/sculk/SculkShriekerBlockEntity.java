package io.github.asgerjon.mcfly.sculk;

import ganymedes01.etfuturum.potion.EffectRegistry;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.mcfly.gameevent.VibrationSystem;
import io.github.asgerjon.mcfly.network.LevelEventMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * A sculk shrieker's block entity (26.3's `SculkShriekerBlockEntity` and its `VibrationUser`; SPEC-modern-movement.md
 * §90.9, step H4).
 *
 * - **Listening:** out to 8, only `#shrieker_can_listen` (a sensor's clicking), only from a player or what's
 *   theirs, not while shrieking, with the chunks round it loaded.
 * - **The shriek:** a warning first if it can respond; then shrieking for 90 ticks, level event 3007 and `shriek`.
 * - **The response at its end:** the warden's reply (a warden at level 4 once McFly has one) and Darkness to the
 *   players within 40.
 * - **Saved:** `warning_level` and `listener`.
 */
public class SculkShriekerBlockEntity extends TileEntity implements VibrationSystem.User {

	private static final int SHRIEKING_TICKS = 90;
	private static final int DARKNESS_RADIUS = 40;
	private static final String[] REPLY_BY_LEVEL = {null, "entity.warden.nearby_close", "entity.warden.nearby_closer", "entity.warden.nearby_closest",
		"entity.warden.listening_angry"};

	private final VibrationSystem.Data data = new VibrationSystem.Data();
	private final VibrationSystem.Listener listener = new VibrationSystem.Listener(data, this);
	private int warningLevel;
	private boolean registered;

	// ---------------------------------------------------------------- listening

	private void listen() {
		if (!registered && worldObj != null && !worldObj.isRemote) {
			GameEvents.register(worldObj, listener);
			registered = true;
		}
	}

	private void stopListening() {
		if (registered && worldObj != null) {
			GameEvents.unregister(worldObj, listener);
		}
		registered = false;
	}

	@Override
	public void validate() {
		super.validate();
		listen();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		stopListening();
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		stopListening();
	}

	@Override
	public void updateEntity() {
		if (worldObj.isRemote) {
			return;
		}
		listen();
		VibrationSystem.tick(worldObj, data, this);
	}

	// ---------------------------------------------------------------- 26.3's shrieking

	/** 26.3's `tryGetPlayer`: a player, a player's vehicle, a player's projectile, a player's dropped item; or null. */
	public static EntityPlayerMP tryGetPlayer(Entity source) {
		if (source instanceof EntityPlayerMP) {
			return (EntityPlayerMP) source;
		}
		if (source != null && source.riddenByEntity instanceof EntityPlayerMP) {
			return (EntityPlayerMP) source.riddenByEntity;
		}
		Entity owner = null;
		if (source instanceof EntityArrow) {
			owner = ((EntityArrow) source).shootingEntity;
		} else if (source instanceof EntityThrowable) {
			owner = ((EntityThrowable) source).getThrower();
		} else if (source instanceof EntityFireball) {
			owner = ((EntityFireball) source).shootingEntity;
		} else if (source instanceof EntityItem) {
			String thrower = ((EntityItem) source).func_145800_j();
			owner = thrower == null ? null : source.worldObj.getPlayerEntityByName(thrower);
		}
		return owner instanceof EntityPlayerMP ? (EntityPlayerMP) owner : null;
	}

	private boolean isShrieking() {
		return SculkShriekerBlock.shrieking(worldObj.getBlockMetadata(xCoord, yCoord, zCoord));
	}

	/** 26.3's `tryShriek`. */
	void tryShriek(EntityPlayerMP player) {
		if (player == null || isShrieking()) {
			return;
		}
		warningLevel = 0;
		if (!canRespond() || tryToWarn(player)) {
			shriek(player);
		}
	}

	private boolean tryToWarn(EntityPlayerMP player) {
		int level = WardenSpawnTracker.tryWarn(worldObj, xCoord, yCoord, zCoord, player);
		if (level < 0) {
			return false;
		}
		warningLevel = level;
		markDirty();
		return true;
	}

	/** 26.3's `shriek`: shrieking for 90, level event 3007, `shriek` at it. */
	private void shriek(Entity source) {
		worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, worldObj.getBlockMetadata(xCoord, yCoord, zCoord) | SculkShriekerBlock.SHRIEKING, 2);
		worldObj.scheduleBlockUpdate(xCoord, yCoord, zCoord, getBlockType(), SHRIEKING_TICKS);
		if (worldObj instanceof WorldServer) {
			LevelEventMessage.sendAround((WorldServer) worldObj, LevelEventMessage.SCULK_SHRIEK, xCoord, yCoord, zCoord, 0);
		}
		GameEvents.postAtBlock(worldObj, "shriek", xCoord, yCoord, zCoord, source, null, 0);
	}

	/** 26.3's `canRespond`: `can_summon`, not Peaceful, the wardens' game rule on. */
	private boolean canRespond() {
		return SculkShriekerBlock.canSummon(worldObj.getBlockMetadata(xCoord, yCoord, zCoord)) && worldObj.difficultySetting != EnumDifficulty.PEACEFUL
			&& SpawnWardens.isOn(worldObj);
	}

	/** 26.3's `tryRespond`: with a warning, the reply (a warden at 4 once McFly has one) and Darkness about. */
	void tryRespond() {
		if (canRespond() && warningLevel > 0) {
			playReply();
			applyDarknessAround(worldObj, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, DARKNESS_RADIUS);
		}
	}

	/** 26.3's `playWardenReplySound`: the level's reply 10 off at random, hostile, volume 5. */
	private void playReply() {
		String sound = warningLevel < REPLY_BY_LEVEL.length ? REPLY_BY_LEVEL[warningLevel] : null;
		if (sound == null) {
			return;
		}
		int x = xCoord + between(-10, 10), y = yCoord + between(-10, 10), z = zCoord + between(-10, 10);
		worldObj.playSoundEffect(x, y, z, McFly.MC_ASSETS + ":" + sound, 5.0F, 1.0F);
	}

	private int between(int min, int max) {
		return min + worldObj.rand.nextInt(max - min + 1);
	}

	/**
	 * 26.3's `Warden.applyDarknessAround` with `MobEffectUtil.addEffectToPlayersAround`: Darkness (260 ticks, level 0, no
	 * particles) to each survival or adventure player within the radius without it, or whose own ends within 199.
	 */
	public static void applyDarknessAround(World world, double x, double y, double z, int radius) {
		Potion darkness = EffectRegistry.get("minecraft:darkness");
		if (darkness == null) {
			return;
		}
		for (Object entry : world.playerEntities) {
			EntityPlayerMP player = (EntityPlayerMP) entry;
			double dx = player.posX - x, dy = player.boundingBox.minY - y, dz = player.posZ - z;
			if (!player.theItemInWorldManager.isCreative() && !io.github.asgerjon.mcfly.spectator.Spectator.isSpectator(player)
				&& dx * dx + dy * dy + dz * dz < (double) radius * radius) {
				PotionEffect had = player.getActivePotionEffect(darkness);
				if (had == null || had.getAmplifier() < 0 || had.getDuration() <= 199) {
					player.addPotionEffect(new PotionEffect(darkness.id, 260, 0, false));
				}
			}
		}
	}

	// ---------------------------------------------------------------- 26.3's VibrationUser

	@Override
	public int radius() {
		return 8;
	}

	@Override
	public double[] position(World world) {
		return new double[]{xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D};
	}

	@Override
	public String listenableTag() {
		return "minecraft:shrieker_can_listen";
	}

	@Override
	public boolean canReceive(World world, int x, int y, int z, GameEvent event, GameEvent.Context context) {
		return !isShrieking() && tryGetPlayer(context.source) != null;
	}

	@Override
	public void onReceive(World world, int x, int y, int z, GameEvent event, Entity source, float distance) {
		tryShriek(tryGetPlayer(source));
	}

	@Override
	public void onDataChanged() {
		markDirty();
	}

	@Override
	public boolean requiresAdjacentChunks() {
		return true;
	}

	// ---------------------------------------------------------------- saving

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("warning_level", warningLevel);
		NBTTagCompound vibration = new NBTTagCompound();
		data.writeToNBT(vibration);
		tag.setTag("listener", vibration);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		warningLevel = Math.max(0, tag.getInteger("warning_level"));
		data.readFromNBT(tag.getCompoundTag("listener"));
	}
}
