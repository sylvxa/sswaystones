/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.storage;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.block.ModBlocks;
import lol.sylvie.sswaystones.config.Configuration;
import lol.sylvie.sswaystones.util.HashUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
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
        Configuration.Instance config = Waystones.configuration.getInstance();
        // Experience cost
        int requiredXp = config.xpCost;
        if (requiredXp > 0 && !player.isCreative()) {
            if (player.experienceLevel < requiredXp) {
                player.sendMessage(
                        Text.translatable("error.sswaystones.not_enough_xp", requiredXp - player.experienceLevel)
                                .formatted(Formatting.RED),
                        true);
                return;
            } else {
                player.addExperienceLevels(Math.min(-requiredXp, 0)); // Stop negative values from adding xp
            }
        }

        MinecraftServer server = player.getServer();
        assert server != null;

        BlockPos target = this.getPos();
        ServerWorld targetWorld = this.getWorld(server);

        if (targetWorld == null) {
            player.sendMessage(Text.translatable("error.sswaystones.no_dimension").formatted(Formatting.RED));
            return;
        }

        // Remove invalid waystones
        if (!targetWorld.getBlockState(target).isOf(ModBlocks.WAYSTONE) && config.removeInvalidWaystones) {
            WaystoneStorage.getServerState(server).destroyWaystone(server, this);
            player.sendMessage(Text.translatable("error.sswaystones.invalid_waystone").formatted(Formatting.RED));
            return;
        }

        if (config.safeTeleport) {
            // Remove any blocks trying to suffocate the player
            BlockPos head = target.add(0, 1, 0);
            BlockState headState = targetWorld.getBlockState(head);
            if (!headState.getCollisionShape(targetWorld, head).isEmpty()) {
                if (headState.getHardness(targetWorld, head) != -1) {
                    player.getServer().executeSync(() -> targetWorld.breakBlock(head, true));
                }
            }

            // Make sure there is a platform beneath the waystone
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos ground = this.pos.add(x, -1, z);
                    if (targetWorld.getBlockState(ground).isAir()) {
                        targetWorld.setBlockState(ground, Blocks.COBBLESTONE.getDefaultState());
                    }
                }
            }
        }

        // Search for a suitable teleport location
        List<Vec3i> positionChecks = List.of(new Vec3i(-1, -1, 0), new Vec3i(1, -1, 0), new Vec3i(0, -1, -1),
                new Vec3i(0, -1, 1), new Vec3i(-1, -1, -1), new Vec3i(1, -1, 1), new Vec3i(1, -1, -1),
                new Vec3i(-1, -1, 1));

        for (Vec3i checkPos : positionChecks) {
            BlockPos ground = target.add(checkPos);
            BlockPos feet = ground.add(0, 1, 0);
            BlockPos head = feet.add(0, 1, 0);

            if (!targetWorld.getBlockState(ground).getCollisionShape(targetWorld, ground).isEmpty()
                    && targetWorld.getBlockState(feet).getCollisionShape(targetWorld, feet).isEmpty()
                    && targetWorld.getBlockState(head).getCollisionShape(targetWorld, head).isEmpty()) {
                target = feet;
                break;
            }
        }

        // Teleport!
        Vec3d center = target.toBottomCenterPos();
        player.teleport(targetWorld, center.getX(), center.getY(), center.getZ(), Set.of(), player.getYaw(),
                player.getPitch(), false);
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

    public ServerWorld getWorld(MinecraftServer server) {
        return server.getWorld(this.getWorldKey());
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
