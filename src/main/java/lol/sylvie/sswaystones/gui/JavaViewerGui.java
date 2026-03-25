/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.*;
import java.util.List;
import lol.sylvie.sswaystones.storage.PlayerData;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

public class JavaViewerGui extends SimpleGui {
    private static final int ITEMS_PER_PAGE = 9 * 5;
    private final WaystoneRecord waystone;
    private int pageIndex = 0;

    private final List<WaystoneRecord> accessible;
    private final int maxPages;

    public JavaViewerGui(ServerPlayer player, @Nullable WaystoneRecord waystone) {
        super(MenuType.GENERIC_9x6, player, false);
        this.waystone = waystone;

        WaystoneStorage storage = WaystoneStorage.getServerState(player.level().getServer());
        this.accessible = storage.getAccessibleWaystones(player, waystone);
        this.maxPages = Math.max(Math.ceilDiv(this.accessible.size(), ITEMS_PER_PAGE), 1);

        this.updateMenu();
    }

    public void updateMenu() {
        // If there are no other waystones this will display as 0
        if (waystone != null) {
            this.setTitle(Component.literal(String.format("%s [%s] (%s/%s)", waystone.getWaystoneName(),
                    waystone.getOwnerName(), pageIndex + 1, maxPages)));
        } else {
            this.setTitle(Component.literal(String.format("Waystones (%s/%s)", pageIndex + 1, maxPages)));
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

            GuiElementBuilder element = new GuiElementBuilder(record.getIconOrHead(player.level().getServer()))
                    .setName(record.getWaystoneText().copy().withStyle(ChatFormatting.YELLOW));

            if (!record.getAccessSettings().isServerOwned())
                element.setLore(List.of(Component.nullToEmpty(record.getOwnerName())));
            else
                element.glow(true);

            element.setCallback((index, type, action, gui) -> {
                if (type.isRight) {
                    WaystoneStorage storage = WaystoneStorage.getServerState(player.level().getServer());
                    if (!record.getAccessSettings().isEffectivelyGlobal()
                            && !record.getOwnerUUID().equals(player.getUUID())
                            && storage.waystones.containsKey(record.getHash()))
                        new ConfirmDeleteGui(waystone, record, player).open();
                } else {
                    record.handleTeleport(player);
                    gui.close();
                }
            });

            this.setSlot(slot, element);
        }

        for (int i = 45; i < 54; i++) {
            this.setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(Component.empty()));
        }

        // Gui controls
        this.setSlot(45,
                new GuiElementBuilder(Items.PLAYER_HEAD).setProfileSkinTexture(IconConstants.ARROW_LEFT)
                        .setName(Component.translatable("gui.sswaystones.page_previous"))
                        .setCallback((index, type, action, gui) -> previousPage()));

        this.setSlot(47,
                new GuiElementBuilder(Items.PLAYER_HEAD).setProfileSkinTexture(IconConstants.ARROW_RIGHT)
                        .setName(Component.translatable("gui.sswaystones.page_next"))
                        .setCallback((index, type, action, gui) -> nextPage()));

        // Waystone settings
        if (waystone == null)
            return;

        if (waystone.canPlayerEdit(player)) {
            if (Permissions.check(player, "sswaystones.manager", 4)
                    && !waystone.getOwnerUUID().equals(player.getUUID())) {
                this.setSlot(50, new GuiElementBuilder(Items.PLAYER_HEAD).setProfileSkinTexture(IconConstants.CHEST)
                        .setName(Component.translatable("gui.sswaystones.steal_waystone").withStyle(ChatFormatting.RED))
                        .setCallback((index, type, action, gui) -> {
                            waystone.setOwner(player);
                            this.updateMenu();
                        }));
            }

            // Setting menus
            this.setSlot(51, new GuiElementBuilder(waystone.getIconOrHead(player.level().getServer()))
                    .setName(Component.translatable("gui.sswaystones.change_icon").withStyle(ChatFormatting.YELLOW))
                    .glow().setCallback((index, type, action, gui) -> new IconGui(waystone, player).open()));

            this.setSlot(52, new GuiElementBuilder(Items.PLAYER_HEAD).setProfileSkinTexture(IconConstants.ANVIL)
                    .setName(Component.translatable("gui.sswaystones.change_name").withStyle(ChatFormatting.YELLOW))
                    .setCallback((index, type, action, gui) -> new NameGui(waystone, player).open()));

            this.setSlot(53,
                    new GuiElementBuilder(Items.PLAYER_HEAD).setProfileSkinTexture(IconConstants.COMMAND_BLOCK)
                            .setName(Component.translatable("gui.sswaystones.access_settings")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE))
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

        public NameGui(WaystoneRecord waystone, ServerPlayer player) {
            super(player, false);
            this.waystone = waystone;

            this.setDefaultInputValue(waystone.getWaystoneName());
            this.setSlot(1,
                    new GuiElementBuilder(Items.PLAYER_HEAD).setProfileSkinTexture(IconConstants.CANCEL)
                            .setName(Component.translatable("gui.back"))
                            .setCallback((index, type, action, gui) -> gui.close()));

            this.setSlot(2, new GuiElementBuilder(Items.PLAYER_HEAD).setProfileSkinTexture(IconConstants.CHECKMARK)
                    .setName(CommonComponents.GUI_DONE).setCallback((index, type, action, gui) -> {
                        String input = this.getInput();
                        waystone.setWaystoneName(input);
                        gui.close();
                    }));

            this.setTitle(Component.translatable("gui.sswaystones.change_name_title"));
        }

        @Override
        public void close() {
            super.close();
            ViewerUtil.openJavaGui(player, waystone);
        }
    }

