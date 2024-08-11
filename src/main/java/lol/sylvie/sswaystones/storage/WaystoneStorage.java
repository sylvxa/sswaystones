package lol.sylvie.sswaystones.storage;

import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.util.NameGenerator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.*;

public class WaystoneStorage extends PersistentState {
    public HashMap<String, WaystoneRecord> waystones = new HashMap<>();
    public HashMap<UUID, PlayerData> players = new HashMap<>();


    private static final Type<WaystoneStorage> TYPE = new Type<>(
            WaystoneStorage::new,
            WaystoneStorage::createFromNbt,
            null
    );

    // Serialization
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        // Actual waystone info
        NbtCompound waystonesNbt = new NbtCompound();
        waystones.forEach((hash, waystoneRecord) -> waystonesNbt.put(hash, waystoneRecord.toNbt()));
        nbt.put("waystones", waystonesNbt);

        // Player-specific waystone info
        NbtCompound playersNbt = new NbtCompound();
        players.forEach((uuid, playerData) -> playersNbt.put(uuid.toString(), playerData.toNbt()));
        nbt.put("players", playersNbt);

        return nbt;
    }

    // Deserialization
    public static WaystoneStorage createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        WaystoneStorage storage = new WaystoneStorage();

        // Actual waystone info
        NbtCompound waystonesNbt = tag.getCompound("waystones");
        waystonesNbt.getKeys().forEach((hash) -> {
            NbtCompound waystoneNbt = waystonesNbt.getCompound(hash);
            storage.waystones.put(hash, WaystoneRecord.fromNbt(waystoneNbt));
        });

        // Player-specific waystone info
        NbtCompound playersNbt = tag.getCompound("players");
        playersNbt.getKeys().forEach((uuid) -> {
            NbtCompound playerNbt = playersNbt.getCompound(uuid);
            storage.players.put(UUID.fromString(uuid), PlayerData.fromNbt(playerNbt));
        });

        return storage;
    }

    public static WaystoneStorage getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = Objects.requireNonNull(server.getWorld(World.OVERWORLD)).getPersistentStateManager();
        WaystoneStorage state = persistentStateManager.getOrCreate(TYPE, Waystones.MOD_ID);

        state.markDirty();

        return state;
    }

    public static PlayerData getPlayerState(LivingEntity player) {
        WaystoneStorage serverState = getServerState(Objects.requireNonNull(player.getWorld().getServer()));

        return serverState.players.computeIfAbsent(player.getUuid(), uuid -> new PlayerData());
    }

    // Utility functions
    // Shorthand to get waystone record by hash
    public WaystoneRecord getWaystone(String hash) {
        return this.waystones.get(hash);
    }

    // Create a waystone
    public WaystoneRecord createWaystone(BlockPos pos, World world, LivingEntity player) {
        WaystoneRecord record = new WaystoneRecord(player.getUuid(), player.getName().getString(), NameGenerator.generateName(), pos, world.getRegistryKey(), false);
        String hash = record.getHash();
        this.waystones.put(hash, record);

        getPlayerState(player).discoveredWaystones.add(hash);

        return record;
    }

    // Get all known waystones
    public List<WaystoneRecord> getDiscoveredWaystones(LivingEntity player) {
        PlayerData playerData = getPlayerState(player);

        return this.waystones.entrySet()
                .stream()
                .filter(r -> playerData.discoveredWaystones.contains(r.getKey()) || r.getValue().isGlobal())
                .map(Map.Entry::getValue)
                .toList();
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
