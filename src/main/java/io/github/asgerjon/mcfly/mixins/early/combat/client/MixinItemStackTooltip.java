package io.github.asgerjon.mcfly.mixins.early.combat.client;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.combat.DamageRules;
import ganymedes01.etfuturum.combat.WeaponStats;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * The attribute lines of a tooltip as modern writes them (spec §46.5): "When in Main Hand:", and the weapon's own
 * damage and speed as totals in dark green. Other modifiers keep 1.7.10's +/- lines. 1.7.10's section is skipped.
 * Armour gets modern's block too (§46.10).
 */
@Mixin(ItemStack.class)
public class MixinItemStackTooltip {

	@Unique
	private static final DecimalFormat etfu$TOTAL = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
	@Unique
	private static final DecimalFormat etfu$AMOUNT = new DecimalFormat("#.###");
	/** Armour's slots by 1.7.10's armour type, as modern's tooltip keys name them. */
	@Unique
	private static final String[] etfu$SLOTS = {"head", "chest", "legs", "feet"};

	@WrapOperation(method = "getTooltip", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Multimap;isEmpty()Z", remap = false))
	private boolean etfu$modernAttributeLines(Multimap<String, AttributeModifier> modifiers, Operation<Boolean> original,
											  @Local ArrayList<String> lines, @Local(argsOnly = true) EntityPlayer player) {
		if (original.call(modifiers)) {
			if (((ItemStack) (Object) this).getItem() instanceof ItemArmor) {
				etfu$armourLines((ItemStack) (Object) this, lines);
			}
			return true;
		}
		lines.add("");
		lines.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("item.modifiers.mainhand"));
		for (Map.Entry<String, AttributeModifier> entry : modifiers.entries()) {
			AttributeModifier modifier = entry.getValue();
			String name = StatCollector.translateToLocal("attribute.name." + entry.getKey());
			int operation = modifier.getOperation();
			double amount = modifier.getAmount();
			IAttributeInstance attribute = player == null ? null : player.getAttributeMap().getAttributeInstanceByName(entry.getKey());
			if (WeaponStats.isWeaponModifier(modifier) && attribute != null) {
				amount += attribute.getBaseValue();
				double shown = operation == 0 ? amount : amount * 100.0D;
				lines.add(EnumChatFormatting.DARK_GREEN + " " + StatCollector.translateToLocalFormatted("attribute.modifier.equals." + operation, etfu$TOTAL.format(shown), name));
			} else if (amount != 0.0D) {
				double shown = Math.abs(operation == 0 ? amount : amount * 100.0D);
				String key = amount > 0.0D ? "attribute.modifier.plus." : "attribute.modifier.take.";
				lines.add((amount > 0.0D ? EnumChatFormatting.BLUE : EnumChatFormatting.RED) + StatCollector.translateToLocalFormatted(key + operation, etfu$AMOUNT.format(shown), name));
			}
		}
		return true;
	}

	/**
	 * Armour's block, as modern's (spec §46.10): "When on Head:" (Body, Legs, Feet), then its points, toughness and
	 * knockback resistance (shown × 10), blue. 1.7.10 showed none.
	 */
	@Unique
	private static void etfu$armourLines(ItemStack stack, ArrayList<String> lines) {
		ItemArmor armour = (ItemArmor) stack.getItem();
		lines.add("");
		lines.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("item.modifiers." + etfu$SLOTS[armour.armorType]));
		etfu$plus(lines, armour.damageReduceAmount, "generic.armor");
		etfu$plus(lines, Math.round(DamageRules.toughness(stack)), "generic.armorToughness");
		etfu$plus(lines, (int) Math.round(DamageRules.knockbackResistance(stack) * 10.0D), "generic.knockbackResistance");
	}

	/** A blue "+n Name" line (1.7.10's key takes a whole number), if n isn't 0. */
	@Unique
	private static void etfu$plus(ArrayList<String> lines, int amount, String attribute) {
		if (amount != 0) {
			lines.add(EnumChatFormatting.BLUE + StatCollector.translateToLocalFormatted("attribute.modifier.plus.0", amount,
				StatCollector.translateToLocal("attribute.name." + attribute)));
		}
	}
}
