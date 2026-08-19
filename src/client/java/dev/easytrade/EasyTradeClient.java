package dev.easytrade;

import dev.easytrade.config.ModConfig;
import dev.easytrade.gui.TradeSelectScreen;
import dev.easytrade.tracker.TradeWatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class EasyTradeClient implements ClientModInitializer {

	public static final String MOD_ID = "easytrade";

	private static KeyMapping openMenuKey;

	@Override
	public void onInitializeClient() {
		ModConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "category"));
		openMenuKey = new KeyMapping("key.easytrade.menu", GLFW.GLFW_KEY_F7, category);
		KeyBindingHelper.registerKeyBinding(openMenuKey);

		ClientTickEvents.END_CLIENT_TICK.register(EasyTradeClient::onTick);
		HudElementRegistry.attachElementAfter(
			VanillaHudElements.SUBTITLES,
			Identifier.fromNamespaceAndPath(MOD_ID, "overlay"),
			(graphics, deltaTracker) -> TradeWatcher.render(graphics));
	}

	private static void onTick(Minecraft client) {
		if (openMenuKey.consumeClick()) {
			client.setScreenAndShow(new TradeSelectScreen());
		}
		TradeWatcher.tick(client);
	}
}