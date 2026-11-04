package io.github.asgerjon.mcfly.mixins.early.signs;

import ganymedes01.etfuturum.signs.ISignText;
import ganymedes01.etfuturum.signs.ModernSigns;
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

/**
 * A sign's colour, glow and wax (SPEC-modern-movement.md §67.2), on every `TileEntitySign` (EFR's wood signs too):
 * saved as `Color`, `GlowingText`, `Waxed` (absent: black, not glowing, not waxed, as old signs read), and sent to
 * clients with the lines as a tile entity packet (1.7.10's `S33PacketUpdateSign` carries only the lines). The client
 * takes the lines and the look from it, nothing else (not the sign's editable flag, which its edit screen uses).
 */
@Mixin(TileEntitySign.class)
public abstract class MixinTileEntitySign extends TileEntity implements ISignText {

	@Shadow
	public String[] signText;

	@Unique
	private String etfu$color = ModernSigns.BLACK;
	@Unique
	private boolean etfu$glowing;
	@Unique
	private boolean etfu$waxed;

	@Override
	public String etfu$getColor() {
		return etfu$color;
	}

	@Override
	public void etfu$setColor(String color) {
		etfu$color = color == null || color.isEmpty() ? ModernSigns.BLACK : color;
	}

	@Override
	public boolean etfu$isGlowing() {
		return etfu$glowing;
	}

	@Override
	public void etfu$setGlowing(boolean glowing) {
		etfu$glowing = glowing;
	}

	@Override
	public boolean etfu$isWaxed() {
		return etfu$waxed;
	}

	@Override
	public void etfu$setWaxed(boolean waxed) {
		etfu$waxed = waxed;
	}

	@Inject(method = "writeToNBT", at = @At("TAIL"))
	private void etfu$writeLook(NBTTagCompound tag, CallbackInfo ci) {
		etfu$writeLookTo(tag);
	}

	@Inject(method = "readFromNBT", at = @At("TAIL"))
	private void etfu$readLook(NBTTagCompound tag, CallbackInfo ci) {
		etfu$readLookFrom(tag);
	}

	@Unique
	private void etfu$writeLookTo(NBTTagCompound tag) {
		tag.setString("Color", etfu$color);
		tag.setBoolean("GlowingText", etfu$glowing);
		tag.setBoolean("Waxed", etfu$waxed);
	}

	@Unique
	private void etfu$readLookFrom(NBTTagCompound tag) {
		etfu$setColor(tag.getString("Color"));
		etfu$glowing = tag.getBoolean("GlowingText");
		etfu$waxed = tag.getBoolean("Waxed");
	}

	@Inject(method = "getDescriptionPacket", at = @At("HEAD"), cancellable = true)
	private void etfu$sendWithTheLook(CallbackInfoReturnable<Packet> cir) {
		NBTTagCompound tag = new NBTTagCompound();
		for (int i = 0; i < 4; i++) {
			tag.setString("Text" + (i + 1), signText[i]);
		}
		etfu$writeLookTo(tag);
		cir.setReturnValue(new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 9, tag));
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		NBTTagCompound tag = packet.func_148857_g();
		for (int i = 0; i < 4; i++) {
			signText[i] = tag.getString("Text" + (i + 1));
		}
		etfu$readLookFrom(tag);
	}
}
