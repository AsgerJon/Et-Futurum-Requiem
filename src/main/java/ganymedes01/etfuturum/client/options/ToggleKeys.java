package ganymedes01.etfuturum.client.options;

import io.github.asgerjon.mcfly.mixins.early.modernoptions.client.AccessorKeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * 26.3's Hold/Toggle setting of four keys (SPEC-modern-movement.md §54.11): Sneak, Sprint, Attack/Destroy and Use
 * Item/Place Block; true is Toggle. Saved in options.txt as toggleCrouch, toggleSprint, toggleAttack and toggleUse.
 * In Toggle a key-down flips the key and a key-up does nothing (modernoptions.client.MixinKeyBinding); a screen
 * opening lets go of the four and marks those that were down in Toggle, the screen closing back to the game puts the
 * marked ones down again (MixinMinecraft), and a respawn lets go of them but keeps the marks (MixinEntityClientPlayerMP).
 */
public final class ToggleKeys {

	public static boolean crouch;
	public static boolean sprint;
	public static boolean attack;
	public static boolean use;

	/** 26.3's "released by a screen when down": toggle keys a screen let go of, kept until a screen closes. */
	private static final Set<KeyBinding> LET_GO_BY_A_SCREEN = Collections.newSetFromMap(new IdentityHashMap<>());

	private ToggleKeys() {
	}

	/** The setting of key 0 (Sneak), 1 (Sprint), 2 (Attack/Destroy) or 3 (Use Item/Place Block). */
	public static boolean get(int key) {
		return switch (key) {
			case 0 -> crouch;
			case 1 -> sprint;
			case 2 -> attack;
			default -> use;
		};
	}

	public static void set(int key, boolean toggle) {
		switch (key) {
			case 0 -> crouch = toggle;
			case 1 -> sprint = toggle;
			case 2 -> attack = toggle;
			default -> use = toggle;
		}
	}

	/** The four bindings, in the order of get and set; null before the game's settings exist. */
	private static KeyBinding[] keys() {
		GameSettings settings = Minecraft.getMinecraft().gameSettings;
		return settings == null ? null : new KeyBinding[] {settings.keyBindSneak, settings.keyBindSprint, settings.keyBindAttack, settings.keyBindUseItem};
	}

	/** Whether the binding is one of the four and set to Toggle. */
	public static boolean inToggle(KeyBinding binding) {
		KeyBinding[] keys = keys();
		if (keys != null) {
			for (int i = 0; i < keys.length; i++) {
				if (keys[i] == binding) {
					return get(i);
				}
			}
		}
		return false;
	}

	/** A key-down on a binding in Toggle: it goes down if it was up, up if it was down. */
	public static void flip(KeyBinding binding) {
		setDown(binding, !binding.getIsKeyPressed());
	}

	/** A screen opens: each of the four is let go, and marked when it was down in Toggle (a mark stays until a close). */
	public static void screenOpened() {
		KeyBinding[] keys = keys();
		if (keys == null) {
			return;
		}
		for (int i = 0; i < keys.length; i++) {
			if (get(i) && keys[i].getIsKeyPressed()) {
				LET_GO_BY_A_SCREEN.add(keys[i]);
			}
			setDown(keys[i], false);
		}
	}

	/**
	 * The screen closes back to the game: every mark is read and cleared, and a marked key goes down again when it is
	 * in Toggle, bound to the keyboard, and not Use Item, which 26.3 never puts back.
	 */
	public static void screenClosed() {
		KeyBinding[] keys = keys();
		if (keys == null) {
			return;
		}
		for (int i = 0; i < keys.length; i++) {
			boolean marked = LET_GO_BY_A_SCREEN.remove(keys[i]);
			if (marked && i != 3 && get(i) && keys[i].getKeyCode() > 0) {
				setDown(keys[i], true);
			}
		}
	}

	/** A respawn: the four go up, in Hold or Toggle; the marks stay, as 26.3 keeps them. */
	public static void respawned() {
		KeyBinding[] keys = keys();
		if (keys != null) {
			for (KeyBinding key : keys) {
				setDown(key, false);
			}
		}
	}

	private static void setDown(KeyBinding binding, boolean down) {
		((AccessorKeyBinding) binding).etfu$setPressed(down);
	}
}
