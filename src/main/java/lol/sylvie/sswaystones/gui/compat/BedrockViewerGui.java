/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.gui.compat;

import java.util.List;
import java.util.function.Consumer;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import lol.sylvie.sswaystones.util.NameGenerator;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.scoreboard.Team;
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
        List<WaystoneRecord> accessible = storage.getAccessibleWaystones(player, waystone);

        for (WaystoneRecord record : accessible) {
            boolean server = record.getAccessSettings().isServerOwned();
            FormImage.Type type = server ? FormImage.Type.PATH : FormImage.Type.URL;
            String image = server
                    ? "textures/ui/filledStar.png"
                    : AVATAR_API.replace("%s", record.getOwnerUUID().toString());

            ButtonComponent component = ButtonComponent.of(record.getWaystoneName(), type, image);
            builder.button(component);
        }

        boolean showSettingsButton = waystone != null && waystone.canPlayerEdit(player);
        if (showSettingsButton)
            builder.button("Settings", FormImage.Type.PATH, "textures/gui/newgui/anvil-hammer.png");

        builder.validResultHandler(response -> {
            int selectedIndex = response.clickedButtonId();
            if (selectedIndex < accessible.size()) {
                WaystoneRecord selectedWaystone = accessible.get(selectedIndex);
                selectedWaystone.handleTeleport(player);
                return;
            }

            if (selectedIndex == accessible.size() && showSettingsButton) {
                CustomForm form = getSettingsForm(player, waystone);
                sendForm.accept(form);
            }
        });

        return builder.build();
    }

    public static CustomForm getSettingsForm(ServerPlayerEntity player, WaystoneRecord waystone) {
        CustomForm.Builder builder = CustomForm.builder()
                .title(String.format("%s - Settings", waystone.getWaystoneName()));

        WaystoneRecord.AccessSettings accessSettings = waystone.getAccessSettings();
        builder.input("Waystone Name", NameGenerator.generateName(), waystone.getWaystoneName());

        boolean globalAvailable = Permissions.check(player, "sswaystones.create.global", true);
        if (globalAvailable) {
            builder.toggle("Global", accessSettings.isGlobal());
        }

        boolean teamAvailable = player.getScoreboardTeam() != null && Permissions.check(player, "sswaystones.create.team", true);
        if (teamAvailable) {
            builder.toggle("Team", accessSettings.hasTeam());
        }

        boolean serverAvailable = Permissions.check(player, "sswaystones.create.server", 4);
        if (serverAvailable) {
            builder.toggle("Server-Owned", accessSettings.isServerOwned());
        }

        builder.validResultHandler(response -> {
            String name = response.asInput();
            if (name == null)
                return;

            int index = 1;
            if (globalAvailable) {
                boolean global = response.asToggle(index);
                accessSettings.setGlobal(global);
                index += 1;
            }

            if (teamAvailable) {
                boolean team = response.asToggle(index);
                Team playerTeam = player.getScoreboardTeam();
                if (team && playerTeam != null) {
                    accessSettings.setTeam(playerTeam.getName());
                } else
                    accessSettings.setTeam("");

                index += 1;
            }

            if (serverAvailable) {
                boolean server = response.asToggle(index);
                accessSettings.setServerOwned(server);
            }

            waystone.setWaystoneName(name);
        });

        return builder.build();
    }
}
