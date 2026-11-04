package io.github.asgerjon.mcfly.mixins.early.bedspawn;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.asgerjon.mcfly.network.OverlayMessage;
import net.minecraft.block.BlockBed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A bed's problems show above the hotbar in modern's words (SPEC-modern-movement.md §53.3): out of reach (1.7.10 said
 * nothing), only at night, monsters near, occupied (1.7.10 put those three in the chat).
 */
@Mixin(BlockBed.class)
public class MixinBlockBed {

	@ModifyExpressionValue(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;sleepInBedAt(III)Lnet/minecraft/entity/player/EntityPlayer$EnumStatus;"))
	private EntityPlayer.EnumStatus etfu$tooFarAway(EntityPlayer.EnumStatus status, @Local(argsOnly = true) EntityPlayer player) {
		if (status == EntityPlayer.EnumStatus.TOO_FAR_AWAY) {
			OverlayMessage.send(player, new ChatComponentTranslation("block.minecraft.bed.too_far_away"));
		}
		return status;
	}

	@WrapOperation(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;addChatComponentMessage(Lnet/minecraft/util/IChatComponent;)V"))
	private void etfu$aboveTheHotbar(EntityPlayer player, IChatComponent message, Operation<Void> original) {
		String modern = message instanceof ChatComponentTranslation translation ? etfu$modernKey(translation.getKey()) : null;
		if (modern == null) {
			original.call(player, message);
			return;
		}
		OverlayMessage.send(player, new ChatComponentTranslation(modern));
	}

	/** 1.7.10's bed message keys and modern's for the same message. */
	@Unique
	private static String etfu$modernKey(String key) {
		switch (key) {
			case "tile.bed.noSleep":
				return "block.minecraft.bed.no_sleep";
			case "tile.bed.notSafe":
				return "block.minecraft.bed.not_safe";
			case "tile.bed.occupied":
				return "block.minecraft.bed.occupied";
			default:
				return null;
		}
	}
}
