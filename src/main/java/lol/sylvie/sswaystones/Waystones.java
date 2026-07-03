/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones;

import java.util.HashMap;
import java.util.UUID;
import lol.sylvie.sswaystones.block.ModBlocks;
import lol.sylvie.sswaystones.command.WaystonesCommand;
import lol.sylvie.sswaystones.config.Configuration;
import lol.sylvie.sswaystones.item.ModItems;
import lol.sylvie.sswaystones.worldgen.VillageInjector;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Waystones implements ModInitializer {
    public static String MOD_ID = "sswaystones";
    public static Logger LOGGER = LoggerFactory.getLogger("Server-Side Waystones");
    public static Configuration configuration;

    public static HashMap<UUID, Long> combatTimestamps = new HashMap<>();

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

        ServerLifecycleEvents.SERVER_STARTING.register(VillageInjector::inject);
        ResourceLoader.registerBuiltinPack(Waystones.id("remove_waystone_recipes"),
                FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(), PackActivationType.NORMAL);
    }

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

    public static boolean isInCombat(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (!combatTimestamps.containsKey(uuid))
            return false;
        return combatTimestamps.get(uuid) + (configuration.getInstance().combatCooldown * 1000L) > System
                .currentTimeMillis();
    }
}
