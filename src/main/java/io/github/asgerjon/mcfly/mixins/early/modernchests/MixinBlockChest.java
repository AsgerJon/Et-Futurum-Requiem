package io.github.asgerjon.mcfly.mixins.early.modernchests;

import ganymedes01.etfuturum.blocks.ModernChests;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chests placed and joined as 26.3's (spec §65, `ModernChests`): no 1.7.10 limit on where a chest goes, no turning of
 * its neighbours on placing; the facing and type from the placer and the clicked face; 26.3's neighbour rules; the
 * double's inventory and box from the other half only. A chest whose type isn't known yet keeps 1.7.10's ways.
 */
@Mixin(BlockChest.class)
public abstract class MixinBlockChest extends BlockContainer {

	protected MixinBlockChest(Material material) {
		super(material);
	}

	@Shadow
	private static boolean func_149953_o(World world, int x, int y, int z) {
		throw new AssertionError();
	}

	@Inject(method = "canPlaceBlockAt", at = @At("HEAD"), cancellable = true)
	private void etfu$placeAnywhere(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (ModernChests.isOn()) {
			cir.setReturnValue(world.getBlock(x, y, z).isReplaceable(world, x, y, z));
		}
	}

	@Inject(method = "onBlockAdded", at = @At("HEAD"), cancellable = true)
	private void etfu$noFacingUnifying(World world, int x, int y, int z, CallbackInfo ci) {
		if (ModernChests.isOn()) {
			ci.cancel();
		}
	}

	/** The clicked face, which 26.3's placing reads; 1.7.10 gives it only here, just before `onBlockPlacedBy`. */
	@Override
	public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
		ModernChests.CLICKED.set(side);
		return meta;
	}

	@Inject(method = "onBlockPlacedBy", at = @At("HEAD"), cancellable = true)
	private void etfu$place(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack, CallbackInfo ci) {
		if (!ModernChests.isOn()) {
			return;
		}
		ci.cancel();
		ModernChests.place(world, x, y, z, placer, (Block) (Object) this);
		TileEntity tile = world.getTileEntity(x, y, z);
		if (stack.hasDisplayName() && tile instanceof TileEntityChest chest) {
			chest.func_145976_a(stack.getDisplayName());
		}
	}

	@Inject(method = "onNeighborBlockChange", at = @At("HEAD"))
	private void etfu$neighbourRules(World world, int x, int y, int z, Block neighbour, CallbackInfo ci) {
		if (ModernChests.isOn() && !world.isRemote) {
			ModernChests.neighbourChanged(world, x, y, z, (Block) (Object) this);
		}
	}

	@Inject(method = "func_149951_m", at = @At("HEAD"), cancellable = true)
	private void etfu$partnerInventory(World world, int x, int y, int z, CallbackInfoReturnable<IInventory> cir) {
		if (ModernChests.isOn() && !"unknown".equals(ModernChests.typeOf(world, x, y, z))) {
			cir.setReturnValue(ModernChests.inventory(world, x, y, z, (Block) (Object) this,
				(w, bx, by, bz) -> w.isSideSolid(bx, by + 1, bz, ForgeDirection.DOWN) || func_149953_o(w, bx, by, bz)));
		}
	}

	@Inject(method = "setBlockBoundsBasedOnState", at = @At("HEAD"), cancellable = true)
	private void etfu$halfBox(IBlockAccess world, int x, int y, int z, CallbackInfo ci) {
		if (ModernChests.isOn() && !"unknown".equals(ModernChests.typeOf(world, x, y, z))) {
			float[] box = ModernChests.bounds(world, x, y, z, (Block) (Object) this);
			setBlockBounds(box[0], box[1], box[2], box[3], box[4], box[5]);
			ci.cancel();
		}
	}
}
