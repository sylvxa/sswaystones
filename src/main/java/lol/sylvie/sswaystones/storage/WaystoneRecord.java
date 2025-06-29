/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.storage;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.block.WaystoneBlock;
import lol.sylvie.sswaystones.config.Configuration;
import lol.sylvie.sswaystones.enums.Visibility;
import lol.sylvie.sswaystones.util.HashUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Uuids;
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
    private final AccessSettings accessSettings;
    private ItemStack icon;

    public static final Codec<WaystoneRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Uuids.CODEC.fieldOf("waystone_owner").forGetter(WaystoneRecord::getOwnerUUID),
            Codec.STRING.fieldOf("waystone_owner_name").forGetter(WaystoneRecord::getOwnerName),
            Codec.STRING.fieldOf("waystone_name").forGetter(WaystoneRecord::getWaystoneName),
            BlockPos.CODEC.fieldOf("position").forGetter(WaystoneRecord::getPos),
            World.CODEC.fieldOf("world").forGetter(WaystoneRecord::getWorldKey),
            AccessSettings.CODEC.optionalFieldOf("access_settings")
                    .forGetter((i) -> Optional.of(i.getAccessSettings())),
            Codec.either(Registries.ITEM.getCodec(), ItemStack.CODEC)
                            .xmap(either -> either.map(
                                            item -> new ItemStack(item),
                                            itemStack -> itemStack
                                    ),
                                    stack -> Either.right(stack))
                            .optionalFieldOf("icon", Items.PLAYER_HEAD.getDefaultStack())
                            .forGetter(WaystoneRecord::getIcon))
                .apply(instance, WaystoneRecord::new));


    // Optional fields share the same instance of a default value, so we have to use
    // this weird workaround
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private WaystoneRecord(UUID owner, String ownerName, String waystoneName, BlockPos pos, RegistryKey<World> world,
                           Optional<AccessSettings> accessSettings, ItemStack icon) {
        this(owner, ownerName, waystoneName, pos, world,
                accessSettings.orElseGet(() -> new AccessSettings(Visibility.DISCOVERABLE, false, "", new ArrayList<UUID>())), icon);
    }

    public WaystoneRecord(UUID owner, String ownerName, String waystoneName, BlockPos pos, RegistryKey<World> world,
                          AccessSettings accessSettings, ItemStack icon) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.setWaystoneName(waystoneName); // Limits waystone name
        this.pos = pos;
        this.world = world;
        this.accessSettings = accessSettings;
        this.icon = icon == null ? Items.PLAYER_HEAD.getDefaultStack() : icon;
    }

    public void handleTeleport(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        assert server != null;

        // Run on main thread to avoid a race condition for Geyser players
        if (!server.isOnThread()) {
            server.execute(() -> handleTeleport(player));
            return;
        }

        Configuration.Instance config = Waystones.configuration.getInstance();

        // Experience cost
        int requiredXp = getXpCost(player);
        if (requiredXp > 0) {
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

        // This may happen if someone has a waystone in a dimension from a mod that is
        // no longer present
        ServerWorld targetWorld = this.getWorld(server);
        if (targetWorld == null) {
            player.sendMessage(Text.translatable("error.sswaystones.no_dimension").formatted(Formatting.RED));
            return;
        }

        // Remove invalid waystones
        BlockPos target = this.getPos();
        if (!(targetWorld.getBlockState(target).getBlock() instanceof WaystoneBlock) && config.removeInvalidWaystones) {
            WaystoneStorage.getServerState(server).destroyWaystone(this);
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

    public boolean canPlayerEdit(ServerPlayerEntity player) {
        return this.getOwnerUUID().equals(player.getUuid()) || Permissions.check(player, "sswaystones.manager", 4);
    }

    public int getXpCost(ServerPlayerEntity player) {
        Configuration.Instance config = Waystones.configuration.getInstance();
        if (player.isCreative())
            return 0;
        return player.getWorld().getRegistryKey().equals(this.getWorldKey())
                ? config.xpCost
                : config.crossDimensionXpCost;
    }

    public ItemStack getIconOrHead(@Nullable MinecraftServer server) {
        if (icon != null && icon != Items.PLAYER_HEAD.getDefaultStack())
            return icon;

        // The server has to fetch the player's skin
        GameProfile profile = new GameProfile(this.getOwnerUUID(), this.getOwnerName());
        if (server != null && server.getSessionService().getTextures(profile) == MinecraftProfileTextures.EMPTY) {
            ProfileResult fetched = server.getSessionService().fetchProfile(profile.getId(), false);
            if (fetched != null)
                profile = fetched.profile();
        }

        ItemStack head = Items.PLAYER_HEAD.getDefaultStack();
        head.set(DataComponentTypes.PROFILE, new ProfileComponent(profile));
        return head;
    }

    // Getters and setters
    public UUID getOwnerUUID() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwner(PlayerEntity player) {
        this.owner = player.getUuid();
        this.ownerName = player.getGameProfile().getName();
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

    public RegistryKey<World> getWorldKey() {
        return world;
    }

    public AccessSettings getAccessSettings() {
        return accessSettings;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon;
    }

    public static class AccessSettings {
        private Visibility visibility; // Blanket flag, allows all players to access
        private boolean server; // Hides the actual owner and makes it unbreakable
        private String team; // Scoreboard team
        private ArrayList<UUID> trustedPlayers; // Players that can access this waystone on private visibility

        public static final Codec<AccessSettings> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(Codec.STRING.fieldOf("visibility").forGetter(AccessSettings::getVisibilityAsString),
                        Codec.BOOL.fieldOf("server").forGetter(AccessSettings::isServerOwned),
                        Codec.STRING.fieldOf("team").forGetter(AccessSettings::getTeam),
                        Codec.list(Uuids.CODEC).fieldOf("trused_players").forGetter(AccessSettings::getTrustedPlayers))
                .apply(instance, AccessSettings::new));

        public AccessSettings(String visibility, boolean server, String team, List<UUID> trustedPlayers) {
            this.visibility = Enum.valueOf(Visibility.class, visibility);
            this.server = server;
            this.team = team;
            this.trustedPlayers = new ArrayList<>(trustedPlayers);
        }

        public AccessSettings(Visibility visibility, boolean server, String team, List<UUID> trustedPlayers) {
            this.visibility = visibility;
            this.server = server;
            this.team = team;
            this.trustedPlayers = new ArrayList<>(trustedPlayers);
        }

        public boolean canPlayerAccess(WaystoneRecord parent, ServerPlayerEntity player) {
            if(this.visibility == Visibility.PRIVATE && parent.owner.equals(player.getUuid()))
                return true;
            else if(this.visibility == Visibility.PRIVATE && this.trustedPlayers.contains(player.getUuid()))
                return true;
            else if(this.visibility == Visibility.PRIVATE)
                return false;

            PlayerData data = WaystoneStorage.getPlayerState(player);
            if (data.discoveredWaystones.contains(parent.getHash()))
                return true;

            if(this.visibility == Visibility.PUBLIC)
                return true;
            if(this.isServerOwned())
                return true;

            Team team = player.getScoreboardTeam();
            if (team != null && team.getName().equals(this.team))
                return true;

            return false;
        }

        public String getVisibilityAsString() {
            return visibility.toString();
        }

        public Visibility getVisibility() {
            return visibility;
        }

        public void setVisibility(Visibility visibility) {
            this.visibility = visibility;
        }

        public List<UUID> getTrustedPlayers() {
            return trustedPlayers;
        }

        public void addTrustedPlayer(UUID uuid) {
            if(!trustedPlayers.contains(uuid))
                trustedPlayers.add(uuid);
        }

        public void removeTrustedPlayer(UUID uuid) {
            trustedPlayers.remove(uuid);
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

    public Text getWaystoneText() {
        return Text.literal(this.getWaystoneName());
    }

    public ServerWorld getWorld(MinecraftServer server) {
        return server.getWorld(this.getWorldKey());
    }
}
