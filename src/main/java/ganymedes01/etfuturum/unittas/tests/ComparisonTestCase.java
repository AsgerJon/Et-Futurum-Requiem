package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.core.handlers.ClientEventHandler;
import ganymedes01.etfuturum.unittas.TasTestCase;
import ganymedes01.etfuturum.client.skins.PlayerModelManager;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests whose screenshots unittas_mcfly-compare.sh holds against 26.3's (unittas_mcfly-reference takes the same views, with the same
 * names and settings). A pack paints the measured things magenta (the shield, the trident, Riptide's swirl, the
 * lightning rod, the polar bear, the phantom, and magenta wool for the calibration) so their outlines can be measured; field of view 70, no view
 * bobbing, no clouds, GUI scale 2, the main hand the right (the config's mainHandLeft only for the run, not saved).
 * The settings and packs are put back afterwards.
 */
public abstract class ComparisonTestCase extends TasTestCase {

	/** The views, which may throw. */
	@FunctionalInterface
	protected interface Views {
		void take() throws Exception;
	}

	/** Takes the views with the comparison's pack and settings, then puts the player's back (also when a view fails). */
	protected final void comparisonViews(Views views) throws Exception {
		Object[] before = onClient(() -> new Object[]{mc().gameSettings.fovSetting, mc().gameSettings.viewBobbing, mc().gameSettings.clouds,
			mc().gameSettings.guiScale, new ArrayList<>(mc().getResourcePackRepository().getRepositoryEntries()), McFlyConfig.mainHandLeft,
			McFlyConfig.skinCape});
		try {
			magentaPack();
			onClient(() -> {
				mc().gameSettings.fovSetting = 70.0F;
				mc().gameSettings.viewBobbing = false;
				mc().gameSettings.clouds = false;
				mc().gameSettings.guiScale = 2;
				McFlyConfig.mainHandLeft = false;
				// no cape: the comparison's skin has none in 26.3's run (EFR's skin options, sent with the hand)
				McFlyConfig.skinCape = false;
				ClientEventHandler.sendPlayerSettings();
			});
			ResourceLocation ownSkin = onClient(() -> {
				forgetSkinTexture();
				return wearSkin(new ResourceLocation("textures/entity/unittas_skin.png"));
			});
			try {
				views.take();
			} finally {
				onClient(() -> {
					wearSkin(ownSkin);
					PlayerModelManager.alexCache.remove(mc().thePlayer.getUniqueID());
					// 1.7.10 reloads every texture it knows when the packs change: once the pack is gone this one would
					// fail and stay the missing texture for good (the next test's views drew the player magenta)
					forgetSkinTexture();
					return null;
				});
			}
		} finally {
			keyUp(mc().gameSettings.keyBindUseItem);
			onClient(() -> {
				mc().gameSettings.fovSetting = (Float) before[0];
				mc().gameSettings.viewBobbing = (Boolean) before[1];
				mc().gameSettings.clouds = (Boolean) before[2];
				mc().gameSettings.guiScale = (Integer) before[3];
				@SuppressWarnings("unchecked")
				List<ResourcePackRepository.Entry> packs = (List<ResourcePackRepository.Entry>) before[4];
				mc().getResourcePackRepository().func_148527_a(packs);
				mc().refreshResources();
				McFlyConfig.mainHandLeft = (Boolean) before[5];
				McFlyConfig.skinCape = (Boolean) before[6];
				ClientEventHandler.sendPlayerSettings();
			});
		}
	}

	/**
	 * The local player's age at every comparison shot (26.3's run sets the same): the arms' idle sway, Riptide's turn
	 * and the like go by it, and the two runs' players are otherwise of any age. Seen from behind, a raised trident
	 * points nearly at the camera, so the sway alone turned it 20-odd degrees on screen.
	 */
	private static final int SHOT_AGE = 1000;

	/** A comparison view: the player's age set to the fixed one first, then the screenshot (about 3 ticks later). */
	protected final void comparisonShot(String name) {
		onClient(() -> {
			mc().thePlayer.ticksExisted = SHOT_AGE;
		});
		screenshot(name);
	}

	/** The inventory screen with the cursor where the player model looks straight ahead (26.3's run does the same). */
	protected final void inventoryShot(String name) {
		onClient(() -> {
			mc().displayGuiScreen(new GuiInventory(mc().thePlayer) {
				@Override
				public void drawScreen(int mouseX, int mouseY, float partialTicks) {
					// the figure looks from the box's middle with modernInventoryFigure (26.3's), from (51, 25) without
					boolean modern = io.github.asgerjon.mcfly.config.McFlyConfig.modernInventoryFigure;
					super.drawScreen(guiLeft + (modern ? 50 : 51), guiTop + (modern ? 43 : 25), partialTicks);
				}
			});
		});
		ticks(3);
		comparisonShot(name);
		onClient(() -> {
			mc().displayGuiScreen(null);
		});
	}

	/**
	 * The comparison's own skin, the same in both games' packs (26.3's run draws it with the same code): plain grey,
	 * the head lighter, the outer layer left clear, no magenta. Whatever skin the player has otherwise (a downloaded one
	 * can be missing in one game and there in the other) would count wherever it has magenta.
	 */
	static BufferedImage comparisonSkin() {
		BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 64; x++) {
			for (int y = 0; y < 64; y++) {
				boolean outer = y < 16 && x >= 32 || y >= 32 && y < 48 && x < 56 || y >= 48 && (x < 16 || x >= 48);
				skin.setRGB(x, y, outer ? 0 : y < 16 ? 0xFFB4B4B4 : 0xFF6E6E6E);
			}
		}
		return skin;
	}

	/**
	 * The skin's textures out of 1.7.10's texture manager, in both domains EFR's renderer may ask: taken out of its map
	 * (1.7.10's {@code deleteTexture} only frees the GL texture and leaves the entry, and a failed load's entry is the
	 * shared missing texture, whose freeing spoiled every texture after it), and freed unless it is the shared one.
	 */
	@SuppressWarnings("unchecked")
	private static void forgetSkinTexture() throws Exception {
		Field field = null;
		for (String name : new String[]{"mapTextureObjects", "field_110585_a"}) {
			try {
				field = net.minecraft.client.renderer.texture.TextureManager.class.getDeclaredField(name);
				break;
			} catch (NoSuchFieldException ignored) {
			}
		}
		if (field == null) {
			fail("1.7.10's texture manager has no mapTextureObjects");
		}
		field.setAccessible(true);
		java.util.Map<ResourceLocation, net.minecraft.client.renderer.texture.ITextureObject> textures =
				(java.util.Map<ResourceLocation, net.minecraft.client.renderer.texture.ITextureObject>) field.get(mc().getTextureManager());
		for (ResourceLocation skin : new ResourceLocation[]{new ResourceLocation("textures/entity/unittas_skin.png"),
				new ResourceLocation("etfuturum", "textures/entity/unittas_skin.png")}) {
			net.minecraft.client.renderer.texture.ITextureObject texture = textures.remove(skin);
			if (texture != null && texture != net.minecraft.client.renderer.texture.TextureUtil.missingTexture) {
				net.minecraft.client.renderer.texture.TextureUtil.deleteTexture(texture.getGlTextureId());
			}
		}
	}

	/** The client player wears the comparison's skin (wide arms) for the views; null puts its own back. */
	private static ResourceLocation wearSkin(ResourceLocation skin) throws Exception {
		ResourceLocation before = swapPlayerTexture(new String[]{"locationSkin", "field_110312_d"}, skin);
		PlayerModelManager.alexCache.put(mc().thePlayer.getUniqueID(), false);
		return before;
	}

	private static ResourceLocation swapPlayerTexture(String[] names, ResourceLocation texture) throws Exception {
		Field field = null;
		for (String name : names) {
			try {
				field = AbstractClientPlayer.class.getDeclaredField(name);
				break;
			} catch (NoSuchFieldException ignored) {
			}
		}
		if (field == null) {
			fail("1.7.10's player has no " + names[0]);
		}
		field.setAccessible(true);
		ResourceLocation before = (ResourceLocation) field.get(mc().thePlayer);
		field.set(mc().thePlayer, texture);
		return before;
	}

	/** EFR's own copy of a texture (from its jar), every pixel magenta at the pixel's own alpha. */
	private static void paintKeepingAlpha(String path, File out) throws Exception {
		java.io.InputStream in = ComparisonTestCase.class.getResourceAsStream("/" + path);
		if (in == null) {
			fail("EFR has no " + path);
		}
		BufferedImage original;
		try {
			original = ImageIO.read(in);
		} finally {
			in.close();
		}
		BufferedImage painted = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < original.getWidth(); x++) {
			for (int y = 0; y < original.getHeight(); y++) {
				painted.setRGB(x, y, original.getRGB(x, y) & 0xFF000000 | 0x00FF00FF);
			}
		}
		if (!out.getParentFile().isDirectory() && !out.getParentFile().mkdirs()) {
			fail("can't make " + out.getParentFile());
		}
		ImageIO.write(painted, "png", out);
		// an animated texture's frames come with it (lava_still): 1.7.10 takes the .mcmeta only from the same pack or
		// one above, and without it a strip of frames is a broken texture
		try (java.io.InputStream meta = ComparisonTestCase.class.getResourceAsStream("/" + path + ".mcmeta")) {
			if (meta != null) {
				Files.copy(meta, new File(out.getPath() + ".mcmeta").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	/**
	 * A pack in the game's resourcepacks painting the measured textures magenta (every pixel), put on top of the selected
	 * ones. Painting a whole texture is enough: a model only shows the parts it uses.
	 */
	private void magentaPack() throws Exception {
		File pack = onClient(() -> new File(mc().getResourcePackRepository().getDirResourcepacks(), "unittas_magenta"));
		File shield = new File(pack, "assets/etfuturum/textures/entity/shield");
		File trident = new File(pack, "assets/etfuturum/textures/entity/trident");
		File blocks = new File(pack, "assets/minecraft/textures/blocks");
		File bear = new File(pack, "assets/minecraft/textures/entity/bear");
		File phantom = new File(pack, "assets/minecraft/textures/entity/phantom");
		for (File folder : new File[]{shield, trident, blocks, bear, phantom}) {
			if (!folder.isDirectory() && !folder.mkdirs()) {
				fail("can't make " + folder);
			}
		}
		Files.write(new File(pack, "pack.mcmeta").toPath(),
			"{\"pack\":{\"pack_format\":1,\"description\":\"unitTAS: what the comparison measures, in magenta\"}}\n".getBytes(StandardCharsets.UTF_8));
		BufferedImage magenta = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 64; x++) {
			for (int y = 0; y < 64; y++) {
				magenta.setRGB(x, y, 0xFFFF00FF);
			}
		}
		ImageIO.write(magenta, "png", new File(shield, "base_nopattern.png"));
		ImageIO.write(magenta, "png", new File(shield, "base.png"));
		ImageIO.write(magenta, "png", new File(trident, "trident.png"));
		ImageIO.write(magenta, "png", new File(trident, "trident_riptide.png"));
		ImageIO.write(magenta, "png", new File(bear, "polarbear.png"));
		ImageIO.write(magenta, "png", new File(bear, "polarbear_baby.png"));
		// the phantom's body; its eyes left as they are, so they show as the same gaps in both games (spec §68.7)
		ImageIO.write(magenta, "png", new File(phantom, "phantom.png"));
		ImageIO.write(magenta.getSubimage(0, 0, 16, 16), "png", new File(blocks, "wool_colored_magenta.png"));
		ImageIO.write(magenta.getSubimage(0, 0, 16, 16), "png", new File(blocks, "lightning_rod.png"));
		// powder snow (spec §62.5): the block whole, the rest keeping their own transparency (the bucket's outline, the
		// frost's edge, the hearts), as 26.3's run paints them
		ImageIO.write(magenta.getSubimage(0, 0, 16, 16), "png", new File(blocks, "powder_snow.png"));
		paintKeepingAlpha("assets/minecraft/textures/items/powder_snow_bucket.png", new File(pack, "assets/minecraft/textures/items/powder_snow_bucket.png"));
		paintKeepingAlpha("assets/minecraft/textures/misc/powder_snow_outline.png", new File(pack, "assets/minecraft/textures/misc/powder_snow_outline.png"));
		for (String heart : new String[]{"full", "full_blinking", "half", "half_blinking", "hardcore_full", "hardcore_full_blinking", "hardcore_half", "hardcore_half_blinking"}) {
			String path = "assets/minecraft/textures/gui/sprites/hud/heart/frozen_" + heart + ".png";
			paintKeepingAlpha(path, new File(pack, path));
		}
		// batch 4 (spec §63.5, §63.6): the cauldron and lava, and the items held in third person, at their own alpha
		for (String path : new String[]{"blocks/cauldron_side", "blocks/cauldron_top", "blocks/cauldron_inner", "blocks/cauldron_bottom",
				"blocks/lava_still", "blocks/stone", "items/bucket_empty", "items/iron_sword", "items/fishing_rod_uncast", "items/bow_standby",
				"misc/spyglass_scope", "items/spyglass"}) {
			String file = "assets/minecraft/textures/" + path + ".png";
			paintKeepingAlpha(file, new File(pack, file));
		}
		paintKeepingAlpha("assets/etfuturum/textures/item/spyglass_model.png", new File(pack, "assets/etfuturum/textures/item/spyglass_model.png"));
		// the player's skin for the views: EFR's renderer reads a 64 x 64 skin from its own domain, 1.7.10's from minecraft
		for (String domain : new String[]{"etfuturum", "minecraft"}) {
			File skin = new File(pack, "assets/" + domain + "/textures/entity/unittas_skin.png");
			if (!skin.getParentFile().isDirectory() && !skin.getParentFile().mkdirs()) {
				fail("can't make " + skin.getParentFile());
			}
			ImageIO.write(comparisonSkin(), "png", skin);
		}
		onClient(() -> {
			ResourcePackRepository repository = mc().getResourcePackRepository();
			repository.updateRepositoryEntriesAll();
			List<ResourcePackRepository.Entry> selected = new ArrayList<>(repository.getRepositoryEntries());
			for (ResourcePackRepository.Entry entry : repository.getRepositoryEntriesAll()) {
				if (entry.getResourcePackName().equals("unittas_magenta")) {
					// last is on top (1.7.10 reads the packs from the end), above modern-textures and the like
					selected.add(entry);
				}
			}
			repository.func_148527_a(selected);
			mc().refreshResources();
		});
		ticks(10);
	}
}
