package dev.easytrade.mixin;

import dev.easytrade.tracker.TradeWatcher;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class ContainerScreenToastMixin {

	@Inject(method = "render", at = @At("TAIL"))
	private void easytrade$renderPinToast(GuiGraphics graphics, int mouseX, int mouseY, float delta,
		CallbackInfo ci) {
		if ((Object) this instanceof MerchantScreen) {
			TradeWatcher.renderToast(graphics);
		}
	}
}