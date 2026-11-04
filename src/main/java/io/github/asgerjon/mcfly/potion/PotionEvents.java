package io.github.asgerjon.mcfly.potion;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.entities.EntityAreaEffectCloud;
import ganymedes01.etfuturum.offhand.Offhand;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.event.world.ExplosionEvent;

/**
 * Tipped arrows from a bow, and a creeper's lingering cloud (SPEC-modern-movement.md §82). 1.7.10's bow knows only its
 * own arrows: when the first arrows it would take (off-hand first, spec §36) are tipped, they're drawn and shot here.
 */
public final class PotionEvents {

	public static final PotionEvents INSTANCE = new PotionEvents();

	private PotionEvents() {
	}

	/** The first arrows the bow would take, or null: plain arrows first found leave the bow to 1.7.10. */
	private static ItemStack tippedFirst(IInventory inventory) {
		for (int slot : Offhand.slotsOffhandFirst(inventory)) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack == null || stack.stackSize <= 0) {
				continue;
			}
			if (stack.getItem() == Items.arrow) {
				return null;
			}
			if (McFlyItems.TIPPED_ARROW.isEnabled() && stack.getItem() == McFlyItems.TIPPED_ARROW.get()) {
				return stack;
			}
		}
		return null;
	}

	@SubscribeEvent
	public void onNock(ArrowNockEvent event) {
		if (event.result != null && tippedFirst(event.entityPlayer.inventory) != null) {
			event.setCanceled(true);
			event.entityPlayer.setItemInUse(event.result, event.result.getItem().getMaxItemUseDuration(event.result));
		}
	}

	/** The bow's draw, power, punch and flame as 1.7.10's bow shoots an arrow. */
	@SubscribeEvent
	public void onLoose(ArrowLooseEvent event) {
		if (event.bow == null) {
			return;
		}
		ItemStack arrows = tippedFirst(event.entityPlayer.inventory);
		if (arrows == null) {
			return;
		}
		float charge = event.charge / 20.0F;
		charge = (charge * charge + charge * 2.0F) / 3.0F;
		if (charge < 0.1F) {
			return;
		}
		charge = Math.min(charge, 1.0F);
		TippedArrow arrow = new TippedArrow(event.entityPlayer.worldObj, event.entityPlayer, charge * 2.0F);
		arrow.setArrow(arrows);
		arrow.setIsCritical(charge == 1.0F);
		int power = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, event.bow);
		if (power > 0) {
			arrow.setDamage(arrow.getDamage() + power * 0.5D + 0.5D);
		}
		int punch = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, event.bow);
		if (punch > 0) {
			arrow.setKnockbackStrength(punch);
		}
		if (EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, event.bow) > 0) {
			arrow.setFire(100);
		}
		event.bow.damageItem(1, event.entityPlayer);
		event.entityPlayer.worldObj.playSoundAtEntity(event.entityPlayer, "random.bow", 1.0F,
			1.0F / (event.entityPlayer.worldObj.rand.nextFloat() * 0.4F + 1.2F) + charge * 0.5F);
		if (event.entityPlayer.capabilities.isCreativeMode) {
			arrow.canBePickedUp = 2;
		} else {
			arrows.stackSize--;
			if (arrows.stackSize <= 0) {
				for (int slot = 0; slot < event.entityPlayer.inventory.getSizeInventory(); slot++) {
					if (event.entityPlayer.inventory.getStackInSlot(slot) == arrows) {
						event.entityPlayer.inventory.setInventorySlotContents(slot, null);
					}
				}
			}
		}
		if (!event.entityPlayer.worldObj.isRemote) {
			event.entityPlayer.worldObj.spawnEntityInWorld(arrow);
		}
		event.setCanceled(true);
	}

	/**
	 * 26.3's `Creeper.spawnLingeringCloud`: a creeper with effects leaves them in a cloud where it blew up (on 1.7.10's
	 * detonation event: Forge here doesn't fire the explosion's start).
	 */
	@SubscribeEvent
	public void onExplosion(ExplosionEvent.Detonate event) {
		if (!McFlyConfig.enableLingeringPotions || event.world.isRemote || !(event.explosion.exploder instanceof EntityCreeper creeper)) {
			return;
		}
		java.util.Collection<?> effects = creeper.getActivePotionEffects();
		if (effects.isEmpty()) {
			return;
		}
		EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(event.world, creeper.posX, creeper.posY, creeper.posZ);
		cloud.setRadius(2.5F);
		cloud.setRadiusOnUse(-0.5F);
		cloud.setWaitTime(10);
		cloud.setDuration(cloud.getDuration() / 2);
		cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
		for (Object effect : effects) {
			cloud.addEffect(new PotionEffect((PotionEffect) effect));
		}
		event.world.spawnEntityInWorld(cloud);
	}
}
