package io.github.asgerjon.mcfly.mixins.early.frames;

import ganymedes01.etfuturum.entities.ModernFrames;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An item frame's 8 turns and how it is saved (SPEC-modern-movement.md §67.3, §67.5): 26.3's `Facing` written beside
 * 1.7.10's keys; a frame saved without it is 1.7.10's, read on its wall with its 4-way turn doubled (the same look).
 * Its comparators are told when its item or turn changes (26.3's `updateNeighbourForOutputSignal`).
 */
@Mixin(EntityItemFrame.class)
public abstract class MixinEntityItemFrame extends EntityHanging {

	protected MixinEntityItemFrame(World world) {
		super(world);
	}

	@Shadow
	public abstract int getRotation();

	@Shadow
	public abstract void setItemRotation(int rotation);

	/** 1.7.10's `% 4`: 26.3 has 8 turns. */
	@ModifyConstant(method = "setItemRotation", constant = @Constant(intValue = 4))
	private int etfu$eightTurns(int turns) {
		return 8;
	}

	@Inject(method = "setItemRotation", at = @At("TAIL"))
	private void etfu$tellComparatorsOfTheTurn(int rotation, CallbackInfo ci) {
		etfu$tellComparators();
	}

	@Inject(method = "setDisplayedItem", at = @At("TAIL"))
	private void etfu$tellComparatorsOfTheItem(net.minecraft.item.ItemStack stack, CallbackInfo ci) {
		etfu$tellComparators();
	}

	/**
	 * Set while the frame is read from its NBT: then neither its turn nor its item tells comparators (26.3's
	 * `setRotation(rotation, false)` there). Chunk loading reads frames before their chunk is in the world, and the
	 * neighbours' lookup loaded that chunk again from inside its own loading (the user's world, 2026-11-03: frames
	 * lost, each nested load adding the chunk's entities again, the End's server thread stuck).
	 */
	@org.spongepowered.asm.mixin.Unique
	private boolean etfu$reading;

	@Inject(method = "readEntityFromNBT", at = @At("HEAD"))
	private void etfu$startReading(NBTTagCompound tag, CallbackInfo ci) {
		etfu$reading = true;
	}

	private void etfu$tellComparators() {
		if (worldObj != null && !worldObj.isRemote && !etfu$reading) {
			int[] at = ModernFrames.frameBlock((EntityItemFrame) (Object) this);
			worldObj.func_147453_f(at[0], at[1], at[2], Blocks.air);
		}
	}

	@Inject(method = "writeEntityToNBT", at = @At("TAIL"))
	private void etfu$writeFacing(NBTTagCompound tag, CallbackInfo ci) {
		tag.setByte("Facing", (byte) ModernFrames.facing(hangingDirection));
	}

	/** After 1.7.10's own read (the turn, then the wall from `Direction`, set by `EntityHanging`). */
	@Inject(method = "readEntityFromNBT", at = @At("TAIL"))
	private void etfu$readFacing(NBTTagCompound tag, CallbackInfo ci) {
		if (tag.hasKey("Facing")) {
			int direction = ModernFrames.directionOfFacing(tag.getByte("Facing"));
			if (direction != hangingDirection) {
				setDirection(direction);
			}
		} else {
			setItemRotation(tag.getByte("ItemRotation") % 4 * 2);
		}
		etfu$reading = false;
	}
}
