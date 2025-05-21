/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
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
            Instance loaded = GSON.fromJson(reader, Instance.class);
            if (loaded != null)
                instance = loaded;
        } catch (IOException exception) {
            Waystones.LOGGER.warn("Could not load configuration from disk!", exception);
        } catch (JsonSyntaxException exception) {
            Waystones.LOGGER.warn("Invalid configuration!", exception);
        }
    }

    // Configuration instance
    public static class Instance {
        @SerializedName("xp_cost")
        @Description(translation = "config.sswaystones.xp_cost")
        public int xpCost = 0;

        @SerializedName("cross_dimension_xp_cost")
        @Description(translation = "config.sswaystones.cross_dimension_xp_cost")
        public int crossDimensionXpCost = 1;

        @SerializedName("combat_cooldown")
        @Description(translation = "config.sswaystones.combat_cooldown")
        public int combatCooldown = 0;

        @SerializedName("waystone_limit")
        @Description(translation = "config.sswaystones.waystone_limit")
        public int waystoneLimit = 0;

        @SerializedName("paranoid_teleport")
        @Description(translation = "config.sswaystones.paranoid_teleport")
        public boolean safeTeleport = true;

        @SerializedName("remove_invalid_waystones")
        @Description(translation = "config.sswaystones.remove_invalid_waystones")
        public boolean removeInvalidWaystones = true;

        @SerializedName("physical_icon_display")
        @Description(translation = "config.sswaystones.physical_icon_display")
        public boolean physicalIconDisplay = false;
    }
}
