/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.storage;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.Arrays;
import java.util.UUID;
import lol.sylvie.sswaystones.util.HashUtil;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class WaystoneRecord {
    private UUID owner;
    private String ownerName;
    private String waystoneName;
    private final BlockPos pos; // Must be final as the hash is calculated based on pos and world
    private final RegistryKey<World> world;
    private boolean global;
    private Item icon;

    public WaystoneRecord(UUID owner, String ownerName, String waystoneName, BlockPos pos, RegistryKey<World> world,
            boolean global, @Nullable Item icon) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.setWaystoneName(waystoneName); // Limits waystone name
        this.pos = pos;
        this.world = world;
        this.global = global;
        this.icon = icon;
    }

    public String asString() {
        return HashUtil.waystoneIdentifier(pos, world);
    }

    public String getHash() {
        return HashUtil.getHash(this);
    }

    public NbtCompound toNbt() {
        NbtCompound waystoneTag = new NbtCompound();

        waystoneTag.putUuid("waystone_owner", owner);
        waystoneTag.putString("waystone_owner_name", ownerName);

        waystoneTag.putString("waystone_name", waystoneName);

        waystoneTag.putIntArray("position", Arrays.asList(pos.getX(), pos.getY(), pos.getZ()));
        waystoneTag.putString("world", world.getValue().toString());

        waystoneTag.putBoolean("global", global);

        if (icon != null)
            waystoneTag.putString("icon", icon.toString());

        return waystoneTag;
    }

    public static WaystoneRecord fromNbt(NbtCompound nbt) {
        UUID waystoneOwner = nbt.getUuid("waystone_owner");
        String waystoneOwnerName = nbt.getString("waystone_owner_name");

        String waystoneName = nbt.getString("waystone_name");

        int[] posAsInt = nbt.getIntArray("position");
        BlockPos position = new BlockPos(posAsInt[0], posAsInt[1], posAsInt[2]);

        Identifier worldIdentifier = Identifier.of(nbt.getString("world"));
        RegistryKey<World> worldRegistryKey = RegistryKey.of(RegistryKeys.WORLD, worldIdentifier);

        boolean global = nbt.getBoolean("global");

        Item icon = null;
        if (nbt.contains("icon")) {
            String iconStringId = nbt.getString("icon");
            Identifier iconId = Identifier.tryParse(iconStringId);
            if (iconId != null) {
                icon = Registries.ITEM.get(iconId);
            }
        }

        return new WaystoneRecord(waystoneOwner, waystoneOwnerName, waystoneName, position, worldRegistryKey, global,
                icon);
    }

    public void handleTeleport(ServerPlayerEntity player) {
        BlockPos target = this.getPos();
        assert player.getServer() != null;
        ServerWorld targetWorld = player.getServer().getWorld(this.getWorldKey());

        if (targetWorld == null) {
            player.sendMessage(Text.translatable("error.sswaystones.no_dimension"));
            return;
        }

        // Search for suitable teleport location.
        // It looked weird starting on a corner so I make it try a cardinal direction
        // first
        if (targetWorld.getBlockState(target.add(-1, -1, 0)).isAir()) {
            boolean foundTarget = false;
            searchloop : for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockState state = targetWorld.getBlockState(target.add(x, -1, z));
                    if (!state.isAir()) {
                        target = target.add(x, 0, z);
                        foundTarget = true;
                        break searchloop;
                    }
                }
            }

            if (!foundTarget) {
                target = target.add(0, 2, 0);
            }
        } else {
            target = target.add(-1, 0, 0);
        }

        // Teleport!
        Vec3d center = target.toBottomCenterPos();
        player.teleport(targetWorld, center.getX(), center.getY(), center.getZ(), player.getYaw(), player.getPitch());
        targetWorld.playSound(null, target, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1f);
        targetWorld.spawnParticles(ParticleTypes.DRAGON_BREATH, center.getX(), center.getY() + 1f, center.getZ(), 16,
                0.5d, 0.5d, 0.5d, 0.1d);
    }

    public UUID getOwnerUUID() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getWaystoneName() {
        return waystoneName;
    }

    public Text getWaystoneText() {
        return Text.literal(this.getWaystoneName());
    }

    public BlockPos getPos() {
        return pos;
    }

    public RegistryKey<World> getWorldKey() {
        return world;
    }

    public boolean isGlobal() {
        return global;
    }

    public Item getIcon() {
        return icon;
    }

    public ItemStack getIconOrHead(@Nullable MinecraftServer server) {
        // Turns out, the server needs to fetch this! Oops!
        GameProfile profile = new GameProfile(this.getOwnerUUID(), this.getOwnerName());
        if (server != null && server.getSessionService().getTextures(profile) == MinecraftProfileTextures.EMPTY) {
            ProfileResult fetched = server.getSessionService().fetchProfile(profile.getId(), false);
            if (fetched != null)
                profile = fetched.profile();
        }

        if (icon == null) {
            ItemStack head = Items.PLAYER_HEAD.getDefaultStack();
            head.set(DataComponentTypes.PROFILE, new ProfileComponent(profile));
            return head;
        }
        return icon.getDefaultStack();
    }

    public void setOwner(PlayerEntity player) {
        this.owner = player.getUuid();
        this.ownerName = player.getGameProfile().getName();
    }

    public void setWaystoneName(String waystoneName) {
        waystoneName = waystoneName.substring(0, Math.min(waystoneName.length(), 32));
        this.waystoneName = waystoneName;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }

    public void setIcon(Item icon) {
        this.icon = icon;
    }

    public boolean canEdit(ServerPlayerEntity player) {
        return this.getOwnerUUID().equals(player.getUuid()) || player.hasPermissionLevel(4);
    }
}
