/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.gui;

import lol.sylvie.sswaystones.storage.WaystoneRecord;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class ViewerUtil {
    public static void openGui(ServerPlayerEntity player, @Nullable WaystoneRecord record) {
        // TODO: Floodgate hasn't been released for 1.21.3, so I'm not quite sure if this works.
        if (FabricLoader.getInstance().isModLoaded("geyser-fabric")
                || FabricLoader.getInstance().isModLoaded("floodgate")) {
            if (GeyserViewerGui.openGuiIfBedrock(player, record)) {
                return;
            }

        }

        JavaViewerGui gui = new JavaViewerGui(player, record);
        gui.updateMenu();
        gui.open();
    }
}
