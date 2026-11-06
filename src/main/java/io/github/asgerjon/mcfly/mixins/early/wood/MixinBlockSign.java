package io.github.asgerjon.mcfly.mixins.early.wood;

import cpw.mods.fml.common.FMLCommonHandler;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.wood.Signs;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockSign;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;

/**
 * Every `BlockSign` as 26.3's sign (SPEC-modern-movement.md 91.8): vanilla's oak sign, McFly's `McFlySign`, other mods'.
 * Using it is `Signs.use` (1.7.10's sign did nothing); it's drawn as 26.3's block models in the chunk
 * (`template_sign_rot_0` to `_3` turned by quarters, `template_wall_sign` by facing) with its wood's `block/<wood>_sign`
 * texture, its text by McFly's sign renderer.
 */
@Mixin(BlockSign.class)
public abstract class MixinBlockSign extends BlockContainer implements Signs.Kind, ModelledBlock {

	@Shadow
	private boolean field_149967_b;

	@Unique
	private IIcon mcfly$signIcon;

	protected MixinBlockSign(Material material) {
		super(material);
	}

	@Override
	public boolean mcfly$standing() {
		return field_149967_b;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		return Signs.use(world, x, y, z, player) || super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
	}

	@Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
	private void mcfly$drawnAsModels(CallbackInfoReturnable<Integer> cir) {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			cir.setReturnValue(McFlyRenderIds.MODEL);
		}
	}

	/** Set by `wood.client.MixinBlockSignIcon` (1.7.10's `registerBlockIcons` is the client's alone). */
	@Override
	public void mcfly$setSignIcon(IIcon icon) {
		mcfly$signIcon = icon;
	}

	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		if (field_149967_b) {
			return Collections.singletonList(new ModelPart("template_sign_rot_" + (meta & 3), 0, 90 * ((meta & 15) >> 2), false));
		}
		return Collections.singletonList(new ModelPart("template_wall_sign", 0, mcfly$wallTurn(meta), false));
	}

	/** A wall sign's model turn by 1.7.10's meta (2 north, 3 south, 4 west, 5 east), as 26.3's blockstate. */
	@Unique
	private static int mcfly$wallTurn(int meta) {
		switch (meta) {
			case 2:
				return 180;
			case 4:
				return 90;
			case 5:
				return 270;
			default:
				return 0;
		}
	}

	/** Its item is a flat sprite (26.3's `item/generated`): no model. */
	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.emptyList();
	}

	@Override
	public IIcon modelIcon(String texture) {
		return mcfly$signIcon;
	}
}
