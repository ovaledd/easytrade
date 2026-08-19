package dev.easytrade.render;

import dev.easytrade.tracker.PeekData;
import dev.easytrade.util.ScreenPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class PanelRenderer {

	public static final int PANEL_WIDTH = 176;
	public static final Identifier TOOLTIP_BACKGROUND = Identifier.withDefaultNamespace("tooltip/background");
	public static final Identifier TOOLTIP_FRAME = Identifier.withDefaultNamespace("tooltip/frame");
	public static final int COLOR_TITLE = 0xFFF2F2F5;
	public static final int COLOR_DETAIL = 0xFFE9E9EF;
	public static final int COLOR_MATCH = 0xFF4ADE80;
	public static final int TINT_MATCH = 0xA8F0C5;
	public static final float BG_ALPHA = 0.30f;
	public static final float FRAME_ALPHA = 0.65f;

	private static final int PAD = 8;
	private static final int TITLE_H = 11;
	private static final int DETAIL_H = 18;
	private static final int COST_H = 9;
	private static final int CELL_MAX = 24;
	private static final int CELL_MIN = 8;
	private static final int CELL_GAP = 2;
	private static final int MAX_CELLS = 16;
	private static final long FADE_START = 30;
	private static final long FADE_END = 40;
	private static final long FADE_IN_TICKS = 5;

	private static final int COLOR_BORDER = 0xFFA9A9B6;

	private static boolean wasVisible = false;
	private static long appearTick = Long.MIN_VALUE;

	private PanelRenderer() {
	}

	public static void render(GuiGraphics graphics, Minecraft mc, PeekData data, Vec3 anchor) {
		ScreenPos pos = ScreenPos.project(mc, anchor);
		if (pos == null) {
			wasVisible = false;
			return;
		}

		int w = PANEL_WIDTH;
		int gw = graphics.guiWidth();
		int gh = graphics.guiHeight();
		if (pos.x() < -120 || pos.x() > gw + 120 || pos.y() < -120 || pos.y() > gh + 120) {
			wasVisible = false;
			return;
		}

		long now = mc.level.getGameTime();
		long elapsed = now - data.seenAtTick();
		if (elapsed > FADE_END) {
			wasVisible = false;
			return;
		}

		if (!wasVisible) {
			wasVisible = true;
			appearTick = now;
		}

		float fadeIn = clamp01((now - appearTick) / (float) FADE_IN_TICKS);
		float fade = 1.0f;
		if (elapsed > FADE_START) {
			float t = clamp01((FADE_END - elapsed) / (float) (FADE_END - FADE_START));
			fade = 1.0f - (1.0f - t) * (1.0f - t);
		}
		float totalAlpha = fade * fadeIn;
		int alpha = (int) (totalAlpha * 255.0f);
		if (alpha <= 2) {
			return;
		}

		float baseX = graphics.pose().m00();
		float baseY = graphics.pose().m11();
		double guiScale = (double) mc.getWindow().getWidth() / mc.getWindow().getGuiScaledWidth();
		float s = (float) (2.0 / (guiScale * Math.max(1.0E-4, baseX)));

		int x = (int) (pos.x() - w * s / 2.0f);
		x = Math.max(4, Math.min(x, gw - (int) (w * s) - 4));
		int y = (int) (pos.y() - cardHeight(data) * s - 4);
		if (y < 4) {
			y = (int) pos.y() + 4;
		}
		y = Math.max(4, Math.min(y, gh - (int) (cardHeight(data) * s) - 4));

		drawCard(graphics, mc, data, x, y, s, baseX, baseY, alpha, data.matched() ? TINT_MATCH : 0xFFFFFF);
	}

	private static int cardHeight(PeekData data) {
		int count = Math.min(data.lines().size(), MAX_CELLS);
		int cell = CELL_MIN;
		if (count > 0) {
			cell = Math.max(CELL_MIN, Math.min(CELL_MAX, (PANEL_WIDTH - 2 * PAD - CELL_GAP * (count - 1)) / count));
			while (count > 1 && cell * count + CELL_GAP * (count - 1) > PANEL_WIDTH - 2 * PAD) {
				count--;
				cell = Math.max(CELL_MIN, Math.min(CELL_MAX, (PANEL_WIDTH - 2 * PAD - CELL_GAP * (count - 1)) / count));
			}
		}
		int cellsH = count > 0 ? cell + 4 : 0;
		boolean showCaptions = false;
		for (PeekData.OfferLine line : data.lines()) {
			if (line.match()) {
				showCaptions = true;
				break;
			}
		}
		int captionsH = showCaptions ? 7 : 0;
		boolean showCost = !data.price().isEmpty();
		return PAD + TITLE_H + DETAIL_H + 2 + (showCost ? COST_H : 0) + (cellsH > 0 ? 4 : 0) + cellsH + captionsH + PAD - 2;
	}

	private static void drawCard(GuiGraphics graphics, Minecraft mc, PeekData data, int x, int y, float s,
		float baseX, float baseY, int alpha, int frameTint) {
		int w = PANEL_WIDTH;
		int h = cardHeight(data);

		graphics.pose().pushMatrix();
		graphics.pose().translate(x / baseX, y / baseY);
		graphics.pose().scale(s, s);

		int bgColor = ((int) (alpha * BG_ALPHA) << 24) | 0xFFFFFF;
		int frameColor = ((int) (alpha * FRAME_ALPHA) << 24) | frameTint;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TOOLTIP_BACKGROUND, 0, 0, w, h, bgColor);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TOOLTIP_FRAME, 0, 0, w, h, frameColor);

		int yCur = PAD;

		String title = data.title();
		graphics.drawString(mc.font, title, w / 2 - mc.font.width(title) / 2, yCur,
			withAlpha(data.matched() ? COLOR_MATCH : COLOR_TITLE, alpha));
		yCur += TITLE_H;

		String prefix = data.matched() ? "MATCH  " : "";
		String levelText = data.levelText();
		String itemName = data.itemName();
		int textW = mc.font.width(prefix + levelText) + mc.font.width(itemName);
		int startX = w / 2 - (16 + 4 + textW) / 2;
		if (startX < PAD) {
			startX = PAD;
		}
		int avail = w - PAD - startX - 20 - mc.font.width(prefix) - mc.font.width(levelText);
		if (avail >= 10 && mc.font.width(itemName) > avail) {
			itemName = mc.font.plainSubstrByWidth(itemName, avail - mc.font.width("...")) + "...";
		}
		graphics.renderItem(data.mainIcon(), startX, yCur - 4);
		int tx = startX + 20;
		if (!prefix.isEmpty()) {
			graphics.drawString(mc.font, prefix, tx, yCur + 1, withAlpha(COLOR_MATCH, alpha));
			tx += mc.font.width(prefix);
		}
		if (!levelText.isEmpty()) {
			graphics.drawString(mc.font, levelText, tx, yCur + 1, withAlpha(COLOR_DETAIL, alpha));
			tx += mc.font.width(levelText);
		}
		int nameColor;
		if (data.matched()) {
			nameColor = withAlpha(COLOR_MATCH, alpha);
		} else if (data.mainIcon().is(Items.ENCHANTED_BOOK)) {
			nameColor = rainbowColor(mc.level.getGameTime(), alpha);
		} else {
			nameColor = withAlpha(COLOR_DETAIL, alpha);
		}
		graphics.drawString(mc.font, itemName, tx, yCur + 1, nameColor);
		yCur += DETAIL_H + 2;

		if (!data.price().isEmpty()) {
			String price = data.price();
			graphics.drawString(mc.font, price, w / 2 - mc.font.width(price) / 2, yCur, withAlpha(COLOR_MATCH, alpha));
			yCur += COST_H;
		}

		List<PeekData.OfferLine> lines = data.lines();
		int count = Math.min(lines.size(), MAX_CELLS);
		int cell = CELL_MIN;
		if (count > 0) {
			cell = Math.max(CELL_MIN, Math.min(CELL_MAX, (w - 2 * PAD - CELL_GAP * (count - 1)) / count));
			while (count > 1 && cell * count + CELL_GAP * (count - 1) > w - 2 * PAD) {
				count--;
				cell = Math.max(CELL_MIN, Math.min(CELL_MAX, (w - 2 * PAD - CELL_GAP * (count - 1)) / count));
			}
		}
		if (count > 0) {
			yCur += 4;
			int total = cell * count + CELL_GAP * (count - 1);
			int cellsX = (w - total) / 2;
			for (int i = 0; i < count; i++) {
				PeekData.OfferLine line = lines.get(i);
				int cx = cellsX + i * (cell + CELL_GAP);
				int cy = yCur;
				int border = line.match() ? COLOR_MATCH : COLOR_BORDER;
				graphics.fill(cx, cy, cx + cell, cy + 1, withAlpha(border, alpha));
				graphics.fill(cx, cy + cell - 1, cx + cell, cy + cell, withAlpha(border, alpha));
				graphics.fill(cx, cy, cx + 1, cy + cell, withAlpha(border, alpha));
				graphics.fill(cx + cell - 1, cy, cx + cell, cy + cell, withAlpha(border, alpha));
				graphics.fill(cx + 1, cy + 1, cx + cell - 1, cy + cell - 1, ((int) (alpha * 0.10f) << 24) | 0xFFFFFF);
				graphics.pose().pushMatrix();
				graphics.pose().translate(cx + cell / 2.0f, cy + cell / 2.0f);
				graphics.pose().scale((cell - 2) / 16.0f);
				graphics.renderItem(line.output(), -8, -8);
				graphics.pose().popMatrix();
			}
			boolean showCaptions = false;
			for (PeekData.OfferLine line : lines) {
				if (line.match()) {
					showCaptions = true;
					break;
				}
			}
			if (showCaptions) {
				int cy = yCur + cell + 2;
				for (int i = 0; i < count; i++) {
					PeekData.OfferLine line = lines.get(i);
					if (!line.match() || line.price().isEmpty()) {
						continue;
					}
					int cx = cellsX + i * (cell + CELL_GAP);
					int cw = mc.font.width(line.price());
					int px = cx + cell / 2 - cw / 2;
					px = Math.max(PAD, Math.min(px, w - PAD - cw));
					graphics.drawString(mc.font, line.price(), px, cy, withAlpha(COLOR_MATCH, alpha));
				}
			}
		}

		graphics.pose().popMatrix();
	}

	public static int rainbowColor(long tick, int alpha) {
		float hue = ((tick % 60) / 60.0f) * 6.0f;
		int i = (int) hue;
		float f = hue - i;
		float q = 1.0f - f;
		float r;
		float g;
		float b;
		switch (i % 6) {
			case 0 -> {
				r = 1.0f;
				g = f;
				b = 0.0f;
			}
			case 1 -> {
				r = q;
				g = 1.0f;
				b = 0.0f;
			}
			case 2 -> {
				r = 0.0f;
				g = 1.0f;
				b = f;
			}
			case 3 -> {
				r = 0.0f;
				g = q;
				b = 1.0f;
			}
			case 4 -> {
				r = f;
				g = 0.0f;
				b = 1.0f;
			}
			default -> {
				r = 1.0f;
				g = 0.0f;
				b = q;
			}
		}
		int rgb = ((int) (r * 255.0f) << 16) | ((int) (g * 255.0f) << 8) | (int) (b * 255.0f);
		return (alpha << 24) | rgb;
	}

	private static float clamp01(float v) {
		return Math.max(0.0f, Math.min(1.0f, v));
	}

	private static int withAlpha(int color, int alpha) {
		return (alpha << 24) | (color & 0xFFFFFF);
	}
}