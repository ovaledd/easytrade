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
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Failed to load easytrade config, using defaults", e);
			INSTANCE = new ModConfig();
		}
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
}