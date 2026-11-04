package ganymedes01.etfuturum.client.renderer.tileentity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import ganymedes01.etfuturum.client.OpenGLHelper;
import ganymedes01.etfuturum.client.model.ModelHead;
import ganymedes01.etfuturum.core.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Map;

public class TileEntityFancySkullRenderer extends TileEntitySpecialRenderer {

	private static final ResourceLocation skeleton_texture = Utils.getResource("textures/entity/skeleton/skeleton.png");
	private static final ResourceLocation wither_skeleton_texture = Utils.getResource("textures/entity/skeleton/wither_skeleton.png");
	private static final ResourceLocation zombie_texture = Utils.getResource("textures/entity/zombie/zombie.png");
	private static final ResourceLocation creeper_texture = Utils.getResource("textures/entity/creeper/creeper.png");

	public static TileEntityFancySkullRenderer instance;

	private final ModelHead model1 = new ModelHead(32);
	private final ModelHead model2 = new ModelHead(64);

	@Override
	public void func_147497_a(TileEntityRendererDispatcher dispatcher) {
		super.func_147497_a(dispatcher);
		instance = this;
	}

	private static final ResourceLocation dragon_texture = new ResourceLocation("textures/entity/enderdragon/dragon.png");
	private final ganymedes01.etfuturum.client.model.ModelDragonHead dragonModel = new ganymedes01.etfuturum.client.model.ModelDragonHead();
	/** The dragon head's jaw animation for the head being drawn (26.3's SkullBlockEntity.getAnimation; 0 for items). */
	private float dragonAnimation;

	public void renderSkull(float x, float y, float z, int meta, float rotation, int type, GameProfile profile) {
		if (type == 5) {
			renderDragonHead(x, y, z, meta, rotation, dragonAnimation);
			return;
		}
		ModelHead model = model1;

		switch (type) {
			case 0:
			default:
				bindTexture(skeleton_texture);
				break;
			case 1:
				bindTexture(wither_skeleton_texture);
				break;
			case 2:
				bindTexture(zombie_texture);
				model = model2;
				break;
			case 3:
				ResourceLocation texture = AbstractClientPlayer.locationStevePng;
				if (profile != null) {
					Minecraft minecraft = Minecraft.getMinecraft();
					Map<Type, MinecraftProfileTexture> map = minecraft.func_152342_ad/*getSkinManager*/().func_152788_a/*loadSkinFromCache*/(profile);
					if (map.containsKey(Type.SKIN))
						texture = minecraft.func_152342_ad/*getSkinManager*/().func_152792_a/*loadSkin*/(map.get(Type.SKIN), Type.SKIN);
				}
				bindTexture(texture);
				break;
			case 4:
				bindTexture(creeper_texture);
		}

		OpenGLHelper.pushMatrix();
		OpenGLHelper.disableCull();

		if (meta != 1)
			switch (meta) {
				case 2:
					OpenGLHelper.translate(x + 0.5F, y + 0.25F, z + 0.74F);
					break;
				case 3:
					OpenGLHelper.translate(x + 0.5F, y + 0.25F, z + 0.26F);
					rotation = 180.0F;
					break;
				case 4:
					OpenGLHelper.translate(x + 0.74F, y + 0.25F, z + 0.5F);
					rotation = 270.0F;
					break;
				case 5:
				default:
					OpenGLHelper.translate(x + 0.26F, y + 0.25F, z + 0.5F);
					rotation = 90.0F;
			}
		else
			GL11.glTranslatef(x + 0.5F, y, z + 0.5F);

		OpenGLHelper.enableRescaleNormal();
		OpenGLHelper.scale(-1.0F, -1.0F, 1.0F);
		OpenGLHelper.enableAlpha();
		model.render(rotation);
		OpenGLHelper.popMatrix();
	}

	/**
	 * 26.3's dragon head (SPEC-modern-movement.md §76.12, 7d): on a wall a quarter block out from it, on the floor at
	 * the middle, turned as a skull turns; 26.3's model and the dragon's texture.
	 */
	private void renderDragonHead(float x, float y, float z, int meta, float rotation, float animation) {
		bindTexture(dragon_texture);
		OpenGLHelper.pushMatrix();
		OpenGLHelper.disableCull();
		switch (meta) {
			case 1 -> GL11.glTranslatef(x + 0.5F, y, z + 0.5F);
			case 2 -> GL11.glTranslatef(x + 0.5F, y + 0.25F, z + 0.75F);
			case 3 -> {
				GL11.glTranslatef(x + 0.5F, y + 0.25F, z + 0.25F);
				rotation = 180.0F;
			}
			case 4 -> {
				GL11.glTranslatef(x + 0.75F, y + 0.25F, z + 0.5F);
				rotation = 270.0F;
			}
			default -> {
				GL11.glTranslatef(x + 0.25F, y + 0.25F, z + 0.5F);
				rotation = 90.0F;
			}
		}
		OpenGLHelper.enableRescaleNormal();
		OpenGLHelper.scale(-1.0F, -1.0F, 1.0F);
		OpenGLHelper.enableAlpha();
		dragonModel.render(rotation, animation);
		OpenGLHelper.popMatrix();
	}

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTick) {
		TileEntitySkull skull = (TileEntitySkull) tile;
		dragonAnimation = skull instanceof ganymedes01.etfuturum.ducks.IDragonHead head ? head.etfu$animation(partialTick) : 0.0F;
		renderSkull((float) x, (float) y, (float) z, tile.getBlockMetadata() & 7, skull.func_145906_b()/*getSkullRotation*/ * 360 / 16.0F, skull.func_145904_a()/*getSkullType*/, skull.func_152108_a());
		dragonAnimation = 0.0F;
	}
}