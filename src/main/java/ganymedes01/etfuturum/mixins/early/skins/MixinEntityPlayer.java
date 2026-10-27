package ganymedes01.etfuturum.mixins.early.skins;

import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.skins.SkinParts;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The synced byte of shown skin parts (SkinParts), all shown until the player's settings arrive. */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase {

	public MixinEntityPlayer(World world) {
		super(world);
	}

	@Inject(method = "entityInit", at = @At("TAIL"))
	private void etfu$registerSkinParts(CallbackInfo ci) {
		this.dataWatcher.addObject(ConfigFunctions.skinPartsDataWatcherId, (byte) SkinParts.ALL);
	}
}
