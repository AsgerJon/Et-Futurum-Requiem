package ganymedes01.etfuturum.combat;

import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A player's item cooldowns (spec §46.9), as modern's: an item can't be used until its cooldown runs out. The server
 * starts them and tells the player's client; both count them down.
 */
public final class ItemCooldowns {

	private final Map<Item, int[]> cooldowns = new HashMap<>();
	private int ticks;

	public void tick() {
		ticks++;
		Iterator<int[]> entries = cooldowns.values().iterator();
		while (entries.hasNext()) {
			if (entries.next()[1] <= ticks) {
				entries.remove();
			}
		}
	}

	public void add(Item item, int duration) {
		cooldowns.put(item, new int[]{ticks, ticks + duration});
	}

	public boolean isCoolingDown(Item item) {
		return remaining(item, 0.0F) > 0.0F;
	}

	/** The share of the cooldown left, 1 at the start down to 0, {@code partialTick} ahead. */
	public float remaining(Item item, float partialTick) {
		int[] cooldown = cooldowns.get(item);
		if (cooldown == null) {
			return 0.0F;
		}
		float total = cooldown[1] - cooldown[0];
		float left = cooldown[1] - (ticks + partialTick);
		return Math.max(0.0F, Math.min(1.0F, left / total));
	}
}
