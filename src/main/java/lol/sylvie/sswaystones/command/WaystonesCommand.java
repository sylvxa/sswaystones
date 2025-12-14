/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.command;

import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.block.ModBlocks;
import lol.sylvie.sswaystones.config.Configuration;
import lol.sylvie.sswaystones.config.Description;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class WaystonesCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("sswaystones")
                .requires(source -> Permissions.check(source, "sswaystones.command", false)).executes(context -> {
                    String version = FabricLoader.getInstance().getModContainer(Waystones.MOD_ID).orElseThrow().getMetadata().getVersion().getFriendlyString();
                    context.getSource().sendSuccess(() -> Component.literal("sswaystones " + version + ", made with <3 by sylvie"), false);
                    return 1;
                }).then(literal("list").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.translatable("command.sswaystones.list_header"), false);

                    WaystoneStorage storage = WaystoneStorage.getServerState(context.getSource().getServer());
                    for (Map.Entry<String, WaystoneRecord> waystone : storage.waystones.entrySet()) {
                        WaystoneRecord record = waystone.getValue();
                        context.getSource().sendSuccess(
                                () -> Component.literal(
                                        String.format("(%s) [%s >" + " %s] %s", waystone.getKey().substring(0, 7),
                                                record.getOwnerName(), record.getWaystoneName(), record.asString())),
                                false);
                    }

                    return 1;
                })).then(literal("remove").then(argument("hash", StringArgumentType.word()).executes(context -> {
                    WaystoneStorage storage = WaystoneStorage.getServerState(context.getSource().getServer());
                    String search = StringArgumentType.getString(context, "hash").toLowerCase(Locale.ROOT);
                    Optional<Map.Entry<String, WaystoneRecord>> entry = storage.waystones.entrySet().stream()
                            .filter(w -> w.getKey().toLowerCase(Locale.ROOT).startsWith(search)).findFirst();
                    if (entry.isEmpty()) {
                        throw new CommandSyntaxException(
                                CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument(),
                                Component.translatable("command.sswaystones.waystone_not_found"));
                    }

                    MinecraftServer server = context.getSource().getServer();
                    WaystoneRecord record = entry.get().getValue();
                    storage.destroyWaystone(record);

                    // Remove it in the world
                    ServerLevel world = record.getWorld(server);
                    if (world.getBlockState(record.getPos()).is(ModBlocks.WAYSTONE)) {
                        world.destroyBlock(record.getPos(), true);
                    }

                    context.getSource().sendSuccess(
                            () -> Component.translatable("command.sswaystones.waystone_removed_successfully"), true);

                    return 1;
                }))).then(literal("config").then(literal("help").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.translatable("command.sswaystones.config_help_header"),
                            false);
                    for (Map.Entry<String, Component> option : getConfigOptions().entrySet()) {
                        context.getSource().sendSuccess(() -> Component.translatable("command.sswaystones.config_format",
                                formatKey(option.getKey()), option.getValue()), false);
                    }
                    return 1;
                })).then(literal("reload").executes(context -> {
                    Waystones.configuration.load();
                    context.getSource()
                            .sendSuccess(() -> Component.translatable("command.sswaystones.config_reload_success"), true);
                    return 1;
                })).then(literal("save").executes(context -> {
                    Waystones.configuration.save();
                    context.getSource().sendSuccess(() -> Component.translatable("command.sswaystones.config_save_success"),
                            false);
                    return 1;
                })).then(literal("set").then(argument("key", StringArgumentType.word())
                        .then(argument("value", StringArgumentType.greedyString()).executes(context -> {
                            String key = StringArgumentType.getString(context, "key");
                            Field field = getConfigByName(key);
                            if (field == null)
                                throw new CommandSyntaxException(
                                        CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument(),
                                        Component.translatable("command.sswaystones.config_not_found"));
                            Configuration.Instance instance = Waystones.configuration.getInstance();
                            field.setAccessible(true);

                            Class<?> type = field.getType();
                            String value = StringArgumentType.getString(context, "value");
                            Object newValue;
                            try {
                                if (type == int.class) {
                                    field.set(instance, Integer.parseInt(value));
                                } else if (type == float.class) {
                                    field.set(instance, Float.parseFloat(value));
                                } else if (type == double.class) {
                                    field.set(instance, Double.parseDouble(value));
                                } else if (type == boolean.class) {
                                    field.set(instance, Boolean.parseBoolean(value));
                                } else if (type == String.class) {
                                    field.set(instance, value);
                                }
                                newValue = field.get(instance);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            } catch (NumberFormatException e) {
                                throw new CommandSyntaxException(
                                        CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedInt(),
                                        Component.translatable("command.sswaystones.config_set_invalid_type"));
                            }
                            context.getSource()
                                    .sendSuccess(() -> Component.translatable("command.sswaystones.config_set_success",
                                            formatKey(key), formatValue(newValue)), true);
                            return 1;
                        }))))
                        .then(literal("get").then(argument("key", StringArgumentType.string()).executes(context -> {
                            String key = StringArgumentType.getString(context, "key");
                            Field field = getConfigByName(key);
                            if (field == null)
                                throw new CommandSyntaxException(
                                        CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument(),
                                        Component.translatable("command.sswaystones.config_not_found"));
                            Configuration.Instance instance = Waystones.configuration.getInstance();

                            context.getSource().sendSuccess(() -> {
                                try {
                                    return Component.translatable("command.sswaystones.config_format", formatKey(key),
                                            formatValue(field.get(instance)));
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }, false);

                            return 1;
                        })))));
    }

    // Returns Map of String -> Description
    private static Map<String, Component> getConfigOptions() {
        // I'm not proud of this. I'm so, so sorry.
        Field[] fields = Configuration.Instance.class.getDeclaredFields();
        Map<String, Component> descriptionMap = new HashMap<>();
        for (Field field : fields) {
            SerializedName name = field.getAnnotation(SerializedName.class);
            Description description = field.getAnnotation(Description.class);
            descriptionMap.put(name.value(), Component.translatable(description.translation()));
        }
        return descriptionMap;
    }

    private static Field getConfigByName(String key) {
        Field[] fields = Configuration.Instance.class.getDeclaredFields();
        for (Field field : fields) {
            SerializedName name = field.getAnnotation(SerializedName.class);
            if (name.value().equals(key))
                return field;
        }
        return null;
    }

    private static Component formatKey(String key) {
        return Component.literal(key.toLowerCase()).withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE);
    }

    private static Component formatValue(Object value) {
        return Component.literal(String.valueOf(value)).withStyle(ChatFormatting.YELLOW);
    }
}
