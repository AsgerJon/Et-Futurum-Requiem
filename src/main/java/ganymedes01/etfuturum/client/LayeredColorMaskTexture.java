package ganymedes01.etfuturum.client;

import ganymedes01.etfuturum.lib.EnumColor;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;


import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class LayeredColorMaskTexture extends AbstractTexture {

	/**
	 * Access to the Logger, for all your logging needs.
	 */
	private static final Logger LOGGER = LogManager.getLogger();
	/**
	 * The location of the texture.
	 */
	private final ResourceLocation textureLocation;
	private final List<String> listTextures;
	private final List<EnumColor> listDyeColors;

	/** By layer: 26.3's mixing (its own texture's shading times the dye, by its alpha), or 1.8's (spec §66.9, §66.11). */
	private final List<Boolean> ownShading;

	public LayeredColorMaskTexture(ResourceLocation textureLocationIn, List<String> p_i46101_2_, List<EnumColor> p_i46101_3_) {
		this(textureLocationIn, p_i46101_2_, p_i46101_3_, false);
	}

	public LayeredColorMaskTexture(ResourceLocation textureLocationIn, List<String> layers, List<EnumColor> colours, boolean ownShading) {
		this(textureLocationIn, layers, colours, java.util.Collections.nCopies(layers.size(), ownShading));
	}

	/** Each layer by its own rule (a banner: 26.3's patterns by 26.3's, one another mod added by 1.8's, spec §66.11). */
	public LayeredColorMaskTexture(ResourceLocation textureLocationIn, List<String> layers, List<EnumColor> colours, List<Boolean> ownShading) {
		textureLocation = textureLocationIn;
		listTextures = layers;
		listDyeColors = colours;
		this.ownShading = ownShading;
	}

	@Override
	public void loadTexture(IResourceManager resourceManager) throws IOException {
		deleteGlTexture();
		BufferedImage bufferedimage;
		try {
			bufferedimage = compose(resourceManager, textureLocation, listTextures, listDyeColors, ownShading);
		} catch (IOException ioexception) {
			LOGGER.error("Couldn't load layered image", ioexception);
			return;
		}
		TextureUtil.uploadTextureImage(getGlTextureId(), bufferedimage);
	}

	/**
	 * A banner's canvas: the base texture, then each layer's mask (its red as alpha) in the base's shading times the
	 * layer's colour (the loom's preview and tests read it too, spec §66.8).
	 */
	public static BufferedImage compose(IResourceManager resourceManager, ResourceLocation textureLocation, List<String> listTextures,
										List<EnumColor> listDyeColors) throws IOException {
		return compose(resourceManager, textureLocation, listTextures, listDyeColors, false);
	}

	/**
	 * With ownShading, 26.3's way (its shield patterns): each layer pixel is that pixel's own shading times the dye, at its
	 * alpha. Else 1.8's (EFR's banner masks): the mask's red is the alpha, the shading the base texture's.
	 */
	public static BufferedImage compose(IResourceManager resourceManager, ResourceLocation textureLocation, List<String> listTextures,
										List<EnumColor> listDyeColors, boolean ownShading) throws IOException {
		return compose(resourceManager, textureLocation, listTextures, listDyeColors, java.util.Collections.nCopies(listTextures.size(), ownShading));
	}

	/** Each layer by its own rule (true: 26.3's, false: 1.8's). */
	public static BufferedImage compose(IResourceManager resourceManager, ResourceLocation textureLocation, List<String> listTextures,
										List<EnumColor> listDyeColors, List<Boolean> ownShading) throws IOException {
		BufferedImage bufferedimage1 = readBufferedImage(resourceManager.getResource(textureLocation).getInputStream());
		// always RGBA: a canvas of the base's own type rounds every colour to its palette when the base is a palette PNG
		// (26.3's shield frame is: its blue came out 333399, spec §66.9)
		BufferedImage bufferedimage = new BufferedImage(bufferedimage1.getWidth(), bufferedimage1.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics graphics = bufferedimage.getGraphics();
		graphics.drawImage(bufferedimage1, 0, 0, null);
		for (int j = 0; j < listTextures.size() && j < listDyeColors.size(); ++j) {
			String s = listTextures.get(j);
			int colour = colourOf(listDyeColors.get(j));
			if (s != null) {
				InputStream inputstream = resourceManager.getResource(new ResourceLocation(s)).getInputStream();
				BufferedImage bufferedimage2 = readBufferedImage(inputstream);
				if (bufferedimage2.getWidth() == bufferedimage.getWidth() && bufferedimage2.getHeight() == bufferedimage.getHeight() && bufferedimage2.getType() == 6) {
					for (int k = 0; k < bufferedimage2.getHeight(); ++k)
						for (int l = 0; l < bufferedimage2.getWidth(); ++l) {
							int i1 = bufferedimage2.getRGB(l, k);
							if ((i1 & -16777216) != 0) {
								if (ownShading.get(j)) {
									bufferedimage2.setRGB(l, k, i1 & -16777216 | multiplyColor(i1, colour) & 16777215);
								} else {
									int j1 = (i1 & 16711680) << 8 & -16777216;
									int k1 = bufferedimage1.getRGB(l, k);
									int l1 = multiplyColor(k1, colour) & 16777215;
									bufferedimage2.setRGB(l, k, j1 | l1);
								}
							}
						}
					bufferedimage.getGraphics().drawImage(bufferedimage2, 0, 0, null);
				}
				inputstream.close();
			}
		}
		return bufferedimage;
	}

	/** A layer's colour: 26.3's dye tint (spec §66.8), not 1.7.10's map colour. */
	private static int colourOf(EnumColor colour) {
		return ganymedes01.etfuturum.blocks.ModernBanners.textureColour(colour);
	}

	private static int multiplyColor(int p_180188_0_, int p_180188_1_) {
		int k = (p_180188_0_ & 16711680) >> 16;
		int l = (p_180188_1_ & 16711680) >> 16;
		int i1 = (p_180188_0_ & 65280) >> 8;
		int j1 = (p_180188_1_ & 65280) >> 8;
		int k1 = (p_180188_0_ & 255) >> 0;
		int l1 = (p_180188_1_ & 255) >> 0;
		int i2 = (int) ((float) k * (float) l / 255.0F);
		int j2 = (int) ((float) i1 * (float) j1 / 255.0F);
		int k2 = (int) ((float) k1 * (float) l1 / 255.0F);
		return p_180188_0_ & -16777216 | i2 << 16 | j2 << 8 | k2;
	}

	private static BufferedImage readBufferedImage(InputStream imageStream) throws IOException {
		BufferedImage bufferedimage;

		try {
			bufferedimage = ImageIO.read(imageStream);
		} finally {
			IOUtils.closeQuietly(imageStream);
		}

		return bufferedimage;
	}
}