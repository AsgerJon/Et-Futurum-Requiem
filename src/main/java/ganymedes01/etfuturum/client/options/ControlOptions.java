package ganymedes01.etfuturum.client.options;

import ganymedes01.etfuturum.client.chat.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.server.integrated.IntegratedServer;

/**
 * The rest of 26.3's Controls and Mouse Settings (SPEC-modern-movement.md §54.12): Auto-Jump, Sprint Window, Operator
 * Items Tab, Invert Mouse X, Scroll Sensitivity and Discrete Scrolling, saved in options.txt under 26.3's names
 * (OptionsTxt). Auto-Jump is modernoptions.client.MixinEntityPlayerSPControls, the Sprint Window that mixin and
 * modernmovement's sprint start, the tab CreativeTabOperator with MixinGuiContainerCreative hiding it.
 */
public final class ControlOptions {

	public static boolean autoJump;
	/** Ticks in which a second press of forward starts sprinting; 0 is off. */
	public static int sprintWindow = 7;
	public static boolean operatorItemsTab;
	public static boolean invertMouseX;
	/** 0.01 to 10. */
	public static double scrollSensitivity = 1.0D;
	public static boolean discreteScrolling;

	/** The hotbar's wheel, and one for each kind of screen that reads the wheel (each reads every event itself). */
	private static final Wheel HOTBAR = new Wheel(), SCREENS = new Wheel();
	public static final Wheel LISTS = new Wheel(), CHAT = new Wheel(), CREATIVE = new Wheel(), ACHIEVEMENTS = new Wheel();

	private ControlOptions() {
	}

	/**
	 * Whether the creative screen shows the Operator Utilities tab: the option on, and the player may use game-master
	 * blocks (26.3's rule). 1.7.10's client doesn't know its permission level: the command list the server sent holds
	 * /setblock (level 2) only for a player who may use it; without that list, single player with cheats on.
	 */
	public static boolean operatorTabShown() {
		Minecraft mc = Minecraft.getMinecraft();
		if (!operatorItemsTab || mc.playerController == null || !mc.playerController.isInCreativeMode()) {
			return false;
		}
		if (ClientCommands.active()) {
			return ClientCommands.dispatcher().getRoot().getChild("setblock") != null;
		}
		IntegratedServer server = mc.getIntegratedServer();
		return server != null && server.worldServers.length > 0 && server.worldServers[0].getWorldInfo().areCommandsAllowed();
	}

	/** The mouse's left-right turn as it reaches the player. */
	public static float turnYaw(float yaw) {
		return invertMouseX ? -yaw : yaw;
	}

	/** One wheel event over the game (1.7.10's units: 120 a notch): one slot each time a whole notch has gathered. */
	public static void scrollHotbar(InventoryPlayer inventory, int wheel) {
		int notches = HOTBAR.notches(wheel);
		if (notches != 0) {
			inventory.changeCurrentItem(notches);
		}
	}

	/** One wheel event as a screen reads it (1.7.10's units: 120 a notch). */
	public static int screenWheel(int wheel) {
		return SCREENS.screen(wheel);
	}

	/**
	 * 26.3's ScrollWheelHandler for one reader: an event's amount in notches is its sign (Discrete Scrolling) or its
	 * size, times the sensitivity; the amounts gather, a turn the other way drops what had gathered, and the whole
	 * notches go out, the rest kept.
	 */
	public static final class Wheel {
		private double gathered;

		/** The whole notches this event (1.7.10's units, 120 a notch) completes. */
		public int notches(int wheel) {
			if (wheel == 0) {
				return 0;
			}
			double amount = (discreteScrolling ? Math.signum(wheel) : wheel / 120.0D) * scrollSensitivity;
			if (gathered != 0.0D && Math.signum(amount) != Math.signum(gathered)) {
				gathered = 0.0D;
			}
			gathered += amount;
			int whole = (int) gathered;
			gathered -= whole;
			return whole;
		}

		/** The event as a screen should see it: the whole notches in 1.7.10's units. */
		public int screen(int wheel) {
			return notches(wheel) * 120;
		}
	}
}
