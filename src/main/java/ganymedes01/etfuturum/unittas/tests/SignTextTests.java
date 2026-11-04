package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.signs.ISignText;
import ganymedes01.etfuturum.signs.ModernSigns;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.WorldSettings;

/**
 * Signs take dyes, glow ink, ink sacs and honeycomb (SPEC-modern-movement.md §67.2; 26.3's `SignBlock.useItemOn`, the
 * `SignApplicator`s, `SignText`, `AbstractSignRenderer`). A sign at (2, 4, 3), beside the spawn, used by the server's
 * own right-click path (`activateBlockOrUseItem`: the interact event, the block, then the item).
 */
public class SignTextTests extends TasTestCase {

	private static final int X = 2, Y = 4, Z = 3;
	private static final String MODERN = Reference.MCAssetVer + ":";

	@Override
	protected void setUp() {
		if (!McFlyConfig.modernSignText) {
			skipTest("modernSignText is off");
		}
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			world().setBlockToAir(X, Y, Z);
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	/** A dye colours the text, used up, with its sound; the same dye again changes nothing and isn't used. */
	public void testDyeColoursTheText() {
		sign(Blocks.standing_sign, "Hello");
		survival();
		setHeld(new ItemStack(Items.dye, 2, 1));
		clearSounds();
		use();
		assertEqual("red", state()[0], "the colour");
		assertEqual(1, onServer(() -> player().getHeldItem().stackSize), "dyes left");
		waitForSound(MODERN + "item.dye.use", 10);
		clearSounds();
		use();
		assertEqual(1, onServer(() -> player().getHeldItem() == null ? 0 : player().getHeldItem().stackSize), "dyes left after the same dye again");
		ticks(5);
		assertNotIn(MODERN + "item.dye.use", soundsHeard(), "the dye's sound again");
	}

	/** In creative the dye isn't used up. */
	public void testCreativeKeepsTheDye() {
		sign(Blocks.standing_sign, "Hello");
		setHeld(new ItemStack(Items.dye, 1, 11));
		use();
		assertEqual("yellow", state()[0], "the colour");
		assertEqual(1, onServer(() -> player().getHeldItem().stackSize), "dyes left in creative");
	}

	/** A glow ink sac makes the text glow; an ink sac (1.7.10's black dye, `dye:0`) stops it; neither twice. */
	public void testGlowAndInk() {
		need(McFlyItems.GLOW_INK_SAC.isEnabled(), "the glow ink sac is off");
		sign(Blocks.standing_sign, "Hello");
		survival();
		setHeld(McFlyItems.GLOW_INK_SAC.newItemStack(2));
		clearSounds();
		use();
		assertEqual("glowing", state()[1], "the glow");
		waitForSound(MODERN + "item.glow_ink_sac.use", 10);
		use();
		assertEqual(1, onServer(() -> player().getHeldItem().stackSize), "glow ink sacs left after a second use");
		setHeld(new ItemStack(Items.dye, 2, 0));
		clearSounds();
		use();
		assertEqual("plain", state()[1], "the glow after an ink sac");
		waitForSound(MODERN + "item.ink_sac.use", 10);
		use();
		assertEqual(1, onServer(() -> player().getHeldItem().stackSize), "ink sacs left on a sign that doesn't glow");
		assertEqual("black", state()[0], "an ink sac coloured the text");
	}

	/** A sign with no text takes nothing. */
	public void testEmptySignTakesNothing() {
		sign(Blocks.standing_sign, "");
		survival();
		setHeld(new ItemStack(Items.dye, 1, 1));
		use();
		assertEqual("black", state()[0], "an empty sign's colour");
		assertEqual(1, onServer(() -> player().getHeldItem() == null ? 0 : player().getHeldItem().stackSize), "dyes left");
	}

	/** Honeycomb waxes the sign (its sound), and a waxed sign takes no dye. */
	public void testHoneycombWaxes() {
		need(ModItems.HONEYCOMB.isEnabled(), "honeycomb is off");
		sign(Blocks.standing_sign, "Hello");
		survival();
		setHeld(ModItems.HONEYCOMB.newItemStack(1));
		clearSounds();
		use();
		assertEqual("waxed", state()[2], "the wax");
		waitForSound(MODERN + "item.honeycomb.wax_on", 10);
		setHeld(new ItemStack(Items.dye, 1, 1));
		use();
		assertEqual("black", state()[0], "a waxed sign's colour after a dye");
	}

	/** EFR's dyes: its black dye colours black; with them, bone meal (1.7.10's white dye) isn't a dye for signs. */
	public void testEfrDyes() {
		need(ModItems.DYE.isEnabled(), "EFR's dyes are off");
		sign(Blocks.standing_sign, "Hello");
		setHeld(new ItemStack(Items.dye, 1, 1));
		use();
		setHeld(ModItems.DYE.newItemStack(1, 3));
		use();
		assertEqual("black", state()[0], "after EFR's black dye");
		setHeld(new ItemStack(Items.dye, 1, 15));
		use();
		assertEqual("black", state()[0], "after bone meal");
		setHeld(ModItems.DYE.newItemStack(1, 0));
		use();
		assertEqual("white", state()[0], "after EFR's white dye");
	}

	/** EFR's own wood signs take them too. */
	public void testEfrSign() {
		need(ModBlocks.SIGN_SPRUCE.isEnabled(), "EFR's signs are off");
		sign(ModBlocks.SIGN_SPRUCE.get(), "Hello");
		setHeld(new ItemStack(Items.dye, 1, 2));
		use();
		assertEqual("green", state()[0], "an EFR sign's colour");
	}

	/** Saved (`Color`, `GlowingText`, `Waxed`), read back, and on the client. */
	public void testSavedAndOnTheClient() {
		need(McFlyItems.GLOW_INK_SAC.isEnabled() && ModItems.HONEYCOMB.isEnabled(), "the glow ink sac or honeycomb is off");
		sign(Blocks.standing_sign, "Hello");
		for (ItemStack stack : new ItemStack[]{new ItemStack(Items.dye, 1, 5), McFlyItems.GLOW_INK_SAC.newItemStack(1), ModItems.HONEYCOMB.newItemStack(1)}) {
			setHeld(stack);
			use();
		}
		String[] saved = onServer(() -> {
			NBTTagCompound tag = new NBTTagCompound();
			world().getTileEntity(X, Y, Z).writeToNBT(tag);
			TileEntitySign copy = new TileEntitySign();
			copy.readFromNBT(tag);
			ISignText text = (ISignText) copy;
			return new String[]{tag.getString("Color"), String.valueOf(tag.getBoolean("GlowingText")), String.valueOf(tag.getBoolean("Waxed")),
				text.etfu$getColor() + " " + text.etfu$isGlowing() + " " + text.etfu$isWaxed()};
		});
		assertEqual("purple", saved[0], "saved Color");
		assertEqual("true", saved[1], "saved GlowingText");
		assertEqual("true", saved[2], "saved Waxed");
		assertEqual("purple true true", saved[3], "read back");
		waitUntilOnClient("the client's sign purple and glowing", () -> {
			TileEntity sign = mc().theWorld.getTileEntity(X, Y, Z);
			return sign instanceof ISignText text && "purple".equals(text.etfu$getColor()) && text.etfu$isGlowing();
		}, 40);
	}

	/**
	 * The colours drawn (26.3's `AbstractSignRenderer`): plain text the dye's colour x 0.4; glowing text the colour
	 * itself, outlined in the x 0.4 one; glowing black outlined in 0xF0EBCC, near or far.
	 */
	public void testColoursDrawn() {
		assertEqual(0x660000, ModernSigns.textColor("red", false));
		assertEqual(-1, ModernSigns.outlineColor("red", false));
		assertEqual(0xFF0000, ModernSigns.textColor("red", true));
		assertEqual(0x660000, ModernSigns.outlineColor("red", true));
		assertFalse(ModernSigns.outlineAlways("red", true), "red's outline drawn from afar");
		assertEqual(0x000000, ModernSigns.textColor("black", false));
		assertEqual(0x000000, ModernSigns.textColor("black", true));
		assertEqual(0xF0EBCC, ModernSigns.outlineColor("black", true));
		assertTrue(ModernSigns.outlineAlways("black", true), "black's outline not drawn from afar");
		assertEqual(0x666666, ModernSigns.textColor("white", false));
		assertEqual(0x371B07, ModernSigns.textColor("brown", false));
	}

	// ---------------------------------------------------------------- helpers

	private static void need(boolean on, String why) {
		if (!on) {
			skipTest(why);
		}
	}

	private void survival() {
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
	}

	/** A standing sign of the block at the spot, with the text on its first line. */
	private void sign(Block block, String text) {
		onServer(() -> {
			world().setBlock(X, Y, Z, block, 0, 3);
			TileEntity sign = world().getTileEntity(X, Y, Z);
			assertTrue(sign instanceof TileEntitySign, "no sign at the spot");
			((TileEntitySign) sign).signText[0] = text;
			world().markBlockForUpdate(X, Y, Z);
		});
		assertTrue(onServer(() -> world().getTileEntity(X, Y, Z) instanceof ISignText), "signs have no colour, glow or wax");
	}

	/** The player right-clicks the sign with what they hold, as the server takes a click. */
	private void use() {
		onServer(() -> player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), player().getHeldItem(), X, Y, Z, 2, 0.5F, 0.5F, 0.5F));
	}

	/** The sign's colour, "glowing" or "plain", "waxed" or "unwaxed". */
	private String[] state() {
		return onServer(() -> {
			ISignText text = (ISignText) world().getTileEntity(X, Y, Z);
			return new String[]{text.etfu$getColor(), text.etfu$isGlowing() ? "glowing" : "plain", text.etfu$isWaxed() ? "waxed" : "unwaxed"};
		});
	}
}
