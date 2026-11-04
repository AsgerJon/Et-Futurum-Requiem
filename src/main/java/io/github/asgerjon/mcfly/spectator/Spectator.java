package io.github.asgerjon.mcfly.spectator;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * Spectator mode (SPEC-modern-movement.md §83; 26.3's game type `spectator`, id 3, as EFR stored it): a spectator flies
 * through blocks unseen and unfelt; it touches nothing (no breaking, placing, picking up, tossing, attacking, using)
 * but looks into containers without changing them; attacking an entity follows it (its eyes), sneaking lets go; mobs
 * don't find it, and it doesn't keep the night from passing.
 */
public final class Spectator {

	public static final Spectator INSTANCE = new Spectator();

	/** The game type, added to 1.7.10's in pre-init (null while the mode is off). */
	public static WorldSettings.GameType GAME_TYPE;

	/** Everything but spectating players (for searches mobs and others make). */
	public static final IEntitySelector NOT_SPECTATING = new IEntitySelector() {
		@Override
		public boolean isEntityApplicable(Entity entity) {
			return !(entity instanceof EntityPlayer player) || !isSpectator(player);
		}
	};

	/** Blocks a spectator mustn't open though they hold an inventory (for another mod's layer to fill). */
	public static final Set<Block> CANT_OPEN = new HashSet<>();

	/** Who each spectator follows. */
	static final Map<EntityPlayer, Entity> FOLLOWING = new WeakHashMap<>();

	private final Map<EntityPlayer, Boolean> wasSpectatingServer = new WeakHashMap<>();
	private final Map<EntityPlayer, Boolean> wasSpectatingClient = new WeakHashMap<>();

	private Spectator() {
	}

	/** In pre-init: 1.7.10's game types get spectator, id 3. */
	public static void init() {
		if (McFlyConfig.enableSpectatorMode && GAME_TYPE == null) {
			GAME_TYPE = EnumHelper.addEnum(WorldSettings.GameType.class, "SPECTATOR", new Class[]{int.class, String.class}, new Object[]{3, "spectator"});
		}
	}

	public static boolean isSpectator(EntityPlayer player) {
		if (GAME_TYPE == null || player == null || player instanceof FakePlayer || player.worldObj == null) {
			return false;
		}
		if (player.worldObj.isRemote) {
			return SpectatorClient.isClientSpectator(player);
		}
		return player instanceof EntityPlayerMP mp && mp.theItemInWorldManager.getGameType() == GAME_TYPE;
	}

	/** The players in a list who aren't spectating. */
	public static List<EntityPlayer> withoutSpectators(List<EntityPlayer> players) {
		return players.stream().filter(player -> !isSpectator(player)).collect(Collectors.toList());
	}

	/** Whom a spectator follows, or null. */
	public static Entity following(EntityPlayer player) {
		return FOLLOWING.get(player);
	}

	/** What a spectator may open: a block's inventory (not one another mod barred), an ender chest. */
	public static boolean canOpen(TileEntity tile) {
		return tile instanceof IInventory || tile instanceof TileEntityEnderChest;
	}

	// ---------------------------------------------------------------- what a spectator doesn't do

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent event) {
		if (!isSpectator(event.entityPlayer)) {
			return;
		}
		if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || !event.world.blockExists(event.x, event.y, event.z)) {
			event.setCanceled(true);
			return;
		}
		if (!canOpen(event.world.getTileEntity(event.x, event.y, event.z)) || CANT_OPEN.contains(event.world.getBlock(event.x, event.y, event.z))) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlace(BlockEvent.PlaceEvent event) {
		if (isSpectator(event.player)) {
			event.setCanceled(true);
		}
	}

	/** 26.3's spectators break nothing (`blockActionRestricted`), however the break comes. */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBreak(BlockEvent.BreakEvent event) {
		if (isSpectator(event.getPlayer())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
		if (isSpectator(event.entityPlayer)) {
			if (event.entityPlayer.worldObj.isRemote) {
				SpectatorClient.stopHitting(event.entityPlayer);
			}
			event.newSpeed = 0;
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPickup(EntityItemPickupEvent event) {
		if (isSpectator(event.entityPlayer)) {
			event.setCanceled(true);
		}
	}

	/** What it tries to throw stays with it. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onToss(ItemTossEvent event) {
		if (isSpectator(event.player)) {
			event.setCanceled(true);
			ItemStack item = event.entityItem.getEntityItem();
			event.player.inventory.addItemStackToInventory(item);
		}
	}

	/** Attacking follows the entity instead (26.3's `setCamera`). */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onAttack(AttackEntityEvent event) {
		if (!isSpectator(event.entityPlayer)) {
			return;
		}
		if (!FOLLOWING.containsKey(event.entityPlayer)) {
			FOLLOWING.put(event.entityPlayer, event.target);
			if (event.entityPlayer.worldObj.isRemote) {
				SpectatorClient.showLetGoHint();
			}
		}
		event.setCanceled(true);
	}

	/** Using an entity: only looking into one that holds an inventory (a chest boat, a tamed adult horse, a minecart). */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onUseEntity(EntityInteractEvent event) {
		if (!isSpectator(event.entityPlayer)) {
			return;
		}
		if (!event.entityPlayer.worldObj.isRemote && !(event.target instanceof EntityPlayer)
			&& (event.target instanceof IInventory || event.target instanceof net.minecraft.inventory.IInvBasic)) {
			openInventoryOf(event.entityPlayer, event.target);
		}
		event.setCanceled(true);
	}

	private static void openInventoryOf(EntityPlayer player, Entity target) {
		boolean sneaking = player.isSneaking();
		// those that do something else when used open their inventory to a sneaking player
		if (target instanceof ganymedes01.etfuturum.entities.EntityNewBoatWithChest) {
			player.setSneaking(true);
		} else if (target instanceof EntityHorse horse) {
			if (!horse.isAdultHorse() || !horse.isTame()) {
				return;
			}
			player.setSneaking(true);
		}
		target.interactFirst(player);
		player.setSneaking(sneaking);
	}

	// ---------------------------------------------------------------- its state, and following

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.START) {
			return;
		}
		// each side keeps its own record, so each side sets and clears the state itself
		Map<EntityPlayer, Boolean> was = event.side == Side.SERVER ? wasSpectatingServer : wasSpectatingClient;
		EntityPlayer player = event.player;
		boolean spectating = isSpectator(player);
		if (spectating) {
			player.noClip = true;
			player.onGround = false;
			player.setInvisible(true);
			if (player.ridingEntity != null) {
				player.dismountEntity(player.ridingEntity);
				player.ridingEntity = null;
			}
		} else if (was.getOrDefault(player, false)) {
			player.noClip = false;
			player.setInvisible(false);
			FOLLOWING.remove(player);
		}
		was.put(player, spectating);
		Entity followed = FOLLOWING.get(player);
		if (followed != null) {
			if (followed instanceof EntityPlayer other && isSpectator(other) || followed.isDead || player.isSneaking()) {
				FOLLOWING.remove(player);
				return;
			}
			follow(player, followed);
		}
	}

	/** At its eyes, looking where it looks, moving as it moves. */
	static void follow(EntityPlayer player, Entity entity) {
		float pitch = entity.rotationPitch;
		float yaw = entity instanceof EntityLiving living ? living.rotationYawHead : entity.rotationYaw;
		player.setLocationAndAngles(entity.posX, entity.posY - player.yOffset + entity.getEyeHeight(), entity.posZ, yaw, pitch);
		player.motionX = entity.motionX;
		player.motionY = entity.motionY;
		player.motionZ = entity.motionZ;
		if (player.isSprinting()) {
			player.setSprinting(false);
		}
	}
}
