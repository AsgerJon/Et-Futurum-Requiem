package io.github.asgerjon.mcfly.elytra;

import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;

/**
 * The elytra's world side (SPEC-modern-movement.md §18.4, §20, §85): 26.3's `FireworkRocketItem.use` while gliding (a
 * rocket attached to the glider, one used up but in creative, the use counted), and the `disableElytraMovementCheck`
 * game rule (added only when a world hasn't it yet, so a saved value stays).
 */
public final class ElytraEvents {

	public static final ElytraEvents INSTANCE = new ElytraEvents();

	private ElytraEvents() {
	}

	@SubscribeEvent
	public void fireworkWhileGliding(PlayerInteractEvent event) {
		if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
			return;
		}
		EntityPlayer player = event.entityPlayer;
		ItemStack held = player.getHeldItem();
		if (held == null || held.getItem() != Items.fireworks || !FallFlying.isFallFlying(player)) {
			return;
		}
		player.swingItem();
		if (!event.world.isRemote) {
			event.world.spawnEntityInWorld(new BoostingRocket(event.world, held, player));
			if (!player.capabilities.isCreativeMode) {
				held.stackSize--;
			}
			player.addStat(StatList.objectUseStats[Item.getIdFromItem(Items.fireworks)], 1);
			event.useItem = Result.ALLOW;
		}
	}

	@SubscribeEvent
	public void movementCheckRule(WorldEvent.Load event) {
		if (!event.world.getGameRules().hasRule("disableElytraMovementCheck")) {
			event.world.getGameRules().addGameRule("disableElytraMovementCheck", "false");
		}
	}
}
