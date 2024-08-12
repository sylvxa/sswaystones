package lol.sylvie.sswaystones.gui;

import com.mojang.authlib.GameProfile;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class JavaViewerGui extends SimpleGui {
    private static final int ITEMS_PER_PAGE = 9 * 5;
    private final WaystoneRecord waystone;
    private int pageIndex = 0;

    private final List<WaystoneRecord> discovered;
    private final int maxPages;

    public JavaViewerGui(ServerPlayerEntity player, @Nullable WaystoneRecord waystone) {
        super(ScreenHandlerType.GENERIC_9X6, player, false);
        this.waystone = waystone;

        assert player.getServer() != null; // It's a ServerPlayerEntity.
        WaystoneStorage storage = WaystoneStorage.getServerState(player.getServer());
        this.discovered = storage.getDiscoveredWaystones(player).stream()
                .filter(r -> !r.equals(waystone))
                .sorted(Comparator.comparing(WaystoneRecord::getWaystoneName))
                .toList();
        this.maxPages = Math.ceilDiv(this.discovered.size(), ITEMS_PER_PAGE);
    }

    public void updateMenu() {
        // If there are no other waystones this will display as 0
        int displayMaxPages = Math.max(maxPages, 1);
        if (waystone != null) {
            this.setTitle(Text.literal(String.format("%s [%s] (%s/%s)", waystone.getWaystoneName(), waystone.getOwnerName(), pageIndex + 1, displayMaxPages)));
        } else {
            this.setTitle(Text.literal(String.format("Waystones (%s/%s)", pageIndex + 1, displayMaxPages)));
        }

        int offset = ITEMS_PER_PAGE * pageIndex;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            this.clearSlot(i);
        }

        for (int i = offset; i < this.discovered.size(); i++) {
            WaystoneRecord record = this.discovered.get(i);

            this.setSlot(i - offset, new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setSkullOwner(new GameProfile(record.getOwnerUUID(), record.getOwnerName()), player.server)
                    .setName(Text.literal(record.getWaystoneName()))
                    .setLore(List.of(Text.of(record.getOwnerName())))
                    .setCallback((index, type, action, gui) -> {
                        record.handleTeleport(player);
                        gui.close();
                    }));
        }

        String arrowLeft = "ewogICJ0aW1lc3RhbXAiIDogMTU5Mzk3NTc5NDQ3NCwKICAicHJvZmlsZUlkIiA6ICJhNjhmMGI2NDhkMTQ0MDAwYTk1ZjRiOWJhMTRmOGRmOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNSEZfQXJyb3dMZWZ0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y3YWFjYWQxOTNlMjIyNjk3MWVkOTUzMDJkYmE0MzM0MzhiZTQ2NDRmYmFiNWViZjgxODA1NDA2MTY2N2ZiZTIiCiAgICB9CiAgfQp9";
        String arrowRight = "ewogICJ0aW1lc3RhbXAiIDogMTYwMDk5NjI3NjA3OSwKICAicHJvZmlsZUlkIiA6ICI1MGM4NTEwYjVlYTA0ZDYwYmU5YTdkNTQyZDZjZDE1NiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNSEZfQXJyb3dSaWdodCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kMzRlZjA2Mzg1MzcyMjJiMjBmNDgwNjk0ZGFkYzBmODVmYmUwNzU5ZDU4MWFhN2ZjZGYyZTQzMTM5Mzc3MTU4IgogICAgfQogIH0KfQ==";

        // I couldn't think of an icon that fits "take ownership", but this looks cool!
        String companionCube = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTMxMTI1YjBmMjk0MmZhM2NkMjdjODAyNTg2M2ViYzNlOWQ3YmZkNjg1NDdlNjEwYTlkM2UxODMyMDc1MzM2NCJ9fX0=";

        String anvil = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWI0MjVhYTNkOTQ2MThhODdkYWM5Yzk0ZjM3N2FmNmNhNDk4NGMwNzU3OTY3NGZhZDkxN2Y2MDJiN2JmMjM1In19fQ==";
        String globe = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjBhY2EwMTMxNzhhOWY0NzkxM2U4OTRkM2QwYmZkNGIwYjY2MTIwODI1YjlhYWI4YTRkN2Q5YmYwMjQ1YWJmIn19fQ==";

        for (int i = 45; i < 54; i++) {
            this.setSlot(i, new ItemStack(Items.GRAY_STAINED_GLASS_PANE));
        }

        // Gui controls
        this.setSlot(45, new GuiElementBuilder()
                .setItem(Items.PLAYER_HEAD)
                .setSkullOwner(arrowLeft, null, null)
                .setName(Text.literal("Previous Page"))
                .setCallback((index, type, action, gui) -> previousPage()));

        this.setSlot(47, new GuiElementBuilder()
                .setItem(Items.PLAYER_HEAD)
                .setSkullOwner(arrowRight, null, null)
                .setName(Text.literal("Next Page"))
                .setCallback((index, type, action, gui) -> nextPage()));

        // Waystone settings
        if (waystone == null) return;

        if (waystone.canEdit(player)) {
            if (player.hasPermissionLevel(4)) {
                this.setSlot(51, new GuiElementBuilder()
                        .setItem(Items.PLAYER_HEAD)
                        .setSkullOwner(companionCube)
                        .setName(Text.literal("Take Ownership"))
                        .setCallback((index, type, action, gui) -> {
                            waystone.setOwner(player);
                            this.updateMenu();
                        }));
            }

            this.setSlot(52, new GuiElementBuilder()
                    .setItem(Items.PLAYER_HEAD)
                    .setSkullOwner(anvil)
                    .setName(Text.literal("Change Name"))
                    .setCallback((index, type, action, gui) -> {
                        this.changeName();
                        this.updateMenu();
                    }));

            boolean global = this.waystone.isGlobal();
            this.setSlot(53, new GuiElementBuilder()
                    .setItem(Items.PLAYER_HEAD)
                    .setSkullOwner(globe)
                    .setName(Text.literal("Toggle Global").formatted(global ? Formatting.GREEN : Formatting.RED))
                    .glow(global)
                    .setCallback((index, type, action, gui) -> {
                        this.waystone.setGlobal(!global);
                        this.updateMenu();
                    }));
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

    public void changeName() {
        AnvilInputGui anvilGui = new AnvilInputGui(player, false) {
            @Override
            public void onClose() {
                super.onClose();

                JavaViewerGui viewer = new JavaViewerGui(player, waystone);
                viewer.updateMenu();
                viewer.open();
            }
        };

        anvilGui.setDefaultInputValue(waystone.getWaystoneName());
        String checkmark = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTc5YTVjOTVlZTE3YWJmZWY0NWM4ZGMyMjQxODk5NjQ5NDRkNTYwZjE5YTQ0ZjE5ZjhhNDZhZWYzZmVlNDc1NiJ9fX0=";

        anvilGui.setSlot(1, new GuiElementBuilder()
                .setItem(Items.BARRIER)
                .setName(Text.literal("Back"))
                .setCallback((index, type, action, gui) -> gui.close()));

        anvilGui.setSlot(2, new GuiElementBuilder()
                .setItem(Items.PLAYER_HEAD)
                .setSkullOwner(checkmark)
                .setName(Text.literal("Confirm"))
                .setCallback((index, type, action, gui) -> {
                    String input = anvilGui.getInput();
                    waystone.setWaystoneName(input);
                    gui.close();
                }));

        anvilGui.setTitle(Text.literal("Waystone Name"));
        anvilGui.open();
    }
}
