package ganymedes01.etfuturum.mixins.early.unittas.client;

import ganymedes01.etfuturum.unittas.ITracedConnection;
import ganymedes01.etfuturum.unittas.TasTrace;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

/** unitTAS's loading trace (TasTrace): both ends of the local connection. Never required, so never in the way. */
@Mixin(NetworkManager.class)
public abstract class MixinNetworkManagerTrace implements ITracedConnection {

	@Shadow
	@Final
	private boolean isClientSide;
	@Shadow
	private Channel channel;

	private String etfu$side() {
		return isClientSide ? "client" : "server";
	}

	@Override
	public String etfu$traceDescription() {
		return "#" + Integer.toHexString(System.identityHashCode(this)) + " " + etfu$side() + ", channel "
			+ (channel == null ? "none yet" : channel.isOpen() ? "open" : "CLOSED");
	}

	@Inject(method = "<init>", at = @At("RETURN"), require = 0)
	private void etfu$made(boolean clientSide, CallbackInfo ci) {
		if (TasTrace.recording()) {
			TasTrace.note("connection " + etfu$traceDescription() + " made");
		}
	}

	@Inject(method = "channelActive", at = @At("TAIL"), remap = false, require = 0)
	private void etfu$active(ChannelHandlerContext context, CallbackInfo ci) {
		if (TasTrace.recording()) {
			TasTrace.note(etfu$side() + " channel active: " + channel + ", connection " + etfu$traceDescription());
		}
	}

	@Inject(method = "channelInactive", at = @At("HEAD"), remap = false, require = 0)
	private void etfu$inactive(ChannelHandlerContext context, CallbackInfo ci) {
		if (TasTrace.recording()) {
			TasTrace.note(etfu$side() + " channel INACTIVE, connection " + etfu$traceDescription());
		}
	}

	@Inject(method = "exceptionCaught", at = @At("HEAD"), remap = false, require = 0)
	private void etfu$exception(ChannelHandlerContext context, Throwable thrown, CallbackInfo ci) {
		if (TasTrace.recording()) {
			TasTrace.note(etfu$side() + " EXCEPTION: " + thrown);
		}
	}

	@Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), remap = false, require = 0)
	private void etfu$read(ChannelHandlerContext context, Packet packet, CallbackInfo ci) {
		if (TasTrace.recording()) {
			TasTrace.packet(isClientSide, false, packet, "");
		}
	}

	@Inject(method = "scheduleOutboundPacket", at = @At("HEAD"), require = 0)
	private void etfu$send(Packet packet, GenericFutureListener[] listeners, CallbackInfo ci) {
		if (TasTrace.recording()) {
			TasTrace.packet(isClientSide, true, packet, channel == null ? " (queued: no channel yet)" : channel.isOpen() ? "" : " (queued: channel closed)");
		}
	}

	@Inject(method = "processReceivedPackets", at = @At("HEAD"), require = 0)
	private void etfu$process(CallbackInfo ci) {
		if (TasTrace.recording()) {
			TasTrace.noteOnce("process " + etfu$side() + System.identityHashCode(this), etfu$side() + " processes its packets for the first time"
					+ (channel == null ? " (no channel yet)" : ""));
		}
	}

	@Inject(method = "provideLocalClient", at = @At("RETURN"), require = 0)
	private static void etfu$connected(SocketAddress address, CallbackInfoReturnable<NetworkManager> cir) {
		if (TasTrace.recording()) {
			TasTrace.note("client connected to " + address + ": " + ((MixinNetworkManagerTrace) (Object) cir.getReturnValue()).channel);
		}
	}
}
