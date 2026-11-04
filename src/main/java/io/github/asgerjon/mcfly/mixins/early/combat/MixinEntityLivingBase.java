package io.github.asgerjon.mcfly.mixins.early.combat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.combat.DamageRules;
import ganymedes01.etfuturum.combat.ShieldBlocking;
import ganymedes01.etfuturum.items.equipment.ItemShield;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shields block (spec §46.9), where modern blocks: after the invulnerability and fire resistance checks, before the
 * helmet and the hurt timer. A blocked hit does no damage, no flash, no knockback and counts as not landing; outside
 * the hurt timer it starts it, at 0, as modern's zero-damage hit does. And the damage rules (§46.10): mobs' armour by
 * modern's formula (players' goes through Forge's step, MixinEntityPlayer), and modern's knockback for everyone.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {

	@Shadow
	public int maxHurtResistantTime;

	@Shadow
	protected float lastDamage;

	@Shadow
	public abstract void setRevengeTarget(EntityLivingBase target);

	@Shadow
	public abstract int getTotalArmorValue();

	@Shadow
	protected abstract void damageArmor(float damage);

	public MixinEntityLivingBase(World world) {
		super(world);
	}

	@Inject(method = "attackEntityFrom", at = @At(value = "FIELD", target = "Lnet/minecraft/util/DamageSource;anvil:Lnet/minecraft/util/DamageSource;", opcode = Opcodes.GETSTATIC, ordinal = 0), cancellable = true)
	private void etfu$shieldBlocks(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (!ShieldBlocking.tryBlock((EntityLivingBase) (Object) this, source, amount)) {
			return;
		}
		if (!((float) this.hurtResistantTime > (float) this.maxHurtResistantTime / 2.0F)) {
			this.lastDamage = 0.0F;
			this.hurtResistantTime = this.maxHurtResistantTime;
		}
		if (source.getEntity() instanceof EntityLivingBase attacker) {
			this.setRevengeTarget(attacker);
		}
		cir.setReturnValue(false);
	}

	/** Armour by modern's formula, with toughness; the wear as before (none for mobs, as in modern). */
	@Inject(method = "applyArmorCalculations", at = @At("HEAD"), cancellable = true)
	private void etfu$modernArmour(DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
		if (!source.isUnblockable()) {
			this.damageArmor(damage);
			damage = DamageRules.afterArmour(damage, this.getTotalArmorValue(), DamageRules.wornToughness((EntityLivingBase) (Object) this));
		}
		cir.setReturnValue(damage);
	}

	/** Being hurt pushes as modern's knockback does: 0.4 less the resistance, lifting only on the ground. */
	@Inject(method = "knockBack", at = @At("HEAD"), cancellable = true)
	private void etfu$modernKnockback(Entity attacker, float unused, double x, double z, CallbackInfo ci) {
		DamageRules.knockback((EntityLivingBase) (Object) this, 0.4D, x, z);
		ci.cancel();
	}

	/**
	 * A shield that wears out breaks with its own sound, for everyone and its holder too (modern's break_sound,
	 * item.shield.break at 0.8, pitch 0.8 to 1.2). 1.7.10 played random.break through EntityPlayer.playSound, which
	 * leaves the player out. The rest (the particles) is as before.
	 */
	@WrapOperation(method = "renderBrokenItemStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;playSound(Ljava/lang/String;FF)V"))
	private void etfu$shieldBreakSound(EntityLivingBase self, String sound, float volume, float pitch, Operation<Void> original, @Local(argsOnly = true) ItemStack broken) {
		if (broken == null || !(broken.getItem() instanceof ItemShield)) {
			original.call(self, sound, volume, pitch);
		} else if (!this.worldObj.isRemote) {
			this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, Reference.MCAssetVer + ":item.shield.break", volume, pitch);
		}
	}
}
