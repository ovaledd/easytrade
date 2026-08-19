package dev.easytrade.mixin;

import dev.easytrade.tracker.TradeWatcher;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
public class AbstractContainerScreenMixin {

	@Shadow
	private RecipeBookComponent<?> recipeBookComponent;

	@Inject(method = "render", at = @At("TAIL"))
	private void easytrade$renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float delta,
		CallbackInfo ci) {
		if ((Object) this instanceof InventoryScreen) {
			Screen self = (Screen) (Object) this;
			int left = this.recipeBookComponent.updateScreenPosition(self.width, 176);
			TradeWatcher.renderInventoryOverlay(graphics, left, (self.height - 166) / 2, 176);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void easytrade$handleOverlayClick(MouseButtonEvent event, boolean doubleClicked,
		CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof InventoryScreen
			&& TradeWatcher.handleInventoryClick((int) event.x(), (int) event.y())) {
			cir.setReturnValue(true);
		}
	}
}