package ganymedes01.etfuturum.potion;

import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import net.minecraft.potion.Potion;

/**
 * The potion liquid colours of 26.3 (changed in 1.19.4) for the effects 1.7.10 has, plus this mod's levitation.
 * Everything that colours a potion reads {@link Potion#getLiquidColor()}: the bottle's liquid, thrown potions,
 * the particles on an entity, the lingering cloud and tipped arrows. The water bottle keeps its blue, which is the
 * same in both versions. Applied in postInit so it runs after the rabbit-foot leaping colour set in ModRecipes.
 */
public final class ModernPotionColors {

	private ModernPotionColors() {
	}

	public static void apply() {
		if (!ConfigEnchantsPotions.modernPotionColors) {
			return;
		}
		Potion.moveSpeed.liquidColor = 0x33EBFF;
		Potion.moveSlowdown.liquidColor = 0x8BAFE0;
		Potion.digSpeed.liquidColor = 0xD9C043;
		Potion.digSlowdown.liquidColor = 0x4A4217;
		Potion.damageBoost.liquidColor = 0xFFC700;
		Potion.heal.liquidColor = 0xF82423;
		Potion.harm.liquidColor = 0xA9656A;
		Potion.jump.liquidColor = 0xFDFF84;
		Potion.confusion.liquidColor = 0x551D4A;
		Potion.regeneration.liquidColor = 0xCD5CAB;
		Potion.resistance.liquidColor = 0x9146F0;
		Potion.fireResistance.liquidColor = 0xFF9900;
		Potion.waterBreathing.liquidColor = 0x98DAC0;
		Potion.invisibility.liquidColor = 0xF6F6F6;
		Potion.blindness.liquidColor = 0x1F1F23;
		Potion.nightVision.liquidColor = 0xC2FF66;
		Potion.hunger.liquidColor = 0x587653;
		Potion.weakness.liquidColor = 0x484D48;
		Potion.poison.liquidColor = 0x87A363;
		Potion.wither.liquidColor = 0x736156;
		Potion.field_76434_w.liquidColor = 0xF87D23; // health boost
		Potion.field_76444_x.liquidColor = 0x2552A5; // absorption
		Potion.field_76443_y.liquidColor = 0xF82423; // saturation
		if (ModPotions.levitation != null) {
			ModPotions.levitation.liquidColor = 0xCEFFFF;
		}
	}
}
