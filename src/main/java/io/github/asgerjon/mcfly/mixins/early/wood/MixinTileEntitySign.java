package io.github.asgerjon.mcfly.mixins.early.wood;

import io.github.asgerjon.mcfly.wood.SignSide;
import io.github.asgerjon.mcfly.wood.Signs;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * 26.3's sign state on every `TileEntitySign` (SPEC-modern-movement.md 91.8; `SignBlockEntity`; the fork's spec 67.2
 * look, rebuilt): two sides, the wax, the player who may edit it.
 * - **Saved:** the front as 1.7.10's `Text1` to `Text4` with the fork's `Color`, `GlowingText` and `Waxed`; the back as
 *   26.3's `back_text`. Absent: black, plain, not waxed, the back empty. The lines are read whole (1.7.10 cut them at 15).
 * - **Sent** to clients as a tile entity packet with all of it (1.7.10's `S33PacketUpdateSign` carries the front's lines
 *   alone, 15 characters each).
 * - **Ticked:** the editor cleared once gone or out of reach (26.3's `SignBlockEntity.tick`).
 * The editor isn't saved (26.3's neither).
 */
@Mixin(TileEntitySign.class)
public abstract class MixinTileEntitySign extends TileEntity implements Signs.Sided {

	@Shadow
	public String[] signText;

	/**
	 * Made when first wanted: a mixin's field initialisers aren't copied into the target's constructor when javac puts the
	 * implicit constructor's return on an initialiser's line (Mixin then reads them as the constructor's body).
	 */
	@Unique
	private SignSide mcfly$front;
	@Unique
	private SignSide mcfly$back;
	@Unique
	private boolean mcfly$waxed;
	@Unique
	private UUID mcfly$editor;

	@Override
	public SignSide mcfly$side(boolean front) {
		if (front) {
			if (mcfly$front == null) {
				mcfly$front = new SignSide(signText);
			}
			mcfly$front.lines = signText;
			return mcfly$front;
		}
		if (mcfly$back == null) {
			mcfly$back = SignSide.empty();
		}
		return mcfly$back;
	}

	@Override
	public boolean mcfly$isWaxed() {
		return mcfly$waxed;
	}

	@Override
	public boolean mcfly$setWaxed(boolean waxed) {
		if (mcfly$waxed == waxed) {
			return false;
		}
		mcfly$waxed = waxed;
		return true;
	}

	@Override
	public UUID mcfly$editor() {
		return mcfly$editor;
	}

	@Override
	public void mcfly$setEditor(UUID player) {
		mcfly$editor = player;
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		Signs.tick((TileEntitySign) (Object) this);
	}

	@Inject(method = "writeToNBT", at = @At("TAIL"))
	private void mcfly$writeSides(NBTTagCompound tag, CallbackInfo ci) {
		mcfly$writeLook(tag);
	}

	@Inject(method = "readFromNBT", at = @At("TAIL"))
	private void mcfly$readSides(NBTTagCompound tag, CallbackInfo ci) {
		mcfly$readAll(tag);
	}

	/** The front's colour, glow and wax (the fork's keys) and the back (26.3's `back_text`). */
	@Unique
	private void mcfly$writeLook(NBTTagCompound tag) {
		SignSide front = mcfly$side(true);
		tag.setString("Color", front.color);
		tag.setBoolean("GlowingText", front.glowing);
		tag.setBoolean("Waxed", mcfly$waxed);
		tag.setTag("back_text", mcfly$side(false).write());
	}

	/** The front's lines whole, its look, the wax and the back. */
	@Unique
	private void mcfly$readAll(NBTTagCompound tag) {
		for (int i = 0; i < Signs.LINES; i++) {
			signText[i] = tag.getString("Text" + (i + 1));
		}
		SignSide front = mcfly$side(true);
		front.setColor(tag.getString("Color"));
		front.glowing = tag.getBoolean("GlowingText");
		mcfly$waxed = tag.getBoolean("Waxed");
		if (tag.hasKey("back_text", 10)) {
			mcfly$side(false).read(tag.getCompoundTag("back_text"));
		} else {
			mcfly$side(false).clear();
		}
	}

	@Inject(method = "getDescriptionPacket", at = @At("HEAD"), cancellable = true)
	private void mcfly$sendBothSides(CallbackInfoReturnable<Packet> cir) {
		NBTTagCompound tag = new NBTTagCompound();
		for (int i = 0; i < Signs.LINES; i++) {
			tag.setString("Text" + (i + 1), signText[i] == null ? "" : signText[i]);
		}
		mcfly$writeLook(tag);
		cir.setReturnValue(new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 9, tag));
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		mcfly$readAll(packet.func_148857_g());
	}
}
