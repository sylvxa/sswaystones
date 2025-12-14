/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.util.NameGenerator;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class WaystoneStorage extends SavedData {
    public HashMap<String, WaystoneRecord> waystones;
    public HashMap<UUID, PlayerData> players;

    public WaystoneStorage() {
        this(new HashMap<>(), new HashMap<>());
    }

    public WaystoneStorage(Map<String, WaystoneRecord> waystones, Map<UUID, PlayerData> players) {
        this.waystones = new HashMap<>(waystones);
        this.players = new HashMap<>(players);
    }

    public static final Codec<WaystoneStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, WaystoneRecord.CODEC).fieldOf("waystones")
                    .forGetter(WaystoneStorage::getWaystones),
            Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, PlayerData.CODEC).fieldOf("players")
                    .forGetter(WaystoneStorage::getPlayers))
            .apply(instance, WaystoneStorage::new));

    private static final SavedDataType<WaystoneStorage> TYPE = new SavedDataType<>(Waystones.MOD_ID,
            WaystoneStorage::new, CODEC, null);

    public Map<String, WaystoneRecord> getWaystones() {
        return waystones;
    }

    public Map<UUID, PlayerData> getPlayers() {
        return players;
    }

    public static WaystoneStorage getServerState(MinecraftServer server) {
        DimensionDataStorage persistentStateManager = Objects.requireNonNull(server.getLevel(Level.OVERWORLD))
                .getDataStorage();
        WaystoneStorage state = persistentStateManager.computeIfAbsent(TYPE);

        state.setDirty();

        return state;
    }

    public static PlayerData getPlayerState(ServerPlayer player) {
        WaystoneStorage serverState = getServerState(Objects.requireNonNull(player.level().getServer()));

        return serverState.players.computeIfAbsent(player.getUUID(), uuid -> new PlayerData());
    }

    // Utility functions
    public WaystoneRecord getWaystone(String hash) {
        return this.waystones.get(hash);
    }

    public List<WaystoneRecord> getAccessibleWaystones(ServerPlayer player, WaystoneRecord record) {
        // Get all waystones that the player can access
        // Sorted by name, though prioritize server owned waystones
        return this.waystones.values().stream()
                .filter(waystone -> waystone.getAccessSettings().canPlayerAccess(waystone, player)
                        && waystone != record)
                .sorted(Comparator.comparing(WaystoneRecord::getWaystoneName))
                .sorted(Comparator.comparing((waystone) -> !waystone.getAccessSettings().isServerOwned())).toList();
    }

    // Create a waystone
    public WaystoneRecord createWaystone(BlockPos pos, Level world, ServerPlayer player) {
        if (!Permissions.check(player, "sswaystones.create.place", true)) {
            player.sendSystemMessage(
                    Component.translatable("error.sswaystones.no_create_permission").withStyle(ChatFormatting.RED));
            return null;
        }

        int waystoneLimit = Waystones.configuration.getInstance().waystoneLimit;
        int waystoneCount = (int) waystones.values().stream()
                .filter(w -> w.getOwnerUUID().equals(player.getUUID()) && !w.getAccessSettings().isServerOwned())
                .count();
        if (waystoneLimit != 0 && waystoneCount >= waystoneLimit
                && !Permissions.check(player, "sswaystones.manager.bypass_limit", 4)) {
            player.sendSystemMessage(
                    Component.translatable("error.sswaystones.reached_limit").withStyle(ChatFormatting.RED));
            return null;
        }

        WaystoneRecord record = new WaystoneRecord(player.getUUID(), player.getName().getString(),
                NameGenerator.generateName(), pos, world.dimension(),
                new WaystoneRecord.AccessSettings(false, false, ""), Items.PLAYER_HEAD);
        String hash = record.getHash();
        this.waystones.put(hash, record);

        getPlayerState(player).discoveredWaystones.add(hash);

        return record;
    }

    // Make all players forget about waystone
    public void amnesiaWaystone(WaystoneRecord record) {
        String hash = record.getHash();

        for (PlayerData playerData : this.players.values()) {
            playerData.discoveredWaystones.remove(hash);
        }
    }

    // Remove all traces of waystone
    public void destroyWaystone(WaystoneRecord record) {
        amnesiaWaystone(record);

        this.waystones.remove(record.getHash());
    }
}
