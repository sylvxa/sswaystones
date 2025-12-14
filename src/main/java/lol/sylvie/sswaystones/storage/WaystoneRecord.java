/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.storage;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.block.WaystoneBlock;
import lol.sylvie.sswaystones.config.Configuration;
import lol.sylvie.sswaystones.util.HashUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

public final class WaystoneRecord {
    private UUID owner;
    private String ownerName;
    private String waystoneName;
    private final BlockPos pos; // Must be final as the hash is calculated based on pos and world
    private final ResourceKey<Level> world;
    private final AccessSettings accessSettings;
    private Item icon;

    public static final Codec<WaystoneRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.AUTHLIB_CODEC.fieldOf("waystone_owner").forGetter(WaystoneRecord::getOwnerUUID),
            Codec.STRING.fieldOf("waystone_owner_name").forGetter(WaystoneRecord::getOwnerName),
            Codec.STRING.fieldOf("waystone_name").forGetter(WaystoneRecord::getWaystoneName),
            BlockPos.CODEC.fieldOf("position").forGetter(WaystoneRecord::getPos),
            Level.RESOURCE_KEY_CODEC.fieldOf("world").forGetter(WaystoneRecord::getWorldKey),
            AccessSettings.CODEC.optionalFieldOf("access_settings")
                    .forGetter((i) -> Optional.of(i.getAccessSettings())),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.PLAYER_HEAD).forGetter(WaystoneRecord::getIcon))
            .apply(instance, WaystoneRecord::new));

    // Optional fields share the same instance of a default value, so we have to use
    // this weird workaround
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private WaystoneRecord(UUID owner, String ownerName, String waystoneName, BlockPos pos, ResourceKey<Level> world,
            Optional<AccessSettings> accessSettings, Item icon) {
        this(owner, ownerName, waystoneName, pos, world,
                accessSettings.orElseGet(() -> new AccessSettings(false, false, "")), icon);
    }

    public WaystoneRecord(UUID owner, String ownerName, String waystoneName, BlockPos pos, ResourceKey<Level> world,
            AccessSettings accessSettings, Item icon) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.setWaystoneName(waystoneName); // Limits waystone name
        this.pos = pos;
        this.world = world;
        this.accessSettings = accessSettings;
        this.icon = icon == null ? Items.PLAYER_HEAD : icon;
    }

    public void handleTeleport(ServerPlayer player) {
        Level world = player.level();
        MinecraftServer server = world.getServer();
        assert server != null;

        // Run on main thread to avoid a race condition for Geyser players
        if (!server.isSameThread()) {
            server.execute(() -> handleTeleport(player));
            return;
        }

        Configuration.Instance config = Waystones.configuration.getInstance();

        // Experience cost
        int requiredXp = getXpCost(player);
        if (requiredXp > 0) {
            if (player.experienceLevel < requiredXp) {
                player.displayClientMessage(
                        Component.translatable("error.sswaystones.not_enough_xp", requiredXp - player.experienceLevel)
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            } else {
                player.giveExperienceLevels(Math.min(-requiredXp, 0)); // Stop negative values from adding xp
            }
        }

        // This may happen if someone has a waystone in a dimension from a mod that is
        // no longer present
        ServerLevel targetWorld = this.getWorld(server);
        if (targetWorld == null) {
            player.sendSystemMessage(Component.translatable("error.sswaystones.no_dimension").withStyle(ChatFormatting.RED));
            return;
        }

        // Remove invalid waystones
        BlockPos target = this.getPos();
        if (!(targetWorld.getBlockState(target).getBlock() instanceof WaystoneBlock) && config.removeInvalidWaystones) {
            WaystoneStorage.getServerState(server).destroyWaystone(this);
            player.sendSystemMessage(Component.translatable("error.sswaystones.invalid_waystone").withStyle(ChatFormatting.RED));
            return;
        }

        if (config.safeTeleport) {
            // Remove any blocks trying to suffocate the player
            BlockPos head = target.offset(0, 1, 0);
            BlockState headState = targetWorld.getBlockState(head);
            if (!headState.getCollisionShape(targetWorld, head).isEmpty()) {
                if (headState.getDestroySpeed(targetWorld, head) != -1) {
                    server.executeIfPossible(() -> targetWorld.destroyBlock(head, true));
                }
            }

            // Make sure there is a platform beneath the waystone
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos ground = this.pos.offset(x, -1, z);
                    if (targetWorld.getBlockState(ground).isAir()) {
                        targetWorld.setBlockAndUpdate(ground, Blocks.COBBLESTONE.defaultBlockState());
                    }
                }
            }
        }

        // Search for a suitable teleport location
        List<Vec3i> positionChecks = List.of(new Vec3i(-1, -1, 0), new Vec3i(1, -1, 0), new Vec3i(0, -1, -1),
                new Vec3i(0, -1, 1), new Vec3i(-1, -1, -1), new Vec3i(1, -1, 1), new Vec3i(1, -1, -1),
                new Vec3i(-1, -1, 1));

        for (Vec3i checkPos : positionChecks) {
            BlockPos ground = target.offset(checkPos);
            BlockPos feet = ground.offset(0, 1, 0);
            BlockPos head = feet.offset(0, 1, 0);

            if (!targetWorld.getBlockState(ground).getCollisionShape(targetWorld, ground).isEmpty()
                    && targetWorld.getBlockState(feet).getCollisionShape(targetWorld, feet).isEmpty()
                    && targetWorld.getBlockState(head).getCollisionShape(targetWorld, head).isEmpty()) {
                target = feet;
                break;
            }
        }

        // Teleport!
        Vec3 center = target.getBottomCenter();
        player.teleportTo(targetWorld, center.x(), center.y(), center.z(), Set.of(), player.getYRot(),
                player.getXRot(), false);
        targetWorld.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
        targetWorld.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1f), center.x(),
                center.y() + 1f, center.z(), 16, 0.5d, 0.5d, 0.5d, 0.1d);
    }

    public boolean canPlayerEdit(ServerPlayer player) {
        return this.getOwnerUUID().equals(player.getUUID()) || Permissions.check(player, "sswaystones.manager", 4);
    }

    public int getXpCost(ServerPlayer player) {
        Configuration.Instance config = Waystones.configuration.getInstance();
        if (player.isCreative())
            return 0;
        return player.level().dimension().equals(this.getWorldKey())
                ? config.xpCost
                : config.crossDimensionXpCost;
    }

    public ItemStack getIconOrHead(@Nullable MinecraftServer server) {
        if (icon != null && icon != Items.PLAYER_HEAD)
            return icon.getDefaultInstance();

        // The server has to fetch the player's skin
        GameProfile profile = new GameProfile(this.getOwnerUUID(), this.getOwnerName());
        if (server != null) {
            MinecraftSessionService service = server.services().sessionService();
            if (service.getTextures(profile) == MinecraftProfileTextures.EMPTY) {
                ProfileResult fetched = service.fetchProfile(profile.id(), false);
                if (fetched != null)
                    profile = fetched.profile();
            }
        }

        ItemStack head = Items.PLAYER_HEAD.getDefaultInstance();
        head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
        return head;
    }

    // Getters and setters
    public UUID getOwnerUUID() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
        this.ownerName = player.getGameProfile().name();
    }

    public String getWaystoneName() {
        return waystoneName;
    }

    public void setWaystoneName(String waystoneName) {
        waystoneName = waystoneName.substring(0, Math.min(waystoneName.length(), 32));
        this.waystoneName = waystoneName;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ResourceKey<Level> getWorldKey() {
        return world;
    }

    public AccessSettings getAccessSettings() {
        return accessSettings;
    }

    public Item getIcon() {
        return icon;
    }

    public void setIcon(Item icon) {
        this.icon = icon;
    }

    public static class AccessSettings {
        private boolean global; // Blanket flag, allows all players to access
        private boolean server; // Hides the actual owner and makes it unbreakable
        private String team; // Scoreboard team

        public static final Codec<AccessSettings> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(Codec.BOOL.fieldOf("global").forGetter(AccessSettings::isGlobal),
                        Codec.BOOL.fieldOf("server").forGetter(AccessSettings::isServerOwned),
                        Codec.STRING.fieldOf("team").forGetter(AccessSettings::getTeam))
                .apply(instance, AccessSettings::new));

        public AccessSettings(boolean global, boolean server, String team) {
            this.global = global;
            this.server = server;
            this.team = team;
        }

        public boolean canPlayerAccess(WaystoneRecord parent, ServerPlayer player) {
            PlayerData data = WaystoneStorage.getPlayerState(player);
            if (data.discoveredWaystones.contains(parent.getHash()))
                return true;

            if (this.isGlobal())
                return true;
            if (this.isServerOwned())
                return true;
            PlayerTeam team = player.getTeam();
            if (team != null && team.getName().equals(this.team))
                return true;

            return false;
        }

        public boolean isGlobal() {
            return global;
        }

        public void setGlobal(boolean global) {
            this.global = global;
        }

        public boolean isServerOwned() {
            return server;
        }

        public void setServerOwned(boolean server) {
            this.server = server;
        }

        public String getTeam() {
            return team;
        }

        public void setTeam(String team) {
            this.team = team;
        }

        public boolean hasTeam() {
            return !this.getTeam().isEmpty();
        }
    }

    public String asString() {
        return HashUtil.waystoneIdentifier(pos, world);
    }

    public String getHash() {
        return HashUtil.getHash(this);
    }

    public Component getWaystoneText() {
        return Component.literal(this.getWaystoneName());
    }

    public ServerLevel getWorld(MinecraftServer server) {
        return server.getLevel(this.getWorldKey());
    }
}
