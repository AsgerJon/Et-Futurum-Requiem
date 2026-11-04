package io.github.asgerjon.mcfly.mixins.early.tntminecart;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityMinecartTNT;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The TNT minecart's break as 26.3's (SPEC-modern-movement.md §59, §61.1). Broken when it would go off (the damage from
 * a burning projectile, or fire or an explosion, or while it moves sideways at speed² 0.01 or more): with
 * `modernTntMinecarts` it is lit with a fuse of 0 to 38 ticks and stays, else with `dropVehiclesTogether` it goes off at
 * once and drops nothing. Otherwise it drops as one (MixinEntityMinecart; its separate TNT skipped here). A burning arrow
 * sets it off at once (`modernTntMinecarts`).
 */
@Mixin(EntityMinecartTNT.class)
public abstract class MixinEntityMinecartTNT extends EntityMinecart {

	@Shadow
	private int minecartTNTFuse;

	@Shadow
	protected abstract void explodeCart(double speedSqr);

	@Shadow
	public abstract void ignite();

	private MixinEntityMinecartTNT(World world) {
		super(world);
	}

	@Inject(method = "killMinecart", at = @At("HEAD"), cancellable = true)
	private void etfu$breakAsModern(DamageSource source, CallbackInfo ci) {
		double speedSqr = motionX * motionX + motionZ * motionZ;
		if (!etfu$ignites(source) && speedSqr < 0.01D) {
			return;
		}
		if (McFlyConfig.modernTntMinecarts) {
			ci.cancel();
			if (minecartTNTFuse < 0) {
				ignite();
				minecartTNTFuse = rand.nextInt(20) + rand.nextInt(20);
			}
		} else if (McFlyConfig.dropVehiclesTogether) {
			ci.cancel();
			setDead();
			explodeCart(speedSqr);
		}
	}

	/** 26.3's: from a projectile, when it burns; else fire or an explosion. */
	private static boolean etfu$ignites(DamageSource source) {
		Entity direct = source.getSourceOfDamage();
		if (direct instanceof IProjectile || direct instanceof EntityFireball || direct instanceof EntityThrowable) {
			return direct.isBurning();
		}
		return source.isFireDamage() || source.isExplosion();
	}

	@WrapOperation(method = "killMinecart", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityMinecartTNT;entityDropItem(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/item/EntityItem;"))
	private EntityItem etfu$noSeparateTnt(EntityMinecartTNT cart, ItemStack tnt, float offset, Operation<EntityItem> original) {
		return McFlyConfig.dropVehiclesTogether ? null : original.call(cart, tnt, offset);
	}

	/** A burning arrow sets it off at once, with power from the arrow's speed (26.3's). */
	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (McFlyConfig.modernTntMinecarts && !worldObj.isRemote && !isDead && source.getSourceOfDamage() instanceof EntityArrow arrow && arrow.isBurning()) {
			explodeCart(arrow.motionX * arrow.motionX + arrow.motionY * arrow.motionY + arrow.motionZ * arrow.motionZ);
		}
		return super.attackEntityFrom(source, amount);
	}
}
