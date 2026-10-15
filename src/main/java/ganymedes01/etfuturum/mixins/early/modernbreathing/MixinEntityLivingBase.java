package ganymedes01.etfuturum.mixins.early.modernbreathing;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {

	public MixinEntityLivingBase(World world) {
		super(world);
	}

	/**
	 * Out of water, air comes back 4 per tick (1.5 seconds from empty) instead of all at once, like modern. The third
	 * setAir call in onEntityUpdate is the refill.
	 */
	@ModifyArg(method = "onEntityUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;setAir(I)V", ordinal = 2))
	private int etfu$refillAirGradually(int maxAir) {
		return Math.min(this.getAir() + 4, maxAir);
	}
}
