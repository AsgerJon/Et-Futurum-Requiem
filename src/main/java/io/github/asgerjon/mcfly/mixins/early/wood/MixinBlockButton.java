package io.github.asgerjon.mcfly.mixins.early.wood;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlySetType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * 26.3's floor and ceiling buttons for every `BlockButton` (SPEC-modern-movement.md §91.7, step E; EFR's
 * `floorceilbutton` mixin, rebuilt), in 1.7.10's spare metas: 5 on a floor and 0 under a ceiling facing north or south
 * (long along x), 6 and 7 facing east or west (long along z); `+8` pressed (a button is the same turned half a turn, so
 * 26.3's four facings fit two metas). Placed on a floor or ceiling by the player's facing; held by the sturdy face it's
 * on; 26.3's shapes; 15 strong into that block alone (26.3's `getDirectSignal`). Its clicks are the set type's
 * (`button.click_on`, `click_off`; 1.7.10 played `random.click` higher or lower).
 */
@Mixin(BlockButton.class)
public abstract class MixinBlockButton extends Block {

	protected MixinBlockButton(Material material) {
		super(material);
	}

	private static boolean onFloor(int kind) {
		return kind == 5 || kind == 6;
	}

	private static boolean onCeiling(int kind) {
		return kind == 0 || kind == 7;
	}

	/** The fork's full-face rule (spec §53.4: glass holds a button too), as the walls' checks have it. */
	private static boolean floorHolds(World world, int x, int y, int z) {
		return ganymedes01.etfuturum.core.utils.BlockSupport.canAttachTo(world, x, y - 1, z, ForgeDirection.UP);
	}

	private static boolean ceilingHolds(World world, int x, int y, int z) {
		return ganymedes01.etfuturum.core.utils.BlockSupport.canAttachTo(world, x, y + 1, z, ForgeDirection.DOWN);
	}

	@Inject(method = "canPlaceBlockOnSide", at = @At("HEAD"), cancellable = true)
	private void mcfly$onFloorOrCeiling(World world, int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		if (side == 1 && floorHolds(world, x, y, z) || side == 0 && ceilingHolds(world, x, y, z)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "canPlaceBlockAt", at = @At("HEAD"), cancellable = true)
	private void mcfly$floorOrCeilingHolds(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (floorHolds(world, x, y, z) || ceilingHolds(world, x, y, z)) {
			cir.setReturnValue(true);
		}
	}

	/** On a floor's top 5, under a ceiling 0 (turned by the player's facing once placed). */
	@Inject(method = "onBlockPlaced", at = @At("HEAD"), cancellable = true)
	private void mcfly$placedUpOrDown(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta, CallbackInfoReturnable<Integer> cir) {
		if (side == 1 && floorHolds(world, x, y, z)) {
			cir.setReturnValue(5);
		} else if (side == 0 && ceilingHolds(world, x, y, z)) {
			cir.setReturnValue(0);
		}
	}

	/** With no face clicked that holds it: a floor, then a ceiling, before the walls (EFR's order). */
	@Inject(method = "func_150045_e", at = @At("HEAD"), cancellable = true)
	private void mcfly$floorFirst(World world, int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		if (floorHolds(world, x, y, z)) {
			cir.setReturnValue(5);
		} else if (ceilingHolds(world, x, y, z)) {
			cir.setReturnValue(0);
		}
	}

	/** 26.3's facing on a floor or ceiling is the player's: east or west turns it along z (6, 7). */
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		int meta = world.getBlockMetadata(x, y, z);
		int facing = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
		if ((facing & 1) != 0 && ((meta & 7) == 5 || (meta & 7) == 0)) {
			world.setBlockMetadataWithNotify(x, y, z, meta & 8 | ((meta & 7) == 5 ? 6 : 7), 2);
		}
	}

	/** A floor or ceiling button falls when the face it's on goes. */
	@Inject(method = "onNeighborBlockChange", at = @At("HEAD"), cancellable = true)
	private void mcfly$heldUpOrDown(World world, int x, int y, int z, Block neighbour, CallbackInfo ci) {
		int kind = world.getBlockMetadata(x, y, z) & 7;
		if (onFloor(kind) || onCeiling(kind)) {
			if (onFloor(kind) ? !floorHolds(world, x, y, z) : !ceilingHolds(world, x, y, z)) {
				dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
				world.setBlockToAir(x, y, z);
			}
			ci.cancel();
		}
	}

	/** 26.3's shapes: 6 by 4, 2 high (1 pressed), along its facing. */
	@Inject(method = "func_150043_b", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void mcfly$floorAndCeilingShapes(int meta, CallbackInfo ci, int kind, boolean pressed, float f, float f1, float f2, float f3) {
		switch (kind) {
			case 5:
				setBlockBounds(0.5F - f2, 0.0F, f, 0.5F + f2, f3, f1);
				break;
			case 6:
				setBlockBounds(f, 0.0F, 0.5F - f2, f1, f3, 0.5F + f2);
				break;
			case 0:
				setBlockBounds(0.5F - f2, 1.0F - f3, f, 0.5F + f2, 1.0F, f1);
				break;
			case 7:
				setBlockBounds(f, 1.0F - f3, 0.5F - f2, f1, 1.0F, 0.5F + f2);
				break;
			default:
				break;
		}
	}

	/** A ceiling button's press tells the block above (1.7.10's fallback tells the one below: a floor's). */
	@Inject(method = "func_150042_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;notifyBlocksOfNeighborChange(IIILnet/minecraft/block/Block;)V", ordinal = 5),
		cancellable = true)
	private void mcfly$tellTheCeiling(World world, int x, int y, int z, int kind, CallbackInfo ci) {
		if (onCeiling(kind)) {
			world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
			ci.cancel();
		}
	}

	/** 15 strong into the floor or ceiling it's on, nowhere else (26.3's `getDirectSignal`). */
	@Inject(method = "isProvidingStrongPower", at = @At("HEAD"), cancellable = true)
	private void mcfly$strongUpOrDown(IBlockAccess world, int x, int y, int z, int side, CallbackInfoReturnable<Integer> cir) {
		int meta = world.getBlockMetadata(x, y, z);
		int kind = meta & 7;
		if (onFloor(kind) || onCeiling(kind)) {
			cir.setReturnValue((meta & 8) != 0 && side == (onFloor(kind) ? 1 : 0) ? 15 : 0);
		}
	}

	@WrapOperation(method = {"onBlockActivated", "updateTick", "func_150046_n"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V"))
	private void mcfly$setTypeClick(World world, double x, double y, double z, String name, float volume, float pitch, Operation<Void> original) {
		if ("random.click".equals(name)) {
			world.playSoundEffect(x, y, z, McFly.MC_ASSETS + ":" + McFlySetType.of(this).buttonSound(pitch > 0.55F), 1.0F, 1.0F);
		} else {
			original.call(world, x, y, z, name, volume, pitch);
		}
	}
}
