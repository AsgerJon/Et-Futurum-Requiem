package io.github.asgerjon.mcfly.potion;

import com.google.common.collect.HashMultimap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import java.util.List;
import java.util.Map;

/**
 * A potion's tooltip lines for our potion items (SPEC-modern-movement.md §82): each effect (bad ones red) with its
 * potency and duration, then what its attributes do. 1.7.10's potion item reads only its own item's effects, so a
 * lingering potion or a tipped arrow lists its own this way.
 */
public final class PotionTooltip {

	private PotionTooltip() {
	}

	public static void add(List<PotionEffect> effects, List<String> lines) {
		HashMultimap<String, AttributeModifier> attributes = HashMultimap.create();
		if (effects == null || effects.isEmpty()) {
			lines.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("potion.empty").trim());
		} else {
			for (PotionEffect effect : effects) {
				Potion potion = Potion.potionTypes[effect.getPotionID()];
				String line = StatCollector.translateToLocal(effect.getEffectName()).trim();
				Map<IAttribute, AttributeModifier> modifiers = potion.func_111186_k();
				if (modifiers != null) {
					for (Map.Entry<IAttribute, AttributeModifier> entry : modifiers.entrySet()) {
						AttributeModifier modifier = entry.getValue();
						attributes.put(entry.getKey().getAttributeUnlocalizedName(),
							new AttributeModifier(modifier.getName(), potion.func_111183_a(effect.getAmplifier(), modifier), modifier.getOperation()));
					}
				}
				if (effect.getAmplifier() > 0) {
					line += " " + StatCollector.translateToLocal("potion.potency." + effect.getAmplifier()).trim();
				}
				if (effect.getDuration() > 20) {
					line += " (" + Potion.getDurationString(effect) + ")";
				}
				lines.add((potion.isBadEffect() ? EnumChatFormatting.RED : EnumChatFormatting.GRAY) + line);
			}
		}
		if (attributes.isEmpty()) {
			return;
		}
		lines.add("");
		lines.add(EnumChatFormatting.DARK_PURPLE + StatCollector.translateToLocal("potion.effects.whenDrank"));
		for (Map.Entry<String, AttributeModifier> entry : attributes.entries()) {
			AttributeModifier modifier = entry.getValue();
			double amount = modifier.getAmount();
			double shown = modifier.getOperation() == 1 || modifier.getOperation() == 2 ? amount * 100.0 : amount;
			String name = StatCollector.translateToLocal("attribute.name." + entry.getKey());
			if (amount > 0.0) {
				lines.add(EnumChatFormatting.BLUE + StatCollector.translateToLocalFormatted("attribute.modifier.plus." + modifier.getOperation(),
					ItemStack.field_111284_a.format(shown), name));
			} else if (amount < 0.0) {
				lines.add(EnumChatFormatting.RED + StatCollector.translateToLocalFormatted("attribute.modifier.take." + modifier.getOperation(),
					ItemStack.field_111284_a.format(-shown), name));
			}
		}
	}
}
