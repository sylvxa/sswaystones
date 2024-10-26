/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones;

import lol.sylvie.sswaystones.block.ModBlocks;
import lol.sylvie.sswaystones.command.WaystonesCommand;
import lol.sylvie.sswaystones.config.Configuration;
import lol.sylvie.sswaystones.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Waystones implements ModInitializer {
    public static String MOD_ID = "sswaystones";
    public static Logger LOGGER = LoggerFactory.getLogger("Server-Side Waystones");
    public static Configuration configuration;

    @Override
    public void onInitialize() {
        configuration = new Configuration(MOD_ID + ".json");
        configuration.load();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> configuration.save()));

        LOGGER.info("{} is made with <3 by sylvie", MOD_ID);
        ModBlocks.initialize();
        ModItems.initialize();

        CommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess, environment) -> WaystonesCommand.register(dispatcher));
    }

    public static Identifier id(String name) {
        return Identifier.of(MOD_ID, name);
    }
}
