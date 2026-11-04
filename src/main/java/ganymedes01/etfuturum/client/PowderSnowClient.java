package ganymedes01.etfuturum.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.entities.Freezing;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

/**
 * Powder snow on the client (SPEC-modern-movement.md §62.5): the frost overlay (26.3 {@code Hud}, drawn in every
 * view, after the pumpkin's) and the fog with the camera in powder snow (26.3 {@code Camera.getFluidInCamera},
 * {@code PowderedSnowFogEnvironment}).
 */
public class PowderSnowClient {

	public static final PowderSnowClient INSTANCE = new PowderSnowClient();

	private static final ResourceLocation OUTLINE = new ResourceLocation("textures/misc/powder_snow_outline.png");

	/** 26.3's {@code -6308916}: (159, 187, 204). */
	private static final float FOG_RED = 159 / 255.0F;
	private static final float FOG_GREEN = 187 / 255.0F;
	private static final float FOG_BLUE = 204 / 255.0F;

	@SubscribeEvent
	public void onOverlay(RenderGameOverlayEvent.Post event) {
		if (event.type != RenderGameOverlayEvent.ElementType.HELMET) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.thePlayer == null || Freezing.getTicksFrozen(mc.thePlayer) <= 0) {
			return;
		}
		int width = event.resolution.getScaledWidth();
		int height = event.resolution.getScaledHeight();
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_BLEND);
		OpenGlHelper.glBlendFunc(770, 771, 1, 0);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, Freezing.getPercentFrozen(mc.thePlayer));
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		mc.getTextureManager().bindTexture(OUTLINE);
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(0.0D, height, -90.0D, 0.0D, 1.0D);
		tessellator.addVertexWithUV(width, height, -90.0D, 1.0D, 1.0D);
		tessellator.addVertexWithUV(width, 0.0D, -90.0D, 1.0D, 0.0D);
		tessellator.addVertexWithUV(0.0D, 0.0D, -90.0D, 0.0D, 0.0D);
		tessellator.draw();
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@SubscribeEvent
	public void onFogColours(EntityViewRenderEvent.FogColors event) {
		if (cameraInPowderSnow(event.entity, (float) event.renderPartialTicks)) {
			event.red = FOG_RED;
			event.green = FOG_GREEN;
			event.blue = FOG_BLUE;
		}
	}

	/** 26.3: from 0 to 2 blocks, sky and clouds too. */
	@SubscribeEvent
	public void onFog(EntityViewRenderEvent.RenderFogEvent event) {
		if (cameraInPowderSnow(event.entity, (float) event.renderPartialTicks)) {
			GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
			GL11.glFogf(GL11.GL_FOG_START, 0.0F);
			GL11.glFogf(GL11.GL_FOG_END, 2.0F);
		}
	}

	/**
	 * 26.3 {@code getFluidInCamera}: water at the camera first; then the near plane's centre and four corners (z near
	 * 0.05, the FOV setting, the window's aspect), any in powder snow.
	 */
	public static boolean cameraInPowderSnow(Entity viewer, float partialTicks) {
		Minecraft mc = Minecraft.getMinecraft();
		if (!(viewer instanceof EntityLivingBase) || mc.theWorld == null) {
			return false;
		}
		World world = mc.theWorld;
		Block atCamera = ActiveRenderInfo.getBlockAtEntityViewpoint(world, (EntityLivingBase) viewer, partialTicks);
		if (atCamera.getMaterial() == Material.water || atCamera.getMaterial() == Material.lava) {
			return false;
		}
		Vec3 camera = ActiveRenderInfo.projectViewFromEntity((EntityLivingBase) viewer, partialTicks);
		float yaw = viewer.prevRotationYaw + (viewer.rotationYaw - viewer.prevRotationYaw) * partialTicks;
		float pitch = viewer.prevRotationPitch + (viewer.rotationPitch - viewer.prevRotationPitch) * partialTicks;
		if (mc.gameSettings.thirdPersonView == 2) {
			yaw += 180.0F;
			pitch = -pitch;
		}
		double yawR = Math.toRadians(yaw), pitchR = Math.toRadians(pitch);
		double fx = -Math.sin(yawR) * Math.cos(pitchR), fy = -Math.sin(pitchR), fz = Math.cos(yawR) * Math.cos(pitchR);
		// a level left vector, and up = left x forward (unit length; the sign doesn't matter, both corners are tried)
		double lx = Math.cos(yawR), lz = Math.sin(yawR);
		double ux = Math.sin(yawR) * Math.sin(pitchR), uy = -Math.cos(pitchR), uz = -Math.cos(yawR) * Math.sin(pitchR);
		double zNear = 0.05D;
		double planeHeight = Math.tan(Math.toRadians(mc.gameSettings.fovSetting) / 2.0D) * zNear;
		double planeWidth = planeHeight * mc.displayWidth / Math.max(1, mc.displayHeight);
		double cx = camera.xCoord + fx * zNear, cy = camera.yCoord + fy * zNear, cz = camera.zCoord + fz * zNear;
		for (int point = 0; point < 5; point++) {
			double side = point == 0 ? 0.0D : point % 2 == 1 ? 1.0D : -1.0D;
			double vertical = point == 0 ? 0.0D : point <= 2 ? 1.0D : -1.0D;
			double x = cx + lx * planeWidth * side + ux * planeHeight * vertical;
			double y = cy + uy * planeHeight * vertical;
			double z = cz + lz * planeWidth * side + uz * planeHeight * vertical;
			Block block = world.getBlock(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
			if (Freezing.isPowderSnow(block)) {
				return true;
			}
		}
		return false;
	}
}
