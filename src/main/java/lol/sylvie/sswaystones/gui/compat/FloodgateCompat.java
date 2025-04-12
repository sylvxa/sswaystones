/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.gui.compat;

import java.util.UUID;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import net.minecraft.server.network.ServerPlayerEntity;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.Nullable;

public class FloodgateCompat {
    public static final String MOD_ID = "floodgate";

    public static boolean openGuiOrFalse(ServerPlayerEntity player, @Nullable WaystoneRecord record) {
        FloodgateApi api = FloodgateApi.getInstance();
        UUID uuid = player.getUuid();
        if (api.isFloodgatePlayer(uuid)) {
            BedrockViewerGui.openGui(player, record, form -> {
                api.sendForm(uuid, form);
            });
            return true;
        }
        return false;
    }
}
