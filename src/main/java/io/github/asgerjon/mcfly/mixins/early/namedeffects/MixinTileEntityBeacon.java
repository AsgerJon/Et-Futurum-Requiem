package io.github.asgerjon.mcfly.mixins.early.namedeffects;

import ganymedes01.etfuturum.potion.EffectRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntityBeacon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A beacon's effects saved by name (SPEC-modern-movement.md §70.3, stage B), as 26.3's `primary_effect` and
 * `secondary_effect`, with no number; an old beacon's `Primary` and `Secondary` numbers still read.
 */
@Mixin(TileEntityBeacon.class)
public abstract class MixinTileEntityBeacon {

	@Shadow
	private int primaryEffect;

	@Shadow
	private int secondaryEffect;

	@Inject(method = "writeToNBT", at = @At("RETURN"))
	private void etfu$writeNames(NBTTagCompound tag, CallbackInfo ci) {
		tag.removeTag("Primary");
		tag.removeTag("Secondary");
		etfu$put(tag, "primary_effect", primaryEffect);
		etfu$put(tag, "secondary_effect", secondaryEffect);
	}

	@Inject(method = "readFromNBT", at = @At("RETURN"))
	private void etfu$readNames(NBTTagCompound tag, CallbackInfo ci) {
		if (tag.hasKey("primary_effect", 8)) {
			primaryEffect = etfu$number(tag.getString("primary_effect"));
		}
		if (tag.hasKey("secondary_effect", 8)) {
			secondaryEffect = etfu$number(tag.getString("secondary_effect"));
		}
	}

	@Unique
	private static void etfu$put(NBTTagCompound tag, String key, int number) {
		Potion potion = number > 0 && number < Potion.potionTypes.length ? Potion.potionTypes[number] : null;
		String name = EffectRegistry.key(potion);
		if (name != null) {
			tag.setString(key, name);
		}
	}

	@Unique
	private static int etfu$number(String name) {
		Potion potion = EffectRegistry.get(name);
		return potion == null ? 0 : potion.id;
	}
}
