package lol.sylvie.sswaystones;

import lol.sylvie.sswaystones.block.ModBlocks;
import lol.sylvie.sswaystones.block.ModItems;
import lol.sylvie.sswaystones.command.WaystonesCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Waystones implements ModInitializer {
    public static String MOD_ID = "sswaystones";
    public static Logger LOGGER = LoggerFactory.getLogger("Server-Side Waystones");

    @Override
    public void onInitialize() {
        LOGGER.info("{} is made with <3 by sylvie", MOD_ID);
        ModBlocks.initialize();
        ModItems.initialize();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> WaystonesCommand.register(dispatcher));
    }
}
