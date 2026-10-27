package ganymedes01.etfuturum.mixins.early.offhand.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.offhand.Arm;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.IOffhandItemRenderer;
import ganymedes01.etfuturum.offhand.IOffhandPlayer;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Both hands in first person (SPEC-modern-movement.md §36.5). 1.7.10 draws one hand from three fields: the shown
 * item, its raise height and the previous height. The off-hand keeps its own three and the same drawing code runs a
 * second time with them swapped in, after the main hand as in modern, with the view mirrored in x for the left side:
 * modern's left-arm transforms are the right arm's with the x translations and the y and z rotations negated, which
 * is exactly that mirror, and item renderers from other mods come along. The swing and the use animation are
 * answered for the hand being drawn only. An empty off-hand draws nothing; only the drawing hand shows while a bow
 * is drawn. A map in the off-hand is skipped (1.7.10 only has the two-handed map view; §36.5 gap).
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer implements IOffhandItemRenderer {
	@Shadow
	private Minecraft mc;
	@Shadow
	private ItemStack itemToRender;
	@Shadow
	private float equippedProgress;
	@Shadow
	private float prevEquippedProgress;

	@Shadow
	public abstract void renderItemInFirstPerson(float partialTicks);

	@Unique
	private ItemStack etfu$offhandItemToRender;
	@Unique
	private float etfu$offhandEquippedProgress;
	@Unique
	private float etfu$offhandPrevEquippedProgress;
	@Unique
	private Hand etfu$renderingHand = Hand.MAIN;
	@Unique
	private boolean etfu$mainHandSkipped;
	@Unique
	private boolean etfu$mainMirrored;
	@Unique
	private int etfu$mainFrontFace;

	/**
	 * A bow drawn from the off-hand hides the main hand: the off-hand is drawn alone. Otherwise the main hand is drawn
	 * by vanilla, in a mirrored space when it is the left one (§37), which the RETURN hook undoes.
	 */
	@Inject(method = "renderItemInFirstPerson", at = @At("HEAD"), cancellable = true)
	private void etfu$offhandAlone(float partialTicks, CallbackInfo ci) {
		if (etfu$renderingHand == Hand.OFF || !Offhand.isEnabled()) {
			return;
		}
		etfu$mainHandSkipped = false;
		if (etfu$bowDrawnFrom(Hand.OFF)) {
			etfu$renderOffhand(partialTicks);
			etfu$mainHandSkipped = true;
			ci.cancel();
			return;
		}
		if (Hand.MAIN.arm(Offhand.isLeftHanded(mc.thePlayer)) == Arm.LEFT) {
			etfu$mainFrontFace = GL11.glGetInteger(GL11.GL_FRONT_FACE);
			GL11.glPushMatrix();
			GL11.glScalef(-1.0F, 1.0F, 1.0F);
			GL11.glFrontFace(GL11.GL_CW);
			etfu$mainMirrored = true;
		}
	}

	/** After the main hand, the off-hand, unless a bow is drawn from the main hand. */
	@Inject(method = "renderItemInFirstPerson", at = @At("RETURN"))
	private void etfu$offhandAfterMain(float partialTicks, CallbackInfo ci) {
		if (etfu$renderingHand == Hand.OFF) {
			return;
		}
		if (etfu$mainMirrored) {
			GL11.glFrontFace(etfu$mainFrontFace);
			GL11.glPopMatrix();
			etfu$mainMirrored = false;
		}
		if (!Offhand.isEnabled() || etfu$mainHandSkipped) {
			return;
		}
		if (!etfu$bowDrawnFrom(Hand.MAIN)) {
			etfu$renderOffhand(partialTicks);
		}
	}

	@Override
	public Hand etfu$drawnHand() {
		return etfu$renderingHand;
	}

	@Unique
	private void etfu$renderOffhand(float partialTicks) {
		ItemStack stack = etfu$offhandItemToRender;
		if (stack == null || stack.getItem() instanceof ItemMap) {
			return;
		}
		EntityClientPlayerMP player = mc.thePlayer;
		ItemStack mainItem = itemToRender;
		float mainProgress = equippedProgress;
		float mainPrevProgress = prevEquippedProgress;
		itemToRender = stack;
		equippedProgress = etfu$offhandEquippedProgress;
		prevEquippedProgress = etfu$offhandPrevEquippedProgress;
		etfu$renderingHand = Hand.OFF;
		boolean mirror = Hand.OFF.arm(Offhand.isLeftHanded(player)) == Arm.LEFT;
		int frontFace = GL11.glGetInteger(GL11.GL_FRONT_FACE);
		GL11.glPushMatrix();
		if (mirror) {
			GL11.glScalef(-1.0F, 1.0F, 1.0F);
			GL11.glFrontFace(GL11.GL_CW);
		}
		try {
			renderItemInFirstPerson(partialTicks);
		} finally {
			if (mirror) {
				GL11.glFrontFace(frontFace);
			}
			GL11.glPopMatrix();
			etfu$renderingHand = Hand.MAIN;
			itemToRender = mainItem;
			equippedProgress = mainProgress;
			prevEquippedProgress = mainPrevProgress;
		}
	}

	/** The swing animation belongs to the swinging hand; the other hand stays still. */
	@ModifyExpressionValue(method = "renderItemInFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;getSwingProgress(F)F"))
	private float etfu$swingOfTheDrawnHand(float swing) {
		return Offhand.isEnabled() && Offhand.getSwingingHand(mc.thePlayer) != etfu$renderingHand ? 0.0F : swing;
	}

	/** The use animation belongs to the using hand. */
	@ModifyExpressionValue(method = "renderItemInFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;getItemInUseCount()I"))
	private int etfu$useOfTheDrawnHand(int count) {
		return Offhand.isEnabled() && Offhand.getUsingHand(mc.thePlayer) != etfu$renderingHand ? 0 : count;
	}

	/**
	 * The off-hand's raise height, kept like the main hand's (§36.5): the shown item is replaced once the height has
	 * dropped below 0.1; a stack that only changed in count is adopted at once.
	 */
	@Inject(method = "updateEquippedItem", at = @At("TAIL"))
	private void etfu$updateOffhandEquippedItem(CallbackInfo ci) {
		etfu$offhandPrevEquippedProgress = etfu$offhandEquippedProgress;
		ItemStack stack = Offhand.isEnabled() ? Offhand.getOffhand(mc.thePlayer) : null;
		boolean shown = stack == etfu$offhandItemToRender;
		if (stack != null && etfu$offhandItemToRender != null && stack != etfu$offhandItemToRender
				&& stack.getItem() == etfu$offhandItemToRender.getItem() && stack.getItemDamage() == etfu$offhandItemToRender.getItemDamage()) {
			etfu$offhandItemToRender = stack;
			shown = true;
		}
		float target = shown ? 1.0F : 0.0F;
		etfu$offhandEquippedProgress += MathHelper.clamp_float(target - etfu$offhandEquippedProgress, -0.4F, 0.4F);
		if (etfu$offhandEquippedProgress < 0.1F) {
			etfu$offhandItemToRender = stack;
		}
	}

	@Override
	public void etfu$resetOffhandEquippedProgress() {
		etfu$offhandEquippedProgress = 0.0F;
	}

	@Unique
	private boolean etfu$bowDrawnFrom(Hand hand) {
		EntityClientPlayerMP player = mc.thePlayer;
		ItemStack inUse = player.getItemInUse();
		return inUse != null && player.getItemInUseCount() > 0 && inUse.getItemUseAction() == EnumAction.bow
				&& ((IOffhandPlayer) player).etfu$getUsingHand() == hand;
	}
}
