package io.github.asgerjon.mcfly.banner.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.banner.BannerBlockEntity;
import io.github.asgerjon.mcfly.banner.BannerPatterns;
import io.github.asgerjon.mcfly.banner.Banners;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A banner's or shield's canvas (SPEC-modern-movement.md §80): one texture made from a base picture and its layers on
 * top, each tinted by its colour (26.3's `DyeColor.getTextureDiffuseColor`), as 26.3 draws the layers one over another.
 * A layer of 26.3's is its own shading times the colour, at its own alpha; one another mod added is a 1.8 mask (its red
 * is the alpha, the shading the base picture's). A banner's canvas is made once and kept a minute while it's drawn.
 */
@SideOnly(Side.CLIENT)
public class BannerCanvas extends AbstractTexture {

	/** 26.3's banner (its pole, bar and plain flag), under the layers. */
	public static final ResourceLocation BANNER_BASE = new ResourceLocation(McFly.DOMAIN, "textures/entity/banner/banner_base.png");

	/** A layer: its picture, its colour (RGB), and whether it's shaded by itself (26.3's) or by the base (a 1.8 mask). */
	public static final class Part {

		final String texture;
		final int colour;
		final boolean ownShading;

		public Part(String texture, int colour, boolean ownShading) {
			this.texture = texture;
			this.colour = colour;
			this.ownShading = ownShading;
		}
	}

	private static final Map<String, Timed> BANNERS = new LinkedHashMap<>();

	private static final class Timed {

		ResourceLocation location;
		long used;
	}

	private final ResourceLocation base;
	private final List<Part> parts;

	public BannerCanvas(ResourceLocation base, List<Part> parts) {
		this.base = base;
		this.parts = parts;
	}

	@Override
	public void loadTexture(IResourceManager resources) throws IOException {
		deleteGlTexture();
		TextureUtil.uploadTextureImage(getGlTextureId(), compose(resources, base, parts));
	}

	/** The canvas as a picture: the base, then each layer over it. A layer whose picture isn't the base's size is left off. */
	public static BufferedImage compose(IResourceManager resources, ResourceLocation base, List<Part> parts) throws IOException {
		BufferedImage under = read(resources, base);
		// always ARGB, whatever the base is (a palette base would round every colour to its palette)
		BufferedImage canvas = argb(under);
		Graphics2D graphics = canvas.createGraphics();
		for (Part part : parts) {
			BufferedImage layer = argb(read(resources, new ResourceLocation(part.texture)));
			if (layer.getWidth() != canvas.getWidth() || layer.getHeight() != canvas.getHeight()) {
				continue;
			}
			for (int y = 0; y < layer.getHeight(); y++) {
				for (int x = 0; x < layer.getWidth(); x++) {
					int pixel = layer.getRGB(x, y);
					if ((pixel >>> 24) == 0) {
						continue;
					}
					if (part.ownShading) {
						layer.setRGB(x, y, pixel & 0xFF000000 | multiply(pixel, part.colour));
					} else {
						int alpha = (pixel >> 16 & 0xFF) << 24;
						layer.setRGB(x, y, alpha | multiply(under.getRGB(x, y), part.colour));
					}
				}
			}
			graphics.drawImage(layer, 0, 0, null);
		}
		graphics.dispose();
		return canvas;
	}

	/** Each channel times the colour's, out of 255. */
	private static int multiply(int pixel, int colour) {
		int red = (int) ((pixel >> 16 & 0xFF) * (float) (colour >> 16 & 0xFF) / 255.0F);
		int green = (int) ((pixel >> 8 & 0xFF) * (float) (colour >> 8 & 0xFF) / 255.0F);
		int blue = (int) ((pixel & 0xFF) * (float) (colour & 0xFF) / 255.0F);
		return red << 16 | green << 8 | blue;
	}

	private static BufferedImage argb(BufferedImage image) {
		BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = copy.createGraphics();
		graphics.drawImage(image, 0, 0, null);
		graphics.dispose();
		return copy;
	}

	private static BufferedImage read(IResourceManager resources, ResourceLocation location) throws IOException {
		try (InputStream in = resources.getResource(location).getInputStream()) {
			return ImageIO.read(in);
		}
	}

	// ---------------------------------------------------------------- a banner's

	/** A banner's layers as parts: its base colour as 26.3's `base` pattern, then its layers. */
	public static List<Part> partsOf(BannerBlockEntity banner) {
		List<Part> parts = new ArrayList<>();
		parts.add(new Part(BannerPatterns.BASE.texture(), banner.baseColour().textureColour(), true));
		for (Banners.Layer layer : banner.layers()) {
			parts.add(new Part(layer.pattern().texture(), layer.colour().textureColour(), layer.pattern().isModern()));
		}
		return parts;
	}

	/** Each layer's picture, the base colour's first (the tests read it). */
	public static List<String> layerTextures(BannerBlockEntity banner) {
		List<String> textures = new ArrayList<>();
		for (Part part : partsOf(banner)) {
			textures.add(part.texture);
		}
		return textures;
	}

	/** A banner's canvas as a picture, as its texture is made (the tests read it). */
	public static BufferedImage imageOf(IResourceManager resources, BannerBlockEntity banner) throws IOException {
		return compose(resources, BANNER_BASE, partsOf(banner));
	}

	/** A banner's canvas texture, made the first time and kept while used (up to 256, each a minute after its last use). */
	public static ResourceLocation of(BannerBlockEntity banner) {
		String key = banner.canvasKey();
		Timed timed = BANNERS.get(key);
		if (timed == null) {
			if (BANNERS.size() >= 256) {
				long now = System.currentTimeMillis();
				for (Iterator<Timed> it = BANNERS.values().iterator(); it.hasNext(); ) {
					Timed old = it.next();
					if (now - old.used > 60000L) {
						Minecraft.getMinecraft().getTextureManager().deleteTexture(old.location);
						it.remove();
					}
				}
				if (BANNERS.size() >= 256) {
					return null;
				}
			}
			timed = new Timed();
			timed.location = new ResourceLocation(McFly.DOMAIN, "canvas/banner/" + key);
			Minecraft.getMinecraft().getTextureManager().loadTexture(timed.location, new BannerCanvas(BANNER_BASE, partsOf(banner)));
			BANNERS.put(key, timed);
		}
		timed.used = System.currentTimeMillis();
		return timed.location;
	}
}
