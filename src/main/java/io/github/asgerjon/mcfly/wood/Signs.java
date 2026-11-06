package io.github.asgerjon.mcfly.wood;

import ganymedes01.etfuturum.ModItems;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlySign;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.copper.Weathering;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.mcfly.network.McFlyNetwork;
import io.github.asgerjon.mcfly.network.OpenSignEditorMessage;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Signs as 26.3 has them (SPEC-modern-movement.md 91.8, step F; 26.3's `SignBlock`, `SignBlockEntity`, the
 * `SignApplicator`s, `handleSignUpdate`), for every `TileEntitySign`: vanilla's oak sign and McFly's. Two sides, the
 * side a player faces, using one (applicators, then the editor), who may edit, the update from the editor; the dyes
 * and the colours drawn (the fork's `ModernSigns`, spec 67.2, moved here).
 */
public final class Signs {

	private static final Logger LOGGER = LogManager.getLogger("McFly");

	public static final String BLACK = "black";
	public static final int LINES = 4;
	/** 26.3's `MAX_TEXT_LINE_WIDTH` and `TEXT_LINE_HEIGHT`. */
	public static final int MAX_WIDTH = 90, LINE_HEIGHT = 10;
	/** 26.3's `ServerboundSignUpdatePacket`: a line of at most 384 characters. */
	public static final int MAX_CHARACTERS = 384;
	private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9A-FK-OR]");

	private Signs() {
	}

	/**
	 * McFly's signs' tile entity, under EFR's id `etfuturum.sign` (registered whatever the options): 1.7.10's sign, with
	 * McFly's sides, wax and editor as every `TileEntitySign` has them.
	 */
	public static class Tile extends TileEntitySign {
	}

	/** A sign's state (26.3's `SignBlockEntity`), on every `TileEntitySign` by `mixins.early.wood.MixinTileEntitySign`. */
	public interface Sided {
		/** The side; the front's lines are 1.7.10's `signText`. */
		SignSide mcfly$side(boolean front);

		boolean mcfly$isWaxed();

		/** Whether it changed. */
		boolean mcfly$setWaxed(boolean waxed);

		/** The player who may edit it (26.3's `playerWhoMayEdit`), or null. */
		UUID mcfly$editor();

		void mcfly$setEditor(UUID player);
	}

	/** Whether a sign block stands (or hangs on a wall): 1.7.10's `BlockSign` flag, by `MixinBlockSign`. */
	public interface Kind {
		boolean mcfly$standing();

		/** Its wood's `block/<wood>_sign` texture, for its models. */
		void mcfly$setSignIcon(net.minecraft.util.IIcon icon);
	}

	/** The sign's wood: its block texture, the editor's picture, its item texture. Vanilla's oak's for any other sign. */
	public static String blockTexture(Block block) {
		return block instanceof McFlySign sign ? sign.blockTexture
			: block instanceof io.github.asgerjon.mcfly.base.McFlyHangingSign hanging ? hanging.blockTexture : "minecraft:oak_sign";
	}

	public static String guiTexture(Block block) {
		return block instanceof McFlySign sign ? sign.guiTexture
			: block instanceof io.github.asgerjon.mcfly.base.McFlyHangingSign hanging ? hanging.guiTexture : "minecraft:textures/gui/signs/oak.png";
	}

	/** Whether it's a hanging sign's (91.9): 60-pixel lines 9 apart, its own editor and drawing. */
	public static boolean hanging(TileEntity sign) {
		return sign instanceof HangingSigns.Tile;
	}

	/** 26.3's `getMaxTextLineWidth`: 90, a hanging sign's 60. */
	public static int maxWidth(TileEntity sign) {
		return hanging(sign) ? HangingSigns.MAX_WIDTH : MAX_WIDTH;
	}

	/** 26.3's `getTextLineHeight`: 10, a hanging sign's 9. */
	public static int lineHeight(TileEntity sign) {
		return hanging(sign) ? HangingSigns.LINE_HEIGHT : LINE_HEIGHT;
	}

	public static boolean standing(Block block) {
		return block instanceof Kind kind ? kind.mcfly$standing() : block != net.minecraft.init.Blocks.wall_sign;
	}

	// ---------------------------------------------------------------- the side a player faces

	/** 26.3's `getYRotationDegrees`: a standing sign's 16th turns; a wall sign's facing (2 north 180, 3 south 0, 4 west 90, 5 east 270). */
	public static float yRot(Block block, int meta) {
		if (standing(block)) {
			return (meta & 15) * 22.5F;
		}
		switch (meta) {
			case 2:
				return 180.0F;
			case 4:
				return 90.0F;
			case 5:
				return 270.0F;
			default:
				return 0.0F;
		}
	}

	/** 26.3's `getSignHitboxCenterPosition`: the middle; a wall sign's board's (its shape's) middle. */
	private static double[] middle(Block block, int meta) {
		if (standing(block) || block instanceof io.github.asgerjon.mcfly.base.McFlyHangingSign) {
			return new double[]{0.5D, 0.5D, 0.5D};
		}
		double y = (4.5D + 12.5D) / 2.0D / 16.0D;
		switch (meta) {
			case 2:
				return new double[]{0.5D, y, 15.0D / 16.0D};
			case 4:
				return new double[]{15.0D / 16.0D, y, 0.5D};
			case 5:
				return new double[]{1.0D / 16.0D, y, 0.5D};
			default:
				return new double[]{0.5D, y, 1.0D / 16.0D};
		}
	}

	/** 26.3's `getSlotPlayerIsFacing`: the front within 90 degrees of the sign's turn, seen from its middle. */
	public static boolean facesFront(TileEntity sign, EntityPlayer player) {
		Block block = sign.getBlockType();
		int meta = sign.getBlockMetadata();
		double[] middle = middle(block, meta);
		double xd = player.posX - (sign.xCoord + middle[0]);
		double zd = player.posZ - (sign.zCoord + middle[2]);
		float playerYRot = (float) (Math.atan2(zd, xd) * 180.0D / Math.PI) - 90.0F;
		return Math.abs(MathHelper.wrapAngleTo180_float(playerYRot - yRot(block, meta))) <= 90.0F;
	}

	// ---------------------------------------------------------------- using a sign

	/** On the client: the last use was taken without a swing (26.3's `CONSUME`); McFly's right click reads and clears it. */
	public static boolean noSwing;

	/**
	 * The player uses the sign (26.3's `useItemOn`, then `useWithoutItem`): whether it's taken (1.7.10's activation). An
	 * applicator applies to the side faced; otherwise a waxed sign refuses with its sound, and anyone who may build
	 * opens the editor, unless another player is editing.
	 */
	public static boolean use(World world, int x, int y, int z, EntityPlayer player) {
		TileEntity tile = world.getTileEntity(x, y, z);
		if (!(tile instanceof TileEntitySign sign) || !(tile instanceof Sided sided)) {
			return false;
		}
		ItemStack held = player.getCurrentEquippedItem();
		Applicator applicator = held == null || !McFlyConfig.modernSignText ? null : applicatorOf(held);
		boolean toUse = applicator != null && player.capabilities.allowEdit;
		if (world.isRemote) {
			noSwing = !toUse && !sided.mcfly$isWaxed();
			return true;
		}
		boolean someoneElse = sided.mcfly$editor() != null && !sided.mcfly$editor().equals(player.getUniqueID());
		boolean front = facesFront(sign, player);
		if (toUse && !sided.mcfly$isWaxed() && !someoneElse) {
			SignSide side = sided.mcfly$side(front);
			if ((applicator == Applicator.WAX || side.hasMessage()) && apply(applicator, held, sign, sided, side)) {
				player.addStat(StatList.objectUseStats[Item.getIdFromItem(held.getItem())], 1);
				GameEvents.postAtBlock(world, "block_change", x, y, z, player, sign.getBlockType(), sign.getBlockMetadata());
				if (!player.capabilities.isCreativeMode && --held.stackSize <= 0) {
					player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
				}
				changed(sign);
				return true;
			}
		}
		if (sided.mcfly$isWaxed()) {
			world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + (hanging(sign) ? ":block.hanging_sign.waxed_interact_fail"
				: ":block.sign.waxed_interact_fail"), 1.0F, 1.0F);
			return true;
		}
		if (!someoneElse && player.capabilities.allowEdit && player instanceof EntityPlayerMP editor) {
			openEditor(editor, sign, front);
			return true;
		}
		return false;
	}

	private enum Applicator {
		DYE, GLOW, INK, WAX
	}

	private static Applicator applicatorOf(ItemStack stack) {
		if (McFlyItems.GLOW_INK_SAC.isEnabled() && stack.getItem() == McFlyItems.GLOW_INK_SAC.get()) {
			return Applicator.GLOW;
		}
		if (ModItems.HONEYCOMB.isEnabled() && stack.getItem() == ModItems.HONEYCOMB.get()) {
			return Applicator.WAX;
		}
		if (stack.getItem() == Items.dye && stack.getItemDamage() == 0) {
			return Applicator.INK;
		}
		return dyeColor(stack) != null ? Applicator.DYE : null;
	}

	/** 26.3's `tryApplyToSign`s: whether it changed the side (or waxed the sign), with its sound. */
	private static boolean apply(Applicator applicator, ItemStack held, TileEntitySign sign, Sided sided, SignSide side) {
		World world = sign.getWorldObj();
		String sound;
		switch (applicator) {
			case GLOW:
				if (side.glowing) {
					return false;
				}
				side.glowing = true;
				sound = "item.glow_ink_sac.use";
				break;
			case INK:
				if (!side.glowing) {
					return false;
				}
				side.glowing = false;
				sound = "item.ink_sac.use";
				break;
			case WAX:
				if (!sided.mcfly$setWaxed(true)) {
					return false;
				}
				Weathering.particles(world, sign.xCoord, sign.yCoord, sign.zCoord, Weathering.WAX_ON);
				sound = "item.honeycomb.wax_on";
				break;
			default:
				String color = dyeColor(held);
				if (color.equals(side.color)) {
					return false;
				}
				side.setColor(color);
				sound = "item.dye.use";
		}
		world.playSoundEffect(sign.xCoord + 0.5D, sign.yCoord + 0.5D, sign.zCoord + 0.5D, McFly.MC_ASSETS + ":" + sound, 1.0F, 1.0F);
		return true;
	}

	/** 1.7.10's dye damages by colour (0 is the ink sac, black). */
	private static final String[] VANILLA_DYES = {BLACK, "red", "green", "brown", "blue", "purple", "cyan", "light_gray", "gray", "pink", "lime", "yellow",
		"light_blue", "magenta", "orange", "white"};
	/** EFR's dyes (`ModItems.DYE`), by damage. */
	private static final String[] EFR_DYES = {"white", "blue", "brown", BLACK};

	/**
	 * The dye's colour, or null. 1.7.10's dyes by damage (but the ink sac, 0); with EFR's new dyes, bone meal, lapis and
	 * cocoa beans aren't dyes (26.3's aren't), EFR's white, blue, brown and black are; other mods' by ore name.
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

	private static void changed(TileEntitySign sign) {
		sign.markDirty();
		sign.getWorldObj().markBlockForUpdate(sign.xCoord, sign.yCoord, sign.zCoord);
	}

	// ---------------------------------------------------------------- editing

	/**
	 * 26.3's `openTextEdit`: the player may edit it, the sign is sent to them, then McFly's open message (one channel:
	 * they arrive in order).
	 */
	public static void openEditor(EntityPlayerMP player, TileEntitySign sign, boolean front) {
		((Sided) sign).mcfly$setEditor(player.getUniqueID());
		player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(sign.xCoord, sign.yCoord, sign.zCoord, sign.getWorldObj()));
		Packet description = sign.getDescriptionPacket();
		if (description != null) {
			player.playerNetServerHandler.sendPacket(description);
		}
		McFlyNetwork.CHANNEL.sendTo(new OpenSignEditorMessage(sign.xCoord, sign.yCoord, sign.zCoord, front), player);
	}

	/** 26.3's `isWithinBlockInteractionRange(pos, 4.0)`: the eyes within the reach (4.5, creative 5) plus 4 of the block. */
	public static boolean inReach(EntityPlayer player, int x, int y, int z) {
		double eyeY = player.boundingBox.minY + (player.isSneaking() ? 1.27D : 1.62D);
		double dx = Math.max(Math.max(x - player.posX, player.posX - (x + 1)), 0.0D);
		double dy = Math.max(Math.max(y - eyeY, eyeY - (y + 1)), 0.0D);
		double dz = Math.max(Math.max(z - player.posZ, player.posZ - (z + 1)), 0.0D);
		double range = (player.capabilities.isCreativeMode ? 5.0D : 4.5D) + 4.0D;
		return dx * dx + dy * dy + dz * dz < range * range;
	}

	/** Each tick on the server: an editor gone or out of reach may edit no more (26.3's `clearInvalidPlayerWhoMayEdit`). */
	public static void tick(TileEntitySign sign) {
		Sided sided = (Sided) sign;
		UUID editor = sided.mcfly$editor();
		if (editor == null || sign.getWorldObj() == null || sign.getWorldObj().isRemote) {
			return;
		}
		EntityPlayer player = sign.getWorldObj().func_152378_a(editor);
		if (player == null || !inReach(player, sign.xCoord, sign.yCoord, sign.zCoord)) {
			sided.mcfly$setEditor(null);
		}
	}

	/**
	 * The editor's lines (26.3's `handleSignUpdate`, `updateSignText`): formatting codes stripped; taken from the player
	 * who may edit it, on a sign not waxed, into that side (its colour and glow kept); the editor cleared.
	 */
	public static void update(EntityPlayerMP player, int x, int y, int z, boolean front, String[] lines) {
		player.func_143004_u();
		World world = player.worldObj;
		if (!world.blockExists(x, y, z)) {
			return;
		}
		TileEntity tile = world.getTileEntity(x, y, z);
		if (!(tile instanceof TileEntitySign sign) || !(tile instanceof Sided sided)) {
			return;
		}
		if (sided.mcfly$isWaxed() || !player.getUniqueID().equals(sided.mcfly$editor())) {
			LOGGER.warn("Player {} just tried to change non-editable sign", player.getCommandSenderName());
			return;
		}
		SignSide side = sided.mcfly$side(front);
		for (int i = 0; i < LINES; i++) {
			side.lines[i] = i < lines.length && lines[i] != null ? FORMATTING.matcher(lines[i]).replaceAll("") : "";
		}
		sided.mcfly$setEditor(null);
		changed(sign);
	}

	// ---------------------------------------------------------------- the colours drawn

	/** 26.3's `DyeColor.textColor`, by the dye's name. */
	private static final Map<String, Integer> TEXT_COLORS = new HashMap<>();

	static {
		String[] names = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red",
			BLACK};
		int[] colors = {0xFFFFFF, 0xFF681F, 0xFF00FF, 0x9AC0CD, 0xFFFF00, 0xBFFF00, 0xFF69B4, 0x808080, 0xD3D3D3, 0x00FFFF, 0xA020F0, 0x0000FF, 0x8B4513, 0x00FF00,
			0xFF0000, 0x000000};
		for (int i = 0; i < names.length; i++) {
			TEXT_COLORS.put(names[i], colors[i]);
		}
	}

	private static int textColorOf(String color) {
		Integer value = TEXT_COLORS.get(color);
		return value == null ? 0 : value;
	}

	/** 26.3's `ARGB.scaleRGB(color, 0.4F)`. */
	private static int dark(int color) {
		return (int) ((color >> 16 & 255) * 0.4F) << 16 | (int) ((color >> 8 & 255) * 0.4F) << 8 | (int) ((color & 255) * 0.4F);
	}

	/** The colour the text is drawn in: plain, the dye's times 0.4; glowing, the dye's own. */
	public static int textColor(String color, boolean glowing) {
		int text = textColorOf(color);
		return glowing ? text : dark(text);
	}

	/** The outline's colour, or -1 for none: glowing text only, the dye's times 0.4; glowing black's 0xF0EBCC. */
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
