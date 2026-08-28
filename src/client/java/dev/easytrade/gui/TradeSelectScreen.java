package dev.easytrade.gui;

import dev.easytrade.config.DesiredTrade;
import dev.easytrade.config.ModConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TradeSelectScreen extends Screen {

	private static final int RESULTS_CAP = 48;
	private static final int ROW_HEIGHT = 16;
	private static final int COLOR_TITLE = 0xFFFFFFFF;
	private static final int COLOR_HEADER = 0xFFA8A8B3;
	private static final int COLOR_TEXT = 0xFFE4E4E9;
	private static final int COLOR_MUTED = 0xFF70707A;
	private static final int COLOR_X = 0xFFFF6B6B;

	private EditBox searchBox;
	private String query = "";
	private List<ResultEntry> results = new ArrayList<>();
	private List<ResultEntry> allResults = new ArrayList<>();
	private Button clearAllButton;

	private record ResultEntry(String label, DesiredTrade trade, ItemStack icon) {
	}

	public TradeSelectScreen() {
		super(Component.literal("Easy Trade"));
	}

	@Override
	protected void init() {
		this.searchBox = new EditBox(this.font, this.width / 2 - 130, 26, 260, 18, Component.translatable("easytrade.search.hint"));
		this.searchBox.setMaxLength(64);
		this.searchBox.setHint(Component.translatable("easytrade.search.hint"));
		this.searchBox.setResponder(q -> {
			this.query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
			this.filterResults();
		});
		this.addRenderableWidget(this.searchBox);

		this.clearAllButton = Button.builder(Component.translatable("easytrade.screen.clear_all"), b -> {
			ModConfig.INSTANCE.desiredTrades.clear();
			ModConfig.save();
		}).bounds(this.width - 80, 4, 76, 18).build();
		this.addRenderableWidget(this.clearAllButton);
		this.clearAllButton.visible = !ModConfig.INSTANCE.desiredTrades.isEmpty();

		this.refreshAllResults();
		this.filterResults();
	}

	@Override
	protected void setInitialFocus() {
		this.setInitialFocus(this.searchBox);
	}

	private void refreshAllResults() {
		allResults.clear();
		Minecraft mc = Minecraft.getInstance();

		for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
			if (id.getPath().equals("air")) continue;
			var item = BuiltInRegistries.ITEM.getValue(id);
			if (item == Items.AIR) continue;
			ItemStack stack = new ItemStack(item);
			String label = stack.getHoverName().getString();
			allResults.add(new ResultEntry(label, new DesiredTrade("item", id.toString(), 0), stack));
		}

		if (mc.level != null) {
			mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(holder -> {
				Identifier id = holder.key().identifier();
				String label = holder.value().description().getString();
				ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
				allResults.add(new ResultEntry(label, new DesiredTrade("enchantment", id.toString(), 0), book));
			});
		}
	}

	private void filterResults() {
		results.clear();
		for (ResultEntry entry : allResults) {
			if (matchesQuery(entry)) {
				results.add(entry);
			}
		}
	}

	private boolean matchesQuery(ResultEntry entry) {
		if (query.isEmpty()) return true;
		return entry.trade().id().toLowerCase(Locale.ROOT).contains(query) ||
			entry.label().toLowerCase(Locale.ROOT).contains(query);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, this.width, this.height, 0x9910141A);

		super.extractRenderState(graphics, mouseX, mouseY, delta);

		Component title = Component.translatable("easytrade.screen.title");
		graphics.text(this.font, title, this.width / 2 - this.font.width(title) / 2, 8, COLOR_TITLE);

		int colX = 16;
		int colW = 300;
		Component wantedHeader = Component.translatable("easytrade.screen.wanted");
		graphics.text(this.font, wantedHeader, colX, 50, COLOR_HEADER);
		int y = 66;
		for (DesiredTrade trade : ModConfig.INSTANCE.desiredTrades) {
			graphics.text(this.font, Component.literal("X"), colX + 4, y + 1, COLOR_X);
			graphics.text(this.font, Component.literal(labelOf(trade)), colX + 20, y + 2, COLOR_TEXT);
			y += ROW_HEIGHT;
		}
		if (ModConfig.INSTANCE.desiredTrades.isEmpty()) {
			String none = Component.translatable("easytrade.screen.none").getString();
			graphics.text(this.font, none, colX + colW / 2 - this.font.width(none) / 2, y + 2, COLOR_MUTED);
		}

		int resX = colX + colW + 24;
		int resW = this.width - resX - 16;
		Component resultsHeader = Component.translatable("easytrade.screen.results");
		graphics.text(this.font, resultsHeader, resX, 50, COLOR_HEADER);
		if (results.isEmpty()) {
			String hint = query.isEmpty()
				? Component.translatable("easytrade.screen.typehint").getString()
				: Component.translatable("easytrade.screen.nomatch").getString();
			graphics.text(this.font, hint, resX + resW / 2 - this.font.width(hint) / 2, 70, COLOR_MUTED);
		} else {
			int ry = 66;
			for (ResultEntry entry : results) {
				graphics.item(entry.icon(), resX, ry);
				graphics.text(this.font, Component.literal(entry.label()), resX + 20, ry + 4, COLOR_TEXT);
				ry += ROW_HEIGHT;
			}
		}

		String hint = Component.translatable("easytrade.screen.hint").getString();
		graphics.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2, this.height - 14, COLOR_MUTED);

		Component pinnedHint = Component.translatable("easytrade.screen.pinned_hint", ModConfig.INSTANCE.getMaxPins());
		graphics.text(this.font, pinnedHint, this.width / 2 - this.font.width(pinnedHint) / 2, this.height - 28, COLOR_MUTED);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
		if (event.buttonInfo().button() == 0) {
			int mx = (int) event.x();
			int my = (int) event.y();

			int colX = 16;
			int y = 66;
			int idx = 0;
			List<DesiredTrade> wanted = ModConfig.INSTANCE.desiredTrades;
			for (DesiredTrade trade : wanted) {
				if (mx >= colX && mx <= colX + 16 && my >= y && my <= y + ROW_HEIGHT) {
					wanted.remove(idx);
					ModConfig.save();
					clearAllButton.visible = !wanted.isEmpty();
					return true;
				}
				idx++;
				y += ROW_HEIGHT;
			}

			int resX = colX + 324;
			int ry = 66;
			for (ResultEntry entry : results) {
				if (mx >= resX && mx <= resX + 260 && my >= ry && my <= ry + ROW_HEIGHT) {
					if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT)) {
						removeWanted(entry.trade());
					} else {
						addWanted(entry.trade());
					}
					return true;
				}
				ry += ROW_HEIGHT;
			}
		}
		return super.mouseClicked(event, doubleClicked);
	}

	private void addWanted(DesiredTrade trade) {
		List<DesiredTrade> wanted = ModConfig.INSTANCE.desiredTrades;
		for (DesiredTrade existing : wanted) {
			if (existing.type().equals(trade.type()) && existing.id().equals(trade.id())) {
				return;
			}
		}
		wanted.add(trade);
		ModConfig.save();
		clearAllButton.visible = true;
	}

	private void removeWanted(DesiredTrade trade) {
		List<DesiredTrade> wanted = ModConfig.INSTANCE.desiredTrades;
		wanted.removeIf(t -> t.type().equals(trade.type()) && t.id().equals(trade.id()));
		ModConfig.save();
		clearAllButton.visible = !wanted.isEmpty();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ENTER && !results.isEmpty()) {
			if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT)) {
				removeWanted(results.get(0).trade());
			} else {
				addWanted(results.get(0).trade());
			}
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		ModConfig.save();
		super.onClose();
	}

	private static String labelOf(DesiredTrade trade) {
		if (trade.type().equals("enchantment")) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != null) {
				var lookup = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
				var holder = lookup.get(Identifier.parse(trade.id()));
				if (holder.isPresent()) {
					return holder.get().value().description().getString();
				}
			}
			return trade.id();
		}
		var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(trade.id()));
		if (item != Items.AIR) {
			return new ItemStack(item).getHoverName().getString();
		}
		return trade.id();
	}
}