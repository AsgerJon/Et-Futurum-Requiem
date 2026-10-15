package ganymedes01.etfuturum.client.gui;

import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * The modern air bubble row: bubbles burst one by one with a pop that gets louder and higher as they run out, empty
 * outlines show the air already used (and refill after surfacing), and the empty row wobbles once the air is gone.
 * Replaces Forge's air element.
 */
public final class ModernAirBubbles {

	private static final ResourceLocation AIR = new ResourceLocation("etfuturum", "textures/gui/hud/air.png");
	private static final ResourceLocation AIR_BURSTING = new ResourceLocation("etfuturum", "textures/gui/hud/air_bursting.png");
	private static final ResourceLocation AIR_EMPTY = new ResourceLocation("etfuturum", "textures/gui/hud/air_empty.png");
	private static final String POP_SOUND = Reference.MCAssetVer + ":block.bubble_column.bubble_pop";
	private static final int BUBBLES = 10;
	private static final int MAX_AIR = 300;

	private static final Random random = new Random();
	private static int lastPoppedBubble;

	private ModernAirBubbles() {
	}

	public static void render(Minecraft mc, int screenWidth, int screenHeight) {
		EntityPlayer player = mc.thePlayer;
		int air = MathHelper.clamp_int(player.getAir(), 0, MAX_AIR);
		boolean underWater = player.isInsideOfMaterial(Material.water);
		if (!underWater) {
			lastPoppedBubble = 0;
		}
		if (!underWater && air >= MAX_AIR) {
			return;
		}

		int right = screenWidth / 2 + 91;
		int top = screenHeight - GuiIngameForge.right_height;
		// A bubble shows as full until its last 2 ticks of air, bursts for those 2 ticks, and turns into an empty
		// outline one tick after it's gone (no delay once out of air or out of water).
		int fullBubbles = bubblesFor(air, -2);
		int burstingBubble = bubblesFor(air, 0);
		int emptyBubbles = BUBBLES - bubblesFor(air, air != 0 && underWater ? 1 : 0);
		boolean bursting = fullBubbles != burstingBubble;

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		for (int bubble = 1; bubble <= BUBBLES; bubble++) {
			int x = right - (bubble - 1) * 8 - 9;
			if (bubble <= fullBubbles) {
				draw(mc, AIR, x, top);
			} else if (bursting && bubble == burstingBubble && underWater) {
				draw(mc, AIR_BURSTING, x, top);
				playPop(player, bubble, emptyBubbles);
			} else if (bubble > BUBBLES - emptyBubbles) {
				int wobble = emptyBubbles == BUBBLES && mc.ingameGUI.getUpdateCounter() % 2 == 0 ? random.nextInt(2) : 0;
				draw(mc, AIR_EMPTY, x, top + wobble);
			}
		}
		GL11.glDisable(GL11.GL_BLEND);
		mc.getTextureManager().bindTexture(Gui.icons);
		GuiIngameForge.right_height += 10;
	}

	private static int bubblesFor(int air, int tickOffset) {
		return MathHelper.ceiling_float_int((float) ((air + tickOffset) * BUBBLES) / MAX_AIR);
	}

	/**
	 * One pop per bubble. Louder from the 3rd empty bubble on and higher from the 5th, by 0.1 per bubble.
	 */
	private static void playPop(EntityPlayer player, int bubble, int emptyBubbles) {
		if (lastPoppedBubble != bubble) {
			float volume = 0.5F + 0.1F * Math.max(0, emptyBubbles - 2);
			float pitch = 1.0F + 0.1F * Math.max(0, emptyBubbles - 4);
			player.playSound(POP_SOUND, volume, pitch);
			lastPoppedBubble = bubble;
		}
	}

	private static void draw(Minecraft mc, ResourceLocation texture, int x, int y) {
		mc.getTextureManager().bindTexture(texture);
		Gui.func_146110_a(x, y, 0.0F, 0.0F, 9, 9, 9.0F, 9.0F);
	}
}
