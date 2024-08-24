/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.*;
import java.nio.file.Path;
import lol.sylvie.sswaystones.Waystones;
import net.fabricmc.loader.api.FabricLoader;

public class Configuration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final File configFile;
    private Instance instance = new Instance();

    public Configuration(String name) {
        Path configFolder = FabricLoader.getInstance().getConfigDir();
        configFile = configFolder.resolve(name).toFile();
    }

    public Instance getInstance() {
        return instance;
    }

    // Saving/loading to/from disk.
    public void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(instance, writer);
        } catch (IOException exception) {
            Waystones.LOGGER.error("Could not save configuration to disk!", exception);
        }
    }

    public void load() {
        try (FileReader reader = new FileReader(configFile)) {
            instance = GSON.fromJson(reader, Instance.class);
        } catch (IOException exception) {
            Waystones.LOGGER.warn("Could not load configuration from disk!", exception);
        }
    }

    // Configuration instance
    public static class Instance {
        @SerializedName("xp_cost")
        @Description(translation = "config.sswaystones.xp_cost")
        public int xpCost = 0;
    }
}
