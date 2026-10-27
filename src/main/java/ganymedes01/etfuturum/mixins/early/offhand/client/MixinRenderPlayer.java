package ganymedes01.etfuturum.mixins.early.offhand.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.offhand.Arm;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.IOffhandPlayer;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.block.Block;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third person with two hands (SPEC-modern-movement.md §36.6): each arm takes the pose of its hand's item (empty,
 * held, blocking, or the two-handed bow when that hand draws one), and the off-hand's item hangs from its arm with
 * the main hand's transform mirrored in x for the left arm. 1.7.10 posed the right arm from the held item and the
 * use count, whichever hand the use belonged to; both are set here from the hands instead.
 */
@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer extends RendererLivingEntity {
	@Unique
	private static final int EMPTY = 0, HELD = 1, BLOCKING = 3, BOW = -1;

	@Shadow
	public ModelBiped modelBipedMain;
	@Shadow
	public ModelBiped modelArmorChestplate;
	@Shadow
	public ModelBiped modelArmor;

	public MixinRenderPlayer(ModelBase model, float shadowSize) {
		super(model, shadowSize);
	}

	/** Right before the model is drawn, after vanilla's own held-item setup, which this replaces. */
	@Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V"))
	private void etfu$poseBothArms(AbstractClientPlayer player, double x, double y, double z, float yaw, float partialTicks, CallbackInfo ci) {
		if (!Offhand.isEnabled()) {
			return;
		}
		Hand usingHand = Offhand.getUsingHand(player);
		int mainPose = etfu$armPose(player, player.inventory.getCurrentItem(), Hand.MAIN, usingHand);
		int offPose = etfu$armPose(player, Offhand.getOffhandOf(player), Hand.OFF, usingHand);
		// The bow takes both arms; the other hand's item still shows as held, which vanilla's bow pose covers.
		boolean bow = mainPose == BOW || offPose == BOW;
		boolean leftHanded = Offhand.isLeftHanded(player);
		int rightArm = Hand.MAIN.arm(leftHanded) == Arm.RIGHT ? mainPose : offPose;
		int leftArm = Hand.MAIN.arm(leftHanded) == Arm.LEFT ? mainPose : offPose;
		for (ModelBiped model : new ModelBiped[] {modelBipedMain, modelArmor, modelArmorChestplate}) {
			model.heldItemRight = bow ? EMPTY : rightArm;
			model.heldItemLeft = bow ? EMPTY : leftArm;
			model.aimedBow = bow;
		}
	}

	@Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At("RETURN"))
	private void etfu$resetLeftArm(AbstractClientPlayer player, double x, double y, double z, float yaw, float partialTicks, CallbackInfo ci) {
		modelBipedMain.heldItemLeft = modelArmor.heldItemLeft = modelArmorChestplate.heldItemLeft = EMPTY;
	}

	/** The hand on the right arm, the one vanilla's own item pass draws: the main hand, or the off-hand of a left-handed player (§37). */
	@Unique
	private static Hand etfu$handOnTheRightArm(EntityPlayer player) {
		return Hand.MAIN.arm(Offhand.isLeftHanded(player)) == Arm.RIGHT ? Hand.MAIN : Hand.OFF;
	}

	/** Vanilla's pass draws the right arm's item: a left-handed player's off-hand item. */
	@ModifyExpressionValue(method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"))
	private ItemStack etfu$rightArmItem(ItemStack current, @Local(argsOnly = true) AbstractClientPlayer player) {
		return Offhand.isEnabled() && etfu$handOnTheRightArm(player) == Hand.OFF ? Offhand.getOffhandOf(player) : current;
	}

	/** The right arm's use transforms only apply when its hand is the one using. */
	@ModifyExpressionValue(method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;getItemInUseCount()I"))
	private int etfu$mainHandUseCount(int count, @Local(argsOnly = true) AbstractClientPlayer player) {
		return Offhand.isEnabled() && Offhand.getUsingHand(player) != etfu$handOnTheRightArm(player) ? 0 : count;
	}

	/**
	 * The left arm's item (the off-hand's, or a left-handed player's main-hand item), drawn like vanilla draws the
	 * right arm's (same transforms, same render passes) in a mirrored space. Right before the Specials.Post event, so
	 * it respects the Pre event's renderItem flag like the other item.
	 */
	@Inject(method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V", at = @At(value = "INVOKE", target = "Lcpw/mods/fml/common/eventhandler/EventBus;post(Lcpw/mods/fml/common/eventhandler/Event;)Z", ordinal = 1, remap = false))
	private void etfu$renderOffhandItem(AbstractClientPlayer player, float partialTicks, CallbackInfo ci, @Local RenderPlayerEvent.Specials.Pre event) {
		if (!Offhand.isEnabled() || !event.renderItem) {
			return;
		}
		Hand hand = etfu$handOnTheRightArm(player) == Hand.MAIN ? Hand.OFF : Hand.MAIN;
		ItemStack stack = hand == Hand.OFF ? Offhand.getOffhandOf(player) : player.inventory.getCurrentItem();
		if (stack == null) {
			return;
		}
		boolean leftArm = true;
		EnumAction action = player.getItemInUseCount() > 0 && Offhand.getUsingHand(player) == hand ? stack.getItemUseAction() : null;
		int frontFace = GL11.glGetInteger(GL11.GL_FRONT_FACE);
		GL11.glPushMatrix();
		modelBipedMain.bipedLeftArm.postRender(0.0625F);
		if (leftArm) {
			GL11.glScalef(-1.0F, 1.0F, 1.0F);
			GL11.glFrontFace(GL11.GL_CW);
		}
		GL11.glTranslatef(-0.0625F, 0.4375F, 0.0625F);
		IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(stack, IItemRenderer.ItemRenderType.EQUIPPED);
		boolean is3D = customRenderer != null && customRenderer.shouldUseRenderHelper(IItemRenderer.ItemRenderType.EQUIPPED, stack, IItemRenderer.ItemRendererHelper.BLOCK_3D);
		float scale;
		if (is3D || stack.getItem() instanceof ItemBlock && RenderBlocks.renderItemIn3d(Block.getBlockFromItem(stack.getItem()).getRenderType())) {
			scale = 0.5F;
			GL11.glTranslatef(0.0F, 0.1875F, -0.3125F);
			scale *= 0.75F;
			GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
			GL11.glScalef(-scale, -scale, scale);
		} else if (stack.getItem() == Items.bow) {
			scale = 0.625F;
			GL11.glTranslatef(0.0F, 0.125F, 0.3125F);
			GL11.glRotatef(-20.0F, 0.0F, 1.0F, 0.0F);
			GL11.glScalef(scale, -scale, scale);
			GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
		} else if (stack.getItem().isFull3D()) {
			scale = 0.625F;
			if (stack.getItem().shouldRotateAroundWhenRendering()) {
				GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
				GL11.glTranslatef(0.0F, -0.125F, 0.0F);
			}
			if (action == EnumAction.block) {
				GL11.glTranslatef(0.05F, 0.0F, -0.1F);
				GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
				GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(-60.0F, 0.0F, 0.0F, 1.0F);
			}
			GL11.glTranslatef(0.0F, 0.1875F, 0.0F);
			GL11.glScalef(scale, -scale, scale);
			GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
		} else {
			scale = 0.375F;
			GL11.glTranslatef(0.25F, 0.1875F, -0.1875F);
			GL11.glScalef(scale, scale, scale);
			GL11.glRotatef(60.0F, 0.0F, 0.0F, 1.0F);
			GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);
		}
		int passes = stack.getItem().requiresMultipleRenderPasses() ? stack.getItem().getRenderPasses(stack.getItemDamage()) : 1;
		for (int pass = 0; pass < passes; pass++) {
			int colour = stack.getItem().getColorFromItemStack(stack, pass);
			GL11.glColor4f((colour >> 16 & 255) / 255.0F, (colour >> 8 & 255) / 255.0F, (colour & 255) / 255.0F, 1.0F);
			this.renderManager.itemRenderer.renderItem(player, stack, pass);
		}
		if (leftArm) {
			GL11.glFrontFace(frontFace);
		}
		GL11.glPopMatrix();
	}

	/** Modern's arm pose for a hand: empty, held, or the use pose while that hand uses the item. */
	@Unique
	private static int etfu$armPose(EntityPlayer player, ItemStack stack, Hand hand, Hand usingHand) {
		if (stack == null) {
			return EMPTY;
		}
		if (usingHand == hand && player.getItemInUseCount() > 0) {
			EnumAction action = stack.getItemUseAction();
			if (action == EnumAction.block) {
				return BLOCKING;
			}
			if (action == EnumAction.bow) {
				return BOW;
			}
		}
		return HELD;
	}
}
