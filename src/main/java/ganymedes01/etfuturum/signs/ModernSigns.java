package ganymedes01.etfuturum.signs;

import io.github.asgerjon.mcfly.registry.McFlyItems;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.client.particle.CustomParticles;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashMap;
import java.util.Map;

/**
 * Signs that take dyes, glow ink, ink sacs and honeycomb (SPEC-modern-movement.md §67.2; 26.3's `SignBlock.useItemOn`,
 * the `SignApplicator`s, `DyeColor`, `AbstractSignRenderer`). Used on a sign with text, not waxed, by a player who may
 * build: a dye sets the colour, a glow ink sac the glow, an ink sac takes the glow off, honeycomb waxes it. Only what
 * changes something is used up (not in creative) and makes its sound.
 */
public final class ModernSigns {

	public static final String BLACK = "black";

	/** 26.3's `DyeColor.textColor`, by the dye's name. */
	private static final Map<String, Integer> TEXT_COLORS = new HashMap<>();
	/** 1.7.10's `dye` damage values, by colour (0 is the ink sac, black). */
	private static final String[] VANILLA_DYES = {BLACK, "red", "green", "brown", "blue", "purple", "cyan", "light_gray",
		"gray", "pink", "lime", "yellow", "light_blue", "magenta", "orange", "white"};
	/** EFR's dyes (`ModItems.DYE`), by damage. */
	private static final String[] EFR_DYES = {"white", "blue", "brown", BLACK};

