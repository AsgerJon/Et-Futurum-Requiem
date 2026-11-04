package io.github.asgerjon.mcfly.mixins.early.quickwins4;

import ganymedes01.etfuturum.ducks.IAggressive;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7.10's pigman has no AI tasks (26.3's zombified piglin has the zombie's attack goal): its chase and its
 * {@code attackTime} (20 after a hit) stand in for the goal and its next attack (SPEC-modern-movement.md §63.9).
 */
@Mixin(EntityPigZombie.class)
public abstract class MixinEntityPigZombie extends EntityZombie {

	@Unique
	private int etfu$chaseTicks;

	public MixinEntityPigZombie(World world) {
		super(world);
	}

	@Inject(method = "onUpdate", at = @At("TAIL"))
	private void etfu$aggressive(CallbackInfo ci) {
		if (worldObj.isRemote) {
			return;
		}
		etfu$chaseTicks = getEntityToAttack() != null ? etfu$chaseTicks + 1 : 0;
		((IAggressive) this).etfu$setAggressive(etfu$chaseTicks >= 5 && attackTime < 10);
	}
}
