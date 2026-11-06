package io.github.asgerjon.mcfly.mixins.early.wood;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.asgerjon.mcfly.base.McFlySetType;
import io.github.asgerjon.mcfly.wood.Trapdoors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.7.10's trapdoors by 26.3's rules (SPEC-modern-movement.md §91.6, step D; the user: "make trapdoors as capable as the
 * 26.3"): vanilla's oak trapdoor keeps 26.3's powered state in McFly's block entity ({@link Trapdoors.Tile}: its four
 * meta bits are all used), so every neighbour's update asks and only a change of signal acts; level event 1003 becomes
 * the set type's sound by the new state; every trapdoor over a ladder of its facing climbs (26.3's
 * `trapdoorUsableAsLadder`; EFR's `laddertrapdoors` took any open trapdoor over any ladder).
 */
@Mixin(BlockTrapDoor.class)
public abstract class MixinBlockTrapDoor extends Block {

	protected MixinBlockTrapDoor(Material material) {
		super(material);
	}

	/** Vanilla's own trapdoor (not a mod's subclass, which keeps its own) keeps the powered state. */
	@Override
	public boolean hasTileEntity(int meta) {
		return ((Object) this).getClass() == (Object) BlockTrapDoor.class;
	}

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		return ((Object) this).getClass() == (Object) BlockTrapDoor.class ? new Trapdoors.Tile() : null;
	}

	@Override
	public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
		return Trapdoors.usableAsLadder(world, x, y, z);
	}

	@WrapOperation(method = {"onBlockActivated", "func_150120_a"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/World;playAuxSFXAtEntity(Lnet/minecraft/entity/player/EntityPlayer;IIIII)V"))
	private void mcfly$setTypeSound(World world, EntityPlayer player, int id, int x, int y, int z, int data, Operation<Void> original) {
		if (id != 1003) {
			original.call(world, player, id, x, y, z, data);
			return;
		}
		if (!world.isRemote) {
			McFlySetType.of(this).playTrapdoor(world, x, y, z, (world.getBlockMetadata(x, y, z) & Trapdoors.OPEN) != 0);
		}
	}

	/** A trapdoor keeping the powered state is asked on every update; another as 1.7.10 asked it. */
	@WrapOperation(method = "onNeighborBlockChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;canProvidePower()Z"))
	private boolean mcfly$everyUpdate(Block neighbour, Operation<Boolean> original, @Local(argsOnly = true) World world,
									  @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y, @Local(argsOnly = true, ordinal = 2) int z) {
		return Trapdoors.tile(world, x, y, z) != null || original.call(neighbour);
	}

	@Redirect(method = "onNeighborBlockChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockTrapDoor;func_150120_a(Lnet/minecraft/world/World;IIIZ)V"))
	private void mcfly$byPower(BlockTrapDoor trapdoor, World world, int x, int y, int z, boolean signal) {
		if (!Trapdoors.redstone(trapdoor, world, x, y, z)) {
			trapdoor.func_150120_a(world, x, y, z, signal);
		}
	}
}
