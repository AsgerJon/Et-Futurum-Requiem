package ganymedes01.etfuturum.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.util.StatCollector;

/**
 * The dragon fight's bar on the client (SPEC-modern-movement.md §76.6): while the fight says it's shown and the player
 * is in the End, 1.7.10's boss bar is kept set from it (its bar, and the End's boss music, as when 1.7.10 draws the
 * dragon).
 */
public final class EndBossBar {

	public static final EndBossBar INSTANCE = new EndBossBar();
	private static volatile boolean visible;
	private static volatile float progress;

	private EndBossBar() {
	}

	public static void set(boolean shown, float health) {
		visible = shown;
		progress = health;
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.thePlayer == null || mc.theWorld == null) {
			visible = false;
			return;
		}
		if (visible && mc.thePlayer.dimension == 1) {
			BossStatus.bossName = StatCollector.translateToLocal("entity.EnderDragon.name");
			BossStatus.healthScale = progress;
			BossStatus.statusBarTime = 100;
			BossStatus.hasColorModifier = false;
		}
	}
}
