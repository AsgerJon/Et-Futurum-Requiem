package io.github.asgerjon.mcfly.elytra.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.elytra.FallFlying;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

/**
 * 26.3's `ElytraOnPlayerSoundInstance` (SPEC-modern-movement.md §21.5, §85): `item.elytra.flying`, looping, following
 * the gliding player; silent for its first 20 ticks, then fading in over 20; its volume the speed's square over 4 (up
 * to 1), its pitch rising above 0.8 of that; it ends 20 ticks in once the glide is over, or with the player.
 */
@SideOnly(Side.CLIENT)
public class ElytraFlightSound extends MovingSound {

	private final EntityPlayerSP player;
	private int time;

	public ElytraFlightSound(EntityPlayerSP player) {
		super(new ResourceLocation(McFly.MC_ASSETS, "item.elytra.flying"));
		this.player = player;
		this.repeat = true;
		this.field_147665_h = 0;
		this.volume = 0.1F;
	}

	@Override
	public void update() {
		time++;
		if (!player.isDead && (time <= 20 || FallFlying.isFallFlying(player))) {
			this.xPosF = (float) player.posX;
			this.yPosF = (float) player.posY;
			this.zPosF = (float) player.posZ;
			float speed = (float) (player.motionX * player.motionX + player.motionY * player.motionY + player.motionZ * player.motionZ);
			this.volume = speed >= 1.0E-7F ? MathHelper.clamp_float(speed / 4.0F, 0.0F, 1.0F) : 0.0F;
			if (time < 20) {
				this.volume = 0.0F;
			} else if (time < 40) {
				this.volume *= (time - 20) / 20.0F;
			}
			this.field_147663_c = this.volume > 0.8F ? 1.0F + (this.volume - 0.8F) : 1.0F;
		} else {
			this.donePlaying = true;
		}
	}
}
