/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.gui;

import com.mojang.authlib.GameProfile;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.*;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lol.sylvie.sswaystones.enums.Visibility;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public class JavaViewerGui extends SimpleGui {
    private static final int ITEMS_PER_PAGE = 9 * 5;
    private final WaystoneRecord waystone;
    private int pageIndex = 0;

    private final List<WaystoneRecord> accessible;
    private final int maxPages;

    public JavaViewerGui(ServerPlayerEntity player, @Nullable WaystoneRecord waystone) {
        super(ScreenHandlerType.GENERIC_9X6, player, false);
        this.waystone = waystone;

        assert player.getServer() != null; // It's a ServerPlayerEntity.
        WaystoneStorage storage = WaystoneStorage.getServerState(player.getServer());
        this.accessible = storage.getAccessibleWaystones(player, waystone);
        this.maxPages = Math.max(Math.ceilDiv(this.accessible.size(), ITEMS_PER_PAGE), 1);

        this.updateMenu();
    }

    public void updateMenu() {
        // If there are no other waystones this will display as 0
        if (waystone != null) {
            this.setTitle(Text.literal(String.format("%s [%s] (%s/%s)", waystone.getWaystoneName(),
                    waystone.getOwnerName(), pageIndex + 1, maxPages)));
        } else {
            this.setTitle(Text.literal(String.format("Waystones (%s/%s)", pageIndex + 1, maxPages)));
        }

        int offset = ITEMS_PER_PAGE * pageIndex;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            this.clearSlot(i);
        }

        for (int i = offset; i < this.accessible.size(); i++) {
            WaystoneRecord record = this.accessible.get(i);
            int slot = i - offset;
            if (slot >= 45)
                break;

            GuiElementBuilder element = new GuiElementBuilder(record.getIconOrHead(player.server))
                    .setName(record.getWaystoneText().copy().formatted(Formatting.YELLOW));

            if (!record.getAccessSettings().isServerOwned())
                element.setLore(List.of(Text.of(record.getOwnerName())));
            else
                element.glow(true);

            element.setCallback((index, type, action, gui) -> {
                record.handleTeleport(player);
                gui.close();
            });

            this.setSlot(slot, element);
        }

        for (int i = 45; i < 54; i++) {
            this.setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(Text.empty()));
        }

        // Gui controls
        this.setSlot(45,
                new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(IconConstants.ARROW_LEFT)
                        .setName(Text.translatable("gui.sswaystones.page_previous"))
                        .setCallback((index, type, action, gui) -> previousPage()));

        this.setSlot(47,
                new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(IconConstants.ARROW_RIGHT)
                        .setName(Text.translatable("gui.sswaystones.page_next"))
                        .setCallback((index, type, action, gui) -> nextPage()));

        // Waystone settings
        if (waystone == null)
            return;

        if (waystone.canPlayerEdit(player)) {
            if (Permissions.check(player, "sswaystones.manager", 4)
                    && !waystone.getOwnerUUID().equals(player.getUuid())) {
                this.setSlot(50,
                        new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(IconConstants.CHEST)
                                .setName(Text.translatable("gui.sswaystones.steal_waystone").formatted(Formatting.RED))
                                .setCallback((index, type, action, gui) -> {
                                    waystone.setOwner(player);
                                    this.updateMenu();
                                }));
            }

            // Setting menus
            this.setSlot(51,
                    new GuiElementBuilder(waystone.getIconOrHead(player.server))
                            .setName(Text.translatable("gui.sswaystones.change_icon").formatted(Formatting.YELLOW))
                            .glow().setCallback((index, type, action, gui) -> new IconGui(waystone, player).open()));

            this.setSlot(52,
                    new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(IconConstants.ANVIL)
                            .setName(Text.translatable("gui.sswaystones.change_name").formatted(Formatting.YELLOW))
                            .setCallback((index, type, action, gui) -> new NameGui(waystone, player).open()));

            this.setSlot(53, new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(IconConstants.COMMAND_BLOCK)
                    .setName(Text.translatable("gui.sswaystones.access_settings").formatted(Formatting.LIGHT_PURPLE))
                    .setCallback((index, type, action, gui) -> new AccessSettingsGui(waystone, player).open()));
        }
    }

    public void previousPage() {
        pageIndex--;
        if (pageIndex < 0) {
            pageIndex = maxPages - 1;
        }

        this.updateMenu();
    }

    public void nextPage() {
        pageIndex++;
        if (pageIndex >= maxPages) {
            pageIndex = 0;
        }

        this.updateMenu();
    }

    protected static class NameGui extends AnvilInputGui {
        private final WaystoneRecord waystone;

        public NameGui(WaystoneRecord waystone, ServerPlayerEntity player) {
            super(player, false);
            this.waystone = waystone;

            this.setDefaultInputValue(waystone.getWaystoneName());
            this.setSlot(1, new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(IconConstants.CANCEL)
                    .setName(Text.translatable("gui.back")).setCallback((index, type, action, gui) -> gui.close()));

            this.setSlot(2, new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(IconConstants.CHECKMARK)
                    .setName(Text.translatable("gui.done")).setCallback((index, type, action, gui) -> {
                        String input = this.getInput();
                        waystone.setWaystoneName(input);
                        gui.close();
                    }));

            this.setTitle(Text.translatable("gui.sswaystones.change_name_title"));
        }

        @Override
        public void onClose() {
            super.onClose();
            ViewerUtil.openJavaGui(player, waystone);
        }
    }

    protected static class IconGui extends SimpleGui {
        private final WaystoneRecord waystone;

        public IconGui(WaystoneRecord waystone, ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_3X3, player, false);
            this.waystone = waystone;
            this.updateMenu();
            this.setTitle(Text.translatable("gui.sswaystones.change_icon_title"));
        }

        private void updateMenu() {
            for (int i = 0; i < 9; i++) {
                this.setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(
                        Text.translatable("gui.sswaystones.change_icon_instruction").formatted(Formatting.GRAY)));
            }
            this.setSlot(4, waystone.getIconOrHead(player.server));
        }

        @Override
        public boolean onAnyClick(int index, ClickType type, SlotActionType action) {
            if (index > 8 && action.equals(SlotActionType.PICKUP)) {
                if (index > 35)
                    index -= 36; // Get hotbar slot
                ItemStack stack = player.getInventory().getStack(index);

                if (stack != null && !stack.isOf(Items.AIR)) {
                    waystone.setIcon(stack.getItem());
                    this.close();
                }
            }
            return super.onAnyClick(index, type, action);
        }

        @Override
        public void onClose() {
            super.onClose();
            ViewerUtil.openJavaGui(player, waystone);
        }
    }

    //TODO: Add Paging
    protected static class AccessSettingsGui extends SimpleGui {
        private final WaystoneRecord waystone;

        public AccessSettingsGui(WaystoneRecord waystone, ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.waystone = waystone;

            this.setTitle(Text.translatable("gui.sswaystones.access_settings"));
            this.updateMenu();
        }

        private void updateMenu() {
            // Framing
            for (int i = 0; i < (9 * 3); i++) {
                this.setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(
                        Text.translatable("gui.sswaystones.access_settings_instruction").formatted(Formatting.GRAY)));
            }

            for (int i = 10; i < 17; i++) {
                this.clearSlot(i);
            }

            // Settings
            WaystoneRecord.AccessSettings accessSettings = waystone.getAccessSettings();
            int slot = 10;

            // Global
            if (Permissions.check(player, "sswaystones.create.global", true)) {
                GuiElementBuilder globalToggle = new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setSkullOwner(IconConstants.GLOBE)
                        .setName(Text.translatable(accessSettings.getVisibility().getDisplayName()).formatted(accessSettings.getVisibility().getFormatting()));

                globalToggle.setCallback((index, type, action, gui) -> {
                    accessSettings.setVisibility(
                            Visibility.values()[
                                    (accessSettings.getVisibility().ordinal() + 1) % Visibility.values().length
                                    ]
                    );
                    this.updateMenu();
                });

                int trustedPlayerSlot = 16;
                if (accessSettings.getVisibility() == Visibility.PRIVATE) {
                    GuiElementBuilder trustedPlayers = new GuiElementBuilder(Items.WRITABLE_BOOK)
                            .setName(Text.translatable("gui.sswaystones.trusted_players").formatted(Formatting.GRAY));

                    trustedPlayers.setCallback((index, type, action, gui) -> {
                        TrustedPlayersGui trustedPlayerGui = new TrustedPlayersGui(waystone, player);
                        trustedPlayerGui.open();
                    });

                    this.setSlot(trustedPlayerSlot, trustedPlayers);
                }

                this.setSlot(slot, globalToggle);
                slot += 1;
            }

            // Team
            Team team = player.getScoreboardTeam();
            if (team != null && Permissions.check(player, "sswaystones.create.team", true)) {
                String teamName = team.getName();
                GuiElementBuilder teamToggle = new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setSkullOwner(IconConstants.SHIELD).setName(Text.translatable("gui.sswaystones.toggle_team")
                                .formatted(accessSettings.hasTeam() ? Formatting.GREEN : Formatting.RED));

                teamToggle.setCallback((index, type, action, gui) -> {
                    accessSettings.setTeam(accessSettings.hasTeam() ? "" : teamName);
                    this.updateMenu();
                });
                this.setSlot(slot, teamToggle);
                slot += 1;
            }

            // Server-owned
            if (Permissions.check(player, "sswaystones.create.server", 4)) {
                GuiElementBuilder serverToggle = new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setSkullOwner(IconConstants.OBSERVER)
                        .setName(Text.translatable("gui.sswaystones.toggle_server")
                                .formatted(accessSettings.isServerOwned() ? Formatting.GREEN : Formatting.RED));

                serverToggle.setCallback((index, type, action, gui) -> {
                    accessSettings.setServerOwned(!accessSettings.isServerOwned());
                    this.updateMenu();
                });
                this.setSlot(slot, serverToggle);
                slot += 1;
            }

            // If no settings were available
            if (slot == 10) {
                this.setSlot(13,
                        new GuiElementBuilder(Items.BARRIER)
                                .setLore(List.of(Text.translatable("error.sswaystones.no_modification_permission")
                                        .formatted(Formatting.GRAY)))
                                .setName(Text.translatable("gui.back").formatted(Formatting.RED))
                                .setCallback((index, type, action, gui) -> {
                                    this.close();
                                    ViewerUtil.openJavaGui(player, waystone);
                                }));
            }
        }

        @Override
        public void onClose() {
            super.onClose();
            ViewerUtil.openJavaGui(player, waystone);
        }
    }

    public static class AddTrustedPlayerGui extends SimpleGui {
        private final WaystoneRecord waystone;
        private final ArrayList<ServerPlayerEntity> onlinePlayers = new ArrayList<>(this.getPlayer().server.getPlayerManager().getPlayerList());
        private int pageIndex = 0;

        public AddTrustedPlayerGui(WaystoneRecord waystone, ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X6, player, false);
            this.waystone = waystone;

            this.setTitle(Text.translatable("gui.sswaystones.add_trusted_player"));
            updateMenu();
        }


        public void updateMenu() {
            int totalPlayers = getPlayer().server.getPlayerManager().getPlayerList().size();
            int playersPerPage = 45;
            int maxPages = Math.max((int) Math.ceil((double) totalPlayers / playersPerPage), 1);
            int offset = pageIndex * playersPerPage;
            this.setTitle(Text.translatable("gui.sswaystones.add_trusted_player").append(String.format(" [%d/%d]", pageIndex + 1, maxPages)));

            GuiElementBuilder previousPage = new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setSkullOwner(IconConstants.ARROW_LEFT)
                    .setName(Text.translatable("gui.sswaystones.page_previous"))
                    .setCallback((index2, type2, action2, gui2) -> {
                        pageIndex--;
                        if (pageIndex < 0) {
                            pageIndex = (int) Math.ceil((double) onlinePlayers.size() / 45) - 1;
                        }
                        this.updateMenu();
                    });

            GuiElementBuilder nextPage = new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setSkullOwner(IconConstants.ARROW_RIGHT)
                    .setName(Text.translatable("gui.sswaystones.page_next"))
                    .setCallback((index2, type2, action2, gui2) -> {
                        pageIndex++;
                        if (pageIndex >= Math.ceil((double) onlinePlayers.size() / 45)) {
                            pageIndex = 0;
                        }
                        this.updateMenu();
                    });

            GuiElementBuilder cancel = new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setSkullOwner(IconConstants.CANCEL)
                    .setName(Text.translatable("gui.sswaystones.cancel").formatted(Formatting.RED))
                    .setCallback((index2, type2, action2, gui2) -> {
                        this.close();
                        new TrustedPlayersGui(waystone, this.getPlayer()).open();
                    });

            for (int i = 0; i < 54; i++) {
                if (i > 45) {
                    this.setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(Text.literal("")));
                }
                if (i == 45) {
                    this.setSlot(i, previousPage);
                }
                if (i == 47) {
                    this.setSlot(i, nextPage);
                }
                if(i == 49){
                    this.setSlot(i, cancel);
                }
            }

            onlinePlayers.remove(player);
            for (int i = offset; i < Math.min(offset + playersPerPage, onlinePlayers.size()); i++) {
                ServerPlayerEntity player = onlinePlayers.get(i);
                GuiElementBuilder playerItemBuilder = new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setSkullOwner(player.getGameProfile(), this.getPlayer().server)
                        .setName(Text.literal("Add " + player.getName().getString()).formatted(Formatting.GREEN));

                playerItemBuilder.setCallback((index2, type2, action2, gui2) -> {
                    waystone.getAccessSettings().addTrustedPlayer(player.getUuid());
                    new TrustedPlayersGui(waystone, this.getPlayer()).open();
                });

                this.setSlot(i - offset, playerItemBuilder);
            }
        }

        @Override
        public void onClose() {
            super.onClose();
            new TrustedPlayersGui(waystone, player).open();
        }
    }

    public static class AddTrustedOfflinePlayerGui extends AnvilInputGui {
        private final WaystoneRecord waystone;
        private ScheduledFuture<?> scheduledLookup = null;
        private final ScheduledExecutorService scheduler = newSingleThreadScheduledExecutor();
        private String lastLookupQuery = null;

        public AddTrustedOfflinePlayerGui(WaystoneRecord waystone, ServerPlayerEntity player) {
            super(player, false);
            this.waystone = waystone;

            this.setSlot(1, new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(IconConstants.CANCEL)
                    .setName(Text.translatable("gui.back")).setCallback((index, type, action, gui) ->  new TrustedPlayersGui(waystone, player).open()));

            updateDoneButton();

            this.setTitle(Text.translatable("gui.sswaystones.add_trusted_offline_player"));
        }

        @Override
        public void onInput(String input) {
            super.onInput(input);
            updateDoneButton();
        }

       private void updateDoneButton() {
            String input = this.getInput();

            GuiElementBuilder button = new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setName(Text.translatable("gui.sswaystones.done").formatted(Formatting.GREEN))
                    .setSkullOwner(IconConstants.CHECKMARK);

            button.setCallback((index, type, action, gui) -> {
                String playerName = this.getInput();
                if (playerName.isEmpty()) {
                    this.close();
                    new TrustedPlayersGui(waystone, player).open();
                    return;
                }

                lookupPlayerProfile(playerName, (profileOpt) -> {
                    if (profileOpt.isPresent()) {
                        waystone.getAccessSettings().addTrustedPlayer(profileOpt.get().getId());
                    }
                    this.close();
                    new TrustedPlayersGui(waystone, player).open();
                });
            });
            this.setSlot(2, button);

           //TODO: look into this, for some reason for CookieDoughnut2 it doens't work and returns null or smthing
            if (!input.isEmpty()) {
                lookupPlayerProfile(input, (cachedProfile) -> {
                    if (cachedProfile.isPresent()) {
                        GuiElementBuilder updatedButton = new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(Text.translatable("gui.sswaystones.done").formatted(Formatting.GREEN))
                                .setSkullOwner(cachedProfile.get(), player.server)
                                .setCallback((index, type, action, gui) -> {
                                    waystone.getAccessSettings().addTrustedPlayer(cachedProfile.get().getId());
                                    this.close();
                                    new TrustedPlayersGui(waystone, player).open();
                                });
                        this.setSlot(2, updatedButton);
                    }
                });
            }
        }

        private void lookupPlayerProfile(String playerName, java.util.function.Consumer<Optional<GameProfile>> callback) {
            if (playerName == null || playerName.isEmpty()) {
                player.server.execute(() -> callback.accept(Optional.empty()));
                return;
            }
            if (scheduledLookup != null && !scheduledLookup.isDone()) {
                scheduledLookup.cancel(false);
            }

            final String currentQuery = playerName;
            if (currentQuery.equals(lastLookupQuery)) {
                return;
            }

            scheduledLookup = scheduler.schedule(() -> {
                lastLookupQuery = currentQuery;

                Optional<GameProfile> profile = Optional.empty();
                try {
                    String apiUrl = "https://api.mojang.com/users/profiles/minecraft/" + currentQuery;
                    URL url = new URL(apiUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    if (connection.getResponseCode() == 200) {
                        BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        String json = response.toString();
                        String id = extractJsonValue(json, "id");
                        String name = extractJsonValue(json, "name");

                        if (id != null && name != null) {
                            UUID uuid = convertDashlessUUID(id);
                            profile = Optional.of(new GameProfile(uuid, name));
                        }
                    }
                    connection.disconnect();
                } catch (Exception e) {}

                final Optional<GameProfile> finalProfile = profile;
                player.server.execute(() -> callback.accept(finalProfile));
            }, 150, TimeUnit.MILLISECONDS);
        }

        private UUID convertDashlessUUID(String id) {
            return UUID.fromString(id.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5"
            ));
        }

        private String extractJsonValue(String json, String key) {
            String keyPattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(keyPattern);
            java.util.regex.Matcher matcher = pattern.matcher(json);

            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }

        @Override
        public void onClose() {
            super.onClose();
            new TrustedPlayersGui(waystone, player).open();
        }
    }

    public static class TrustedPlayersGui extends SimpleGui {
        private final WaystoneRecord waystone;
        private int pageIndex = 0;

        public TrustedPlayersGui(WaystoneRecord waystone, ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X6, player, false);
            this.waystone = waystone;
            this.updateMenu();
        }

        private void updateMenu() {
            for(int i = 0; i < 54; i++) {
                this.clearSlot(i);
            }

            int totalTrustedPlayers = waystone.getAccessSettings().getTrustedPlayers().size();
            int playersPerPage = 45;
            int maxPages = Math.max((int) Math.ceil((double) totalTrustedPlayers / playersPerPage), 1);
            int offset = pageIndex * playersPerPage;
            this.setTitle(Text.translatable("gui.sswaystones.trusted_players").append(String.format(" [%d/%d]", pageIndex + 1, maxPages)));

            for (int i = 0; i < 54; i++) {
                if (i > 44) {
                    this.setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(Text.literal("")));
                }
                if (i == 45) {
                    this.setSlot(i, new GuiElementBuilder(Items.PLAYER_HEAD)
                            .setSkullOwner(IconConstants.ARROW_LEFT)
                            .setName(Text.translatable("gui.sswaystones.page_previous"))
                            .setCallback((index, type, action, gui) -> {
                                if (totalTrustedPlayers > 0) {
                                    pageIndex--;
                                    if (pageIndex < 0) {
                                        pageIndex = maxPages - 1;
                                    }
                                    this.updateMenu();
                                }
                            }));
                }
                if (i == 47) {
                    this.setSlot(i, new GuiElementBuilder(Items.PLAYER_HEAD)
                            .setSkullOwner(IconConstants.ARROW_RIGHT)
                            .setName(Text.translatable("gui.sswaystones.page_next"))
                            .setCallback((index, type, action, gui) -> {
                                pageIndex++;
                                if (pageIndex >= maxPages) {
                                    pageIndex = 0;
                                }
                                this.updateMenu();
                            }));
                }
            }

            List<UUID> trustedPlayers = waystone.getAccessSettings().getTrustedPlayers();
            for (int i = offset; i < Math.min(offset + playersPerPage, trustedPlayers.size()); i++) {
                UUID playerUuid = trustedPlayers.get(i);
                GameProfile profile = this.getPlayer().server.getUserCache().getByUuid(playerUuid).orElse(null);

                if(profile == null) {
                    int finalI = i;
                    GameProfile tempProfile = new GameProfile(playerUuid, "Loading...");
                    GuiElementBuilder loadingItem = new GuiElementBuilder(Items.PLAYER_HEAD)
                            .setName(Text.literal("Loading player data...").formatted(Formatting.GRAY));
                    this.setSlot(finalI - offset, loadingItem);

                    lookupPlayerProfile(playerUuid.toString(), (optionalProfile) -> {
                        if (optionalProfile.isPresent()) {
                            GameProfile newProfile = optionalProfile.get();
                            String playerName = newProfile.getName();
                            GuiElementBuilder playerItemBuilder = new GuiElementBuilder(Items.PLAYER_HEAD)
                                    .setSkullOwner(newProfile, this.getPlayer().server)
                                    .setName(Text.literal("Remove " + playerName).formatted(Formatting.RED));

                            playerItemBuilder.setCallback((index, type, action, gui) -> {
                                waystone.getAccessSettings().removeTrustedPlayer(playerUuid);
                                this.updateMenu();
                            });
                            // Check if this slot is still valid before updating
                            if (isOpen() && finalI - offset < 45) {
                                this.setSlot(finalI - offset, playerItemBuilder);
                            }
                        }
                    });
                }else{
                    String playerName = profile != null ? profile.getName() : playerUuid.toString();
                    GuiElementBuilder playerItemBuilder = new GuiElementBuilder(Items.PLAYER_HEAD)
                            .setSkullOwner(profile != null ? profile : new GameProfile(playerUuid, null), this.getPlayer().server)
                            .setName(Text.literal("Remove " + playerName).formatted(Formatting.RED));

                    playerItemBuilder.setCallback((index, type, action, gui) -> {
                        waystone.getAccessSettings().removeTrustedPlayer(playerUuid);
                        this.updateMenu();
                    });
                    this.setSlot(i - offset, playerItemBuilder);
                }
            }

            GuiElementBuilder cancel = new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setSkullOwner(IconConstants.CANCEL)
                    .setName(Text.translatable("gui.sswaystones.back").formatted(Formatting.RED))
                    .setCallback((index, type, action, gui) -> {
                        this.close();
                        ViewerUtil.openJavaGui(this.getPlayer(), waystone);
                    });

            GuiElementBuilder addTrustedPlayerItem = new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setSkullOwner(IconConstants.PLUS)
                    .setName(Text.translatable("gui.sswaystones.add_trusted_player").formatted(Formatting.GREEN));

            addTrustedPlayerItem.setCallback((index, type, action, gui) -> {
                AddTrustedPlayerGui addTrustedPlayerGui = new AddTrustedPlayerGui(waystone, this.getPlayer());
                addTrustedPlayerGui.open();
            });

            GuiElementBuilder addTrustedOfflinePlayer = new GuiElementBuilder(Items.NAME_TAG)
                    .setItemName(Text.translatable("gui.sswaystones.add_trusted_offline_player").formatted(Formatting.GREEN));

            addTrustedOfflinePlayer.setCallback((index, type, action, gui) -> {
                AddTrustedOfflinePlayerGui addTrustedOfflinePlayerGui = new AddTrustedOfflinePlayerGui(waystone, this.getPlayer());
                addTrustedOfflinePlayerGui.open();
            });

            this.setSlot(50, addTrustedOfflinePlayer);
            this.setSlot(49, addTrustedPlayerItem);
            this.setSlot(53, cancel);
        }

       private void lookupPlayerProfile(String uuidOrName, java.util.function.Consumer<Optional<GameProfile>> callback) {
           final ScheduledExecutorService scheduler = newSingleThreadScheduledExecutor();

            if (uuidOrName == null || uuidOrName.isEmpty()) {
                player.server.execute(() -> callback.accept(Optional.empty()));
                return;
            }

           scheduler.schedule(() -> {
                Optional<GameProfile> profile = Optional.empty();
                try {
                    String apiUrl;
                    boolean isUUID = uuidOrName.length() > 16;

                    if (isUUID) {
                        String uuidWithoutDashes = uuidOrName.replace("-", "");
                        apiUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuidWithoutDashes;
                    } else {
                        apiUrl = "https://api.mojang.com/users/profiles/minecraft/" + uuidOrName;
                    }

                    URL url = new URL(apiUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    if (connection.getResponseCode() == 200) {
                        BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        String json = response.toString();
                        String id = extractJsonValue(json, "id");
                        String name = extractJsonValue(json, "name");

                        if (id != null && name != null) {
                            UUID uuid = convertDashlessUUID(id);
                            profile = Optional.of(new GameProfile(uuid, name));
                        }
                    }
                    connection.disconnect();
                } catch (Exception e) {}

                final Optional<GameProfile> finalProfile = profile;
                player.server.execute(() -> callback.accept(finalProfile));
            }, 0, TimeUnit.MILLISECONDS);
        }

        private UUID convertDashlessUUID(String id) {
            return UUID.fromString(id.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5"
            ));
        }

        private String extractJsonValue(String json, String key) {
            String keyPattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(keyPattern);
            java.util.regex.Matcher matcher = pattern.matcher(json);

            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }

        @Override
        public void onClose() {
            super.onClose();
            new AccessSettingsGui(waystone, player).open();
        }
    }
}
