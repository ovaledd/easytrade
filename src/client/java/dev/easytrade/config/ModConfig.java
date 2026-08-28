package dev.easytrade.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger("easytrade");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type TRADE_LIST_TYPE = new TypeToken<List<DesiredTrade>>() {}.getType();

	public int pollIntervalTicks = 2;
	public boolean alertSound = true;
	public String alertSoundType = "experience_orb_pickup"; // experience_orb_pickup, block_note_block_pling, entity_villager_yes, ui_button_click
	public boolean rainbowEffect = true;
	public float panelOpacity = 0.30f;
	public float panelFrameOpacity = 0.65f;
	public int maxPins = 4;
	public int fadeInTicks = 5;
	public int fadeStartTicks = 30;
	public int fadeEndTicks = 40;
	public boolean showProfession = true;
	public boolean showMatchedPrice = true;
	public List<DesiredTrade> desiredTrades = new ArrayList<>();

	public static ModConfig INSTANCE = new ModConfig();

	public static void load() {
		Path path = configPath();
		if (!Files.exists(path)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
			if (loaded != null) {
				INSTANCE = loaded;
				if (INSTANCE.desiredTrades == null) {
					INSTANCE.desiredTrades = new ArrayList<>();
				}
				migrate();
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Failed to load easytrade config, using defaults", e);
			INSTANCE = new ModConfig();
		}
	}

	private static void migrate() {
		if (INSTANCE.maxPins <= 0) INSTANCE.maxPins = 4;
		if (INSTANCE.panelOpacity <= 0) INSTANCE.panelOpacity = 0.30f;
		if (INSTANCE.panelFrameOpacity <= 0) INSTANCE.panelFrameOpacity = 0.65f;
		if (INSTANCE.fadeInTicks <= 0) INSTANCE.fadeInTicks = 5;
		if (INSTANCE.fadeStartTicks <= 0) INSTANCE.fadeStartTicks = 30;
		if (INSTANCE.fadeEndTicks <= 0) INSTANCE.fadeEndTicks = 40;
		if (INSTANCE.alertSoundType == null || INSTANCE.alertSoundType.isBlank()) INSTANCE.alertSoundType = "experience_orb_pickup";
	}

	public static void save() {
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(INSTANCE, writer);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to save easytrade config", e);
		}
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("easytrade.json");
	}

	public int getMaxPins() {
		return Math.max(1, maxPins);
	}

	public float getPanelOpacity() {
		return Math.max(0.05f, Math.min(1.0f, panelOpacity));
	}

	public float getPanelFrameOpacity() {
		return Math.max(0.05f, Math.min(1.0f, panelFrameOpacity));
	}
}