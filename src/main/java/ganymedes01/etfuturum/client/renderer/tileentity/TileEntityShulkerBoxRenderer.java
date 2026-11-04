package ganymedes01.etfuturum.client.renderer.tileentity;

import com.google.common.primitives.SignedBytes;
import ganymedes01.etfuturum.client.model.ModelShulker;
import ganymedes01.etfuturum.tileentities.TileEntityShulkerBox;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public class TileEntityShulkerBoxRenderer extends TileEntitySpecialRenderer {

	public int tier = 0;
	private final Random random;
	private final RenderItem itemRenderer;
	private static final float[][] shifts = {{0.3F, 0.45F, 0.3F}, {0.7F, 0.45F, 0.3F}, {0.3F, 0.45F, 0.7F}, {0.7F, 0.45F, 0.7F}, {0.3F, 0.1F, 0.3F},
			{0.7F, 0.1F, 0.3F}, {0.3F, 0.1F, 0.7F}, {0.7F, 0.1F, 0.7F}, {0.5F, 0.32F, 0.5F},};
	protected static final ResourceLocation[] DESTROY_STAGES = new ResourceLocation[]{
			new ResourceLocation("textures/blocks/destroy_stage_0.png"), new ResourceLocation("textures/blocks/destroy_stage_1.png"),
			new ResourceLocation("textures/blocks/destroy_stage_2.png"), new ResourceLocation("textures/blocks/destroy_stage_3.png"),
			new ResourceLocation("textures/blocks/destroy_stage_4.png"), new ResourceLocation("textures/blocks/destroy_stage_5.png"),
			new ResourceLocation("textures/blocks/destroy_stage_6.png"), new ResourceLocation("textures/blocks/destroy_stage_7.png"),
			new ResourceLocation("textures/blocks/destroy_stage_8.png"), new ResourceLocation("textures/blocks/destroy_stage_9.png")};
	/** The box's textures: 26.3's shulker ones (none, then the 16 colours), then the iron shulker boxes' tiers, 17 each. */
	public static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
			new ResourceLocation("textures/entity/shulker/shulker.png"),
			new ResourceLocation("textures/entity/shulker/shulker_white.png"), new ResourceLocation("textures/entity/shulker/shulker_orange.png"),
			new ResourceLocation("textures/entity/shulker/shulker_magenta.png"), new ResourceLocation("textures/entity/shulker/shulker_light_blue.png"),
			new ResourceLocation("textures/entity/shulker/shulker_yellow.png"), new ResourceLocation("textures/entity/shulker/shulker_lime.png"),
			new ResourceLocation("textures/entity/shulker/shulker_pink.png"), new ResourceLocation("textures/entity/shulker/shulker_gray.png"),
			new ResourceLocation("textures/entity/shulker/shulker_light_gray.png"), new ResourceLocation("textures/entity/shulker/shulker_cyan.png"),
			new ResourceLocation("textures/entity/shulker/shulker_purple.png"), new ResourceLocation("textures/entity/shulker/shulker_blue.png"),
			new ResourceLocation("textures/entity/shulker/shulker_brown.png"), new ResourceLocation("textures/entity/shulker/shulker_green.png"),
			new ResourceLocation("textures/entity/shulker/shulker_red.png"), new ResourceLocation("textures/entity/shulker/shulker_black.png"),

			new ResourceLocation("ironshulkerbox:textures/model/vanilla/shulker_iron.png"),
			new ResourceLocation("ironshulkerbox:textures/model/white/shulker_white_iron.png"), new ResourceLocation("ironshulkerbox:textures/model/orange/shulker_orange_iron.png"),
			new ResourceLocation("ironshulkerbox:textures/model/magenta/shulker_magenta_iron.png"), new ResourceLocation("ironshulkerbox:textures/model/light_blue/shulker_light_blue_iron.png"),
			new ResourceLocation("ironshulkerbox:textures/model/yellow/shulker_yellow_iron.png"), new ResourceLocation("ironshulkerbox:textures/model/lime/shulker_lime_iron.png"),
			new ResourceLocation("ironshulkerbox:textures/model/pink/shulker_pink_iron.png"), new ResourceLocation("ironshulkerbox:textures/model/gray/shulker_gray_iron.png"),
			new ResourceLocation("ironshulkerbox:textures/model/light_gray/shulker_light_gray_iron.png"), new ResourceLocation("ironshulkerbox:textures/model/cyan/shulker_cyan_iron.png"),
			new ResourceLocation("ironshulkerbox:textures/model/purple/shulker_purple_iron.png"), new ResourceLocation("ironshulkerbox:textures/model/blue/shulker_blue_iron.png"),
			new ResourceLocation("ironshulkerbox:textures/model/brown/shulker_brown_iron.png"), new ResourceLocation("ironshulkerbox:textures/model/green/shulker_green_iron.png"),
			new ResourceLocation("ironshulkerbox:textures/model/red/shulker_red_iron.png"), new ResourceLocation("ironshulkerbox:textures/model/black/shulker_black_iron.png"),

			new ResourceLocation("ironshulkerbox:textures/model/vanilla/shulker_gold.png"),
			new ResourceLocation("ironshulkerbox:textures/model/white/shulker_white_gold.png"), new ResourceLocation("ironshulkerbox:textures/model/orange/shulker_orange_gold.png"),
			new ResourceLocation("ironshulkerbox:textures/model/magenta/shulker_magenta_gold.png"), new ResourceLocation("ironshulkerbox:textures/model/light_blue/shulker_light_blue_gold.png"),
			new ResourceLocation("ironshulkerbox:textures/model/yellow/shulker_yellow_gold.png"), new ResourceLocation("ironshulkerbox:textures/model/lime/shulker_lime_gold.png"),
			new ResourceLocation("ironshulkerbox:textures/model/pink/shulker_pink_gold.png"), new ResourceLocation("ironshulkerbox:textures/model/gray/shulker_gray_gold.png"),
			new ResourceLocation("ironshulkerbox:textures/model/light_gray/shulker_light_gray_gold.png"), new ResourceLocation("ironshulkerbox:textures/model/cyan/shulker_cyan_gold.png"),
			new ResourceLocation("ironshulkerbox:textures/model/purple/shulker_purple_gold.png"), new ResourceLocation("ironshulkerbox:textures/model/blue/shulker_blue_gold.png"),
			new ResourceLocation("ironshulkerbox:textures/model/brown/shulker_brown_gold.png"), new ResourceLocation("ironshulkerbox:textures/model/green/shulker_green_gold.png"),
			new ResourceLocation("ironshulkerbox:textures/model/red/shulker_red_gold.png"), new ResourceLocation("ironshulkerbox:textures/model/black/shulker_black_gold.png"),

			new ResourceLocation("ironshulkerbox:textures/model/vanilla/shulker_diamond.png"),
			new ResourceLocation("ironshulkerbox:textures/model/white/shulker_white_diamond.png"), new ResourceLocation("ironshulkerbox:textures/model/orange/shulker_orange_diamond.png"),
			new ResourceLocation("ironshulkerbox:textures/model/magenta/shulker_magenta_diamond.png"), new ResourceLocation("ironshulkerbox:textures/model/light_blue/shulker_light_blue_diamond.png"),
			new ResourceLocation("ironshulkerbox:textures/model/yellow/shulker_yellow_diamond.png"), new ResourceLocation("ironshulkerbox:textures/model/lime/shulker_lime_diamond.png"),
			new ResourceLocation("ironshulkerbox:textures/model/pink/shulker_pink_diamond.png"), new ResourceLocation("ironshulkerbox:textures/model/gray/shulker_gray_diamond.png"),
			new ResourceLocation("ironshulkerbox:textures/model/light_gray/shulker_light_gray_diamond.png"), new ResourceLocation("ironshulkerbox:textures/model/cyan/shulker_cyan_diamond.png"),
			new ResourceLocation("ironshulkerbox:textures/model/purple/shulker_purple_diamond.png"), new ResourceLocation("ironshulkerbox:textures/model/blue/shulker_blue_diamond.png"),
			new ResourceLocation("ironshulkerbox:textures/model/brown/shulker_brown_diamond.png"), new ResourceLocation("ironshulkerbox:textures/model/green/shulker_green_diamond.png"),
			new ResourceLocation("ironshulkerbox:textures/model/red/shulker_red_diamond.png"), new ResourceLocation("ironshulkerbox:textures/model/black/shulker_black_diamond.png"),

			new ResourceLocation("ironshulkerbox:textures/model/vanilla/shulker_copper.png"),
			new ResourceLocation("ironshulkerbox:textures/model/white/shulker_white_copper.png"), new ResourceLocation("ironshulkerbox:textures/model/orange/shulker_orange_copper.png"),
			new ResourceLocation("ironshulkerbox:textures/model/magenta/shulker_magenta_copper.png"), new ResourceLocation("ironshulkerbox:textures/model/light_blue/shulker_light_blue_copper.png"),
			new ResourceLocation("ironshulkerbox:textures/model/yellow/shulker_yellow_copper.png"), new ResourceLocation("ironshulkerbox:textures/model/lime/shulker_lime_copper.png"),
			new ResourceLocation("ironshulkerbox:textures/model/pink/shulker_pink_copper.png"), new ResourceLocation("ironshulkerbox:textures/model/gray/shulker_gray_copper.png"),
			new ResourceLocation("ironshulkerbox:textures/model/light_gray/shulker_light_gray_copper.png"), new ResourceLocation("ironshulkerbox:textures/model/cyan/shulker_cyan_copper.png"),
			new ResourceLocation("ironshulkerbox:textures/model/purple/shulker_purple_copper.png"), new ResourceLocation("ironshulkerbox:textures/model/blue/shulker_blue_copper.png"),
			new ResourceLocation("ironshulkerbox:textures/model/brown/shulker_brown_copper.png"), new ResourceLocation("ironshulkerbox:textures/model/green/shulker_green_copper.png"),
			new ResourceLocation("ironshulkerbox:textures/model/red/shulker_red_copper.png"), new ResourceLocation("ironshulkerbox:textures/model/black/shulker_black_copper.png"),

			new ResourceLocation("ironshulkerbox:textures/model/vanilla/shulker_silver.png"),
			new ResourceLocation("ironshulkerbox:textures/model/white/shulker_white_silver.png"), new ResourceLocation("ironshulkerbox:textures/model/orange/shulker_orange_silver.png"),
			new ResourceLocation("ironshulkerbox:textures/model/magenta/shulker_magenta_silver.png"), new ResourceLocation("ironshulkerbox:textures/model/light_blue/shulker_light_blue_silver.png"),
			new ResourceLocation("ironshulkerbox:textures/model/yellow/shulker_yellow_silver.png"), new ResourceLocation("ironshulkerbox:textures/model/lime/shulker_lime_silver.png"),
			new ResourceLocation("ironshulkerbox:textures/model/pink/shulker_pink_silver.png"), new ResourceLocation("ironshulkerbox:textures/model/gray/shulker_gray_silver.png"),
			new ResourceLocation("ironshulkerbox:textures/model/light_gray/shulker_light_gray_silver.png"), new ResourceLocation("ironshulkerbox:textures/model/cyan/shulker_cyan_silver.png"),
			new ResourceLocation("ironshulkerbox:textures/model/purple/shulker_purple_silver.png"), new ResourceLocation("ironshulkerbox:textures/model/blue/shulker_blue_silver.png"),
			new ResourceLocation("ironshulkerbox:textures/model/brown/shulker_brown_silver.png"), new ResourceLocation("ironshulkerbox:textures/model/green/shulker_green_silver.png"),
			new ResourceLocation("ironshulkerbox:textures/model/red/shulker_red_silver.png"), new ResourceLocation("ironshulkerbox:textures/model/black/shulker_black_silver.png"),

			new ResourceLocation("ironshulkerbox:textures/model/vanilla/shulker_crystal.png"),
			new ResourceLocation("ironshulkerbox:textures/model/white/shulker_white_crystal.png"), new ResourceLocation("ironshulkerbox:textures/model/orange/shulker_orange_crystal.png"),
			new ResourceLocation("ironshulkerbox:textures/model/magenta/shulker_magenta_crystal.png"), new ResourceLocation("ironshulkerbox:textures/model/light_blue/shulker_light_blue_crystal.png"),
			new ResourceLocation("ironshulkerbox:textures/model/yellow/shulker_yellow_crystal.png"), new ResourceLocation("ironshulkerbox:textures/model/lime/shulker_lime_crystal.png"),
			new ResourceLocation("ironshulkerbox:textures/model/pink/shulker_pink_crystal.png"), new ResourceLocation("ironshulkerbox:textures/model/gray/shulker_gray_crystal.png"),
			new ResourceLocation("ironshulkerbox:textures/model/light_gray/shulker_light_gray_crystal.png"), new ResourceLocation("ironshulkerbox:textures/model/cyan/shulker_cyan_crystal.png"),
			new ResourceLocation("ironshulkerbox:textures/model/purple/shulker_purple_crystal.png"), new ResourceLocation("ironshulkerbox:textures/model/blue/shulker_blue_crystal.png"),
			new ResourceLocation("ironshulkerbox:textures/model/brown/shulker_brown_crystal.png"), new ResourceLocation("ironshulkerbox:textures/model/green/shulker_green_crystal.png"),
			new ResourceLocation("ironshulkerbox:textures/model/red/shulker_red_crystal.png"), new ResourceLocation("ironshulkerbox:textures/model/black/shulker_black_crystal.png"),

			new ResourceLocation("ironshulkerbox:textures/model/vanilla/shulker_obsidian.png"),
			new ResourceLocation("ironshulkerbox:textures/model/white/shulker_white_obsidian.png"), new ResourceLocation("ironshulkerbox:textures/model/orange/shulker_orange_obsidian.png"),
			new ResourceLocation("ironshulkerbox:textures/model/magenta/shulker_magenta_obsidian.png"), new ResourceLocation("ironshulkerbox:textures/model/light_blue/shulker_light_blue_obsidian.png"),
			new ResourceLocation("ironshulkerbox:textures/model/yellow/shulker_yellow_obsidian.png"), new ResourceLocation("ironshulkerbox:textures/model/lime/shulker_lime_obsidian.png"),
			new ResourceLocation("ironshulkerbox:textures/model/pink/shulker_pink_obsidian.png"), new ResourceLocation("ironshulkerbox:textures/model/gray/shulker_gray_obsidian.png"),
			new ResourceLocation("ironshulkerbox:textures/model/light_gray/shulker_light_gray_obsidian.png"), new ResourceLocation("ironshulkerbox:textures/model/cyan/shulker_cyan_obsidian.png"),
			new ResourceLocation("ironshulkerbox:textures/model/purple/shulker_purple_obsidian.png"), new ResourceLocation("ironshulkerbox:textures/model/blue/shulker_blue_obsidian.png"),
			new ResourceLocation("ironshulkerbox:textures/model/brown/shulker_brown_obsidian.png"), new ResourceLocation("ironshulkerbox:textures/model/green/shulker_green_obsidian.png"),
			new ResourceLocation("ironshulkerbox:textures/model/red/shulker_red_obsidian.png"), new ResourceLocation("ironshulkerbox:textures/model/black/shulker_black_obsidian.png")};
	private final ModelShulker modelShulker;

	public TileEntityShulkerBoxRenderer(ModelShulker p_i47216_1_) {
		this.modelShulker = p_i47216_1_;
		this.random = new Random();
		itemRenderer = new RenderItem() {
			@Override
			public byte getMiniBlockCount(ItemStack stack, byte original) {
				return SignedBytes.saturatedCast(Math.min(stack.stackSize / 32, 15) + 1);
			}

			@Override
			public byte getMiniItemCount(ItemStack stack, byte original) {
				return SignedBytes.saturatedCast(Math.min(stack.stackSize / 32, 7) + 1);
			}

			@Override
			public boolean shouldBob() {
				return false;
			}

			@Override
			public boolean shouldSpreadItems() {
				return false;
			}
		};
		itemRenderer.setRenderManager(RenderManager.instance);
	}

	public void renderTileEntityAt(TileEntityShulkerBox te, double x, double y, double z, float partialTicks, int destroyStage) {
		ForgeDirection enumfacing = ForgeDirection.UP;

		tier = te.type.ordinal();

		if (te.hasWorldObj()) {
			int facing = te.facing;
			enumfacing = ForgeDirection.VALID_DIRECTIONS[facing];
		}

		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);

