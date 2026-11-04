package io.github.asgerjon.mcfly.mixins.early.quickwins4;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.core.utils.FilledResult;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Milking in creative adds a milk bucket, once (26.3 {@code Cow.mobInteract}; spec §63.1); 1.7.10 gave none. */
@Mixin(EntityCow.class)
public abstract class MixinEntityCow extends EntityAnimal {

	public MixinEntityCow(World world) {
		super(world);
	}

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void etfu$creativeMilk(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
		ItemStack held = player.inventory.getCurrentItem();
		if (McFlyConfig.modernCreativeFilling && player.capabilities.isCreativeMode && held != null && held.getItem() == Items.bucket && !isChild()) {
			if (!worldObj.isRemote) {
				FilledResult.fill(player, held, new ItemStack(Items.milk_bucket));
			}
			cir.setReturnValue(true);
		}
	}
}
