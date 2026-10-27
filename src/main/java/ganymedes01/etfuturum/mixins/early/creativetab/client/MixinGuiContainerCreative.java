package ganymedes01.etfuturum.mixins.early.creativetab.client;

import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

/**
 * The creative screen's survival inventory tab laid out as modern's (1.9+): the armour slots in two columns, 54 and
 * 108, with the player drawn between them following the mouse, the off-hand slot at their left (placed by the
 * off-hand's own mixin) and modern's background for the tab. 1.7.10 had its two columns at 9 and 63 with the player
 * between them at 43, which left no room for the off-hand slot.
 */
@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreative extends InventoryEffectRenderer {
	@Shadow
	private static int selectedTabIndex;

	public MixinGuiContainerCreative(Container container) {
		super(container);
	}

	@Inject(method = "setCurrentCreativeTab", at = @At("TAIL"))
	private void etfu$armourInTwoColumns(CreativeTabs tab, CallbackInfo ci) {
		if (tab != CreativeTabs.tabInventory) {
			return;
		}
		List<?> slots = this.inventorySlots.inventorySlots;
		for (int k = 5; k < 9 && k < slots.size(); k++) {
			Slot slot = (Slot) slots.get(k);
			slot.xDisplayPosition = 54 + (k - 5) / 2 * 54;
			slot.yDisplayPosition = 6 + (k - 5) % 2 * 27;
		}
	}

	/** Modern's background for the tab, with the frames where the slots now are. */
	@ModifyArg(method = "drawGuiContainerBackgroundLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ResourceLocation;<init>(Ljava/lang/String;)V"))
	private String etfu$modernTabBackground(String path) {
		return path.endsWith("tab_inventory.png") ? Reference.MOD_ID + ":" + path : path;
	}

	/**
	 * Vanilla's player draw moved to modern's place: modern draws it in the box 73..105 by 6..49 at scale 20, the feet
	 * at the box's centre plus half the height, so centred at 89 with the feet at 47; the mouse offsets follow.
	 */
	@ModifyArgs(method = "drawGuiContainerBackgroundLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/inventory/GuiInventory;func_147046_a(IIIFFLnet/minecraft/entity/EntityLivingBase;)V"))
	private void etfu$playerBetweenTheArmour(Args args, float partialTicks, int mouseX, int mouseY) {
		int x = this.guiLeft + 89;
		int feet = this.guiTop + 47;
		args.set(0, x);
		args.set(1, feet);
		args.set(3, (float) (x - mouseX));
		args.set(4, (float) (feet - 30 - mouseY));
	}
}
