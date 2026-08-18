package dev.easytrade.mixin;

import dev.easytrade.tracker.TradeWatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

	@Inject(method = "handleMerchantOffers", at = @At("TAIL"))
	private void easytrade$onMerchantOffers(ClientboundMerchantOffersPacket packet, CallbackInfo ci) {
		TradeWatcher.onOffersReceived();
	}

	@Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
	private void easytrade$protectOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (!TradeWatcher.isPeeking() || packet.getType() != MenuType.MERCHANT || mc.player == null) {
			return;
		}
		TradeWatcher.openPeekContainer(packet.getContainerId());
		ci.cancel();
	}
}