//        GlStateManager.enableDepth();
//        GlStateManager.depthFunc(515);
//        GlStateManager.depthMask(true);
//        GlStateManager.disableCull();
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_ALPHA_TEST); // Needed because the texture has transparent pixels

		if (destroyStage >= 0) {
			this.bindTexture(DESTROY_STAGES[destroyStage]);
			GL11.glMatrixMode(GL11.GL_TEXTURE);
//            GL11.glPushMatrix();
			GL11.glScalef(4.0F, 4.0F, 1.0F);
			GL11.glTranslatef(0.0625F, 0.0625F, 0.0625F);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
		} else {
			this.bindTexture(TEXTURES[te.color % TEXTURES.length + (tier % (TileEntityShulkerBox.tiers.length + 1) * 17)]);
		}

		GL11.glPushMatrix();
//        GlStateManager.enableRescaleNormal();

		if (destroyStage < 0) {
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		}

		GL11.glTranslatef((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
		GL11.glScalef(1.0F, -1.0F, -1.0F);
		GL11.glTranslatef(0.0F, 1.0F, 0.0F);
		GL11.glScalef(0.9995F, 0.9995F, 0.9995F);
		GL11.glTranslatef(0.0F, -1.0F, 0.0F);

		switch (enumfacing) {
			case DOWN:
				GL11.glTranslatef(0.0F, 2.0F, 0.0F);
				GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);

			case UP:
			default:
				break;

			case NORTH:
				GL11.glTranslatef(0.0F, 1.0F, 1.0F);
				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
				break;

			case SOUTH:
				GL11.glTranslatef(0.0F, 1.0F, -1.0F);
				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				break;

			case WEST:
				GL11.glTranslatef(-1.0F, 1.0F, 0.0F);
				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);
				break;

			case EAST:
				GL11.glTranslatef(1.0F, 1.0F, 0.0F);
				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
		}

		this.modelShulker.base.render(0.0625F);
		GL11.glTranslatef(0.0F, -te.getProgress(partialTicks) * 0.5F, 0.0F);
		GL11.glRotatef(270.0F * te.getProgress(partialTicks), 0.0F, 1.0F, 0.0F);
		this.modelShulker.lid.render(0.0625F);
