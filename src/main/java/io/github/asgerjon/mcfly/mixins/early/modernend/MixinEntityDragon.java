package io.github.asgerjon.mcfly.mixins.early.modernend;

import ganymedes01.etfuturum.world.end.modern.EnderDragonFight;
import ganymedes01.etfuturum.world.end.modern.ModernEnd;
import ganymedes01.etfuturum.world.end.modern.dragon.ModernDragon;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.7.10's dragon as 26.3's (SPEC-modern-movement.md §76.6, §76.7): `ModernDragon` has its phases, flight, parts, damage
 * and death; its death tells the fight instead of building 1.7.10's portal where it died.
 */
@Mixin(EntityDragon.class)
public class MixinEntityDragon implements ModernDragon.IModernDragon {

	@Unique
	private ModernDragon etfu$modern;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void etfu$make(World world, CallbackInfo ci) {
		if (ModernEnd.isActive()) {
			etfu$modern = new ModernDragon((EntityDragon) (Object) this);
		}
	}

	@Override
	public ModernDragon etfu$modern() {
		return etfu$modern;
	}

	@Inject(method = "entityInit", at = @At("TAIL"))
	private void etfu$phaseWatcher(CallbackInfo ci) {
		((EntityDragon) (Object) this).getDataWatcher().addObject(ModernDragon.PHASE_WATCHER, 10);
	}

	@Inject(method = "onLivingUpdate", at = @At("HEAD"), cancellable = true)
	private void etfu$tick(CallbackInfo ci) {
		if (etfu$modern != null) {
			etfu$modern.aiStep();
			ci.cancel();
		}
	}

	@Inject(method = "attackEntityFromPart", at = @At("HEAD"), cancellable = true)
	private void etfu$hurtPart(EntityDragonPart part, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (etfu$modern != null) {
			cir.setReturnValue(etfu$modern.hurt(part, source, amount));
		}
	}

	/** Damage to the dragon itself goes to its body (26.3's). */
	@Inject(method = "attackEntityFrom", at = @At("HEAD"), cancellable = true)
	private void etfu$hurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (etfu$modern != null) {
			EntityDragon dragon = (EntityDragon) (Object) this;
			cir.setReturnValue(etfu$modern.hurt(dragon.dragonPartBody, source, amount));
		}
	}

	@Inject(method = "onDeathUpdate", at = @At("HEAD"), cancellable = true)
	private void etfu$death(CallbackInfo ci) {
		if (etfu$modern != null) {
			etfu$modern.tickDeath();
			ci.cancel();
		}
	}

	@Inject(method = "createEnderPortal", at = @At("HEAD"), cancellable = true)
	private void etfu$fightPortal(int x, int z, CallbackInfo ci) {
		EntityDragon dragon = (EntityDragon) (Object) this;
		EnderDragonFight fight = EnderDragonFight.ifThere(dragon.worldObj);
		if (fight != null) {
			fight.setDragonKilled(dragon);
			ci.cancel();
		}
	}
}
