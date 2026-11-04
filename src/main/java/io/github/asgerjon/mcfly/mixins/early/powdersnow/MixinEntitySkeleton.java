package io.github.asgerjon.mcfly.mixins.early.powdersnow;

import io.github.asgerjon.mcfly.network.McFlyNetwork;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ducks.IStrayConversion;
import ganymedes01.etfuturum.entities.EntityStray;
import ganymedes01.etfuturum.entities.Freezing;
import ganymedes01.etfuturum.lib.Reference;
import io.github.asgerjon.mcfly.network.StrayConversionMessage;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A skeleton in powder snow turns into a stray (SPEC-modern-movement.md §62.7; 26.3 {@code Skeleton.freezingTracker}, a
 * {@code ConversionTracker}): 140 ticks in it start 300 of converting (shaking), then a stray takes its place with its
 * equipment, name and persistence; out of the snow it all starts over. Only 1.7.10's plain skeleton (type 0), not a
 * wither skeleton or EFR's stray (a skeleton subclass).
 */
@Mixin(EntitySkeleton.class)
public abstract class MixinEntitySkeleton extends EntityMob implements IStrayConversion {

	@Unique
	private int etfu$afflictionTime;
	@Unique
	private int etfu$conversionTime;
	@Unique
	private boolean etfu$converting;

	public MixinEntitySkeleton(World world) {
		super(world);
	}

	@Override
	public boolean etfu$isConverting() {
		return etfu$converting;
	}

	@Override
	public void etfu$setConvertingSeen(boolean converting) {
		etfu$converting = converting;
	}

	@Unique
	private boolean etfu$canConvert() {
		return ((EntitySkeleton) (Object) this).getSkeletonType() == 0 && !((Object) this instanceof EntityStray)
				&& ganymedes01.etfuturum.configuration.configs.ConfigEntities.enableStray;
	}

	/**
	 * 26.3 {@code ConversionTracker.tick}, run before the skeleton's own tick, so "in powder snow" is last tick's: here at
	 * the start of the living update (1.7.10's skeleton has no tick of its own), where that is {@code wasInPowderSnow}.
	 */
	@Inject(method = "onLivingUpdate", at = @At("HEAD"))
	private void etfu$freezeIntoStray(CallbackInfo ci) {
		if (worldObj.isRemote || !isEntityAlive() || !etfu$canConvert()) {
			return;
		}
		if (Freezing.wasInPowderSnow(this)) {
			if (etfu$converting) {
				if (--etfu$conversionTime < 0) {
					etfu$convert();
				}
			} else if (++etfu$afflictionTime >= 140) {
				etfu$startConversion(300);
			}
		} else {
			etfu$afflictionTime = -1;
			etfu$setConverting(false);
		}
	}

	@Unique
	private void etfu$startConversion(int time) {
		etfu$conversionTime = time;
		etfu$setConverting(true);
	}

	@Unique
	private void etfu$setConverting(boolean converting) {
		if (etfu$converting != converting) {
			etfu$converting = converting;
			if (worldObj instanceof WorldServer) {
				((WorldServer) worldObj).getEntityTracker().func_151247_a(this,
						McFlyNetwork.CHANNEL.getPacketFrom(new StrayConversionMessage(getEntityId(), converting)));
			}
		}
	}

	/** 26.3 {@code convertTo} with {@code ConversionParams.single(mob, true, true)}: equipment and its drop chances kept. */
	@Unique
	private void etfu$convert() {
		EntityStray stray = new EntityStray(worldObj);
		stray.copyLocationAndAnglesFrom(this);
		stray.renderYawOffset = renderYawOffset;
		stray.rotationYawHead = rotationYawHead;
		for (int slot = 0; slot < 5; slot++) {
			stray.setCurrentItemOrArmor(slot, getEquipmentInSlot(slot) == null ? null : getEquipmentInSlot(slot).copy());
			stray.setEquipmentDropChance(slot, equipmentDropChances[slot]);
		}
		stray.setCanPickUpLoot(canPickUpLoot());
		if (hasCustomNameTag()) {
			stray.setCustomNameTag(getCustomNameTag());
			stray.setAlwaysRenderNameTag(getAlwaysRenderNameTag());
		}
		if (isNoDespawnRequired()) {
			stray.func_110163_bv();
		}
		if (getLeashed() && getLeashedToEntity() != null) {
			stray.setLeashedToEntity(getLeashedToEntity(), true);
			clearLeashed(true, false);
		}
		setDead();
		worldObj.spawnEntityInWorld(stray);
		// 26.3's level event 1048
		worldObj.playSoundEffect(stray.posX, stray.posY, stray.posZ, Reference.MCAssetVer + ":entity.skeleton.converted_to_stray", 2.0F,
				(rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
	}

	@Inject(method = "writeEntityToNBT", at = @At("TAIL"))
	private void etfu$saveConversion(NBTTagCompound tag, CallbackInfo ci) {
		if (etfu$canConvert()) {
			tag.setInteger("StrayConversionTime", etfu$converting ? etfu$conversionTime : -1);
			tag.setInteger("FreezingTime", Freezing.isInPowderSnow(this) || Freezing.wasInPowderSnow(this) ? etfu$afflictionTime : -1);
		}
	}

	@Inject(method = "readEntityFromNBT", at = @At("TAIL"))
	private void etfu$loadConversion(NBTTagCompound tag, CallbackInfo ci) {
		etfu$afflictionTime = tag.hasKey("FreezingTime") ? tag.getInteger("FreezingTime") : 0;
		int conversion = tag.hasKey("StrayConversionTime") ? tag.getInteger("StrayConversionTime") : -1;
		if (conversion != -1) {
			etfu$startConversion(conversion);
		} else {
			etfu$converting = false;
		}
	}
}
