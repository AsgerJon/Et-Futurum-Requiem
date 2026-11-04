package ganymedes01.etfuturum.entities;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.spectator.Spectator;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.network.McFlyNetwork;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.ducks.IFreezingEntity;
import io.github.asgerjon.mcfly.network.FreezingMessage;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

import java.util.UUID;

/**
 * Freezing in powder snow (SPEC-modern-movement.md §62.3, §62.4): every entity's frozen ticks, 140 being fully
 * frozen, who can freeze, who walks on top, and the living entity's freezing step (26.3 {@code LivingEntity.aiStep}'s
 * "freezing" section). The state itself is on each entity ({@link IFreezingEntity}, {@code powdersnow.MixinEntity}).
 */
public final class Freezing {

	/** 26.3's {@code getTicksRequiredToFreeze}. */
	public static final int TICKS_TO_FREEZE = 140;

	/** 26.3's {@code FREEZE_HURT_FREQUENCY}. */
	public static final int HURT_EVERY = 40;

	/** The damage type {@code freeze}: through armour, no exhaustion ({@code damage_type/freeze.json}). */
	public static final DamageSource FREEZE = new DamageSource("freeze") {
		{
			setDamageBypassesArmor();
		}
	};

	/** The game rule {@code freezeDamage}: players take freeze damage (26.3 {@code Player.isInvulnerableTo}). */
	public static final String GAMERULE = "freezeDamage";

	/** 1.17's {@code SPEED_MODIFIER_POWDER_SNOW_UUID}, the frost's slowness. */
	private static final UUID SPEED_ID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");

	private Freezing() {
	}

	public static boolean isPowderSnow(Block block) {
		return McFlyBlocks.POWDER_SNOW.isEnabled() && block == McFlyBlocks.POWDER_SNOW.get();
	}

	public static int getTicksFrozen(Entity entity) {
		return entity instanceof IFreezingEntity ? ((IFreezingEntity) entity).etfu$getTicksFrozen() : 0;
	}

	public static void setTicksFrozen(Entity entity, int ticks) {
		if (entity instanceof IFreezingEntity) {
			((IFreezingEntity) entity).etfu$setTicksFrozen(ticks);
		}
	}

	public static float getPercentFrozen(Entity entity) {
		return Math.min(getTicksFrozen(entity), TICKS_TO_FREEZE) / (float) TICKS_TO_FREEZE;
	}

	public static boolean isFullyFrozen(Entity entity) {
		return getTicksFrozen(entity) >= TICKS_TO_FREEZE;
	}

	public static boolean isInPowderSnow(Entity entity) {
		return entity instanceof IFreezingEntity && ((IFreezingEntity) entity).etfu$isInPowderSnow();
	}

	public static boolean wasInPowderSnow(Entity entity) {
		return entity instanceof IFreezingEntity && ((IFreezingEntity) entity).etfu$wasInPowderSnow();
	}

