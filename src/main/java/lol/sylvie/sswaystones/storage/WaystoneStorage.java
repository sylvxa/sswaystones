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
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

public class WaystoneStorage extends PersistentState {
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
            Codec.unboundedMap(Uuids.CODEC, PlayerData.CODEC).fieldOf("players").forGetter(WaystoneStorage::getPlayers))
            .apply(instance, WaystoneStorage::new));

    private static final PersistentStateType<WaystoneStorage> TYPE = new PersistentStateType<>(Waystones.MOD_ID,
            WaystoneStorage::new, CODEC, null);

    public Map<String, WaystoneRecord> getWaystones() {
        return waystones;
    }

    public Map<UUID, PlayerData> getPlayers() {
        return players;
    }

    public static WaystoneStorage getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = Objects.requireNonNull(server.getWorld(World.OVERWORLD))
                .getPersistentStateManager();
        WaystoneStorage state = persistentStateManager.getOrCreate(TYPE);

        state.markDirty();

        return state;
    }

    public static PlayerData getPlayerState(ServerPlayerEntity player) {
        WaystoneStorage serverState = getServerState(Objects.requireNonNull(player.getWorld().getServer()));

        return serverState.players.computeIfAbsent(player.getUuid(), uuid -> new PlayerData());
    }

    // Utility functions
    public WaystoneRecord getWaystone(String hash) {
        return this.waystones.get(hash);
    }

    public List<WaystoneRecord> getAccessibleWaystones(ServerPlayerEntity player, WaystoneRecord record) {
        // Get all waystones that the player can access
        // Sorted by name, though prioritize server owned waystones
        return this.waystones.values().stream()
                .filter(waystone -> waystone.getAccessSettings().canPlayerAccess(waystone, player) && waystone != record)
                .sorted(Comparator.comparing(WaystoneRecord::getWaystoneName))
                .sorted(Comparator.comparing((waystone) -> !waystone.getAccessSettings().isServerOwned()))
                .toList();
    }

    // Create a waystone
    public WaystoneRecord createWaystone(BlockPos pos, World world, ServerPlayerEntity player) {
        WaystoneRecord record = new WaystoneRecord(player.getUuid(), player.getName().getString(),
                NameGenerator.generateName(), pos, world.getRegistryKey(), new WaystoneRecord.AccessSettings(false, false, ""), Items.PLAYER_HEAD);
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
