package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.items.LodestoneCompass;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The enchantment glint on items in item frames (SPEC-modern-movement.md §64.2; the user's report: none glint in EFR's
 * frames, dropped ones do; books, tools, armour, a lodestone compass). The glint scrolls with time and nothing else
 * in the view moves, so shots a few ticks apart differ where something glints: five frames in a row on black wool, a
 * plain sword (the still control), then an enchanted sword, an enchanted book, an enchanted chestplate and a lodestone
 * compass (its needle held still: it points at a fixed spot).
 */
public class GlintTests extends TasTestCase {

	private static final int GROUND = 4, AT_X = 260, AT_Z = 300, WALL = AT_Z - 5;
	private static final String[] NAMES = {"a plain sword", "an enchanted sword", "an enchanted book", "an enchanted chestplate", "a lodestone compass"};
	/**
	 * Mean change (0-255) over a frame's strip that a glint makes, and the most a still one may show. The still frame
	 * reads 0.00; the thin sword read as low as 0.12 (2026-11-03: a streak only partly over its blade), the others 0.25.
	 */
	private static final double GLINTS = 0.05D, STILL = 0.02D;

	private boolean fancy;
	private float fov;

	@Override
	protected int timeoutSeconds() {
		return 90;
	}

	@Override
	protected void setUp() {
		fancy = onClient(() -> mc().gameSettings.fancyGraphics);
		fov = onClient(() -> mc().gameSettings.fovSetting);
		onClient(() -> {
			mc().gameSettings.fancyGraphics = true;
			mc().gameSettings.fovSetting = 70.0F;
		});
	}

	@Override
	protected void tearDown() {
		onClient(() -> {
			mc().gameSettings.fancyGraphics = fancy;
			mc().gameSettings.fovSetting = fov;
			mc().gameSettings.hideGUI = false;
		});
		onServer(() -> {
			for (EntityItemFrame frame : entitiesNear(EntityItemFrame.class, 300.0D)) {
				frame.setDead();
			}
			for (int x = AT_X - 8; x <= AT_X + 8; x++) {
				for (int y = GROUND; y <= GROUND + 7; y++) {
					world().setBlockToAir(x, y, WALL);
				}
			}
		});
	}

	/** Every enchanted item in a frame glints; the plain one stays still. */
	public void testItemsInFramesGlint() throws Exception {
		onServer(() -> {
			for (int x = AT_X - 8; x <= AT_X + 8; x++) {
				for (int y = GROUND; y <= GROUND + 7; y++) {
					world().setBlock(x, y, WALL, Blocks.wool, 15, 3);
				}
			}
			ItemStack[] items = items();
			for (int i = 0; i < items.length; i++) {
				EntityItemFrame frame = new EntityItemFrame(world(), AT_X - 4 + 2 * i, GROUND + 2, WALL, 0);
				if (!frame.onValidSurface()) {
					fail("the frame can't hang on the wall");
				}
				world().spawnEntityInWorld(frame);
				frame.setDisplayedItem(items[i]);
			}
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			player().playerNetServerHandler.setPlayerLocation(AT_X + 0.5D, GROUND, AT_Z + 0.5D, 180.0F, 0.0F);
		});
		waitUntilOnClient("arriving", () -> Math.abs(mc().thePlayer.posX - (AT_X + 0.5D)) < 0.01D && Math.abs(mc().thePlayer.posZ - (AT_Z + 0.5D)) < 0.01D, 60);
		look(180.0F, 0.0F);
		onClient(() -> {
			mc().gameSettings.hideGUI = true;
		});
		ticks(20);
		// 1.7.10's glint scrolls in 375 ms (its first layer; 609 ms the second): shots about half a period apart (each
		// screenshot takes 2 ticks more: 550 and 950 ms, and 1.5 s from first to last), the most change of any two
		// (0.75 s apart, two whole periods, it read low once)
		List<BufferedImage> shots = new ArrayList<>();
		shots.add(shot("glint.frames.0"));
		int[] gaps = {9, 17};
		for (int i = 0; i < gaps.length; i++) {
			ticks(gaps[i]);
			shots.add(shot("glint.frames." + (i + 1)));
		}
		double[] change = new double[NAMES.length];
		for (int a = 0; a < shots.size(); a++) {
			for (int b = a + 1; b < shots.size(); b++) {
				double[] step = changePerStrip(shots.get(a), shots.get(b));
				for (int strip = 0; strip < NAMES.length; strip++) {
					change[strip] = Math.max(change[strip], step[strip]);
				}
			}
		}
		StringBuilder seen = new StringBuilder();
		for (int i = 0; i < NAMES.length; i++) {
			seen.append(NAMES[i]).append(' ').append(String.format("%.2f", change[i])).append(i < NAMES.length - 1 ? ", " : "");
		}
		assertTrue(change[0] < STILL, "the plain sword's frame changed (" + seen + "): something else moves in the view");
		List<String> still = new ArrayList<>();
		for (int i = 1; i < NAMES.length; i++) {
			if (change[i] < GLINTS) {
				still.add(NAMES[i]);
			}
		}
		assertEqual(new ArrayList<String>(), still, "no glint in the frame (change per frame: " + seen + ")");
	}

	// ---------------------------------------------------------------- helpers

	private static ItemStack[] items() {
		ItemStack sword = new ItemStack(Items.diamond_sword);
		ItemStack enchanted = new ItemStack(Items.diamond_sword);
		enchanted.addEnchantment(Enchantment.sharpness, 1);
		ItemStack book = new ItemStack(Items.enchanted_book);
		Items.enchanted_book.addEnchantment(book, new net.minecraft.enchantment.EnchantmentData(Enchantment.sharpness, 1));
		ItemStack chestplate = new ItemStack(Items.iron_chestplate);
		chestplate.addEnchantment(Enchantment.protection, 1);
		ItemStack compass = new ItemStack(Items.compass);
		LodestoneCompass.setTracker(compass, false, new int[]{AT_X, GROUND, WALL - 20}, 0);
		return new ItemStack[]{sword, enchanted, book, chestplate, compass};
	}

	private BufferedImage shot(String name) throws Exception {
		return ImageIO.read(new File(resultsFolder(), screenshot(name)));
	}

	/** Mean change per pixel (0-255, the channels' mean) in each frame's strip of the upper half: the frames 2 blocks apart. */
	private static double[] changePerStrip(BufferedImage a, BufferedImage b) {
		int width = a.getWidth(), height = a.getHeight();
		double[] change = new double[NAMES.length];
		int[] count = new int[NAMES.length];
		// at 4.5 blocks a 2-block step is about a fifth of the half-width either way; strips between the midpoints
		double step = width / 2.0D * (2.0D / 4.5D) / Math.tan(Math.toRadians(mc().gameSettings.fovSetting / 2.0D)) / ((double) width / height);
		for (int y = height / 20; y < height / 2; y++) {
			for (int x = 0; x < width; x++) {
				int strip = (int) Math.floor((x - width / 2.0D) / step + 2.5D);
				if (strip < 0 || strip >= NAMES.length) {
					continue;
				}
				int p = a.getRGB(x, y), q = b.getRGB(x, y);
				change[strip] += (Math.abs((p >> 16 & 255) - (q >> 16 & 255)) + Math.abs((p >> 8 & 255) - (q >> 8 & 255)) + Math.abs((p & 255) - (q & 255))) / 3.0D;
				count[strip]++;
			}
		}
		for (int i = 0; i < NAMES.length; i++) {
			change[i] = count[i] == 0 ? 0.0D : change[i] / count[i];
		}
		return change;
	}
}