    protected static class IconGui extends SimpleGui {
        private final WaystoneRecord waystone;

        public IconGui(WaystoneRecord waystone, ServerPlayer player) {
            super(MenuType.GENERIC_3x3, player, false);
            this.waystone = waystone;
            this.updateMenu();
            this.setTitle(Component.translatable("gui.sswaystones.change_icon_title"));
        }

        private void updateMenu() {
            for (int i = 0; i < 9; i++) {
                this.setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(Component
                        .translatable("gui.sswaystones.change_icon_instruction").withStyle(ChatFormatting.GRAY)));
            }
            this.setSlot(4, waystone.getIconOrHead(player.level().getServer()));
        }

        @Override
        public boolean onAnyClick(int index, ClickType type, ContainerInput action) {
            if (index > 8 && action.equals(ContainerInput.PICKUP)) {
                if (index > 35)
                    index -= 36; // Get hotbar slot
                ItemStack stack = player.getInventory().getItem(index);

                if (!stack.is(Items.AIR)) {
                    waystone.setIcon(stack.getItem());
                    this.close();
                }
            }
            return super.onAnyClick(index, type, action);
        }

        @Override
        public void close() {
            super.close();
            ViewerUtil.openJavaGui(player, waystone);
        }
    }

    protected static class AccessSettingsGui extends SimpleGui {
        private final WaystoneRecord waystone;

        public AccessSettingsGui(WaystoneRecord waystone, ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);
            this.waystone = waystone;

            this.setTitle(Component.translatable("gui.sswaystones.access_settings"));
            this.updateMenu();
        }

        private void updateMenu() {
            // Framing
            for (int i = 0; i < (9 * 3); i++) {
                this.setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(Component
                        .translatable("gui.sswaystones.access_settings_instruction").withStyle(ChatFormatting.GRAY)));
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
                        .setProfileSkinTexture(IconConstants.GLOBE)
                        .setName(Component.translatable("gui.sswaystones.toggle_global")
                                .withStyle(accessSettings.isGlobal() ? ChatFormatting.GREEN : ChatFormatting.RED));

                globalToggle.setCallback((index, type, action, gui) -> {
                    accessSettings.setGlobal(!accessSettings.isGlobal());
                    this.updateMenu();
                });
                this.setSlot(slot, globalToggle);
                slot += 1;
            }

            // Team
            PlayerTeam team = player.getTeam();
            if (team != null && Permissions.check(player, "sswaystones.create.team", true)) {
                String teamName = team.getName();
                GuiElementBuilder teamToggle = new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setProfileSkinTexture(IconConstants.SHIELD)
                        .setName(Component.translatable("gui.sswaystones.toggle_team")
                                .withStyle(accessSettings.hasTeam() ? ChatFormatting.GREEN : ChatFormatting.RED));

                teamToggle.setCallback((index, type, action, gui) -> {
                    accessSettings.setTeam(accessSettings.hasTeam() ? "" : teamName);
                    this.updateMenu();
                });
                this.setSlot(slot, teamToggle);
                slot += 1;
            }

            // Server-owned
            if (Permissions.check(player, "sswaystones.create.server", false)) {
                GuiElementBuilder serverToggle = new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setProfileSkinTexture(IconConstants.OBSERVER)
                        .setName(Component.translatable("gui.sswaystones.toggle_server")
                                .withStyle(accessSettings.isServerOwned() ? ChatFormatting.GREEN : ChatFormatting.RED));

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
                                .setLore(List.of(Component.translatable("error.sswaystones.no_modification_permission")
                                        .withStyle(ChatFormatting.GRAY)))
                                .setName(CommonComponents.GUI_BACK.copy().withStyle(ChatFormatting.RED))
                                .setCallback((index, type, action, gui) -> {
                                    this.close();
                                    ViewerUtil.openJavaGui(player, waystone);
                                }));
            }
        }

        @Override
        public void close() {
            super.close();
            ViewerUtil.openJavaGui(player, waystone);
        }
    }

    protected static class ConfirmDeleteGui extends SimpleGui {
        private final WaystoneRecord waystone;
        private final WaystoneRecord target;

        public ConfirmDeleteGui(WaystoneRecord waystone, WaystoneRecord target, ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);
            this.waystone = waystone;
            this.target = target;

            this.setTitle(Component.translatable("gui.sswaystones.forget_waystone", target.getWaystoneName()));
            this.updateMenu();
        }

        private void updateMenu() {
            for (int i = 0; i < (9 * 3); i++) {
                this.setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(Component.empty()));
            }

            this.setSlot(10,
                    new GuiElementBuilder(Items.PLAYER_HEAD).setProfileSkinTexture(IconConstants.TRASH)
                            .setName(Component.translatable("selectWorld.deleteButton").withStyle(ChatFormatting.RED))
                            .setCallback((index, type, action, gui) -> {
                                PlayerData data = WaystoneStorage.getPlayerState(player);
                                data.getDiscoveredWaystones().remove(this.target.getHash());
                                gui.close();
                            }));

            this.setSlot(16,
                    new GuiElementBuilder(Items.PLAYER_HEAD).setProfileSkinTexture(IconConstants.ARROW_LEFT)
                            .setName(CommonComponents.GUI_CANCEL.copy().withStyle(ChatFormatting.GREEN))
                            .setCallback((index, type, action, gui) -> {
                                gui.close();
                            }));
        }

        @Override
        public void close() {
            super.close();
            ViewerUtil.openJavaGui(player, waystone);
        }
    }
}
