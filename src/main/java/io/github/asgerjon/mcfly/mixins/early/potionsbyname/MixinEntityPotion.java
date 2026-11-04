package io.github.asgerjon.mcfly.mixins.early.potionsbyname;

import ganymedes01.etfuturum.potion.ThrownPotions;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A thrown potion kept by name shows its colour (SPEC-modern-movement.md §69.7, stage C): its item rides in its
 * DataWatcher (1.7.10's client gets only the damage value in the spawn packet), and its splash's world event carries
 * the colour (`ThrownPotions.splashData`) instead of the damage value.
 */
@Mixin(EntityPotion.class)
public abstract class MixinEntityPotion extends EntityThrowable {

	@Shadow
	private ItemStack potionDamage;

	public MixinEntityPotion(World world) {
		super(world);
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		dataWatcher.addObjectByDataType(ThrownPotions.WATCHER, 5);
	}

	@Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;)V", at = @At("RETURN"))
	private void etfu$syncThrown(CallbackInfo ci) {
		etfu$sync();
	}

	@Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", at = @At("RETURN"))
	private void etfu$syncPlaced(CallbackInfo ci) {
		etfu$sync();
	}

	@Inject(method = "setPotionDamage", at = @At("RETURN"))
	private void etfu$syncDamage(CallbackInfo ci) {
		etfu$sync();
	}

	@Inject(method = "readEntityFromNBT", at = @At("RETURN"))
	private void etfu$syncLoaded(NBTTagCompound tag, CallbackInfo ci) {
		etfu$sync();
	}

	@ModifyArg(method = "onImpact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playAuxSFX(IIIII)V"), index = 4)
	private int etfu$splashData(int damage) {
		return potionDamage == null ? damage : ThrownPotions.splashData(potionDamage);
	}

	@Unique
	private void etfu$sync() {
		if (worldObj != null && !worldObj.isRemote && potionDamage != null) {
			dataWatcher.updateObject(ThrownPotions.WATCHER, potionDamage.copy());
		}
	}
}
