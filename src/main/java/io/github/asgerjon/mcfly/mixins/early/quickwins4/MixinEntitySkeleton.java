package io.github.asgerjon.mcfly.mixins.early.quickwins4;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.ducks.IAggressive;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The skeleton's aggressive flag (26.3 {@code Mob.isAggressive}), synced for its arms (SPEC-modern-movement.md §64.1). */
@Mixin(EntitySkeleton.class)
public abstract class MixinEntitySkeleton extends EntityMob implements IAggressive {

	public MixinEntitySkeleton(World world) {
		super(world);
	}

	@Inject(method = "entityInit", at = @At("TAIL"))
	private void etfu$aggressiveWatcher(CallbackInfo ci) {
		dataWatcher.addObject(McFlyConfig.aggressiveDataWatcherId, (byte) 0);
	}

	@Override
	public boolean etfu$isAggressive() {
		return dataWatcher.getWatchableObjectByte(McFlyConfig.aggressiveDataWatcherId) != 0;
	}

	@Override
	public void etfu$setAggressive(boolean aggressive) {
		dataWatcher.updateObject(McFlyConfig.aggressiveDataWatcherId, (byte) (aggressive ? 1 : 0));
	}
}
