package ganymedes01.etfuturum.trident;

import ganymedes01.etfuturum.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Impaling's damage (SPEC-modern-movement.md §58.4): 2.5 a level more against the mobs 26.3 makes sensitive to it, its
 * aquatic ones. In 1.7.10 that is the squid; EFR's own aquatic mobs join the list as they come (the guardians, the
 * fish, the dolphin, the turtle, the axolotl, the glow squid, the tadpole, the nautili).
 */
public final class Impaling {

	/** The kinds of mob it works on. */
	private static final List<Class<? extends Entity>> AQUATIC = new ArrayList<>();

	static {
		AQUATIC.add(EntitySquid.class);
	}

	private Impaling() {
	}

	public static void addAquatic(Class<? extends Entity> type) {
		AQUATIC.add(type);
	}

	public static boolean isAquatic(Entity entity) {
		for (Class<? extends Entity> type : AQUATIC) {
			if (type.isInstance(entity)) {
				return true;
			}
		}
		return false;
	}

	/** The extra damage this weapon does to this target: 2.5 a level of Impaling to an aquatic mob, else 0. */
	public static float bonus(ItemStack weapon, Entity target) {
		if (ModEnchantments.impaling == null || weapon == null || !isAquatic(target)) {
			return 0.0F;
		}
		return 2.5F * EnchantmentHelper.getEnchantmentLevel(ModEnchantments.impaling.effectId, weapon);
	}
}