//        GlStateManager.enableCull();
		GL11.glEnable(GL11.GL_CULL_FACE);
//        GlStateManager.disableRescaleNormal();
		GL11.glPopMatrix();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		if (destroyStage >= 0) {
			GL11.glMatrixMode(GL11.GL_TEXTURE);
			GL11.glPopMatrix();
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
		}
		if (te.hasWorldObj() && te.type.getIsClear() && te.getDistanceFrom(this.field_147501_a.field_147560_j, this.field_147501_a.field_147561_k, this.field_147501_a.field_147558_l) < 128d) {
			random.setSeed(254L);
			float shiftX;
			float shiftY;
			float shiftZ;
			int shift = 0;
			float blockScale = 0.70F;
			float timeD = (float) (360.0 * (double) (System.currentTimeMillis() & 0x3FFFL) / (double) 0x3FFFL);
			if (te.getTopItemStacks()[1] == null) {
				shift = 8;
				blockScale = 0.85F;
			}
			GL11.glPushMatrix();
			GL11.glDisable(GL11.GL_LIGHTING /* GL_LIGHTING */);
			GL11.glTranslatef((float) x, (float) y, (float) z);
			EntityItem customitem = new EntityItem(field_147501_a.field_147550_f/*worldObj*/);
			customitem.hoverStart = 0f;
			for (ItemStack item : te.getTopItemStacks()) {
				if (shift > shifts.length) {
					break;
				}
				if (item == null) {
					shift++;
					continue;
				}
				shiftX = shifts[shift][0];
				shiftY = shifts[shift][1];
				shiftZ = shifts[shift][2];
				shift++;
				GL11.glPushMatrix();
				GL11.glTranslatef(shiftX, shiftY, shiftZ);
				GL11.glRotatef(timeD, 0.0F, 1.0F, 0.0F);
				GL11.glScalef(blockScale, blockScale, blockScale);
				customitem.setEntityItemStack(item);
				itemRenderer.doRender(customitem, 0, 0, 0, 0, 0);
				GL11.glPopMatrix();
			}
			GL11.glEnable(GL11.GL_LIGHTING /* GL_LIGHTING */);
			GL11.glPopMatrix();
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		}

		GL11.glPopAttrib();
	}

	@Override
	public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
		this.renderTileEntityAt((TileEntityShulkerBox) te, x, y, z, partialTicks, -1);
	}

}
