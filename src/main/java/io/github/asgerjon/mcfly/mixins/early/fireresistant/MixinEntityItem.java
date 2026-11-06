package io.github.asgerjon.mcfly.mixins.early.fireresistant;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.base.FireResistant;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fire-resistant items' entities (SPEC-modern-movement.md §88.4), as 26.3's `ItemEntity.fireImmune`: immune to fire
 * (lava doesn't hurt or light them), no fizz and hop out of lava (§30's floating keeps them up), fire damage refused.
 * By {@link FireResistant}, each tick (the stack can change).
 */
@Mixin(value = EntityItem.class, priority = 1001)
public abstract class MixinEntityItem extends Entity {

	@Shadow
	public abstract ItemStack getEntityItem();

	public MixinEntityItem(World world) {
		super(world);
	}

	@Inject(method = "onUpdate", at = @At("HEAD"))
	private void mcfly$fireResistant(CallbackInfo ci) {
		if (FireResistant.is(getEntityItem())) {
			this.isImmuneToFire = true;
			this.extinguish();
		}
	}

	/** The lava check of 1.7.10's item tick (a hop and a fizz; its first `getBlock`, the second is friction): nothing for a fire-resistant item. */
	@WrapOperation(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;", ordinal = 0))
	private Block mcfly$noFizz(World world, int x, int y, int z, Operation<Block> original) {
		return isImmuneToFire ? Blocks.air : original.call(world, x, y, z);
	}

	@Inject(method = "attackEntityFrom", at = @At("HEAD"), cancellable = true)
	private void mcfly$noFireDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (isImmuneToFire && source.isFireDamage()) {
			cir.setReturnValue(false);
		}
	}
}
