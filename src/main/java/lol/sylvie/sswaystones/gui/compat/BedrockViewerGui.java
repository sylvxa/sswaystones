/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.gui.compat;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import lol.sylvie.sswaystones.util.NameGenerator;
import net.minecraft.server.network.ServerPlayerEntity;
import org.geysermc.cumulus.component.ButtonComponent;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.jetbrains.annotations.Nullable;

public class BedrockViewerGui {
    private static final String AVATAR_API = "https://api.tydiumcraft.net/v1/players/skin?uuid=%s&type=avatar";

    public static void openGui(ServerPlayerEntity player, @Nullable WaystoneRecord waystone, Consumer<Form> sendForm) {
        SimpleForm form = BedrockViewerGui.getViewerForm(player, waystone, sendForm);
        sendForm.accept(form);
    }

    public static SimpleForm getViewerForm(ServerPlayerEntity player, @Nullable WaystoneRecord waystone,
            Consumer<Form> sendForm) {
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
                    AVATAR_API.replace("%s", record.getOwnerUUID().toString()));
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
                sendForm.accept(form);
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