	static {
		String[] names = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
			"purple", "blue", "brown", "green", "red", BLACK};
		int[] colors = {0xFFFFFF, 0xFF681F, 0xFF00FF, 0x9AC0CD, 0xFFFF00, 0xBFFF00, 0xFF69B4, 0x808080, 0xD3D3D3, 0x00FFFF,
			0xA020F0, 0x0000FF, 0x8B4513, 0x00FF00, 0xFF0000, 0x000000};
		for (int i = 0; i < names.length; i++) {
			TEXT_COLORS.put(names[i], colors[i]);
		}
	}

	private ModernSigns() {
	}

	// ---------------------------------------------------------------- using an item on a sign

	/**
	 * The player uses what they hold on the sign: whether it took it (the block's use then counts as done; otherwise the
	 * item's own use goes on, as 26.3's `TRY_WITH_EMPTY_HAND`). On the client, whether it would (so the arm swings).
	 */
	public static boolean use(World world, int x, int y, int z, EntityPlayer player) {
		ItemStack held = player.getCurrentEquippedItem();
		TileEntity tile = world.getTileEntity(x, y, z);
		if (held == null || !(tile instanceof TileEntitySign sign) || !(tile instanceof ISignText text)) {
			return false;
		}
		Kind kind = kindOf(held);
		if (kind == null || !player.capabilities.allowEdit || text.etfu$isWaxed() || !hasText(sign)) {
			return false;
		}
		if (world.isRemote) {
			if (kind == Kind.WAX && !text.etfu$isWaxed()) {
				waxParticles(world, x, y, z);
			}
			return true;
		}
		String sound;
		switch (kind) {
			case GLOW:
				if (text.etfu$isGlowing()) {
					return false;
				}
				text.etfu$setGlowing(true);
				sound = "item.glow_ink_sac.use";
				break;
			case INK:
				if (!text.etfu$isGlowing()) {
					return false;
				}
				text.etfu$setGlowing(false);
				sound = "item.ink_sac.use";
				break;
			case WAX:
				text.etfu$setWaxed(true);
				sound = "item.honeycomb.wax_on";
				break;
			default:
				String color = dyeColor(held);
				if (color.equals(text.etfu$getColor())) {
					return false;
				}
				text.etfu$setColor(color);
				sound = "item.dye.use";
		}
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Reference.MCAssetVer + ":" + sound, 1.0F, 1.0F);
		if (!player.capabilities.isCreativeMode && --held.stackSize <= 0) {
			player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
		}
		sign.markDirty();
		world.markBlockForUpdate(x, y, z);
		return true;
	}

	private enum Kind {
		DYE, GLOW, INK, WAX
	}

	private static Kind kindOf(ItemStack stack) {
		if (McFlyItems.GLOW_INK_SAC.isEnabled() && stack.getItem() == McFlyItems.GLOW_INK_SAC.get()) {
			return Kind.GLOW;
		}
		if (ModItems.HONEYCOMB.isEnabled() && stack.getItem() == ModItems.HONEYCOMB.get()) {
			return Kind.WAX;
		}
		if (stack.getItem() == Items.dye && stack.getItemDamage() == 0) {
			return Kind.INK;
		}
		return dyeColor(stack) != null ? Kind.DYE : null;
	}

	/**
	 * The dye's colour, or null. 1.7.10's dyes by damage (but the ink sac, 0); with EFR's new dyes, bone meal, lapis and
	 * cocoa beans aren't dyes either (26.3's aren't), EFR's white, blue, brown and black are; other mods' by ore name.
	 */
	public static String dyeColor(ItemStack stack) {
		if (stack.getItem() == Items.dye) {
			int damage = stack.getItemDamage();
			if (damage <= 0 || damage >= VANILLA_DYES.length || ModItems.DYE.isEnabled() && (damage == 3 || damage == 4 || damage == 15)) {
				return null;
			}
			return VANILLA_DYES[damage];
		}
		if (ModItems.DYE.isEnabled() && stack.getItem() == ModItems.DYE.get()) {
			int damage = stack.getItemDamage();
			return damage >= 0 && damage < EFR_DYES.length ? EFR_DYES[damage] : null;
		}
		for (int id : OreDictionary.getOreIDs(stack)) {
			String ore = OreDictionary.getOreName(id);
			if (ore.startsWith("dye") && ore.length() > 3) {
				String color = ore.substring(3).replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
				if (TEXT_COLORS.containsKey(color)) {
					return color;
				}
			}
		}
		return null;
	}

	/** 26.3's `canApplyToSign`: some line has text. */
	private static boolean hasText(TileEntitySign sign) {
		for (String line : sign.signText) {
			if (line != null && !line.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** 26.3's level event 3003 for a sign (EFR's copper wax particles, on the clicking client). */
	private static void waxParticles(World world, int x, int y, int z) {
		for (int i = 0; i < 10; i++) {
			CustomParticles.spawnCopperWaxOnParticle(world, x + world.rand.nextFloat(), y + world.rand.nextFloat(), z + world.rand.nextFloat());
		}
	}

	// ---------------------------------------------------------------- the colours drawn

	private static int textColorOf(String color) {
		Integer value = TEXT_COLORS.get(color);
		return value == null ? 0 : value;
	}

	/** 26.3's `ARGB.scaleRGB(color, 0.4F)`, each channel cut down. */
	private static int dark(int color) {
		return (int) ((color >> 16 & 255) * 0.4F) << 16 | (int) ((color >> 8 & 255) * 0.4F) << 8 | (int) ((color & 255) * 0.4F);
	}

	/** The colour the text is drawn in: plain, the dye's x 0.4; glowing, the dye's own. */
	public static int textColor(String color, boolean glowing) {
		int text = textColorOf(color);
		return glowing ? text : dark(text);
	}

	/** The outline's colour, or -1 for none: glowing text only, the dye's x 0.4; glowing black's 0xF0EBCC. */
	public static int outlineColor(String color, boolean glowing) {
		if (!glowing) {
			return -1;
		}
		return BLACK.equals(color) ? 0xF0EBCC : dark(textColorOf(color));
	}

	/** Whether the outline is drawn however far the camera is (glowing black text; the rest within 16 blocks). */
	public static boolean outlineAlways(String color, boolean glowing) {
		return glowing && BLACK.equals(color);
	}
}
