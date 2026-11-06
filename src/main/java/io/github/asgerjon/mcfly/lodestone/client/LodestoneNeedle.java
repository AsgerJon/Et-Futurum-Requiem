package io.github.asgerjon.mcfly.lodestone.client;

import io.github.asgerjon.mcfly.McFly;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.lodestone.LodestoneCompass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * The lodestone compass's needle (SPEC-modern-movement.md §57), from 26.3's CompassAngleState and
 * NeedleDirectionHelper: which of the compass's 32 frames a lodestone compass draws, by whoever holds or shows it.
 * The frames are cut from the game's own compass texture, so it looks like the compass under any resource pack.
 */
@SideOnly(Side.CLIENT)
public final class LodestoneNeedle {

	public static final LodestoneNeedle INSTANCE = new LodestoneNeedle();

	private static final int FRAMES = 32;
	private static final NeedleSprite[] SPRITES = new NeedleSprite[FRAMES];
	/** The two smoothed values of 26.3's compass model, shared by every lodestone compass, as there. */
	private static final Wobbler POINTING = new Wobbler(), SPINNING = new Wobbler();
	private static final Random RANDOM = new Random();

	/** Who shows the compass being drawn, while a renderer says so (an item frame, a dropped item). */
	private static Entity shownBy;
	/** Where in a screen the item being drawn is: a spinning needle's offset there. */
	private static int screenSeed;

	private LodestoneNeedle() {
	}

	@SubscribeEvent
	public void registerSprites(TextureStitchEvent.Pre event) {
		if (event.map.getTextureType() == 1) {
			for (int frame = 0; frame < FRAMES; frame++) {
				SPRITES[frame] = new NeedleSprite(frame);
				event.map.setTextureEntry(SPRITES[frame].getIconName(), SPRITES[frame]);
			}
		}
	}

	/** The icon the stack draws with when the owner (an item frame, a dropped item, a living entity) shows it. */
	public static IIcon icon(ItemStack stack, Entity owner, int seed) {
		NeedleSprite sprite = SPRITES[frame(stack, owner, seed)];
		return sprite != null ? sprite : Items.compass.getIconFromDamage(0);
	}

	/** For Item.getIconIndex: shown by whoever a renderer named, otherwise by the player (a screen, a tooltip). */
	public static IIcon icon(ItemStack stack) {
		Entity owner = shownBy;
		return owner != null ? icon(stack, owner, owner.getEntityId()) : icon(stack, Minecraft.getMinecraft().thePlayer, screenSeed);
	}

	/** Which needle frame the icon is, 0 to 31 (16 points straight up), or -1 when it isn't one. */
	public static int frameOf(IIcon icon) {
		for (int frame = 0; frame < FRAMES; frame++) {
			if (icon != null && icon == SPRITES[frame]) {
				return frame;
			}
		}
		return -1;
	}

	/** A renderer starts drawing what the entity shows; false when someone else already is (the frame's own item). */
	public static boolean startShowing(Entity entity) {
		if (shownBy != null) {
			return false;
		}
		shownBy = entity;
		return true;
	}

	public static void stopShowing(Entity entity) {
		if (shownBy == entity) {
			shownBy = null;
		}
	}

	/** An item drawn in a screen at x, y. */
	public static void inScreenAt(int x, int y) {
		screenSeed = 31 * x + y;
	}

