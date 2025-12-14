/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.block;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import java.awt.*;
import java.util.Random;
import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import lol.sylvie.sswaystones.util.HashUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class WaystoneBlockEntity extends BlockEntity {
    private static final Random RANDOM = new Random();
    private final ElementHolder holder = new ElementHolder();
    private ChunkAttachment attachment;

    public TextDisplayElement nameDisplay = null;
    public ItemDisplayElement eyeDisplay = null;

    public WaystoneRecord waystone;

    public WaystoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.WAYSTONE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level world, WaystoneBlockEntity waystoneEntity) {
        WaystoneRecord record = waystoneEntity.getThisWaystone(world);
        boolean waystoneOwned = record != null;
        boolean shouldCreateName = record != null && waystoneEntity.nameDisplay == null;

        // Create the display itself
        if (waystoneEntity.eyeDisplay == null || shouldCreateName) {
            waystoneEntity.createHologramDisplay(world);
        }

        ChatFormatting color = ChatFormatting.RESET;
        if (waystoneOwned) {
            // Team coloring
            String teamName = record.getAccessSettings().getTeam();
            if (!teamName.isEmpty()) {
                PlayerTeam team = world.getScoreboard().getPlayerTeam(teamName);
                if (team != null) {
                    color = team.getColor();
                }
            }

            if (waystoneEntity.nameDisplay != null) {
                waystoneEntity.nameDisplay.setText(record.getWaystoneText().copy().withStyle(color));
            }

            // TODO: Maybe cache this value?
            waystoneEntity.eyeDisplay.setItem(getDisplayIcon(world.getServer(), record));
        }

        // Eye rotation
        waystoneEntity.eyeDisplay.setYaw(waystoneEntity.eyeDisplay.getYaw() + 4f);

        // Particles
        if (RANDOM.nextInt(0, 20) == 0 && world instanceof ServerLevel serverWorld) {
            Vec3 pos = waystoneEntity.getBlockPos().offset(0, 1, 0).getCenter();
            Integer colorValue = color.getColor();
            serverWorld.sendParticles(
                    new DustParticleOptions(
                            color == ChatFormatting.RESET || colorValue == null ? Color.RED.getRGB() : colorValue, 1f),
                    pos.x(), pos.y(), pos.z(), 8, 0.1d, 0.1d, 0.1d, 0.1d);
        }

        if (waystoneEntity.nameDisplay == null)
            return;

        // Bob up and down
        double y = (Math.sin((double) System.currentTimeMillis() / 1000) / 32) + 1.55d;
        waystoneEntity.nameDisplay.setOffset(new Vec3(0, y, 0));
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
        return Waystones.configuration.getInstance().physicalIconDisplay && record != null
                ? record.getIconOrHead(server)
                : Items.ENDER_EYE.getDefaultInstance();
    }

    public void createHologramDisplay(Level world) {
        this.removeDisplay();

        WaystoneRecord record = getThisWaystone(world);

        // Eye display
        ItemStack stack = getDisplayIcon(world.getServer(), record);

        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        eyeDisplay = new ItemDisplayElement(stack);
        eyeDisplay.setOffset(new Vec3(0, 1.125, 0));
        eyeDisplay.setScale(new Vector3f(0.75f, 0.75f, 0.75f));
        eyeDisplay.setInterpolationDuration(2);

        holder.addElement(eyeDisplay);

        // Waystone name display
        if (record != null) {
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
