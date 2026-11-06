package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.entity.Entity;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An explosion posts `explode` at its middle, by what set it off, before it does anything (SPEC-modern-movement.md §90.9, step H1; 26.3's `ServerExplosion.explode`). */
@Mixin(Explosion.class)
public abstract class MixinExplosion {

	@Shadow
	private World worldObj;
	@Shadow
	public double explosionX;
	@Shadow
	public double explosionY;
	@Shadow
	public double explosionZ;
	@Shadow
	public Entity exploder;

	@Inject(method = "doExplosionA", at = @At("HEAD"))
	private void mcfly$explode(CallbackInfo ci) {
		GameEvents.post(worldObj, GameEvent.get("explode"), explosionX, explosionY, explosionZ, exploder, null, 0);
	}
}
