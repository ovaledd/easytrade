package dev.easytrade.mixin;

import dev.easytrade.tracker.TradeWatcher;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantScreen.class)
public class MerchantScreenMixin {

	@Shadow
	private int scrollOff;

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void easytrade$pinOnRightClick(MouseButtonEvent event, boolean doubleClick,
		CallbackInfoReturnable<Boolean> cir) {
		if (event.button() != 1) {
			return;
		}
		MerchantScreen screen = (MerchantScreen) (Object) this;
		int xo = (screen.width - 176) / 2;
		int yo = (screen.height - 166) / 2;
		int mx = (int) event.x();
		int my = (int) event.y();
		for (int i = 0; i < 7; i++) {
			int bx = xo + 5;
			int by = yo + 18 + i * 20;
			if (mx >= bx && mx < bx + 88 && my >= by && my < by + 20) {
				MerchantOffers offers = screen.getMenu().getOffers();
				int index = i + this.scrollOff;
				if (index >= 0 && index < offers.size()) {
					TradeWatcher.pinOffer(offers.get(index));
				}
				cir.setReturnValue(true);
				return;
			}
		}
	}
}