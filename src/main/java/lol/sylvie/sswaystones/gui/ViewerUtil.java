/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.gui;

import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.gui.compat.FloodgateCompat;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class ViewerUtil {
    public static void openGui(ServerPlayer player, @Nullable WaystoneRecord record) {
        if (Waystones.isInCombat(player)) {
            player.displayClientMessage(
                    Component.translatable("error.sswaystones.combat_cooldown").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (FabricLoader.getInstance().isModLoaded(FloodgateCompat.MOD_ID)
                && FloodgateCompat.openGuiOrFalse(player, record)) {
            return;
        }

        openJavaGui(player, record);
    }

    public static void openJavaGui(ServerPlayer player, @Nullable WaystoneRecord record) {
        JavaViewerGui gui = new JavaViewerGui(player, record);
        gui.open();
    }
}
