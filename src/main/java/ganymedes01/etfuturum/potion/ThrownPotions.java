package ganymedes01.etfuturum.potion;

import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;

/**
 * A thrown potion's colour in flight and in its splash (SPEC-modern-movement.md §69.7, stage C). 1.7.10's client
 * knows a thrown potion only by its damage value (the spawn packet carries no more) and draws its splash from the
 * world event 2002's number, also the damage value: a potion kept by name would be water's blue there. So the
 * thrown potion's item rides in its DataWatcher ({@link #WATCHER}, `MixinEntityPotion`), and for a named potion the
 * event's number carries the colour and whether it is instant instead (bit 30 marks it; a damage value is 15 bits).
 */
public final class ThrownPotions {

	/** EntityPotion's DataWatcher slot for its item (1.7.10's entities use 0 and 1 there; a thrown potion no more). */
	public static final int WATCHER = 20;

	private static final int ENCODED = 1 << 30;
	private static final int INSTANT = 1 << 29;

	private ThrownPotions() {
	}

	/** The colour a thrown potion is drawn in, on the client: its item's when that came, else its damage value's. */
	public static int colourOf(EntityPotion potion) {
		ItemStack stack = NamedPotions.isReady() ? potion.getDataWatcher().getWatchableObjectItemStack(WATCHER) : null;
		if (stack != null) {
			PotionContents contents = PotionContents.of(stack);
			if (contents.isNamed()) {
				return contents.color();
			}
		}
		return PotionHelper.func_77915_a(potion.getPotionDamage(), false);
	}

	/** The number the splash's world event (2002) carries: the damage value, or the encoded colour for a named potion. */
	public static int splashData(ItemStack potion) {
		if (potion == null) {
			return 0;
		}
		PotionContents contents = NamedPotions.isReady() ? PotionContents.of(potion) : PotionContents.EMPTY;
		if (!contents.isNamed()) {
			return potion.getItemDamage();
		}
		boolean instant = false;
		for (PotionEffect effect : contents.allEffects()) {
			Potion type = Potion.potionTypes[effect.getPotionID()];
			instant |= type != null && type.isInstant();
		}
		return ENCODED | (instant ? INSTANT : 0) | contents.color() & 0xFFFFFF;
	}

	public static boolean isEncoded(int data) {
		return (data & ENCODED) != 0;
	}

	/** The splash's particle colour from the event's number. */
	public static int splashColour(int data) {
		return isEncoded(data) ? data & 0xFFFFFF : PotionHelper.func_77915_a(data, false);
	}

	/** Whether the splash's particles are the instant ones. */
	public static boolean splashInstant(int data) {
		return isEncoded(data) ? (data & INSTANT) != 0 : Items.potionitem.isEffectInstant(data);
	}
}