	/** 26.3's frame: the value from 0 to 1 times 32, 0 being frame 16, straight up. */
	private static int frame(ItemStack stack, Entity owner, int seed) {
		if (owner == null || owner.worldObj == null) {
			return 16;
		}
		long time = owner.worldObj.getTotalWorldTime();
		float value;
		NBTTagCompound tag = stack.getTagCompound();
		NBTTagCompound pos = tag != null && tag.hasKey(LodestoneCompass.POS) ? tag.getCompoundTag(LodestoneCompass.POS) : null;
		double targetX = pos == null ? 0.0D : pos.getInteger("X") + 0.5D;
		double targetY = pos == null ? 0.0D : pos.getInteger("Y") + 0.5D;
		double targetZ = pos == null ? 0.0D : pos.getInteger("Z") + 0.5D;
		boolean valid = pos != null && tag.getInteger(LodestoneCompass.DIMENSION) == owner.worldObj.provider.dimensionId
			&& !(square(targetX - owner.posX) + square(targetY - owner.posY) + square(targetZ - owner.posZ) < 1.0E-5F);
		if (!valid) {
			if (SPINNING.time != time) {
				SPINNING.update(time, RANDOM.nextFloat());
			}
			value = SPINNING.rotation + seed * 1327217883 / 2.1474836E9F;
		} else {
			float toTarget = (float) (Math.atan2(targetZ - owner.posZ, targetX - owner.posX) / (Math.PI * 2.0D));
			float facing = positiveModulo(visualYaw(owner) / 360.0F, 1.0F);
			if (owner == Minecraft.getMinecraft().thePlayer) {
				if (POINTING.time != time) {
					POINTING.update(time, 0.5F - (facing - 0.25F));
				}
				value = toTarget + POINTING.rotation;
			} else {
				value = 0.5F - (facing - 0.25F - toTarget);
			}
		}
		return ((int) Math.floor(positiveModulo(value, 1.0F) * FRAMES + 0.5F) + FRAMES / 2) % FRAMES;
	}

	/** 26.3's ItemOwner.getVisualRotationYInDegrees for each kind of owner. */
	private static float visualYaw(Entity owner) {
		if (owner instanceof EntityItemFrame frame) {
			// 26.3's, on walls, floors and ceilings, in 45 degree turns (spec §67.3); 1.7.10's quarter turns without them
			return ganymedes01.etfuturum.entities.ModernFrames.visualYaw(frame);
		}
		if (owner instanceof EntityItem item) {
			// the turn it's drawn with
			return 180.0F - (float) Math.toDegrees((item.age + 0.5F) / 20.0F + item.hoverStart);
		}
		if (owner instanceof EntityLivingBase living && owner != Minecraft.getMinecraft().thePlayer) {
			return living.renderYawOffset;
		}
		return owner.rotationYaw;
	}

	private static double square(double d) {
		return d * d;
	}

	private static float positiveModulo(float input, float mod) {
		return (input % mod + mod) % mod;
	}

	/** 26.3's NeedleDirectionHelper.standardWobbler(0.8): eases toward its target, once per game tick. */
	private static final class Wobbler {
		private float rotation;
		private float delta;
		private long time = Long.MIN_VALUE;

		private void update(long tick, float target) {
			time = tick;
			float step = positiveModulo(target - rotation + 0.5F, 1.0F) - 0.5F;
			delta += step * 0.1F;
			delta *= 0.8F;
			rotation = positiveModulo(rotation + delta, 1.0F);
		}
	}

	/** One frame of the game's own compass texture, read through the resource packs. */
	private static final class NeedleSprite extends TextureAtlasSprite {

		private static final ResourceLocation COMPASS = new ResourceLocation("textures/items/compass.png");

		private final int frame;

		private NeedleSprite(int frame) {
			super(McFly.DOMAIN + ":lodestone_compass_needle_" + (frame < 10 ? "0" : "") + frame);
			this.frame = frame;
		}

		@Override
		public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
			return true;
		}

		/** False when loaded, as TextureMap wants it. A pack's strip with another frame count is sampled at 32 places. */
		@Override
		public boolean load(IResourceManager manager, ResourceLocation location) {
			try (InputStream in = manager.getResource(COMPASS).getInputStream()) {
				BufferedImage strip = ImageIO.read(in);
				int size = strip.getWidth();
				int frames = Math.max(1, strip.getHeight() / size);
				int from = Math.round(frame * frames / (float) FRAMES) % frames;
				BufferedImage[] images = new BufferedImage[1 + Minecraft.getMinecraft().gameSettings.mipmapLevels];
				images[0] = strip.getSubimage(0, from * size, size, size);
				loadSprite(images, null, Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1);
				return false;
			} catch (IOException | RuntimeException e) {
				McFly.LOG.error("Couldn't cut the lodestone compass's needle " + frame + " from the compass texture: " + e);
				return true;
			}
		}
	}
}
