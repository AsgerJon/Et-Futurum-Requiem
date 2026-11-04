package ganymedes01.etfuturum.client;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.particle.McFlyParticlesClient;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * What a client shows when a totem saves an entity it sees (SPEC-modern-movement.md §55.2, modern's entity event 35): the
 * sound at the entity, the particles around it and, for its own player, the totem flying at the screen.
 */
public final class TotemEffects {

	private TotemEffects() {
	}

	public static void play(Entity entity) {
		McFlyParticlesClient.totemEmitter(entity);
		entity.worldObj.playSound(entity.posX, entity.boundingBox.minY, entity.posZ, McFly.MC_ASSETS + ":item.totem.use", 1.0F, 1.0F, false);
		Minecraft mc = Minecraft.getMinecraft();
		if (entity == mc.thePlayer) {
			TotemActivation.activate(heldTotem(mc.thePlayer));
		}
	}

	/** The totem in either hand (this client may not have seen it used up yet), or a new one. */
	private static ItemStack heldTotem(EntityPlayer player) {
		ItemStack main = player.getHeldItem();
		if (main != null && main.getItem() == ModItems.TOTEM_OF_UNDYING.get()) {
			return main;
		}
		ItemStack offhand = Offhand.isEnabled() ? Offhand.getOffhand(player) : null;
		if (offhand != null && offhand.getItem() == ModItems.TOTEM_OF_UNDYING.get()) {
			return offhand;
		}
		return new ItemStack(ModItems.TOTEM_OF_UNDYING.get());
	}
}
