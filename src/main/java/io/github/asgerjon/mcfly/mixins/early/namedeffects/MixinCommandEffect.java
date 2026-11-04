package io.github.asgerjon.mcfly.mixins.early.namedeffects;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.potion.EffectRegistry;
import net.minecraft.command.CommandEffect;
import net.minecraft.command.ICommandSender;
import net.minecraft.potion.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.7.10's `/effect` takes an effect's name as well as its number (spec §70.3, stage C). */
@Mixin(CommandEffect.class)
public abstract class MixinCommandEffect {

	@WrapOperation(method = "processCommand", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/command/CommandEffect;parseIntWithMin(Lnet/minecraft/command/ICommandSender;Ljava/lang/String;I)I", ordinal = 0))
	private int etfu$effectByName(ICommandSender sender, String text, int min, Operation<Integer> original) {
		Potion potion = text.matches("-?\\d+") ? null : EffectRegistry.get(text);
		return potion != null ? potion.id : original.call(sender, text, min);
	}
}
