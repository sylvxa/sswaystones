/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.block;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import java.util.Random;
import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import lol.sylvie.sswaystones.util.HashUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class WaystoneBlockEntity extends BlockEntity {
    private static final Random RANDOM = new Random();
    private final ElementHolder holder = new ElementHolder();
    private ChunkAttachment attachment;
    private boolean lastTickNameHidden = false;

    public TextDisplayElement nameDisplay = null;
    public ItemDisplayElement eyeDisplay = null;

    public WaystoneRecord waystone;

    public WaystoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.WAYSTONE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level world, WaystoneBlockEntity waystoneEntity) {
        WaystoneRecord record = waystoneEntity.getThisWaystone(world);
        boolean waystoneOwned = record != null;
        boolean nameHidden = record.getAccessSettings().isNameHidden();
        boolean shouldCreateName = record != null && waystoneEntity.nameDisplay == null && !nameHidden;
        // Create the display itself
        if ((waystoneEntity.eyeDisplay == null || shouldCreateName
                || waystoneEntity.lastTickNameHidden != nameHidden)) {
            waystoneEntity.createHologramDisplay(world);
        }
        waystoneEntity.lastTickNameHidden = nameHidden;

        // Eye rotation
        // We use game time instead of just adding yaw because the display is recreated
        // when a waystone is discovered
        // and it would reset jarringly if it wasn't separate from the display objects
        waystoneEntity.eyeDisplay.setYaw(((world.getGameTime() + waystoneEntity.hashCode()) % 90) * 4);

        TeamColor color = null;
        if (waystoneOwned) {
            // Team coloring
            String teamName = record.getAccessSettings().getTeam();
            if (!teamName.isEmpty()) {
                PlayerTeam team = world.getScoreboard().getPlayerTeam(teamName);
                if (team != null) {
                    color = team.getColor().orElse(null);
                }
            }

            // TODO: Maybe cache this value?
            waystoneEntity.eyeDisplay.setItem(getDisplayIcon(world.getServer(), record));

            if (waystoneEntity.nameDisplay != null) {
                MutableComponent displayName = record.getWaystoneText().copy();
                if (color != null)
                    displayName.withColor(color.textColor());
                waystoneEntity.nameDisplay.setText((Component) displayName);

                // Bob up and down
                double y = (Math.sin((double) System.currentTimeMillis() / 1000) / 32) + 1.55d;
                waystoneEntity.nameDisplay.setOffset(new Vec3(0, y, 0));
            }
        }
        // Particles
        if (RANDOM.nextInt(0, waystoneOwned ? 20 : 10) == 0 && world instanceof ServerLevel) {
            ServerLevel serverWorld = (ServerLevel) world;
            Vec3 pos = Vec3.atBottomCenterOf((Vec3i) waystoneEntity.getBlockPos()).add(0.0D, 1.0D, 0.0D);
            boolean noTeam = (color == null);
            ParticleOptions options = noTeam
                    ? (ParticleOptions) ParticleTypes.PORTAL
                    : (ParticleOptions) new DustParticleOptions(color.rgb(), 1.0F);
            serverWorld.sendParticles(options, pos.x(), pos.y(), pos.z(), 8, 0.1D, 0.1D, 0.1D, 0.1D);
        }
    }

    private @Nullable WaystoneRecord getThisWaystone(Level world) {
        if (world.isClientSide())
            return null;
        assert world.getServer() != null; // World can't be client.

        // Grab waystone if it isn't there
        if (this.waystone == null) {
            WaystoneStorage storage = WaystoneStorage.getServerState(world.getServer());
            this.waystone = storage.getWaystone(HashUtil.getHash(worldPosition, world.dimension()));
        }

        // This can still be null!
        return this.waystone;
    }

    public static ItemStack getDisplayIcon(MinecraftServer server, @Nullable WaystoneRecord record) {
        ItemStack iconStack = Waystones.configuration.getInstance().physicalIconDisplay && record != null
                ? record.getIconOrHead(server)
                : Items.ENDER_EYE.getDefaultInstance();
        iconStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return iconStack;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null)
            WaystoneBlock.onRemoved(level, pos);
    }

    public void createHologramDisplay(Level world) {
        this.removeDisplay();

        WaystoneRecord record = getThisWaystone(world);
        boolean exists = record != null;
        assert record != null;
        WaystoneRecord.AccessSettings accessSettings = record.getAccessSettings();
        // Eye display
        ItemStack glowingEnderPearl = Items.ENDER_PEARL.getDefaultInstance();
        glowingEnderPearl.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        ItemStack stack = exists ? getDisplayIcon(world.getServer(), record) : glowingEnderPearl;
        eyeDisplay = new ItemDisplayElement(stack);
        eyeDisplay.setOffset(new Vec3(0, 1.125, 0));
        eyeDisplay.setScale(new Vector3f(0.75f, 0.75f, 0.75f));
        eyeDisplay.setInterpolationDuration(1);

        holder.addElement(eyeDisplay);

        // Waystone name display
        if (!accessSettings.isNameHidden()) {
            nameDisplay = new TextDisplayElement();

            nameDisplay.setText(record.getWaystoneText());
            nameDisplay.setTextAlignment(Display.TextDisplay.Align.CENTER);
            nameDisplay.setBillboardMode(Display.BillboardConstraints.CENTER);

            holder.addElement(nameDisplay);
        }

        attachment = (ChunkAttachment) ChunkAttachment.ofTicking(holder, (ServerLevel) world, worldPosition);
    }

    public void removeDisplay() {
        if (eyeDisplay != null)
            holder.removeElement(eyeDisplay);
        if (nameDisplay != null)
            holder.removeElement(nameDisplay);
        if (attachment != null)
            attachment.destroy();
    }
}
