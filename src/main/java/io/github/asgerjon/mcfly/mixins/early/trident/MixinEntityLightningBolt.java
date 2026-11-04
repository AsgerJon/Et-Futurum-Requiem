package io.github.asgerjon.mcfly.mixins.early.trident;

import io.github.asgerjon.mcfly.McFly;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.trident.IVisualBolt;
import ganymedes01.etfuturum.trident.Lightning;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A bolt as 26.3's (SPEC-modern-movement.md §58.3): on its first tick on the server it acts on the block it lands on
 * (Lightning.landed), it sounds as modern's (thunder and impact), and it can be visual-only (Lightning.visualBolt):
 * no fire, no one struck.
 */
@Mixin(EntityLightningBolt.class)
public abstract class MixinEntityLightningBolt implements IVisualBolt {

	@Shadow
	private int lightningState;

	@Unique
	private boolean etfu$landed;
	@Unique
	private boolean etfu$visualOnly;

	@Override
	public boolean etfu$isVisualOnly() {
		return etfu$visualOnly;
	}

	@Inject(method = "<init>(Lnet/minecraft/world/World;DDD)V", at = @At("RETURN"))
	private void etfu$visualOnly(CallbackInfo ci) {
		etfu$visualOnly = Lightning.isMakingAVisualBolt();
	}

	/** The constructor's fire, before the bolt could be marked: Lightning's flag says whether it's a visual one. */
	@WrapOperation(method = "<init>(Lnet/minecraft/world/World;DDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;)Z"))
	private boolean etfu$noFireAtFirst(World world, int x, int y, int z, Block block, Operation<Boolean> original) {
		return !Lightning.isMakingAVisualBolt() && original.call(world, x, y, z, block);
	}

	@WrapOperation(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;)Z"))
	private boolean etfu$noFireLater(World world, int x, int y, int z, Block block, Operation<Boolean> original) {
		return !etfu$visualOnly && original.call(world, x, y, z, block);
	}

	@WrapOperation(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getEntitiesWithinAABBExcludingEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;"))
	private List<?> etfu$noOneStruck(World world, Entity bolt, AxisAlignedBB box, Operation<List<?>> original) {
		return etfu$visualOnly ? new ArrayList<>() : original.call(world, bolt, box);
	}

	@Inject(method = "onUpdate", at = @At("HEAD"))
	private void etfu$land(CallbackInfo ci) {
		EntityLightningBolt bolt = (EntityLightningBolt) (Object) this;
		if (!etfu$landed && lightningState == 2 && !bolt.worldObj.isRemote) {
			etfu$landed = true;
			Lightning.landed(bolt);
		}
	}

	@ModifyConstant(method = "onUpdate", constant = @Constant(stringValue = "ambient.weather.thunder"))
	private String etfu$modernThunder(String sound) {
		return McFly.MC_ASSETS + ":entity.lightning_bolt.thunder";
	}

	@ModifyConstant(method = "onUpdate", constant = @Constant(stringValue = "random.explode"))
	private String etfu$modernImpact(String sound) {
		return McFly.MC_ASSETS + ":entity.lightning_bolt.impact";
	}
}