	/**
	 * 26.3's {@code canFreeze}: not in {@code freeze_immune_entity_types} (stray, polar bear, snow golem, wither), not a
	 * skeleton (it turns into a stray instead; 1.7.10's type 0, the wither skeleton can), not a spectator, and no piece
	 * of {@code freeze_immune_wearables} (the four leather pieces) worn.
	 */
	public static boolean canFreeze(Entity entity) {
		if (entity instanceof EntityStray || entity instanceof EntityPolarBear || entity instanceof EntitySnowman || entity instanceof EntityWither) {
			return false;
		}
		if (entity instanceof EntitySkeleton && ((EntitySkeleton) entity).getSkeletonType() == 0) {
			return false;
		}
		if (entity instanceof EntityPlayer && McFlyConfig.enableSpectatorMode && Spectator.isSpectator((EntityPlayer) entity)) {
			return false;
		}
		if (entity instanceof EntityLivingBase) {
			for (int slot = 1; slot <= 4; slot++) {
				if (isLeatherArmour(((EntityLivingBase) entity).getEquipmentInSlot(slot))) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isLeatherArmour(ItemStack stack) {
		if (stack == null) {
			return false;
		}
		Item item = stack.getItem();
		return item == Items.leather_helmet || item == Items.leather_chestplate || item == Items.leather_leggings || item == Items.leather_boots;
	}

	/** 26.3's {@code canEntityWalkOnPowderSnow}: {@code powder_snow_walkable_mobs}, or leather boots on. */
	public static boolean canWalkOnPowderSnow(Entity entity) {
		if (entity instanceof EntityRabbit || entity instanceof EntityFox || entity instanceof EntityEndermite || entity instanceof EntitySilverfish) {
			return true;
		}
		if (entity instanceof EntityLivingBase) {
			ItemStack feet = ((EntityLivingBase) entity).getEquipmentInSlot(1);
			return feet != null && feet.getItem() == Items.leather_boots;
		}
		return false;
	}

	/** 26.3's {@code immuneTo(Blocks.POWDER_SNOW)}: these may spawn in it (stray, polar bear, snow golem). */
	public static boolean maySpawnInPowderSnow(Entity entity) {
		return entity instanceof EntityStray || entity instanceof EntityPolarBear || entity instanceof EntitySnowman;
	}

	/**
	 * The freezing step, on the server once per living update (26.3 {@code aiStep}'s "freezing"): thaw 2 a tick out of
	 * the snow (or when it can't freeze), the frost's slowness, the hurt every 40th tick when fully frozen. A server
	 * player's update runs before the move of the same packet (1.7.10's {@code NetHandlerPlayServer.processPlayer}), so
	 * for it "in the snow" is what the last packet's move found.
	 */
	public static void tick(EntityLivingBase living) {
		if (living.worldObj.isRemote || !(living instanceof IFreezingEntity)) {
			return;
		}
		IFreezingEntity state = (IFreezingEntity) living;
		boolean inSnow = living instanceof EntityPlayerMP ? state.etfu$wasInPowderSnow() : state.etfu$isInPowderSnow();
		boolean canFreeze = canFreeze(living);
		if (!inSnow || !canFreeze) {
			state.etfu$setTicksFrozen(Math.max(0, state.etfu$getTicksFrozen() - 2));
		}
		frost(living, state.etfu$getTicksFrozen());
		if (living.ticksExisted % HURT_EVERY == 0 && isFullyFrozen(living) && canFreeze) {
			hurt(living);
		}
		sync(living, state);
	}

	/** 26.3's {@code removeFrost} then {@code tryAddFrost}: speed + (-0.05 * percent frozen) standing on a block. */
	private static void frost(EntityLivingBase living, int ticks) {
		IAttributeInstance speed = living.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
		if (speed == null) {
			return;
		}
		AttributeModifier old = speed.getModifier(SPEED_ID);
		if (old != null) {
			speed.removeModifier(old);
		}
		if (ticks > 0) {
			int x = MathHelper.floor_double(living.posX);
			int y = MathHelper.floor_double(living.boundingBox.minY - 0.2D);
			int z = MathHelper.floor_double(living.posZ);
			if (living.worldObj.getBlock(x, y, z).getMaterial() != Material.air) {
				float slow = -0.05F * Math.min(ticks, TICKS_TO_FREEZE) / TICKS_TO_FREEZE;
				speed.applyModifier(new AttributeModifier(SPEED_ID, "Powder snow slow", slow, 0).setSaved(false));
			}
		}
	}

	private static void hurt(EntityLivingBase living) {
		if (living instanceof EntityPlayer && !living.worldObj.getGameRules().getGameRuleBooleanValue(GAMERULE)) {
			return;
		}
		boolean extra = living instanceof EntityBlaze || living instanceof EntityMagmaCube;
		living.attackEntityFrom(FREEZE, extra ? 5.0F : 1.0F);
	}

	/** Tells the player its own frozen ticks whenever they change, and those tracking a mob when it is fully frozen or not. */
	private static void sync(EntityLivingBase living, IFreezingEntity state) {
		int ticks = state.etfu$getTicksFrozen();
		int sent = state.etfu$lastSentTicks();
		if (ticks == sent) {
			return;
		}
		if (living instanceof EntityPlayerMP) {
			McFlyNetwork.CHANNEL.sendTo(new FreezingMessage(living.getEntityId(), ticks), (EntityPlayerMP) living);
		}
		if (sent >= TICKS_TO_FREEZE != ticks >= TICKS_TO_FREEZE && living.worldObj instanceof WorldServer) {
			((WorldServer) living.worldObj).getEntityTracker().func_151247_a(living,
					McFlyNetwork.CHANNEL.getPacketFrom(new FreezingMessage(living.getEntityId(), ticks)));
		}
		state.etfu$setLastSentTicks(ticks);
	}

	/** A skeleton turning into a stray (spec §62.7; 26.3 {@code Skeleton.freezingTracker.isConverting}), on either side. */
	public static boolean isConvertingToStray(Entity entity) {
		return entity instanceof ganymedes01.etfuturum.ducks.IStrayConversion && ((ganymedes01.etfuturum.ducks.IStrayConversion) entity).etfu$isConverting();
	}

	/** A player starting to see a frozen mob is told (Forge's {@code PlayerEvent.StartTracking}). */
	public static void startTracking(Entity target, EntityPlayerMP watcher) {
		if (isConvertingToStray(target)) {
			McFlyNetwork.CHANNEL.sendTo(new io.github.asgerjon.mcfly.network.StrayConversionMessage(target.getEntityId(), true), watcher);
		}
		int ticks = getTicksFrozen(target);
		if (ticks > 0 && target instanceof EntityLivingBase) {
			McFlyNetwork.CHANNEL.sendTo(new FreezingMessage(target.getEntityId(), ticks), watcher);
		}
	}
}
