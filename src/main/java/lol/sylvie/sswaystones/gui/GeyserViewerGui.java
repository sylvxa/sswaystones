/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.gui;

import java.util.Comparator;
import java.util.List;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import lol.sylvie.sswaystones.util.NameGenerator;
import net.minecraft.server.network.ServerPlayerEntity;
import org.geysermc.cumulus.component.ButtonComponent;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.jetbrains.annotations.Nullable;

public class GeyserViewerGui {
    private static final String CRAFATAR = "https://crafatar.com/avatars/%s?overlay";

    public static boolean openGuiIfBedrock(ServerPlayerEntity player, @Nullable WaystoneRecord waystone) {
        GeyserConnection connection = GeyserApi.api().connectionByUuid(player.getUuid());
        if (connection == null)
            return false;

        SimpleForm form = GeyserViewerGui.getViewerForm(player, connection, waystone);
        connection.sendForm(form);
        return true;
    }

    public static SimpleForm getViewerForm(ServerPlayerEntity player, GeyserConnection connection,
            @Nullable WaystoneRecord waystone) {
        String title = "Waystones";
        if (waystone != null) {
            title = String.format("%s [%s]", waystone.getWaystoneName(), waystone.getOwnerName());
        }

        SimpleForm.Builder builder = SimpleForm.builder().title(title);

        assert player.getServer() != null; // It's a ServerPlayerEntity.
        WaystoneStorage storage = WaystoneStorage.getServerState(player.getServer());
        List<WaystoneRecord> discovered = storage.getDiscoveredWaystones(player).stream()
                .filter(r -> !r.equals(waystone)).sorted(Comparator.comparing(WaystoneRecord::getWaystoneName))
                .toList();

        for (WaystoneRecord record : discovered) {
            ButtonComponent component = ButtonComponent.of(record.getWaystoneName(), FormImage.Type.URL,
                    CRAFATAR.replace("%s", record.getOwnerUUID().toString()));
            builder.button(component);
        }

        boolean showSettingsButton = waystone != null && waystone.canEdit(player);
        if (showSettingsButton)
            builder.button("Settings", FormImage.Type.PATH, "textures/gui/newgui/anvil-hammer.png");

        builder.validResultHandler(response -> {
            int selectedIndex = response.clickedButtonId();
            if (selectedIndex < discovered.size()) {
                WaystoneRecord selectedWaystone = discovered.get(selectedIndex);
                selectedWaystone.handleTeleport(player);
                return;
            }

            if (selectedIndex == discovered.size() && showSettingsButton) {
                CustomForm form = getSettingsForm(waystone);
                connection.sendForm(form);
            }
        });

        return builder.build();
    }

    public static CustomForm getSettingsForm(WaystoneRecord waystone) {
        CustomForm.Builder builder = CustomForm.builder()
                .title(String.format("%s - Settings", waystone.getWaystoneName()));

        builder.input("Waystone Name", NameGenerator.generateName(), waystone.getWaystoneName());
        builder.toggle("Global", waystone.isGlobal());

        builder.validResultHandler(response -> {
            String name = response.asInput();
            if (name == null)
                return;

            boolean global = response.asToggle();

            waystone.setWaystoneName(name);
            waystone.setGlobal(global);
        });

        return builder.build();
    }
}
