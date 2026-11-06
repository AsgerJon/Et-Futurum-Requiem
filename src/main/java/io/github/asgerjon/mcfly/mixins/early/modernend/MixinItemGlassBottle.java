package io.github.asgerjon.mcfly.mixins.early.modernend;

import io.github.asgerjon.mcfly.McFly;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.entities.EntityAreaEffectCloud;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemGlassBottle;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * A glass bottle used within 2 of a cloud a dragon made takes dragon's breath, the cloud half a block smaller (26.3's
 * BottleItem; SPEC-modern-movement.md §76.8).
 */
/* Before quickwins4's creative water filling (26.3 checks for a dragon's cloud first). */
@Mixin(value = ItemGlassBottle.class, priority = 900)
public class MixinItemGlassBottle {

	@Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
	private void etfu$dragonBreath(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
		if (!io.github.asgerjon.mcfly.registry.McFlyItems.DRAGON_BREATH.isEnabled()) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<EntityAreaEffectCloud> clouds = world.getEntitiesWithinAABB(EntityAreaEffectCloud.class, player.boundingBox.expand(2.0, 2.0, 2.0));
		for (EntityAreaEffectCloud cloud : clouds) {
			if (!cloud.isDead && cloud.byDragon()) {
				cloud.setRadius(cloud.getRadius() - 0.5F);
				world.playSoundAtEntity(player, McFly.MC_ASSETS + ":item.bottle.fill_dragonbreath", 1.0F, 1.0F);
				ItemStack breath = new ItemStack(io.github.asgerjon.mcfly.registry.McFlyItems.DRAGON_BREATH.get());
				if (!player.capabilities.isCreativeMode) {
					stack.stackSize--;
				}
				if (stack.stackSize <= 0) {
					cir.setReturnValue(breath);
				} else {
					if (!player.inventory.addItemStackToInventory(breath)) {
						player.dropPlayerItemWithRandomChoice(breath, false);
					}
					cir.setReturnValue(stack);
				}
				return;
			}
		}
	}
}
