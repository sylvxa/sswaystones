package lol.sylvie.sswaystones.command;

import com.mojang.brigadier.CommandDispatcher;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.literal;

public class WaystonesCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("sswaystones").requires(source -> source.hasPermissionLevel(4)).executes(context -> {
            // /waystones
            context.getSource().sendFeedback(() -> Text.literal("Server-Sided Waystones by sylvxa"), false);
            return 1;
        }).then(literal("list").executes(context -> {
            // /waystones list
            context.getSource().sendFeedback(() -> Text.translatable("command.sswaystones.list_header").formatted(Formatting.BOLD), false);

            WaystoneStorage storage = WaystoneStorage.getServerState(context.getSource().getServer());
            for (Map.Entry<String, WaystoneRecord> waystone : storage.waystones.entrySet()) {
                WaystoneRecord record = waystone.getValue();
                context.getSource().sendFeedback(() -> Text.literal(String.format("[%s > %s] %s", record.getOwnerName(), record.getWaystoneName(), record.asString())), false);
            }

            return 1;
        })));
    }
}
