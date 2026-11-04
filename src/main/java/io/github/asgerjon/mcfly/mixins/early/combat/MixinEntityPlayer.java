package io.github.asgerjon.mcfly.mixins.early.combat;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.particle.McFlyParticles;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModEnchantments;
import ganymedes01.etfuturum.combat.AttackCooldown;
import ganymedes01.etfuturum.combat.DamageRules;
import ganymedes01.etfuturum.combat.ICombatPlayer;
import ganymedes01.etfuturum.combat.ItemCooldowns;
import ganymedes01.etfuturum.items.equipment.ItemShield;
import ganymedes01.etfuturum.enchantment.SweepingEdge;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * The attack cooldown on players (spec §46.1, §46.3): the timers, the attack speed attribute, and the attack's damage,
 * knockback, critical and sweep rules by charge, with modern's attack sounds.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase implements ICombatPlayer {

	@Shadow
	public InventoryPlayer inventory;

	@Unique
	private int etfu$attackTicks;
	@Unique
	private int etfu$swapTicks;
	@Unique
	private ItemStack etfu$lastMainHand;
	@Unique
	private final ItemCooldowns etfu$cooldowns = new ItemCooldowns();
	/** The attack being worked out: its charge, and what it is. */
	@Unique
	private float etfu$charge;
	@Unique
	private boolean etfu$knockbackAttack;
	@Unique
	private boolean etfu$critical;
	@Unique
	private boolean etfu$sweep;
	/** The attack's damage by charge, before a critical; a sweep's share of it comes from Sweeping Edge. */
	@Unique
	private float etfu$sweepBase;

	public MixinEntityPlayer(World world) {
		super(world);
	}

	@Override
	public int etfu$getAttackTicks() {
		return etfu$attackTicks;
	}

	@Override
	public int etfu$getSwapTicks() {
		return etfu$swapTicks;
	}

	@Override
	public void etfu$resetAttackStrength() {
		etfu$attackTicks = 0;
		etfu$swapTicks = 0;
	}

	@Override
	public void etfu$resetAttackTicks() {
		etfu$attackTicks = 0;
	}

	@Override
	public ItemCooldowns etfu$getCooldowns() {
		return etfu$cooldowns;
	}

	/** 1.7.10 halves damage while blocking; a shield blocks in full from the front and not at all from behind (§46.9). */
	@ModifyExpressionValue(method = "damageEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;isBlocking()Z"))
	private boolean etfu$noHalvingForShields(boolean blocking) {
		ItemStack inUse = ((EntityPlayer) (Object) this).getItemInUse();
		return blocking && !(inUse != null && inUse.getItem() instanceof ItemShield);
	}

	/**
	 * The player's armour as modern's (§46.10), in place of Forge's step: vanilla armour and EFR's netherite by the
	 * modern formula; other mods' ISpecialArmor still through Forge's step, on what is left.
	 */
	@WrapOperation(method = "damageEntity", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ISpecialArmor$ArmorProperties;ApplyArmor(Lnet/minecraft/entity/EntityLivingBase;[Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/DamageSource;D)F", remap = false))
	private float etfu$modernArmour(EntityLivingBase entity, ItemStack[] armour, DamageSource source, double damage, Operation<Float> original) {
		return DamageRules.playerArmour((EntityPlayer) entity, armour, source, (float) damage);
	}

	/**
	 * The hit's extra push (Knockback, a sprint hit at full charge) as modern's knockback (§46.10): it halves the
	 * target's motion and counts its resistance, where 1.7.10 added it on top of everything. Anything not living is
	 * pushed as before.
	 */
	@WrapOperation(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"))
	private void etfu$modernExtraKnockback(Entity target, double x, double y, double z, Operation<Void> original) {
		if (target instanceof EntityLivingBase living) {
			DamageRules.knockback(living, Math.sqrt(x * x + z * z), -x, -z);
		} else {
			original.call(target, x, y, z);
		}
	}

	@Inject(method = "applyEntityAttributes", at = @At("TAIL"))
	private void etfu$registerAttackSpeed(CallbackInfo ci) {
		this.getAttributeMap().registerAttribute(AttackCooldown.ATTACK_SPEED);
	}

	/** Both timers count up; a different item in the main hand restarts them (more of the same doesn't). */
	@Inject(method = "onUpdate", at = @At("TAIL"))
	private void etfu$tickAttackStrength(CallbackInfo ci) {
		etfu$attackTicks++;
		etfu$swapTicks++;
		etfu$cooldowns.tick();
		ItemStack held = inventory.getCurrentItem();
		if (!ItemStack.areItemStacksEqual(etfu$lastMainHand, held)) {
			if (!etfu$isSameItem(etfu$lastMainHand, held)) {
				etfu$resetAttackStrength();
			}
			etfu$lastMainHand = held == null ? null : held.copy();
		}
	}

	/** Modern's "same item": 1.7.10 variants with their own data value (wool colours) are different items. */
	@Unique
	private static boolean etfu$isSameItem(ItemStack a, ItemStack b) {
		if (a == null || b == null) {
			return a == b;
		}
		return a.getItem() == b.getItem() && (!a.getHasSubtypes() || a.getItemDamage() == b.getItemDamage());
	}

	@Inject(method = "dropOneItem", at = @At("TAIL"))
	private void etfu$dropRestarts(boolean wholeStack, CallbackInfoReturnable<EntityItem> cir) {
		if (!this.worldObj.isRemote) {
			etfu$resetAttackStrength();
		}
	}

	/** The weapon's damage by charge, half a tick ahead; working it out is the attack, which restarts the timer. */
	@ModifyVariable(method = "attackTargetEntityWithCurrentItem", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
	private float etfu$damageByCharge(float damage) {
		etfu$charge = AttackCooldown.charge((EntityPlayer) (Object) this, 0.5F);
		etfu$resetAttackTicks();
		return damage * AttackCooldown.damageScale(etfu$charge);
	}

	/** The enchantment bonus by charge. */
	@ModifyExpressionValue(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;getEnchantmentModifierLiving(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/entity/EntityLivingBase;)F"))
	private float etfu$enchantmentByCharge(float bonus) {
		return bonus * etfu$charge;
	}

	/** Sprinting adds knockback only at full charge. */
	@ModifyExpressionValue(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;isSprinting()Z", ordinal = 0))
	private boolean etfu$knockbackAtFullCharge(boolean sprinting) {
		etfu$knockbackAttack = sprinting && etfu$charge > AttackCooldown.FULL;
		return etfu$knockbackAttack;
	}

	/**
	 * Inside "some damage": the knockback sound, the critical rule (full charge, not sprinting) and whether this is a
	 * sweep, decided before the hit as in modern.
	 */
	@ModifyVariable(method = "attackTargetEntityWithCurrentItem", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
	private boolean etfu$criticalAtFullCharge(boolean critical, @Local(ordinal = 0) float damage) {
		boolean full = etfu$charge > AttackCooldown.FULL;
		if (etfu$knockbackAttack) {
			etfu$playAttackSound("knockback");
		}
		etfu$critical = critical && full && !this.isSprinting();
		etfu$sweepBase = damage;
		etfu$sweep = full && !etfu$critical && !etfu$knockbackAttack && this.onGround && etfu$isSlowEnoughToSweep()
			&& inventory.getCurrentItem() != null && inventory.getCurrentItem().getItem() instanceof ItemSword;
		return etfu$critical;
	}

	/** Moving less than 2.5 × the movement speed attribute sideways since the last position (§46.6). */
	@Unique
	private boolean etfu$isSlowEnoughToSweep() {
		double dx = this.posX - this.prevPosX;
		double dz = this.posZ - this.prevPosZ;
		double limit = this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue() * 2.5D;
		return dx * dx + dz * dz < limit * limit;
	}

	/** After the hit: the sweep, then the sounds; a hit that does nothing gets the no-damage sound. */
	@WrapOperation(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
	private boolean etfu$afterHit(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
		boolean hurt = original.call(target, source, amount);
		if (this.worldObj.isRemote) {
			return hurt;
		}
		if (!hurt) {
			etfu$playAttackSound("nodamage");
			return false;
		}
		if (etfu$sweep) {
			etfu$sweepAround(target);
		}
		if (etfu$critical) {
			etfu$playAttackSound("crit");
		} else if (!etfu$sweep) {
			etfu$playAttackSound(etfu$charge > AttackCooldown.FULL ? "strong" : "weak");
		}
		return true;
	}

	/**
	 * The sweep (§46.3): living things by the target, within 3 of the attacker and not on its team, take 1 plus
	 * Sweeping Edge's share of the attack's damage plus their enchantment bonus, all by charge, and are knocked back
	 * along the attacker's facing.
	 */
	@Unique
	private void etfu$sweepAround(Entity target) {
		EntityPlayer self = (EntityPlayer) (Object) this;
		float yaw = this.rotationYaw * (float) Math.PI / 180.0F;
		int sweepingEdge = ModEnchantments.sweepingEdge == null ? 0
			: EnchantmentHelper.getEnchantmentLevel(ModEnchantments.sweepingEdge.effectId, inventory.getCurrentItem());
		float sweepDamage = 1.0F + SweepingEdge.damageRatio(sweepingEdge) * etfu$sweepBase;
		@SuppressWarnings("unchecked")
		List<EntityLivingBase> nearby = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, target.boundingBox.expand(1.0D, 0.25D, 1.0D));
		for (EntityLivingBase other : nearby) {
			if (other == self || other == target || this.isOnSameTeam(other) || this.getDistanceSqToEntity(other) >= 9.0D) {
				continue;
			}
			float damage = (sweepDamage + EnchantmentHelper.getEnchantmentModifierLiving(self, other)) * etfu$charge;
			if (other.attackEntityFrom(DamageSource.causePlayerDamage(self), damage)) {
				other.knockBack(self, 0.4F, MathHelper.sin(yaw), -MathHelper.cos(yaw));
				EnchantmentHelper.func_151384_a(other, self);
				EnchantmentHelper.func_151385_b(self, other);
			}
		}
		etfu$playAttackSound("sweep");
		// the sweep's particle, a block ahead at half height, for the players around: 26.3's
		// sendParticles(SWEEP_ATTACK, x + xd, y(0.5), z + zd, 0, xd, 0, zd, 0), by name (spec §79 B7)
		double xd = -MathHelper.sin(yaw);
		double zd = MathHelper.cos(yaw);
		if (this.worldObj instanceof net.minecraft.world.WorldServer server) {
			McFlyParticles.send(server, McFlyParticles.SWEEP_ATTACK, this.posX + xd, this.posY + this.height * 0.5D, this.posZ + zd, 0, xd, 0.0D, zd, 0.0D);
		}
	}

	@Unique
	private void etfu$playAttackSound(String name) {
		if (!this.worldObj.isRemote) {
			this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, McFly.MC_ASSETS + ":entity.player.attack." + name, 1.0F, 1.0F);
		}
	}
}
