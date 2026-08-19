package dev.easytrade.tracker;

import dev.easytrade.config.DesiredTrade;
import dev.easytrade.config.ModConfig;
import dev.easytrade.render.PanelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TradeWatcher {

	private static final double MAX_RANGE = 3.0;
	private static final double MAX_ANGLE_COS = Math.cos(Math.toRadians(13.0));
	private static final int PEEK_TIMEOUT_TICKS = 60;
	private static final int DISPLAY_FADE_TICKS = 40;
	private static final int MAX_CELLS = 16;
	private static final int MANUAL_SUPPRESS_TICKS = 10;
	private static final int MAX_PINS = 4;
	private static final int TOAST_TICKS = 60;
	private static final int TOAST_FADE_TICKS = 20;
	private static final int COLOR_TOAST_GREEN = 0x55FF55;
	private static final int COLOR_TOAST_MAROON = 0x8B0000;

	private static boolean peekMode = false;
	private static boolean peekInteract = false;
	private static long manualInteractTick = -100;

	public static boolean isPeeking() {
		return peekMode;
	}

	public static boolean isPeekInteract() {
		return peekInteract;
	}

	public static void onPlayerInteract() {
		peekMode = false;
		peekTicks = 0;
		pendingVillager = null;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			manualInteractTick = mc.level.getGameTime();
		}
	}

	public static void openPeekContainer(int containerId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		mc.player.containerMenu = new MerchantMenu(containerId, mc.player.getInventory());
	}

	private static void closePeekContainer(LocalPlayer player) {
		if (player.containerMenu instanceof MerchantMenu menu) {
			player.connection.send(new ServerboundContainerClosePacket(menu.containerId));
			player.containerMenu = player.inventoryMenu;
		}
	}

	private static int peekTicks = 0;
	private static AbstractVillager pendingVillager = null;
	private static int pollCounter = 0;
	private static PeekData data = null;
	private static Vec3 anchor = null;
	private static int currentVillagerId = -1;
	private static long lastPeekTick = -100;
	private static final Set<String> alertedMatches = new HashSet<>();

	private record PinnedTrade(int villagerId, String signature, PeekData data) {
	}

	private record InvButton(int x, int y, int w, int h, int index) {
	}

	private static final List<PinnedTrade> pinnedTrades = new ArrayList<>();
	private static final List<InvButton> invButtons = new ArrayList<>();
	private static String toastText = null;
	private static int toastColor = COLOR_TOAST_GREEN;
	private static long toastExpireTick = 0;

	private TradeWatcher() {
	}

	private static void showToast(String text, int color) {
		toastText = text;
		toastColor = color;
		Minecraft mc = Minecraft.getInstance();
		toastExpireTick = mc.level != null ? mc.level.getGameTime() + TOAST_TICKS : 0;
	}

	public static void tick(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			reset();
			return;
		}
		LocalPlayer player = client.player;

		if (peekMode) {
			peekTicks++;
			if (peekTicks > PEEK_TIMEOUT_TICKS) {
				peekMode = false;
				pendingVillager = null;
				closePeekContainer(player);
			}
			return;
		}

		if (client.screen != null) {
			return;
		}
		if (player.isShiftKeyDown() || player.isSpectator() || player.isDeadOrDying()) {
			return;
		}
		if (player.containerMenu != player.inventoryMenu) {
			return;
		}
		if (client.level.getGameTime() - manualInteractTick < MANUAL_SUPPRESS_TICKS) {
			return;
		}

		AbstractVillager target = findTarget(client, player);
		if (target == null) {
			return;
		}

		int interval = (data != null && currentVillagerId != target.getId())
			? 1
			: ModConfig.INSTANCE.pollIntervalTicks;
		if (++pollCounter < interval) {
			return;
		}
		pollCounter = 0;
		if (data == null && lastPeekTick >= 0 && client.level.getGameTime() - lastPeekTick < 2) {
			return;
		}

		if (client.gameMode == null) {
			return;
		}
		peekMode = true;
		peekTicks = 0;
		pendingVillager = target;
		lastPeekTick = client.level.getGameTime();
		peekInteract = true;
		client.gameMode.interact(player, target, InteractionHand.MAIN_HAND);
		peekInteract = false;
	}

	private static AbstractVillager findTarget(Minecraft client, LocalPlayer player) {
		Vec3 eye = player.getEyePosition();
		AABB box = new AABB(eye, eye).inflate(MAX_RANGE);
		List<AbstractVillager> villagers = client.level.getEntities(
			EntityTypeTest.forClass(AbstractVillager.class),
			box,
			v -> v.isAlive() && !v.isRemoved());
		Vec3 view = player.getViewVector(1.0f);
		AbstractVillager best = null;
		double bestScore = Double.MAX_VALUE;
		for (AbstractVillager v : villagers) {
			Vec3 to = v.getEyePosition().subtract(eye).normalize();
			double cos = to.dot(view);
			if (cos < MAX_ANGLE_COS) {
				continue;
			}
			double dist = v.getEyePosition().distanceToSqr(eye);
			if (dist > MAX_RANGE * MAX_RANGE) {
				continue;
			}
			double score = dist / cos;
			if (score < bestScore) {
				bestScore = score;
				best = v;
			}
		}
		return best;
	}

	public static void onOffersReceived() {
		if (!peekMode) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) {
			return;
		}
		peekMode = false;
		peekTicks = 0;
		AbstractVillager villager = pendingVillager;
		pendingVillager = null;
		if (player.containerMenu instanceof MerchantMenu menu) {
			processOffers(menu.getOffers(), menu.getTraderLevel(), player, villager);
		}
		closePeekContainer(player);
	}

	private static boolean isBookTrade(MerchantOffer offer) {
		ItemStack result = offer.getResult();
		return result.is(Items.ENCHANTED_BOOK) || result.is(Items.BOOK);
	}

	private static void processOffers(MerchantOffers offers, int merchantLevel, LocalPlayer player, AbstractVillager villager) {
		if (villager == null || offers.isEmpty()) {
			clearDisplay();
			return;
		}
		ModConfig cfg = ModConfig.INSTANCE;
		String title = TradeNames.villagerTitle(villager);
		List<PeekData.OfferLine> lines = new ArrayList<>();
		List<MerchantOffer> matchedOffers = new ArrayList<>();

		List<MerchantOffer> ordered = new ArrayList<>();
		for (MerchantOffer offer : offers) {
			if (isBookTrade(offer)) {
				ordered.add(offer);
			}
		}
		for (MerchantOffer offer : offers) {
			if (!isBookTrade(offer)) {
				ordered.add(offer);
			}
		}

		MerchantOffer main = ordered.get(0);
		if (wants(main, cfg, player, villager)) {
			matchedOffers.add(main);
		}

		String levelText = merchantLevel > 0 ? "Level " + merchantLevel + "  " : "";
		String itemName = TradeNames.offerTitle(main);
		ItemStack mainIcon = main.getResult();
		String mainCost = TradeNames.costTotal(List.of(main));
		ItemStack costA = main.getBaseCostA();
		ItemStack costB = main.getCostB();

		for (int i = 1; i < ordered.size() && lines.size() < MAX_CELLS; i++) {
			MerchantOffer offer = ordered.get(i);
			boolean m = wants(offer, cfg, player, villager);
			if (m) {
				matchedOffers.add(offer);
			}
			lines.add(new PeekData.OfferLine(TradeNames.offerTitle(offer), offer.getResult(), m,
				m ? TradeNames.emeraldCost(offer) : ""));
		}

		String price = matchedOffers.isEmpty() ? "" : TradeNames.costTotal(matchedOffers);

		if (alertedMatches.size() > 1000) {
			alertedMatches.clear();
		}
		anchor = villager.position().add(0, villager.getBbHeight() + 0.5, 0);
		currentVillagerId = villager.getId();
		data = new PeekData(title, levelText, itemName, price, mainCost, mainIcon, costA, costB, lines,
			!matchedOffers.isEmpty(), player.level().getGameTime());
	}

	private static boolean wants(MerchantOffer offer, ModConfig cfg, LocalPlayer player, AbstractVillager villager) {
		for (DesiredTrade want : cfg.desiredTrades) {
			if (want.matches(offer.getResult())) {
				String sigKey = villager.getUUID() + "|" + TradeNames.offerSignature(offer);
				if (cfg.alertSound && alertedMatches.add(sigKey)) {
					player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
				}
				return true;
			}
		}
		return false;
	}

	public static void pinOffer(MerchantOffer offer) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}
		AbstractVillager villager = findTarget(mc, mc.player);
		if (villager == null && currentVillagerId >= 0) {
			Entity e = mc.level.getEntity(currentVillagerId);
			if (e instanceof AbstractVillager v && v.isAlive()) {
				villager = v;
			}
		}
		if (villager == null) {
			return;
		}
		String signature = TradeNames.offerSignature(offer);
		for (PinnedTrade pin : pinnedTrades) {
			if (pin.villagerId() == villager.getId() && pin.signature().equals(signature)) {
				return;
			}
		}
		if (pinnedTrades.size() >= MAX_PINS) {
			showToast("You have reached the maximum pinned trades!", COLOR_TOAST_MAROON);
			return;
		}
		PeekData d = new PeekData(TradeNames.villagerTitle(villager), "", TradeNames.offerTitle(offer), "",
			TradeNames.costTotal(List.of(offer)), offer.getResult(), offer.getBaseCostA(), offer.getCostB(),
			List.of(), false, mc.level.getGameTime());
		pinnedTrades.add(new PinnedTrade(villager.getId(), signature, d));
		showToast("Trade pinned successfully", COLOR_TOAST_GREEN);
	}

	public static void renderToast(GuiGraphics graphics) {
		if (toastText == null) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			toastText = null;
			return;
		}
		long remaining = toastExpireTick - mc.level.getGameTime();
		if (remaining <= 0) {
			toastText = null;
			return;
		}
		int alpha = 255;
		if (remaining < TOAST_FADE_TICKS) {
			alpha = (int) (255 * remaining / (double) TOAST_FADE_TICKS);
		}
		int color = (alpha << 24) | toastColor;
		int w = mc.font.width(toastText);
		int x = (graphics.guiWidth() - w) / 2;
		int y = graphics.guiHeight() - 62;
		graphics.drawString(mc.font, toastText, x, y, color);
	}

	public static void renderInventoryOverlay(GuiGraphics graphics, int containerLeft, int containerTop,
		int containerWidth) {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.player == null || mc.level == null) {
			return;
		}
		LocalPlayer player = mc.player;
		invButtons.clear();
		int gw = graphics.guiWidth();
		int x = containerLeft + containerWidth + 8;
		int maxX = gw - PanelRenderer.PANEL_WIDTH - 4;
		if (x > maxX) {
			x = Math.max(containerLeft + containerWidth + 4, maxX);
		}
		if (x < 4) {
			return;
		}
		int y = containerTop;
		for (int i = 0; i < pinnedTrades.size(); i++) {
			PinnedTrade pin = pinnedTrades.get(i);
			y = drawInventoryCard(graphics, mc, player, pin.data(), x, y, i);
			y += 8;
		}
	}

	private static int drawInventoryCard(GuiGraphics graphics, Minecraft mc, LocalPlayer player, PeekData d,
		int x, int y, int index) {
		int w = PanelRenderer.PANEL_WIDTH;
		int h = 54;
		boolean affordable = canAfford(player, d.costA(), d.costB());
		int bgColor = ((int) (255 * PanelRenderer.BG_ALPHA) << 24) | 0xFFFFFF;
		int frameColor = ((int) (255 * PanelRenderer.FRAME_ALPHA) << 24)
			| (affordable ? PanelRenderer.TINT_MATCH : 0xFFFFFF);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PanelRenderer.TOOLTIP_BACKGROUND, x, y, w, h, bgColor);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PanelRenderer.TOOLTIP_FRAME, x, y, w, h, frameColor);

		int buttonX = x + w - 22;
		int buttonY = y + 4;
		graphics.fill(buttonX, buttonY, buttonX + 14, buttonY + 10, 0x66000000);
		graphics.drawString(mc.font, "X", buttonX + (14 - mc.font.width("X")) / 2, buttonY + 1, 0xFFFF6B6B);
		invButtons.add(new InvButton(buttonX, buttonY, 14, 10, index));

		String title = d.title();
		graphics.drawString(mc.font, title, x + 8, y + 4, d.matched() ? PanelRenderer.COLOR_MATCH : PanelRenderer.COLOR_TITLE);

		graphics.renderItem(d.mainIcon(), x + 8, y + 20);
		String itemName = d.itemName();
		int nameColor;
		if (d.matched()) {
			nameColor = PanelRenderer.COLOR_MATCH;
		} else if (d.mainIcon().is(Items.ENCHANTED_BOOK)) {
			nameColor = PanelRenderer.rainbowColor(mc.level.getGameTime(), 255);
		} else {
			nameColor = PanelRenderer.COLOR_DETAIL;
		}
		graphics.drawString(mc.font, itemName, x + 28, y + 25, nameColor);

		String cost = d.mainCost();
		graphics.drawString(mc.font, cost, x + 8, y + 40, affordable ? PanelRenderer.COLOR_MATCH : PanelRenderer.COLOR_DETAIL);
		return y + h;
	}

	public static boolean handleInventoryClick(int mx, int my) {
		for (InvButton b : invButtons) {
			if (mx >= b.x() && mx < b.x() + b.w() && my >= b.y() && my < b.y() + b.h()) {
				unpin(b.index());
				return true;
			}
		}
		return false;
	}

	private static void unpin(int index) {
		if (index >= 0 && index < pinnedTrades.size()) {
			pinnedTrades.remove(index);
		}
	}

	private static boolean canAfford(LocalPlayer player, ItemStack costA, ItemStack costB) {
		if (!costA.isEmpty() && countItem(player, costA.getItem()) < costA.getCount()) {
			return false;
		}
		return costB.isEmpty() || countItem(player, costB.getItem()) >= costB.getCount();
	}

	private static int countItem(LocalPlayer player, Item item) {
		int total = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		ItemStack offhand = player.getInventory().getItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND);
		if (offhand.is(item)) {
			total += offhand.getCount();
		}
		return total;
	}

	public static void render(GuiGraphics graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.player == null || mc.level == null || mc.screen != null) {
			return;
		}
		PeekData d = data;
		if (d == null || anchor == null) {
			return;
		}
		if (mc.level.getGameTime() - d.seenAtTick() > DISPLAY_FADE_TICKS) {
			return;
		}

		Vec3 head = anchor;
		if (currentVillagerId >= 0) {
			Entity villager = mc.level.getEntity(currentVillagerId);
			if (villager != null && villager.isAlive()) {
				head = villager.position().add(0, villager.getBbHeight() + 0.5, 0);
			}
		}

		double dx = head.x - mc.player.position().x;
		double dz = head.z - mc.player.position().z;
		if (dx * dx + dz * dz > MAX_RANGE * MAX_RANGE) {
			return;
		}

		PanelRenderer.render(graphics, mc, d, head);
	}

	private static void clearDisplay() {
		data = null;
		anchor = null;
		currentVillagerId = -1;
	}

	private static void reset() {
		peekMode = false;
		peekTicks = 0;
		pendingVillager = null;
		pollCounter = 0;
		pinnedTrades.clear();
		invButtons.clear();
		clearDisplay();
	}
}